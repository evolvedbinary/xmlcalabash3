package com.xmlcalabash.steps

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.XdmEmptySequence
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

        val result = atomicResult(reportedCount.toString())
        val properties = DocumentProperties(mapOf(Ns.baseUri to XdmEmptySequence.getInstance()))
        receiver.output("result", XProcDocument.ofXml(result, stepConfig, properties))
    }

    override fun toString(): String = "p:count"
}