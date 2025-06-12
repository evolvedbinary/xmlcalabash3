package com.xmlcalabash.util.fileselector.mappers

class FlattenMapper: Mapper {
    override fun selects(targetPaths: List<String>): List<String> {
        val results = mutableListOf<String>()
        for (path in targetPaths) {
            val pos = path.lastIndexOf('/')
            val flat = if (pos >= 0) {
                path.substring(pos + 1)
            } else {
                path
            }
            if (flat !in results) {
                results.add(flat)
            }
        }
        return results
    }
}