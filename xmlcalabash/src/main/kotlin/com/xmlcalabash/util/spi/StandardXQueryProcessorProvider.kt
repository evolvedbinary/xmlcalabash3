package com.xmlcalabash.util.spi

import com.xmlcalabash.spi.XQueryProcessor
import com.xmlcalabash.spi.XQueryProcessorProvider
import com.xmlcalabash.steps.XQuerySaxonProcessor
import java.net.URI

class StandardXQueryProcessorProvider(): XQueryProcessorProvider {
    override val implementationUri = URI.create("https://saxonica.com/")
    override fun getImplementation(): XQueryProcessor {
       return XQuerySaxonProcessor()
    }
}