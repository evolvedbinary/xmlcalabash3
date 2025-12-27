package com.xmlcalabash.util

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import org.nineml.coffeefilter.InvisibleXml
import org.nineml.coffeefilter.InvisibleXmlParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class InvisibleXmlNineML(stepConfig: XProcStepConfiguration): InvisibleXmlImpl(stepConfig, "nineml") {
    override fun parse(grammar: XdmNode, input: String, failOnError: Boolean, parameters: Map<QName, XdmValue>): XProcDocument {
        val invisibleXml = InvisibleXml()

        // This is also kind of an ugly hack...
        val xmldoc = XProcDocument.ofXml(grammar, stepConfig, MediaType.XML)
        val baos = ByteArrayOutputStream()
        val writer = DocumentWriter(xmldoc, baos)
        writer.write()

        val bais = ByteArrayInputStream(baos.toByteArray())
        val parser = invisibleXml.getParserFromVxml(bais, grammar.baseURI.toString())

        return runParser(parser, input, failOnError, parameters)
    }

    override fun parse(grammar: String?, input: String, failOnError: Boolean, parameters: Map<QName, XdmValue>): XProcDocument {
        val invisibleXml = InvisibleXml()
        val parser = if (grammar != null) {
            invisibleXml.getParserFromIxml(grammar)
        } else {
            invisibleXml.getParser()
        }

        return runParser(parser, input, failOnError, parameters)
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