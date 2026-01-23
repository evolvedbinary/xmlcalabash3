package com.xmlcalabash.app

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.XmlCalabashBuildConfig
import com.xmlcalabash.XmlCalabashBuilder
import com.xmlcalabash.api.MessageReporter
import com.xmlcalabash.config.ConfigurationLoader
import com.xmlcalabash.config.XmlCalabashInput
import com.xmlcalabash.config.XmlCalabashOutput
import com.xmlcalabash.datamodel.*
import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.DefaultErrorExplanation
import com.xmlcalabash.exceptions.ErrorExplanation
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.io.MessagePrinter
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.spi.DocumentResolverServiceProvider
import com.xmlcalabash.util.*
import net.sf.saxon.Configuration
import net.sf.saxon.s9api.*
import org.apache.logging.log4j.kotlin.logger
import org.xml.sax.SAXParseException
import org.xmlresolver.ResolverFeature
import org.xmlresolver.XMLResolver
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

/**
 * Run the XML Calabash application with the specified arguments.
 * @throws XProcException if an error occurs.
 */
class XmlCalabashCli private constructor() {
    companion object {
        fun run(args: Array<out String>) {
            val cli = XmlCalabashCli()
            cli.run(args)
        }
    }

    private lateinit var xmlCalabash: XmlCalabash
    private lateinit var builder: XmlCalabashBuilder
    private lateinit var cliPrinter: MessagePrinter
    private lateinit var cliReporter: MessageReporter
    private lateinit var cliExplain: ErrorExplanation
    private lateinit var stepConfig: InstructionConfiguration
    private val serializationParameters = mutableMapOf<String, MutableMap<QName, XdmAtomicValue>>()
    private var sawStdout = false
    // This is a terrible hack because we try to initialize the SaxonConfiguration before
    // we've initialized the stepConfig. It would be better refactor this.
    private var haveStepConfig = false

