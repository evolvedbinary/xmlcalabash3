package com.xmlcalabash.util.fileselector.selectors

import com.xmlcalabash.util.fileselector.FSFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.readAttributes

class PosixPermissionsSelector(val userperm: String, val followSymlinks: Boolean = true): Selector {
    val permissions: Int

    init {
        var perms: Int? = null
        try {
            perms = Integer.parseInt(userperm, 8)
        } catch (_: NumberFormatException) {
            // must not be octal...
        }
        if (perms != null) {
            permissions = perms
        } else {
            if (userperm.length != 9) {
                throw IllegalArgumentException("User permissions must be octal or 9 characters")
            }
            val validChars = arrayOf('r', 'w', 'x')
            val chars = userperm.toCharArray()
            var value = 0
            var factor = 1
            for (pos in 8 downTo 0) {
                if (chars[pos] != '-') {
                    if (chars[pos] != validChars[pos % 3]) {
                        throw IllegalArgumentException("Invalid permissions, '${chars[pos]}' found where '${validChars[pos % 3]}' or '-' expected.")
                    }
                    value += factor
                }
                factor *= 2
            }
            permissions = value
        }
    }

    override fun selects(file: FSFile): Boolean {
        try {
            val attr = if (followSymlinks) {
                Files.readAttributes(file.path.toPath(), PosixFileAttributes::class.java)
            } else {
                Files.readAttributes(file.path.toPath(), PosixFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
            }
            val perm = attr.permissions()
            // Very weird that I have to do it this way
            val bits = arrayOf(
                PosixFilePermission.OTHERS_EXECUTE, PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_READ,
                PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_READ,
                PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_READ)
            var factor = 1
            var value = 0
            for (bit in bits) {
                if (perm.contains(bit)) {
                    value += factor
                }
                factor *= 2
            }
            return value == permissions
        } catch (_: IOException) {
            return false
        }
    }
}