package com.xmlcalabash.config

import com.xmlcalabash.XmlCalabashBuilder
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.io.BasicDocumentLoader
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.util.*
import net.sf.saxon.lib.FeatureIndex
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import org.apache.logging.log4j.kotlin.logger
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.net.URI
import java.util.*
import javax.xml.transform.sax.SAXSource

class ConfigurationLoader() {
    companion object {
        val ns = NamespaceUri.of("https://xmlcalabash.com/ns/configuration")
        val ccXmlCalabash = QName(ns, "cc:xml-calabash")
        val ccSystemProperty = QName(ns, "cc:system-property")
        val ccProxy = QName(ns, "cc:proxy")
        val ccThreading = QName(ns, "cc:threading")
        val ccInline = QName(ns, "cc:inline")
        val ccGraphviz = QName(ns, "cc:graphviz")
        val ccSaxonConfigurationProperty = QName(ns, "cc:saxon-configuration-property")
        val ccSerialization = QName(ns, "cc:serialization")
        val ccMimetype = QName(ns, "cc:mimetype")
        val ccSendmail = QName(ns, "cc:send-mail")
        val ccPagedMedia = QName(ns, "cc:paged-media")
        val ccVisualizer = QName(ns, "cc:visualizer")
        val ccMessageReporter = QName(ns, "cc:message-reporter")
        val ccXmlSchema = QName(ns, "cc:xml-schema")
        val ccCatalog = QName(ns, "cc:catalog")
        val ccExtension = QName(ns, "cc:extension")
        val ccXQueryProcessor = QName(ns, "cc:xquery-processor")
        val ccPipeline = QName(ns, "cc:pipeline")
        val ccInput = QName(ns, "cc:input")
        val ccOutput = QName(ns, "cc:output")
        val ccOption = QName(ns, "cc:option")
        val ccManifest = QName(ns, "cc:manifest")
        val ccInputMultiplex = QName(ns, "cc:input-multiplex")
        val ccOutputMultiplex = QName(ns, "cc:output-multiplex")
        val ccNamespace = QName(ns, "cc:namespace")
        val ccFallback = QName(ns, "cc:fallback")
        val ccInitializer = QName(ns, "cc:initializer")

        private val _class = QName("class")
        private val _count = QName("count")
        private val _cssFormatter = QName("css-formatter")
        private val _dot = QName("dot")
        private val _style = QName("style")
        private val _extensions = QName("extensions")
        private val _filespec = QName("filespec")
        private val _ignoreErrors = QName("ignore-errors")
        private val _licensed = QName("licensed")
        private val _lineNumbering = QName("line-numbering")
        private val _mpt = QName("mpt")
        private val _output = QName("output")
        private val _piped_io = QName("piped-io")
        private val _mime_multipart = QName("mime-multipart")
        private val _stacktrace = QName("stacktrace")
        private val _saxonConfiguration = QName("saxon-configuration")
        private val _scheme = QName("scheme")
        private val _trimWhitespace = QName("trim-whitespace")
        private val _uri = QName("uri")
        private val _value = QName("value")
        private val _verbosity = QName("verbosity")
        private val _xslFormatter = QName("xsl-formatter")
        private val _bufferSize = QName("buffer-size")
        private val _defaultXQueryProcessor = QName("default-xquery-processor")
    }

    private lateinit var configFile: String
    private lateinit var builder: XmlCalabashBuilder

    fun load(source: File): XmlCalabashBuilder {
        logger.info { "Loading XML Calabash configuration ${source.absoluteFile}" }
        val isrc = InputSource(FileInputStream(source))
        isrc.systemId = source.toURI().toString()
        return load(isrc)
    }

    fun load(source: URI): XmlCalabashBuilder {
        logger.info { "Loading XML Calabash configuration ${source}" }
        val isrc = InputSource(source.toString())
        return load(isrc)
    }

