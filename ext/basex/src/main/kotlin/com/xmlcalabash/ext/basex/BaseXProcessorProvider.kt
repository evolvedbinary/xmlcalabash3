package com.xmlcalabash.ext.basex

import com.xmlcalabash.spi.XQueryProcessor
import com.xmlcalabash.spi.XQueryProcessorProvider
import net.sf.saxon.s9api.QName
import java.net.URI

class BaseXProcessorProvider(): XQueryProcessorProvider {
    override val implementationUri: URI = URI.create("https://basex.org/")
    override fun getImplementation(): XQueryProcessor {
        return XQueryBaseXProcessor()
    }
}