    private fun run(args: Array<out String>) {
        builder = XmlCalabashBuilder()

        try {
            builder.update(CommandLine.parse(args))
        } catch (ex: XProcException) {
            setupDefaultMessaging()
            abort(cliExplain, ex)
        }

        loadConfiguration(builder.configurationFile.getOrDefault())?.let { builder.update(it) }

        val resolver = XMLResolver()
        resolver.configuration.setFeature(ResolverFeature.CLASSPATH_CATALOGS, true)
        builder.xmlResolver.set(resolver)

        setupDefaultMessaging()

        var tstart: Long = 0
        var tend: Long = 0
        try {
            val moon = Moon.illumination()
            if (moon > builder.mpt.getOrDefault()!!) {
                if (moon > 0.99) {
                    warn { "The moon is full." }
                } else {
                    warn { "The moon is ${"%3.1f".format(moon * 100.0)}% full." }
                }
            }

            xmlCalabash = builder.build()
            val xprocParser = xmlCalabash.newXProcParser()
            stepConfig = xprocParser.builder.stepConfig
            haveStepConfig = true
            cliExplain = stepConfig.errorExplanation

            val command = builder.command.getOrDefault()!!
            if (command == "help" || (command == "run" && builder.pipelineUri.get() == null && builder.step.get() == null)) {
                help()
                return
            }

            logVersion()

            when (command) {
                "info-version" -> {
                    version()
                    return
                }
                "info-mimetype" -> {
                    showMimetype(builder.commandOptions.getOrDefault()?.firstOrNull())
                    return
                }
                "info-mimetypes" -> {
                    showMimetypes()
                    return
                }
                "info" -> {
                    stepConfig.messagePrinter.println("The 'info' subcommands are 'version', 'mimetypes', or 'mimetype'.")
                    stepConfig.messagePrinter.println("")
                    help()
                    return
                }
                else -> Unit
            }

            evaluateSerializationParameters(builder.outputSerialization.getOrDefault() ?: emptyMap())

            // N.B. It's illegal to shadow a static option name, so we can shove all the
            // options into the static options before we parse the pipeline. This is...odd
            // and, I expect, unsatisfactory in the long term. But I'm not going to try
            // to fix that today.
            evaluateOptions(xprocParser.builder)

            val pipelineUri = builder.pipelineUri.getOrDefault()
            val step = builder.step.getOrDefault()

            val compStart = System.nanoTime()
            val declstep = if (pipelineUri != null) {
                val qname = if (step == null) {
                    null
                } else {
                    stepConfig.typeUtils.parseQName(step, builder.namespaces.get()!!)
                }
                xprocParser.parse(pipelineUri, qname)
            } else {
                val type = stepConfig.typeUtils.parseQName(step!!, builder.namespaces.get()!!)
                constructWrapper(type)
            }
            val pipeline = declstep.getExecutable()
            val compEnd = System.nanoTime()

            stepConfig.debug { "Elapsed compile time: ${(compEnd - compStart) / 1e9}s" }

            var inputMap = builder.inputs.getOrDefault() ?: emptyMap()
            var explicitStdin: String? = null
            for ((port, inputs) in inputMap) {
                for (input in inputs) {
                    if (input.href == CommandLine.STDIO_URI) {
                        explicitStdin = port
                    }
                }
            }

            var implicitStdin: String? = null
            if (explicitStdin == null && xmlCalabash.config.pipe) {
                for ((port, input) in pipeline.inputManifold) {
                    if (input.primary && port !in inputMap) {
                        implicitStdin = port
                    }
                }
            }

            var outputMap = builder.outputs.getOrDefault() ?: emptyMap()
            var explicitStdout: String? = null
            for ((port, output) in outputMap) {
                if (output.pattern == CommandLine.STDIO_NAME) {
                    explicitStdout = port
                }
            }

            var implicitStdout: String? = null
            if (xmlCalabash.config.pipe) {
                for ((port, output) in pipeline.outputManifold) {
                    if (output.primary) {
                        implicitStdout = port
                    }
                }
            }

            if (builder.graphs.getOrDefault() != null) {
                val description = pipeline.runtime.description()
                val vis = VisualizerOutput(builder, xmlCalabash, description, builder.graphs.getOrDefault()!!)
                if (builder.graphviz.getOrDefault() == null) {
                    warn { "Cannot create SVG, graphviz is not configured"}
                    vis.xml()
                } else {
                    vis.svg()
                }
            }

            if (!builder.go.getOrDefault()!!) {
                stepConfig.debug { "Execution suppressed with --nogo" }
                exitProcess(0)
            }

            if (implicitStdin != null) {
                val ctype = implicitContentType(pipeline.inputManifold[implicitStdin]?.contentTypes)
                builder.inputs.put(implicitStdin, listOf(XmlCalabashInput(CommandLine.STDIO_URI, ctype)))
                inputMap = builder.inputs.get()!!
            }

            val stdin = if (explicitStdin != null || implicitStdin != null) {
                val port = explicitStdin ?: implicitStdin!!
                var ctype = implicitContentType(pipeline.inputManifold[port]?.contentTypes)

                val inputList = inputMap[port]!!
                for (input in inputList) {
                    if (input.href == CommandLine.STDIO_URI) {
                        if (input.contentType != MediaType.ANY) {
                            ctype = input.contentType
                        }
                        break;
                    }
                }
                val loader = DocumentLoader(pipeline.config, CommandLine.STDIO_URI)
                loader.load(System.`in`, ctype)
            } else {
                null
            }

            if ("*anonymous" in inputMap && pipeline.inputManifold.size != 1) {
                throw XProcError.xiCliPortNameRequired("input").exception()
            }

            for ((portName, inputs) in inputMap) {
                val port = if (portName == "*anonymous") {
                    pipeline.inputManifold.keys.first()
                } else {
                    portName
                }

                for (input in inputs) {
                    if (input.href == CommandLine.STDIO_URI) {
                        pipeline.input(port, stdin!!)
                    } else {
                        val props = DocumentProperties()
                        if (input.contentType != MediaType.ANY) {
                            props[Ns.contentType] = input.contentType.toString()
                        }

                        val doc = if (input.href == null) {
                            input.doc!!.with(input.contentType).with(props)
                        } else {
                            stepConfig.environment.documentManager.load(input.href!!, pipeline.config, props)
                        }
                        pipeline.input(port, doc)
                    }
                }
            }

            if (implicitStdout != null && implicitStdout !in outputMap) {
                builder.outputs.put(implicitStdout, XmlCalabashOutput(CommandLine.STDIO_NAME))
                outputMap = builder.outputs.get()!!
            }

            if ("*anonymous" in outputMap && pipeline.outputManifold.size != 1) {
                throw XProcError.xiCliPortNameRequired("output").exception()
            }

            val realOutputs = mutableMapOf<String, XmlCalabashOutput>()
            for ((portName, output) in outputMap) {
                val port = if (portName == "*anonymous") {
                    pipeline.outputManifold.keys.first()
                } else {
                    portName
                }
                realOutputs[port] = output

                if (!pipeline.outputManifold.containsKey(port)) {
                    throw XProcError.xiNoSuchOutputPort(port).exception()
                }
                if (output.pattern == "-") {
                    if (sawStdout) {
                        throw XProcError.xiAtMostOneStdout().exception()
                    }
                    sawStdout = true
                }
            }

            val optManager = xprocParser.builder.staticOptionsManager
            for ((name, value) in optManager.useWhenOptions) {
                if (name !in optManager.staticOptions) {
                    pipeline.option(name, XProcDocument.ofValue(value, stepConfig, MediaType.OCTET_STREAM, DocumentProperties()))
                }
            }

            for ((name, map) in serializationParameters) {
                val portName = if (name == "*") {
                    var rname: String? = null
                    for ((pname, port) in pipeline.outputManifold) {
                        if (port.primary) {
                            rname = pname
                            break
                        }
                    }
                    if (rname == null) {
                        throw XProcError.xiCliNoPrimaryOutputPort().exception()
                    }
                    rname
                } else {
                    name
                }

                val port = pipeline.outputManifold[portName]
                if (port == null) {
                    throw XProcError.xiCliNoOutputPort(portName).exception()
                }
                for ((key, value) in map) {
                    stepConfig.debug { "Override serialization property on ${portName}: ${key}=${value}" }
                    port.serialization = port.serialization.put(XdmAtomicValue(key), value)
                }
            }

            pipeline.receiver = FileOutputReceiver(xmlCalabash, stepConfig.processor, pipeline.outputManifold, realOutputs, explicitStdout ?: implicitStdout)
            tstart = System.nanoTime()
            pipeline.run()
            tend = System.nanoTime()
        } catch (ex: Exception) {
            tend = System.nanoTime()
            if (ex is XProcException) {
                if (ex.error.code == NsErr.xi(XProcError.DEBUGGER_ABORT)) {
                    exitProcess(1)
                }
                abort(cliExplain, ex)
            } else {
                if (builder.verbosity.getOrDefault()!! <= Verbosity.DEBUG
                    || builder.stacktrace.getOrDefault()!!) {
                    ex.printStackTrace()
                }
                System.err.println(ex)
                exitProcess(1)
            }
        }

        stepConfig.debug { "Elapsed time: ${(tend - tstart) / 1e9}s" }
    }