    fun load(source: InputSource): XmlCalabashBuilder {
        configFile = source.systemId ?: ""

        val processor = Processor(false)
        val dbuilder = processor.newDocumentBuilder()
        val destination = XdmDestination()
        dbuilder.parse(SAXSource(source), destination)

        val root = S9Api.documentElement(destination.xdmNode)
        if (root.nodeName != ccXmlCalabash) {
            throw XProcError.xiConfigurationInvalid(configFile).exception()
        }

        synchronized(Companion) {
            builder = XmlCalabashBuilder()
            parse(root)
            return builder
        }
    }

    private fun parse(root: XdmNode) {
        checkAttributes(root, listOf(), listOf(
            _defaultXQueryProcessor,
            _licensed,
            _lineNumbering,
            _mpt,
            _piped_io,
            _mime_multipart,
            _saxonConfiguration,
            _stacktrace,
            Ns.tryNamespaces,
            Ns.useLocationHints,
            Ns.validationMode,
            _verbosity,
            Ns.version))

        if ((root.getAttributeValue(Ns.version) ?: "1.0") != "1.0") {
            throw XProcError.xiInvalidConfigurationAttributeValue(root.nodeName, Ns.version, root.getAttributeValue(Ns.version)!!).exception()
        }

        val saxonConfig = root.getAttributeValue(_saxonConfiguration)
        if (saxonConfig != null) {
            val uri = UriUtils.resolve(root.baseURI, saxonConfig)!!
            builder.saxonConfigurationFile.set(File(UriUtils.path(uri)))
        }

        if (root.getAttributeValue(_mpt) != null) {
            try {
                builder.mpt.set(root.getAttributeValue(_mpt).toDouble())
            } catch (_: NumberFormatException) {
                // nevermind, it's not important
                logger.debug { "mpt must be a number: ${root.getAttributeValue(_mpt)}"}
            }
        }

        root.getAttributeValue(_licensed)?.let { builder.licensed.set(booleanAttribute(it, "licensed")) }
        root.getAttributeValue(_lineNumbering)?.let { builder.lineNumbering.set(booleanAttribute(it, "lineNumbering")) }
        root.getAttributeValue(_piped_io)?.let { builder.pipedMode.set(booleanAttribute(it, "piped-io")) }
        root.getAttributeValue(_stacktrace)?.let { builder.stacktrace.set(booleanAttribute(it, "stacktrace")) }
        root.getAttributeValue(_verbosity)?.let { builder.verbosity.set(verbosityAttribute(it)) }

        when (root.getAttributeValue(Ns.validationMode)) {
            null -> Unit
            "strict" -> builder.validationMode.set(ValidationMode.STRICT)
            "lax" -> builder.validationMode.set(ValidationMode.LAX)
            else -> throw XProcError.xiConfigurationInvalid(configFile,
                "validation mode: ${root.getAttributeValue(Ns.validationMode)}").exception()
        }

        root.getAttributeValue(Ns.tryNamespaces)?.let { builder.tryNamespaces.set(booleanAttribute(it, "try-namespaces")) }
        root.getAttributeValue(Ns.useLocationHints)?.let { builder.useLocationHints.set(booleanAttribute(it, "use-location-hints")) }
        root.getAttributeValue(_defaultXQueryProcessor)?.let { builder.defaultXQueryProcessor.set(URI(it)) }

        for (child in root.axisIterator(Axis.CHILD)) {
            when (child.nodeKind) {
                XdmNodeKind.ELEMENT -> {
                    when (child.nodeName) {
                        ccSystemProperty -> parseSystemProperty(child)
                        ccProxy -> parseProxy(child)
                        ccThreading -> parseThreading(child)
                        ccInitializer -> parseInitializer(child)
                        ccInline -> parseInline(child)
                        ccGraphviz -> parseGraphviz(child)
                        ccSaxonConfigurationProperty -> parseSaxonConfigurationProperty(child)
                        ccSerialization -> parseSerialization(child)
                        ccMimetype -> parseMimetype(child)
                        ccSendmail -> parseSendmail(child)
                        ccPagedMedia -> parsePagedMedia(child)
                        ccMessageReporter -> parseMessageReporter(child)
                        ccVisualizer -> parseVisualizer(child)
                        ccXmlSchema -> parseXmlSchema(child)
                        ccCatalog -> parseCatalog(child)
                        ccExtension -> parseExtension(child)
                        ccXQueryProcessor -> parseXQueryImplementation(child)
                        ccNamespace -> parseNamespace(child)
                        ccPipeline -> parsePipeline(child)
                        else -> {
                            if (child.nodeName.namespaceUri == ns) {
                                throw XProcError.xiUnrecognizedConfigurationProperty(child.nodeName).exception()
                            }
                            parseOther(child)
                        }
                    }
                }
                XdmNodeKind.TEXT -> {
                    val text = child.underlyingValue.stringValue
                    if (text.isNotBlank()) {
                        throw XProcError.xiConfigurationInvalid(configFile, "text is not allowed: ${text}").exception()
                    }
                }
                else -> Unit
            }
        }
    }

