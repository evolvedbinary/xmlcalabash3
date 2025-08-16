package com.xmlcalabash.ext.existdb

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.Ns.properties
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.steps.AbstractAtomicStep
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import java.net.URI

class ExistDbStep(): AbstractAtomicStep() {
    companion object {
        val EXISTDB = URI.create("https://exist-db.org/")
        val _databaseUri = QName("database-uri")
        val _queryParameters = QName("query-parameters")
        val _queryProperties = QName("query-properties")
    }

    lateinit var xqueryImpl: XQueryExistDbProcessor

    override fun run() {
        super.run()

        val config = mutableMapOf<QName, String>()
        config.putAll(stepConfig.xmlCalabashConfig.configuredXQueryProcessors[EXISTDB] ?: emptyMap())

        stringBinding(_databaseUri)?.let { config[_databaseUri] = it }
        stringBinding(Ns.username)?.let { config[Ns.username] = it }
        stringBinding(Ns.password)?.let { config[Ns.password] = it }

        xqueryImpl = XQueryExistDbProcessor()

        xqueryImpl.setup(stepConfig, receiver, stepParams, config)

        val qparams = qnameMapBinding(_queryParameters)
        val qproperties = qnameMapBinding(_queryProperties)
        val parameters = mutableMapOf<QName, XdmValue>()

        parameters.putAll(qnameMapBinding(Ns.parameters))
        parameters[NsCx.query] = stepConfig.typeUtils.asXdmMap(qparams)
        parameters[NsCx.properties] = stepConfig.typeUtils.asXdmMap(qproperties)

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