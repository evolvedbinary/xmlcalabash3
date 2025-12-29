package com.xmlcalabash

import com.xmlcalabash.api.MessageReporter
import com.xmlcalabash.config.CfgListValue
import com.xmlcalabash.config.CfgMapValue
import com.xmlcalabash.config.CfgValue
import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.config.XmlCalabashInput
import com.xmlcalabash.config.XmlCalabashOutput
import com.xmlcalabash.exceptions.DefaultErrorExplanation
import com.xmlcalabash.exceptions.ErrorExplanation
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentManager
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.io.MessagePrinter
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsFn
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.namespace.NsSaxon
import com.xmlcalabash.namespace.NsXml
import com.xmlcalabash.namespace.NsXs
import com.xmlcalabash.spi.Configurer
import com.xmlcalabash.spi.ConfigurerServiceProvider
import com.xmlcalabash.spi.DocumentResolverServiceProvider
import com.xmlcalabash.spi.PagedMediaManager
import com.xmlcalabash.spi.PagedMediaServiceProvider
import com.xmlcalabash.util.AssertionsLevel
import com.xmlcalabash.util.DefaultMessagePrinter
import com.xmlcalabash.util.DefaultMessageReporter
import com.xmlcalabash.util.ExtensionName
import com.xmlcalabash.util.Verbosity
import net.sf.saxon.Configuration
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.ValidationMode
import org.apache.logging.log4j.kotlin.logger
import org.xmlresolver.XMLResolver
import java.io.File
import java.net.URI
import kotlin.collections.iterator

class XmlCalabashBuilder {
    // XmlCalabashConfiguration
    val assertions = CfgValue(AssertionsLevel.WARNING)
    val debug = CfgValue(false)
    val debugger = CfgValue(false)
    val xmlResolver = CfgValue<XMLResolver>()
    val documentManager = CfgValue<DocumentManager>()
    val eagerEvaluation = CfgValue(false)
    val errorExplanation = CfgValue<ErrorExplanation>()
    val implicitParameterName = CfgValue<QName>()
    val inlineTrimWhitespace = CfgValue(false)
    val licensed = CfgValue(false)
    val lineNumbering = CfgValue(false)
    val messagePrinter = CfgValue<MessagePrinter>()
    val messageReporter = CfgValue<MessageReporter>()
    val other = CfgMapValue<QName, List<Map<QName, String>>>()
    val pagedMediaManagers = CfgListValue<PagedMediaManager>()
    val pagedMediaCssProcessors = CfgListValue<URI>()
    val pagedMediaXslProcessors = CfgListValue<URI>()
    val configuredXQueryProcessors = CfgMapValue<URI, Map<QName, String>>()
    val defaultXQueryProcessor = CfgValue<URI>(URI.create("https://saxonica.com/"))
    val pipedMode = CfgValue(false)
    val proxies = CfgMapValue<String, String>()
    val saxonConfigurationFile = CfgValue<File>()
    val saxonConfigurationProperties = CfgMapValue<String, String>()
    val sendmail = CfgMapValue<String, String>()
    val serialization = CfgMapValue<MediaType, Map<QName, String>>()
    val maxThreadCount = CfgValue(1)
    val trace = CfgValue<File>()
    val traceDocuments = CfgValue<File>()
    val tryNamespaces = CfgValue<Boolean>()
    val uniqueInlineUris = CfgValue(true)
    val useLocationHints = CfgValue<Boolean>()
    val validationMode = CfgValue<ValidationMode>()
    val verbosity = CfgValue(Verbosity.INFO)
    val visualizerName = CfgValue("silent")
    val xmlCatalogs = CfgListValue<URI>()
    val xmlSchemas = CfgListValue<URI>()
    val extensions = CfgListValue<ExtensionName>(emptyList())

    // Pipeline configuration
    val namespaces = CfgMapValue<String, NamespaceUri>()
    val command = CfgValue<String>()
    val commandOptions = CfgListValue<String>()
    val configurationFile = CfgValue<File>()
    val inputs = CfgMapValue<String, List<XmlCalabashInput>>()
    val outputs = CfgMapValue<String, XmlCalabashOutput>()
    val outputSerialization = CfgMapValue<String, Map<String, String>>()
    val options = CfgMapValue<String, List<Any>>()
    val initializers = CfgListValue<Pair<String, Boolean>>()
    val graphs = CfgValue<File>()
    val graphStyle = CfgValue<URI>()
    val graphviz = CfgValue<File>()
    val explainErrors = CfgValue(false)
    val stacktrace = CfgValue(false)
    val go = CfgValue(true)
    val step = CfgValue<String>()
    val pipelineUri = CfgValue<URI>()
    val mimeTypes = CfgMapValue<String, List<String>>()
    val mpt = CfgValue(0.99999998)
    val cssFormatter = CfgMapValue<URI, Map<QName, String>>()
    val xslFormatter = CfgMapValue<URI, Map<QName, String>>()
    val messageReporterBufferSize = CfgValue(32)
    val configurers = CfgListValue<Configurer>()
    val additionalMimeTypeMappings = CfgMapValue<MediaType, Set<String>>()

