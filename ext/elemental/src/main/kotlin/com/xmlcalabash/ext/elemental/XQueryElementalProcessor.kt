package com.xmlcalabash.ext.elemental

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.io.InternetProtocolRequest
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.api.Receiver
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import com.xmlcalabash.spi.XQueryProcessor
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmArray
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.charset.StandardCharsets

class XQueryElementalProcessor(): XQueryProcessor {
    companion object {
        val _databaseUri = QName("database-uri")
        val existns = NamespaceUri.of("http://exist.sourceforge.net/NS/exist")
        val cx_databaseUri = QName(NsCx.namespace, "cx:database-uri")

        private val serialns = NamespaceUri.of("http://exist-db.org/xquery/types/serialized")
        private val exist_query = QName(existns, "query")
        private val exist_text = QName(existns, "text")
        private val exist_variables = QName(existns, "variables")
        private val exist_variable = QName(existns, "variable")
        private val exist_qname = QName(existns, "qname")
        private val exist_prefix = QName(existns, "prefix")
        private val exist_localname = QName(existns, "localname")
        private val exist_namespace = QName(existns, "namespace")
        private val serial_sequence = QName(serialns, "sequence")
        private val serial_value = QName(serialns, "value")
        private val exist_properties = QName(existns, "properties")
        private val exist_property = QName(existns, "property")
        private val _cache = QName("cache")
        private val _wrap = QName("wrap")
        private val _typed = QName("typed")
    }

    lateinit var stepConfig: XProcStepConfiguration
    lateinit var receiver: Receiver
    lateinit var stepParams: RuntimeStepParameters

    lateinit var sources: List<XProcDocument>
    lateinit var parameters: Map<QName, XdmValue>

    private val config = mutableMapOf<QName, String>()

    override fun setup(stepConfig: XProcStepConfiguration, receiver: Receiver, stepParams: RuntimeStepParameters, config: Map<QName, String>) {
        this.stepConfig = stepConfig
        this.receiver = receiver
        this.stepParams = stepParams
        this.config.putAll(config)
    }

