package com.xmlcalabash.util.fileselector.selectors

import com.xmlcalabash.util.fileselector.FSFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.attribute.PosixFileAttributes
import kotlin.io.path.readAttributes

class PosixGroupSelector(val group: String, val followSymlinks: Boolean = true): Selector {
    override fun selects(file: FSFile): Boolean {
        try {
            val attr = if (followSymlinks) {
                Files.readAttributes(file.path.toPath(), PosixFileAttributes::class.java)
            } else {
                Files.readAttributes(file.path.toPath(), PosixFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
            }

            return attr.group().name == group
        } catch (_: IOException) {
            return false
        }
    }
}