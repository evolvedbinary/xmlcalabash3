package com.xmlcalabash.util.fileselector.selectors

import com.xmlcalabash.util.fileselector.FSFile

class AndSelector(val selectors: List<Selector>) : Selector {
    override fun selects(file: FSFile): Boolean {
        for (selector in selectors) {
            if (!selector.selects(file)) {
                return false
            }
        }

        return true
    }
}