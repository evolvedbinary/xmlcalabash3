package com.xmlcalabash.app.test

import com.xmlcalabash.config.XmlCalabashInput
import com.xmlcalabash.XmlCalabashBuilder
import com.xmlcalabash.config.XmlCalabashOutput
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsFn
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.namespace.NsSaxon
import com.xmlcalabash.namespace.NsXml
import com.xmlcalabash.namespace.NsXs
import com.xmlcalabash.util.AssertionsLevel
import com.xmlcalabash.util.ExtensionName
import com.xmlcalabash.util.Verbosity
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import java.net.URI
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

class BuilderConfiguration(config: XmlCalabashBuilder) {
    companion object {
        val defaultNamespaces = mapOf<String, NamespaceUri>(
            "cx" to NsCx.namespace,
            "p" to NsP.namespace,
            "xs" to NsXs.namespace,
            "fn" to NsFn.namespace,
            "map" to NsFn.mapNamespace,
            "array" to NsFn.arrayNamespace,
            "math" to NsFn.mathNamespace,
            "saxon" to NsSaxon.namespace,
            "xml" to NsXml.namespace
        )
    }

    val configValues = mapOf<String, Any?>(
        "additionalMimeTypeMappings" to config.additionalMimeTypeMappings.getOrDefault(),
        "assertions" to config.assertions.getOrDefault()!!,
        "command" to config.command.getOrDefault(),
        "commandOptions" to config.commandOptions.getOrDefault(),
        "configurationFile" to config.configurationFile.getOrDefault(),
        "configuredXQueryProcessors" to config.configuredXQueryProcessors.getOrDefault(),
        "configurers" to config.configurers.getOrDefault(),
        "cssFormatter" to config.cssFormatter.getOrDefault(),
        "debug" to config.debug.getOrDefault(),
        "debugger" to config.debugger.getOrDefault(),
        "defaultXQueryProcessor" to config.defaultXQueryProcessor.getOrDefault(),
        "documentManager" to config.documentManager.getOrDefault(),
        "eagerEvaluation" to config.eagerEvaluation.getOrDefault(),
        "errorExplanation" to config.errorExplanation.getOrDefault(),
        "explainErrors" to config.explainErrors.getOrDefault(),
        "extensions" to config.extensions.getOrDefault(),
        "go" to config.go.getOrDefault(),
        "graphStyle" to config.graphStyle.getOrDefault(),
        "graphs" to config.graphs.getOrDefault(),
        "graphviz" to config.graphviz.getOrDefault(),
        "implicitParameterName" to config.implicitParameterName.getOrDefault(),
        "initializers" to config.initializers.getOrDefault(),
        "inlineTrimWhitespace" to config.inlineTrimWhitespace.getOrDefault(),
        "inputs" to config.inputs.getOrDefault(),
        "licensed" to config.licensed.getOrDefault(),
        "lineNumbering" to config.lineNumbering.getOrDefault(),
        "maxThreadCount" to config.maxThreadCount.getOrDefault(),
        "messagePrinter" to config.messagePrinter.getOrDefault(),
        "messageReporter" to config.messageReporter.getOrDefault(),
        "messageReporterBufferSize" to config.messageReporterBufferSize.getOrDefault(),
        "mimeTypes" to config.mimeTypes.getOrDefault(),
        "mpt" to config.mpt.getOrDefault(),
        "namespaces" to config.namespaces.getOrDefault(),
        "options" to config.options.getOrDefault(),
        "other" to config.other.getOrDefault(),
        "outputSerialization" to config.outputSerialization.getOrDefault(),
        "outputs" to config.outputs.getOrDefault(),
        "pagedMediaCssProcessors" to config.pagedMediaCssProcessors.getOrDefault(),
        "pagedMediaManagers" to config.pagedMediaManagers.getOrDefault(),
        "pagedMediaXslProcessors" to config.pagedMediaXslProcessors.getOrDefault(),
        "pipedMode" to config.pipedMode.getOrDefault(),
        "pipelineUri" to config.pipelineUri.getOrDefault(),
        "proxies" to config.proxies.getOrDefault(),
        "saxonConfigurationFile" to config.saxonConfigurationFile.getOrDefault(),
        "saxonConfigurationProperties" to config.saxonConfigurationProperties.getOrDefault(),
        "sendmail" to config.sendmail.getOrDefault(),
        "serialization" to config.serialization.getOrDefault(),
        "stacktrace" to config.stacktrace.getOrDefault(),
        "step" to config.step.getOrDefault(),
        "trace" to config.trace.getOrDefault(),
        "traceDocuments" to config.traceDocuments.getOrDefault(),
        "tryNamespaces" to config.tryNamespaces.getOrDefault(),
        "uniqueInlineUris" to config.uniqueInlineUris.getOrDefault(),
        "useLocationHints" to config.useLocationHints.getOrDefault(),
        "validationMode" to config.validationMode.getOrDefault(),
        "verbosity" to config.verbosity.getOrDefault(),
        "visualizer" to mapOf("name" to config.visualizerName.getOrDefault(), "options" to config.visualizerName.options),
        "xmlCatalogs" to config.xmlCatalogs.getOrDefault(),
        "xmlSchemas" to config.xmlSchemas.getOrDefault(),
        "xslFormatter" to config.xslFormatter.getOrDefault(),
    )

