package com.xmlcalabash.ext.elemental

import com.xmlcalabash.datamodel.DocumentContextImpl
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.ext.elemental.ElementalServer.QueryResult
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.NamespaceMap
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmArray
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmItem
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmNodeKind
import net.sf.saxon.s9api.XdmValue
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.charset.StandardCharsets
import javax.xml.XMLConstants
import kotlin.collections.iterator

class RemoteElementalServer(val databaseUri: String, val requestTimeout: Int? = null, val responseTimeout: Int? = null) : ElementalServer {

    val existns = NamespaceUri.of("http://exist.sourceforge.net/NS/exist")

    private val serialns = NamespaceUri.of("http://exist-db.org/xquery/types/serialized")
    private val exist_query = QName(existns, "query")
    private val exist_text = QName(existns, "text")
    private val exist_context_item = QName(existns, "context-item")
    private val exist_variables = QName(existns, "variables")
    private val exist_variable = QName(existns, "variable")
    private val exist_qname = QName(existns, "qname")
    private val exist_prefix = QName(existns, "prefix")
    private val exist_localname = QName(existns, "localname")
    private val exist_namespace = QName(existns, "namespace")
    private val serial_entry = QName(serialns, "entry")
    private val serial_key = QName(serialns, "key")
    private val serial_sequence = QName(serialns, "sequence")
    private val serial_value = QName(serialns, "value")
    private val exist_properties = QName(existns, "properties")
    private val exist_property = QName(existns, "property")
    private val query_results_cache = QName("cache")
    private val query_results_max = QName("max")
    private val query_results_start = QName("start")
    private val query_results_wrap = QName("wrap")
    private val query_results_typed = QName("typed")