    private val uninitializedFormatters = mutableSetOf<URI>()
    private val configurerList = mutableListOf<Configurer>()

    init {
        for ((prefix, namespace) in mapOf(
            "cx" to NsCx.namespace,
            "p" to NsP.namespace,
            "xs" to NsXs.namespace,
            "fn" to NsFn.namespace,
            "map" to NsFn.mapNamespace,
            "array" to NsFn.arrayNamespace,
            "math" to NsFn.mathNamespace,
            "saxon" to NsSaxon.namespace,
            "xml" to NsXml.namespace
        )) {
            namespaces.put(prefix, namespace)
        }
    }

    fun addMimeTypeMapping(contentType: MediaType, extensions: Set<String>) {
        val allext = mutableSetOf<String>()
        allext.addAll(additionalMimeTypeMappings.get(contentType) ?: emptySet())
        allext.addAll(extensions)
        additionalMimeTypeMappings.put(contentType, allext)
    }

    fun build(): XmlCalabash {
        synchronized(this) {
            commonBuild()
            val xconfig = InvokedConfiguration(this)

            val configInit = mutableMapOf<String, Boolean>()
            // It feels like this configuration should go somewhere else...but since
            // CoffeeSacks is now bundled, try to initialize it for the user...
            configInit["org.nineml.coffeesacks.RegisterCoffeeSacks"] = true
            for (pair in initializers.getOrDefault() ?: emptyList()) {
                configInit[pair.first] = pair.second
            }
            val saxonConfiguration = SaxonConfiguration.newInstance(xconfig.licensed, xconfig.saxonConfigurationFile?.toURI(),
                xconfig.saxonConfigurationProperties, xconfig.xmlSchemas, configInit, configurerList)
            saxonConfiguration.configuration.resourceResolver = xconfig.documentManager
            saxonConfiguration.configuration.isLineNumbering = lineNumbering.getOrDefault() == true
            xconfig._saxonConfiguration = saxonConfiguration

                if (xconfig.xmlSchemas.isNotEmpty()
                && !saxonConfiguration.configuration.isLicensedFeature(Configuration.LicenseFeature.SCHEMA_VALIDATION)) {
                logger.warn { "Schema validation feature is not enabled, ignoring configured schemas" }
            }

            for (manager in xconfig.pagedMediaManagers) {
                for (formatter in manager.formatters()) {
                    if (formatter in uninitializedFormatters) {
                        manager.configure(formatter, emptyMap())
                    }
                }
            }

            return XmlCalabash(xconfig)
        }
    }

    fun build(configuration: Configuration): XmlCalabash {
        synchronized(this) {
            commonBuild()

            if (saxonConfigurationFile.getOrDefault() != null
                || (saxonConfigurationProperties.getOrDefault() ?: emptyMap()).isNotEmpty()
                || (this@XmlCalabashBuilder.xmlSchemas.getOrDefault() ?: emptyList()).isNotEmpty()
                || (initializers.getOrDefault() ?: emptyList()).isNotEmpty()) {
                throw XProcError.Companion.xiNoConfigurationAllowed().exception()
            }

            val xconfig = InvokedConfiguration(this)
            val saxonConfiguration = SaxonConfiguration.Companion.newInstance(configuration)
            xconfig._saxonConfiguration = saxonConfiguration
            return XmlCalabash(xconfig)
        }
    }