    fun assertDefaults(): List<String> {
        val diffs = mutableListOf<String>()

        for ((name, value) in configValues) {
            if (name == "namespaces") {
                @Suppress("UNCHECKED_CAST")
                val ns = value as Map<String, NamespaceUri>
                if (ns.size != defaultNamespaces.size) {
                    diffs.add(name)
                } else {
                    for ((prefix, uri) in defaultNamespaces) {
                        if (uri != ns[prefix]) {
                            diffs.add(name)
                            break;
                        }
                    }
                }
                continue
            }

            val expected: Any? = when (name) {
                "assertions" -> AssertionsLevel.WARNING
                "debug" -> false
                "debugger" -> false
                "defaultXQueryProcessor" -> URI.create("https://saxonica.com/")
                "eagerEvaluation" -> false
                "explainErrors" -> false
                "extensions" -> emptyList<ExtensionName>()
                "go" -> true
                "inlineTrimWhitespace" -> false
                "licensed" -> false
                "lineNumbering" -> false
                "maxThreadCount" -> 1
                "messageReporterBufferSize" -> 32
                "mpt" -> 0.99999998
                "pipedMode" -> false
                "stacktrace" -> false
                "uniqueInlineUris" -> true
                "verbosity" -> Verbosity.INFO
                "visualizer" -> mapOf("name" to "silent", "options" to null)
                else -> null
            }

            if (expected != value) {
                diffs.add(name)
            }
        }

        return diffs;
    }

