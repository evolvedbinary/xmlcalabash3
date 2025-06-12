package com.xmlcalabash.util.fileselector.selectors

import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.fileselector.FSFile
import java.io.FileInputStream
import java.net.URI
import java.nio.charset.Charset

class ContainsSelector(val stepConfig: XProcStepConfiguration,
                       val text: String,
                       val caseSensitive: Boolean = true,
                       val ignoreWhitespace: Boolean = false,
                       val encoding: String = "UTF-8") : Selector {
    override fun selects(file: FSFile): Boolean {
        val charset = Charset.forName(encoding)
        val loader = DocumentLoader(stepConfig, URI(file.path.absolutePath))
        val doc = loader.load(FileInputStream(file.path), MediaType.TEXT, charset)

        val content = if (ignoreWhitespace) {
            doc.value.underlyingValue.stringValue.replace("\\s".toRegex(), "")
        } else {
            doc.value.underlyingValue.stringValue
        }

        val target = if (ignoreWhitespace) {
            text.replace("\\s".toRegex(), "")
        } else {
            text
        }

        if (caseSensitive) {
            return content.contains(target)
        }

        return content.lowercase().contains(target.lowercase())
    }
}