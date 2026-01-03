package com.xmlcalabash.api

import com.xmlcalabash.config.ConfigurationLoader
import com.xmlcalabash.XmlCalabashBuilder
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.util.UriUtils
import net.sf.saxon.Configuration
import net.sf.saxon.lib.CatalogResourceResolver
import net.sf.saxon.lib.Initializer
import org.apache.logging.log4j.kotlin.logger
import org.xmlresolver.ResolverFeature
import java.net.URI
import kotlin.collections.iterator

class RegisterSaxonFunctions(): Initializer {
    override fun initialize(config: Configuration?) {
        if (config == null) {
            return
        }

        val libraryUri = System.getProperty("com.xmlcalabash.pipelines")
        if (libraryUri != null) {
            val invocation = XmlCalabashBuilder()

            if (config.resourceResolver is CatalogResourceResolver) {
                val resolver = config.resourceResolver as CatalogResourceResolver
                val catalogs = mutableListOf<URI>()
                catalogs.addAll(invocation.xmlCatalogs.getOrDefault() ?: emptyList())
                for (catalog in resolver.getFeature(ResolverFeature.CATALOG_FILES)) {
                    val uri = UriUtils.cwdAsUri().resolve(catalog)
                    catalogs.add(uri)
                    logger.debug { "Adding ${uri} to list of catalogs" }
                }
                for (catalog in resolver.getFeature(ResolverFeature.CATALOG_ADDITIONS)) {
                    val uri = UriUtils.cwdAsUri().resolve(catalog)
                    catalogs.add(uri)
                    logger.debug { "Adding ${uri} to list of catalogs" }
                }
                invocation.xmlCatalogs.set(catalogs)
            }

            val calabashConfig = System.getProperty("com.xmlcalabash.configuration")
            if (calabashConfig != null) {
                val loader = ConfigurationLoader()
                val configured = loader.load(UriUtils.cwdAsUri().resolve(calabashConfig))
                invocation.update(configured)
            }

            val xmlCalabash = invocation.build(config)
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