package com.xmlcalabash.util.fileselector.selectors

import com.xmlcalabash.util.fileselector.FSDirectory
import com.xmlcalabash.util.fileselector.FSFile

class TypeSelector(val fileType: FileType): Selector {
    override fun selects(file: FSFile): Boolean {
        return (fileType == FileType.DIRECTORY && file is FSDirectory)
            || (fileType == FileType.FILE && file !is FSDirectory)
    }
}