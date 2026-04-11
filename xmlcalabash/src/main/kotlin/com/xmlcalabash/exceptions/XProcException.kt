package com.xmlcalabash.exceptions

import com.xmlcalabash.datamodel.Location
import net.sf.saxon.expr.parser.XPathParser
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SaxonApiException
import net.sf.saxon.trans.XPathException


class XProcException(val error: XProcError, cause: Throwable? = null): RuntimeException(error.toString(), cause) {
    internal var elaborated = false

    internal fun elaborate(type: QName, name: String, location: Location) {
        // Always update the stack trace, even if we've otherwise elaborated this error
        error.updateAt(type, name)

        if (elaborated) {
            return
        }
        elaborated = true

        if (error.errorLocation == Location.NULL) {
            error.updateAt(location)
        }

        when (cause) {
            is SaxonApiException -> elaborateSaxonApiException(cause as SaxonApiException)
            is XPathException -> elaborateXPathException(cause as XPathException)
            else -> Unit
        }
    }

    private fun elaborateSaxonApiException(ex: SaxonApiException) {
        if (ex.cause is XPathException) {
            elaborateXPathException(ex.cause as XPathException)
        }
    }

    private fun elaborateXPathException(ex: XPathException) {
        if (ex.locator is XPathParser.NestedLocation) {
            val nestloc = ex.locator as XPathParser.NestedLocation
            error.updateAtInput(Location(nestloc))
        } else if (ex.locator != null) {
            error.updateAtInput(Location(ex.locator))
        }
    }

}