    private fun setupDefaultMessaging() {
        if (builder.messagePrinter.get() == null) {
            builder.messagePrinter.set(DefaultMessagePrinter())
        }
        cliPrinter = builder.messagePrinter.get()!!

        if (builder.messageReporter.get() == null) {
            val defaultReporter = DefaultMessageReporter(LoggingMessageReporter())
            defaultReporter.setMessagePrinter(cliPrinter)
            val bufsize = builder.messageReporterBufferSize.getOrDefault()!!
            val bufferingReporter = BufferingMessageReporter(bufsize, defaultReporter)
            builder.messageReporter.set(bufferingReporter)
        }
        cliReporter = builder.messageReporter.get()!!
        cliReporter.setThreshold(builder.verbosity.getOrDefault()!!)

        if (builder.errorExplanation.get() == null) {
            builder.errorExplanation.set(DefaultErrorExplanation(cliReporter))
        }
        cliExplain = builder.errorExplanation.get()!!
        cliExplain.showStacktrace = builder.stacktrace.getOrDefault()!!
    }

    private fun warn(message: () -> String) {
        cliReporter.report(Verbosity.WARN) { Report(Verbosity.WARN, message) }
    }

    private fun implicitContentType(types: List<MediaType>?): MediaType {
        if (types == null) {
            return MediaType.XML
        }
        for (type in types) {
            if (type.inclusive) {
                if (type.mediaType == "*" || type.mediaSubtype == "*") {
                    when (type.suffix) {
                        "xml" -> return MediaType.XML
                        "json" -> return MediaType.JSON
                    }
                    if (type.mediaType == "text") {
                        return MediaType.TEXT
                    }
                } else {
                    return type
                }
            }
        }

        return MediaType.XML
    }