    private fun booleanAttribute(value: String?, name: String): Boolean {
        if (value != null && value != "true" && value != "false") {
            throw XProcError.xiConfigurationInvalid(configFile, "invalid ${name} setting: ${value}").exception()
        }
        return value == "true"
    }

    private fun verbosityAttribute(value: String?): Verbosity {
        return when (value) {
            null -> Verbosity.INFO
            "error" -> Verbosity.ERROR
            "warn" -> Verbosity.WARN
            "info" -> Verbosity.INFO
            "debug" -> Verbosity.DEBUG
            "trace" -> Verbosity.TRACE
            else -> throw XProcError.xiConfigurationInvalid(configFile, "invalid verbose setting: ${value}").exception()
        }
    }

    private fun parseSystemProperty(node: XdmNode) {
        checkAttributes(node, listOf(Ns.name, _value))
        System.setProperty(node.getAttributeValue(Ns.name)!!, node.getAttributeValue(_value)!!)
    }

    private fun parseProxy(node: XdmNode) {
        checkAttributes(node, listOf(_scheme, _uri))
        builder.proxies.put(node.getAttributeValue(_scheme), node.getAttributeValue(_uri))
    }

    private fun parseThreading(node: XdmNode) {
        checkAttributes(node, emptyList(), listOf(_count))
        try {
            builder.maxThreadCount.set(node.getAttributeValue(_count)?.toInt() ?: Runtime.getRuntime().availableProcessors())
        } catch (_: NumberFormatException) {
            throw XProcError.xiInvalidSaxonConfigurationProperty("cc:threading", node.getAttributeValue(_count)!!).exception()
        }
    }

    private fun parseInitializer(node: XdmNode) {
        checkAttributes(node, listOf(_class), listOf(_ignoreErrors))
        val value = node.getAttributeValue(_class)!!
        val ignore = booleanAttribute(node.getAttributeValue(_ignoreErrors) ?: "true", "ignore-errors")
        builder.initializers.add(Pair(value, ignore))
    }

    private fun parseInline(node: XdmNode) {
        checkAttributes(node, listOf(_trimWhitespace))
        val value = node.getAttributeValue(_trimWhitespace)!!
        if (value == "true" || value == "false") {
            builder.inlineTrimWhitespace.set(value == "true")
        } else {
            throw XProcError.xiUnrecognizedConfigurationValue(node.nodeName, _trimWhitespace, value).exception()
        }
    }

