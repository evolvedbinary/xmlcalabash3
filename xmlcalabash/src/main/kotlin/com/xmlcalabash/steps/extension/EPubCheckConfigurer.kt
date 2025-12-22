package com.xmlcalabash.steps.extension

import com.xmlcalabash.XmlCalabashBuilder
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.spi.Configurer
import com.xmlcalabash.spi.ConfigurerProvider
import net.sf.saxon.Configuration

class EPubCheckConfigurer(): Configurer, ConfigurerProvider {
    override fun configure(builder: XmlCalabashBuilder) {
        builder.addMimeTypeMapping(MediaType.parse("application/epub+zip"), setOf("epub"))
    }

    override fun configureSaxon(config: Configuration) {
        // nop
    }

    override fun create(): Configurer {
        return this
    }
}