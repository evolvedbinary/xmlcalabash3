package com.xmlcalabash.util.spi

import com.xmlcalabash.spi.XQueryProcessor
import com.xmlcalabash.spi.XQueryProcessorProvider
import com.xmlcalabash.steps.XQuerySaxonProcessor
import net.sf.saxon.s9api.QName
import java.net.URI

class StandardXQueryProcessorProvider(): XQueryProcessorProvider {
    override val implementationUri = URI.create("https://saxonica.com/")
    override val configuration: Map<QName, String> = emptyMap()

    override fun configure(properties: Map<QName, String>) {
        // nop
    }

    override fun getImplementation(): XQueryProcessor {
       return XQuerySaxonProcessor()
    }
}