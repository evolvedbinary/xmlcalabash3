package com.xmlcalabash.config

import com.xmlcalabash.io.MediaType
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.collections.iterator

class XmlCalabashTempOutput(port: String?, pattern: String): XmlCalabashOutput(port, pattern) {
    private var count = 1

    fun nextFile(port: String, contentType: MediaType?, contentTypes: Map<String,String>): File {
        val ext = when (contentType) {
                MediaType.GZIP -> "gzip"
                MediaType.YAML -> "yaml"
                MediaType.XHTML -> "xhtml"
                MediaType.XML -> "xml"
                MediaType.XQUERY -> "xqy"
                MediaType.XSLT -> "xsl"
                MediaType.JPEG -> "jpg"
                MediaType.TEXT -> "txt"
                null -> "xmlc"
                else -> {
                    var ext = "xmlc"
                    for ((cext, ctype) in contentTypes) {
                        if (contentType.toString() == ctype) {
                            ext = cext
                            break
                        }
                    }
                    ext
                }
            }

        val prefix = "xmlcalabash_${port}_${count.toString().padStart(3, '0')}_"
        count += 1

        if (pattern.isEmpty()) {
            return Files.createTempFile(prefix, ".${ext}").toFile()
        }

        val tempPath = Paths.get(pattern)
        return Files.createTempFile(tempPath, prefix, ".${ext}").toFile()
    }
}