    private fun parseGraphviz(node: XdmNode) {
        checkAttributes(node, listOf(_dot), listOf(_style, _output))
        val dot = File(node.getAttributeValue(_dot)!!)
        if (!dot.exists() || dot.isDirectory) {
            throw XProcError.xiCannotFindGraphviz(dot.absolutePath).exception()
        }
        if (!dot.canExecute()) {
            throw XProcError.xiCannotExecuteGraphviz(dot.absolutePath).exception()
        }
        builder.graphviz.set(dot)
        node.getAttributeValue(_style)?.let { builder.graphStyle.set(UriUtils.resolve(node.baseURI, it)) }

        if (node.getAttributeValue(_output) != null) {
            val output = File(node.getAttributeValue(_output)!!)
            if (output.exists() && !output.isDirectory) {
                throw XProcError.xiInvalidConfigurationAttributeValue(node.nodeName, _output, output.absolutePath).exception()
            }
            builder.graphs.set(output)
        }
    }

    private fun parseSaxonConfigurationProperty(node: XdmNode) {
        checkAttributes(node, listOf(Ns.name, _value))

        val key = node.getAttributeValue(Ns.name)!!
        val value = node.getAttributeValue(_value)!!
        val data = FeatureIndex.getData(key) ?: throw XProcError.xiUnrecognizedSaxonConfigurationProperty(key).exception()
        if (data.type == Boolean::class.java) {
            if (value == "true" || value == "false") {
                builder.saxonConfigurationProperties.put(key, value)
            } else {
                throw XProcError.xiInvalidSaxonConfigurationProperty(key, value).exception()
            }
        } else {
            builder.saxonConfigurationProperties.put(key, value)
        }
    }

    private fun parseSerialization(node: XdmNode) {
        if (node.getAttributeValue(Ns.contentType) == null) {
            throw XProcError.xiMissingConfigurationAttribute(node.nodeName, Ns.contentType).exception()
        }

        val ctype = MediaType.parse(node.getAttributeValue(Ns.contentType)) // Just for the side effect
        for (attr in node.axisIterator(Axis.ATTRIBUTE)) {
            if (attr.nodeName == Ns.contentType) {
                continue
            }

            // This seems a little clumsy
            val map = mutableMapOf<QName, String>()
            for ((key,value) in builder.serialization.get(ctype) ?: emptyMap()) {
                map[key] = value
            }
            map[attr.nodeName] = attr.stringValue
            builder.serialization.put(ctype, map)
        }
    }

    private fun parseMimetype(node: XdmNode) {
        checkAttributes(node, listOf(Ns.contentType, _extensions))
        val ctype = node.getAttributeValue(Ns.contentType)!!
        val ext = node.getAttributeValue(_extensions)!!
        builder.mimeTypes.put(ctype, ext.split("\\s+".toRegex()))
    }

    private fun parseSendmail(node: XdmNode) {
        for (attr in node.axisIterator(Axis.ATTRIBUTE)) {
            if (attr.nodeName.namespaceUri == NamespaceUri.NULL) {
                builder.sendmail.put(attr.nodeName.localName, attr.underlyingNode.stringValue)
            }
        }
    }

    private fun parsePagedMedia(node: XdmNode) {
        var cssFormatter: URI? = null
        var xslFormatter: URI? = null
        val properties = mutableMapOf<QName,String>()
        for (attr in node.axisIterator(Axis.ATTRIBUTE)) {
            when (attr.nodeName) {
                _cssFormatter -> cssFormatter = pagedMediaProcessorUri("css-formatter", attr.stringValue)
                _xslFormatter -> xslFormatter = pagedMediaProcessorUri("xsl-formatter", attr.stringValue)
                else -> properties[attr.nodeName] = attr.stringValue
            }
        }

        if (cssFormatter == null && xslFormatter == null) {
            throw XProcError.xiMissingConfigurationAttributes(node.nodeName, "at least one of xsl-formatter or css-formatter is required").exception()
        }

        if (cssFormatter != null) {
            builder.cssFormatter.put(cssFormatter, properties)
        }

        if (xslFormatter != null) {
            builder.xslFormatter.put(xslFormatter, properties)
        }
    }