    fun assertConfiguration(map: Map<String, Any?>): List<String> {
        val diffs = mutableSetOf<String>()
        val notDefaults = assertDefaults()

        for ((name, expected) in map) {
            if (expected != configValues[name]) {
                diffs.add(name)
            }
        }

        for (name in notDefaults) {
            if (name !in map && name !in listOf("documentManager", "errorExplanation",
                    "messagePrinter", "messageReporter", "pagedMediaManagers")) {
                diffs.add(name)
            }
        }

        for ((name, value) in map) {
            if (name !in configValues) {
                diffs.add(name)
                continue
            }

            when (name) {
                "inputs" -> {
                    @Suppress("UNCHECKED_CAST")
                    val expected = value as Map<String, List<XmlCalabashInput>>
                    @Suppress("UNCHECKED_CAST")
                    val actual = configValues[name] as Map<String, List<XmlCalabashInput>>

                    if (expected.size != actual.size) {
                        diffs.add(name)
                    } else {
                        for ((port, value) in actual) {
                            if (port in expected) {
                                val elist = expected[port]!!
                                val alist = actual[port]!!
                                if (elist.size != alist.size) {
                                    diffs.add(name)
                                } else {
                                    for ((index, expected) in elist.withIndex()) {
                                        if (expected != alist[index]) {
                                            diffs.add(name)
                                        }
                                    }

                                }
                            } else {
                                diffs.add(port)
                            }
                        }
                    }
                }
                "outputs" -> {
                    @Suppress("UNCHECKED_CAST")
                    val expected = value as Map<String, XmlCalabashOutput>
                    @Suppress("UNCHECKED_CAST")
                    val actual = configValues[name] as Map<String, XmlCalabashOutput>

                    if (expected.size != actual.size) {
                        diffs.add(name)
                    } else {
                        for ((name, value) in actual) {
                            if (expected[name] != value) {
                                diffs.add(name)
                            }
                        }
                    }
                }
                "options" -> {
                    @Suppress("UNCHECKED_CAST")
                    val expected = value as Map<String, List<Any>>
                    @Suppress("UNCHECKED_CAST")
                    val actual = configValues[name] as Map<String, List<Any>>

                    if (expected.size != actual.size) {
                        diffs.add(name)
                    } else {
                        for ((name, value) in actual) {
                            if (name in expected) {
                                val elist = expected[name]!!
                                val alist = actual[name]!!
                                if (elist.size != alist.size) {
                                    diffs.add(name)
                                } else {
                                    for ((index, expected) in elist.withIndex()) {
                                        if (expected != alist[index]) {
                                            diffs.add(name)
                                        }
                                    }

                                }
                            } else {
                                diffs.add(name)
                            }
                        }
                    }
                }
                "namespaces" -> {
                    @Suppress("UNCHECKED_CAST")
                    val expected = value as Map<String, NamespaceUri>
                    @Suppress("UNCHECKED_CAST")
                    val actual = configValues[name] as Map<String, NamespaceUri>

                    if (expected.size != actual.size) {
                        diffs.add(name)
                    } else {
                        for ((name, value) in actual) {
                            if (expected[name] != value) {
                                diffs.add(name)
                            }
                        }
                    }
                }
                "initializers" -> {
                    @Suppress("UNCHECKED_CAST")
                    val expected = value as List<Pair<String,Boolean>>
                    @Suppress("UNCHECKED_CAST")
                    val actual = configValues[name] as List<Pair<String,Boolean>>

                    if (expected.size != actual.size) {
                        diffs.add(name)
                    } else {
                        for ((index, expected) in expected.withIndex()) {
                            if (expected.first != actual[index].first || expected.second != actual[index].second) {
                                diffs.add(name)
                            }
                        }
                    }
                }
                "visualizer" -> {
                    @Suppress("UNCHECKED_CAST")
                    val expected = value as Map<String,*>
                    @Suppress("UNCHECKED_CAST")
                    val actual = configValues[name] as Map<String, *>

                    if ("name" !in expected || expected["name"] != actual["name"]) {
                        diffs.add(name)
                    } else {
                        if (expected["options"] != actual["options"]) {
                            diffs.add(name)
                        }
                    }
                }
                "serialization" -> {
                    @Suppress("UNCHECKED_CAST")
                    val expected = value as Map<MediaType, Map<QName, String>>
                    @Suppress("UNCHECKED_CAST")
                    val actual = configValues[name] as Map<MediaType, Map<QName, String>>

                    if (expected.size != actual.size) {
                        diffs.add(name)
                    } else {
                        for ((ename, emap) in expected) {
                            val amap = actual[ename]!!
                            if (emap.size != amap.size) {
                                diffs.add(name)
                            } else {
                                for ((key, evalue) in emap) {
                                    if (evalue != amap[key]) {
                                        diffs.add(name)
                                    }
                                }
                            }
                        }
                    }
                }
                "configuredXQueryProcessors", "cssFormatter", "xslFormatter" -> {
                    @Suppress("UNCHECKED_CAST")
                    val expected = value as Map<URI, Map<QName, String>>
                    @Suppress("UNCHECKED_CAST")
                    val actual = configValues[name] as Map<URI, Map<QName, String>>

                    if (expected.size != actual.size) {
                        diffs.add(name)
                    } else {
                        for ((ename, emap) in expected) {
                            val amap = actual[ename]!!
                            if (emap.size != amap.size) {
                                diffs.add(name)
                            } else {
                                for ((key, evalue) in emap) {
                                    if (evalue != amap[key]) {
                                        diffs.add(name)
                                    }
                                }
                            }
                        }
                    }
                }
                "other" -> {
                    @Suppress("UNCHECKED_CAST")
                    val expected = value as Map<QName, List<Map<QName, String>>>
                    @Suppress("UNCHECKED_CAST")
                    val actual = configValues[name] as Map<QName, List<Map<QName, String>>>

                    if (expected.size != actual.size) {
                        diffs.add(name)
                    } else {
                        for ((ename, elist) in expected) {
                            val alist = actual[ename]!!
                            if (elist.size != alist.size) {
                                diffs.add(name)
                            } else {
                                for ((index, emap) in elist.withIndex()) {
                                    val amap = alist[index]
                                    if (emap.size != amap.size) {
                                        diffs.add(name)
                                    } else {
                                        for ((key, evalue) in emap) {
                                            if (evalue != amap[key]) {
                                                diffs.add(name)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    if (value != configValues[name]) {
                        diffs.add(name)
                    }
                }
            }
        }

        return diffs.toList()
    }
}