package com.xmlcalabash.steps.extension.pdf

import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.PdfBoxUtils

class PdfDecryptStep(val isCopy: Boolean): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val primary = queues["source"]!!.first() as XProcBinaryDocument
        val password = stringBinding(Ns.password)
        val compression = stringBinding(PdfBoxUtils.compression)
        val (pdf, _) = PdfBoxUtils.read(primary, password)

        PdfBoxUtils.write(pdf, receiver, stepConfig, compression != "none")
    }

    override fun toString(): String {
        if (isCopy) {
            return "cx:pdf-copy"
        }
        return "cx:pdf-decrypt"
    }
}