package com.xmlcalabash.util.fileselector.selectors

import com.xmlcalabash.util.fileselector.FSFile
import java.io.File

class OrSelector(val selectors: List<Selector>) : Selector {
    override fun selects(file: FSFile): Boolean {
        for (selector in selectors) {
            if (selector.selects(file)) {
                return true
            }
        }

        return false
    }
}