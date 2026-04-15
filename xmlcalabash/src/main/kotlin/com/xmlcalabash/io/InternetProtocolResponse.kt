package com.xmlcalabash.io

import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmMap
import org.apache.hc.client5.http.cookie.CookieStore
import java.net.URI
import java.nio.charset.Charset

/**
 * The response from an {@link InternetProtocolRequest}
 *
 * Note: the `mediaType` is the ultimate content type that the request wants. This may override
 * what the server actually responded. The `headerContentType` and `headerCharset` identify what
 * the server said.
 */
interface InternetProtocolResponse<T> {

    val responseUri: URI
    val statusCode: Int
    var report: XdmMap?
    var mediaType: MediaType?
    var cookieStore: CookieStore?
    val headerContentType: MediaType?
    val headerCharset: Charset?
    var headers: Map<String, XdmAtomicValue>
    val empty: Boolean
    val singlepart: Boolean
    val multipart: Boolean
    val response: List<T>

    fun addResponse(response: T)
}