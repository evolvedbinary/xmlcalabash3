package com.xmlcalabash.util.fileselector.mappers

class UnpackageMapper(from: String, to: String, caseSensitive: Boolean = true): RegexpMapper(from, to, caseSensitive) {
    override fun selects(targetPaths: List<String>): List<String> {
        val globResults = super.selects(targetPaths)
        val results = mutableListOf<String>()
        for (result in globResults) {
            val pos = result.lastIndexOf('.')
            val unpackage = result.replace('.', '/')
            if (pos >= 0) {
                results.add("${unpackage.substring(0, pos)}.${unpackage.substring(pos + 1)}")
            } else {
                results.add(result)
            }
        }
        return results
    }
}