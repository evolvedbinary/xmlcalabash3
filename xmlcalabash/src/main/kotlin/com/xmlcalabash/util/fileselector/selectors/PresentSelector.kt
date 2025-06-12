package com.xmlcalabash.util.fileselector.selectors

import com.xmlcalabash.util.fileselector.FSFile
import com.xmlcalabash.util.fileselector.mappers.Mapper
import java.io.File

class PresentSelector(val mapper: Mapper, val targetroot: File, val present: Present = Present.BOTH): AbstractSelector() {
    override fun selects(file: FSFile): Boolean {
        if (!targetroot.isDirectory) {
            throw IllegalArgumentException("Target path must be a directory")
        }
        val files = mapper.selects(listOf(file.localpath))

        var targetExists = false
        for (filename in files) {
            val item = resolveAgainstDirectory(targetroot, filename)
            if (item.exists()) {
                targetExists = true
                break
            }
        }

        if (present == Present.BOTH) {
            return targetExists
        }

        return !targetExists
    }
}