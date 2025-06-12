package com.xmlcalabash.util.fileselector

import com.xmlcalabash.util.Urify
import java.io.File
import java.net.URI

class FileSelector(val includeGlobs: List<String> = listOf(),
                   val excludeGlobs: List<String> = listOf(),
                   val defaultExcludes: Boolean = true) {
    lateinit var rootpath: String

    fun directory(path: File): FSDirectory {
        return directory(URI(Urify.urify(path.canonicalPath)))
    }

    fun directory(root: URI): FSDirectory {
        val path = mustBeFile(root, true)
        rootpath = Urify.urify(if (root.path.endsWith("/")) root.path else "${root.path}/")
        val dir = recurse(null, path, "/", 1)
        return dir
    }

    fun file(path: File): FSFile? {
        return file(URI(Urify.urify(path.canonicalPath)))
    }

    fun file(root: URI): FSFile? {
        val path = mustBeFile(root, false)
        rootpath = Urify.urify(root.path)
        return if (matches(rootpath)) {
            FSFile(null, path, rootpath, 0)
        } else {
            null
        }
    }

    private fun mustBeFile(root: URI, mustBeDirectory: Boolean): File {
        if (!root.isAbsolute) {
            throw IllegalArgumentException("Path must be absolute")
        }
        if (root.scheme != "file") {
            throw IllegalArgumentException("Path must be file: URI")
        }
        val path = File(root.path)
        if (mustBeDirectory != path.isDirectory) {
            if (mustBeDirectory) {
                throw IllegalArgumentException("Path must be directory: ${root}")
            }
            throw IllegalArgumentException("Path must be file: ${root}")
        }
        return path
    }

    private fun recurse(parent: FSDirectory?, path: File, local: String, depth: Int): FSDirectory {
        val localuri = Urify.urify("${path.absolutePath}/")
        val localpath = if (localuri == rootpath) {
            rootpath
        } else {
            localuri.substring(rootpath.length)
        }

        val directory = FSDirectory(parent, path, localpath, depth)
        for (file in path.listFiles()!!) {
            val filepath = Urify.urify(file.absolutePath).substring(rootpath.length - 1)
            if (file.isDirectory) {
                directory.add(recurse(directory,file, filepath,depth + 1))
            } else {
                if (matches(filepath)) {
                    directory.add(FSFile(directory,file, filepath, depth))
                }
            }
        }

        return directory
    }

    private fun matches(localpath: String): Boolean {
        if (includeGlobs.isNotEmpty()) {
            var included = false
            for (include in includeGlobs) {
                if (matchesGlob(include, localpath)) {
                    included = true
                    break
                }
            }
            if (!included) {
                return false
            }
        }

        if (defaultExcludes && defaultExclude(localpath)) {
            return false
        }

        for (exclude in excludeGlobs) {
            if (matchesGlob(exclude, localpath)) {
                return false
            }
        }

        return true
    }

    private fun matchesGlob(globPart: String, path: String): Boolean {
        val regexparts = mutableListOf<String>()
        var glob = if (globPart.startsWith("/")) {
            globPart
        } else {
            "**/${globPart}"
        }

        if (glob.endsWith("**")) {
            glob += "/*"
        }

        var rpos = regexPos(glob)
        while (rpos >= 0) {
            if (rpos > 0) {
                regexparts.add(Regex.escape(glob.substring(0, rpos)))
                glob = glob.substring(rpos)
            }
            if (glob.startsWith("**/")) {
                regexparts.add("**")
                glob = glob.substring(3)
            } else if (glob.startsWith("*")) {
                regexparts.add("*")
                glob = glob.substring(1)
            } else {
                regexparts.add("?")
                glob = glob.substring(1)
            }
            rpos = regexPos(glob)
        }
        if (glob.isNotEmpty()) {
            regexparts.add(Regex.escape(glob))
        }

        val sb = StringBuilder()
        for (part in regexparts) {
            when (part) {
                "?" -> sb.append("[^/]")
                "*" -> sb.append("[^/]*")
                "**" ->sb.append("(.*/)?")
                else -> sb.append(part)
            }
        }

        val regex = sb.toString().toRegex()
        val matchPath = if (globPart.startsWith("/")) {
            path
        } else {
            path.substring(1)
        }

        val matches = regex.matches(matchPath)
        return matches
    }

    private fun regexPos(glob: String): Int {
        val indexes = mutableListOf<Int>()
        var pos = glob.indexOf('?')
        if (pos >= 0) {
            indexes.add(pos)
        }
        pos = glob.indexOf("**/")
        if (pos >= 0) {
            indexes.add(pos)
        }
        pos = glob.indexOf("*")
        if (pos >= 0) {
            indexes.add(pos)
        }
        if (indexes.isEmpty()) {
            return -1
        }
        return indexes.min()
    }

    private fun defaultExclude(path: String): Boolean {
        val lastSlash = path.lastIndexOf('/')
        val filename = if (lastSlash >= 0) {
            path.substring(lastSlash + 1)
        } else {
            path
        }

        // This is hardcoded because it's faster...

        if (filename in listOf(
                ".DS_Store",
                ".bzr", ".bzrignore",
                ".git", ".gitattributes", ".gitignore", ".gitmodules",
                ".hg", ".hgignore", ".hgsub", ".hgsubstate", ".hgtags",
                ".svn",
                "CVS", ".cvsignore",
                "SCCS",
                "vssver.scc"
            )) {
            return true
        }

        if (filename.endsWith("~")                                          // **/*~
            || filename.startsWith(".#")                                    // **/.#*
            || filename.startsWith("._")                                    // **/._*
            || (filename.startsWith("#") && filename.endsWith("#")) // **/#*#
            || (filename.startsWith("%") && filename.endsWith("%")) // **/%*%
        ) {
            return true
        }

        return path.contains("/CVS/")
                || path.contains("/SCCS/")
                || path.contains("/.svn/")
                || path.contains("/.git/")
                || path.contains("/.hg/")
                || path.contains("/.bzr/")
    }
}