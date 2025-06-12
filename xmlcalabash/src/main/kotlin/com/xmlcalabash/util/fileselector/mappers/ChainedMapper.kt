package com.xmlcalabash.util.fileselector.mappers

class ChainedMapper(val mappers: List<Mapper>): Mapper {
    override fun selects(targetPaths: List<String>): List<String> {
        val localResults = mutableListOf<String>()
        localResults.addAll(targetPaths)

        val results = mutableListOf<String>()
        for (chained in mappers) {
            results.clear()
            results.addAll(chained.selects(localResults))
            localResults.clear()
            localResults.addAll(results)
        }

        return results
    }
}