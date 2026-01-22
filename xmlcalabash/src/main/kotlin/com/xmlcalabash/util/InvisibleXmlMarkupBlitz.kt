package com.xmlcalabash.util

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.resourcecache.CompiledMarkupBlitzResource
import com.xmlcalabash.resourcecache.CompiledResourceType
import com.xmlcalabash.runtime.XProcStepConfiguration
import de.bottlecaps.markup.Blitz
import de.bottlecaps.markup.BlitzException
import de.bottlecaps.markup.blitz.Parser
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import java.io.ByteArrayInputStream
import java.net.URI

class InvisibleXmlMarkupBlitz(stepConfig: XProcStepConfiguration, stepName: String, cacheGrammar: Boolean): InvisibleXmlImpl(stepConfig, "markup-blitz", stepName, cacheGrammar) {
    companion object {
        private var invisibleXml: String? = null
    }

    override fun parse(grammarUri: URI?, grammar: XdmNode, input: String, failOnError: Boolean, parameters: Map<QName, XdmValue>): XProcDocument {
        throw stepConfig.exception(XProcError.xdStepFailed("Cannot process XML grammars with Markup Blitz"))
    }

    override fun parse(grammarUri: URI?, grammar: String?, input: String, failOnError: Boolean, parameters: Map<QName, XdmValue>): XProcDocument {
        val ixmlGrammar = if (grammar == null) {
            if (invisibleXml == null) {
                val ixmlStream = InvisibleXmlMarkupBlitz::class.java.getResourceAsStream("/com/xmlcalabash/mb-ixml.ixml")
                invisibleXml = ixmlStream!!.readAllBytes().toString(Charsets.UTF_8)
            }
            invisibleXml!!
        } else {
            grammar
        }

        val blitzOptions = mutableListOf<Blitz.Option>()
        var parser: Parser? = null

        if (cacheGrammar && grammarUri != null) {
            if (stepConfig.compiledResourceCache.contains(stepName, grammarUri)) {
                val rsrc = stepConfig.compiledResourceCache.get(stepName, grammarUri)!!
                if (rsrc.type == CompiledResourceType.IXMLMB) {
                    val mbrsrc = rsrc as CompiledMarkupBlitzResource
                    parser = mbrsrc.parser
                    blitzOptions.addAll(mbrsrc.options)
                }
            }
        }

        if (parser == null) {
            if (failOnError) {
                blitzOptions.add(Blitz.Option.FAIL_ON_ERROR)
            }

            for ((name, value) in parameters) {
                val bool = value.underlyingValue.effectiveBooleanValue()
                if (bool) {
                    when (name) {
                        Ns.indent -> blitzOptions.add(Blitz.Option.INDENT)
                        Ns.trace -> blitzOptions.add(Blitz.Option.TRACE)
                        Ns.timing -> blitzOptions.add(Blitz.Option.TIMING)
                        Ns.verbose -> blitzOptions.add(Blitz.Option.VERBOSE)
                        else -> {
                            stepConfig.warn { "Ignoring unknown parameter: ${name}"}
                        }
                    }
                }
            }

            parser = try {
                Blitz.generate(ixmlGrammar, *blitzOptions.toTypedArray())
            } catch (ex: BlitzException) {
                throw stepConfig.exception(XProcError.xcInvalidIxmlGrammar(), ex)
            }

            if (cacheGrammar && grammarUri != null) {
                stepConfig.compiledResourceCache.put(stepName, grammarUri, CompiledMarkupBlitzResource(parser, blitzOptions))
            }
        }

        val xml = try {
            parser.parse(input, *blitzOptions.toTypedArray())
        } catch (ex: BlitzException) {
            throw stepConfig.exception(XProcError.xcInvisibleXmlParseFailed(), ex)
        }

        val loader = DocumentLoader(stepConfig, null)
        return loader.load(ByteArrayInputStream(xml.toByteArray()), MediaType.XML)
    }
}