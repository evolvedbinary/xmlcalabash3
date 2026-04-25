package com.xmlcalabash.test

import com.xmlcalabash.XmlCalabashBuilder
import com.xmlcalabash.app.CommandLine
import com.xmlcalabash.config.ConfigurationLoader
import com.xmlcalabash.config.XmlCalabashInput
import com.xmlcalabash.config.XmlCalabashOutput
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

class ConfigPipelineTest {
    @Test
    fun noConfigSource() {
        val builder = XmlCalabashBuilder()
        builder.update(CommandLine.parse(arrayOf("-i:source=source.xml")))
        compareInputs(listOf("source", "source.xml"), builder.inputs.getOrDefault())
        compareOutputs(emptyList(), builder.outputs.getOrDefault())
        compareOptions(emptyMap(), builder.options.getOrDefault())
    }

    @Test
    fun noConfigAll() {
        val builder = XmlCalabashBuilder()
        builder.update(CommandLine.parse(arrayOf("-i:source=source.xml", "-i:schema=schema.sch", "-o:result=result.xml", "-o:report=report.xml", "prime=3")))
        compareInputs(listOf("source", "source.xml", "schema", "schema.sch"), builder.inputs.getOrDefault())
        compareOutputs(listOf("result", "result.xml", "report", "report.xml"), builder.outputs.getOrDefault())
        compareOptions(mapOf("prime" to listOf("3")), builder.options.getOrDefault())
    }

    @Test
    fun allConfigNoPipeline() {
        val builder = XmlCalabashBuilder()
        builder.update(CommandLine.parse(arrayOf()))
        loadConfig(builder)

        compareInputs(listOf("source", "doc.xml", "schema", "doc.sch"), builder.inputs.getOrDefault())
        compareOutputs(listOf("result", "/tmp/result.xml", "report", "/tmp/report.xml"), builder.outputs.getOrDefault())
        compareOptions(mapOf("thing" to listOf("a thing")), builder.options.getOrDefault())
    }

    @Test
    fun allConfigSamePipeline() {
        val builder = XmlCalabashBuilder()
        builder.update(CommandLine.parse(arrayOf("/tmp/pipe.xpl")))
        loadConfig(builder)

        compareInputs(listOf("source", "doc.xml", "schema", "doc.sch"), builder.inputs.getOrDefault())
        compareOutputs(listOf("result", "/tmp/result.xml", "report", "/tmp/report.xml"), builder.outputs.getOrDefault())
        compareOptions(mapOf("thing" to listOf("a thing")), builder.options.getOrDefault())
    }

    @Test
    fun allConfigDifferentPipeline() {
        val builder = XmlCalabashBuilder()
        builder.update(CommandLine.parse(arrayOf("/tmp/other.xpl")))
        loadConfig(builder)

        compareInputs(emptyList(), builder.inputs.getOrDefault())
        compareOutputs(emptyList(), builder.outputs.getOrDefault())
        compareOptions(emptyMap(), builder.options.getOrDefault())
    }

    @Test
    fun overSource() {
        val builder = XmlCalabashBuilder()

        builder.update(CommandLine.parse(arrayOf("-i:source=source.xml")))
        loadConfig(builder)

        compareInputs(listOf("source", "source.xml", "schema", "doc.sch"), builder.inputs.getOrDefault())
        compareOutputs(listOf("result", "/tmp/result.xml", "report", "/tmp/report.xml"), builder.outputs.getOrDefault())
        compareOptions(mapOf("thing" to listOf("a thing")), builder.options.getOrDefault())
    }

    @Test
    fun overSchema() {
        val builder = XmlCalabashBuilder()

        builder.update(CommandLine.parse(arrayOf("-i:schema=schema.sch")))
        loadConfig(builder)

        compareInputs(listOf("schema", "schema.sch", "source", "doc.xml"), builder.inputs.getOrDefault())
        compareOutputs(listOf("result", "/tmp/result.xml", "report", "/tmp/report.xml"), builder.outputs.getOrDefault())
        compareOptions(mapOf("thing" to listOf("a thing")), builder.options.getOrDefault())
    }

    @Test
    fun overInputMultiplex() {
        val builder = XmlCalabashBuilder()

        builder.update(CommandLine.parse(arrayOf("--input-multiplex:source.mime")))
        loadConfig(builder)

        compareInputs(listOf("", "source.mime"), builder.inputs.getOrDefault())
        compareOutputs(listOf("result", "/tmp/result.xml", "report", "/tmp/report.xml"), builder.outputs.getOrDefault())
    }

    @Test
    fun overThing() {
        val builder = XmlCalabashBuilder()

        builder.update(CommandLine.parse(arrayOf("-i:schema=schema.sch", "thing=17")))
        loadConfig(builder)

        compareInputs(listOf("schema", "schema.sch", "source", "doc.xml"), builder.inputs.getOrDefault())
        compareOutputs(listOf("result", "/tmp/result.xml", "report", "/tmp/report.xml"), builder.outputs.getOrDefault())
        compareOptions(mapOf("thing" to listOf("17")), builder.options.getOrDefault())
    }

