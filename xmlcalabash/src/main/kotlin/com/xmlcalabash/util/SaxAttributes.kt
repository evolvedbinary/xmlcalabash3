package com.xmlcalabash.util

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.Axis
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import org.xml.sax.Attributes

class SaxAttributes(): Attributes {
    val uriList = mutableListOf<String>()
    val nameList = mutableListOf<String>()
    val qnameList = mutableListOf<String>()
    val valueList = mutableListOf<String>()

    constructor(node: XdmNode): this() {
        for (attr in node.axisIterator(Axis.ATTRIBUTE)) {
            addAttribute(attr.nodeName, attr.stringValue)
        }
    }

    fun addAttribute(nsUri: String, localName: String, qname: String, value: String) {
        for ((index, uri) in uriList.withIndex()) {
            val local = nameList[index]
            if (nsUri == uri && localName == local) {
                throw IllegalArgumentException("Attribute '${qname}' has already been added")
            }
        }
        uriList.add(nsUri)
        nameList.add(localName)
        qnameList.add(qname)
        valueList.add(value)
    }

    fun addAttribute(name: QName, value: String) {
        addAttribute(name.namespaceUri.toString(), name.localName, name.toString(), value)
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