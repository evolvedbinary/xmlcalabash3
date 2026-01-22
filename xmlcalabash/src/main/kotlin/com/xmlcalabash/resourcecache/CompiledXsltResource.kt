package com.xmlcalabash.resourcecache

import net.sf.saxon.s9api.XsltExecutable

class CompiledXsltResource(val exec: XsltExecutable) : CompiledResource() {
    override val type = CompiledResourceType.XSLT
}