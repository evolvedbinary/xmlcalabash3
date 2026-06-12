package com.xmlcalabash.steps.extension.pdf

import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.PdfBoxUtils
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class PdfToImages(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val primary = queues["source"]!!.first() as XProcBinaryDocument
        val password = stringBinding(Ns.password)
        val (pdf, _) = PdfBoxUtils.read(primary, password)

        val dpi = (integerBinding(PdfBoxUtils.dpi) ?: 300).toFloat()
        val format = stringBinding(Ns.format) ?: "png"

        val imageContentType = when(format) {
            "jpeg", "jpg" -> MediaType.JPEG
            "bmp" -> MediaType.parse("image/bmp")
            else -> MediaType.PNG
        }

        val pdfRenderer = PDFRenderer(pdf)
        for (pageno in 0 until pdf.numberOfPages) {
            val image = pdfRenderer.renderImageWithDPI(pageno, dpi, ImageType.RGB)
            val baos = ByteArrayOutputStream()
            ImageIO.write(image, format, baos)
            if (baos.size() == 0) {
                throw IllegalArgumentException("Failed to render ${format} image")
            }
            val output = XProcBinaryDocument(baos.toByteArray(), null, stepConfig).with(imageContentType)
            receiver.output("result", output)
        }
    }

    override fun toString(): String = "cx:pdf-to-images"
}