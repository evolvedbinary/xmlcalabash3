package com.xmlcalabash.util.fileselector.selectors

import com.xmlcalabash.util.Urify
import com.xmlcalabash.util.fileselector.FSFile
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.util.concurrent.TimeUnit

class DateSelector(val dateTime: Instant, val whenRelative: WhenRelative = WhenRelative.EQUAL, granularityms: Int? = null): Selector {
    private val granularity = granularityms ?: if (Urify.isWindows) 2000 else 0

    override fun selects(file: FSFile): Boolean {
        val tseconds = FileTime.from(dateTime).to(TimeUnit.MILLISECONDS)
        val dtseconds = FileTime.fromMillis(file.path.lastModified()).to(TimeUnit.MILLISECONDS)

        val diff = tseconds - dtseconds
        when (whenRelative) {
            WhenRelative.EQUAL -> return Math.abs(diff) <= granularity
            WhenRelative.BEFORE -> return Math.abs(diff) > granularity && diff > 0
            WhenRelative.AFTER -> return Math.abs(diff) > granularity && diff < 0
        }
    }
}