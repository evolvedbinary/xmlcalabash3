package com.xmlcalabash.spi

import net.sf.saxon.s9api.QName
import java.net.URI

interface XQueryProcessorProvider {
    val implementationUri: URI
    val configuration: Map<QName, String>
    fun configure(properties: Map<QName, String>)
    fun getImplementation(): XQueryProcessor
}