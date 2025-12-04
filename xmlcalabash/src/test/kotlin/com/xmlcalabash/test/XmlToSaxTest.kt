package com.xmlcalabash.test

import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.XmlToSax
import net.sf.saxon.s9api.Processor
import net.sf.saxon.s9api.XdmNode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

class XmlToSaxTest {
    @Test
    fun xmlToSax() {
        val processor = Processor(false)
        val builder = processor.newDocumentBuilder()
        builder.isLineNumbering = true
        val node = builder.build(File("src/test/resources/document.xml"))
        val numbers = lineNumbers(node)
        Assertions.assertTrue(numbers.isNotEmpty())
        Assertions.assertTrue(numbers.first().first > 0)

        val source = XmlToSax.asSaxSource(node)
        val node2 = builder.build(source)

        for ((index, pair) in lineNumbers(node2).withIndex()) {
            Assertions.assertEquals(numbers[index], pair)
        }
    }

    private fun lineNumbers(node: XdmNode): List<Pair<Int,Int>> {
        val list = mutableListOf<Pair<Int,Int>>()
        val documentElement = S9Api.documentElement(node)
        list.add(Pair(documentElement.lineNumber, documentElement.columnNumber))
        for (child in documentElement.children()) {
            list.add(Pair(child.lineNumber, child.columnNumber))
        }
        return list
    }
}