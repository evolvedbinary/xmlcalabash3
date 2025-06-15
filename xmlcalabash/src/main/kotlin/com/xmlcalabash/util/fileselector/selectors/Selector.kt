package com.xmlcalabash.util.fileselector.selectors

import com.xmlcalabash.util.fileselector.FSFile

interface Selector {
    fun selects(file: FSFile): Boolean
}