package com.xmlcalabash.resourcecache

import org.nineml.coffeefilter.InvisibleXmlParser

class CompiledNineMLResource(val parser: InvisibleXmlParser) : CompiledResource() {
    override val type = CompiledResourceType.IXML9ML
}