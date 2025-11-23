package com.xmlcalabash.exceptions

interface ErrorExplanation {
    var showStacktrace: Boolean
    var messageWidth: Int
    fun report(error: XProcError)
    fun reportExplanation(error: XProcError)
    fun message(error: XProcError, includeDetails: Boolean): String
    fun explanation(error: XProcError): String
}