    private fun pagedMediaProcessorUri(type: String, value: String?): URI? {
        if (value == null) {
            return null
        }
        if (value.contains("/")) {
            return URI.create(value)
        }
        return URI.create("https://xmlcalabash.com/paged-media/${type}/${value}")
    }

    private fun parseMessageReporter(node: XdmNode) {
        checkAttributes(node, emptyList(), listOf(_bufferSize))
        val size = node.getAttributeValue(_bufferSize)
        try {
            size?.let { builder.messageReporterBufferSize.set(it.toInt()) }
        } catch (_: NumberFormatException) {
            throw XProcError.xiInvalidConfigurationAttributeValue(node.getNodeName(), _bufferSize, size).exception()
        }
    }

    private fun parseVisualizer(node: XdmNode) {
        val value = node.getAttributeValue(Ns.name)
        val options = mutableMapOf<String, String>()
        for (attr in node.axisIterator(Axis.ATTRIBUTE)) {
            if (attr.nodeName != Ns.name && attr.nodeName.namespaceUri == NamespaceUri.NULL) {
                options[attr.nodeName.localName] = attr.stringValue
            }
        }

        if (value != null) {
            builder.visualizerName.set(value)
            if (options.isNotEmpty()) {
                builder.visualizerName.options = mutableMapOf()
                builder.visualizerName.options!!.putAll(options)
            }
        }
    }

    private fun parseOther(node: XdmNode) {
        val map = mutableMapOf<QName,String>()
        for (attr in node.axisIterator(Axis.ATTRIBUTE)) {
            map[attr.nodeName] = attr.stringValue
        }
        val list = mutableListOf<Map<QName, String>>()
        builder.other.get(node.nodeName)?.let { list.addAll(it) }
        list.add(map)
        builder.other.put(node.nodeName, list)
    }

    private fun parseXmlSchema(node: XdmNode) {
        checkAttributes(node, listOf(Ns.href))
        builder.xmlSchemas.add(UriUtils.resolve(node.baseURI, node.getAttributeValue(Ns.href))!!)
        if (node.children().firstOrNull() != null) {
            throw XProcError.xiConfigurationXmlSchemaElementMustBeEmpty().exception()
        }
    }

    private fun parseCatalog(node: XdmNode) {
        checkAttributes(node, listOf(Ns.href))
        builder.xmlCatalogs.add(UriUtils.resolve(node.baseURI, node.getAttributeValue(Ns.href))!!)
        if (node.children().firstOrNull() != null) {
            throw XProcError.xiConfigurationCatalogElementMustBeEmpty().exception()
        }
    }

    private fun parseExtension(node: XdmNode) {
        checkAttributes(node, listOf(Ns.name))
        val name = node.getAttributeValue(Ns.name)!!
        when (name) {
            "eager-uri-resolution" -> {
                val list = builder.extensions.get() ?: emptyList()
                if (!list.contains(ExtensionName.EAGER_URI_RESOLUTION)) {
                    builder.extensions.add(ExtensionName.EAGER_URI_RESOLUTION)
                }
            }
            "ignore-invalid-uris" -> {
                val list = builder.extensions.get() ?: emptyList()
                if (!list.contains(ExtensionName.IGNORE_INVALID_URIS)) {
                    builder.extensions.add(ExtensionName.IGNORE_INVALID_URIS)
                }
            }
            else -> throw XProcError.xiUnrecognizedExtension(name).exception()
        }
    }

    private fun parseXQueryImplementation(node: XdmNode) {
        val name =
            node.getAttributeValue(Ns.name)
                ?: throw XProcError.xiMissingConfigurationAttribute(node.nodeName, Ns.name).exception()

        val impl = node.baseURI.resolve(name)

        val properties = mutableMapOf<QName,String>()
        for (attr in node.axisIterator(Axis.ATTRIBUTE)) {
            if (attr.nodeName != Ns.name) {
                if (attr.nodeName.namespaceUri == ns) {
                    if (attr.nodeName == ccFallback) {
                        properties[attr.nodeName] = attr.stringValue
                    } else {
                        throw XProcError.xiUnrecognizedConfigurationAttribute(node.nodeName, attr.nodeName).exception()
                    }
                } else {
                    properties[attr.nodeName] = attr.stringValue
                }
            }
        }

        builder.configuredXQueryProcessors.put(impl, properties)
    }

