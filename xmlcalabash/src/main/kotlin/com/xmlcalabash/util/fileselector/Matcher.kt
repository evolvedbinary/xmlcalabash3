package com.xmlcalabash.util.fileselector

object Matcher {
    fun matchesGlob(inputGlob: String, inputPath: String, caseSensitive: Boolean = false): Boolean {
        val glob = if (caseSensitive) {
            inputGlob
        } else {
            inputGlob.lowercase()
        }

        val path = if (caseSensitive) {
            inputPath
        } else {
            inputPath.lowercase()
        }

        val regexparts = mutableListOf<String>()
        var pattern = if (glob.startsWith("/")) {
            glob
        } else {
            "**/${glob}"
        }

        if (pattern.endsWith("**")) {
            pattern += "/*"
        }

        var rpos = regexPos(pattern)
        while (rpos >= 0) {
            if (rpos > 0) {
                regexparts.add(Regex.escape(pattern.substring(0, rpos)))
                pattern = pattern.substring(rpos)
            }
            if (pattern.startsWith("**/")) {
                regexparts.add("**")
                pattern = pattern.substring(3)
            } else if (pattern.startsWith("*")) {
                regexparts.add("*")
                pattern = pattern.substring(1)
            } else {
                regexparts.add("?")
                pattern = pattern.substring(1)
            }
            rpos = regexPos(pattern)
        }
        if (pattern.isNotEmpty()) {
            regexparts.add(Regex.escape(pattern))
        }

        val sb = StringBuilder()
        for (part in regexparts) {
            when (part) {
                "?" -> sb.append(".")
                "*" -> sb.append(".*")
                "**" ->sb.append(".*/")
                else -> sb.append(part)
            }
        }

        val matches = sb.toString().toRegex().matches(path)
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

    fun matchesRegex(regexp: String, path: String, caseSensitive: Boolean = false): Boolean {
        val opts = if (caseSensitive) {
            setOf()
        } else {
            setOf(RegexOption.IGNORE_CASE)
        }
        return regexp.toRegex(opts).find(path) != null
    }
}