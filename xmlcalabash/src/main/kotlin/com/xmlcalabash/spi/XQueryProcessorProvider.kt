package com.xmlcalabash.spi

import java.net.URI

interface XQueryProcessorProvider {
    val implementationUri: URI
    fun getImplementation(): XQueryProcessor
}