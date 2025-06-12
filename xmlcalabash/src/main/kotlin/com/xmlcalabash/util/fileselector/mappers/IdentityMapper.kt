package com.xmlcalabash.util.fileselector.mappers

class IdentityMapper: Mapper {
    override fun selects(targetPaths: List<String>): List<String> {
        return targetPaths
    }
}