    private fun parseNamespace(node: XdmNode) {
        checkAttributes(node, listOf(Ns.prefix, _uri))
        val prefix = node.getAttributeValue(Ns.prefix)!!
        val uri = NamespaceUri.of(node.getAttributeValue(_uri))
        builder.namespaces.put(prefix, uri)
    }

    private fun parsePipeline(node: XdmNode) {
        checkAttributes(node, listOf(Ns.href), listOf(Ns.step, Ns.temporaryFiles))
        val href = node.baseURI.resolve(node.getAttributeValue(Ns.href)!!)
        builder.pipelineUri.set(href)
        node.getAttributeValue(Ns.step)?.let { builder.step.set(it) }
        node.getAttributeValue(Ns.temporaryFiles)?.let { builder.temporaryFiles.set(it) }

        if (builder.command.getOrDefault() == null) {
            builder.command.set("run")
        }

        for (child in elementChildren(node)) {
            when (child.nodeName) {
                ccInput -> parseInput(child)
                ccOutput -> parseOutput(child)
                ccOption -> parseOption(child)
                ccManifest -> parseManifest(child)
                ccInputMultiplex -> parseInputMultiplex(child)
                ccOutputMultiplex -> parseOutputMultiplex(child)
                else -> {
                    throw XProcError.xiUnrecognizedConfigurationProperty(child.nodeName).exception()
                }
            }
        }
    }

    private fun parseInput(node: XdmNode) {
        checkAttributes(node, listOf(Ns.port), listOf(Ns.href, Ns.contentType, Ns.encoding))

        val port = node.getAttributeValue(Ns.port)!!

        var contentType = MediaType.ANY
        node.getAttributeValue(Ns.contentType)?.let { contentType = MediaType.parse(it) }

        val encoding = node.getAttributeValue(Ns.encoding)

        if (node.getAttributeValue(Ns.href) != null) {
            val href = node.baseURI.resolve(node.getAttributeValue(Ns.href)!!)
            node.getAttributeValue(Ns.encoding)?.let { throw XProcError.xiConfigurationInvalid(configFile,
                "An encoding cannot be specified for an input href").exception()
            }
            if (elementChildren(node).isNotEmpty()) {
                throw XProcError.xiConfigurationInvalid(configFile, "Content cannot be specified for an input href").exception()
            }

            builder.inputs.add(XmlCalabashInput(port, href, contentType))
        } else {
            when (encoding) {
                null -> {
                    val xinput = XmlCalabashInput(port, null, contentType)
                    xinput.doc = parseInlineContent(node, contentType)
                    builder.inputs.add(xinput)
                }
                "base64" -> {
                    for (child in node.children()) {
                        when (child.nodeKind) {
                            XdmNodeKind.PROCESSING_INSTRUCTION, XdmNodeKind.COMMENT, XdmNodeKind.ELEMENT -> {
                                throw XProcError.xiConfigurationInvalid(configFile, "Markup not allowed with ${encoding} encoding").exception()
                            }
                            else -> Unit
                        }
                    }

                    // Assume whitespace is not part of the encoding (it's not part of base64)
                    val cleanText = node.stringValue.replace("\\s+".toRegex(), "")
                    try {
                        val xinput = XmlCalabashInput(port, null, contentType)

                        val decoder = Base64.getDecoder()
                        val bytes = decoder.decode(cleanText)

                        if (contentType.classification() == MediaClassification.TEXT) {
                            contentType.charset()
                        }

                        val loader = BasicDocumentLoader(null, node.processor)
                        val bais = ByteArrayInputStream(bytes)
                        xinput.doc = loader.load(bais, contentType, contentType.charset())

                        builder.inputs.add(xinput)
                    } catch (ex: IllegalArgumentException) {
                        throw XProcException(XProcError.xdBadBase64Input(), ex)
                    }
                }
                else -> {
                    throw XProcError.xiConfigurationInvalid(configFile, "Invalid encoding: ${encoding}").exception()
                }
            }
        }
    }

