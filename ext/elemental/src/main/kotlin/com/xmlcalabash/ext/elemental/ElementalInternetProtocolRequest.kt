package com.xmlcalabash.ext.elemental

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.io.AbstractInternetProtocolRequest
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.util.DefaultLocation
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.event.ReceiverOption
import net.sf.saxon.om.AttributeInfo
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.om.FingerprintedQName
import net.sf.saxon.om.LargeAttributeMap
import net.sf.saxon.om.NamespaceResolver
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.om.SmallAttributeMap
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.s9api.ItemType
import net.sf.saxon.s9api.ItemTypeFactory
import net.sf.saxon.s9api.Location
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmArray
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmItem
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.str.StringView
import net.sf.saxon.tree.util.Orphan
import net.sf.saxon.type.BuiltInAtomicType
import net.sf.saxon.type.Type
import org.xml.sax.Attributes
import org.xml.sax.ContentHandler
import org.xml.sax.InputSource
import org.xml.sax.Locator
import org.xml.sax.SAXException
import org.xml.sax.ext.LexicalHandler
import java.io.InputStream
import java.net.URI
import java.nio.charset.Charset
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory

class ElementalInternetProtocolRequest(override val stepConfig: StepConfiguration, override val uri: URI) : AbstractInternetProtocolRequest<ElementalQueryTypedResponseDocument, ElementalInternetProtocolResponse>(stepConfig, uri) {

    override fun createResponse(responseUri: URI, statusCode: Int): ElementalInternetProtocolResponse {
        return ElementalInternetProtocolResponse(responseUri, statusCode)
    }

    override fun buildDocument(href: URI?, documentProperties: DocumentProperties, stream: InputStream, mediaType: MediaType, charset: Charset?) : ElementalQueryTypedResponseDocument {
        val startTime = System.currentTimeMillis()
        val factory = SAXParserFactory.newInstance()
        factory.setNamespaceAware(true)
        factory.setValidating(false)
        val saxParser = factory.newSAXParser()
        val xmlReader = saxParser.getXMLReader()

        val contentHandler = ElementalQueryResultHandler(stepConfig)
        xmlReader.setContentHandler(contentHandler)
        xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", contentHandler)
        xmlReader.parse(InputSource(stream))
        val results = contentHandler.results()

        val unmarshallingTime = System.currentTimeMillis() - startTime

        return ElementalQueryTypedResponseDocument(contentHandler.compilationTime, contentHandler.executionTime, unmarshallingTime, results)
    }

    override fun addDocumentProperties(responseDocument: ElementalQueryTypedResponseDocument, properties: Map<QName, XdmValue>): ElementalQueryTypedResponseDocument {
        properties.forEach(responseDocument.documentProperties::set)
        return responseDocument
    }

    /**
     * SAX Content Handler for an exist:result document
     * produced by Elemental's REST API when wrap="true" typed="true"
     * is set on the request.
     */
    class ElementalQueryResultHandler(val stepConfig: StepConfiguration) : ContentHandler, LexicalHandler {
        enum class ResultState {
            UNINITIALIZED,

            STARTED,
            STARTED_RESULT,

            STARTED_DOCUMENT,
            FINISHED_DOCUMENT,

            STARTED_ELEMENT,
            FINISHED_ELEMENT,

            STARTED_ATTRIBUTE,
            FINISHED_ATTRIBUTE,

            STARTED_TEXT,
            FINISHED_TEXT,

            STARTED_VALUE,
            FINISHED_VALUE,

            STARTED_ARRAY,
            FINISHED_ARRAY,

            STARTED_MAP,
            FINISHED_MAP,

            FINISHED_RESULT,
            FINISHED
        }
        private var state = ResultState.UNINITIALIZED

        private var elementDepth = 0
        private var attrQname : StructuredQName? = null
        private var valueType : ItemType? = null
        private val characters = StringBuilder()
        private var saxonTreeBuilder : SaxonTreeBuilder? = null

        private val existns = NamespaceUri.of("http://exist.sourceforge.net/NS/exist")
        private val exist_result = QName(existns, "result")
        private val exist_document = QName(existns, "document")
        private val exist_attribute = QName(existns, "attribute")
        private val exist_text = QName(existns, "text")
        private val exist_sequence = QName(existns, "sequence")
        private val exist_value = QName(existns, "value")
        private val exist_array = QName(existns, "array")
        private val exist_map = QName(existns, "map")
        private val exist_entry = QName(existns, "entry")
        private val exist_key = QName(existns, "key")

