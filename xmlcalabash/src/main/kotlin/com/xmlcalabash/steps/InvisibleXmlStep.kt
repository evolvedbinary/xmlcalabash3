package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import com.xmlcalabash.util.InvisibleXmlImpl
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.SaxonErrorReporter
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import java.net.URI

class InvisibleXmlStep(): AbstractAtomicStep() {
    private val extensionAttr = mutableMapOf<QName, String>()
    private lateinit var stepName: String
    private var cacheGrammar = false

    init {
        expectedExtensionAttributes.addAll(listOf(NsCx.processor, NsCx.cacheGrammar))
    }

    override fun setup(stepConfig: XProcStepConfiguration, receiver: com.xmlcalabash.runtime.api.Receiver, stepParams: RuntimeStepParameters) {
        super.setup(stepConfig, receiver, stepParams)
        stepName = stepParams.stepName
    }

    override fun extensionAttributes(attributes: Map<QName, String>, staticOptions: Map<QName, XdmValue>) {
        cacheGrammar = extensionAttributeBooleanValue(attributes, NsCx.cacheGrammar, staticOptions)
        extensionAttr.clear()
        extensionAttr.putAll(attributes)
        extensionAttr.remove(NsCx.cacheGrammar)
    }

    override fun run() {
        super.run()

        val source = queues["source"]!!.first()
        val grammar = queues["grammar"]!!
        val implementation = extensionAttr[NsCx.processor]
            ?: System.getProperty("com.xmlcalabash.invisible-xml")
            ?: "nineml"

        stepConfig.debug { "Invisible XML implementation: ${implementation}" }

        if (stepParams.stepType == NsP.ixml) {
            stepConfig.info { "The step type p:ixml is deprecated, use p:invisible-xml instead" }
        }

        val failOnError = booleanBinding(Ns.failOnError) != false
        val parameters = qnameMapBinding(Ns.parameters)
        var grammarURI: URI? = null

        val grammarText = if (grammar.isEmpty()) {
            null
        } else {
            if (grammar.size != 1) {
                throw stepConfig.exception(XProcError.xcAtMostOneGrammar())
            }
            val theGrammar = grammar.first()
            grammarURI = theGrammar.baseURI
            val grammarCtc = (theGrammar.contentType ?: MediaType.TEXT).classification()
            if (grammarCtc == MediaClassification.TEXT) {
                theGrammar.value.underlyingValue.stringValue
            } else {
                null
            }
        }

        val grammarXml = if (grammar.isEmpty() || grammarText != null) {
            null
        } else {
            val theGrammar = grammar.first()
            grammarURI = theGrammar.baseURI
            val grammarCtc = (theGrammar.contentType ?: MediaType.XML).classification()
            if (grammarCtc != MediaClassification.XML) {
                throw IllegalArgumentException("Grammar must be text or XML")
            }
            theGrammar.value as XdmNode
        }

        val input = source.value.underlyingValue.stringValue

        val impl = InvisibleXmlImpl(stepConfig, implementation, stepName, cacheGrammar)

        val xml = if (grammarXml != null) {
            impl.parse(grammarURI, grammarXml, input, failOnError, parameters)
        } else {
            impl.parse(grammarURI, grammarText, input, failOnError, parameters)
        }
        receiver.output("result", xml)
    }

    override fun toString(): String = "p:invisible-xml"
}