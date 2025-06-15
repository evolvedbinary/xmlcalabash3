package com.xmlcalabash.util.fileselector.mappers

class FirstMatchMapper(val mappers: List<Mapper>): Mapper {
    override fun selects(targetPaths: List<String>): List<String> {
        for (mapper in mappers) {
            val results = mapper.selects(targetPaths)
            if (results.isNotEmpty()) {
                return results
            }
        }
        return listOf()
    }
}