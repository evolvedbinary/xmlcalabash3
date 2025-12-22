package com.xmlcalabash.config

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.MediaType
import net.sf.saxon.s9api.XdmValue
import java.net.URI

class XmlCalabashInput(val href: URI?, val contentType: MediaType) {
    var encoding: String? = null
    var doc: XProcDocument? = null

    override fun hashCode(): Int {
        var code = if (href == null) {
            7 * contentType.hashCode()
        } else {
            href.hashCode() + (7 * contentType.hashCode())
        }
        encoding?.let { code += (3 * it.hashCode()) }
        doc?.let { code += (5 * it.hashCode()) }
        return code
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as XmlCalabashInput

        return href == other.href && contentType == other.contentType
                && encoding == other.encoding && doc == other.doc
    }

}