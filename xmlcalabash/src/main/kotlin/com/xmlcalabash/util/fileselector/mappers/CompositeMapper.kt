package com.xmlcalabash.util.fileselector.mappers

class CompositeMapper(val mappers: List<Mapper>): Mapper {
    override fun selects(targetPaths: List<String>): List<String> {
        val results = mutableListOf<String>()
        for (mapper in mappers) {
            results.addAll(mapper.selects(targetPaths))
        }
        return results
    }
}