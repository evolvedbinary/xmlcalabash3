package com.xmlcalabash.util

import net.sf.saxon.s9api.Axis
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmNodeKind
import org.xml.sax.*
import org.xml.sax.ext.LexicalHandler
import org.xmlresolver.utils.SaxProducer
import javax.xml.transform.sax.SAXSource

object XmlToSax {
    fun asSaxSource(node: XdmNode): SAXSource {
        val input = XdmNodeInputSource(node)
        input.encoding = "UTF-8"
        input.systemId = node.baseURI?.toString()
        val source = SAXSource(input)
        source.xmlReader = XdmNodeXMLReader()
        return source
    }

    fun asSaxProducer(node: XdmNode): SaxProducer {
        return XdmNodeSaxProducer(node)
    }

    class XdmNodeInputSource(val node: XdmNode): InputSource()

    class XdmNodeXMLReader: XMLReader {
        private var dtdHandler: DTDHandler? = null
        private var contentHandler: ContentHandler? = null
        private var errHandler: ErrorHandler? = null
        private var lexHandler: LexicalHandler? = null

        override fun getFeature(name: String?): Boolean {
            return false
        }

        override fun setFeature(name: String?, value: Boolean) {
            // nop
            println("Set feature ${name} to ${value}")
        }

        override fun getProperty(name: String?): Any? {
            return null
        }

        override fun setProperty(name: String?, value: Any?) {
            if (name == "http://xml.org/sax/properties/lexical-handler") {
                lexHandler = value as LexicalHandler
                return
            }
            println("Set property ${name} to ${value}")
        }

        override fun setEntityResolver(resolver: EntityResolver?) {
            throw UnsupportedOperationException("The entity resolver is fixed")
        }

        override fun getEntityResolver(): EntityResolver? {
            return null
        }

        override fun setDTDHandler(handler: DTDHandler?) {
            dtdHandler = handler

        }

        override fun getDTDHandler(): DTDHandler? {
            return dtdHandler
        }

        override fun setContentHandler(handler: ContentHandler?) {
            contentHandler = handler
        }

        override fun getContentHandler(): ContentHandler? {
            return contentHandler
        }

        override fun setErrorHandler(handler: ErrorHandler?) {
            errHandler = handler
        }

        override fun getErrorHandler(): ErrorHandler? {
            return errHandler
        }

        override fun parse(input: InputSource?) {
            if (input !is XdmNodeInputSource) {
                throw IllegalArgumentException("Input node is not an XdmNodeInputSource")
            }

            if (contentHandler == null) {
                contentHandler == NopContentHandler()
            }

            if (lexHandler == null) {
                lexHandler == NopLexicalHandler()
            }

            parse(input.node)
        }

        override fun parse(systemId: String?) {
            throw UnsupportedOperationException("The XdmNodeXMLReader does not support parsing a systemId")
        }

        private fun parse(node: XdmNode) {
            when (node.nodeKind) {
                XdmNodeKind.DOCUMENT -> {
                    contentHandler!!.startDocument()
                    for (child in node.children()) {
                        parse(child)
                    }
                    contentHandler!!.endDocument()
                }
                XdmNodeKind.ELEMENT -> {
                    val uri = node.nodeName.namespaceUri.toString()
                    val local = node.nodeName.localName
                    val qname = node.nodeName.toString();

                    contentHandler!!.setDocumentLocator(LocalLocator(node))
                    for (ns in node.axisIterator(Axis.NAMESPACE)) {
                        contentHandler!!.startPrefixMapping(ns.nodeName?.localName ?: "", ns.stringValue)
                    }
                    contentHandler!!.startElement(uri, local, qname, LocalAttributes(node))

                    for (child in node.children()) {
                        parse(child)
                    }

                    contentHandler!!.endElement(uri, local, qname)
                    for (ns in node.axisIterator(Axis.NAMESPACE)) {
                        contentHandler!!.endPrefixMapping(ns.nodeName?.localName ?: "")
                    }
                }
                XdmNodeKind.TEXT -> {
                    val arr = node.stringValue.toCharArray()
                    contentHandler!!.setDocumentLocator(LocalLocator(node))
                    contentHandler!!.characters(arr, 0, arr.size)
                }
                XdmNodeKind.PROCESSING_INSTRUCTION -> {
                    contentHandler!!.setDocumentLocator(LocalLocator(node))
                    contentHandler!!.processingInstruction(node.nodeName.localName, node.stringValue)
                }
                XdmNodeKind.COMMENT -> {
                    val arr = node.stringValue.toCharArray()
                    contentHandler!!.setDocumentLocator(LocalLocator(node))
                    lexHandler!!.comment(arr, 0, arr.size)
                }
                XdmNodeKind.ATTRIBUTE -> { /* nop */ }
                XdmNodeKind.NAMESPACE -> { /* nop */ }
            }
        }
    }

