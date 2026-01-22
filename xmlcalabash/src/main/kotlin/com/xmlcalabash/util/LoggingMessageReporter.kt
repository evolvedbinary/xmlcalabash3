package com.xmlcalabash.util

import com.xmlcalabash.api.MessageReporter
import com.xmlcalabash.util.NopMessageReporter
import net.sf.saxon.s9api.QName
import org.apache.logging.log4j.kotlin.logger

class LoggingMessageReporter(nextReporter: MessageReporter? = null): NopMessageReporter(nextReporter) {
    override fun report(severity: Verbosity, report: () -> Report) {
        val reified = report()
        when (severity) {
            Verbosity.ERROR -> logger.error(reified.message())
            Verbosity.WARN -> logger.warn(reified.message())
            Verbosity.INFO -> logger.info(reified.message())
            Verbosity.DEBUG -> logger.debug(reified.message())
            Verbosity.TRACE -> logger.trace(reified.message())
        }
        nextReporter?.report(severity, report)
    }
}
