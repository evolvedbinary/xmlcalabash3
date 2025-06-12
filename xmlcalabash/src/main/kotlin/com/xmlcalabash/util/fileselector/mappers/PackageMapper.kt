package com.xmlcalabash.util.fileselector.mappers

class PackageMapper(from: String, to: String, caseSensitive: Boolean = true): RegexpMapper(from, to, caseSensitive) {
    override fun selects(targetPaths: List<String>): List<String> {
        val results = super.selects(targetPaths)
        val packages = mutableListOf<String>()
        for (result in results) {
            if (result.startsWith("/")) {
                packages.add(result.substring(1).replace('/', '.'))
            } else {
                packages.add(result.replace('/', '.'))
            }
        }
        return packages
    }
}