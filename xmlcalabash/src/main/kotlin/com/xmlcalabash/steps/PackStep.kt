package com.xmlcalabash.steps

import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXml
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmEmptySequence
import java.net.URI

open class PackStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()
        val source = mutableListOf<XProcDocument>()
        source.addAll(queues["source"]!!)
        val alternate = mutableListOf<XProcDocument>()
        alternate.addAll(queues["alternate"]!!)
        val wrapperName = qnameBinding(Ns.wrapper)!!

        var baseUri: URI? = null
        val attributeSet = mutableMapOf<QName,String?>()
        val attrMap = qnameMapBinding(Ns.attributes)
        for ((key, value) in attrMap) {
            forbidNamespaceAttribute(key)
            attributeSet[key] = (value as XdmAtomicValue).underlyingValue.stringValue
            if (key == NsXml.base) {
                baseUri = URI(value.underlyingValue.stringValue)
            }
        }

        while (source.isNotEmpty() || alternate.isNotEmpty()) {
            val builder = SaxonTreeBuilder(stepConfig)
            builder.startDocument(baseUri)
            builder.addStartElement(wrapperName, stepConfig.typeUtils.attributeMap(attributeSet))
            if (source.isNotEmpty()) {
                builder.addSubtree(source.removeFirst().value)
            }
            if (alternate.isNotEmpty()) {
                builder.addSubtree(alternate.removeFirst().value)
            }
            builder.addEndElement()
            builder.endDocument()

            val properties = if (baseUri == null) {
                DocumentProperties(mapOf(Ns.baseUri to XdmEmptySequence.getInstance()))
            } else {
                DocumentProperties(mapOf(Ns.baseUri to XdmAtomicValue(baseUri)))
            }
            properties[Ns.contentType] = MediaType.XML

            val result = builder.result
            receiver.output("result", XProcDocument.ofXml(result, stepConfig, properties))
        }
    }

    override fun toString(): String = "p:pack"


}