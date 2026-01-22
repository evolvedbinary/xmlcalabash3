package com.xmlcalabash.spi

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.api.Receiver
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue

interface XQueryProcessor {
    fun setup(stepConfig: XProcStepConfiguration, receiver: Receiver, stepParams: RuntimeStepParameters, cacheQuery: Boolean, config: Map<QName, String>)
    fun run(sources: List<XProcDocument>, query: XProcDocument, parameters: Map<QName, XdmValue>, version: String)
    fun reset()
    fun teardown()
}