package com.xmlcalabash.resourcecache

import net.sf.saxon.s9api.XQueryExecutable

class CompiledXQueryResource(val exec: XQueryExecutable) : CompiledResource() {
    override val type = CompiledResourceType.XQUERY
}