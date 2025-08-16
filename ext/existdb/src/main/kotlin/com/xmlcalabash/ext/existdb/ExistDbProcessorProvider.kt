package com.xmlcalabash.ext.existdb

import com.xmlcalabash.spi.XQueryProcessor
import com.xmlcalabash.spi.XQueryProcessorProvider
import net.sf.saxon.s9api.QName
import java.net.URI

class ExistDbProcessorProvider(): XQueryProcessorProvider {
    override val implementationUri: URI = URI.create("https://exist-db.org/")
    override fun getImplementation(): XQueryProcessor {
        return XQueryExistDbProcessor()
    }
}