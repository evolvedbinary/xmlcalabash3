package com.xmlcalabash.steps.extension

import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.BasicDocumentLoader
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.steps.AbstractAtomicStep
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmArray
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmValue
import net.sourceforge.tess4j.Tesseract
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import javax.imageio.ImageIO

class TesseractStep(): AbstractAtomicStep() {
    companion object {
        val _engineMode = QName("engine-mode")
        val _pageSegmentationMode = QName("page-segmentation-mode")
        val _language = QName("language")
        val _dataPath = QName("data-path")
        val _outputFormat = QName("output-format")
        val _variables = QName("variables")
        val _dpi = QName("dpi")
        val _debugOutput = QName("debug-output")
    }

    override fun run() {
        super.run()

        val language = stringBinding(_language)
        val engineMode = stringBinding(_engineMode)
        val ocrEngineMode = when (engineMode) {
            "tesseract-only" -> 0
            "lstm-only" -> 1
            "lstm-combined" -> 2
            else -> 3
        }

        val pageSegmentationMode = stringBinding(_pageSegmentationMode)
        val pageSegMode = when (pageSegmentationMode) {
            "osd-only" -> 0
            "auto-osd" -> 1
            "auto-only" -> 2
            "auto" -> 3
            "single-column" -> 4
            "single-block-vert-text" -> 5
            "single-block" -> 6
            "single-line" -> 7
            "single-word" -> 8
            "circle-word" -> 9
            "single-char" -> 10
            "sparse-text" -> 11
            "sparse-text-osd" -> 12
            "raw-line" -> 13
            else -> 1
        }
        val datapath = stringBinding(_dataPath)

        val imagedoc = queues["source"]!!.first() as XProcBinaryDocument
        val image = ImageIO.read(ByteArrayInputStream(imagedoc.binaryValue))

        val tesseract = Tesseract()

        datapath?.let { tesseract.setDatapath(it) }
        tesseract.setLanguage(language)
        tesseract.setPageSegMode(pageSegMode)
        tesseract.setOcrEngineMode(ocrEngineMode)

        val variables = stringMapBinding(_variables)
        for ((name, value) in variables) {
            tesseract.setVariable(name, value)
        }

        integerBinding(_dpi)?.let { tesseract.setVariable("user_defined_dpi", "${it}") }

        val outputFormat = stringBinding(_outputFormat) ?: "text"
        when (outputFormat) {
            "text" -> Unit
            "hocr" -> tesseract.setVariable("tessedit_create_hocr", "1")
            "tsv" -> tesseract.setVariable("tessedit_create_tsv", "1")
            "alto" -> tesseract.setVariable("tessedit_create_alto", "1")
            "lstmbox" -> tesseract.setVariable("tessedit_create_lstmbox", "1")
            "wordstrbox" -> tesseract.setVariable("tessedit_create_wordstrbox", "1")
            // segfaults java "page-xml" -> tesseract.setVariable("tessedit_create_page_xml", "1")
            // only text formats work via the API "pdf" -> tesseract.setVariable("tessedit_create_pdf", "1")
            else -> Unit
        }

        stringBinding(_debugOutput)?.let { tesseract.setVariable("debug_output", it) }

        val ocr = tesseract.doOCR(image)

        when (outputFormat) {
            "hocr" -> {
                val stream = ByteArrayInputStream(ocr.toByteArray(StandardCharsets.UTF_8))
                val loader = BasicDocumentLoader(null, stepConfig.processor, stepConfig.documentManager)
                val doc = loader.load(stream, MediaType.HTML)
                receiver.output("result", doc)
            }
            "alto" -> {
                val stream = ByteArrayInputStream(ocr.toByteArray(StandardCharsets.UTF_8))
                val loader = BasicDocumentLoader(null, stepConfig.processor, stepConfig.documentManager)
                val doc = loader.load(stream, MediaType.XML)
                receiver.output("result", doc)
            }
            "tsv" -> {
                var arrayOfLines = XdmArray()
                // https://tomrochette.com/tesseract-tsv-format/
                arrayOfLines = arrayOfLines.addMember(XdmArray(
                    listOf("level", "page_num", "block_num", "par_num", "line_num", "word_num", "left", "top", "width", "height", "conf", "text").map { XdmAtomicValue(it) }))

                for (line in ocr.split("\\r?\\n".toRegex())) {
                    val columns = mutableListOf<XdmValue>()
                    for (item in line.split("\t")) {
                        columns.add(XdmAtomicValue(item))
                    }
                    arrayOfLines = arrayOfLines.addMember(XdmArray(columns))
                }
                val json = XProcDocument.ofJson(arrayOfLines, stepConfig)
                receiver.output("result", json)
            }
            else -> {
                val result = XProcDocument.ofText(ocr, stepConfig)
                receiver.output("result", result)
            }
        }
    }
}