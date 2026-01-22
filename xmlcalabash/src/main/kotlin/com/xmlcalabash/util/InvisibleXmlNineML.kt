package com.xmlcalabash.util

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.resourcecache.CompiledNineMLResource
import com.xmlcalabash.resourcecache.CompiledResourceType
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import org.nineml.coffeefilter.InvisibleXml
import org.nineml.coffeefilter.InvisibleXmlParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI

class InvisibleXmlNineML(stepConfig: XProcStepConfiguration, stepName: String, cacheGrammar: Boolean): InvisibleXmlImpl(stepConfig, "nineml", stepName, cacheGrammar) {
    override fun parse(grammarUri: URI?, grammar: XdmNode, input: String, failOnError: Boolean, parameters: Map<QName, XdmValue>): XProcDocument {
        var parser = getCachedParser(grammarUri)
        if (parser == null) {
            val invisibleXml = InvisibleXml()

            // This is also kind of an ugly hack...
            val xmldoc = XProcDocument.ofXml(grammar, stepConfig, MediaType.XML)
            val baos = ByteArrayOutputStream()
            val writer = DocumentWriter(xmldoc, baos)
            writer.write()

            val bais = ByteArrayInputStream(baos.toByteArray())
            parser = invisibleXml.getParserFromVxml(bais, grammar.baseURI.toString())

            if (cacheGrammar && grammarUri != null) {
                stepConfig.compiledResourceCache.put(stepName, grammarUri, CompiledNineMLResource(parser))
            }
        }

        return runParser(parser, input, failOnError, parameters)
    }

    override fun parse(grammarUri: URI?, grammar: String?, input: String, failOnError: Boolean, parameters: Map<QName, XdmValue>): XProcDocument {
        var parser = getCachedParser(grammarUri)
        if (parser == null) {
            val invisibleXml = InvisibleXml()
            parser = if (grammar != null) {
                invisibleXml.getParserFromIxml(grammar)
            } else {
                invisibleXml.getParser()
            }
            if (cacheGrammar && grammarUri != null) {
                stepConfig.compiledResourceCache.put(stepName, grammarUri, CompiledNineMLResource(parser))
            }
        }

        return runParser(parser, input, failOnError, parameters)
    }

    private fun getCachedParser(grammarURI: URI?): InvisibleXmlParser? {
        if (cacheGrammar && grammarURI != null) {
            if (stepConfig.compiledResourceCache.contains(stepName, grammarURI)) {
                val rsrc = stepConfig.compiledResourceCache.get(stepName, grammarURI)!!
                if (rsrc.type == CompiledResourceType.IXML9ML) {
                    return (rsrc as CompiledNineMLResource).parser
                }
            }
        }
        return null
    }

    private fun runParser(parser: InvisibleXmlParser, input: String, failOnError: Boolean, parameters: Map<QName, XdmValue>): XProcDocument {
        val doc = parser.parse(input)

        val builder = stepConfig.processor.newDocumentBuilder()
        builder.isLineNumbering = stepConfig.xmlCalabashConfig.lineNumbering
        val bch = builder.newBuildingContentHandler()
        doc.getTree(bch)
        val tree = bch.documentNode

        if (!doc.succeeded() && failOnError) {
            throw stepConfig.exception(XProcError.xcInvisibleXmlParseFailed(tree))
        }

        return XProcDocument.ofXml(tree, stepConfig)
    }

}