    @Test
    fun overAddOption() {
        val builder = XmlCalabashBuilder()

        builder.update(CommandLine.parse(arrayOf("-i:schema=schema.sch", "more=true")))
        loadConfig(builder)

        compareInputs(listOf("schema", "schema.sch", "source", "doc.xml"), builder.inputs.getOrDefault())
        compareOutputs(listOf("result", "/tmp/result.xml", "report", "/tmp/report.xml"), builder.outputs.getOrDefault())
        compareOptions(mapOf("thing" to listOf("a thing"), "more" to listOf("true")), builder.options.getOrDefault())
    }

    @Test
    fun multiplexConfig() {
        val builder = XmlCalabashBuilder()
        builder.update(CommandLine.parse(arrayOf()))
        loadConfig(builder, "config-multiplex.xml")

        compareInputs(listOf("", "source.mime"), builder.inputs.getOrDefault())
        compareOutputs(listOf("", "/tmp/output.mime"), builder.outputs.getOrDefault())
    }

    @Test
    fun multiplexConfigOverSource() {
        val builder = XmlCalabashBuilder()
        builder.update(CommandLine.parse(arrayOf("-i:source=source.xml")))
        loadConfig(builder, "config-multiplex.xml")

        compareInputs(listOf("source", "source.xml"), builder.inputs.getOrDefault())
        compareOutputs(listOf("", "/tmp/output.mime"), builder.outputs.getOrDefault())
    }

    @Test
    fun multiplexConfigOverResult() {
        val builder = XmlCalabashBuilder()
        builder.update(CommandLine.parse(arrayOf("-o:result=result.xml")))
        loadConfig(builder, "config-multiplex.xml")

        compareInputs(listOf("", "source.mime"), builder.inputs.getOrDefault())
        compareOutputs(listOf("result", "result.xml", "", "/tmp/output.mime"), builder.outputs.getOrDefault())
    }

    @Test
    fun multiplexConfigOverOutputMultiplex() {
        val builder = XmlCalabashBuilder()
        builder.update(CommandLine.parse(arrayOf("--output-multiplex:result.mime")))
        loadConfig(builder, "config-multiplex.xml")

        compareInputs(listOf("", "source.mime"), builder.inputs.getOrDefault())
        compareOutputs(listOf("", "result.mime"), builder.outputs.getOrDefault())
    }

    private fun compareInputs(expected: List<String>, inputs: List<XmlCalabashInput>?) {
        if (expected.isEmpty()) {
            Assertions.assertNull(inputs)
            return
        }

        inputs as List<XmlCalabashInput>
        Assertions.assertEquals(expected.size, inputs.size * 2)
        for ((index, input) in inputs.withIndex()) {
            val exidx = index * 2
            if (expected[exidx] == "") {
                Assertions.assertNull(input.port)
            } else {
                Assertions.assertEquals(expected[exidx], input.port)
            }
            // Cheap and cheerful
            val name = input.href!!.path.substring(input.href!!.path.lastIndexOf("/")+1)
            Assertions.assertEquals(expected[exidx+1], name)
        }
    }

    private fun compareOutputs(expected: List<String>, outputs: List<XmlCalabashOutput>?) {
        if (expected.isEmpty()) {
            Assertions.assertNull(outputs)
            return
        }

        outputs as List<XmlCalabashOutput>
        Assertions.assertEquals(expected.size, outputs.size * 2)
        for ((index, output) in outputs.withIndex()) {
            val exidx = index * 2
            if (expected[exidx] == "") {
                Assertions.assertNull(output.port)
            } else {
                Assertions.assertEquals(expected[exidx], output.port)
            }
            // Cheap and cheerful
            Assertions.assertEquals(expected[exidx+1], output.pattern)
        }
    }

    private fun compareOptions(expected: Map<String, List<Any>>, values: Map<String,List<Any>>?) {
        if (expected.isEmpty()) {
            Assertions.assertNull(values)
            return
        }

        values as Map<String,List<Any>>
        Assertions.assertEquals(expected.size, values.size)
        for ((key, value) in values) {
            val expected = expected[key]!!
            Assertions.assertEquals(expected.size, value.size)
            val citer = expected.iterator()
            val riter = value.iterator()
            while (citer.hasNext()) {
                val cvalue = citer.next()
                val rvalue = riter.next()
                Assertions.assertEquals(cvalue, rvalue)
            }
        }
    }

    private fun loadConfig(builder: XmlCalabashBuilder, configFile: String = "config-pipeline.xml") {
        val loader = ConfigurationLoader()
        val config = File("src/test/resources/${configFile}")
        Assertions.assertTrue(config.exists())
        val cbuilder = loader.load(config)
        Assertions.assertNotNull(cbuilder)
        builder.update(cbuilder)
    }
}
