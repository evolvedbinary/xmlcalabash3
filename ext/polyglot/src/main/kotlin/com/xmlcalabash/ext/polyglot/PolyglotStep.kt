package com.xmlcalabash.ext.polyglot

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import net.sf.saxon.value.*
import org.apache.logging.log4j.kotlin.logger
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.EnvironmentAccess
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.PolyglotAccess
import org.graalvm.polyglot.PolyglotException
import org.graalvm.polyglot.SandboxPolicy
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.io.IOAccess
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

class PolyglotStep(val stepLanguage: String): AbstractAtomicStep() {
    companion object {
        val ns_variables = QName("variables")
        val ns_options = QName("options")
        val ns_allowHostAccess = QName("allowHostAccess")
        val ns_allowNativeAccess = QName("allowNativeAccess")
        val ns_allowCreateThread = QName("allowCreateThread")
        val ns_allowAllAccess = QName("allowAllAccess")
        val ns_allowExperimentalOptions = QName("allowExperimentalOptions")
        val ns_allowPolyglotAccess = QName("allowPolyglotAccess")
        val ns_allowValueSharing = QName("allowValueSharing")
        val ns_allowInnerContextOptions = QName("allowInnerContextOptions")
        val ns_allowIO = QName("allowIO")
        val ns_allowCreateProcess = QName("allowCreateProcess")
        val ns_sandbox = QName("sandbox")
        val ns_allowEnvironmentAccess = QName("allowEnvironmentAccess")
        val ns_environment = QName("environment")
    }

    lateinit var jscontext: Context
    lateinit var language: String

