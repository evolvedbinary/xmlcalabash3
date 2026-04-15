package com.xmlcalabash.io

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import org.apache.hc.client5.http.cookie.CookieStore

/**
 * An IP Request.
 *
 * @param <T> The type of the document built for the response.
 * @param <U> The type of the response.
 */
interface InternetProtocolRequest<T, U : InternetProtocolResponse<T>> {

    /**
     * HTTP Request Timeout in Milliseconds.
     */
    var requestTimeout: Int?

    /**
     * HTTP Response Timeout in Milliseconds.
     */
    var responseTimeout: Int?

    var httpVersion: Pair<Int,Int>?
    var statusOnly:Boolean
    var suppressCookies:Boolean
    var overrideContentType: MediaType?
    var followRedirectCount: Int
    var sendBodyAnyway: Boolean

    var properties: DocumentProperties

    var parameters: Map<QName,XdmValue>

    var cookieStore: CookieStore?

    fun addSource(doc: XProcDocument)

    fun addHeader(name: String, value: String)

    fun authentication(method: String, username: String, password: String, preemptive: Boolean = false)

    fun execute(method: String): U
}