        private var locator: Locator? = null
        private val prefixMappings: MutableMap<String, ArrayDeque<NamespaceUri>> = mutableMapOf(Pair(XMLConstants.DEFAULT_NS_PREFIX, ArrayDeque(listOf(NamespaceUri.NULL))))
        private val namespaceResolver = NamespaceResolverImpl()
        private val itemTypeFactory = ItemTypeFactory(stepConfig.processor)

        private var hits: Int = 0
        internal var compilationTime: Long = -1
        internal var executionTime: Long = -1
        private val results: ArrayDeque<MutableList<XdmItem>> = ArrayDeque(listOf(mutableListOf()))
        private val sequencesCount: ArrayDeque<Int> = ArrayDeque()

        inner class NamespaceResolverImpl : NamespaceResolver {
            var defaultNamespace: NamespaceUri = NamespaceUri.NULL

            override fun getURIForPrefix(prefix: String, useDefault: Boolean): NamespaceUri? {
                if (useDefault && XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
                    return defaultNamespace
                } else {
                    val deque = prefixMappings[prefix]
                    if (deque == null) {
                        return null
                    }

                    return deque.first()
                }
            }

            override fun iteratePrefixes(): Iterator<String> {
                return prefixMappings.keys.iterator()
            }
        }

        override fun setDocumentLocator(locator: Locator?) {
            this.locator = locator
        }

        override fun startDocument() {
            state = ResultState.STARTED
        }

        override fun endDocument() {
            state = ResultState.FINISHED
        }

        override fun startPrefixMapping(prefix: String, uri: String) {
            var deque = prefixMappings[prefix]
            if (deque == null) {
                deque = ArrayDeque()
                prefixMappings[prefix] = deque
            }
            deque.addFirst(NamespaceUri.of(uri))
        }

        override fun endPrefixMapping(prefix: String) {
            var deque = prefixMappings[prefix]
            if (deque != null) {
                deque.removeFirst()
                if (deque.isEmpty()) {
                    prefixMappings.remove(prefix)
                }
            }
        }

