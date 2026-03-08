package com.xmlcalabash.app

import com.xmlcalabash.config.XmlCalabashInput
import com.xmlcalabash.XmlCalabashBuilder
import com.xmlcalabash.config.XmlCalabashOutput
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.util.AssertionsLevel
import com.xmlcalabash.util.ExtensionName
import com.xmlcalabash.util.UriUtils
import com.xmlcalabash.util.Verbosity
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.ValidationMode
import java.io.File
import java.net.URI

/**
 * The parser for options, arguments, and parameters passed on the command line.
 *
 * This class parses an array of strings (as might appear as arguments on the command line). The resulting
 * object has properties for all the options provided. If an option is not provided, and it is not required,
 * the value of the corresponding property will be `null`.
 *
 * @constructor The (private) primary constructor.
 * @property args The array of command line arguments to parse.
 */

class CommandLine private constructor(val args: Array<out String>) {
    /**
     * Provides a static method to create a [CommandLine] from a list of arguments.
     * You cannot instantiate a [CommandLine] directly.
     */
    companion object {
        /** The name of "standard input".
         * The string "-" is used to represent `stdin`.
         */
        const val STDIO_NAME = "-"

        /** The URI used to represent "standard output".
         * Outputs are sent to URIs, generally `file:` URIs, this URI is magic and represents
         * `stdout`.
         */
        val STDIO_URI = URI("https://xmlcalabash.com/ns/stdio")

        /**
         * Parse a list of arguments.
         */
        fun parse(args: Array<out String>): XmlCalabashBuilder {
            val cli = CommandLine(args)
            return cli.parse()
        }
    }

    private lateinit var builder: XmlCalabashBuilder
    private val validCommands = listOf("version", "run", "help", "info")
    private var _help = false
    private val seenNamespaces = mutableSetOf<String>()

