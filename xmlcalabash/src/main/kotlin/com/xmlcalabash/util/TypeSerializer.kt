package com.xmlcalabash.util

import com.xmlcalabash.io.BasicDocumentLoader
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXs
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import net.sf.saxon.value.StringValue
import org.apache.logging.log4j.kotlin.logger
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import javax.xml.transform.sax.SAXSource

class TypeSerializer(val processor: Processor) {
    fun unmarshal(value: XdmValue): XdmValue {
        var values = XdmArray()

        if (value.size() != 1) {
            return unmarshallSequence(value)
        }

        when (value) {
            is XdmNode -> {
                values = values.addMember(XdmAtomicValue("xml"))
                val baos = ByteArrayOutputStream()
                val serializer = processor.newSerializer(baos)
                serializer.setOutputProperty(Serializer.Property.METHOD, "xml")
                serializer.setOutputProperty(Serializer.Property.INDENT, "no")
                serializer.setOutputProperty(Serializer.Property.ENCODING, "UTF-8")
                serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes")
                serializer.serializeXdmValue(value)
                values = values.addMember(XdmAtomicValue(String(baos.toByteArray(), StandardCharsets.UTF_8)))
            }
            is XdmMap -> {
                values = values.addMember(XdmAtomicValue("map"))
                var content = XdmArray()
                for ((mkey, mvalue) in TypeUtils.asGenericMap(value)) {
                    content = content.addMember(unmarshal(mkey))
                    content = content.addMember(unmarshal(mvalue))
                }
                values = values.addMember(content)
            }
            is XdmArray -> {
                values = values.addMember(XdmAtomicValue("array"))
                var content = XdmArray()
                for (avalue in value.asList()) {
                    content = content.addMember(unmarshal(avalue))
                }
                values = values.addMember(content)
            }
            is XdmAtomicValue -> {
                values = values.addMember(XdmAtomicValue("xs:${value.primitiveTypeName.localName}"))
                var content = XdmArray()
                if (value.primitiveTypeName == NsXs.QName) {
                    content = content.addMember(XdmAtomicValue(value.qNameValue.namespaceUri.toString()))
                    content = content.addMember(XdmAtomicValue(value.underlyingValue.stringValue))
                    values = values.addMember(content)
                } else {
                    values = values.addMember(XdmAtomicValue(value.underlyingValue.stringValue))
                }
            }
            else -> {
                logger.warn { "Cannot unmarshal ${value}" }
            }
        }

        return values
    }

    private fun unmarshallSequence(value: XdmValue): XdmValue {
        var values = XdmArray()
        values = values.addMember(XdmAtomicValue("sequence"))
        var content = XdmArray()
        val iter = value.iterator()
        while (iter.hasNext()) {
            val seqvalue = iter.next()
            content = content.addMember(unmarshal(seqvalue))
        }
        values = values.addMember(content)
        return values
    }

    fun marshal(value: String): XdmValue {
        val loader = BasicDocumentLoader(null, processor)
        val bais = ByteArrayInputStream(value.toByteArray())
        val doc = loader.load(bais, MediaType.JSON)
        return marshal(doc.value)
    }

    fun marshal(value: XdmValue): XdmValue {
        if (value !is XdmArray) {
            throw IllegalArgumentException("Attempt to marshal a value that isn't an array")
        }
        if (value.arrayLength() != 2) {
            throw IllegalArgumentException("Attempt to marshal a value that isn't a tuple")
        }

        val type = value.get(0)
        if (type.underlyingValue !is StringValue) {
            throw IllegalArgumentException("Attempt to marshal a value with an invalid type")
        }

        val ovalue = value.get(1)

        when (type.underlyingValue.stringValue) {
            "xml" -> {
                if (ovalue.underlyingValue !is StringValue) {
                    throw IllegalArgumentException("Attempt to marshal an incorrectly encoded XML value")
                }
                val bais = ByteArrayInputStream(ovalue.underlyingValue.stringValue.toByteArray(StandardCharsets.UTF_8))
                val builder = processor.newDocumentBuilder()
                return builder.build(SAXSource(InputSource(bais)))
            }
            "map" -> {
                if (ovalue !is XdmArray || (ovalue.arrayLength() % 2 != 0)) {
                    throw IllegalArgumentException("Attempt to marshal an incorrectly encoded map value")
                }
                var map = XdmMap()
                var kindex = 0
                while (kindex < ovalue.arrayLength()) {
                    val mkey = marshal(ovalue.get(kindex))
                    val mvalue = marshal(ovalue.get(kindex+1))
                    map = map.put(mkey as XdmAtomicValue, mvalue)
                    kindex += 2
                }
                return map
            }
            "array" -> {
                if (ovalue !is XdmArray) {
                    throw IllegalArgumentException("Attempt to marshal an incorrectly encoded array value")
                }
                var array = XdmArray()
                for (index in 0 until value.arrayLength()) {
                    val avalue = marshal(ovalue.get(index))
                    array = array.addMember(avalue)
                }
                return array
            }
            "sequence" -> {
                if (ovalue !is XdmArray) {
                    throw IllegalArgumentException("Attempt to marshal an incorrectly encoded sequence value")
                }
                var sequence: XdmValue = XdmEmptySequence.getInstance()
                for (index in 0 until ovalue.arrayLength()) {
                    sequence = sequence.append(marshal(ovalue.get(index)))
                }
                return sequence
            }
            "xs:QName" -> {
                if (ovalue !is XdmArray || ovalue.arrayLength() != 2) {
                    throw IllegalArgumentException("Attempt to marshal a incorrectly encoded QName value")
                }
                val uri = ovalue.get(0)
                if (uri.underlyingValue !is StringValue) {
                    throw IllegalArgumentException("Attempt to marshal an incorrectly encoded QName URI value")
                }

                val lex = ovalue.get(1)
                if (lex.underlyingValue !is StringValue) {
                    throw IllegalArgumentException("Attempt to marshal an incorrectly encoded QName lexical value")
                }

                return XdmAtomicValue(QName(NamespaceUri.of(uri.underlyingValue.stringValue), lex.underlyingValue.stringValue))
            }
            else -> {
                // This is cheap and cheerful, but probably not especially efficient.
                if (ovalue.underlyingValue !is StringValue) {
                    throw IllegalArgumentException("Attempt to marshal an incorrectly encoded atomic value")
                }
                val compiler = processor.newXPathCompiler()
                compiler.declareNamespace("xs", NsXs.namespace.toString())
                val constructor = "${type.underlyingValue}(\"${ovalue.underlyingValue.stringValue}\")"
                val exec = compiler.compile(constructor)
                val select = exec.load()
                return select.evaluate()
            }
        }
    }
}