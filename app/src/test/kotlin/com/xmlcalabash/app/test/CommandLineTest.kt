package com.xmlcalabash.app.test

import com.xmlcalabash.app.CommandLine
import com.xmlcalabash.config.XmlCalabashInput
import com.xmlcalabash.XmlCalabashBuilder
import com.xmlcalabash.config.XmlCalabashOutput
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.util.AssertionsLevel
import com.xmlcalabash.util.ExtensionName
import com.xmlcalabash.util.Verbosity
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.ValidationMode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URI

class CommandLineTest {
    @Test
    fun nopTest() {
        val info = BuilderConfiguration(XmlCalabashBuilder())
        Assertions.assertTrue(info.assertDefaults().isEmpty())
    }

    @Test
    fun emptyTest() {
        val builder = CommandLine.parse(arrayOf())
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "command" to "help"
        )).isEmpty())
    }

    // [--configuration:configuration-file]
    @Test
    fun configurationFileTest() {
        val builder = CommandLine.parse(arrayOf("--configuration:src/test/resources/configxsd.xml"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "configurationFile" to File("src/test/resources/configxsd.xml")
        )).isEmpty())
    }

    // [--pipe]
    @Test
    fun pipeTest() {
        val builder = CommandLine.parse(arrayOf("--pipe"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "pipedMode" to true
        )).isEmpty())
    }

    // [--input:[type@]port=uri…]
    @Test
    fun inputOneTest() {
        val builder = CommandLine.parse(arrayOf("--input:source=file:/path/to/doc.xml"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "inputs" to listOf(XmlCalabashInput("source", URI.create("file:/path/to/doc.xml"), MediaType.ANY))
        )).isEmpty())
    }

    // [--input:[type@]port=uri…]
    @Test
    fun inputTwoTest() {
        val builder = CommandLine.parse(arrayOf("--input:source=file:/path/to/doc.xml", "-i:source=file:/path/to/alt.xml"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "inputs" to listOf(XmlCalabashInput("source", URI.create("file:/path/to/doc.xml"), MediaType.ANY),
                XmlCalabashInput("source", URI.create("file:/path/to/alt.xml"), MediaType.ANY))
        )).isEmpty())
    }

    // [--input:[type@]port=uri…]
    @Test
    fun inputContentTypeTest() {
        val builder = CommandLine.parse(arrayOf("--input:application/html+xml@source=file:/path/to/doc.html"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "inputs" to listOf(XmlCalabashInput("source", URI.create("file:/path/to/doc.html"), MediaType.parse("application/html+xml")))
        )).isEmpty())
    }

    // [--output:port=filespec…]
    @Test
    fun outputOneTest() {
        val builder = CommandLine.parse(arrayOf("--output:result=/dev/null"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "outputs" to listOf(XmlCalabashOutput("result", "/dev/null"))
        )).isEmpty())
    }

    // [--output:port=filespec…]
    @Test
    fun outputTwoTest() {
        val builder = CommandLine.parse(arrayOf("--output:result=/dev/null", "-o:alt=file:/tmp/out%0d.xml"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "outputs" to listOf(XmlCalabashOutput("result", "/dev/null"),
                XmlCalabashOutput("alt", "file:/tmp/out%0d.xml"))
        )).isEmpty())
    }

    // [--namespace:prefix=uri…]
    @Test
    fun namespaceTest() {
        val builder = CommandLine.parse(arrayOf("--namespace:x=http://example.com"))
        val info = BuilderConfiguration(builder)

        val result = mutableMapOf<String, NamespaceUri>()
        result.putAll(BuilderConfiguration.defaultNamespaces)
        result["x"] = NamespaceUri.of("http://example.com")

        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "namespaces" to result
        )).isEmpty())
    }

    // [--namespace:prefix=uri…]
    @Test
    fun namespaceReplaceTest() {
        val builder = CommandLine.parse(arrayOf("--namespace:cx=http://example.com"))
        val info = BuilderConfiguration(builder)

        val result = mutableMapOf<String, NamespaceUri>()
        result.putAll(BuilderConfiguration.defaultNamespaces)
        result["cx"] = NamespaceUri.of("http://example.com")

        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "namespaces" to result
        )).isEmpty())
    }

    // [--namespace:prefix=uri…]
    @Test
    fun namespaceDupTest() {
        try {
            CommandLine.parse(arrayOf("--namespace:x=http://example.com", "--namespace:x=http://example.com"))
            fail()
        } catch (ex: XProcException) {
            Assertions.assertEquals(NsErr.xi(214), ex.error.code)
        }
    }

    // [--init:class-name…]
    @Test
    fun initTest() {
        val builder = CommandLine.parse(arrayOf("--init:class.one", "--init:class.two"))
        val info = BuilderConfiguration(builder)

        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "initializers" to listOf(Pair("class.one", false), Pair("class.two", false))
        )).isEmpty())
    }

    // [--graphs:graph-output-directory]
    @Test
    fun graphsTest() {
        val builder = CommandLine.parse(arrayOf("--graphs:/tmp/pipe"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "graphs" to File("/tmp/pipe")
        )).isEmpty())
    }

    // [--verbosity:verbosity]
    @Test
    fun verbosityTest() {
        val builder = CommandLine.parse(arrayOf("--verbosity:warn"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "verbosity" to Verbosity.WARN
        )).isEmpty())
    }

    // [--explain]
    @Test
    fun explainTest() {
        val builder = CommandLine.parse(arrayOf("--explain"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "explainErrors" to true
        )).isEmpty())
    }

    // [--visualizer:name]
    @Test
    fun visualizerTest() {
        val builder = CommandLine.parse(arrayOf("--visualizer:detail"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "visualizer" to mapOf("name" to "detail", "options" to null)
        )).isEmpty())
    }

    // [--trace:output-file]
    @Test
    fun traceTest() {
        val builder = CommandLine.parse(arrayOf("--trace:/tmp/out.xml"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "trace" to File("/tmp/out.xml")
        )).isEmpty())
    }

    // [--trace-documents:output-directory]
    @Test
    fun traceDocuments() {
        val builder = CommandLine.parse(arrayOf("run", "--trace-documents:/tmp/trace-output"))
        builder.build()
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "command" to "run",
            "traceDocuments" to File("/tmp/trace-output"),
            "trace" to File("/tmp/trace-output/trace.xml")
        )).isEmpty())
    }

    // [--assertions:level]
    @Test
    fun assertionsTest() {
        val builder = CommandLine.parse(arrayOf("--assertions:error"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "assertions" to AssertionsLevel.ERROR
        )).isEmpty())
    }

    // [--extension:name…]
    @Test
    fun extensionTest() {
        val builder = CommandLine.parse(arrayOf("--extension:eager-uri-resolution"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "extensions" to listOf(ExtensionName.EAGER_URI_RESOLUTION)
        )).isEmpty())
    }

    // [--stacktrace]
    @Test
    fun stacktraceTest() {
        val builder = CommandLine.parse(arrayOf("--stacktrace"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "stacktrace" to true
        )).isEmpty())
    }

    // [--licensed]
    @Test
    fun licensedTest() {
        val builder = CommandLine.parse(arrayOf("--licensed"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "licensed" to true
        )).isEmpty())
    }

    // [--line-numbering]
    @Test
    fun lineNumberingTest() {
        val builder = CommandLine.parse(arrayOf("--line-numbering"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "lineNumbering" to true
        )).isEmpty())
    }

    // [--debug]
    @Test
    fun debugTest() {
        val builder = CommandLine.parse(arrayOf("--debug"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "debug" to true,
            "verbosity" to Verbosity.DEBUG
        )).isEmpty())
    }

    // [--debugger]
    @Test
    fun debuggerTest() {
        val builder = CommandLine.parse(arrayOf("--debugger"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "debugger" to true
        )).isEmpty())
    }

    // [--nogo]
    @Test
    fun nogoTest() {
        val builder = CommandLine.parse(arrayOf("--nogo"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "go" to false
        )).isEmpty())
    }

    // [--catalog:catalog-file-uri…]
    @Test
    fun catalogsTest() {
        val builder = CommandLine.parse(arrayOf("--catalog:/tmp/catalog1.xml", "--catalog:/other/catalog2.xml"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "xmlCatalogs" to listOf(URI.create("file:/tmp/catalog1.xml"), URI.create("file:/other/catalog2.xml"))
        )).isEmpty())
    }

    // [--xml-schema:xml-schema-file-uri…]
    @Test
    fun xmlSchema() {
        val builder = CommandLine.parse(arrayOf("--xml-schema:/tmp/schema1.xsd", "--xml-schema:http://example.com/schema.xsd"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "xmlSchemas" to listOf(URI.create("file:/tmp/schema1.xsd"), URI.create("http://example.com/schema.xsd"))
        )).isEmpty())
    }

    // [--validation-mode:mode]
    @Test
    fun validationMode() {
        val builder = CommandLine.parse(arrayOf("--validation-mode:strict"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "validationMode" to ValidationMode.STRICT
        )).isEmpty())
    }

    // [--try-namespaces]
    @Test
    fun tryNamespacesTest() {
        val builder = CommandLine.parse(arrayOf("--try-namespaces"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "tryNamespaces" to true
        )).isEmpty())
    }

    // [--use-location-hints]
    @Test
    fun useLocationHintsTest() {
        val builder = CommandLine.parse(arrayOf("--use-location-hints"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "useLocationHints" to true
        )).isEmpty())
    }

    // [--help]
    @Test
    fun help() {
        val builder = CommandLine.parse(arrayOf("--help"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "command" to "help"
        )).isEmpty())
    }

    // [--step:step-name]
    @Test
    fun stepNameTest() {
        val builder = CommandLine.parse(arrayOf("--step:somename"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "step" to "somename"
        )).isEmpty())
    }

    // [pipeline.xpl]
    @Test
    fun pipelineTest() {
        val builder = CommandLine.parse(arrayOf("file:/tmp/pipe.xpl"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "command" to "run",
            "pipelineUri" to URI.create("file:/tmp/pipe.xpl")
        )).isEmpty())
    }

    // [option=value…]
    @Test
    fun optionTest() {
        val builder = CommandLine.parse(arrayOf("file:/tmp/pipe.xpl", "x=1", "y=?1+1"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "command" to "run",
            "pipelineUri" to URI.create("file:/tmp/pipe.xpl"),
            "options" to mapOf("x" to listOf("1"), "y" to listOf("?1+1"))
        )).isEmpty())
    }

    // [!serialparam=value…]
    @Test
    fun serialParam() {
        val builder = CommandLine.parse(arrayOf("!result::method=json", "!indent=true"))
        val info = BuilderConfiguration(builder)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "outputSerialization" to mapOf("result" to mapOf("method" to "json"), "*" to mapOf("indent" to "true")),
        )).isEmpty())
    }
}