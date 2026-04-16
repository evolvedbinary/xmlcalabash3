package com.xmlcalabash.ext.elemental

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.api.Receiver
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import com.xmlcalabash.spi.XQueryProcessor
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmValue

class XQueryElementalProcessor: XQueryProcessor {
    companion object {
        val _databaseUri = QName("database-uri")
        val existns = NamespaceUri.of("http://exist.sourceforge.net/NS/exist")
        val cx_databaseUri = QName(NsCx.namespace, "cx:database-uri")
        private val serialns = NamespaceUri.of("http://exist-db.org/xquery/types/serialized")
    }

    lateinit var stepConfig: XProcStepConfiguration
    lateinit var receiver: Receiver
    lateinit var stepParams: RuntimeStepParameters

    lateinit var sources: List<XProcDocument>
    lateinit var query: String
    lateinit var parameters: Map<QName, XdmValue>

    private val config = mutableMapOf<QName, String>()

    var databaseUri: String? = null
    var username: String? = null
    var password: String? = null
    var requestTimeout: Int? = null
    var responseTimeout: Int? = null

    override fun setup(stepConfig: XProcStepConfiguration, receiver: Receiver, stepParams: RuntimeStepParameters, cacheQuery: Boolean, config: Map<QName, String>) {
        this.stepConfig = stepConfig
        this.receiver = receiver
        this.stepParams = stepParams
        this.config.putAll(config)
        if (cacheQuery) {
            stepConfig.debug { "Queries cannot be cached with the Elemental XQuery processor." }
        }
    }

    override fun run(sources: List<XProcDocument>, query: XProcDocument, parameters: Map<QName, XdmValue>, version: String) {
        this.sources = sources
        this.query = query.value.underlyingValue.stringValue
        this.parameters = parameters

        databaseUri = parameters[cx_databaseUri]?.underlyingValue?.stringValue ?: config[_databaseUri]
        username = parameters[NsCx.username]?.underlyingValue?.stringValue ?: config[Ns.username] ?: "admin"
        password = parameters[NsCx.password]?.underlyingValue?.stringValue ?: config[Ns.password] ?: ""
        requestTimeout = (parameters[NsCx.requestTimeout]?.underlyingValue?.stringValue ?: config[Ns.requestTimeout])?.toInt()
        responseTimeout = (parameters[NsCx.responseTimeout]?.underlyingValue?.stringValue ?: config[Ns.responseTimeout])?.toInt()

        val elementalServer: ElementalServer

        if (databaseUri == null) {
            elementalServer = LocalElementalServer() // TODO(AR) pass parameters for Elemental configuration properties and config file?
        } else {
            elementalServer = RemoteElementalServer(databaseUri!!, requestTimeout, responseTimeout)
        }

        val queryProperties = extractQueryPropertiesFromConfig()
        val queryVariables = extractQueryVariablesFromConfig()

        val queryResult: ElementalServer.QueryResult
        try {
            queryResult = elementalServer.query(stepConfig, this.query, false, username!!, password!!, queryProperties, queryVariables)
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xdStepFailed(ex.message ?: ""), ex)
        }

        // TODO(AR) figure out how to report compilation, execution, and unmarshalling times back to the step caller - add additional output ports perhaps?
//        receiver.output("compilation-time", queryResult.compilationTime)
//        receiver.output("execution-time", queryResult.executionTime)
//        receiver.output("umarshalling-time", queryResult.umarshallingTime)

        for (resultDoc in queryResult.result) {
            receiver.output("result", resultDoc)
        }
    }

    /**
     * Extract properties from the config that control how the XQuery is executed and serialized.
     *
     * @return A Map of Property names to values.
     */
    private fun extractQueryPropertiesFromConfig() : Map<QName, String> {
        val queryProperties = mutableMapOf<QName, String>()

        for ((name, value) in config) {
            if (name.namespaceUri == serialns) {
                queryProperties[QName(name.localName)] = value
            }
        }

        if (NsCx.properties in parameters) {
            val qprop = stepConfig.typeUtils.forceQNameKeys(parameters[NsCx.properties] as XdmMap)
            for ((name, value) in stepConfig.typeUtils.asMap(qprop)) {
                queryProperties[name] = value.underlyingValue.stringValue
            }
        }

        return queryProperties.toMap();
    }

    private fun extractQueryVariablesFromConfig() : Map <QName, XdmValue> {
        return parameters.filterKeys { qname ->  qname.namespaceUri != NsCx.namespace && qname.namespaceUri != existns }
    }

    override fun reset() {
        // nop
    }

    override fun teardown() {
        // nop
    }
}