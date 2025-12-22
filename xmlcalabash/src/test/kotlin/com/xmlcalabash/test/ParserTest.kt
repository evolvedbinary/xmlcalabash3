package com.xmlcalabash.test

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.util.BufferingReceiver
import com.xmlcalabash.util.UriUtils
import com.xmlcalabash.parsers.xpl.XplParser
import net.sf.saxon.Configuration
import org.apache.logging.log4j.kotlin.logger
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import java.io.StringReader
import javax.xml.transform.stream.StreamSource

class ParserTest {
    @Test
    fun parseLibrary1() {
        val xmlCalabash = XmlCalabash.newInstance();
        val builder = xmlCalabash.newPipelineBuilder()
        val parser = XplParser(builder)
        val pipeline = parser.parse(UriUtils.cwdAsUri().resolve("src/test/resources/parser/library1.xpl"))
        Assertions.assertNotNull(pipeline)
    }

    @Test
    fun parseLibrarySource() {
        val xmlCalabash = XmlCalabash.newInstance();
        val builder = xmlCalabash.newPipelineBuilder()
        val parser = XplParser(builder)

        val uri = UriUtils.cwdAsUri().resolve("src/test/resources/parser/library1.xpl")
        val source = StreamSource(uri.toURL().openStream(), uri.toString())
        val pipeline = parser.parse(source)

        Assertions.assertNotNull(pipeline)
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "SKIP_EE_TESTS", matches = "true")
    fun importFunctions1() {
        val xmlCalabash = XmlCalabash.newInstance()
        if (xmlCalabash.saxonConfiguration.configuration.isLicensedFeature(Configuration.LicenseFeature.PROFESSIONAL_EDITION)) {
            val builder = xmlCalabash.newPipelineBuilder()
            val parser = XplParser(builder)
            val decl = parser.parse(UriUtils.cwdAsUri().resolve("src/test/resources/parser/importfunctions.xpl"))
            val pipeline = decl.getExecutable()
            val receiver = BufferingReceiver()
            pipeline.receiver = receiver
            pipeline.run()
            Assertions.assertNotNull(receiver.outputs["result"])
        } else {
            logger.warn { "Skipping import functions test; apparently running HE" }
        }
    }
}