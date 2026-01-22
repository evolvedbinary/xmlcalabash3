package com.xmlcalabash.api

import com.xmlcalabash.io.MessagePrinter
import com.xmlcalabash.util.Report
import com.xmlcalabash.util.Verbosity
import net.sf.saxon.s9api.QName

interface MessageReporter {
    val messagePrinter: MessagePrinter
    val threshold: Verbosity
    fun setMessagePrinter(messagePrinter: MessagePrinter)
    fun setThreshold(threshold: Verbosity, applyDownstream: Boolean = true)
    fun report(severity: Verbosity, report: () -> Report)
}