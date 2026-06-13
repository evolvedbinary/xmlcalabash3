package com.xmlcalabash.steps.extension.pdf

import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsAcroForm
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.PdfBoxUtils
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.XdmEmptySequence
import net.sf.saxon.s9api.XdmNode
import org.apache.pdfbox.pdmodel.interactive.form.PDButton
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox
import org.apache.pdfbox.pdmodel.interactive.form.PDChoice
import org.apache.pdfbox.pdmodel.interactive.form.PDComboBox
import org.apache.pdfbox.pdmodel.interactive.form.PDRadioButton


class PdfFormStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val primary = queues["source"]!!.first() as XProcBinaryDocument
        val data = queues["data"]!!.first()
        val password = stringBinding(Ns.password)
        val compression = stringBinding(PdfBoxUtils.compression)
        val (pdf, _) = PdfBoxUtils.read(primary, password)

        if (pdf.documentCatalog.acroForm == null) {
            throw IllegalArgumentException("No form in PDF")
        }

        if (S9Api.documentElement(data.value as XdmNode).nodeName != NsAcroForm.acroForm) {
            throw IllegalArgumentException("Data is not an f:acro-form document")
        }

        val acro = pdf.documentCatalog.acroForm!!

        val compiler = stepConfig.newXPathCompiler()
        compiler.declareNamespace("f", NsAcroForm.namespace.toString())
        val exec = compiler.compile("/f:acro-form/*")
        val select = exec.load()
        select.contextItem = data.value as XdmNode
        for (item in select.iterator()) {
            item as XdmNode
            if (item.nodeName in setOf(NsAcroForm.field, NsAcroForm.checkbox, NsAcroForm.radiobutton, NsAcroForm.button,
                    NsAcroForm.text, NsAcroForm.combobox, NsAcroForm.listbox
                )) {
                val name = item.getAttributeValue(Ns.name)
                    ?: throw IllegalArgumentException("Form element has no name: ${item}")
                val field = acro.getField(name) ?: throw IllegalArgumentException("No field in PDF: ${name}")

                val vexec = compiler.compile("f:value")
                val vselect = vexec.load()
                vselect.contextItem = item
                val vvalue = vselect.evaluate()
                var pdfValue = if (vvalue == XdmEmptySequence.getInstance()) {
                    item.underlyingValue.stringValue
                } else {
                    // There can be more than one, but I haven't found any examples of how that works
                    vvalue.first().underlyingValue.stringValue
                }

                if (field is PDCheckBox) {
                    if (pdfValue in listOf("On", "Yes", "true", "1")) {
                        pdfValue = "Yes"
                    } else if (pdfValue in listOf("Off", "No", "false", "0")) {
                        pdfValue = "Off"
                    } else {
                        throw IllegalArgumentException("Invalid button value: ${pdfValue}")
                    }
                }

                field.setValue(pdfValue)
            } else {
                throw IllegalArgumentException("Unexpected form element: ${item.nodeName}")
            }
        }

        PdfBoxUtils.write(pdf, receiver, stepConfig, compression != "none")
    }
}