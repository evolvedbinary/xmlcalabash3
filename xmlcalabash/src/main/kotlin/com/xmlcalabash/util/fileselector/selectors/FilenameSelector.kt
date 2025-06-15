package com.xmlcalabash.util.fileselector.selectors

import com.xmlcalabash.util.fileselector.FSFile
import com.xmlcalabash.util.fileselector.Matcher

class FilenameSelector(val name: String?, val regexp: String?, val caseSensitive: Boolean = true, val negate: Boolean = false): Selector {
    override fun selects(file: FSFile): Boolean {
        val matches = if (name != null) {
            Matcher.matchesGlob(name, file.localpath, caseSensitive)
        } else {
            Matcher.matchesRegex(regexp!!, file.localpath, caseSensitive)
        }
        return if (negate) !matches else matches
    }
}