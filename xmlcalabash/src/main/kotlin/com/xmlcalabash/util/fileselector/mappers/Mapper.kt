package com.xmlcalabash.util.fileselector.mappers

import com.xmlcalabash.util.fileselector.FSFile
import java.io.File

interface Mapper {
    fun selects(targetPaths: List<String>): List<String>
}