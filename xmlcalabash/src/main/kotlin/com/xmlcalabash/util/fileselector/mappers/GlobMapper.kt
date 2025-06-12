package com.xmlcalabash.util.fileselector.mappers

open class GlobMapper(val from: String, val to: String, val caseSensitive: Boolean = true): Mapper {
    override fun selects(targetPaths: List<String>): List<String> {
        val opts = if (caseSensitive) {
            setOf()
        } else {
            setOf(RegexOption.IGNORE_CASE)
        }
        val fromPair = starGlob(from)
        val toPair = starGlob(to)

        val fromRegex = "${Regex.escape(fromPair.first)}(.*)${Regex.escape(fromPair.second)}".toRegex(opts)
        val results = mutableListOf<String>()
        for (path in targetPaths) {
            val matches = fromRegex.find(path) ?: continue
            results.add("${toPair.first}${matches.groupValues[1]}${toPair.second}")
        }

        return results
    }

    private fun starGlob(pattern: String): Pair<String,String> {
        val pos = pattern.indexOf("*")
        if (pos < 0) {
            throw IllegalArgumentException("Pattern must contain exactly one '*' character")
        }
        val pre = pattern.substring(0, pos)
        val post = pattern.substring(pos + 1)
        if (post.contains("*")) {
            throw IllegalArgumentException("Pattern must contain exactly one '*' character")
        }
        return Pair(pre, post)
    }

}