    private fun commonBuild() {
        uninitializedFormatters.clear()
        configurerList.clear()

        if (command.get() == null) {
            command.set("help")
        }

        if (messagePrinter.get() == null) {
            messagePrinter.set(DefaultMessagePrinter())
        }
        val printer = messagePrinter.get()!!

        if (messageReporter.get() == null) {
            val defaultReporter = DefaultMessageReporter()
            defaultReporter.setThreshold(verbosity.getOrDefault()!!)
            defaultReporter.setMessagePrinter(printer)
            messageReporter.set(defaultReporter)
        }
        val reporter = messageReporter.get()!!

        if (errorExplanation.get() == null) {
            errorExplanation.set(DefaultErrorExplanation(reporter))
        }

        if (xmlResolver.get() == null) {
            xmlResolver.set(XMLResolver())
        }

        if (documentManager.get() == null) {
            documentManager.set(DocumentManager(this, xmlResolver.get()!!))
        }
        val documentManager = documentManager.get()!!

        for (provider in DocumentResolverServiceProvider.providers()) {
            val manager = provider.create();
            manager.configure(documentManager)
        }

        for ((contentType, exensions) in additionalMimeTypeMappings.getOrDefault() ?: emptyMap()) {
            documentManager.mimetypesFileTypeMap.addMimeTypes("${contentType} ${extensions}")
        }

        for (provider in PagedMediaServiceProvider.Companion.providers()) {
            val manager = provider.create()
            pagedMediaManagers.add(manager)
            uninitializedFormatters.addAll(manager.formatters())
        }

        configurerList.addAll(configurers.getOrDefault() ?: emptyList())
        for (provider in ConfigurerServiceProvider.Companion.providers()) {
            val configurer = provider.create()
            configurerList.add(configurer)
        }

        if (debug.getOrDefault()!! && verbosity.getOrDefault()!! < Verbosity.DEBUG) {
            verbosity.set(Verbosity.DEBUG)
        }

        if (debugger.getOrDefault()!!) {
            visualizerName.set("silent")
        }

        if (visualizerName.getOrDefault() !in listOf("silent", "plain", "detail")) {
            printer.print("Unexpected visualizer: ${visualizerName.getOrDefault()}")
            visualizerName.set("silent")
        }

        val docs = traceDocuments.get()
        if (docs != null) {
            if (docs.exists() && !docs.isDirectory) {
                logger.warn { "Trace documents output must be a directory: ${docs.absolutePath}" }
                traceDocuments.set(null)
            } else {
                if (trace.get() == null) {
                    trace.set(traceDocuments.get()!!.resolve("trace.xml"))
                }
            }
        }
    }

    fun update(props: XmlCalabashBuilder) {
        assertions.update(props.assertions)
        debug.update(props.debug)
        debugger.update(props.debugger)
        documentManager.update(props.documentManager)
        eagerEvaluation.update(props.eagerEvaluation)
        errorExplanation.update(props.errorExplanation)
        graphStyle.update(props.graphStyle)
        graphviz.update(props.graphviz)
        implicitParameterName.update(props.implicitParameterName)
        inlineTrimWhitespace.update(props.inlineTrimWhitespace)
        licensed.update(props.licensed)
        lineNumbering.update(props.lineNumbering)
        messagePrinter.update(props.messagePrinter)
        messageReporter.update(props.messageReporter)
        other.update(props.other)
        pagedMediaManagers.update(props.pagedMediaManagers)
        pagedMediaCssProcessors.update(props.pagedMediaCssProcessors)
        pagedMediaXslProcessors.update(props.pagedMediaXslProcessors)
        configuredXQueryProcessors.update(props.configuredXQueryProcessors)
        defaultXQueryProcessor.update(props.defaultXQueryProcessor)
        pipedMode.update(props.pipedMode)
        proxies.update(props.proxies)
        saxonConfigurationFile.update(props.saxonConfigurationFile)
        saxonConfigurationProperties.update(props.saxonConfigurationProperties)
        sendmail.update(props.sendmail)
        serialization.update(props.serialization)
        maxThreadCount.update(props.maxThreadCount)
        trace.update(props.trace)
        traceDocuments.update(props.traceDocuments)
        tryNamespaces.update(props.tryNamespaces)
        uniqueInlineUris.update(props.uniqueInlineUris)
        useLocationHints.update(props.useLocationHints)
        validationMode.update(props.validationMode)
        verbosity.update(props.verbosity)
        visualizerName.update(props.visualizerName)
        xmlCatalogs.update(props.xmlCatalogs)
        xmlSchemas.update(props.xmlSchemas)
        extensions.update(props.extensions)

        // Namespaces is special; merge them...
        val nsmap = mutableMapOf<String, NamespaceUri>()
        namespaces.get()?.let { nsmap.putAll(it) }
        props.namespaces.get()?.let { nsmap.putAll(it) }
        namespaces.set(nsmap)

        command.update(props.command)
        commandOptions.update(props.commandOptions)
        configurationFile.update(props.configurationFile)
        inputs.update(props.inputs)
        outputs.update(props.outputs)
        outputSerialization.update(props.outputSerialization)
        initializers.update(props.initializers)
        graphs.update(props.graphs)
        explainErrors.update(props.explainErrors)
        stacktrace.update(props.stacktrace)
        go.update(props.go)
        step.update(props.step)
        pipelineUri.update(props.pipelineUri)
        options.update(props.options)
        mimeTypes.update(props.mimeTypes)
        mpt.update(props.mpt)
        cssFormatter.update(props.cssFormatter)
        xslFormatter.update(props.xslFormatter)
        messageReporterBufferSize.update(props.messageReporterBufferSize)
    }

