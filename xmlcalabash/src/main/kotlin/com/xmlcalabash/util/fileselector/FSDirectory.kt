package com.xmlcalabash.util.fileselector

import java.io.File

class FSDirectory(parent: FSDirectory?, path: File, localpath: String, depth: Int): FSFile(parent, path, localpath, depth) {
    internal val _contents = mutableListOf<FSFile>()
    val contents: List<FSFile>
        get() = _contents

    override fun files(): List<FSFile> {
        val files = mutableListOf<FSFile>()
        for (file in _contents) {
            files.addAll(file.files())
        }
        return files
    }

    internal fun add(file: FSFile) {
        _contents.add(file)
    }

    override fun toString(): String {
        return "${path.absolutePath} :: ${localpath} :: ${depth} (${_contents.size} files)"
    }
}