    private fun parseOutput(node: XdmNode) {
        checkAttributes(node, listOf(Ns.port, _filespec))

        val port = node.getAttributeValue(Ns.port)!!
        val filespec = node.getAttributeValue(_filespec)!!

        builder.outputs.add(XmlCalabashOutput(port, filespec))
    }

    private fun parseManifest(node: XdmNode) {
        checkAttributes(node, listOf(Ns.href))

        val href = node.getAttributeValue(Ns.href)!!

        builder.manifest.set(XmlCalabashOutput(null, href))
    }

    private fun parseInputMultiplex(node: XdmNode) {
        checkAttributes(node, listOf(Ns.href))

        val uri = node.getAttributeValue(Ns.href)!!
        val pos = uri.indexOf("?")

        val href = when (pos) {
            -1 -> uri
            0 -> throw XProcError.xiConfigurationInvalid(configFile, "Input multiplex must identify a location: ${uri}").exception()
            else -> uri.substring(0, pos)
        }

        // - and the stdio URI are constants in CommandLine but that's not available here; they should be put somewhere common
        val input = if (href == "-") {
            XmlCalabashInput(null, URI("https://xmlcalabash.com/ns/stdio"), MediaType.MULTIPART_MIXED)
        } else {
            XmlCalabashInput(null, UriUtils.resolve(href), MediaType.MULTIPART_MIXED)
        }

        input.multiplex = true
        if (pos > 0) {
            // I think the user wants to write source=result, meaning that the source port should
            // come from the port labeled result. Of course, in reality, what I want in the mapping
            // is result=source, rename result to source...
            val maplist = uri.substring(pos+1).split(";")
            for (map in maplist) {
                if (map.trim().isEmpty()) {
                    continue
                }
                val mapping = map.split("=")
                if (mapping.size != 2 || mapping[0].isEmpty() || mapping[1].isEmpty()) {
                    throw XProcError.xiCliMalformedOption("input", uri).exception()
                }
                // ...that's why this is "backwards".
                input.multiplexMapping[mapping[1]] = mapping[0]
            }
        }

        builder.inputs.add(input)
    }

    private fun parseOutputMultiplex(node: XdmNode) {
        checkAttributes(node, listOf(Ns.href))

        val href = node.getAttributeValue(Ns.href)!!

        builder.outputs.add(XmlCalabashOutput(null, href, true, true))
    }

