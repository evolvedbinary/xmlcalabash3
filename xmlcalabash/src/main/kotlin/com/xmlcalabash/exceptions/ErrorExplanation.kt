package com.xmlcalabash.exceptions

import com.xmlcalabash.api.MessageReporter

interface ErrorExplanation {
    val reporter: MessageReporter
    var showStacktrace: Boolean
    var messageWidth: Int
    fun report(error: XProcError)
    fun reportExplanation(error: XProcError)
    fun message(error: XProcError, includeDetails: Boolean): String
    fun explanation(error: XProcError): String
}