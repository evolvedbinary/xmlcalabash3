package com.xmlcalabash.util.fileselector.selectors

import java.io.File

abstract class AbstractSelector: Selector {
    protected fun resolveAgainstDirectory(dir: File, path: String): File {
        if (path.startsWith("/")) {
            return dir.resolve(path.substring(1))
        } else {
            return dir.resolve(path)
        }
    }
}