package com.xmlcalabash.datamodel

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.util.SaxonLocation
import net.sf.saxon.expr.parser.XPathParser
import net.sf.saxon.s9api.XdmNode
import java.net.URI

class Location(uri: URI?, lineNo: Int?, colNo: Int?) {
    companion object {
        val NULL = Location(null, null, null)
    }

    val baseUri: URI? = uri
    var _lineNumber: Int = lineNo ?: -1
    val lineNumber: Int
        get() = _lineNumber
    var _columnNumber: Int = colNo ?: -1
    val columnNumber: Int
        get() = _columnNumber

    constructor(uri: URI?): this(uri, null, null)
    constructor(loc: net.sf.saxon.s9api.Location): this(if (loc.systemId == null) null else URI(loc.systemId), loc.lineNumber, loc.columnNumber) {
        // This is analogous to what Saxon does. See also https://saxonica.plan.io/issues/7008
        if (loc is XPathParser.NestedLocation) {
            val outerloc = loc.containingLocation
            _lineNumber = outerloc.lineNumber
            _columnNumber = outerloc.columnNumber
        }
    }
    constructor(node: XdmNode): this(node.baseURI, node.lineNumber, node.columnNumber)
    constructor(doc: XProcDocument): this(doc.baseURI) {
        if (doc.value is XdmNode) {
            _lineNumber = (doc.value as XdmNode).lineNumber
            _columnNumber = (doc.value as XdmNode).columnNumber
        }
    }

    fun asSaxonLocation(): net.sf.saxon.s9api.Location {
        return SaxonLocation(this)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(baseUri ?: "???")
        if (lineNumber >= 0) {
            sb.append(":${lineNumber}")
        }
        if (columnNumber >= 0) {
            sb.append(":${columnNumber}")
        }
        return sb.toString()
    }
}