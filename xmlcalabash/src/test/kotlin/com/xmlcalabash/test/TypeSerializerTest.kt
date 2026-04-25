package com.xmlcalabash.test

import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsXs
import com.xmlcalabash.util.TypeSerializer
import net.sf.saxon.s9api.Processor
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.Serializer
import net.sf.saxon.s9api.XdmArray
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmEmptySequence
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmNodeKind
import net.sf.saxon.s9api.XdmValue
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import javax.xml.transform.sax.SAXSource

class TypeSerializerTest {
    @Test
    fun testString() {
        val processor = Processor(false)
        val ts = TypeSerializer(processor)
        val unmarshalled = ts.unmarshal(XdmAtomicValue(17))
        Assertions.assertEquals("[\"xs:integer\",\"17\"]", serialize(processor, unmarshalled))
        val orig = ts.marshal(unmarshalled)
        Assertions.assertInstanceOf(XdmAtomicValue::class.java, orig)
        orig as XdmAtomicValue
        Assertions.assertEquals(NsXs.integer, orig.primitiveTypeName)
        Assertions.assertEquals(orig.underlyingValue, XdmAtomicValue(17).underlyingValue)
    }

    @Test
    fun testSequence() {
        val processor = Processor(false)

        var sequence: XdmValue = XdmAtomicValue(1)
        sequence = sequence.append(XdmAtomicValue(false))
        sequence = sequence.append(XdmAtomicValue("Spoon!"))

        val ts = TypeSerializer(processor)
        val unmarshalled = ts.unmarshal(sequence)
        Assertions.assertEquals("[\"sequence\",[[\"xs:integer\",\"1\"],[\"xs:boolean\",\"false\"],[\"xs:string\",\"Spoon!\"]]]", serialize(processor, unmarshalled))
        val orig = ts.marshal(unmarshalled)

        Assertions.assertEquals(3, orig.size())
        val citer = sequence.iterator()
        val riter = orig.iterator()
        while (citer.hasNext()) {
            val cvalue = citer.next()
            val rvalue = riter.next()
            Assertions.assertEquals(cvalue, rvalue)
        }
    }

    @Test
    fun testEmptySequence() {
        val processor = Processor(false)
        val ts = TypeSerializer(processor)
        val unmarshalled = ts.unmarshal(XdmEmptySequence.getInstance())
        Assertions.assertEquals("[\"sequence\",[]]", serialize(processor, unmarshalled))
        val orig = ts.marshal(unmarshalled)
        Assertions.assertInstanceOf(XdmValue::class.java, orig)
        Assertions.assertEquals(0, orig.size())
    }

    @Test
    fun testQName() {
        val processor = Processor(false)
        val ts = TypeSerializer(processor)
        val unmarshalled = ts.unmarshal(XdmAtomicValue(QName(NsCx.namespace, "foo:bar")))
        Assertions.assertEquals("[\"xs:QName\",[\"http:\\/\\/xmlcalabash.com\\/ns\\/extensions\",\"foo:bar\"]]", serialize(processor, unmarshalled))
        val orig = ts.marshal(unmarshalled)
        Assertions.assertInstanceOf(XdmAtomicValue::class.java, orig)
        orig as XdmAtomicValue
        Assertions.assertEquals(NsXs.QName, orig.primitiveTypeName)
        Assertions.assertEquals(orig.underlyingValue, XdmAtomicValue(QName(NsCx.namespace, "foo:bar")).underlyingValue)
    }

    @Test
    fun testMap() {
        val processor = Processor(false)
        var map = XdmMap()
        map = map.put(XdmAtomicValue("qname"), XdmAtomicValue(QName(NsCx.namespace, "foo:bar")))
        map = map.put(XdmAtomicValue(QName(NsCx.namespace, "foo:baz")), XdmAtomicValue(17))
        val ts = TypeSerializer(processor)
        val unmarshalled = ts.unmarshal(map)
        Assertions.assertEquals("[\"map\",[[\"xs:QName\",[\"http:\\/\\/xmlcalabash.com\\/ns\\/extensions\",\"foo:baz\"]],[\"xs:integer\",\"17\"],[\"xs:string\",\"qname\"],[\"xs:QName\",[\"http:\\/\\/xmlcalabash.com\\/ns\\/extensions\",\"foo:bar\"]]]]",
            serialize(processor, unmarshalled))
        val orig = ts.marshal(unmarshalled)
        Assertions.assertInstanceOf(XdmMap::class.java, orig)
        orig as XdmMap
        Assertions.assertEquals(2, orig.mapSize())
        Assertions.assertEquals(XdmAtomicValue(QName(NsCx.namespace, "foo:bar")), orig.get(XdmAtomicValue("qname")))
        Assertions.assertEquals(XdmAtomicValue(17), orig.get(XdmAtomicValue(QName(NsCx.namespace, "foo:baz"))))
    }

