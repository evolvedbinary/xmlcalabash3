package com.xmlcalabash.resourcecache

import de.bottlecaps.markup.Blitz
import de.bottlecaps.markup.blitz.Parser

class CompiledMarkupBlitzResource(val parser: Parser, val options: List<Blitz.Option>) : CompiledResource() {
    override val type = CompiledResourceType.IXMLMB
}