    private fun evaluateSerializationParameters(params: Map<String,Map<String,String>>) {
        for ((port, map) in params) {
            for ((key, value) in map) {
                val pair = evaluateKeyValue(key, value)
                if (pair.second !is XdmAtomicValue) {
                    throw XProcError.xiCliSerializationMustBeAtomic(pair.first).exception()
                }
                val map = serializationParameters[port] ?: mutableMapOf<QName,XdmAtomicValue>()
                map.put(pair.first, pair.second as XdmAtomicValue)
                serializationParameters[port] = map
            }
        }
    }

    private fun evaluateOptions(pipelineBuilder: PipelineBuilder) {
        // FIXME: refactor this method to use evaluateKeyValue()
        val nsmap = builder.namespaces.get()!!
        val processor = stepConfig.processor

        val compiler = processor.newXPathCompiler()
        compiler.baseURI = UriUtils.cwdAsUri()
        compiler.isSchemaAware = processor.isSchemaAware
        for ((name, namespace) in nsmap) {
            compiler.declareNamespace(name, namespace.toString())
        }

        val implicitParameterName = pipelineBuilder.stepConfig.xmlCalabashConfig.implicitParameterName
        val mapOptions = mutableMapOf<QName, MutableMap<QName, XdmValue>>()

        for ((name, initializers) in builder.options.getOrDefault() ?: emptyMap()) {
            var mapName: QName? = null
            val ccpos = name.indexOf("::")
            val qname = if (ccpos >= 0) {
                if (ccpos > 0) {
                    mapName = stepConfig.typeUtils.parseQName(name.substring(0, ccpos), nsmap)
                } else {
                    mapName = implicitParameterName
                }
                stepConfig.typeUtils.parseQName(name.substring(ccpos + 2), nsmap)
            } else {
                stepConfig.typeUtils.parseQName(name, nsmap)
            }

            var value: XdmValue = if (mapName != null) {
                if (mapName !in mapOptions) {
                    mapOptions[mapName] = mutableMapOf()
                }
                mapOptions[mapName]!![qname] ?: XdmEmptySequence.getInstance()
            } else {
                XdmEmptySequence.getInstance()
            }

            for (initializer in initializers) {
                when (initializer) {
                    is XdmValue -> {
                        value = value.append(initializer)
                    }
                    is String -> {
                        val ivalue = if (initializer.startsWith("?")) {
                            val exec = compiler.compile(initializer.substring(1))
                            val selector = exec.load()
                            selector.evaluate()
                        } else {
                            XdmAtomicValue(initializer, ItemType.UNTYPED_ATOMIC)
                        }
                        value = value.append(ivalue)
                    }
                    else -> {
                        throw IllegalArgumentException("Unknown initializer (not XdmValue or String): $initializer")
                    }
                }
            }

            if (mapName != null) {
                mapOptions[mapName]!![qname] = value
            } else {
                pipelineBuilder.option(qname, value)
            }
        }

        for ((name, value) in mapOptions) {
            pipelineBuilder.option(name, stepConfig.typeUtils.asXdmMap(value))
        }
    }

    private fun evaluateKeyValue(name: String, value: String): Pair<QName, XdmValue> {
        val nsmap = builder.namespaces.get()!!
        val processor = stepConfig.processor

        val compiler = processor.newXPathCompiler()
        compiler.baseURI = UriUtils.cwdAsUri()
        compiler.isSchemaAware = processor.isSchemaAware
        for ((name, namespace) in nsmap) {
            compiler.declareNamespace(name, namespace.toString())
        }

        val qname = stepConfig.typeUtils.parseQName(name, nsmap)
        val ivalue = if (value.startsWith("?")) {
            val exec = compiler.compile(value.substring(1))
            val selector = exec.load()
            selector.evaluate()
        } else {
            XdmAtomicValue(value, ItemType.UNTYPED_ATOMIC)
        }

        return Pair(qname, ivalue)
    }

    private fun loadConfiguration(commandLineConfig: File?): XmlCalabashBuilder? {
        val configLocations = mutableListOf<File>()
        commandLineConfig?.let { configLocations.add(it) }
        configLocations.add(File(UriUtils.path(UriUtils.resolve(UriUtils.cwdAsUri(),".xmlcalabash3")!!)))
        configLocations.add(File(UriUtils.path(UriUtils.resolve(UriUtils.homeAsUri(), ".xmlcalabash3")!!)))
        for (config in configLocations) {
            if (config.exists() && config.isFile) {
                val loader = ConfigurationLoader()
                return loader.load(config)
            }
        }
        return null
    }

