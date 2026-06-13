package com.xmlcalabash.steps.extension.pdf

import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.PdfBoxUtils
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import org.apache.pdfbox.Loader
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import java.io.ByteArrayOutputStream

class PdfEncryptStep(): AbstractAtomicStep() {
    companion object {
        val _keysize = QName("keysize")
        val _ownerPassword = QName("owner-password")
        val _userPassword = QName("user-password")
    }

    override fun run() {
        super.run()

        val primary = queues["source"]!!.first() as XProcBinaryDocument
        val password = stringBinding(Ns.password)
        val compression = stringBinding(PdfBoxUtils.compression)
        val (pdf, _) = PdfBoxUtils.read(primary, password)

        val baos = ByteArrayOutputStream()
        pdf.save(baos)

        val encpdf = Loader.loadPDF(baos.toByteArray())

        val keysize = integerBinding(_keysize) ?: 256
        val ownerPassword = stringBinding(_ownerPassword)
        val userPassword = stringBinding(_userPassword)

        val permissions = AccessPermission()

        boolParam("print")?.let { permissions.setCanPrint(it) }
        boolParam("modify")?.let { permissions.setCanModify(it) }
        boolParam("extract")?.let { permissions.setCanExtractContent(it) }
        boolParam("modify-annotations")?.let { permissions.setCanModifyAnnotations(it) }
        boolParam("fill-in-form")?.let { permissions.setCanFillInForm(it) }
        boolParam("extract-for-accessibility")?.let { permissions.setCanExtractForAccessibility(it) }
        boolParam("assemble")?.let { permissions.setCanAssembleDocument(it) }
        boolParam("print-faithful")?.let { permissions.setCanPrintFaithful(it) }
        boolParam("readonly")?.let { permissions.setReadOnly() }

        val spp = StandardProtectionPolicy(ownerPassword, userPassword, permissions)
        spp.encryptionKeyLength = keysize
        encpdf.protect(spp)

        PdfBoxUtils.write(encpdf, receiver, stepConfig, compression != "none")
    }

    private fun boolParam(name: String): Boolean? {
        val value = booleanBinding(QName(NamespaceUri.NULL, name)) ?: return null
        return stepConfig.typeUtils.parseBoolean("${value}")
    }

    override fun toString(): String = "cx:pdf-encrypt"
}