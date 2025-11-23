package com.xmlcalabash.util

import net.sf.saxon.s9api.Location

class SaxonLocation(val sysId: String?, val line: Int, val col: Int): Location {
    constructor(loc: com.xmlcalabash.datamodel.Location): this(loc.baseUri?.toString(), loc.lineNumber, loc.columnNumber)

    override fun getSystemId(): String? {
        return sysId
    }

    override fun getPublicId(): String? {
        return null
    }

    override fun getLineNumber(): Int {
        return line
    }

    override fun getColumnNumber(): Int {
        return col
    }

    override fun saveLocation(): Location? {
        return SaxonLocation(systemId, line, col)
    }
}