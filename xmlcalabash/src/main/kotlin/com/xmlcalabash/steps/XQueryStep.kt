package com.xmlcalabash.steps

import com.xmlcalabash.config.ConfigurationLoader
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.spi.XQueryProcessor
import com.xmlcalabash.spi.XQueryProcessorProvider
import com.xmlcalabash.spi.XQueryProcessorServiceProvider
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import java.net.URI

open class XQueryStep(): AbstractAtomicStep() {
    lateinit var xqueryImpl: XQueryProcessor
    var requestedProcessor: URI? = null
    var fallbackProcessor: URI? = null
    var cachedQuery = false

    init {
        expectedExtensionAttributes.addAll(listOf(NsCx.processor, NsCx.fallback, NsCx.cacheQuery))
    }

    override fun extensionAttributes(attributes: Map<QName, String>, staticOptions: Map<QName, XdmValue>) {
        super.extensionAttributes(attributes, staticOptions)
        attributes[NsCx.processor]?.let { requestedProcessor = URI(it) }
        attributes[NsCx.fallback]?.let { fallbackProcessor = URI(it) }
        cachedQuery = extensionAttributeBooleanValue(attributes, NsCx.cacheQuery, staticOptions)
    }

    override fun run() {
        super.run()

        if (requestedProcessor != null) {
            var proc = getProcessorProvider(requestedProcessor!!)
            if (proc == null) {
                if (fallbackProcessor != null) {
                    proc = getProcessorProvider(fallbackProcessor!!)
                }
            }
            if (proc == null) {
                throw stepConfig.exception(XProcError.xdStepFailed("Cannot find requested XQuery processor (${requestedProcessor}) or fallback"))
            }
            xqueryImpl = proc.getImplementation()

            val pconfig = stepConfig.xmlCalabashConfig.configuredXQueryProcessors[proc.implementationUri] ?: emptyMap()
            xqueryImpl.setup(stepConfig, receiver, stepParams, cachedQuery, pconfig)
        } else {
            var processor = stepConfig.xmlCalabashConfig.defaultXQueryProcessor
            var found = false
            val attempted = mutableSetOf<URI>()
            while (!found) {
                val proc = getProcessorProvider(processor)
                if (proc != null) {
                    xqueryImpl = proc.getImplementation()
                    val pconfig = stepConfig.xmlCalabashConfig.configuredXQueryProcessors[proc.implementationUri] ?: emptyMap()
                    xqueryImpl.setup(stepConfig, receiver, stepParams, cachedQuery, pconfig)
                    found = true
                } else {
                    attempted.add(processor)
                    val config = stepConfig.xmlCalabashConfig.configuredXQueryProcessors[processor] ?: emptyMap()
                    val fallback = config[ConfigurationLoader.ccFallback]
                    if (fallback == null) {
                        throw stepConfig.exception(XProcError.xdStepFailed("Cannot find configured XQuery processor starting at ${stepConfig.xmlCalabashConfig.defaultXQueryProcessor}"))
                    } else {
                        processor = URI(fallback)
                        if (attempted.contains(processor)) {
                            throw stepConfig.exception(XProcError.xdStepFailed("Cannot find configured XQuery processor starting at ${stepConfig.xmlCalabashConfig.defaultXQueryProcessor}"))
                        }
                    }
                }
            }
        }

        val parameters = qnameMapBinding(Ns.parameters)
        val version = stringBinding(Ns.version) ?: "3.1"

        xqueryImpl.run(queues["source"] ?: emptyList(), queues["query"]!!.first(), parameters, version)
    }

    private fun getProcessorProvider(name: URI): XQueryProcessorProvider? {
        try {
            val provider = XQueryProcessorServiceProvider.provider(name)
            stepConfig.debug { "Running with XQuery processor: ${provider.implementationUri}" }
            return provider
        } catch (_: IllegalArgumentException) {
            return null
        }
    }

    override fun reset() {
        super.reset()
        xqueryImpl.reset()
        requestedProcessor = null
        fallbackProcessor = null
    }

    override fun teardown() {
        super.teardown()
        xqueryImpl.teardown()
    }
}