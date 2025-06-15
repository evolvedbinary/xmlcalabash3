package com.xmlcalabash.util.fileselector.selectors

import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.fileselector.FSFile
import java.io.File
import java.io.FileInputStream
import java.net.URI
import java.nio.charset.Charset

class ContainsRegexpSelector(val stepConfig: XProcStepConfiguration,
                             val expression: String,
                             val caseSensitive: Boolean = true,
                             val encoding: String = "UTF-8",
                             val multiline: Boolean = false,
                             val singleline: Boolean = false) : Selector {
    override fun selects(file: FSFile): Boolean {
        val charset = Charset.forName(encoding)
        val loader = DocumentLoader(stepConfig, URI(file.path.absolutePath))
        val doc = loader.load(FileInputStream(file.path), MediaType.TEXT, charset)

        val content = doc.value.underlyingValue.stringValue
        val opts = mutableSetOf<RegexOption>()
        if (!caseSensitive) {
            opts.add(RegexOption.IGNORE_CASE)
        }
        if (multiline) {
            opts.add(RegexOption.MULTILINE)
        }
        if (singleline) {
            opts.add(RegexOption.DOT_MATCHES_ALL)
        }

        val regex = expression.toRegex(opts)

        if (singleline || multiline) {
            return regex.containsMatchIn(content)
        }
        for (line in content.lineSequence()) {
            if (regex.containsMatchIn(line)) {
                return true
            }
        }
        return false
    }
}