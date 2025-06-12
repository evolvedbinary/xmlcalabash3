package com.xmlcalabash.util.fileselector.selectors

import com.xmlcalabash.util.Urify
import com.xmlcalabash.util.fileselector.FSFile
import com.xmlcalabash.util.fileselector.mappers.Mapper
import java.io.File
import java.io.FileInputStream

class DifferentSelector(val mapper: Mapper,
                        val targetroot: File,
                        val ignoreFileTimes: Boolean = true,
                        val ignoreContents: Boolean = false,
                        granularityms: Int? = null) : AbstractSelector() {
    private val granularity = granularityms ?: if (Urify.isWindows) 2000 else 0

    override fun selects(file: FSFile): Boolean {
        if (!targetroot.isDirectory) {
            throw IllegalArgumentException("Target path must be a directory")
        }

        val files = mapper.selects(listOf(file.localpath))

        for (filename in files) {
            val item = resolveAgainstDirectory(targetroot, filename)
            if (item.exists()) {
                if (file.path.length() != item.length()) {
                    return true
                }
                if (!ignoreFileTimes) {
                    if (Math.abs(file.path.lastModified() - item.lastModified()) > granularity) {
                        return true
                    }
                }
                if (!ignoreContents) {
                    if (!same(file.path, item)) {
                        return true
                    }
                }
                return false
            }
            return true
        }

        return false
    }

    private fun same(path: File, file: File): Boolean {
        FileInputStream(path).use { sourceStream ->
            FileInputStream(file).use { targetStream ->
                // FIXME: this is a bit memory risky
                val source = sourceStream.readAllBytes()
                val target = targetStream.readAllBytes()
                return source.size == target.size && source contentEquals target
            }
        }
    }
}