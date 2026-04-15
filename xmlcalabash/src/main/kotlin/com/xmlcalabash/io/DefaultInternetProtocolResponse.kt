package com.xmlcalabash.io

import com.xmlcalabash.documents.XProcDocument
import java.net.URI

/**
 * The response from an {@link DefaultInternetProtocolRequest}
 */
class DefaultInternetProtocolResponse(override val responseUri: URI, override val statusCode: Int) : AbstractInternetProtocolResponse<XProcDocument>(responseUri, statusCode)