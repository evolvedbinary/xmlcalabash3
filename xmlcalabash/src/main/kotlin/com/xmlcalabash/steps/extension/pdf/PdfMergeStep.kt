package com.xmlcalabash.steps.extension.pdf

import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.PdfBoxUtils
import org.apache.pdfbox.multipdf.PDFMergerUtility

class PdfMergeStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val primary = queues["source"]!!.first() as XProcBinaryDocument
        val secondary = queues["secondary"] ?: emptyList()
        val password = stringBinding(Ns.password)
        val compression = stringBinding(PdfBoxUtils.compression)
        val (pdf, _) = PdfBoxUtils.read(primary, password)

        // N.B. The secondary PDF documents must not be encrypted.

        val merger = PDFMergerUtility()
        for (doc in secondary) {
            val (spdf, _) = PdfBoxUtils.read(doc as XProcBinaryDocument)
            merger.appendDocument(pdf, spdf)
        }

        PdfBoxUtils.write(pdf, receiver, stepConfig, compression != "none")
    }

    override fun toString(): String = "cx:pdf-merge"
}