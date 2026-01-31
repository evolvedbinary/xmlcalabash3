package com.xmlcalabash.io

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.xmlcalabash.datamodel.DocumentContextImpl
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsXml
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.SaxAttributes
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.TypeUtils
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import net.sf.saxon.value.BooleanValue
import nu.validator.htmlparser.common.XmlViolationPolicy
import nu.validator.htmlparser.sax.HtmlParser
import org.xml.sax.*
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
    }

    var readExternalSubset = true

    val properties = DocumentProperties()

    var mediaType = if (properties.has(Ns.contentType)) {
        MediaType.parse(properties[Ns.contentType]!!.underlyingValue.stringValue)
    } else {
        val fileMediaType = if (href != null && documentManager != null) {
            documentManager.mimetypesFileTypeMap.getContentType(href.toString())
        } else {
            "application/xml"
        }
        MediaType.parse(fileMediaType)
    }

    init {
        properties.setAll(initialProperties)
    }

    fun load(stream: InputStream, overrideMediaType: MediaType, charset: Charset? = null): XProcDocument {
        // If we got here via the InlineInstruction, we may not have started with load(), so we
        // need to make sure that the absURI (which will be used for the base URI of the document)
        // is correct.

        mediaType = overrideMediaType
        properties[Ns.contentType] = mediaType

        val classification = mediaType.classification()

        val doc = when (classification) {
            MediaClassification.XML, MediaClassification.XHTML -> {
                try {
                    loadXml(href, stream)
                } catch (ex: SaxonApiException) {
                    if (href != null) {
                        throw XProcError.xdNotWellFormed(href).exception(ex)
                    }
                    throw XProcError.xdNotWellFormed().exception(ex)
                }
            }
            MediaClassification.HTML -> loadHtml(href, stream)
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
                    if (validating) {
                        throw XProcError.xdNotDtdValid(errorHandler.message ?: "No message provided").exception()
                    }
                    if (href != null) {
                        throw XProcError.xdNotWellFormed(href).exception()
                    }
                    throw XProcError.xdNotWellFormed().exception()
                }
                return XProcDocument.ofXml(xdm, DocumentContextImpl(xdm), properties)
            } finally {
                processor.underlyingConfiguration.parseOptions = saveParseOptions
            }
        }
    }

    private fun loadHtml(uri: URI?, stream: InputStream): XProcDocument {
        val builder = processor.newDocumentBuilder()
        builder.isLineNumbering = parameters[NsCx.lineNumbering]?.underlyingValue?.effectiveBooleanValue() ?: false
        uri?.let { builder.baseURI = it }

        val xdmContentHandler = builder.newBuildingContentHandler()
        val contentHandler = HtmlContentHandler(xdmContentHandler)

        val parser = HtmlParser(XmlViolationPolicy.ALTER_INFOSET)
        parser.contentHandler = contentHandler

        val source = InputSource(stream)
        source.systemId = uri.toString();

        parser.parse(source)

        val xdm = xdmContentHandler.documentNode
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
                throw XProcError.xdDuplicateKey(key).exception(ex)
            }

            if ((ex.message ?: "").startsWith("Invalid JSON")) {
                throw XProcError.xdNotWellFormedJson(inputjson).exception(ex)
            }

            throw ex
        }
    }

    private fun loadYaml(stream: InputStream): XProcDocument {
        val yamlReader = ObjectMapper(YAMLFactory())
        val obj = yamlReader.readValue(stream, Object::class.java)
        val jsonWriter = ObjectMapper()
        val str = jsonWriter.writeValueAsString(obj)
        // Hack
        return loadJson(ByteArrayInputStream(str.toByteArray(StandardCharsets.UTF_8)))
    }

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
        var errorCount = 0
        var message: String? = null

        override fun warning(exception: SAXParseException?) {
            // nop
        }

        override fun error(exception: SAXParseException?) {
            if (message == null && exception?.message != null) {
                message = exception.message
            }
            errorCount++
        }

        override fun fatalError(exception: SAXParseException?) {
            if (message == null && exception?.message != null) {
                message = exception.message
            }
            errorCount++
        }
    }

    private inner class HtmlContentHandler(val handler: ContentHandler): ContentHandler {
        val options = if (NsCx.xmlAttributes in parameters) {
            val map = mutableMapOf<String, XdmValue>()
            for ((key, value) in TypeUtils.asGenericMap(parameters[NsCx.xmlAttributes] as XdmMap)) {
                map[key.underlyingValue.stringValue] = value
            }
            map
        } else {
            emptyMap()
        }

        override fun setDocumentLocator(locator: Locator?) {
            handler.setDocumentLocator(locator)
        }

        override fun startDocument() {
            handler.startDocument()
        }

        override fun endDocument() {
            handler.endDocument()
        }

        override fun startPrefixMapping(prefix: String?, uri: String?) {
            handler.startPrefixMapping(prefix, uri)
        }

        override fun endPrefixMapping(prefix: String?) {
            handler.endPrefixMapping(prefix)
        }

        override fun startElement(uri: String?, localName: String?, qName: String?, atts: Attributes?) {
            var newAtts: Attributes? = atts
            if (atts != null) {
                var copy = false
                for (pos in 0 until atts.length) {
                    val name = atts.getLocalName(pos)
                    if (name.startsWith("xmlU00003A")) {
                        copy = true
                        break;
                    }
                }
                if (copy) {
                    val saxAtts = SaxAttributes()
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
                                        saxAtts.addAttribute(NsXml.namespace.toString(), local, mapping, value)
                                    } else {
                                        if (mapping == "" || ":" in mapping) {
                                            throw IllegalArgumentException("Invalid attribute name: ${mapping}")
                                        }
                                        saxAtts.addAttribute("", mapping, mapping, value)
                                    }
                                }
                            } else {
                                saxAtts.addAttribute(NsXml.namespace.toString(), realName, "xml:${realName}", value)
                            }
                        } else {
                            saxAtts.addAttribute(ns, local, qname, value)
                        }
                    }
                    newAtts = saxAtts
                }
            }

            handler.startElement(uri, localName, qName, newAtts)
        }

        override fun endElement(uri: String?, localName: String?, qName: String?) {
            handler.endElement(uri, localName, qName)
        }

        override fun characters(ch: CharArray?, start: Int, length: Int) {
            handler.characters(ch, start, length)
        }

        override fun ignorableWhitespace(ch: CharArray?, start: Int, length: Int) {
            handler.ignorableWhitespace(ch, start, length)
        }

        override fun processingInstruction(target: String?, data: String?) {
            handler.processingInstruction(target, data)
        }

        override fun skippedEntity(name: String?) {
            handler.skippedEntity(name)
        }
    }
}