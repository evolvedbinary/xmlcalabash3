package com.xmlcalabash.api

import com.xmlcalabash.XmlCalabashBuilder
import com.xmlcalabash.config.ConfigurationLoader
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.util.UriUtils
import net.sf.saxon.Configuration
import net.sf.saxon.lib.Initializer
import org.apache.logging.log4j.kotlin.logger
import kotlin.collections.iterator

class RegisterSaxonFunctions(): Initializer {
    override fun initialize(config: Configuration?) {
        if (config == null) {
            return
        }

        val libraryUri = System.getProperty("com.xmlcalabash.pipelines")
        if (libraryUri != null) {
            val builder = XmlCalabashBuilder()

            val calabashConfig = System.getProperty("com.xmlcalabash.configuration")
            if (calabashConfig != null) {
                val loader = ConfigurationLoader(builder)
                loader.load(UriUtils.cwdAsUri().resolve(calabashConfig))
            }

            val xmlCalabash = builder.build(config)
            val xplParser = xmlCalabash.newXProcParser()
            try {
                val library = xplParser.parseLibrary(libraryUri)
                library.validate()
                for ((_, decl) in library.exportedSteps) {
                    xmlCalabash.saxonConfiguration.declareFunction(decl)
                }
            } catch (ex: XProcException) {
                if (ex.error.code == NsErr.xi(216)) {
                    val decl = xplParser.parse(libraryUri)
                    decl.validate()
                    xmlCalabash.saxonConfiguration.declareFunction(decl)
                }
            }
        } else {
            logger.warn { "Cannot register pipeline functions, no library provided in com.xmlcalabash.pipelines" }
        }
    }
}