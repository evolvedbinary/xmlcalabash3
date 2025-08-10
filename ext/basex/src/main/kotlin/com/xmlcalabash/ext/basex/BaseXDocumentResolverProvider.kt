package com.xmlcalabash.ext.basex

import com.xmlcalabash.util.SimpleDocumentResolverProvider
import java.net.URI

class BaseXDocumentResolverProvider: SimpleDocumentResolverProvider(
    URI("https://xmlcalabash.com/ext/library/basex.xpl"),
    "/com/xmlcalabash/ext/basex.xpl")