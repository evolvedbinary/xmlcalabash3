package com.xmlcalabash.util.fileselector.selectors

import com.xmlcalabash.util.Urify
import com.xmlcalabash.util.fileselector.FSFile
import com.xmlcalabash.util.fileselector.mappers.Mapper
import java.io.File

class DependsSelector(val mapper: Mapper, val targetroot: File, granularityms: Int? = null): AbstractSelector() {
    private val granularity = granularityms ?: if (Urify.isWindows) 2000 else 0

    override fun selects(file: FSFile): Boolean {
        if (!targetroot.isDirectory) {
            throw IllegalArgumentException("Target path must be a directory")
        }
        val files = mapper.selects(listOf(file.localpath))

        for (filename in files) {
            val item = resolveAgainstDirectory(targetroot, filename)
            if (item.exists()) {
                if (item.lastModified() - file.path.lastModified() > granularity) {
                    return true
                }
            }
        }

        return false
    }
}