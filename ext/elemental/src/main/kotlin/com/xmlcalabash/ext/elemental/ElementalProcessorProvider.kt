package com.xmlcalabash.ext.elemental

import com.xmlcalabash.spi.XQueryProcessor
import com.xmlcalabash.spi.XQueryProcessorProvider
import java.net.URI

class ElementalProcessorProvider(): XQueryProcessorProvider {
    override val implementationUri: URI = URI.create("https://elemental.xyz/")
    override fun getImplementation(): XQueryProcessor {
        return XQueryElementalProcessor()
    }
}