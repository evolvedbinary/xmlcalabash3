package com.xmlcalabash.ext.basex

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.steps.AbstractAtomicStep
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import java.net.URI
import kotlin.math.exp

class BaseXStep(): AbstractAtomicStep() {
    companion object {
        val BASEX = URI.create("https://basex.org/")
    }

    lateinit var xqueryImpl: XQueryBaseXProcessor
    var cachedQuery = false

    init {
        expectedExtensionAttributes.addAll(listOf(NsCx.cacheQuery))
    }

    override fun extensionAttributes(attributes: Map<QName, String>, staticOptions: Map<QName, XdmValue>) {
        super.extensionAttributes(attributes, staticOptions)
        cachedQuery = extensionAttributeBooleanValue(attributes, NsCx.cacheQuery, staticOptions)
    }

    override fun run() {
        super.run()

        val config = mutableMapOf<QName, String>()
        config.putAll(stepConfig.xmlCalabashConfig.configuredXQueryProcessors[BASEX] ?: emptyMap())

        stringBinding(Ns.host)?.let { config[Ns.host] = it }
        stringBinding(Ns.port)?.let { config[Ns.port] = it }
        stringBinding(Ns.username)?.let { config[Ns.username] = it }
        stringBinding(Ns.password)?.let { config[Ns.password] = it }

        xqueryImpl = XQueryBaseXProcessor()

        xqueryImpl.setup(stepConfig, receiver, stepParams, cachedQuery, config)

        val parameters = qnameMapBinding(Ns.parameters)
        val version = stringBinding(Ns.version) ?: "3.1"

        xqueryImpl.run(queues["source"] ?: emptyList(), queues["query"]!!.first(), parameters, version)
    }

    override fun reset() {
        super.reset()
        xqueryImpl.reset()
    }

    override fun teardown() {
        super.teardown()
        xqueryImpl.teardown()
    }
}