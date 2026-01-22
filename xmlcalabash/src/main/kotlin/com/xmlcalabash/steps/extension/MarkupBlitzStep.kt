package com.xmlcalabash.steps.extension

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.InvisibleXmlMarkupBlitz
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue

class MarkupBlitzStep(): AbstractAtomicStep() {
    private lateinit var stepName: String
    private var cacheGrammar = false

    init {
        expectedExtensionAttributes.addAll(listOf(NsCx.cacheQuery))
    }

    override fun setup(stepConfig: XProcStepConfiguration, receiver: com.xmlcalabash.runtime.api.Receiver, stepParams: RuntimeStepParameters) {
        super.setup(stepConfig, receiver, stepParams)
        stepName = stepParams.stepName
    }

    override fun extensionAttributes(attributes: Map<QName, String>, staticOptions: Map<QName, XdmValue>) {
        super.extensionAttributes(attributes, staticOptions)
        cacheGrammar = extensionAttributeBooleanValue(attributes, NsCx.cacheGrammar, staticOptions)
    }

    override fun run() {
        super.run()

        val sourceDoc = queues["source"]!!.first()
        val grammarDoc = queues["grammar"]!!.firstOrNull()
        val failOnError = booleanBinding(Ns.failOnError) != false
        val parameters = qnameMapBinding(Ns.parameters)

        val impl = InvisibleXmlMarkupBlitz(stepConfig, stepName, cacheGrammar)

        val source = (sourceDoc.value as XdmNode).underlyingValue.stringValue
        val grammar = (grammarDoc?.value as XdmNode?)?.underlyingValue?.stringValue

        val result = impl.parse(grammarDoc?.baseURI, grammar, source, failOnError, parameters)
        receiver.output("result", result)
    }

    override fun toString(): String = "cx:markup-blitz"
}