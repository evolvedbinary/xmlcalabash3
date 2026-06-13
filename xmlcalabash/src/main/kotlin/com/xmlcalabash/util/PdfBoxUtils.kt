package com.xmlcalabash.util

import com.xmlcalabash.datamodel.DocumentContext
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.BasicDocumentLoader
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.api.Receiver
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.om.NamespaceMap
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdfwriter.compress.CompressParameters
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDocumentCatalog
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.xmpbox.type.*
import org.apache.xmpbox.xml.DomXmpParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat

object PdfBoxUtils {
    val compression = QName("compression")
    val pages = QName("pages")
    val dpi = QName("dpi")

    private val rdfAlt = QName("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#", "Alt")
    private val rdfSeq = QName("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#", "Seq")
    private val rdfLi = QName("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#", "li")
    private val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

    fun read(document: XProcBinaryDocument, password: String? = null): Pair<PDDocument, AccessPermission> {
        if (password != null) {
            val encpdf = Loader.loadPDF(document.binaryValue, password)
            val permissions = encpdf.currentAccessPermission
            encpdf.isAllSecurityToBeRemoved = true
            return Pair(encpdf, permissions)
        } else {
            val pdf = Loader.loadPDF(document.binaryValue)
            val permissions = pdf.currentAccessPermission
            return Pair(pdf, permissions)
        }
    }

    fun write(pdf: PDDocument, receiver: Receiver, context: DocumentContext, compress: Boolean, port: String = "result") {
        val baos = ByteArrayOutputStream()

        if (compress) {
            pdf.save(baos, CompressParameters.DEFAULT_COMPRESSION)
        } else {
            pdf.save(baos, CompressParameters.NO_COMPRESSION)
        }

        val output = XProcBinaryDocument(baos.toByteArray(), null, context).with(MediaType.PDF)
        receiver.output(port, output)
    }

    fun readMetadata(catalog: PDDocumentCatalog, stepConfig: XProcStepConfiguration): XProcDocument {
        val meta = catalog.metadata ?: return XProcDocument.ofEmpty(stepConfig)

        // The Apache xmpbox API is only designed to handle PDF/A files and rejects files that
        // use XMP metadata not defined in PDF/A. So this code just punts, it parses the XMP
        // RDF and returns it. I don't know what's better.

        val xmpsource = ByteArrayInputStream(meta.createInputStream().readAllBytes())
        val loader = BasicDocumentLoader(null, stepConfig.processor, stepConfig.documentManager)

        return loader.load(xmpsource, MediaType.XML)
    }

    private fun computePrefix(pfx: String, ns: String, pfxMap: MutableMap<String,String>, nsMap: MutableMap<String,String>): String {
        if (nsMap.contains(ns)) {
            return nsMap[ns]!!
        } else if (!pfxMap.contains(pfx)) {
            return pfx
        } else {
            val cpfx = "ns_"
            var count = 1
            while (pfxMap.contains("$cpfx$count")) {
                count += 1
            }
            return "$cpfx$count"
        }
    }

}