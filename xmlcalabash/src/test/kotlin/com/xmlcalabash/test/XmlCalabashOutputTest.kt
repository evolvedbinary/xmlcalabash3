package com.xmlcalabash.test

import com.xmlcalabash.config.XmlCalabashOutput
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class XmlCalabashOutputTest {
    @Test
    fun testNotPatterned() {
        val outfn = XmlCalabashOutput(null, "test.xml")
        Assertions.assertFalse(outfn.isSequential())
        Assertions.assertEquals("test.xml", outfn.nextFile().name)
        Assertions.assertEquals("test.xml", outfn.nextFile().name)
    }

    @Test
    fun testSimplePatternedNoLeadingZeros() {
        val outfn = XmlCalabashOutput(null, "test%2d.xml")
        Assertions.assertTrue(outfn.isSequential())
        Assertions.assertEquals("test 1.xml", outfn.nextFile().name)
        Assertions.assertEquals("test 2.xml", outfn.nextFile().name)
    }

    @Test
    fun testSimplePatterned() {
        val outfn = XmlCalabashOutput(null, "test%02d.xml")
        Assertions.assertTrue(outfn.isSequential())
        Assertions.assertEquals("test01.xml", outfn.nextFile().name)
        Assertions.assertEquals("test02.xml", outfn.nextFile().name)
    }

    @Test
    fun testMultiplePatterned() {
        val outfn = XmlCalabashOutput(null, "test%02d_%x.xml")
        Assertions.assertTrue(outfn.isSequential())
        Assertions.assertEquals("test01_1.xml", outfn.nextFile().name)
        for (index in 2 .. 9) {
            outfn.nextFile()
        }
        Assertions.assertEquals("test10_a.xml", outfn.nextFile().name)
    }

    @Test
    fun testLiteralPercent() {
        val outfn = XmlCalabashOutput(null, "test%%.xml")
        Assertions.assertFalse(outfn.isSequential())
        Assertions.assertEquals("test%.xml", outfn.nextFile().name)
        Assertions.assertEquals("test%.xml", outfn.nextFile().name)
    }

    @Test
    fun testBothLiteralPercent() {
        val outfn = XmlCalabashOutput(null, "test%%%02X%%.xml")
        Assertions.assertTrue(outfn.isSequential())
        Assertions.assertEquals("test%01%.xml", outfn.nextFile().name)
        for (index in 2 .. 8) {
            outfn.nextFile()
        }
        Assertions.assertEquals("test%09%.xml", outfn.nextFile().name)
        Assertions.assertEquals("test%0A%.xml", outfn.nextFile().name)
    }

    @Test
    fun testBothMultiplePatterned() {
        val outfn = XmlCalabashOutput(null, "test%02d%%%x.xml")
        Assertions.assertTrue(outfn.isSequential())
        Assertions.assertEquals("test01%1.xml", outfn.nextFile().name)
        for (index in 2 .. 8) {
            outfn.nextFile()
        }
        Assertions.assertEquals("test09%9.xml", outfn.nextFile().name)
        Assertions.assertEquals("test10%a.xml", outfn.nextFile().name)
    }


}