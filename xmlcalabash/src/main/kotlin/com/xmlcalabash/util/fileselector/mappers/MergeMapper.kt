package com.xmlcalabash.util.fileselector.mappers

class MergeMapper(val to: String): Mapper {
    override fun selects(targetPaths: List<String>): List<String> {
        return listOf(to)
    }
}