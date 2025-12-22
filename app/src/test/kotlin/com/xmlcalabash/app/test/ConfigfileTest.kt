package com.xmlcalabash.app.test

import com.xmlcalabash.app.CommandLine
import com.xmlcalabash.config.ConfigurationLoader
import com.xmlcalabash.config.XmlCalabashInput
import com.xmlcalabash.XmlCalabashBuilder
import com.xmlcalabash.config.XmlCalabashOutput
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.util.ExtensionName
import com.xmlcalabash.util.Verbosity
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.ValidationMode
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import org.apache.logging.log4j.kotlin.logger
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URI

class ConfigfileTest {
    @Test
    fun nopTest() {
        val info = BuilderConfiguration(XmlCalabashBuilder())
        Assertions.assertTrue(info.assertDefaults().isEmpty())
    }

    @Test
    fun emptyTest() {
        val config = ConfigurationLoader().load(File("src/test/resources/cfg/empty.xml"))
        val info = BuilderConfiguration(config)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
        )).isEmpty())
    }

    @Test
    fun rootTest() {
        val config = ConfigurationLoader().load(File("src/test/resources/cfg/root.xml"))
        val info = BuilderConfiguration(config)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "saxonConfigurationFile" to File("/path/to/saxonConfig.xml"),
            "licensed" to true,
            "verbosity" to Verbosity.WARN,
            "pipedMode" to true,
            "tryNamespaces" to true,
            "useLocationHints" to false,
            "validationMode" to ValidationMode.LAX,
            "defaultXQueryProcessor" to URI.create("http://example.com")
        )).isEmpty())
    }

    // <cc:catalog xmlns:cc="https://xmlcalabash.com/ns/configuration"
    //   href? = anyURI />
    // <cc:xml-schema xmlns:cc="https://xmlcalabash.com/ns/configuration"
    //   href? = anyURI>
    //     any-name*
    // </cc:xml-schema>
    @Test
    fun catalogsAndSchemasTest() {
        val config = ConfigurationLoader().load(File("src/test/resources/cfg/catschema.xml"))
        val info = BuilderConfiguration(config)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "xmlCatalogs" to listOf(URI.create("file:/tmp/catalog1.xml"), URI.create("file:/other/catalog2.xml")),
            "xmlSchemas" to listOf(URI.create("file:/tmp/schema1.xsd"), URI.create("http://example.com/schema.xsd"))
        )).isEmpty())
    }

    // <cc:extension xmlns:cc="https://xmlcalabash.com/ns/configuration"
    //   name = string />
    @Test
    fun extensionTest() {
        val config = ConfigurationLoader().load(File("src/test/resources/cfg/extension.xml"))
        val info = BuilderConfiguration(config)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "extensions" to listOf(ExtensionName.EAGER_URI_RESOLUTION)
        )).isEmpty())
    }

    // <cc:graphviz xmlns:cc="https://xmlcalabash.com/ns/configuration"
    //   dot = string
    //   style? = string
    //   output? = string/>
    @Test
    fun graphvizTest() {
        val dot = File("/opt/homebrew/bin/dot")
        if (dot.exists() && dot.canExecute()) {
            val config = ConfigurationLoader().load(File("src/test/resources/cfg/graphviz.xml"))
            val info = BuilderConfiguration(config)
            Assertions.assertTrue(info.assertConfiguration(mapOf(
                "graphviz" to dot,
                "graphStyle" to URI.create("file:/path/to/style.xsl"),
                "graphs" to File("/tmp/pipe")
            )).isEmpty())
        } else {
            logger.warn("Skipping graphvizTest, no dot")
        }
    }

    // <cc:inline xmlns:cc="https://xmlcalabash.com/ns/configuration"
    //   trim-whitespace = boolean />
    // <cc:system-property xmlns:cc="https://xmlcalabash.com/ns/configuration"
    //   name = string
    //   value = string />
    // <cc:visualizer xmlns:cc="https://xmlcalabash.com/ns/configuration"
    //   name = silent|plain|detail
    //   {any-name}* = string />
    // <cc:threading xmlns:cc="https://xmlcalabash.com/ns/configuration"
    //   count? = integer />
    @Test
    fun inlineTest() {
        val config = ConfigurationLoader().load(File("src/test/resources/cfg/inline.xml"))
        val info = BuilderConfiguration(config)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "inlineTrimWhitespace" to true,
            "visualizer" to mapOf("name" to "plain", "options" to mapOf("thing" to "value")),
            "maxThreadCount" to 7
        )).isEmpty())
        Assertions.assertEquals("4", System.getProperty("CATDEBUG"))
        Assertions.assertEquals("yyy", System.getProperty("xxx"))
    }

    // <cc:mimetype xmlns:cc="https://xmlcalabash.com/ns/configuration"
    //   content-type = string
    //   extensions = string />
    // <cc:proxy xmlns:cc="https://xmlcalabash.com/ns/configuration"
    //   scheme = string
    //   uri = anyURI />
    // <cc:send-mail xmlns:cc="https://xmlcalabash.com/ns/configuration"
    //   {any-name}* = string />
    @Test
    fun mimetypeTest() {
        val config = ConfigurationLoader().load(File("src/test/resources/cfg/mimetype.xml"))
        val info = BuilderConfiguration(config)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "mimeTypes" to mapOf("application/foobar" to listOf("foo", "bar"), "application/barfoo" to listOf("barfoo")),
            "proxies" to mapOf("http" to "http://localhost:8888/", "https" to "http://localhost:7777/"),
            "sendmail" to mapOf("username" to "fred", "password" to "sekrit", "port" to "999")
        )).isEmpty())
    }

    // <cc:saxon-configuration-property xmlns:cc="https://xmlcalabash.com/ns/configuration"
    //   name = string
    //   value = string />
    // <cc:serialization xmlns:cc="https://xmlcalabash.com/ns/configuration"
    //   content-type = string
    //   {any-name}* = string />
    // <cc:message-reporter xmlns:cc="https://xmlcalabash.com/ns/configuration"
    //   buffer-size? = integer />
    // <cc:xquery-processor xmlns:cc="https://xmlcalabash.com/ns/configuration"
    //   name = anyURI
    //   {any-name}* = string />
    @Test
    fun saxonconfigTest() {
        val config = ConfigurationLoader().load(File("src/test/resources/cfg/saxonconfig.xml"))
        val info = BuilderConfiguration(config)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "saxonConfigurationProperties" to mapOf("http://saxon.sf.net/feature/expandAttributeDefaults" to "true"),
            "serialization" to mapOf(
                MediaType.parse("application/xml") to mapOf(QName("method") to "xml", QName("indent") to "true"),
                MediaType.parse("application/json") to mapOf(QName("method") to "json", QName("indent") to "false")),
            "messageReporterBufferSize" to 99,
            "configuredXQueryProcessors" to mapOf(URI.create("http://example.com/") to mapOf(QName("host") to "http://localhost:8234"))
        )).isEmpty())
    }


    // <cc:paged-media xmlns:cc="https://xmlcalabash.com/ns/configuration"
    //   css-formatter? = string
    //   xsl-formatter? = string
    //   {any-name}* = string />
    @Test
    fun pagedMediaTest() {
        val config = ConfigurationLoader().load(File("src/test/resources/cfg/pagedmedia.xml"))
        val info = BuilderConfiguration(config)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "cssFormatter" to mapOf(URI.create("https://xmlcalabash.com/paged-media/css-formatter/weasyprint") to mapOf(QName("exePath") to "/opt/homebrew/bin/weasyprint"),
                URI.create("https://xmlcalabash.com/paged-media/css-formatter") to emptyMap(),
                URI.create("https://xmlcalabash.com/paged-media/css-formatter/antenna-house") to emptyMap()),
            "xslFormatter" to mapOf(URI.create("https://xmlcalabash.com/paged-media/xsl-formatter/fop") to emptyMap())
        )).isEmpty())
    }

    // <x:other>...</x:other>
    @Test
    fun otherTest() {
        val config = ConfigurationLoader().load(File("src/test/resources/cfg/other.xml"))
        val info = BuilderConfiguration(config)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "other" to mapOf(QName(NamespaceUri.of("https://xmlcalabash.com/ext/ns/selenium"), "x:selenium")
                to listOf(mapOf(QName("whitelist") to "http://localhost.* https://testdata.xmlcalabash.com/.*")))
        )).isEmpty())
    }

    // <cc:namespace ...>
    @Test
    fun namespaceTest() {
        val config = ConfigurationLoader().load(File("src/test/resources/cfg/namespace.xml"))
        val info = BuilderConfiguration(config)

        val result = mutableMapOf<String, NamespaceUri>()
        result.putAll(BuilderConfiguration.defaultNamespaces)
        result["x"] = NamespaceUri.of("http://example.com/uri")
        result["cx"] = NamespaceUri.of("http://example.com/cx")

        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "namespaces" to result
        )).isEmpty())
    }

    // <cc:initializer ...>
    @Test
    fun initializerTest() {
        val config = ConfigurationLoader().load(File("src/test/resources/cfg/initializer.xml"))
        val info = BuilderConfiguration(config)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "initializers" to listOf(Pair("one", true), Pair("two", false), Pair("three", true))
        )).isEmpty())
    }

    // <cc:pipeline ...>
    @Test
    fun pipelineTest() {
        val config = ConfigurationLoader().load(File("src/test/resources/cfg/pipeline.xml"))
        val info = BuilderConfiguration(config)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "command" to "run",
            "pipelineUri" to URI.create("file:/opt/pipelines/pipe.xpl")
        )).isEmpty())
    }

    @Test
    fun pipelineInputsTest() {
        val config = ConfigurationLoader().load(File("src/test/resources/cfg/pipeinputs.xml"))
        val info = BuilderConfiguration(config)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "command" to "run",
            "pipelineUri" to URI.create("file:/opt/pipelines/pipe.xpl"),
            "inputs" to mapOf("source" to listOf(XmlCalabashInput(URI.create("http://example.com/"), MediaType.ANY)))
        )).isEmpty())
    }

    @Test
    fun pipelineInputsCLIWinsTest() {
        val cliConfig = CommandLine.parse(arrayOf("-i:source=/tmp/input.xml"))
        val cfgConfig = ConfigurationLoader().load(File("src/test/resources/cfg/pipeinputs.xml"))

        val config = cliConfig
        config.update(cfgConfig)
        val info = BuilderConfiguration(config)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "command" to "run",
            "pipelineUri" to URI.create("file:/opt/pipelines/pipe.xpl"),
            "inputs" to mapOf("source" to listOf(XmlCalabashInput(URI.create("file:/tmp/input.xml"), MediaType.ANY)))
        )).isEmpty())
    }

    @Test
    fun pipelineInlineInputsTest() {
        val config = ConfigurationLoader().load(File("src/test/resources/cfg/pipeinlineinput.xml"))
        val info = BuilderConfiguration(config)

        // This is a hack...
        val value = config.inputs.get("source")?.first()?.doc
        val input = XmlCalabashInput(null, MediaType.XHTML)
        input.doc = value

        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "command" to "run",
            "pipelineUri" to URI.create("file:/opt/pipelines/pipe.xpl"),
            "inputs" to mapOf("source" to listOf(input))
        )).isEmpty())
    }

    @Test
    fun pipelineInlineInputs64Test() {
        val config = ConfigurationLoader().load(File("src/test/resources/cfg/pipeinline64input.xml"))
        val info = BuilderConfiguration(config)

        // This is a hack...
        val value = config.inputs.get("source")?.first()?.doc
        val input = XmlCalabashInput(null, MediaType.JSON)
        input.doc = value

        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "command" to "run",
            "pipelineUri" to URI.create("file:/opt/pipelines/pipe.xpl"),
            "inputs" to mapOf("source" to listOf(input))
        )).isEmpty())
    }

    @Test
    fun pipelineOutputTest() {
        val config = ConfigurationLoader().load(File("src/test/resources/cfg/pipeoutput.xml"))
        val info = BuilderConfiguration(config)
        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "command" to "run",
            "pipelineUri" to URI.create("file:/opt/pipelines/pipe.xpl"),
            "outputs" to mapOf("result" to XmlCalabashOutput("out%02d.xml"))
        )).isEmpty())
    }

    @Test
    fun pipelineSelectOptionTest() {
        val config = ConfigurationLoader().load(File("src/test/resources/cfg/pipeselectoption.xml"))
        val info = BuilderConfiguration(config)

        val result = mutableMapOf<String, NamespaceUri>()
        result.putAll(BuilderConfiguration.defaultNamespaces)
        result["ex"] = NamespaceUri.of("http://example.com/ns")

        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "command" to "run",
            "pipelineUri" to URI.create("file:/opt/pipelines/pipe.xpl"),
            "namespaces" to result,
            "options" to mapOf("ex:opt" to listOf("?3+4"))
        )).isEmpty())
    }

    @Test
    fun pipelineValueOptionTest() {
        val config = ConfigurationLoader().load(File("src/test/resources/cfg/pipevalueoption.xml"))
        val info = BuilderConfiguration(config)

        val result = mutableMapOf<String, NamespaceUri>()
        result.putAll(BuilderConfiguration.defaultNamespaces)
        result["ex"] = NamespaceUri.of("http://example.com/ns")

        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "command" to "run",
            "pipelineUri" to URI.create("file:/opt/pipelines/pipe.xpl"),
            "namespaces" to result,
            "options" to mapOf("ex:opt" to listOf("7"))
        )).isEmpty())
    }

    @Test
    fun pipelineInlineOptionTest() {
        val config = ConfigurationLoader().load(File("src/test/resources/cfg/pipeinlineoption.xml"))
        val info = BuilderConfiguration(config)

        val result = mutableMapOf<String, NamespaceUri>()
        result.putAll(BuilderConfiguration.defaultNamespaces)
        result["ex"] = NamespaceUri.of("http://example.com/ns")

        val optValue = config.options.get("ex:opt")!!.first()
        Assertions.assertInstanceOf(XdmNode::class.java, optValue)

        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "command" to "run",
            "pipelineUri" to URI.create("file:/opt/pipelines/pipe.xpl"),
            "namespaces" to result,
            "options" to mapOf("ex:opt" to listOf(optValue))
        )).isEmpty())
    }

    @Test
    fun pipelineInlineJsonOptionTest() {
        val config = ConfigurationLoader().load(File("src/test/resources/cfg/pipeinlinejsonoption.xml"))
        val info = BuilderConfiguration(config)

        val result = mutableMapOf<String, NamespaceUri>()
        result.putAll(BuilderConfiguration.defaultNamespaces)
        result["ex"] = NamespaceUri.of("http://example.com/ns")

        val optValue = config.options.get("ex:opt")!!.first()
        Assertions.assertInstanceOf(XdmMap::class.java, optValue)

        Assertions.assertTrue(info.assertConfiguration(mapOf(
            "command" to "run",
            "pipelineUri" to URI.create("file:/opt/pipelines/pipe.xpl"),
            "namespaces" to result,
            "options" to mapOf("ex:opt" to listOf(optValue))
        )).isEmpty())
    }

}