    private val arguments = listOf(
        ArgumentDescription("--assertions", listOf(), ArgumentType.STRING, "warn", listOf("ignore", "warn", "warning", "error")) { parseAssertions(it) },
        ArgumentDescription("--catalog", listOf(), ArgumentType.URI) { parseCatalog(it) },
        ArgumentDescription("--configuration", listOf("-c", "--config"), ArgumentType.EXISTING_FILE) { builder.configurationFile.set(File(it)) },
        ArgumentDescription("--debug", listOf("-D"), ArgumentType.BOOLEAN, "true") { builder.debug.set(it == "true") },
        ArgumentDescription("--debugger", listOf(), ArgumentType.BOOLEAN, "true") { builder.debugger.set(it == "true") },
        ArgumentDescription("--explain", listOf(), ArgumentType.BOOLEAN, "true") { builder.explainErrors.set(it == "true") },
        ArgumentDescription("--extension", listOf("-X"), ArgumentType.STRING) { parseExtensionName(it) },
        ArgumentDescription("--graphs", listOf(), ArgumentType.DIRECTORY) { builder.graphs.set(File(it)) },
        ArgumentDescription("--help", listOf(), ArgumentType.BOOLEAN, "true") { _help = it == "true" },
        ArgumentDescription("--init", listOf(), ArgumentType.STRING) { builder.initializers.add(Pair(it, false)) },
        ArgumentDescription("--input", listOf("-i"), ArgumentType.STRING) { parseInput(it) },
        ArgumentDescription("--input-multiplex", listOf("-im"), ArgumentType.STRING) { parseInputMultiplex(it) },
        ArgumentDescription("--licensed", listOf(), ArgumentType.BOOLEAN, "true") { builder.licensed.set(it == "true") },
        ArgumentDescription("--line-numbering", listOf("-l"), ArgumentType.BOOLEAN, "true") { builder.lineNumbering.set(it == "true") },
        ArgumentDescription("--manifest", listOf("-m"), ArgumentType.STRING) { parseManifest(it) },
        ArgumentDescription("--namespace", listOf("-ns"), ArgumentType.STRING) { parseNamespace(it) },
        ArgumentDescription("--nogo", listOf(), ArgumentType.BOOLEAN, "true") { builder.go.set(it != "true") },
        ArgumentDescription("--output", listOf("-o"), ArgumentType.STRING) { parseOutput(it) },
        ArgumentDescription("--output-multiplex", listOf("-om"), ArgumentType.STRING) { parseOutput(it, true) },
        ArgumentDescription("--pipe", listOf(), ArgumentType.BOOLEAN, "true") { builder.pipedMode.set(it == "true") },
        ArgumentDescription("--stacktrace", listOf("--stack-trace"), ArgumentType.BOOLEAN, "true") { builder.stacktrace.set(it == "true") },
        ArgumentDescription("--step", listOf("-s"), ArgumentType.STRING) { builder.step.set(it) },
        ArgumentDescription("--temporary-files", listOf("-temp"), ArgumentType.STRING, "") { parseTemporaryFiles(it) },
        ArgumentDescription("--trace", listOf(), ArgumentType.FILE) { builder.trace.set(File(it)) },
        ArgumentDescription("--trace-documents", listOf("--trace-docs"), ArgumentType.DIRECTORY) { builder.traceDocuments.set(File(it)) },
        ArgumentDescription("--try-namespaces", listOf("--try-ns"), ArgumentType.BOOLEAN, "true") { builder.tryNamespaces.set(it == "true") },
        ArgumentDescription("--use-location-hints", listOf("--hints"), ArgumentType.BOOLEAN, "true") { builder.useLocationHints.set(it == "true") },
        ArgumentDescription("--validation-mode", listOf("--val"), ArgumentType.STRING, "strict") { parseValidationMode(it) },
        ArgumentDescription("--verbosity", listOf("-V"), ArgumentType.STRING, "info", listOf("trace", "debug", "info", "warn", "error")) { parseVerbosity(it) },
        ArgumentDescription("--visualizer", listOf("--vis"), ArgumentType.STRING, "plain", emptyList()) { parseVisualizer(it) },
        ArgumentDescription("--xml-schema", listOf("--xsd"), ArgumentType.URI) { parseXmlSchema(it) },
        )

    private fun parse(): XmlCalabashBuilder {
        builder = XmlCalabashBuilder()

        if (args.isEmpty()) {
            builder.command.set("help")
            return builder
        }

        for (opt in args) {
            var option: String = ""
            var suppliedValue: String? = null

            // Special case for --input and --output
            if (opt.startsWith("--input=") || opt.startsWith("-i=")
                || opt.startsWith("--output=") || opt.startsWith("-o=")) {
                val eqpos = opt.indexOf('=')
                option = opt.substring(0, eqpos)
                suppliedValue = opt.substring(eqpos + 1)
            } else {
                val pos = opt.indexOf(':')
                option = if (pos >= 0) {
                    opt.substring(0, pos)
                } else {
                    opt
                }
                suppliedValue = if (pos >= 0) {
                    opt.substring(pos + 1)
                } else {
                    null
                }

            }

            var processed = false
            for (arg in arguments) {
                if (option == arg.name || arg.synonyms.contains(option)) {
                    processed = true

                    var value = suppliedValue ?: arg.default
                    if (value == null) {
                        throw XProcError.xiCliValueRequired(option).exception()
                    }

                    if (arg.valid.isNotEmpty() && !arg.valid.contains(value)) {
                        throw XProcError.xiCliInvalidValue(option, value).exception()
                    }

                    when (arg.type) {
                        ArgumentType.STRING -> Unit
                        ArgumentType.BOOLEAN -> {
                            if (value != "true" && value != "false") {
                                throw XProcError.xiCliInvalidValue(option, value).exception()
                            }
                        }
                        ArgumentType.FILE -> {
                            val file = File(value)
                            if (file.exists() && !file.isFile) {
                                throw XProcError.xiCliInvalidValue(option, value).exception()
                            }
                        }
                        ArgumentType.EXISTING_FILE -> {
                            val file = File(value)
                            if (!file.exists() || !file.isFile || !file.canRead()) {
                                throw XProcError.xiCliInvalidValue(option, value).exception()
                            }
                        }
                        ArgumentType.DIRECTORY -> {
                            val file = File(value)
                            if (!file.exists()) {
                                if (!file.mkdirs()) {
                                    throw XProcError.xiCliInvalidValue(option, value).exception()
                                }
                            }
                            if (!file.isDirectory) {
                                throw XProcError.xiCliInvalidValue(option, value).exception()
                            }
                        }
                        ArgumentType.URI -> {
                            value = UriUtils.resolve(value).toString()
                        }
                    }

                    arg.process(value)

                    break
                }
            }

            if (!processed) {
                if (option.startsWith("-")) {
                    throw XProcError.xiCliUnrecognizedOption(option).exception()
                } else if (opt.contains("=")) {
                    if (opt.startsWith("!")) {
                        parseSerializationParam(opt)
                    } else {
                        parseOptionParam(opt)
                    }
                } else {
                    if (builder.pipelineUri.isSet) {
                        throw XProcError.xiCliMoreThanOnePipeline(builder.pipelineUri.get()!!.toString(), opt).exception()
                    } else {
                        val cmd = isCommand(opt)
                        if (cmd != null) {
                            builder.command.set(cmd)
                        } else {
                            builder.command.set("run")
                            builder.pipelineUri.set(UriUtils.resolve(opt))
                        }
                    }
                }
            }
        }

        if (builder.debug.isSet && builder.debug.get()!!) {
            builder.verbosity.set(Verbosity.DEBUG)
        }

        if (_help) {
            builder.command.set("help")
        }

        return builder
    }