    class XdmNodeSaxProducer(val node: XdmNode) : SaxProducer {
        override fun produce(contentHandler: ContentHandler?, dtdHandler: DTDHandler?, errorHandler: ErrorHandler?) {
            val source = asSaxSource(node)
            source.xmlReader.contentHandler = contentHandler
            source.xmlReader.errorHandler = errorHandler
            source.xmlReader.dtdHandler = dtdHandler
            source.xmlReader.parse(source.inputSource)
        }
    }

    class NopContentHandler(): ContentHandler {
        override fun setDocumentLocator(locator: Locator?) {
            // nop
        }

        override fun startDocument() {
            // nop
        }

        override fun endDocument() {
            // nop
        }

        override fun startPrefixMapping(prefix: String?, uri: String?) {
            // nop
        }

        override fun endPrefixMapping(prefix: String?) {
            // nop
        }

        override fun startElement(uri: String?, localName: String?, qName: String?, atts: Attributes?) {
            // nop
        }

        override fun endElement(uri: String?, localName: String?, qName: String?) {
            // nop
        }

        override fun characters(ch: CharArray?, start: Int, length: Int) {
            // nop
        }

        override fun ignorableWhitespace(ch: CharArray?, start: Int, length: Int) {
            // nop
        }

        override fun processingInstruction(target: String?, data: String?) {
            // nop
        }

        override fun skippedEntity(name: String?) {
            // nop
        }
    }

    class NopLexicalHandler(): LexicalHandler {
        override fun startDTD(name: String?, publicId: String?, systemId: String?) {
            // nop
        }

        override fun endDTD() {
            // nop
        }

        override fun startEntity(name: String?) {
            // nop
        }

        override fun endEntity(name: String?) {
            // nop
        }

        override fun startCDATA() {
            // nop
        }

        override fun endCDATA() {
            // nop
        }

        override fun comment(ch: CharArray?, start: Int, length: Int) {
            // nop
        }
    }

    class LocalAttributes(node: XdmNode): Attributes {
        val uriList = mutableListOf<String>()
        val nameList = mutableListOf<String>()
        val qnameList = mutableListOf<String>()
        val valueList = mutableListOf<String>()

        init {
            for (attr in node.axisIterator(Axis.ATTRIBUTE)) {
                uriList.add(attr.nodeName.namespaceUri.toString())
                nameList.add(attr.nodeName.localName)
                qnameList.add(attr.nodeName.toString())
                valueList.add(attr.stringValue)
            }
        }

        override fun getLength(): Int {
            return uriList.size
        }

        override fun getURI(index: Int): String? {
            return uriList.getOrNull(index)
        }

        override fun getLocalName(index: Int): String? {
            return nameList.getOrNull(index)
        }

        override fun getQName(index: Int): String? {
            return qnameList.getOrNull(index)
        }

        override fun getType(index: Int): String? {
            val qname = qnameList.getOrNull(index) ?: return null
            if (qname == "xml:id") {
                return "ID"
            }
            return "CDATA"
        }

        override fun getValue(index: Int): String? {
            return valueList.getOrNull(index)
        }

        override fun getIndex(uri: String?, localName: String?): Int {
            for ((index, ns) in uriList.withIndex()) {
                if (ns == uri && getLocalName(index) == localName) {
                    return index
                }
            }
            return -1
        }

        override fun getIndex(qName: String?): Int {
            for ((index, name) in qnameList.withIndex()) {
                if (name == qName) {
                    return index
                }
            }
            return -1
        }

        override fun getType(uri: String?, localName: String?): String? {
            return getType(getIndex(uri, localName))
        }

        override fun getType(qName: String?): String? {
            return getType(getIndex(qName))
        }

        override fun getValue(uri: String?, localName: String?): String? {
            return getValue(getIndex(uri, localName))
        }

        override fun getValue(qName: String?): String? {
            return getValue(getIndex(qName))
        }
    }

    class LocalLocator(node: XdmNode): Locator {
        private val systemId = node.baseURI?.toString()
        private val line = node.lineNumber
        private val column = node.columnNumber

        override fun getPublicId(): String? {
            return null
        }

        override fun getSystemId(): String? {
            return systemId
        }

        override fun getLineNumber(): Int {
            return line
        }

        override fun getColumnNumber(): Int {
            return column
        }
    }

}