    private fun abort(errorExplanation: ErrorExplanation, error: XProcException) {
        abort(errorExplanation, listOf(error))
    }

    private fun abort(errorExplanation: ErrorExplanation, errors: List<XProcException>) {
        val verbosity = builder.verbosity.getOrDefault()!!

        for (error in errors) {
            explainError(errorExplanation, error)
        }

        if (verbosity <= Verbosity.DEBUG || builder.stacktrace.getOrDefault()!!) {
            errors[0].printStackTrace()
            if (errors[0].cause != null && errors[0].cause != errors[0]) {
                errors[0].cause!!.printStackTrace()
            }
        }

        exitProcess(1)
    }

    private fun explainError(errorExplanation: ErrorExplanation, error: XProcException) {
        errorExplanation.report(error.error)

        when (error.error.code) {
            NsErr.xc(93) -> { // XSLT compile error
                // This is kind of awful
                val reports = error.error.details[2] as List<*>
                for (anyReport in reports) {
                    val report = anyReport as Report
                    stepConfig.xmlCalabashConfig.messageReporter.report(report.severity) { report }
                }
            }
            else -> {
                val cause = error.cause
                if (cause != null) {
                    val report = when (cause) {
                        is SAXParseException -> {
                            var sep = ""
                            val sb = StringBuilder()
                            if (cause.systemId != null) {
                                sb.append(cause.systemId!!)
                                sep = ":"
                            }
                            if (cause.lineNumber > 0) {
                                sb.append(sep).append(cause.lineNumber)
                                sep = ":"
                            }
                            if (cause.columnNumber > 0) {
                                sb.append(sep).append(cause.columnNumber)
                                sep = ":"
                            }
                            sb.append(sep)
                            sb.append(cause.message ?: "Unknown error")
                            Report(Verbosity.ERROR, { sb.toString() }, error.cause!!)
                        }
                        else -> {
                            Report(Verbosity.ERROR, { error.cause?.message ?: "Unknown error" }, error.cause!!)
                        }
                    }
                    if (haveStepConfig) {
                        stepConfig.xmlCalabashConfig.messageReporter.report(report.severity) { report }
                    } else {
                        cliReporter.report(report.severity) { report }
                    }
                }
            }
        }

        if (builder.explainErrors.getOrDefault()!!) {
            errorExplanation.reportExplanation(error.error)
        }

        if (error.cause is XProcException) {
            cliPrinter.println("Caused by:")
            explainError(errorExplanation, error.cause as XProcException)
        }
    }

    private fun help() {
        val stream = XmlCalabashCli::class.java.getResourceAsStream("/com/xmlcalabash/help.txt")
        if (stream == null) {
           stepConfig.messagePrinter.println("Error: help is not available.")
            return
        }
        val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
        for (line in reader.lines()) {
            stepConfig.messagePrinter.println(line)
        }
    }

