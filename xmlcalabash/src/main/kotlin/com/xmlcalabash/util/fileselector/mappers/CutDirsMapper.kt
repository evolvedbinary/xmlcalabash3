package com.xmlcalabash.util.fileselector.mappers

class CutDirsMapper(val dirs: Int): Mapper {
    override fun selects(targetPaths: List<String>): List<String> {
        val results = mutableListOf<String>()
        for (path in targetPaths) {
            var pos = if (path.startsWith("/")) 1 else 0
            var matches = true
            for (count in 1 .. dirs) {
                val idx = path.indexOf('/', pos)
                if (idx < 0) {
                    matches = false
                    break
                }
                pos = idx + 1
            }
            if (matches) {
                results.add(path.substring(pos))
            }
        }
        return results
    }
}