package com.xmlcalabash.config

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.MediaType
import net.sf.saxon.s9api.XdmValue
import java.net.URI

class XmlCalabashInput(val port: String?, val href: URI?, val contentType: MediaType) {
    var encoding: String? = null
    var doc: XProcDocument? = null
    var multiplex = false
    val multiplexMapping = mutableMapOf<String, String>()

    fun withPort(port: String): XmlCalabashInput {
        val newInput = XmlCalabashInput(port, href, contentType)
        newInput.encoding = encoding
        newInput.doc = doc
        return newInput
    }

    override fun toString(): String {
        return "${port}: ${href} (${contentType})"
    }

    override fun hashCode(): Int {
        var code = if (href == null) {
            7 * contentType.hashCode() + (11 * multiplex.hashCode())
        } else {
            href.hashCode() + (7 * contentType.hashCode()) + (11 * multiplex.hashCode())
        }
        port?.let { code += 17 * it.hashCode() }
        encoding?.let { code += (3 * it.hashCode()) }
        doc?.let { code += (5 * it.hashCode()) }
        return code
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as XmlCalabashInput

        return port == other.port && href == other.href && contentType == other.contentType
                && encoding == other.encoding && doc == other.doc && multiplex == other.multiplex
    }
}