    private fun version() {
        val proc = stepConfig.processor
        val edition = actualSaxonEdition()

        val stream = XmlCalabash::class.java.getResourceAsStream("/com/xmlcalabash/exproc-contrib-last-modified.txt")
        val exprocDate = if (stream != null) {
            val ps = InputStreamReader(stream, Charsets.UTF_8)
            ps.readLines().firstOrNull()
        } else {
            null
        }

        val totThreads = 2.coerceAtLeast(Runtime.getRuntime().availableProcessors())
        val maxThreads = totThreads.coerceAtMost(stepConfig.xmlCalabashConfig.maxThreadCount)
        val deplist = XmlCalabashBuildConfig.DEPENDENCIES.keys.toList().sorted()

        if (xmlCalabash.config.verbosity <= Verbosity.DEBUG) {
            println("PRODUCT_NAME=${XmlCalabashBuildConfig.PRODUCT_NAME}")
            println("VERSION=${XmlCalabashBuildConfig.VERSION}")
            println("BUILD_DATE=${XmlCalabashBuildConfig.BUILD_DATE}")
            println("BUILD_ID=${XmlCalabashBuildConfig.BUILD_ID}")
            println("SAXON_EDITION=${proc.saxonEdition}")
            println("VENDOR_NAME=${XmlCalabashBuildConfig.VENDOR_NAME}")
            println("VENDOR_URI=${XmlCalabashBuildConfig.VENDOR_URI}")
            println("THREADS=${maxThreads}")
            println("MAX_THREADS=${totThreads}")

            if (exprocDate != null) {
                println("EXPROC_CONTRIB=${exprocDate}")
            }

            for (ext in xmlCalabash.config.extensions) {
                println("${ext}=true")
            }

            for (name in deplist) {
                val version = XmlCalabashBuildConfig.DEPENDENCIES[name]!!
                println("${name}=${version}")
            }
        } else {
            stepConfig.messagePrinter.println(versionLine())
            stepConfig.messagePrinter.print("Running with Saxon ${proc.saxonEdition} version ${proc.saxonProductVersion}")

            if (totThreads == maxThreads) {
                stepConfig.messagePrinter.println(" using ${totThreads} threads")
            } else {
                stepConfig.messagePrinter.println(" using at most ${maxThreads} of ${totThreads} available threads")
            }

            if (exprocDate != null) {
                stepConfig.messagePrinter.println("Including EXProc contributed pipelines from ${exprocDate}.")
            }

            stepConfig.messagePrinter.println("The default character set is ${stepConfig.messagePrinter.encoding}")

            if (xmlCalabash.config.extensions.isNotEmpty()) {
                val sb = StringBuilder()
                for ((index, ext) in xmlCalabash.config.extensions.withIndex()) {
                    if (index > 0) {
                        sb.append(", ")
                    }
                    sb.append(ext)
                }
                stepConfig.messagePrinter.println("Extensions enabled: ${sb}.")
            }

            if (edition != proc.saxonEdition) {
                if (xmlCalabash.config.licensed) {
                    stepConfig.messagePrinter.println("(You appear to have ${edition}; perhaps a license wasn't found?)")
                } else {
                    stepConfig.messagePrinter.println("(You appear to have ${edition} but the license is explicitly disabled.)")
                }
            }
        }
    }

    private fun showMimetype(extension: String?) {
        val types = xmlCalabash.config.documentManager.mimetypesFileTypeMap
        if (extension == null) {
            println("\tdefault content type is ${types.getContentType("default")}")
            return
        }

        if (extension.contains("/")) {
            val types = xmlCalabash.config.documentManager.mimetypesFileTypeMap
            types as MemoMimetypesFileTypeMap
            val extensions = types.extensionMap.keys.sorted()
            val matching = mutableSetOf<String>()
            for (ext in extensions) {
                val ctype = types.extensionMap[ext]!!
                if (ctype.contains(extension)) {
                    matching.add("\t.${ext} is ${ctype}")
                }
            }
            if (matching.isEmpty()) {
                println("No filename extensions found containing \"${extension}\"")
            } else {
                println("Filename extensions for content types containing \"${extension}\":")
                for (line in matching) {
                    println(line)
                }
            }
            println("Additional mappings may have been defined in the JVM, for example with a .mime.types file.")
            println("See https://docs.oracle.com/javase/7/docs/api/javax/activation/MimetypesFileTypeMap.html")
            println("Use the 'info mimetype <ext>' command to query the content type of a particular <ext>.")
        } else {
            println("Filename extension/content type mapping:")
            val dotlessExt = extension.trimStart('.')
            val ctype = types.getContentType("file.${dotlessExt}")
            println("\t.${dotlessExt} is ${ctype}")
        }

    }

    private fun showMimetypes() {
        val types = xmlCalabash.config.documentManager.mimetypesFileTypeMap
        if (types is MemoMimetypesFileTypeMap) {
            val extensions = types.extensionMap.keys.sorted()
            if (extensions.isNotEmpty()) {
                println("Filename extension/content type mappings defined by XML Calabash:")
                for (ext in extensions) {
                    val ctype = types.extensionMap[ext]!!
                    println("\t.${ext} is ${ctype}")
                }
                println("Additional mappings may have been defined in the JVM, for example with a .mime.types file.")
                println("See https://docs.oracle.com/javase/7/docs/api/javax/activation/MimetypesFileTypeMap.html")
                println("Use the 'info mimetype <ext>' command to query the content type of a particular <ext>.")
            }
        }
    }

    private fun logVersion() {
        val proc = stepConfig.processor
        val edition = actualSaxonEdition()

        logger.debug { versionLine() }
        logger.debug { saxonVersionLine() }

        if (edition != proc.saxonEdition) {
            logger.debug {
                if (xmlCalabash.config.licensed) {
                    "Saxon ${edition} is available; perhaps a license wasn't found?"
                } else {
                    "Saxon ${edition} is available, but the license is explicitly disabled."
                }
            }
        }
    }

