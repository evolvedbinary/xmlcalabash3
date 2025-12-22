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
        ArgumentDescription("--input", listOf("-i"), ArgumentType.STRING) {
            parseInput(it) },
        ArgumentDescription("--output", listOf("-o"), ArgumentType.STRING) {
            parseOutput(it) },
        ArgumentDescription("--namespace", listOf("-ns"), ArgumentType.STRING) {
            parseNamespace(it) },
        ArgumentDescription("--xml-schema", listOf("--xsd"), ArgumentType.URI) {
            parseXmlSchema(it) },
        ArgumentDescription("--validation-mode", listOf("--val"), ArgumentType.STRING, "strict") {
            parseValidationMode(it) },
        ArgumentDescription("--use-location-hints", listOf("--hints"), ArgumentType.BOOLEAN, "true") {
            builder.useLocationHints.set(it == "true") },
        ArgumentDescription("--try-namespaces", listOf("--try-ns"), ArgumentType.BOOLEAN, "true") {
            builder.tryNamespaces.set(it == "true") },
        ArgumentDescription("--catalog", listOf(), ArgumentType.URI) {
            parseCatalog(it) },
        ArgumentDescription("--init", listOf(), ArgumentType.STRING) {
            builder.initializers.add(Pair(it, false)) },
        ArgumentDescription("--configuration", listOf("-c", "--config"), ArgumentType.EXISTING_FILE) {
            builder.configurationFile.set(File(it)) },
        ArgumentDescription("--step", listOf("-s"), ArgumentType.STRING) {
            builder.step.set(it) },
        ArgumentDescription("--graphs", listOf(), ArgumentType.DIRECTORY) {
            builder.graphs.set(File(it)) },
        ArgumentDescription("--licensed", listOf(), ArgumentType.BOOLEAN, "true") {
            builder.licensed.set(it == "true") },
        ArgumentDescription("--pipe", listOf(), ArgumentType.BOOLEAN, "true") {
            builder.pipedMode.set(it == "true") },
        ArgumentDescription("--debug", listOf("-D"), ArgumentType.BOOLEAN, "true") {
            builder.debug.set(it == "true") },
        ArgumentDescription("--debugger", listOf(), ArgumentType.BOOLEAN, "true") {
            builder.debugger.set(it == "true") },
        ArgumentDescription("--explain", listOf(), ArgumentType.BOOLEAN, "true") {
            builder.explainErrors.set(it == "true") },
        ArgumentDescription("--help", listOf(), ArgumentType.BOOLEAN, "true") {
            _help = it == "true" },
        ArgumentDescription("--trace", listOf(), ArgumentType.FILE) {
            builder.trace.set(File(it)) },
        ArgumentDescription("--nogo", listOf(), ArgumentType.BOOLEAN, "true") {
            builder.go.set(it != "true") },
        ArgumentDescription("--trace-documents", listOf("--trace-docs"), ArgumentType.DIRECTORY) {
            builder.traceDocuments.set(File(it)) },
        ArgumentDescription("--stacktrace", listOf("--stack-trace"), ArgumentType.BOOLEAN, "true") {
            builder.stacktrace.set(it == "true") },
        ArgumentDescription("--extension", listOf("-X"), ArgumentType.STRING) {
            parseExtensionName(it) },
        ArgumentDescription("--verbosity", listOf("-V"),
            ArgumentType.STRING, "info", listOf("trace", "debug", "info", "warn", "error")) {
            builder.verbosity.set(when(it) {
                "error" -> Verbosity.ERROR
                "warn" -> Verbosity.WARN
                "info" -> Verbosity.INFO
                "debug" -> Verbosity.DEBUG
                "trace" -> Verbosity.TRACE
                else -> Verbosity.INFO
            })},
        ArgumentDescription("--assertions", listOf(),
            ArgumentType.STRING, "warn", listOf("ignore", "warn", "warning", "error")) {
            builder.assertions.set(when(it) {
                "ignore" -> AssertionsLevel.IGNORE
                "warn", "warning" -> AssertionsLevel.WARNING
                "error" -> AssertionsLevel.ERROR
                else -> AssertionsLevel.IGNORE
            })},
        ArgumentDescription("--visualizer", listOf("--vis"),
            ArgumentType.STRING, "plain", emptyList()) { parseVisualizer(it) }
        )

    private fun parse(): XmlCalabashBuilder {
        builder = XmlCalabashBuilder()

        if (args.isEmpty()) {
            builder.command.set("help")
            return builder
        }

        for (opt in args) {
            val pos = opt.indexOf(':')
            val option = if (pos >= 0) {
                opt.substring(0, pos)
            } else {
                opt
            }
            val suppliedValue = if (pos >= 0) {
                opt.substring(pos + 1)
            } else {
                null
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
        if (pos <= 0) {
            if (defaultName != null) {
                return Pair(defaultName, arg)
            }
            throw XProcError.xiCliMalformedOption(type, arg).exception()
        }
        return Pair(arg.substring(0, pos).trim(), arg.substring(pos + 1).trim())
    }

    private fun parseInput(arg: String) {
        // -i:contentType@port=path
        val (portspec, href) = split(arg, "input", "*anonymous")
        var port = portspec
        var contentType = MediaType.ANY
        if (portspec.contains("@")) {
            val index = portspec.indexOf("@")
            contentType = MediaType.parse(portspec.substring(0, index))
            port = port.substring(index + 1).trim()
        }

        val inputs = mutableListOf<XmlCalabashInput>()
        inputs.addAll(builder.inputs.get(port) ?: mutableListOf())
        val input = if (href == STDIO_NAME) {
            XmlCalabashInput(STDIO_URI, contentType)
        } else {
            XmlCalabashInput(UriUtils.resolve(href), contentType)
        }
        inputs.add(input)
        builder.inputs.put(port, inputs)
    }

    private fun parseOutput(arg: String) {
        // -i:port=path
        val (port, filename) = split(arg, "output", "*anonymous")
        val output = builder.outputs.get(port)
        if (output != null) {
            throw XProcError.xiCliDuplicateOutputFile(filename).exception()
        }
        builder.outputs.put(port, XmlCalabashOutput(filename))
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

    internal class ArgumentDescription(val name: String,
                                       val synonyms: List<String>,
                                       val type: ArgumentType,
                                       val default: String? = null,
                                       val valid: List<String> = listOf(),
                                       val process: (String) -> Unit)
    internal enum class ArgumentType {
        STRING, URI, FILE, EXISTING_FILE, DIRECTORY, BOOLEAN
    }
}