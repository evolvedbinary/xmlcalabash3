package com.xmlcalabash.steps.extension

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.resourcecache.CompiledResourceType
import com.xmlcalabash.resourcecache.CompiledXPathResource
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SaxonErrorReporter
import com.xmlcalabash.util.XProcCollectionFinder
import net.sf.saxon.s9api.*

class XPathStep(): AbstractAtomicStep() {
    private lateinit var stepName: String
    var cachedExpression = true

    init {
        expectedExtensionAttributes.addAll(listOf(NsCx.cacheExpression))
    }

    override fun setup(stepConfig: XProcStepConfiguration, receiver: com.xmlcalabash.runtime.api.Receiver, stepParams: RuntimeStepParameters) {
        super.setup(stepConfig, receiver, stepParams)
        stepName = stepParams.stepName
    }

    override fun extensionAttributes(attributes: Map<QName, String>, staticOptions: Map<QName, XdmValue>) {
        super.extensionAttributes(attributes, staticOptions)
        cachedExpression = extensionAttributeBooleanValue(attributes, NsCx.cacheExpression, staticOptions)
    }

    override fun run() {
        super.run()

        val xpath = queues["xpath"]!!.first()
        val sources = mutableListOf<XProcDocument>()
        sources.addAll(queues["source"] ?: emptyList())
        val document = if (sources.size == 1) {
            sources.removeFirst()
        } else {
            null
        }

        val params = qnameMapBinding(Ns.parameters)
        val version = stringBinding(Ns.version)

        if (version != null && version != "3.1") {
            throw stepConfig.exception(XProcError.xiXPathVersionNotSupported(version))
        }

        val underlyingConfig = stepConfig.processor.underlyingConfiguration
        synchronized(underlyingConfig) {
            val collectionFinder = underlyingConfig.collectionFinder
            val defaultCollection = underlyingConfig.defaultCollection

            try {
                underlyingConfig.defaultCollection = XProcCollectionFinder.DEFAULT
                underlyingConfig.collectionFinder = XProcCollectionFinder(sources, collectionFinder)

                val exec = getCompiledExpression(xpath)
                lateinit var selector: XPathSelector
                synchronized(exec) {
                    selector = exec.load()
                }
                for ((name, value) in params) {
                    selector.setVariable(name, value)
                }

                if (document != null) {
                    selector.contextItem = document.value as XdmItem
                }

                try {
                    for (document in S9Api.makeDocuments(stepConfig, selector.evaluate())) {
                        receiver.output("result", document)
                    }
                } finally {
                    underlyingConfig.collectionFinder = collectionFinder
                }


            } finally {
                underlyingConfig.collectionFinder = collectionFinder
                underlyingConfig.defaultCollection = defaultCollection
            }
        }
    }

    private fun getCompiledExpression(xpath: XProcDocument): XPathExecutable {
        if (cachedExpression) {
            if (xpath.baseURI != null && stepConfig.compiledResourceCache.contains(stepName, xpath.baseURI!!)) {
                val rsrc = stepConfig.compiledResourceCache.get(stepName, xpath.baseURI!!)!!
                if (rsrc.type == CompiledResourceType.XPATH) {
                    return (rsrc as CompiledXPathResource).exec
                }
            }
        }

        val params = qnameMapBinding(Ns.parameters)
        val compiler = stepConfig.newXPathCompiler()
        compiler.setWarningHandler(SaxonErrorReporter(stepConfig))
        for ((name, _) in params) {
            compiler.declareVariable(name)
        }

        val exec = compiler.compile(xpath.value.underlyingValue.stringValue)
        if (cachedExpression && xpath.baseURI != null) {
            stepConfig.compiledResourceCache.put(stepName, xpath.baseURI!!, CompiledXPathResource(exec))
        }

        return exec
    }
}