    override fun run(sources: List<XProcDocument>, query: XProcDocument, parameters: Map<QName, XdmValue>, version: String) {
        val databaseUri = parameters[cx_databaseUri]?.underlyingValue?.stringValue
            ?: config[_databaseUri]
            ?: throw stepConfig.exception(XProcError.xdStepFailed("No database-uri configured for eXist-db"))

        val username = parameters[NsCx.username]?.underlyingValue?.stringValue ?: config[Ns.username]
        val password = parameters[NsCx.password]?.underlyingValue?.stringValue ?: config[Ns.password]

        if ((username != null && password == null) || (username == null && password != null)) {
            throw stepConfig.exception(XProcError.xdStepFailed("Either or both of username and password must be specified"))
        }

        this.sources = sources
        this.parameters = parameters

        val qparameters = mutableMapOf<QName, String>()
        for ((name, value) in config) {
            if (name.namespaceUri == existns) {
                qparameters[QName(name.localName)] = value
            }
        }

        if (NsCx.query in parameters) {
            val qparam = stepConfig.typeUtils.forceQNameKeys(parameters[NsCx.query] as XdmMap)
            for ((name, value) in stepConfig.typeUtils.asMap(qparam)) {
                qparameters[name] = value.underlyingValue.stringValue
            }
        }
        if (_wrap !in qparameters) {
            if (qparameters[_cache] == "yes") {
                qparameters[_wrap] = "yes"
            } else {
                qparameters[_wrap] = "no"
            }
        }
        if (_typed !in qparameters) {
            qparameters[_typed] = "no"
        }

        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(null)
        builder.addStartElement(exist_query, stepConfig.typeUtils.attributeMap(qparameters))
        builder.addStartElement(exist_text)
        builder.addText(query.value.underlyingValue.stringValue)
        builder.addEndElement()

        var startedVariables = false
        for ((qname, value) in parameters) {
            val skip = qname.namespaceUri == NsCx.namespace
                    || qname.namespaceUri == existns

            if (!skip) {
                if (!startedVariables) {
                    builder.addStartElement(exist_variables)
                    startedVariables = true
                }
                builder.addStartElement(exist_variable)

                builder.addStartElement(exist_qname)
                if (qname.prefix.isNotEmpty()) {
                    builder.addStartElement(exist_prefix)
                    builder.addText(qname.prefix)
                    builder.addEndElement()
                }
                builder.addStartElement(exist_localname)
                builder.addText(qname.localName)
                builder.addEndElement()
                if (qname.namespaceUri != NamespaceUri.NULL) {
                    builder.addStartElement(exist_namespace)
                    builder.addText(qname.namespaceUri.toString())
                    builder.addEndElement()
                }
                builder.addEndElement()

                // FIXME: handle sequence and type attribute
                builder.addStartElement(serial_sequence)
                for (item in value.iterator()) {
                    when (item) {
                        is XdmAtomicValue -> {
                            val type = "xs:${item.primitiveTypeName.localName}"
                            builder.addStartElement(serial_value, stepConfig.typeUtils.attributeMap(mapOf(Ns.type to type)))
                            builder.addText(item.underlyingValue.stringValue)
                            builder.addEndElement()
                        }
                        is XdmNode -> {
                            val type = "document-node()"
                            builder.addStartElement(serial_value, stepConfig.typeUtils.attributeMap(mapOf(Ns.type to type)))
                            val doc = XProcDocument.ofValue(item, stepConfig, MediaType.XML)
                            val baos = ByteArrayOutputStream()
                            val writer = DocumentWriter(doc, baos)
                            writer.write()
                            builder.addText(baos.toString(StandardCharsets.UTF_8))
                            builder.addEndElement()
                        }
                        is XdmMap, is XdmArray -> {
                            val type = if (item is XdmMap) { "map(*)" } else { "array(*)" }
                            builder.addStartElement(serial_value, stepConfig.typeUtils.attributeMap(mapOf(Ns.type to type)))
                            val doc = XProcDocument.ofValue(item, stepConfig, MediaType.JSON)
                            val baos = ByteArrayOutputStream()
                            val writer = DocumentWriter(doc, baos)
                            writer.write()
                            builder.addText(baos.toString(StandardCharsets.UTF_8))
                            builder.addEndElement()
                        }
                        else -> {
                            val type = "xs:untyped"
                            builder.addStartElement(serial_value, stepConfig.typeUtils.attributeMap(mapOf(Ns.type to type)))
                            builder.addText(item.underlyingValue.stringValue)
                            builder.addEndElement()
                        }
                    }
                }
                builder.addEndElement()

                builder.addEndElement()
            }
        }
        if (startedVariables) {
            builder.addEndElement()
        }

        val qproperties = mutableMapOf<QName, String>()
        for ((name, value) in config) {
            if (name.namespaceUri == serialns) {
                qproperties[QName(name.localName)] = value
            }
        }

        if (NsCx.properties in parameters) {
            val qprop = stepConfig.typeUtils.forceQNameKeys(parameters[NsCx.properties] as XdmMap)
            for ((name, value) in stepConfig.typeUtils.asMap(qprop)) {
                qproperties[name] = value.underlyingValue.stringValue
            }
        }
        if (qproperties.isNotEmpty()) {
            builder.addStartElement(exist_properties)
            for ((name, value) in qproperties) {
                builder.addStartElement(exist_property, stepConfig.typeUtils.attributeMap(mapOf(
                    QName(name.localName) to value
                )))
                builder.addEndElement()
            }
            builder.addEndElement()
        }

        builder.addEndElement()
        builder.endDocument()

        val queryXml = builder.result
        stepConfig.debug { "eXist dabase query: ${queryXml}"}

        val request = InternetProtocolRequest(stepConfig, URI(databaseUri))
        if (username != null) {
            request.authentication("basic", username, password!!, true)
        }
        request.addSource(XProcDocument.ofXml(queryXml, stepConfig,MediaType.XML))
        try {
            val response = request.execute("post")
            if (response.statusCode == 200) {
                for (doc in response.response) {
                    receiver.output("result", doc)
                }
            } else {
                throw stepConfig.exception(XProcError.xdStepFailed(response.response.first().value.toString()))
            }
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xdStepFailed(ex.message ?: ""), ex)
        }
    }

    override fun reset() {
        // nop
    }

    override fun teardown() {
        // nop
    }
}