    private fun parseOption(node: XdmNode) {
        checkAttributes(node, listOf(Ns.name),
            listOf(Ns.value, Ns.select, Ns.contentType, Ns.encoding))

        val name = node.getAttributeValue(Ns.name)!!
        val value = node.getAttributeValue(Ns.value)
        val select = node.getAttributeValue(Ns.select)

        val children = mutableListOf<XdmNode>()
        for (child in node.children()) {
            children.add(child)
        }

        val list = mutableListOf<Any>()
        list.addAll(builder.options.get(name) ?: emptyList())

        if (value != null && select != null) {
            throw XProcError.xiConfigurationInvalid(configFile, "Only one of value or select is allowed").exception()
        }

        if (value == null && select == null && children.isEmpty()) {
            throw XProcError.xiConfigurationInvalid(configFile, "No value provided; use select='()' for empty sequence").exception()
        }

        if (value != null || select != null) {
            if (children.isNotEmpty()) {
                throw XProcError.xiConfigurationInvalid(configFile, "Only one of value or content is allowed").exception()
            }
            if (node.getAttributeValue(Ns.contentType) != null) {
                throw XProcError.xiConfigurationInvalid(configFile, "Content-type is not allowed with value").exception()
            }
            if (node.getAttributeValue(Ns.encoding) != null) {
                throw XProcError.xiConfigurationInvalid(configFile, "Encoding is not allowed with value").exception()
            }
            if (value != null) {
                list.add(value)
            } else {
                list.add("?${select}")
            }
            builder.options.put(name, list)
            return
        }

        var contentType = MediaType.ANY
        node.getAttributeValue(Ns.contentType)?.let { contentType = MediaType.parse(it) }

        val encoding = node.getAttributeValue(Ns.encoding)

        val doc = when (encoding) {
            null -> {
                parseInlineContent(node, contentType)
            }
            "base64" -> {
                for (child in node.children()) {
                    when (child.nodeKind) {
                        XdmNodeKind.PROCESSING_INSTRUCTION, XdmNodeKind.COMMENT, XdmNodeKind.ELEMENT -> {
                            throw XProcError.xiConfigurationInvalid(
                                configFile,
                                "Markup not allowed with ${encoding} encoding"
                            ).exception()
                        }

                        else -> Unit
                    }
                }

                // Assume whitespace is not part of the encoding (it's not part of base64)
                val cleanText = node.stringValue.replace("\\s+".toRegex(), "")
                try {
                    val decoder = Base64.getDecoder()
                    val bytes = decoder.decode(cleanText)

                    if (contentType.classification() == MediaClassification.TEXT) {
                        contentType.charset()
                    }

                    val loader = BasicDocumentLoader(null, node.processor)
                    val bais = ByteArrayInputStream(bytes)
                    loader.load(bais, contentType, contentType.charset())
                } catch (ex: IllegalArgumentException) {
                    throw XProcException(XProcError.xdBadBase64Input(), ex)
                }
            }
            else -> {
                throw XProcError.xiConfigurationInvalid(configFile, "Invalid encoding: ${encoding}").exception()
            }
        }

        list.add(doc.value)
        builder.options.put(name, list)
    }

    private fun parseInlineContent(node: XdmNode, contentType: MediaType): XProcDocument {
        // How much of a hack is this? A lot!
        val sb = StringBuilder()
        for (child in node.children()) {
            sb.append(child.toString())
        }

        val bais = ByteArrayInputStream(sb.toString().toByteArray())
        val loader = BasicDocumentLoader(null, node.processor)
        return loader.load(bais, contentType, contentType.charset())
    }

    private fun elementChildren(node: XdmNode): List<XdmNode> {
        val list = mutableListOf<XdmNode>()
        for (child in node.children()) {
            when (child.nodeKind) {
                XdmNodeKind.TEXT -> {
                    val text = child.underlyingValue.stringValue
                    if (text.isNotBlank()) {
                        throw XProcError.xiConfigurationInvalid(configFile, "text is not allowed: ${text}").exception()
                    }
                }
                XdmNodeKind.ELEMENT -> list.add(child)
                else -> Unit
            }
        }
        return list
    }

    private fun checkAttributes(node: XdmNode, attributes: List<QName>, optionalAttributes: List<QName> = listOf()) {
        val seen = mutableSetOf<QName>()
        for (attr in node.axisIterator(Axis.ATTRIBUTE)) {
            seen.add(attr.nodeName)
            if (!attributes.contains(attr.nodeName) && !optionalAttributes.contains(attr.nodeName)) {
                throw XProcError.xiUnrecognizedConfigurationAttribute(node.nodeName, attr.nodeName).exception()
            }
        }
        for (attr in attributes) {
            if (!seen.contains(attr)) {
                throw XProcError.xiMissingConfigurationAttribute(node.nodeName, attr).exception()
            }
        }
    }
}