    override fun run() {
        super.run()

        language = when (stepLanguage) {
            "javascript" -> "js"
            "dynamic" -> stringBinding(QName("language"))!!
            else -> stepLanguage
        }

        if ((queues["source"]?.size ?: 0) > 1) {
            throw stepConfig.exception(XProcError.xiTooManySources(queues["source"]!!.size))
        }

        val source = queues["source"]?.firstOrNull()
        val program = queues["program"]!!.first().value.underlyingValue.stringValue
        val polyglotVariables = stringItemMapBinding(ns_variables)
        val polyglotParameters = qnameMapBinding(Ns.parameters)
        val polyglotOptions = stringMapBinding(ns_options)

        val ctype = options[Ns.resultContentType]?.value
        val resultContentType = if (ctype == null || ctype === XdmEmptySequence.getInstance()) {
            null
        } else {
            mediaTypeBinding(Ns.resultContentType)
        }

        val args = mutableListOf<String>()
        args.add(stepConfig.baseUri?.toString() ?: "")
        if (options[Ns.args] != null) {
            for (arg in options[Ns.args]!!.value) {
                args.add(arg.toString())
            }
        }

        val inputStream = if (source == null) {
            ByteArrayInputStream("".toByteArray(StandardCharsets.UTF_8))
        } else {
            val baos = ByteArrayOutputStream()
            DocumentWriter(source, baos).write()
            ByteArrayInputStream(baos.toByteArray())
        }
        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()

        var builder = Context.newBuilder(language)
        options[Ns.cwd]?.let { builder = builder.currentWorkingDirectory(Paths.get(it.value.toString())) }
        for ((name, value) in polyglotParameters) {
            when (name) {
                ns_allowHostAccess -> {
                    when (value.toString()) {
                        "ALL" -> builder = builder.allowHostAccess(HostAccess.ALL)
                        "EXPLICIT" -> builder = builder.allowHostAccess(HostAccess.EXPLICIT)
                        "NONE" -> builder = builder.allowHostAccess(HostAccess.NONE)
                        else -> stepConfig.warn { "Unrecognized host access: $value (ignored)" }
                    }
                }
                ns_allowPolyglotAccess -> {
                    when (value.toString()) {
                        "ALL" -> builder = builder.allowPolyglotAccess(PolyglotAccess.ALL)
                        "NONE" -> builder = builder.allowPolyglotAccess(PolyglotAccess.NONE)
                        else -> stepConfig.warn { "Unrecognized polyglot access: $value (ignored)" }
                    }
                }
                ns_allowIO -> {
                    when (value.toString()) {
                        "ALL" -> builder = builder.allowIO(IOAccess.ALL)
                        "NONE" -> builder = builder.allowIO(IOAccess.NONE)
                        else -> stepConfig.warn { "Unrecognized allow IO: $value (ignored)" }
                    }
                }
                ns_sandbox -> {
                    when (value.toString()) {
                        "TRUSTED" -> builder = builder.sandbox(SandboxPolicy.TRUSTED)
                        "CONSTRAINED" -> builder = builder.sandbox(SandboxPolicy.CONSTRAINED)
                        "ISOLATED" -> builder = builder.sandbox(SandboxPolicy.ISOLATED)
                        "UNTRUSTED" -> builder = builder.sandbox(SandboxPolicy.UNTRUSTED)
                        else -> stepConfig.warn { "Unrecognized sandbox: $value (ignored)" }
                    }
                }
                ns_allowEnvironmentAccess -> {
                    when (value.toString()) {
                        "NONE" -> builder = builder.allowEnvironmentAccess(EnvironmentAccess.NONE)
                        "INHERIT" -> builder = builder.allowEnvironmentAccess(EnvironmentAccess.INHERIT)
                        else -> stepConfig.warn { "Unrecognized allow environment access: $value (ignored)" }
                    }
                }
                ns_allowNativeAccess -> builder = builder.allowNativeAccess(asBoolean(value))
                ns_allowCreateThread -> builder = builder.allowCreateThread(asBoolean(value))
                ns_allowAllAccess -> builder = builder.allowAllAccess(asBoolean(value))
                ns_allowExperimentalOptions -> builder = builder.allowExperimentalOptions(asBoolean(value))
                ns_allowValueSharing -> builder = builder.allowValueSharing(asBoolean(value))
                ns_allowInnerContextOptions -> builder = builder.allowInnerContextOptions(asBoolean(value))
                ns_allowCreateProcess -> builder = builder.allowCreateProcess(asBoolean(value))
                ns_environment -> {
                    if (value is XdmMap) {
                        val envMap = mutableMapOf<String, String>()
                        val map = stepConfig.typeUtils.asGenericMap(value)
                        for ((key, value) in map) {
                            envMap[key.stringValue] = value.underlyingValue.stringValue
                        }
                        builder = builder.environment(envMap)
                    } else {
                        stepConfig.warn { "The environment must be a map of strings (ignored)"}
                    }
                }
                else -> {
                    stepConfig.warn { "Unrecognized polyglot parameter: $name (ignored)" }
                }
            }
        }

        if (polyglotOptions.isNotEmpty()) {
            builder = builder.options(polyglotOptions)
        }

        jscontext = builder
            .arguments(language, args.toTypedArray())
            .`in`(inputStream)
            .out(outputStream)
            .err(errorStream).build()

        try {
            val jsbindings = jscontext.getBindings(language)
            for ((name, value) in polyglotVariables) {
                jsbindings.putMember(name, convertToValue(value))
            }

            val jsvalue = try {
                jscontext.parse(language, program)
            } catch (ex: Exception) {
                if (ex is PolyglotException) {
                    throw stepConfig.exception(XProcError.xdStepFailed("error interpreting ${language} input"), ex)
                }
                throw ex
            }

            if (!jsvalue.canExecute()) {
                throw stepConfig.exception(XProcError.xdStepFailed("${language} input was not executable"))
            }

            if (resultContentType != null) {
                jsvalue.executeVoid()

                messages(String(errorStream.toByteArray()), true)

                val properties = DocumentProperties()
                properties[Ns.contentType] = resultContentType
                val loader = DocumentLoader(stepConfig, null, properties, emptyMap())
                val doc = loader.load(ByteArrayInputStream(outputStream.toByteArray()), resultContentType)
                receiver.output("result", doc)
            } else {
                val value = jsvalue.execute()

                messages(String(errorStream.toByteArray()), true)
                messages(String(outputStream.toByteArray()), false)

                logger.debug { "Polyglot return value is ${value}" }
                val result = convertFromValue(value)
                receiver.output("result", XProcDocument.ofValue(result, stepConfig, resultContentType))
            }
        } catch (ex: Exception) {
            when (ex) {
                is XProcException -> throw ex
                is PolyglotException -> throw stepConfig.exception(XProcError.xdStepFailed("${language} evaluation failed or signaled an error"), ex)
                else -> throw stepConfig.exception(XProcError.xdStepFailed(ex.message ?: "(no explanation"), ex)
            }
        } finally {
            jscontext.close()
        }
    }

    override fun toString(): String {
        return "cx:${stepLanguage}"
    }

    private fun messages(text: String, warning: Boolean) {
        for (line in text.split("\n+".toRegex())) {
            if (line != "") {
                if (warning) {
                    stepConfig.warn { line }
                } else {
                    stepConfig.info { line }
                }
            }
        }

    }

