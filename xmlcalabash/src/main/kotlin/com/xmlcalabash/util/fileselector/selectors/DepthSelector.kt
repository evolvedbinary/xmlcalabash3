package com.xmlcalabash.util.fileselector.selectors

import com.xmlcalabash.util.fileselector.FSFile

class DepthSelector(val min: Int, val max: Int): Selector {
    override fun selects(file: FSFile): Boolean {
        val parts = if (file.localpath.startsWith("/")) {
            file.localpath.substring(1).split("/").dropLast(1)
        } else {
            file.localpath.split("/").dropLast(1)
        }
        return parts.size >= min && parts.size <= max;
    }
}