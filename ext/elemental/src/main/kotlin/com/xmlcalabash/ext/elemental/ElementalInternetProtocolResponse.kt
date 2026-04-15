package com.xmlcalabash.ext.elemental

import com.xmlcalabash.io.AbstractInternetProtocolResponse
import java.net.URI

class ElementalInternetProtocolResponse(override val responseUri: URI, override val statusCode: Int) : AbstractInternetProtocolResponse<ElementalQueryTypedResponseDocument>(responseUri, statusCode)