    private fun isCommand(opt: String): String? {
        if (!builder.command.isSet) {
            if (opt == "version") {
                return "info-version"
            }
            if (opt in validCommands) {
                return opt
            }
        }

        val command = builder.command.get()
        if (command == "info") {
            return when (opt) {
                "version" -> "info-version"
                "mimetype" -> "info-mimetype"
                "mimetypes" -> "info-mimetypes"
                else -> null
            }
        }

        if (!builder.commandOptions.isSet && command == "info-mimetype") {
            builder.commandOptions.add(opt)
            return command
        }

        return null
    }

    private fun split(arg: String, type: String, defaultName: String? = null): Pair<String, String> {
        val pos = arg.indexOf("=")
        if (pos < 0) {
            throw XProcError.xiCliMalformedOption(type, arg).exception()
        }
        if (pos == 0) {
            if (defaultName != null) {
                return Pair(defaultName, arg.substring(pos+1).trim())
            }
            throw XProcError.xiCliMalformedOption(type, arg).exception()
        }
        return Pair(arg.substring(0, pos).trim(), arg.substring(pos + 1).trim())
    }

    private fun splitIO(arg: String, type: String, defaultName: String? = null): Pair<String?, String> {
        val pos = arg.indexOf("=")
        if (pos < 0) {
            return Pair(null, arg)
        }
        return split(arg, type, defaultName)
    }

    private fun parseInput(arg: String) {
        // --input-multiplex:uri
        val (portspec, href) = splitIO(arg, "input", "")
        var port: String? = portspec
        var contentType = MediaType.ANY
        if (portspec != null && portspec.contains("@")) {
            val index = portspec.indexOf("@")
            contentType = MediaType.parse(portspec.substring(0, index))
            port = port!!.substring(index + 1).trim()
        }

        if (port == "") {
            port = null
        }

        val input = if (href == STDIO_NAME) {
            XmlCalabashInput(port, STDIO_URI, contentType)
        } else {
            XmlCalabashInput(port, UriUtils.resolve(href), contentType)
        }
        builder.inputs.add(input)
    }

