package com.xmlcalabash.steps.extension.pdf

import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsAcroForm
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.PdfBoxUtils
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.TypeUtils
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmEmptySequence
import net.sf.saxon.s9api.XdmNode
import org.apache.pdfbox.cos.*
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.interactive.form.*
import org.apache.pdfbox.text.PDFTextStripper
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.roundToInt

class PdfInfoStep(): AbstractAtomicStep() {
    companion object {
        val _pageDetails = QName("page-details")
        val _pageText = QName("page-text")
        val _formDetails = QName("form-details")

        val standardKeys = setOf("Author", "CreationDate", "Creator", "Keywords",
            "ModDate", "Producer", "Subject", "Title", "Trapped")

        // Subset from https://github.com/Bubblbu/paper-sizes
        val paperSizes = mapOf(
            "2384x3370" to "A0",
            "1684x2384" to "A1",
            "1191x1684" to "A2",
            "842x1191" to "A3",
            "595x842" to "A4",
            "420x595" to "A5",
            "4008x2835" to "B0",
            "2835x2004" to "B1",
            "2004x1417" to "B2",
            "1417x1001" to "B3",
            "1001x709" to "B4",
            "709x499" to "B5",
            "3677x2599" to "C0",
            "2599x1837" to "C1",
            "1837x1298" to "C2",
            "1298x918" to "C3",
            "918x649" to "C4",
            "649x459" to "C5",
            "612x1008" to "US Legal",
            "612x792" to "US Letter"
        )
    }

    lateinit var builder: SaxonTreeBuilder

    override fun run() {
        super.run()

        val primary = queues["source"]!!.first() as XProcBinaryDocument
        val password = stringBinding(Ns.password)
        val (pdf, permissions) = PdfBoxUtils.read(primary, password)

        val info = pdf.documentInformation

        val pageDetails = booleanBinding(_pageDetails) ?: false
        val pageText = booleanBinding(_pageText) ?: false
        val formDetails = booleanBinding(_formDetails) ?: false

        builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(null)

        val baseUri = primary.baseURI
        if (baseUri != null) {
            infoStart("pdf-info", mapOf("href" to baseUri.toString()))
        } else {
            infoStart("pdf-info")
        }

        info("version", "${pdf.version}")

        if (pdf.documentCatalog.acroForm != null) {
            info("form", "acro-form")
        } else {
            info("form", "none")
        }

        if (formDetails && pdf.documentCatalog.acroForm != null) {
            outputFormDetails(pdf.documentCatalog.acroForm, builder)
        }

        info("pages", "${pdf.numberOfPages}")
        info("encrypted", "${pdf.isEncrypted}")

        if (pdf.isEncrypted) {
            val attr = mutableMapOf<String, String>()
            attr["assemble"] = "${permissions.canAssembleDocument()}"
            attr["extract"] = "${permissions.canExtractContent()}"
            attr["extract-for-accessibility"] = "${permissions.canExtractForAccessibility()}"
            attr["fill-in-form"] = "${permissions.canFillInForm()}"
            attr["modify"] = "${permissions.canModify()}"
            attr["modify-annotations"] = "${permissions.canModifyAnnotations()}"
            attr["print"] = "${permissions.canPrint()}"
            attr["print-faithful"] = "${permissions.canPrintFaithful()}"
            attr["readonly"] = "${permissions.isReadOnly}"
            infoStart("permissions", attr)
            builder.addEndElement()
        }

        var width = 0
        var height = 0
        for (page in 0 until pdf.numberOfPages) {
            val page = pdf.getPage(page)
            if (page.mediaBox.width > width) {
                width = page.mediaBox.width.roundToInt()
            }
            if (page.mediaBox.height > height) {
                height = page.mediaBox.height.roundToInt()
            }
        }

        if (width > 0 && height > 0) {
            val details = mapOf(
                "width" to "${width}",
                "height" to "${height}",
                "units" to "px"
            )
            val size = "${width}x${height}"
            if (paperSizes.containsKey(size)) {
                info("page-size", paperSizes[size]!!, details)
            } else {
                info("page-size","(unknown)", details)
            }
        }

        info("title", info.title)
        info("creator", info.creator)
        info("producer", info.producer)
        info("author", info.author)
        info("creation-date", info.creationDate)
        info("modification-date", info.modificationDate)
        info("subject", info.subject)

        for (keyword in (info.keywords ?: "").split(",\\s*".toRegex())) {
            info("keyword", keyword.trim())
        }

        info("trapped", info.trapped)
        info("file-size", "${primary.binaryValue.size}")

        for (name in info.metadataKeys) {
            if (!standardKeys.contains(name)) {
                val value = info.getPropertyStringValue(name)
                if (value is String && value.startsWith("D:")) {
                    val date = info.cosObject.getDate(name)
                    info("additional", date,
                        mapOf("name" to name, "type" to "dateTime"))
                } else {
                    info("additional", info.getPropertyStringValue(name),
                        mapOf("name" to name))
                }
            }
        }

        val xmp = PdfBoxUtils.readMetadata(pdf.documentCatalog, stepConfig)
        if (xmp.value is XdmNode) {
            builder.addSubtree(S9Api.documentElement(xmp.value as XdmNode))
        }

        if (pageDetails || pageText) {
            infoStart("page-details")
            for (page in 0 until pdf.numberOfPages) {
                infoStart("page", mapOf("page-number" to "${page+1}"))
                val pdfPage = pdf.getPage(page)
                if (pageDetails) {
                    info("media-box", pdfPage.mediaBox)
                    info("art-box", pdfPage.artBox)
                    info("bounding-box", pdfPage.bBox)
                    info("bleed-box", pdfPage.bleedBox)
                    info("crop-box", pdfPage.cropBox)
                    info("trim-box", pdfPage.trimBox)
                    info("rotation", "${pdfPage.rotation}")
                }
                if (pageText) {
                    val stripper = PDFTextStripper()
                    stripper.startPage = page + 1
                    stripper.endPage = page + 1
                    info("text", stripper.getText(pdf))
                }

                builder.addEndElement()
            }
            builder.addEndElement()
        }

        builder.addEndElement()
        builder.endDocument()

        receiver.output("result", XProcDocument.ofXml(builder.result, stepConfig))
    }

