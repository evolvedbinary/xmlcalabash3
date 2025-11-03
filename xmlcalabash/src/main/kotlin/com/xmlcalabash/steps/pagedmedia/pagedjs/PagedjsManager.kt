package com.xmlcalabash.steps.pagedmedia.pagedjs

import com.xmlcalabash.api.CssProcessor
import com.xmlcalabash.api.FoProcessor
import com.xmlcalabash.spi.PagedMediaManager
import com.xmlcalabash.spi.PagedMediaProvider
import net.sf.saxon.s9api.QName
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.net.URI

class PagedjsManager: PagedMediaProvider, PagedMediaManager {
    companion object {
        private val genericCssFormatter = URI("https://xmlcalabash.com/paged-media/css-formatter")
        val pagedJsCssFormatter = URI("https://xmlcalabash.com/paged-media/css-formatter/pagedjs")
        private val pagedMediaProcessors = setOf(genericCssFormatter, pagedJsCssFormatter)
    }

    override fun formatters(): List<URI> {
        return listOf(pagedJsCssFormatter)
    }

    override fun create(): PagedMediaManager {
        logger.info { "Initializing Paged.js paged media manager" }
        return this
    }

    override fun formatterSupported(formatter: URI): Boolean {
        return pagedMediaProcessors.contains(formatter)
    }

    override fun configure(formatter: URI, properties: Map<QName, String>) {
        CssPagedjs.configure(formatter, properties)
    }

    override fun formatterAvailable(formatter: URI): Boolean {
        if (formatter !in pagedMediaProcessors) {
            return false
        }
        val exePath = CssPagedjs.defaultStringOptions[CssPagedjs._exePath]
        if (exePath == null) {
            return false
        }

        val exeName = File(exePath)
        return exeName.exists() && exeName.canExecute()
    }

    override fun getCssProcessor(formatter: URI): CssProcessor {
        when (formatter) {
            genericCssFormatter, pagedJsCssFormatter -> return CssPagedjs()
            else -> throw RuntimeException("paged-media-pagedjs does not provide ${formatter}")
        }
    }

    override fun getFoProcessor(formatter: URI): FoProcessor {
        throw RuntimeException("paged-media-pagedjs does not provide any XSL formatters")
    }
}