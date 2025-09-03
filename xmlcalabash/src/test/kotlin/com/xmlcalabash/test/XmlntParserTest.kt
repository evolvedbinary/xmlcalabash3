package com.xmlcalabash.test

import net.sf.saxon.Configuration
import net.sf.saxon.event.ReceiverOption
import net.sf.saxon.event.ReceivingContentHandler
import net.sf.saxon.lib.SerializerFactory
import net.sf.saxon.om.NodeName
import net.sf.saxon.s9api.Processor
import net.sf.saxon.serialize.Emitter
import net.sf.saxon.serialize.XMLEmitter
import org.xml.sax.Attributes
import org.xml.sax.Locator
import java.util.*

class XmlntParserTest {
    private val processor = Processor(false)
    private val myFactory = MySerializerFactory(processor.underlyingConfiguration)

    init {
        processor.underlyingConfiguration.serializerFactory = myFactory
    }

    /*
    fun parse(stream: InputStream, baseUri: URI?): XdmNode {
        return parse(stream.readAllBytes().toString(StandardCharsets.UTF_8), baseUri)
    }

    fun parse(xml: String, baseUri: URI?): XdmNode {
        val parser = XmlToSax();
        val builder = processor.newDocumentBuilder()
        val handler = builder.newBuildingContentHandler()
        //val handler = TestContentHandler()
        parser.parse(xml, baseUri, handler as ReceivingContentHandler)
        return handler.documentNode
    }

    fun serialize(doc: XdmNode): String {
        val baos = ByteArrayOutputStream()
        val serial = processor.newSerializer(baos)
        serial.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes")
        serial.serializeNode(doc)
        return baos.toString(StandardCharsets.UTF_8)
    }

    @Test
    fun testSimple() {
        val file = File("src/test/resources/xmlnt/simple.xml")
        val stream = FileInputStream(file)
        val doc = parse(stream, file.toURI())
        val sxml = serialize(doc)
        Assertions.assertEquals("<doc a=\"testing\n        with a newline\"/>", sxml)
    }

    @Test
    fun testInternalSubset() {
        val file = File("src/test/resources/xmlnt/isubset.xml")
        val stream = FileInputStream(file)
        val doc = parse(stream, file.toURI())
        val sxml = serialize(doc)
        println(sxml)
    }

    @Test
    fun testExternalSubset() {
        val file = File("src/test/resources/xmlnt/esubset.xml")
        val stream = FileInputStream(file)
        val doc = parse(stream, file.toURI())
        val sxml = serialize(doc)
        println(sxml)
    }

    @Test
    fun testExternalParsedEntity() {
        val doc = parse("<p>testing</p><p>also testing</p>", null)
        println(doc)
    }

    @Test
    public fun xmlParserTest() {
        val parser = XmlToSax();
        val ch = TestContentHandler()
        parser.parse("<a:doc xmlns:a='http://examle.com/' regular='value' irregular='a\n" +
                "b'>test<!-- comment --><?pi data?>more text<spoon/></doc>", null, ch)
    }

    @Test
    public fun xmlInternalSubsetParserTest() {
        val parser = XmlToSax();
        val ch = TestContentHandler()
        parser.parse("<!DOCTYPE a:doc SYSTEM 'spoon.dtd' [ <!ENTITY spoon 'foo'> ]><a:doc xmlns:a='http://examle.com/' regular='value' irregular='a\n" +
                "b'>test<!-- comment --><?pi data?>more text<spoon a='-&spoon;-&lt;-&#23;'>&spoon;</spoon></doc>", null, ch)
    }
    */

    private class TestContentHandler : ReceivingContentHandler() {
        override fun setDocumentLocator(locator: Locator?) {
            TODO("Not yet implemented")
        }

        override fun startDocument() {
            println("START DOCUMENT")
        }

        override fun endDocument() {
            println("END DOCUMENT")
        }

        override fun startPrefixMapping(prefix: String?, uri: String?) {
            println("SRT MAP ${prefix} = ${uri}")
        }

        override fun endPrefixMapping(prefix: String?) {
            println("END MAP ${prefix}")
        }

        override fun startElement(uri: String?, localName: String?, qName: String?, atts: Attributes?) {
            println("START ELEMENT: ${qName}")
        }

        override fun endElement(uri: String?, localName: String?, qName: String?) {
            println("END ELEMENT: ${qName}")
        }

        override fun characters(ch: CharArray?, start: Int, length: Int) {
            println("CHARS: ${ch.toString()}")
        }

        override fun ignorableWhitespace(ch: CharArray?, start: Int, length: Int) {
            // ignored!
        }

        override fun processingInstruction(target: String?, data: String?) {
            println("PI")
        }

        override fun skippedEntity(name: String?) {
            println("SKIPPED ENTITY")
        }
    }

    private class MySerializerFactory(config: Configuration): SerializerFactory(config) {
        override fun newXMLEmitter(properties: Properties?): Emitter {
            return NonconformantXmlEmitter(properties)
        }
    }

    private class NonconformantXmlEmitter(val properties: Properties?): XMLEmitter() {
        override fun writeAttribute(elCode: NodeName, attname: String, value: String, properties: Int) {
            super.writeAttribute(elCode, attname, value, properties or ReceiverOption.DISABLE_ESCAPING)
        }
    }

}