    private fun versionLine(): String {
        val sb = StringBuilder()
        sb.append("${XmlCalabashBuildConfig.PRODUCT_NAME} version ${XmlCalabashBuildConfig.VERSION} ")
        sb.append("(build ${XmlCalabashBuildConfig.BUILD_ID}, ${formattedDate()})")
        return sb.toString()
    }

    private fun saxonVersionLine(): String {
        val proc = stepConfig.processor

        val totThreads = 2.coerceAtLeast(Runtime.getRuntime().availableProcessors())
        val maxThreads = totThreads.coerceAtMost(stepConfig.xmlCalabashConfig.maxThreadCount)

        val sb = StringBuilder()
        sb.append("Running with Saxon ${proc.saxonEdition} version ${proc.saxonProductVersion}")
        if (totThreads == maxThreads) {
            sb.append(" using ${totThreads} threads")
        } else {
            sb.append(" using at most ${maxThreads} of ${totThreads} available threads")
        }

        return sb.toString()
    }

    private fun formattedDate(): String {
        val dateParser = SimpleDateFormat("yyyy-MM-dd")
        val dateInstant = dateParser.parse(XmlCalabashBuildConfig.BUILD_DATE).toInstant()
        val date = LocalDateTime.ofInstant(dateInstant, ZoneId.systemDefault())
        val dateFormatter = DateTimeFormatter.ofPattern("dd LLLL yyyy")
        return dateFormatter.format(date)
    }

    private fun actualSaxonEdition(): String {
        val proc = stepConfig.processor
        val license = proc.underlyingConfiguration.isLicensedFeature(Configuration.LicenseFeature.SCHEMA_VALIDATION)
                || proc.underlyingConfiguration.isLicensedFeature(Configuration.LicenseFeature.PROFESSIONAL_EDITION)
        var edition = proc.saxonEdition
        if (!license) {
            if (javaClassExists("com.saxonica.config.EnterpriseConfiguration")) {
                edition = "EE"
            } else if (javaClassExists("com.saxonica.config.ProfessionalConfiguration")) {
                edition = "PE"
            }
        }
        return edition
    }

    private fun javaClassExists(klass: String): Boolean {
        try {
            if (Class.forName(klass) != null) {
                return true
            }
        } catch (_: ClassNotFoundException) {
            return false
        }
        return false
    }

    private fun constructWrapper(xstep: QName): DeclareStepInstruction {
        val stepDecl = findDeclaration(xstep)
        val step = stepDecl.second

        val builder = xmlCalabash.newPipelineBuilder(3.1)
        val decl = builder.newDeclareStep()

        if (stepDecl.first != null) {
            decl.import(stepDecl.first!!)
        }

        for (input in step.inputs()) {
            decl.input(input.port, input.primary == true, input.sequence == true)
        }
        for (output in step.outputs()) {
            val decloutput = decl.output(output.port, output.primary == true, output.sequence == true)
            decloutput.pipe = output.port

        }
        for (option in step.getOptions()) {
            val decloption = decl.option(option.name)
            decloption.values = option.values
            decloption.asType = option.asType
            decloption.required = option.required
            decloption.select = option.select
            decloption.specialType = option.specialType
        }

        val innerstep = decl.atomicStep(step.type!!)
        for (input in step.inputs()) {
            val wi = innerstep.withInput(input.port)
            wi.pipe = input.port
        }
        for (option in step.getOptions()) {
            val wo = innerstep.withOption(option.name)
            wo.empty()
            wo.select = XProcExpression.select(innerstep.stepConfig, "\$${option.name}")
        }

        return decl
    }

    private fun findDeclaration(type: QName): Pair<LibraryInstruction?, DeclareStepInstruction> {
        val pstep = stepConfig.standardSteps[type]
        if (pstep != null) {
            return Pair(null, pstep)
        }

        for (provider in DocumentResolverServiceProvider.providers()) {
            val resolver = provider.create()
            for (uri in resolver.resolvableLibraryUris()) {
                val xprocParser = xmlCalabash.newXProcParser()
                val library = xprocParser.parseLibrary(uri)
                for (decl in library.children.filterIsInstance<DeclareStepInstruction>()) {
                    if (decl.type == type) {
                        return Pair(library, decl)
                    }
                }
            }
        }

        throw XProcError.xsMissingStepDeclaration(type).exception()
    }
}