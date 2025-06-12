package com.xmlcalabash.util.fileselector.selectors

import com.xmlcalabash.util.fileselector.FSFile

class MajoritySelector(val selectors: List<Selector>, val allowTie: Boolean = true) : Selector {
    override fun selects(file: FSFile): Boolean {
        var pass = 0
        var fail = 0
        for (selector in selectors) {
            if (selector.selects(file)) {
                pass++
            } else {
                fail++
            }
        }
        return pass > fail || (allowTie && pass == fail)
    }
}