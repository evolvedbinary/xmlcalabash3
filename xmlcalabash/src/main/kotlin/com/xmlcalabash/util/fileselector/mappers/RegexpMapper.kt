package com.xmlcalabash.util.fileselector.mappers

open class RegexpMapper(val from: String, val to: String, val caseSensitive: Boolean = true): Mapper {
    override fun selects(targetPaths: List<String>): List<String> {
        val opts = if (caseSensitive) {
            setOf()
        } else {
            setOf(RegexOption.IGNORE_CASE)
        }

        /*
        val compiler = RECompiler()
        if (!caseSensitive) {
            compiler.setFlags(REFlags("i", "XP31"))
        } else {
            compiler.setFlags(REFlags("", "XP31"))
        }
        val program = compiler.compile(StringView.of(from))
        val matcher = REMatcher(program)
        */

        val fromRegex = from.toRegex(opts)
        val results = mutableListOf<String>()

        for (path in targetPaths) {
            //val iter = ARegexIterator(StringView.of(path), StringView.of(from), matcher)
            //matcher.match(path)
            val matches = fromRegex.find(path) ?: continue
            val sb = StringBuilder()

            // Don't accidentally replace text in replacements!
            val chars = to.toCharArray()
            var pos = 0;
            while (pos < chars.size) {
                if (chars[pos] == '\\'
                    && pos+1 < chars.size
                    && chars[pos+1] in listOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')) {
                    val index = chars[pos+1] - '0'
                    sb.append(matches.groupValues[index])
                    pos += 2
                } else {
                    sb.append(chars[pos])
                    pos++
                }
            }

            results.add(sb.toString())
        }

        return results
    }
}