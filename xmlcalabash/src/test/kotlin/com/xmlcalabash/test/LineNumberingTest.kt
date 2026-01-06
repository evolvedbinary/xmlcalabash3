package com.xmlcalabash.test

import com.xmlcalabash.XmlCalabashBuilder
import com.xmlcalabash.util.BufferingReceiver
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.XdmNode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.io.File

class LineNumberingTest {
    @Test
    fun rngFail1() {
        val builder = XmlCalabashBuilder()
        builder.lineNumbering.set(false)

        val pipeline = File("src/test/resources/rng-fail-1.xpl").toURI()

        val calabash = builder.build()
        val parser = calabash.newXProcParser()
        val decl = parser.parse(pipeline)
        val exec = decl.getExecutable()

        val receiver = BufferingReceiver()
        try {
            exec.receiver = receiver
            exec.run()
        } catch (ex: Exception) {
            System.err.println(ex.message)
            fail()
        }

        val result = receiver.outputs["result"]!!.first()
        val node = S9Api.documentElement(result.value as XdmNode).toString()

        Assertions.assertTrue("<xvrl:location href=" in node)
        Assertions.assertTrue("resources/vtestdoc.xml" in node);
        Assertions.assertFalse("line=" in node)
    }

    @Test
    fun rngFail2() {
        val builder = XmlCalabashBuilder()
        builder.lineNumbering.set(true)

        val pipeline = File("src/test/resources/rng-fail-1.xpl").toURI()

        val calabash = builder.build()
        val parser = calabash.newXProcParser()
        val decl = parser.parse(pipeline)
        val exec = decl.getExecutable()

        val receiver = BufferingReceiver()
        try {
            exec.receiver = receiver
            exec.run()
        } catch (ex: Exception) {
            System.err.println(ex.message)
            fail()
        }

        val result = receiver.outputs["result"]!!.first()
        val node = S9Api.documentElement(result.value as XdmNode).toString()

        Assertions.assertTrue("<xvrl:location href=" in node)
        Assertions.assertTrue("resources/vtestdoc.xml" in node);
        Assertions.assertTrue("line=\"5\"" in node || "line='5'" in node);
    }

    @Test
    fun rngFail3() {
        val builder = XmlCalabashBuilder()
        builder.lineNumbering.set(false)

        val pipeline = File("src/test/resources/rng-fail-2.xpl").toURI()

        val calabash = builder.build()
        val parser = calabash.newXProcParser()
        val decl = parser.parse(pipeline)
        val exec = decl.getExecutable()

        val receiver = BufferingReceiver()
        try {
            exec.receiver = receiver
            exec.run()
        } catch (ex: Exception) {
            System.err.println(ex.message)
            fail()
        }

        val result = receiver.outputs["result"]!!.first()
        val node = S9Api.documentElement(result.value as XdmNode).toString()

        Assertions.assertTrue("<xvrl:location href=" in node)
        Assertions.assertTrue("resources/vtestdoc.xml" in node);
        Assertions.assertTrue("line=\"5\"" in node || "line='5'" in node);
    }

    @Test
    fun rngFail4() {
        val builder = XmlCalabashBuilder()
        builder.lineNumbering.set(true)

        val pipeline = File("src/test/resources/rng-fail-3.xpl").toURI()

        val calabash = builder.build()
        val parser = calabash.newXProcParser()
        val decl = parser.parse(pipeline)
        val exec = decl.getExecutable()

        val receiver = BufferingReceiver()
        try {
            exec.receiver = receiver
            exec.run()
        } catch (ex: Exception) {
            System.err.println(ex.message)
            fail()
        }

        val result = receiver.outputs["result"]!!.first()
        val node = S9Api.documentElement(result.value as XdmNode).toString()

        Assertions.assertTrue("<xvrl:location href=" in node)
        Assertions.assertTrue("resources/vtestdoc.xml" in node)
        Assertions.assertFalse("line=" in node)
    }

}