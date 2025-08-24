package com.xmlcalabash.ext.elemental

import com.xmlcalabash.util.SimpleDocumentResolverProvider
import java.net.URI

class ElementalDocumentResolverProvider: SimpleDocumentResolverProvider(
    URI("https://xmlcalabash.com/ext/library/elemental.xpl"),
    "/com/xmlcalabash/ext/elemental.xpl")