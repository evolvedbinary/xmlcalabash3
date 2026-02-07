package com.xmlcalabash.util

import com.xmlcalabash.api.MessageReporter
import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.namespace.Ns
import net.sf.saxon.s9api.QName

class DefaultMessageReporter(nextReporter: MessageReporter? = null): NopMessageReporter(nextReporter) {

    override fun report(severity: Verbosity, report: () -> Report) {
        if (severity >= threshold) {
            val reified = report()
            val prefix = StringBuilder()

            when (severity) {
                Verbosity.TRACE -> prefix.append("Trace ")
                Verbosity.DEBUG -> prefix.append("Debug ")
                Verbosity.INFO -> Unit
                Verbosity.WARN -> prefix.append("Warning ")
                Verbosity.ERROR -> prefix.append("Error ")
            }

            try {
                messagePrinter.println("${prefix}${reified}")
            } catch (ex: Exception) {
                messagePrinter.println("${prefix}failed to evaluate message: ${ex.message}")
            }
        }

        nextReporter?.report(severity, report)
    }
}
