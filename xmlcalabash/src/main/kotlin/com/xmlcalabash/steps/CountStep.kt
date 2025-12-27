package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.util.SaxonTreeBuilder
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import javax.xml.transform.sax.SAXSource
import kotlin.math.max
import kotlin.math.min

class CountStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val count = queues["source"]!!.size
        val limit = max(integerBinding(Ns.limit) ?: 0, 0)
        val reportedCount = if (limit == 0) {
            count
        } else {
            min(limit, count)
        }

        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(null)
        builder.addStartElement(NsC.result)
        builder.addText(reportedCount.toString())
        builder.addEndElement()
        builder.endDocument()
        receiver.output("result", XProcDocument.ofXml(builder.result, stepConfig))
    }

    override fun toString(): String = "p:count"
}