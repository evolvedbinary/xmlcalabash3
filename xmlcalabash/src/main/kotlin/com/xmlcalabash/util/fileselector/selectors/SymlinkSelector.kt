package com.xmlcalabash.util.fileselector.selectors

import com.xmlcalabash.util.fileselector.FSFile
import java.nio.file.Files

class SymlinkSelector(): Selector {
    override fun selects(file: FSFile): Boolean {
        return Files.isSymbolicLink(file.path.toPath())
    }
}