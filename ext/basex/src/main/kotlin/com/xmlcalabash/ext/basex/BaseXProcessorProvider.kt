package com.xmlcalabash.ext.basex

import com.xmlcalabash.spi.XQueryProcessor
import com.xmlcalabash.spi.XQueryProcessorProvider
import net.sf.saxon.s9api.QName
import java.net.URI

class BaseXProcessorProvider(): XQueryProcessorProvider {
    private val properties = mutableMapOf<QName, String>()

    override val configuration: Map<QName, String>
        get() = properties

    override val implementationUri: URI = URI.create("https://basex.org/")

    override fun configure(properties: Map<QName, String>) {
        this.properties.putAll(properties)
    }

    override fun getImplementation(): XQueryProcessor {
        return XQueryBaseXProcessor()
    }
}