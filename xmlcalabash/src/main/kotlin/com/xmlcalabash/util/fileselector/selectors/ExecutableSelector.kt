package com.xmlcalabash.util.fileselector.selectors

import com.xmlcalabash.util.fileselector.FSFile

class ExecutableSelector(): Selector {
    override fun selects(file: FSFile): Boolean {
        return file.path.canExecute()
    }
}