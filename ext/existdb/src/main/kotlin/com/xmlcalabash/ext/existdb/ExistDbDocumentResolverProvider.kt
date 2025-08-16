package com.xmlcalabash.ext.existdb

import com.xmlcalabash.util.SimpleDocumentResolverProvider
import java.net.URI

class ExistDbDocumentResolverProvider: SimpleDocumentResolverProvider(
    URI("https://xmlcalabash.com/ext/library/exist-db.xpl"),
    "/com/xmlcalabash/ext/exist-db.xpl")