    private fun infoStart(name: String, attributes: Map<String,String> = emptyMap()) {
        var attr: AttributeMap = EmptyAttributeMap.getInstance()
        for ((name, value) in attributes) {
            attr = attr.put(TypeUtils.attributeInfo(QName(NamespaceUri.NULL, name), value))
        }
        // Oh, this is a terrible hack.
        val qname = if (name.startsWith("f:")) {
            QName(NsAcroForm.namespace, name)
            } else {
            QName(NsCx.namespace, "cx:${name}")
        }
        builder.addStartElement(qname, attr)
    }

    private fun info(name: String, value: Any?, attributes: Map<String,String> = emptyMap()) {
        if (value == null || value == "") {
            if (name.startsWith("f:")) {
                infoStart(name, attributes)
                builder.addEndElement()
            }
            return
        }

        when (value) {
            is String -> {
                infoStart(name, attributes)
                builder.addText(value)
            }
            is Calendar -> {
                infoStart(name, attributes)
                builder.addText(dateValue(value))
            }
            is PDRectangle -> {
                val attr = mutableMapOf<String,String>()
                attr.putAll(attributes)
                attr.put("width", "${value.width}")
                attr.put("height", "${value.height}")
                attr.put("lower-left-x", "${value.lowerLeftX}")
                attr.put("lower-left-y", "${value.lowerLeftY}")
                attr.put("upper-right-x", "${value.upperRightX}")
                attr.put("upper-right-y", "${value.upperRightY}")
                infoStart(name, attr)
            }
            else -> {
                stepConfig.debug { "Unexpected value type in cx:PdfBox info: ${value}"}
                return
            }
        }

        builder.addEndElement()
    }

    private fun dateValue(date: Calendar): String {
        if (date is GregorianCalendar) {
            val zdt = date.toZonedDateTime().toInstant()
            return DateTimeFormatter.ISO_INSTANT.format(zdt)
        } else {
            return "(Date in unknown calendar)"
        }
    }

    private fun outputFormDetails(form: PDAcroForm, builder: SaxonTreeBuilder) {
        infoStart("f:acro-form")
        for (field in form.fieldTree.iterator()) {
            if (field is PDPushButton) {
                continue
            }

            val attr = mutableMapOf<String,String>()
            attr["name"] = field.fullyQualifiedName
            attr["type"] = field.fieldType
            if (field.isNoExport) {
                attr["no-export"] = "${field.isNoExport}"
            }
            if (field.isRequired) {
                attr["readonly"] = "${field.isReadOnly}"
            }
            if (field.isRequired) {
                attr["required"] = "${field.isRequired}"
            }

            when (field) {
                is PDCheckBox -> {
                    info("f:checkbox", field.value, attr)
                }
                is PDRadioButton -> {
                    info("f:radiobutton", field.value, attr)
                }
                is PDButton -> {
                    info("f:button", field.value, attr)
                }
                is PDTextField -> {
                    info("f:text", field.value, attr)
                }
                is PDComboBox, is PDListBox -> {
                    val values = allowedValues(field)
                    if (field is PDComboBox) {
                        infoStart("f:combobox", attr)
                    } else {
                        infoStart("f:listbox", attr)
                    }
                    for (value in values) {
                        info("f:choice", value)
                    }
                    for (value in field.value.filter { it != "" }) {
                        info("f:value", value)
                    }
                    builder.addEndElement()
                }
                is PDSignatureField -> {
                    infoStart("f:signature", attr)
                    builder.addEndElement()
                }
                else -> {
                    stepConfig.debug { "Unexpected field type: ${field}"}
                }
            }
        }
        builder.addEndElement()

    }

    private fun allowedValues(field: PDField): List<String> {
        val list = mutableListOf<String>()
        val values = field.cosObject.getCOSArray(COSName.OPT)
        if (values != null) {
            for (value in values.iterator()) {
                when (value) {
                    is COSString -> {
                        list.add(value.string)
                    }

                    is COSInteger -> {
                        list.add("${value.intValue()}")
                    }

                    is COSFloat -> {
                        list.add("${value.floatValue()}")
                    }

                    is COSNumber -> {
                        // Not an integer or a float, so ...
                        list.add("${value.longValue()}")
                    }

                    is COSBoolean -> {
                        list.add("${value.value}")
                    }

                    is COSNull -> Unit // ???
                    else -> {
                        stepConfig.debug { "Unexpected list value: $value" }
                        list.add("${value}")
                    }
                }
            }
        }
        return list
    }

    override fun toString(): String = "cx:pdf-info"
}