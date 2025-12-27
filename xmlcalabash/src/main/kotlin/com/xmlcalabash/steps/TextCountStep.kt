package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.util.SaxonTreeBuilder
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import javax.xml.transform.sax.SAXSource

open class TextCountStep(): AbstractTextStep() {
    override fun run() {
        super.run()
        val document = queues["source"]!!.first()

        val count = textLines(document).size

        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(null)
        builder.addStartElement(NsC.result)
        builder.addText(count.toString())
        builder.addEndElement()
        builder.endDocument()
        receiver.output("result", XProcDocument.ofXml(builder.result, stepConfig))
    }

    override fun toString(): String = "p:text-count"
}