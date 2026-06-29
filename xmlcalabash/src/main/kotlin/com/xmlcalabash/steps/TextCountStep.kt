package com.xmlcalabash.steps

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.XdmEmptySequence
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import javax.xml.transform.sax.SAXSource

open class TextCountStep(): AbstractTextStep() {
    override fun run() {
        super.run()
        val document = queues["source"]!!.first()

        val count = textLines(document).size

        val result = atomicResult(count.toString())
        val properties = DocumentProperties(mapOf(Ns.baseUri to XdmEmptySequence.getInstance()))
        receiver.output("result", XProcDocument.ofXml(result, stepConfig, properties))
    }

    override fun toString(): String = "p:text-count"
}