        override fun startElement(uri: String, localName: String, qName: String, atts: Attributes) {
            val colon = qName.indexOf(':')
            var prefix: String? = null
            if (colon > -1) {
                prefix = qName.substring(0, colon)
                startPrefixMapping(prefix, uri)
            } else {
                namespaceResolver.defaultNamespace = NamespaceUri.of(uri)
            }

            val structuredQname = StructuredQName.fromLexicalQName(qName, true, false, namespaceResolver)
            val qname = QName(structuredQname)

            if (qname == exist_result) {
                extractResultInfo(atts)
                state = ResultState.STARTED_RESULT

            } else if (qname == exist_document) {
                saxonTreeBuilder = SaxonTreeBuilder(stepConfig)
                saxonTreeBuilder!!.startDocument(null)        // TODO(AR) implement base URI support in Elemental output
                state = ResultState.STARTED_DOCUMENT

            } else if (qname == exist_attribute) {
                extractAttrQName(atts)
                state = ResultState.STARTED_ATTRIBUTE

            } else if (qname == exist_text) {
                state = ResultState.STARTED_TEXT

            } else if (qname == exist_value) {
                extractType(atts)
                state = ResultState.STARTED_VALUE

            } else if (qname == exist_array) {
                // for an XDM Array, `sequencesCount` is the number of entries in the Array, each needs to be popped from `results`
                sequencesCount.addFirst(0)
                state = ResultState.STARTED_ARRAY

            } else if (qname == exist_map) {
                // for an XDM Map, `sequencesCount` is the number of 2x the entries in the Map, 2x is needed to keep the keys and the values of the entry, these all need to be popped from `results`
                sequencesCount.addFirst(0)
                state = ResultState.STARTED_MAP

            } else if (qname == exist_entry) {
                results.addFirst(mutableListOf())
                sequencesCount.addFirst(sequencesCount.removeFirst() + 1)  // just add one to the current sequencesCount

            } else if (qname == exist_key) {
                // no-op

            } else if (qname == exist_sequence) {
                results.addFirst(mutableListOf())
                sequencesCount.addFirst(sequencesCount.removeFirst() + 1)  // just add one to the current sequencesCount

            } else {
                // any other type of element

                if (state != ResultState.STARTED_DOCUMENT && saxonTreeBuilder == null) {
                    saxonTreeBuilder = SaxonTreeBuilder(stepConfig)
                    saxonTreeBuilder!!.startDocument(null)
                }

                val attributeMap: AttributeMap
                if (atts.length == 0) {
                    attributeMap = EmptyAttributeMap.getInstance()
                } else if (atts.length <= SmallAttributeMap.LIMIT) {
                    attributeMap = SmallAttributeMap(toAttributeInfos(atts))
                } else {
                    attributeMap = LargeAttributeMap(toAttributeInfos(atts))
                }

                saxonTreeBuilder!!.addStartElement(qname, attributeMap)
                elementDepth++

                if (prefix != null) {
                    endPrefixMapping(prefix)
                } else {
                    namespaceResolver.defaultNamespace = NamespaceUri.NULL
                }

                if (state != ResultState.STARTED_DOCUMENT) {
                    state = ResultState.STARTED_ELEMENT
                }
            }
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            val structuredQname = StructuredQName.fromLexicalQName(qName, true, false, namespaceResolver)
            val qname = QName(structuredQname)

            if (qname == exist_result) {
                state = ResultState.FINISHED_RESULT

            } else if (qname == exist_document) {
                saxonTreeBuilder!!.endDocument()
                addResult(saxonTreeBuilder!!.result)
                saxonTreeBuilder = null
                state = ResultState.FINISHED_DOCUMENT

            } else if (qname == exist_attribute) {
                val attr = Orphan(stepConfig.saxonConfig.configuration)
                attr.setNodeKind(Type.ATTRIBUTE)
                attr.setNodeName(FingerprintedQName(attrQname))
                attr.setStringValue(StringView.of(characters.toString()))
                characters.clear()
                addResult(XdmNode(attr))
                state = ResultState.FINISHED_ATTRIBUTE

            } else if (qname == exist_text) {
                val text = Orphan(stepConfig.saxonConfig.configuration)
                text.setNodeKind(Type.TEXT)
                text.setStringValue(StringView.of(characters.toString()))
                characters.clear()
                addResult(XdmNode(text))
                state = ResultState.FINISHED_TEXT

            } else if (qname == exist_value) {
                val value = XdmAtomicValue(characters.toString(), valueType!!)
                characters.clear()
                addResult(value)
                state = ResultState.FINISHED_VALUE

            } else if (qname == exist_array) {
                val count = sequencesCount.removeFirst()
                val arraySequences: List<XdmValue> = List(count, { _ ->
                    XdmValue(results.removeFirst())
                })
                val array = XdmArray(arraySequences.reversed())
                addResult(array)

                state = ResultState.FINISHED_ARRAY

            } else if (qname == exist_map) {
                val count = sequencesCount.removeFirst()
                val mapEntries: MutableMap<XdmAtomicValue, XdmValue> = mutableMapOf()
                for (i in 1..count / 2) {
                    val value = XdmValue(results.removeFirst())
                    val key = results.removeFirst()[0] as XdmAtomicValue
                    mapEntries[key] = value
                }
                val map = XdmMap(mapEntries)
                addResult(map)

                state = ResultState.FINISHED_MAP

            } else if (qname == exist_entry) {
                // no-op

            } else if (qname == exist_key) {
                // no-op

            } else if (qname == exist_sequence) {
                // no-op

            } else {
                // any other type of element

                saxonTreeBuilder!!.addEndElement()
                elementDepth--

                if (state != ResultState.STARTED_DOCUMENT && elementDepth == 0) {
                    // This the end of a standalone element
                    saxonTreeBuilder!!.endDocument()
                    addResult(saxonTreeBuilder!!.result.outermostElement)
                    saxonTreeBuilder = null
                }

                state = ResultState.FINISHED_ELEMENT
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            if (state == ResultState.STARTED_DOCUMENT || state == ResultState.STARTED_ELEMENT) {
                saxonTreeBuilder!!.addText(String(ch, start, length))

            } else if (state == ResultState.STARTED_ATTRIBUTE || state == ResultState.STARTED_TEXT || state == ResultState.STARTED_VALUE){
                characters.append(ch, start, length)
            }
        }

        override fun ignorableWhitespace(ch: CharArray, start: Int, length: Int) {
        }

        override fun processingInstruction(target: String, data: String) {
            if (state == ResultState.STARTED_DOCUMENT || state == ResultState.STARTED_ELEMENT) {
                saxonTreeBuilder!!.addPI(target, data)

            } else {
                val pi = Orphan(stepConfig.saxonConfig.configuration)
                pi.setNodeKind(Type.PROCESSING_INSTRUCTION)
                pi.setNodeName(FingerprintedQName.fromClarkName(target))
                pi.setStringValue(StringView.of(data))
                addResult(XdmNode(pi))
            }
        }

        override fun skippedEntity(name: String) {
        }

        override fun comment(ch: CharArray, start: Int, length: Int) {
            if (state == ResultState.STARTED_DOCUMENT || state == ResultState.STARTED_ELEMENT) {
                saxonTreeBuilder!!.addComment(String(ch, start, length))

            } else {
                val comment = Orphan(stepConfig.saxonConfig.configuration)
                comment.setNodeKind(Type.COMMENT)
                comment.setStringValue(StringView.of(String(ch, start, length)))
                addResult(XdmNode(comment))
            }
        }

        override fun startDTD(name: String, publicId: String, systemId: String) {
        }

        override fun endDTD() {
        }

        override fun startEntity(name: String) {
        }

        override fun endEntity(name: String) {
        }

        override fun startCDATA() {
        }

        override fun endCDATA() {
        }

        private fun extractResultInfo(atts: Attributes) {
            hits = atts.getValue(existns.toString(), "hits").toInt()
            compilationTime = atts.getValue(existns.toString(),"compilation-time").toLong()
            executionTime = atts.getValue(existns.toString(), "execution-time").toLong()
        }

        private fun extractAttrQName(atts: Attributes) {
            val localName: String = atts.getValue(existns.toString(), "local")
            val namespace: String? = atts.getValue(existns.toString(), "target-namespace")
            val prefix: String? = atts.getValue(existns.toString(), "prefix")
            attrQname = StructuredQName(prefix ?: XMLConstants.DEFAULT_NS_PREFIX, namespace ?: XMLConstants.NULL_NS_URI, localName)
        }

        private fun extractType(atts: Attributes) {
            val type = atts.getValue(existns.toString(), "type")
            valueType = itemTypeFactory.parseItemType(type)
        }

        private fun toAttributeInfos(atts: Attributes) : List<AttributeInfo> {
            val attributeInfos : MutableList<AttributeInfo> = mutableListOf()

            for (index in 0 ..< atts.length) {
                val uri = atts.getURI(index)
                val qName = atts.getQName(index)

                val colon = qName.indexOf(':')
                var prefix: String? = null
                if (colon > -1) {
                    prefix = qName.substring(0, colon)
                    startPrefixMapping(prefix, uri)
                } else {
                    namespaceResolver.defaultNamespace = NamespaceUri.of(uri)
                }

                val structuredQname = StructuredQName.fromLexicalQName(qName, true, false, namespaceResolver)
                val nodeName = FingerprintedQName(structuredQname)
                val value = atts.getValue(index)

                val location: Location?
                if (locator != null) {
                   location = DefaultLocation(locator!!.systemId, locator!!.lineNumber, locator!!.columnNumber)
                } else {
                    location = null
                }

                val type = atts.getType(index)
                var properties = ReceiverOption.NONE
                if ("ID" == type) {
                    properties = properties.or(ReceiverOption.IS_ID)
                } else if ("IDREF" == type) {
                    properties = properties.or(ReceiverOption.IS_IDREF)
                }

                val attributeInfo = AttributeInfo(nodeName, BuiltInAtomicType.STRING, value, location, properties)
                attributeInfos += attributeInfo

                if (prefix != null) {
                    endPrefixMapping(prefix)
                } else {
                    namespaceResolver.defaultNamespace = NamespaceUri.NULL
                }
            }

            return attributeInfos
        }

        private fun addResult(item: XdmItem) {
            results.first().add(item)
        }

        fun results() : List<XdmItem> {
            if (state != ResultState.FINISHED) {
                throw SAXException("endDocument() event has not yet been reached")
            }

            if (results.size != 1) {
                throw SAXException("Should only be one set of results left, missing removeFirst() somewhere")
            }

            return results.removeFirst().toList()
        }
    }
}