    private fun parseInputMultiplex(arg: String) {
        if (arg.indexOf("@") > 0 && arg.indexOf("@") < arg.indexOf("=")) {
            throw XProcError.xiCliMalformedOption("input-multiplex", arg.substring(0, arg.indexOf("="))).exception()
        }

        val pos = arg.indexOf("?")
        val href = when (pos) {
            -1 -> arg
            0 -> throw XProcError.xiCliMalformedOption("input", arg).exception()
            else -> arg.substring(0, pos)
        }

        val input = if (href == STDIO_NAME) {
            XmlCalabashInput(null, STDIO_URI, MediaType.MULTIPART_MIXED)
        } else {
            XmlCalabashInput(null, UriUtils.resolve(href), MediaType.MULTIPART_MIXED)
        }

        input.multiplex = true
        if (pos > 0) {
            // I think the user wants to write source=result, meaning that the source port should
            // come from the port labeled result. Of course, in reality, what I want in the mapping
            // is result=source, rename result to source...
            val maplist = arg.substring(pos+1).split(";")
            for (map in maplist) {
                if (map.trim().isEmpty()) {
                    continue
                }
                val mapping = map.split("=")
                if (mapping.size != 2 || mapping[0].isEmpty() || mapping[1].isEmpty()) {
                    throw XProcError.xiCliMalformedOption("input", arg).exception()
                }
                // ...that's why this is "backwards".
                input.multiplexMapping[mapping[1]] = mapping[0]
            }
        }

        builder.inputs.add(input)
    }

    private fun parseOutput(arg: String, multiplex: Boolean = false) {
        // -o:contentType@port=path

        if (multiplex) {
            if ((builder.outputs.getOrDefault() ?: emptyList()).filter { it.multiplex }.isNotEmpty()) {
                throw XProcError.xiCliOnlyOneOutputMultiplex().exception()
            }

            val output = XmlCalabashOutput(null, arg, true, true)
            builder.outputs.add(output)

            if (arg == STDIO_NAME) {
                builder.pipedMode.set(true)
            }

            return
        }

        val (portspec, filename) = splitIO(arg, "output", "")
        var port: String? = portspec
        var multipartMixed = multiplex
        if (portspec != null && portspec.contains("@")) {
            val index = portspec.indexOf("@")
            val contentType = portspec.substring(0, index)
            if (contentType == "multipart/mixed") {
                multipartMixed = true
            } else {
                throw XProcError.xiCliInvalidOutputMediaType(contentType).exception()
            }
            port = port!!.substring(index + 1).trim()

            if (filename == STDIO_NAME) {
                builder.pipedMode.set(true)
            }
        }

        if (port == "") {
            port = null
        }

        builder.outputs.add(XmlCalabashOutput(port,filename, multipartMixed, multiplex))
    }

    private fun parseManifest(arg: String) {
        // -m:path
        builder.manifest.set(XmlCalabashOutput(null, arg))
    }

    private fun parseTemporaryFiles(arg: String) {
        // --temporary-files:path
        builder.temporaryFiles.set(arg.trim())
    }

    private fun parseNamespace(arg: String) {
        // -ns:path or -ns:prefix=path
        val eqpos = arg.indexOf("=")
        val prefix = if (eqpos >= 0) {
            arg.substring(0, eqpos)
        } else {
            ""
        }
        val uri = if (eqpos >= 0) {
            arg.substring(eqpos + 1)
        } else {
            arg
        }

        if (seenNamespaces.contains(prefix)) {
            throw XProcError.xiCliDuplicateNamespace(prefix).exception()
        }

        seenNamespaces.add(prefix)
        builder.namespaces.put(prefix, NamespaceUri.of(uri))
    }

    private fun parseXmlSchema(arg: String) {
        val uri = URI(arg)
        if (uri.isAbsolute) {
            builder.xmlSchemas.add(uri)
        } else {
            builder.xmlSchemas.add(UriUtils.resolve(uri))
        }
    }