    @Test
    fun testMapSequence() {
        val processor = Processor(false)

        var countdown: XdmValue = XdmAtomicValue(3)
        countdown = countdown.append(XdmAtomicValue(2))
        countdown = countdown.append(XdmAtomicValue(1))
        countdown = countdown.append(XdmAtomicValue("Go!"))

        var map = XdmMap()
        map = map.put(XdmAtomicValue("qname"), XdmAtomicValue(QName(NsCx.namespace, "foo:bar")))
        map = map.put(XdmAtomicValue(QName(NsCx.namespace, "foo:baz")), XdmAtomicValue(17))
        map = map.put(XdmAtomicValue("countdown"), countdown)
        val ts = TypeSerializer(processor)
        val unmarshalled = ts.unmarshal(map)

        // Can we rely on this order?
        val expected = ("[\"map\",[[\"xs:QName\",[\"http:\\/\\/xmlcalabash.com\\/ns\\/extensions\",\"foo:baz\"]],[\"xs:integer\",\"17\"],"
                + "[\"xs:string\",\"countdown\"],[\"sequence\",[[\"xs:integer\",\"3\"],[\"xs:integer\",\"2\"],[\"xs:integer\",\"1\"],[\"xs:string\",\"Go!\"]]],"
                + "[\"xs:string\",\"qname\"],[\"xs:QName\",[\"http:\\/\\/xmlcalabash.com\\/ns\\/extensions\",\"foo:bar\"]]]]")

        Assertions.assertEquals(expected, serialize(processor, unmarshalled))
        val orig = ts.marshal(unmarshalled)
        Assertions.assertInstanceOf(XdmMap::class.java, orig)

        orig as XdmMap
        Assertions.assertEquals(3, orig.mapSize())
        Assertions.assertEquals(XdmAtomicValue(QName(NsCx.namespace, "foo:bar")), orig.get(XdmAtomicValue("qname")))
        Assertions.assertEquals(XdmAtomicValue(17), orig.get(XdmAtomicValue(QName(NsCx.namespace, "foo:baz"))))

        val result = orig.get(XdmAtomicValue("countdown"))
        Assertions.assertEquals(4, result.size())
        val citer = countdown.iterator()
        val riter = countdown.iterator()
        while (citer.hasNext()) {
            val cvalue = citer.next()
            val rvalue = riter.next()
            Assertions.assertEquals(cvalue, rvalue)
        }
    }

    @Test
    fun testArray() {
        val processor = Processor(false)
        var values = XdmArray()
        values = values.addMember(XdmAtomicValue(QName(NsCx.namespace, "foo:bar")))
        values = values.addMember(XdmAtomicValue("string"))
        val ts = TypeSerializer(processor)
        val unmarshalled = ts.unmarshal(values)
        Assertions.assertEquals("[\"array\",[[\"xs:QName\",[\"http:\\/\\/xmlcalabash.com\\/ns\\/extensions\",\"foo:bar\"]],[\"xs:string\",\"string\"]]]",
            serialize(processor, unmarshalled))
        val orig = ts.marshal(unmarshalled)
        Assertions.assertInstanceOf(XdmArray::class.java, orig)
        orig as XdmArray
        Assertions.assertEquals(2, orig.arrayLength())
        Assertions.assertEquals(XdmAtomicValue(QName(NsCx.namespace, "foo:bar")), orig.get(0))
        Assertions.assertEquals(XdmAtomicValue("string"), orig.get(1))
    }

    @Test
    fun testNode() {
        val processor = Processor(false)

        val bais = ByteArrayInputStream("<doc><test/></doc>".toByteArray())
        val builder = processor.newDocumentBuilder()
        val node = builder.build(SAXSource(InputSource(bais)))

        val ts = TypeSerializer(processor)
        val unmarshalled = ts.unmarshal(node)
        Assertions.assertEquals("[\"xml\",\"<doc><test\\/><\\/doc>\"]",
            serialize(processor, unmarshalled))
        val orig = ts.marshal(unmarshalled)
        Assertions.assertInstanceOf(XdmNode::class.java, orig)
        orig as XdmNode
        Assertions.assertEquals(XdmNodeKind.DOCUMENT, orig.getNodeKind())

        val baos = ByteArrayOutputStream()
        val serializer = processor.newSerializer(baos)
        serializer.setOutputProperty(Serializer.Property.METHOD, "xml")
        serializer.setOutputProperty(Serializer.Property.INDENT, "no")
        serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes")
        serializer.serializeXdmValue(orig)
        val serialXml = String(baos.toByteArray(), StandardCharsets.UTF_8)
        Assertions.assertEquals("<doc><test/></doc>", serialXml)
    }

    private fun serialize(processor: Processor, value: XdmValue): String {
        val baos = ByteArrayOutputStream()
        val serializer = processor.newSerializer(baos)
        serializer.setOutputProperty(Serializer.Property.METHOD, "json")
        serializer.serializeXdmValue(value)
        return String(baos.toByteArray(), StandardCharsets.UTF_8)
    }
}