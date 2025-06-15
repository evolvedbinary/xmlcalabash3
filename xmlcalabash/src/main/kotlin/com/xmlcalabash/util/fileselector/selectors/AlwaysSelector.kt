package com.xmlcalabash.util.fileselector.selectors

import com.xmlcalabash.util.fileselector.FSFile

/**
 * Always returns true.
 *
 * This selector is only used internally, it can't be created directly by a user.
 */
class AlwaysSelector() : Selector {
    override fun selects(file: FSFile): Boolean {
        return true
    }
}