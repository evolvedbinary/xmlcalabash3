package com.xmlcalabash.steps.validation

import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.steps.AbstractAtomicStep
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue

abstract class AbstractValidationStep: AbstractAtomicStep() {
    companion object {
        fun xvrlParameters(parameters: Map<QName, XdmValue>): Map<String,String> {
            val params = mutableMapOf<String,String>()

            for ((key, value) in parameters) {
                if (key.namespaceUri == NsXvrl.namespace) {
                    params[key.localName] = value.underlyingValue.stringValue
                }
            }

            return params
        }
    }
}