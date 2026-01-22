package com.xmlcalabash.resourcecache

import net.sf.saxon.s9api.XPathExecutable

class CompiledXPathResource(val exec: XPathExecutable) : CompiledResource() {
    override val type = CompiledResourceType.XPATH
}