    private class InvokedConfiguration constructor(props: XmlCalabashBuilder): XmlCalabashConfiguration {
        lateinit var _saxonConfiguration: SaxonConfiguration
        override val saxonConfiguration: SaxonConfiguration
            get() = _saxonConfiguration

        override val documentManager: DocumentManager = props.documentManager.getOrDefault() ?: DocumentManager(props, props.xmlResolver.getOrDefault() ?: XMLResolver())
        override val pagedMediaManagers: List<PagedMediaManager> = props.pagedMediaManagers.getOrDefault() ?: emptyList()
        override val assertions: AssertionsLevel = props.assertions.getOrDefault()!!
        override val debug: Boolean = props.debug.getOrDefault()!!
        override val debugger: Boolean = props.debugger.getOrDefault()!!
        override val eagerEvaluation: Boolean = props.eagerEvaluation.getOrDefault()!!
        override val errorExplanation: ErrorExplanation = props.errorExplanation.get()!!
        override val implicitParameterName: QName? = props.implicitParameterName.getOrDefault()
        override val inlineTrimWhitespace: Boolean = props.inlineTrimWhitespace.getOrDefault()!!
        override val licensed: Boolean = props.licensed.getOrDefault()!!
        override val lineNumbering: Boolean = props.lineNumbering.getOrDefault()!!
        override val messagePrinter: MessagePrinter = props.messagePrinter.get()!!
        override val messageReporter: MessageReporter = props.messageReporter.get()!!
        override val other: Map<QName, List<Map<QName, String>>> = props.other.getOrDefault() ?: emptyMap()
        override val pagedMediaCssProcessors: List<URI> = props.pagedMediaCssProcessors.getOrDefault() ?: emptyList()
        override val pagedMediaXslProcessors: List<URI> = props.pagedMediaXslProcessors.getOrDefault() ?: emptyList()
        override val configuredXQueryProcessors: Map<URI, Map<QName, String>> = props.configuredXQueryProcessors.getOrDefault() ?: emptyMap()
        override val defaultXQueryProcessor: URI = props.defaultXQueryProcessor.getOrDefault()!!
        override val pipe: Boolean = props.pipedMode.getOrDefault()!!
        override val proxies: Map<String, String> = props.proxies.getOrDefault() ?: emptyMap()
        override val saxonConfigurationFile: File? = props.saxonConfigurationFile.getOrDefault()
        override val saxonConfigurationProperties: Map<String, String> = props.saxonConfigurationProperties.getOrDefault() ?: emptyMap()
        override val sendmail: Map<String, String> = props.sendmail.getOrDefault() ?: emptyMap()
        override val serialization: Map<MediaType, Map<QName, String>> = props.serialization.getOrDefault() ?: emptyMap()
        override val maxThreadCount: Int = props.maxThreadCount.getOrDefault()!!
        override val trace: File? = props.trace.getOrDefault()
        override val traceDocuments: File? = props.traceDocuments.getOrDefault()
        override val tryNamespaces: Boolean = props.tryNamespaces.getOrDefault() ?: false
        override val uniqueInlineUris: Boolean = props.uniqueInlineUris.getOrDefault()!!
        override val useLocationHints: Boolean = props.useLocationHints.getOrDefault() ?: false
        override val validationMode: ValidationMode = props.validationMode.getOrDefault() ?: ValidationMode.DEFAULT
        override val verbosity: Verbosity = props.verbosity.getOrDefault()!!
        override val visualizer: String = props.visualizerName.getOrDefault()!!
        override val visualizerProperties: Map<String, String> = props.visualizerName.options ?: emptyMap()
        override val xmlCatalogs: List<URI> = props.xmlCatalogs.getOrDefault() ?: emptyList()
        override val xmlSchemas: List<URI> = props.xmlSchemas.getOrDefault() ?: emptyList()
        override val extensions: Set<ExtensionName> = (props.extensions.getOrDefault() ?: emptyList()).toSet()
    }
}