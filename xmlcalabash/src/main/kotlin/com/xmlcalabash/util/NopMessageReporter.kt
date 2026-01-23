package com.xmlcalabash.util

import com.xmlcalabash.api.MessageReporter
import com.xmlcalabash.io.MessagePrinter
import net.sf.saxon.s9api.QName

open class NopMessageReporter(val nextReporter: MessageReporter? = null): MessageReporter {
    protected var _messagePrinter: MessagePrinter? = null
    protected var _threshold = Verbosity.ERROR // irrelevant

    override val messagePrinter: MessagePrinter
        get() = _messagePrinter!!

    override fun setMessagePrinter(messagePrinter: MessagePrinter) {
        _messagePrinter = messagePrinter
    }

    override val threshold: Verbosity
        get() = _threshold

    override fun setThreshold(threshold: Verbosity, applyDownstream: Boolean) {
        _threshold = threshold
        if (applyDownstream) {
            nextReporter?.setThreshold(threshold)
        }
    }

    override fun report(severity: Verbosity, report: () -> Report) {
        nextReporter?.report(severity, report)
    }
}