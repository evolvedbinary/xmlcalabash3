package com.xmlcalabash.util.fileselector.selectors

import com.xmlcalabash.util.fileselector.FSFile

class WritableSelector(): Selector {
    override fun selects(file: FSFile): Boolean {
        return file.path.canWrite()
    }
}