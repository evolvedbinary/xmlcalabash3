package com.xmlcalabash.io

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import java.io.InputStream
import java.net.URI
import java.nio.charset.Charset

class DefaultInternetProtocolRequest(override val stepConfig: StepConfiguration, override val uri: URI) : AbstractInternetProtocolRequest<XProcDocument, DefaultInternetProtocolResponse>(stepConfig,uri) {

    override fun createResponse(responseUri: URI, statusCode: Int) : DefaultInternetProtocolResponse {
        return DefaultInternetProtocolResponse(responseUri, statusCode)
    }

    override fun buildDocument(href: URI?, documentProperties: DocumentProperties, stream: InputStream, mediaType: MediaType, charset: Charset?): XProcDocument {
        val loader = DocumentLoader(stepConfig, href, documentProperties,mapOf())
        return loader.load(stream, mediaType, charset)
    }

    override fun addDocumentProperties(responseDocument: XProcDocument, properties: Map<QName, XdmValue>): XProcDocument {
        val documentProperties = DocumentProperties(responseDocument.properties)
        properties.forEach(documentProperties::set)
        return responseDocument.with(documentProperties)
    }
}