package com.xmlcalabash.steps.extension.pdf

import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.PdfBoxUtils
import org.apache.pdfbox.pdmodel.PDDocument

class PdfExtractStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val primary = queues["source"]!!.first() as XProcBinaryDocument
        val password = stringBinding(Ns.password)
        val compression = stringBinding(PdfBoxUtils.compression)
        val (pdf, _) = PdfBoxUtils.read(primary, password)

        val pagecount = pdf.numberOfPages
        val pagespecs = stringBinding(PdfBoxUtils.pages) ?: ""
        val pages = mutableListOf<Int>()
        val pagere = "(\\d+)(\\s*-\\s*(\\d+))?".toRegex()
        for (page in pagespecs.split(",\\s*".toRegex())) {
            if (page.trim() == "") {
                continue
            }

            val match = pagere.matchEntire(page.trim())
            if (match == null) {
                throw IllegalArgumentException("Invalid page specification in pages: $page")
            } else {
                val startpg = match.groupValues[1].toInt()
                if (startpg <= 0) {
                    throw IllegalArgumentException("Invalid page specification in pages: $page")
                }
                if (startpg > pagecount) {
                    throw IllegalArgumentException("No such page: $startpg")
                }
                val endstr = match.groupValues[3]
                if (endstr == "") {
                    pages.add(startpg)
                } else {
                    val endpg = endstr.toInt()
                    if (endpg <= 0 || endpg < startpg) {
                        throw IllegalArgumentException("Invalid page specification in pages: $page")
                    }
                    if (endpg > pagecount) {
                        throw IllegalArgumentException("No such page: $endpg")
                    }
                    for (num in startpg..endpg) {
                        pages.add(num)
                    }
                }
            }
        }

        val seenPages = mutableSetOf<Int>()
        var maxPage = 0
        var makeCopy = false
        for (pageno in pages) {
            makeCopy = seenPages.contains(pageno) || pageno <= maxPage
            seenPages.add(pageno)
            maxPage = pageno
            if (makeCopy) {
                break
            }
        }

        val resultpdf = if (makeCopy) {
            val outputpdf = PDDocument()
            outputpdf.document.version = pdf.version
            outputpdf.documentInformation = pdf.documentInformation
            outputpdf.documentCatalog.viewerPreferences = pdf.documentCatalog.viewerPreferences

            for (pageno in pages) {
                val page = pdf.getPage(pageno - 1)
                outputpdf.addPage(page)
            }

            // See https://github.com/mkl-public/testarea-pdfbox2/blob/master/src/test/java/mkl/testarea/pdfbox2/merge/OptimizeAfterMerge.java
            // for a way to optimize a PDF after merging pages
            outputpdf
        } else {
            // We want a subset of pages, in order, so we can just delete pages
            // Remove them from the end so that the indexes don't change
            for (pageno in (pdf.numberOfPages - 1) downTo 0) {
                if (!seenPages.contains(pageno + 1)) {
                    pdf.removePage(pageno)
                }
            }
            pdf
        }

        PdfBoxUtils.write(resultpdf, receiver, stepConfig, compression != "none")
    }

    override fun toString(): String = "cx:pdf-extract"
}