    private fun parseCatalog(arg: String) {
        val uri = URI(arg)
        if (uri.isAbsolute) {
            builder.xmlCatalogs.add(uri)
        } else {
            builder.xmlCatalogs.add(UriUtils.cwdAsUri().resolve(uri))
        }
    }

    private fun parseValidationMode(arg: String) {
        when (arg) {
            "strict" -> builder.validationMode.set(ValidationMode.STRICT)
            "lax" -> builder.validationMode.set(ValidationMode.LAX)
            else -> throw XProcError.xiCliInvalidValue("--validation-mode", arg).exception()
        }
    }

    private fun parseExtensionName(arg: String) {
        when (arg) {
            "eager-uri-resolution" -> builder.extensions.add(ExtensionName.EAGER_URI_RESOLUTION)
            else -> throw XProcError.xiCliInvalidValue("--extension-name", arg).exception()
        }
    }

    private fun parseSerializationParam(arg: String) {
        val (opt, value) = split(arg.substring(1), "serialization")
        val port = if (opt.contains("::")) {
            opt.substring(0, opt.indexOf("::"))
        } else {
            "*"
        }
        val name = if (opt.contains("::")) {
            opt.substring(opt.indexOf("::")+2)
        } else {
            opt
        }

        val map = mutableMapOf<String, String>()
        map.putAll(builder.outputSerialization.get(port) ?: emptyMap())
        map[name] = value
        builder.outputSerialization.put(port, map)
    }

    private fun parseOptionParam(arg: String) {
        val (name, value) = split(arg, "option")
        val values = mutableListOf<Any>()
        values.addAll(builder.options.get(name) ?: emptyList())
        values.add(value)
        builder.options.put(name, values)
    }

    private fun parseVisualizer(arg: String) {
        val pos = arg.indexOf("?")
        val name = if (pos >= 0) {
            arg.substring(0, pos).trim()
        } else {
            arg
        }
        val opts = if (pos >= 0) {
            arg.substring(pos + 1).trim()
        } else {
            ""
        }

        // Cheap and cheerful. And keep it that way.
        val voptions = mutableMapOf<String, String>()
        if (opts.trim().isNotEmpty()) {
            for (nvpair in opts.split(";")) {
                val eqpos = nvpair.indexOf("=")
                if (eqpos <= 0) {
                    throw XProcError.xiCliInvalidValue("--visualizer", arg).exception()
                }
                val key = nvpair.substring(0, eqpos).trim()
                val value = nvpair.substring(eqpos + 1).trim()
                voptions[key] = value
            }
        }

        if (name !in listOf("silent", "plain", "detail")) {
            throw XProcError.xiCliInvalidValue("--visualizer", arg).exception()
        }

        builder.visualizerName.set(name)
        if (voptions.isEmpty()) {
            builder.visualizerName.options = null
        } else {
            builder.visualizerName.options = voptions
        }
    }

    private fun parseVerbosity(arg: String) {
        builder.verbosity.set(when(arg) {
            "error" -> Verbosity.ERROR
            "warn" -> Verbosity.WARN
            "info" -> Verbosity.INFO
            "debug" -> Verbosity.DEBUG
            "trace" -> Verbosity.TRACE
            else -> Verbosity.INFO
        })
    }

    private fun parseAssertions(arg: String) {
        builder.assertions.set(when(arg) {
            "ignore" -> AssertionsLevel.IGNORE
            "warn", "warning" -> AssertionsLevel.WARNING
            "error" -> AssertionsLevel.ERROR
            else -> AssertionsLevel.IGNORE
        })
    }

    internal class ArgumentDescription(val name: String,
                                       val synonyms: List<String>,
                                       val type: ArgumentType,
                                       val default: String? = null,
                                       val valid: List<String> = listOf(),
                                       val process: (String) -> Unit) {
        override fun toString(): String {
            return "${name}: ${type}"
        }
    }

    internal enum class ArgumentType {
        STRING, URI, FILE, EXISTING_FILE, DIRECTORY, BOOLEAN
    }
}