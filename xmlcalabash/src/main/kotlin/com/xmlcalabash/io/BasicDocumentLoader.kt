package com.xmlcalabash.io

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.xmlcalabash.datamodel.DocumentContextImpl
import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsHtml
import com.xmlcalabash.namespace.NsXml
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.TypeUtils
import com.xmlcalabash.util.UriUtils
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.om.NamespaceMap
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import net.sf.saxon.trans.XPathException
import net.sf.saxon.value.BooleanValue
import nu.validator.htmlparser.common.XmlViolationPolicy
import nu.validator.htmlparser.sax.HtmlParser
import org.xml.sax.*
import org.xml.sax.ext.LexicalHandler
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import javax.xml.transform.sax.SAXSource

class BasicDocumentLoader(val href: URI?,
                          val processor: Processor,
                          val documentManager: DocumentManager? = null,
                          initialProperties: DocumentProperties = DocumentProperties(),
                          val parameters: Map<QName,XdmValue> = mapOf()) {
    companion object {
        private val FF = (-1).toByte()
        private val FE = (-2).toByte()

        fun readTextStream(stream: InputStream, suppliedCharset: Charset?): String {
            val bytes = stream.readAllBytes()

            val charset = suppliedCharset
                ?: if (bytes.size < 2) {
                    StandardCharsets.UTF_8
                } else {
                    if (bytes[0] == FF && bytes[1] == FE) {
                        StandardCharsets.UTF_16LE
                    } else if (bytes[0] == FE && bytes[1] == FF) {
                        StandardCharsets.UTF_16BE
                    } else {
                        StandardCharsets.UTF_8
                    }
                }

            val chars = charset.decode(ByteBuffer.wrap(bytes))

            val sb = StringBuilder()
            when (charset) {
                StandardCharsets.UTF_8, StandardCharsets.UTF_16,
                StandardCharsets.UTF_16LE, StandardCharsets.UTF_16BE, -> {
                    if (chars.length > 0 && chars[0] == '\uFEFF') {
                        sb.append(chars, 1, chars.remaining())
                    } else {
                        sb.append(chars)
                    }
                }
                else -> {
                    sb.append(chars)
                }
            }

            return sb.toString()
        }

        val lineNumberRegex = "^.*input on line (\\d+).*$".toRegex()
        fun lineNumber(msg: String?): Int {
            val matches = lineNumberRegex.matchEntire(msg ?: "")
            if (matches != null) {
                return matches.groupValues[1].toInt()
            }
            return -1
        }

        // This seems marginally better than parsing the message with a regex.
        // I don't know if it actually is.
        fun lineAndColumn(ex: Throwable?): Pair<Int,Int> {
            if (ex != null) {
                if (ex is SAXParseException) {
                    return Pair(ex.lineNumber, ex.columnNumber)
                }

                if (ex is SaxonApiException && ex != ex.cause) {
                    return lineAndColumn(ex.cause)
                }

                if (ex is XPathException && ex != ex.cause) {
                    return lineAndColumn(ex.cause)
                }
            }

            return Pair(-1, -1)
        }
    }

    var readExternalSubset = true
    val properties = DocumentProperties()
    var mediaType: MediaType

    init {
        properties.setAll(initialProperties)

        if (properties.has(Ns.contentType)) {
            mediaType = MediaType.parse(properties[Ns.contentType]!!.underlyingValue.stringValue)
            properties[Ns.encoding]?.let { mediaType = mediaType.withParam("charset", it.toString()) }
        } else {
            val fileMediaType = if (href != null && documentManager != null) {
                documentManager.mimetypesFileTypeMap.getContentType(href.toString())
            } else {
                "application/xml"
            }
            mediaType = MediaType.parse(fileMediaType)
        }

        if (properties[Ns.baseUri] == null && href != null) {
            if (href.isAbsolute) {
                properties[Ns.baseUri] = href
            } else {
                properties[Ns.baseUri] = UriUtils.cwdAsUri().resolve(href)
            }
        }
    }

    fun load(stream: InputStream, overrideMediaType: MediaType? = null, overrideCharset: Charset? = null): XProcDocument {
        // If we got here via the InlineInstruction, we may not have started with load(), so we
        // need to make sure that the absURI (which will be used for the base URI of the document)
        // is correct.

        overrideMediaType?.let { mediaType = it }

        var charset = mediaType.charset()
        overrideCharset?.let { charset = it }

        properties[Ns.contentType] = mediaType

        val classification = mediaType.classification()

        val doc = when (classification) {
            MediaClassification.XML, MediaClassification.XHTML -> {
                try {
                    loadXml(href, stream)
                } catch (ex: SaxonApiException) {
                    // This is a hack, but I don't see a better way
                    val lineCol = lineAndColumn(ex)
                    val err = XProcError.xdNotWellFormed().atInput(Location(href, lineCol.first, lineCol.second))
                    throw err.exception(ex)
                }
            }
            MediaClassification.HTML -> loadHtml(href, stream, charset)
            MediaClassification.JSON -> loadJson(stream)
            MediaClassification.TEXT -> loadText(stream, charset)
            // I'm not sure what to do with CSV. Maybe wait for XPath 4?
            // MediaType.CSV -> loadCsv(stream)
            MediaClassification.YAML -> loadYaml(stream)
            MediaClassification.TOML -> loadToml(stream)
            else -> loadBinary(stream)
        }

        return doc
    }

    private fun loadXml(uri: URI?, stream: InputStream): XProcDocument {
        val saveParseOptions = processor.underlyingConfiguration.parseOptions
        val errorHandler = LoaderErrorHandler()

        // Make the cx:line-numbering parameter take precedence over the configuration
        val parseOptions = saveParseOptions.withErrorHandler(errorHandler)
        val numbering = parameters[NsCx.lineNumbering]?.underlyingValue?.effectiveBooleanValue()
            ?: parseOptions.isLineNumbering

        synchronized(processor.underlyingConfiguration) {
            processor.underlyingConfiguration.parseOptions = parseOptions.withLineNumbering(numbering)

            try {
                val builder = processor.newDocumentBuilder()
                builder.isLineNumbering = numbering

                val validating = if (parameters[Ns.dtdValidate] != null) {
                    val value = parameters[Ns.dtdValidate]!!.underlyingValue
                    if (value is BooleanValue) {
                        value.booleanValue
                    } else {
                        // FIXME: this isn't testing for only true/false
                        value.stringValue == "true"
                    }
                } else {
                    false
                }

                builder.isDTDValidation = validating
                if (!validating && !readExternalSubset) {
                    val cfg = processor.underlyingConfiguration
                    cfg.parseOptions = cfg.parseOptions.withParserFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
                }

                val source = InputSource(stream)
                if (uri != null) {
                    source.systemId = uri.toString();
                }

                val xdm = builder.build(SAXSource(source))
                if (errorHandler.errorCount > 0) {
                    val error = if (validating) {
                        XProcError.xdNotDtdValid(errorHandler.messages.first())
                    } else {
                        if (href != null) {
                            XProcError.xdNotWellFormed(href)
                        } else {
                            XProcError.xdNotWellFormed()
                        }
                    }
                    val location = Location(href, errorHandler.positions.first().first, errorHandler.positions.first().second)
                    throw error.atInput(location).exception()
                }
                return XProcDocument.ofXml(xdm, DocumentContextImpl(xdm), properties)
            } finally {
                processor.underlyingConfiguration.parseOptions = saveParseOptions
            }
        }
    }

    private fun loadHtml(uri: URI?, stream: InputStream, charset: Charset?): XProcDocument {
        val builder = processor.newDocumentBuilder()
        builder.isLineNumbering = parameters[NsCx.lineNumbering]?.underlyingValue?.effectiveBooleanValue() ?: false
        uri?.let { builder.baseURI = it }

        val treeBuilder = SaxonTreeBuilder(processor)
        val contentHandler = HtmlContentHandler(treeBuilder)

        val parser = HtmlParser(XmlViolationPolicy.ALTER_INFOSET)
        parser.contentHandler = contentHandler
        parser.lexicalHandler = contentHandler

        val source = InputSource(stream)
        source.systemId = uri.toString();
        charset?.let { source.encoding = it.toString() }

        parser.parse(source)

        val xdm = treeBuilder.result
        return XProcDocument.ofXml(xdm, DocumentContextImpl(xdm), properties)
    }

    private fun loadJson(stream: InputStream): XProcDocument {
        val compiler = processor.newXPathCompiler()
        compiler.declareVariable(QName("a"))
        compiler.declareVariable(QName("opt"))
        val selector = compiler.compile("parse-json(\$a, \$opt)").load()
        if (documentManager != null) {
            selector.resourceResolver = documentManager
        }
        val inputjson = loadTextData(stream, StandardCharsets.UTF_8)
        selector.setVariable(QName("a"), XdmAtomicValue(inputjson))

        // Our parameters map has QName keys, but the options map has string keys
        var optmap = XdmMap()
        for ((key, value) in parameters) {
            if (key.namespaceUri == NamespaceUri.NULL) {
                optmap = optmap.put(XdmAtomicValue(key.localName), value)
            }
        }
        selector.setVariable(QName("opt"), optmap)

        try {
            val json = selector.evaluate()
            return XProcDocument(json, DocumentContextImpl(processor), properties)
        } catch (ex: SaxonApiException) {
            if ((ex.message ?: "").startsWith("Invalid option")) {
                throw XProcError.xdInvalidParameter(ex.message!!).exception(ex)
            }

            val pos = (ex.message ?: "").indexOf("Duplicate key")
            if (pos >= 0) {
                val epos = ex.message!!.indexOf("}")
                val key = ex.message!!.substring(pos+21, epos+1)

                // This is a hack, but I don't see a better way
                val line = lineNumber(ex.message)
                val err = XProcError.xdDuplicateKey(key).atInput(Location(href, line, -1))
                throw err.exception(ex)
            }

            if ((ex.message ?: "").startsWith("Invalid JSON")) {
                throw XProcError.xdNotWellFormedJson(inputjson).exception(ex)
            }

            throw ex
        }
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun loadYaml(stream: InputStream): XProcDocument {
        val yamlReader = ObjectMapper(YAMLFactory())
        val obj = yamlReader.readValue(stream, Object::class.java)
        val jsonWriter = ObjectMapper()
        val str = jsonWriter.writeValueAsString(obj)
        // Hack
        return loadJson(ByteArrayInputStream(str.toByteArray(StandardCharsets.UTF_8)))
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun loadToml(stream: InputStream): XProcDocument {
        val tomlReader = ObjectMapper(TomlFactory())
        val obj = tomlReader.readValue(stream, Object::class.java)
        val jsonWriter = ObjectMapper()
        val str = jsonWriter.writeValueAsString(obj)
        // Hack
        return loadJson(ByteArrayInputStream(str.toByteArray(StandardCharsets.UTF_8)))
    }

    private fun loadCsv(stream: InputStream): XProcDocument {
        throw RuntimeException("Bang")
    }

    private fun loadText(stream: InputStream, charset: Charset?): XProcDocument {
        val builder = SaxonTreeBuilder(processor)
        builder.startDocument(href)
        builder.addText(loadTextData(stream, charset))
        builder.endDocument()
        val result = builder.result
        return XProcDocument.ofXml(result, DocumentContextImpl(result), properties)
    }

    private fun loadBinary(stream: InputStream): XProcDocument {
        val baos = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        var len = stream.read(buf)
        while (len >= 0) {
            baos.write(buf, 0, len)
            len = stream.read(buf)
        }
        val doc = XProcDocument.ofBinary(baos.toByteArray(), DocumentContextImpl(processor), properties)
        return doc
    }

    private fun loadTextData(stream: InputStream, inputCharset: Charset?): String {
        val suppliedCharset =  mediaType.charset() ?: inputCharset
        return readTextStream(stream, suppliedCharset)
    }

    private class LoaderErrorHandler(): ErrorHandler {
        val messages = mutableListOf<String>()
        val positions = mutableListOf<Pair<Int, Int>>()
        val errorCount: Int
            get() = messages.size

        override fun warning(exception: SAXParseException?) {
            // nop
        }

        override fun error(exception: SAXParseException?) {
            messages.add(exception?.message ?: "Unknown error")
            positions.add(Pair(exception?.lineNumber ?: -1, exception?.columnNumber ?: -1))
        }

        override fun fatalError(exception: SAXParseException?) {
            messages.add(exception?.message ?: "Unknown error")
            positions.add(Pair(exception?.lineNumber ?: -1, exception?.columnNumber ?: -1))
        }
    }

    private inner class HtmlContentHandler(val builder: SaxonTreeBuilder): ContentHandler, LexicalHandler {
        val options = if (NsCx.xmlAttributes in parameters) {
            val map = mutableMapOf<String, XdmValue>()
            for ((key, value) in TypeUtils.asGenericMap(parameters[NsCx.xmlAttributes] as XdmMap)) {
                map[key.underlyingValue.stringValue] = value
            }
            map
        } else {
            emptyMap()
        }
        var location: Locator? = null
        var namespaces: NamespaceMap = NamespaceMap.emptyMap()

        override fun setDocumentLocator(locator: Locator?) {
            location = locator
        }

        override fun startDocument() {
            var baseUri: URI? = null
            location?.systemId?.let { baseUri = URI(it) }
            builder.startDocument(baseUri)

            // Make sure HTML output is always in the HTML namespace by default
            namespaces = namespaces.put("", NsHtml.namespace)
        }

        override fun endDocument() {
            builder.endDocument()
        }

        override fun startPrefixMapping(prefix: String?, uri: String?) {
            namespaces = namespaces.put(prefix ?: "", NamespaceUri.of(uri ?: ""))
        }

        override fun endPrefixMapping(prefix: String?) {
            namespaces = namespaces.remove(prefix ?: "")
        }

        override fun startElement(uri: String?, localName: String?, qName: String?, atts: Attributes?) {
            val nodeName = QName(NamespaceUri.of(uri ?: ""), qName)
            var attributes: AttributeMap = EmptyAttributeMap.getInstance()

            if (atts != null) {
                for (pos in 0 until atts.length) {
                    val ns = atts.getURI(pos)
                    val local = atts.getLocalName(pos)
                    val qname = atts.getQName(pos)
                    val value = atts.getValue(pos)
                    if (local.startsWith("xmlU00003A")) {
                        val realName = local.substring(10)
                        if (realName in options) {
                            if (options[realName]!! == XdmEmptySequence.getInstance()) {
                                // discard this attribute
                            } else {
                                val mapping = options[realName]!!.underlyingValue.stringValue
                                if (mapping.startsWith("xml:")) {
                                    val local = mapping.substring(4)
                                    if (local == "" || ":" in local) {
                                        throw IllegalArgumentException("Invalid attribute name: ${mapping}")
                                    }
                                    attributes = attributes.put(TypeUtils.attributeInfo(QName(NsXml.namespace, mapping), value))
                                } else {
                                    if (mapping == "" || ":" in mapping) {
                                        throw IllegalArgumentException("Invalid attribute name: ${mapping}")
                                    }
                                    attributes = attributes.put(TypeUtils.attributeInfo(QName(NamespaceUri.NULL, mapping), value))
                                }
                            }
                        } else {
                            attributes = attributes.put(TypeUtils.attributeInfo(QName(NsXml.namespace, "xml:${realName}"), value))
                        }
                    } else {
                        attributes = attributes.put(TypeUtils.attributeInfo(QName(NamespaceUri.of(ns), qname), value))
                    }
                }
            }

            builder.setSaxLocation(location)
            builder.addStartElement(nodeName, attributes, namespaces)
        }

        override fun endElement(uri: String?, localName: String?, qName: String?) {
            builder.addEndElement()
        }

        override fun characters(ch: CharArray?, start: Int, length: Int) {
            if (ch != null) {
                builder.addText(String(ch, start, length))
            }
        }

        override fun ignorableWhitespace(ch: CharArray?, start: Int, length: Int) {
            characters(ch, start, length)
        }

        override fun processingInstruction(target: String?, data: String?) {
            if (target != null) {
                builder.addPI(target, data ?: "", location?.systemId)
            }
        }

        override fun skippedEntity(name: String?) {
            // nop?
        }

        override fun startDTD(name: String?, publicId: String?, systemId: String?) {
            // nop
        }

        override fun endDTD() {
            // nop
        }

        override fun startEntity(name: String?) {
            // nop
        }

        override fun endEntity(name: String?) {
            // nop
        }

        override fun startCDATA() {
            // nop
        }

        override fun endCDATA() {
            // nop
        }

        override fun comment(ch: CharArray?, start: Int, length: Int) {
            if (ch != null) {
                builder.addComment(String(ch, start, length))
            }
        }
    }

    private class SaxLocation(locator: Locator?): net.sf.saxon.s9api.Location {
        val locatorSystemId = locator?.getSystemId()
        val locatorPublicId = locator?.getPublicId()
        val locatorLineNumber = locator?.getLineNumber() ?: -1
        val locatorColumnNumber = locator?.getColumnNumber() ?: -1

        override fun getSystemId(): String? {
            return locatorSystemId
        }

        override fun getPublicId(): String? {
            return locatorPublicId
        }

        override fun getLineNumber(): Int {
            return locatorLineNumber
        }

        override fun getColumnNumber(): Int {
            return locatorColumnNumber
        }

        override fun saveLocation(): net.sf.saxon.s9api.Location? {
            // nop; not used here
            return this
        }
    }
}