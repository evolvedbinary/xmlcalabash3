package com.xmlcalabash.io

import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmMap
import org.apache.hc.client5.http.cookie.BasicCookieStore
import org.apache.hc.client5.http.cookie.CookieStore
import java.net.URI
import java.nio.charset.Charset

abstract class AbstractInternetProtocolResponse<T>(override val responseUri: URI, override val statusCode: Int) : InternetProtocolResponse<T> {
    override var report: XdmMap? = null
    private val _headers = mutableMapOf<String, XdmAtomicValue>()
    override var mediaType: MediaType? = null
    private val _response = mutableListOf<T>()
    private var _cookieStore: CookieStore? = null

    override var cookieStore: CookieStore?
        get() = _cookieStore
        set(value) {
            _cookieStore = BasicCookieStore()
            for (cookie in value!!.cookies) {
                _cookieStore!!.addCookie(cookie)
            }
        }

    override val headerContentType: MediaType?
        get() {
            val value = headers["content-type"]
            if (value == null) {
                return null
            }

            return MediaType.parse(value.underlyingValue.stringValue).discardParameters(listOf("charset"))
        }

    override val headerCharset: Charset?
        get() {
            val value = headers["content-type"]
            if (value == null) {
                return null
            }
            val ctype = MediaType.parse(value.underlyingValue.stringValue)
            return ctype.charset()
        }

    override var headers: Map<String, XdmAtomicValue>
        get() = _headers
        set(value) {
            _headers.clear()
            _headers.putAll(value)
        }

    override fun addResponse(response: T) {
        _response += response
    }

    override val empty: Boolean
        get() = _response.isEmpty()

    override val singlepart: Boolean
        get() = !multipart

    override val multipart: Boolean
        get() = _response.size > 1 || (mediaType != null && mediaType!!.mediaType == "multipart")

    override val response: List<T> = _response
}