    private fun convertToValue(xdmValue: XdmValue): Value {
        if (xdmValue === XdmEmptySequence.getInstance()) {
            return jscontext.asValue(null)
        }

        when (xdmValue) {
            is XdmAtomicValue -> {
                val uvalue = xdmValue.underlyingValue
                when (uvalue) {
                    is StringValue -> return jscontext.asValue(uvalue.stringValue)
                    is IntegerValue -> return jscontext.asValue(uvalue.longValue())
                    is DoubleValue -> return jscontext.asValue(uvalue.doubleValue)
                    is FloatValue -> return jscontext.asValue(uvalue.floatValue)
                    is BooleanValue -> return jscontext.asValue(uvalue.booleanValue)
                    is DecimalValue -> return jscontext.asValue(uvalue.decimalValue)
                    is DateTimeValue -> return jscontext.asValue(uvalue.toZonedDateTime())
                    is DateValue -> return jscontext.asValue(uvalue.toLocalDate())
                    is QNameValue -> return jscontext.asValue(uvalue.eqName)
                    else -> {
                        stepConfig.warn { "Unexpected atomic value: ${uvalue}" }
                        return jscontext.asValue(uvalue.toString())
                    }
                }
            }
            is XdmMap -> {
                val map = mutableMapOf<String, Value>()
                for ((key, xvalue) in xdmValue.entrySet()) {
                    val name = key.stringValue
                    val value = convertToValue(xvalue)
                    map[name] = value
                }
                return jscontext.asValue(map)
            }
            is XdmArray -> {
                return jscontext.asValue(xdmValue.asList().toTypedArray())
            }
            is XdmNode -> {
                val tempDoc = XProcDocument.ofXml(xdmValue, stepConfig)
                val baos = ByteArrayOutputStream()
                val writer = DocumentWriter(tempDoc, baos)
                writer[Ns.omitXmlDeclaration] = true
                writer.write()
                return jscontext.asValue(baos.toString(StandardCharsets.UTF_8))
            }
            else -> {
                stepConfig.warn { "Unexpected value: ${xdmValue}" }
                return jscontext.asValue(xdmValue.underlyingValue.stringValue)
            }
        }
    }

    private fun convertFromValue(value: Value, seen: MutableSet<Value> = mutableSetOf()): XdmValue {
        // The Gradle engine supports "isDuration()" and "isTime()" but I haven't worked
        // out how to get them back from the host language so they're unsupported at the moment.

        if (value.isBoolean) {
            return XdmAtomicValue(value.asBoolean())
        } else if (value.isInstant) {
            return XdmAtomicValue(value.asInstant())
        } else if (value.isDate) {
            return XdmAtomicValue(value.asDate())
        } else if (value.isException) {
            throw value.throwException()
        } else if (value.isNull) {
            return XdmEmptySequence.getInstance()
        } else if (value.isNumber) {
            if (value.fitsInLong()) {
                return XdmAtomicValue(value.asLong())
            } else {
                return XdmAtomicValue(value.asDouble())
            }
        } else if (value.isString) {
            return XdmAtomicValue(value.asString())
        }

        if (value.hasArrayElements()) {
            var array = XdmArray()
            for (index in 0 ..< value.arraySize) {
                val avalue = value.getArrayElement(index)
                if (avalue !in seen) {
                    seen.add(avalue)
                    array = array.addMember(convertFromValue(avalue, seen))
                }
            }
            return array
        }

        if (value.hasHashEntries()) {
            var map = XdmMap()
            val hashKeysIterator = value.hashKeysIterator
            while (hashKeysIterator.hasIteratorNextElement()) {
                val keyValue = hashKeysIterator.iteratorNextElement
                val entryValue = value.getHashValue(keyValue)
                if (keyValue !in seen && entryValue !in seen) {
                    seen.add(keyValue)
                    val key = convertFromValue(keyValue, seen)
                    if (key is XdmAtomicValue) {
                        seen.add(entryValue)
                        val entry = convertFromValue(entryValue, seen)
                        map = map.put(key, entry)
                    }
                }

            }
            return map
        }

        if (value.hasMembers()) {
            var map = XdmMap()
            for (key in value.memberKeys) {
                val kvalue = value.getMember(key)
                if (kvalue !in seen) {
                    seen.add(kvalue)
                    val xvalue = convertFromValue(kvalue, seen)
                    map = map.put(XdmAtomicValue(key), xvalue)
                }
            }
            return map
        }

        stepConfig.warn { "Unconvertable value: ${value}" }
        return XdmAtomicValue(value.toString())
    }

    private fun asBoolean(value: XdmValue): Boolean {
        when (value.toString()) {
            "true" -> return true
            "false" -> return false
            else -> {
                stepConfig.warn { "Unexpected value for boolean: $value (assuming false)" }
                return false
            }
        }
    }
}