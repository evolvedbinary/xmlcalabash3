package com.xmlcalabash.test

import com.xmlcalabash.XmlCalabashBuilder
import com.xmlcalabash.config.ConfigurationLoader
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.util.ExtensionName
import com.xmlcalabash.util.Verbosity
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.ValidationMode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URI

class ConfigurationLoaderTest {
    @Test
    fun testConfigurationLoader1() {
        val builder = XmlCalabashBuilder()
        val loader = ConfigurationLoader()
        builder.update(loader.load(File("src/test/resources/config1.xml")))
        Assertions.assertEquals(URI.create("http://example.com/default"), builder.defaultXQueryProcessor.getOrDefault())
        Assertions.assertTrue(builder.licensed.getOrDefault() ?: false)
        Assertions.assertTrue(builder.lineNumbering.getOrDefault() ?: false)
        Assertions.assertTrue(builder.pipedMode.getOrDefault() ?: false)
        Assertions.assertEquals(File("/path/to/saxon.xml"), builder.saxonConfigurationFile.getOrDefault())
        Assertions.assertTrue(builder.stacktrace.getOrDefault() ?: false)
        Assertions.assertTrue(builder.tryNamespaces.getOrDefault() ?: false)
        Assertions.assertTrue(builder.useLocationHints.getOrDefault() ?: false)
        Assertions.assertEquals(ValidationMode.STRICT,builder.validationMode.getOrDefault())
        Assertions.assertEquals(Verbosity.DEBUG,builder.verbosity.getOrDefault())
    }

    @Test
    fun testConfigurationLoader2() {
        val builder = XmlCalabashBuilder()
        val loader = ConfigurationLoader()
        builder.update(loader.load(File("src/test/resources/config2.xml")))
        Assertions.assertFalse(builder.licensed.getOrDefault() ?: true)
        Assertions.assertFalse(builder.lineNumbering.getOrDefault() ?: true)
        Assertions.assertFalse(builder.pipedMode.getOrDefault() ?: true)
        Assertions.assertFalse(builder.stacktrace.getOrDefault() ?: true)
        Assertions.assertFalse(builder.tryNamespaces.getOrDefault() ?: true)
        Assertions.assertFalse(builder.useLocationHints.getOrDefault() ?: true)
        Assertions.assertEquals(ValidationMode.LAX,builder.validationMode.getOrDefault())
        Assertions.assertEquals(Verbosity.WARN,builder.verbosity.getOrDefault())
    }

    @Test
    fun testConfigurationLoader3() {
        val builder = XmlCalabashBuilder()
        val loader = ConfigurationLoader()
        try {
            builder.update(loader.load(File("src/test/resources/config3.xml")))
            fail()
        } catch (ex: XProcException) {
            Assertions.assertEquals(NsErr.xi(26), ex.error.code)
        }
    }

    @Test
    fun testConfigurationLoader4() {
        val builder = XmlCalabashBuilder()
        val loader = ConfigurationLoader()
        builder.update(loader.load(File("src/test/resources/config4.xml")))
        Assertions.assertEquals(1, builder.xmlCatalogs.getOrDefault()?.size)
        Assertions.assertEquals(URI.create("file:/path/to/catalog.xml"), builder.xmlCatalogs.getOrDefault()?.get(0))

        Assertions.assertEquals(1, builder.extensions.getOrDefault()?.size)
        Assertions.assertEquals(ExtensionName.EAGER_URI_RESOLUTION, builder.extensions.getOrDefault()?.get(0))

        Assertions.assertEquals(1, builder.initializers.getOrDefault()?.size)
        Assertions.assertEquals("a.b.c", builder.initializers.getOrDefault()?.get(0)?.first)
        Assertions.assertTrue(builder.initializers.getOrDefault()?.get(0)?.second ?: false)

        Assertions.assertTrue(builder.inlineTrimWhitespace.getOrDefault() ?: false)

        Assertions.assertEquals(1, builder.mimeTypes.getOrDefault()?.size)
        Assertions.assertEquals(listOf("spoon", "spork"), builder.mimeTypes.getOrDefault()?.get("application/spoon"))

        Assertions.assertEquals(NamespaceUri.of("http://example.org/ns/xyz"), builder.namespaces.getOrDefault()?.get("xyz"))

        Assertions.assertEquals(1, builder.cssFormatter.getOrDefault()?.size)
        val weasy = URI.create("https://xmlcalabash.com/paged-media/css-formatter/weasyprint")
        val css = builder.cssFormatter.getOrDefault()!!
        // FIXME:
        Assertions.assertEquals(weasy, css.first().first)
        Assertions.assertEquals(1, css.first().second.size)
        Assertions.assertEquals("/opt/homebrew/bin/weasyprint", css.first().second.get(QName("exePath")))

        Assertions.assertEquals(URI.create("file:/path/to/library.xpl"), builder.pipelineUri.getOrDefault())

        Assertions.assertEquals(1, builder.inputs.getOrDefault()?.size)
        Assertions.assertEquals(1, builder.outputs.getOrDefault()?.size)
        Assertions.assertEquals(1, builder.options.getOrDefault()?.size)

        Assertions.assertEquals(1, builder.proxies.getOrDefault()?.size)
        Assertions.assertEquals("http://localhost:8123", builder.proxies.getOrDefault()?.get("http"))

        Assertions.assertEquals("true", builder.saxonConfigurationProperties.getOrDefault()?.get("http://saxon.sf.net/feature/expandAttributeDefaults"))

        Assertions.assertEquals("user", builder.sendmail.getOrDefault()?.get("username"))
        Assertions.assertEquals("sekrit", builder.sendmail.getOrDefault()?.get("password"))

        Assertions.assertEquals("true",
            builder.serialization.getOrDefault()?.get(MediaType.XML)?.get(Ns.indent))

        Assertions.assertEquals("pvalue", System.getProperty("pname"))

        Assertions.assertEquals(17, builder.messageReporterBufferSize.getOrDefault())

        Assertions.assertEquals("plain", builder.visualizerName.getOrDefault())
        Assertions.assertEquals("y", builder.visualizerName.options?.get("x"))

        Assertions.assertEquals(4, builder.maxThreadCount.getOrDefault())

        Assertions.assertEquals(1, builder.xmlSchemas.getOrDefault()?.size)
        Assertions.assertEquals(URI.create("file:/path/to/schema.xsd"), builder.xmlSchemas.getOrDefault()?.get(0))

        val proc = builder.configuredXQueryProcessors.getOrDefault()?.get(URI.create("https://basex.org/"))
        Assertions.assertNotNull(proc)
        Assertions.assertEquals("user", proc?.get(QName("username")))
        Assertions.assertEquals("sekrit", proc?.get(QName("password")))

        val qname = QName(NamespaceUri.of("https://xmlcalabash.com/ext/ns/selenium"), "x:selenium")
        val selenium = builder.other.getOrDefault()?.get(qname)
        Assertions.assertNotNull(selenium)
        Assertions.assertTrue("testdata.x" in selenium?.get(0)?.get(QName("whitelist"))!!)
    }
}