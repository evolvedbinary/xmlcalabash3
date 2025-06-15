package com.xmlcalabash.util.fileselector.selectors

import com.xmlcalabash.util.fileselector.FSFile

class SizeSelector(value: Long, units: String? = null, val whenSize: WhenSize = WhenSize.EQUAL): Selector {
    val size: Long = when (units) {
        null -> value
        "da" -> value * 10
        "h" -> value * 100
        "k" -> value * 1_000
        "M" -> value * 1_000_000
        "G" -> value * 1_000_000_000
        "T" -> value * 1_000_000_000_000
        "P" -> value * 1_000_000_000_000_000
        "E" -> value * 1_000_000_000_000_000_000
        "Ki" -> value * 0b100_0000_0000
        "Mi" -> value * 0b1_0000_0000_0000_0000_0000
        "Gi" -> value * 0b100_0000_0000_0000_0000_0000_0000_0000
        "Ti" -> value * 0b1_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000
        "Pi" -> value * 0b100_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000
        "Ei" -> value * 0b1_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000
        else -> throw IllegalArgumentException("Unknown units: ${units}")
    }

    override fun selects(file: FSFile): Boolean {
        when (whenSize) {
            WhenSize.LESS -> return file.path.length() < size
            WhenSize.MORE -> return file.path.length() > size
            WhenSize.EQUAL -> return file.path.length() == size
        }
    }
}