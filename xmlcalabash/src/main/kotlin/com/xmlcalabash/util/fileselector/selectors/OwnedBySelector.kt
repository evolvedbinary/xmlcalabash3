package com.xmlcalabash.util.fileselector.selectors

import com.xmlcalabash.util.fileselector.FSFile
import java.nio.file.Files
import java.nio.file.LinkOption

class OwnedBySelector(val owner: String, val followSymlinks: Boolean = true): Selector {
    override fun selects(file: FSFile): Boolean {
        val user = if (followSymlinks) {
            Files.getOwner(file.path.toPath())
        } else {
            Files.getOwner(file.path.toPath(), LinkOption.NOFOLLOW_LINKS)
        }

        return user.name == owner
    }
}