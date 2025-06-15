package com.xmlcalabash.util.fileselector

import com.xmlcalabash.util.fileselector.selectors.Selector
import java.io.File

open class FSFile(val parent: FSDirectory?, val path: File, val localpath: String, val depth: Int) {
    open fun files(): List<FSFile> {
        return listOf(this)
    }

    open fun files(selector: Selector): List<FSFile> {
        val files = mutableListOf<FSFile>()
        for (file in files()) {
            try {
                if (file.path.exists() && selector.selects(file)) {
                    files.add(file)
                }
            } catch (ex: Exception) {
                if (ex is IllegalArgumentException) {
                    throw ex
                }
                // ignore
            }
        }
        return files
    }

    override fun toString(): String {
        return "${path.absolutePath} :: ${localpath} :: ${depth}"
    }
}