    override fun query(stepConfig: XProcStepConfiguration, sources: List<XProcDocument>, query: String, cacheQuery: Boolean, username: String, password: String, properties: Map<QName, String>?, variableBindings: Map<QName, XdmValue>?) : QueryResult {

        val builder = SaxonTreeBuilder(stepConfig)

        builder.startDocument(null)

        // Create XML for Query
        val queryParameters: Map<QName, String> = mapOf(
            query_results_wrap to "yes",
            query_results_typed to "yes",

            // TODO(AR) figure out if we can use caching here or not
            query_results_cache to "no",
            query_results_start to "1",
            query_results_max to "-1"
        )

        val attrs = stepConfig.typeUtils.attributeMap(queryParameters)

        // explicitly declare 'xs' XML Schema namespace on root query element
        var nsmap = NamespaceMap.emptyMap()
        if (!exist_query.namespaceUri.isEmpty) {
            nsmap = nsmap.put(exist_query.prefix, exist_query.namespaceUri)
        }
        for (index in 0 until attrs.size()) {
            val attr = attrs.itemAt(index)
            if (attr.nodeName.namespaceUri != NamespaceUri.NULL) {
                nsmap = nsmap.put(attr.nodeName.prefix, attr.nodeName.namespaceUri)
            }
        }
        nsmap = nsmap.put("xs", NamespaceUri.of(XMLConstants.W3C_XML_SCHEMA_NS_URI))

        builder.addStartElement(exist_query, attrs, nsmap)

        // Create XML for XQuery content
        builder.addStartElement(exist_text)
        builder.addText(query)
        builder.addEndElement()

        // TODO(AR) set default collection in Elemental
        // Create XML for Context Item
        if (!sources.isEmpty()) {
            val xdmValue: XdmValue = sources[0].value
            if (!xdmValue.isEmptySequence) {
                val xdmItem = xdmValue.itemAt(0)
                builder.addStartElement(exist_context_item)
                serializeItem(stepConfig, builder, xdmItem)
                builder.addEndElement()
            }
        }

        // Create XML for XQuery Variable bindings
        if (variableBindings != null) {
            var startedVariables = false
            for ((qname, value) in variableBindings) {

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

                builder.addStartElement(serial_sequence)

                for (item in value.iterator()) {
                    serializeItem(stepConfig, builder, item)
                }

                builder.addEndElement()

                builder.addEndElement()
            }
            if (startedVariables) {
                builder.addEndElement()
            }
        }

        // Create XML for Properties
        if (properties != null) {
            builder.addStartElement(exist_properties)
            for ((name, value) in properties) {
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
        stepConfig.debug { "Elemental remote database query: ${queryXml}"}

        val request = ElementalInternetProtocolRequest(stepConfig, URI(databaseUri))
        if (username != null) {
            request.authentication("basic", username!!, password!!, true)
        }
        if (requestTimeout != null) {
            request.requestTimeout = requestTimeout
        }
        if (responseTimeout != null) {
            request.responseTimeout = responseTimeout
        }
        request.addSource(XProcDocument.ofXml(queryXml, stepConfig,MediaType.XML))

        val response = request.execute("post")
        if (response.statusCode == 200) {

            val elementalResponseDocuments = response.response

            // NOTE(AR) extracting processing times like this should be okay, as Elemental will only ever produce one response document
            val firstElementalResponseDocument = elementalResponseDocuments[0]
            val xqueryCompilationTime = firstElementalResponseDocument.xqueryCompilationTime
            val xqueryExecutionTime = firstElementalResponseDocument.xqueryExecutionTime
            val unmarshallingTime = firstElementalResponseDocument.unmarshallingTime

            val startDocumentConversion = System.currentTimeMillis()
            val xprocDocuments = toXProcDocuments(stepConfig, elementalResponseDocuments)
            val documentConversionTime = System.currentTimeMillis() - startDocumentConversion

            return QueryResult(xqueryCompilationTime, xqueryExecutionTime, unmarshallingTime + documentConversionTime, xprocDocuments)

        } else {
            throw stepConfig.exception(XProcError.xdStepFailed(response.response.first().results.first().toString()))
        }
    }

    private fun toXProcDocuments(stepConfig: XProcStepConfiguration, elementalResponseDocuments: List<ElementalQueryTypedResponseDocument>) : List<XProcDocument> {
        return elementalResponseDocuments.flatMap { elementalResponseDocument ->
            elementalResponseDocument.results.map { elementalResponseItem ->
                XProcDocument.ofValue(elementalResponseItem, DocumentContextImpl(stepConfig.saxonConfig))
                    .with(elementalResponseDocument.documentProperties)
            }
        }
    }

    private fun serializeItem(stepConfig: XProcStepConfiguration, builder: SaxonTreeBuilder, item: XdmItem) {
        when (item) {
            is XdmAtomicValue -> serializeAtomicValue(stepConfig, builder, item)

            is XdmNode -> {
                val nodeKind = item.nodeKind
                when (nodeKind) {
                    XdmNodeKind.DOCUMENT -> {
                        builder.addStartElement(
                            serial_value,
                            stepConfig.typeUtils.attributeMap(mapOf(Ns.type to "document-node()"))
                        )
                        builder.addSubtree(item)
                        builder.addEndElement()
                    }

                    XdmNodeKind.ELEMENT -> {
                        builder.addStartElement(
                            serial_value,
                            stepConfig.typeUtils.attributeMap(mapOf(Ns.type to "element()"))
                        )
                        builder.addSubtree(item)
                        builder.addEndElement()
                    }

                    XdmNodeKind.ATTRIBUTE -> {
                        val attrName = item.nodeName
                        var attrNsMap: NamespaceMap = NamespaceMap.of(XMLConstants.DEFAULT_NS_PREFIX, serialns)
                        if (attrName.namespaceUri != NamespaceUri.NULL) {
                            attrNsMap = attrNsMap.put(attrName.prefix, attrName.namespaceUri)
                        }
                        val name: String
                        if (attrName.prefix == XMLConstants.DEFAULT_NS_PREFIX) {
                            name = attrName.localName
                        } else {
                            name = attrName.prefix + ":" + attrName.localName
                        }
                        builder.addStartElement(
                            serial_value,
                            stepConfig.typeUtils.attributeMap(mapOf(
                                Ns.type to "attribute()",
                                Ns.name to name
                            )),
                            attrNsMap
                        )
                        builder.addText(item.stringValue)
                        builder.addEndElement()
                    }

                    XdmNodeKind.COMMENT -> {
                        builder.addStartElement(
                            serial_value,
                            stepConfig.typeUtils.attributeMap(mapOf(
                                Ns.type to "comment()",
                            ))
                        )
                        builder.addComment(item.stringValue)
                        builder.addEndElement()
                    }

                    XdmNodeKind.TEXT -> {
                        builder.addStartElement(
                            serial_value,
                            stepConfig.typeUtils.attributeMap(mapOf(
                                Ns.type to "text()",
                            ))
                        )
                        builder.addText(item.stringValue)
                        builder.addEndElement()
                    }

                    XdmNodeKind.PROCESSING_INSTRUCTION -> {
                        builder.addStartElement(
                            serial_value,
                            stepConfig.typeUtils.attributeMap(mapOf(
                                Ns.type to "processing-instruction()",
                            ))
                        )
                        builder.addPI(item.nodeName.localName, item.stringValue)
                        builder.addEndElement()
                    }

                    else -> throw IllegalStateException("Undefined XDM Node type conversion from Saxon to Elemental for: $nodeKind")
                }
            }

            is XdmMap -> {
                builder.addStartElement(
                    serial_value,
                    stepConfig.typeUtils.attributeMap(mapOf(Ns.type to "map(*)"))
                )

                // Map entries
                item.entrySet().forEach { mapEntry ->
                    builder.addStartElement(serial_entry)

                    // Map key
                    val mapKey = mapEntry.key
                    serializeAtomicValue(stepConfig, builder, mapKey, serial_key)

                    // Map value sequence
                    builder.addStartElement(serial_sequence)
                    mapEntry.value
                    mapEntry.value.forEach { mapValueSequenceItem ->
                        serializeItem(stepConfig, builder, mapValueSequenceItem)
                    }
                    builder.addEndElement()

                    builder.addEndElement()
                }

                builder.addEndElement()
            }

            is XdmArray -> {
                builder.addStartElement(
                    serial_value,
                    stepConfig.typeUtils.attributeMap(mapOf(Ns.type to "array(*)"))
                )

                // Array value sequence
                for (i in 0 ..< item.arrayLength()) {
                    builder.addStartElement(serial_sequence)
                    item.get(i).forEach { arrayValueSequenceItem ->
                        serializeItem(stepConfig, builder, arrayValueSequenceItem)
                    }
                    builder.addEndElement()
                }

                builder.addEndElement()
            }

            else -> {
                val type = "xs:untyped"
                builder.addStartElement(
                    serial_value,
                    stepConfig.typeUtils.attributeMap(mapOf(Ns.type to type))
                )
                builder.addText(item.underlyingValue.stringValue)
                builder.addEndElement()
            }
        }
    }

    private fun serializeAtomicValue(stepConfig: XProcStepConfiguration, builder: SaxonTreeBuilder, atomicValue: XdmAtomicValue, elementContainerName: QName = serial_value) {
        val type = "xs:${atomicValue.primitiveTypeName.localName}"
        builder.addStartElement(
            elementContainerName,
            stepConfig.typeUtils.attributeMap(mapOf(Ns.type to type))
        )
        builder.addText(atomicValue.underlyingValue.stringValue)
        builder.addEndElement()
    }
}
