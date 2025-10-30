package com.xmlcalabash.util

import com.xmlcalabash.api.MessageReporter
import com.xmlcalabash.namespace.Ns
import net.sf.saxon.s9api.QName
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class BufferingMessageReporter(maxsize: Int, nextReporter: MessageReporter): NopMessageReporter(nextReporter) {
    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        private val tzformatter = DateTimeFormatter.ofPattern("Z")
    }

    private val _reports = mutableListOf<Report>()

    private var _maxsize = maxsize

    var maxsize: Int
        get() = _maxsize
        set(value) {
            _maxsize = value
            while (maxsize > 0 && _reports.size >= _maxsize) {
                _reports.removeAt(0)
            }
        }

    fun messages(threshold: Verbosity): List<Report> {
        return _reports.filter { it.severity >= threshold }
    }

    fun clear() {
        synchronized(_reports) {
            _reports.clear()
        }
    }

    override fun report(severity: Verbosity, report: () -> Report) {
        val reified = report()

        val dt = LocalDateTime.now().atZone(ZoneId.systemDefault())
        val dtformatted = dt.format(formatter)
        val dtz = dt.format(tzformatter)
        // I really don't understand why there isn't a Z formatter that
        // always uses the HH:MM format.
        if (dtz.contains(":")) {
            reified.addDetail(Ns.date, "${dtformatted}${dtz}")
        } else {
            reified.addDetail(Ns.date, "${dtformatted}${dtz.substring(0, 3)}:${dtz.substring(3)}")
        }

        while (maxsize > 0 && _reports.size >= maxsize) {
            _reports.removeAt(0)
        }
        _reports.add(reified)

        nextReporter?.report(severity) { reified }
    }
}