package com.xmlcalabash.util.fileselector.selectors

import com.xmlcalabash.util.fileselector.FSFile
import java.io.File

class NotSelector(val selector: Selector) : Selector {
    override fun selects(file: FSFile): Boolean {
        return !selector.selects(file)
    }
}