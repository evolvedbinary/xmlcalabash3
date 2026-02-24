package com.xmlcalabash.test

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.datamodel.DocumentContextImpl
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.BasicDocumentLoader
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.io.MimeDocumentLoader
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.MimeOutputSequence
import com.xmlcalabash.util.UriUtils
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmMap
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

class MimeTest {
    fun xmlDocument(path: String): XProcDocument {
        val xmlCalabash = XmlCalabash.newInstance();
        val processor = xmlCalabash.saxonConfiguration.processor

        var map = XdmMap()
        map = map.put(XdmAtomicValue("a"), XdmAtomicValue(1))
        map = map.put(XdmAtomicValue("b"), XdmAtomicValue(2))

        val builder = processor.newDocumentBuilder()
        val xdm = builder.build(File(path))
        val props = DocumentProperties()
        props[NsCx.message] = "Testing additional property?"
        props[QName("adoc")] = xdm
        props[QName("amap")] = map
        props[QName("pi")] = XdmAtomicValue(3.1415)
        return XProcDocument.ofXml(xdm, DocumentContextImpl(xdm), props)
    }

    fun jsonDocument(path: String): XProcDocument {
        val xmlCalabash = XmlCalabash.newInstance();
        val processor = xmlCalabash.saxonConfiguration.processor

        val prop = DocumentProperties()
        prop[Ns.contentType] = MediaType.JSON
        val loader = BasicDocumentLoader(UriUtils.cwdAsUri().resolve(path),
            processor, null, prop)
        val txtstream = FileInputStream(path)
        return loader.load(txtstream)
    }

    fun textDocument(path: String, encoding: String = "UTF-8"): XProcDocument {
        val xmlCalabash = XmlCalabash.newInstance();
        val processor = xmlCalabash.saxonConfiguration.processor

        val prop = DocumentProperties()
        prop[Ns.encoding] = encoding
        prop[Ns.contentType] = MediaType.TEXT
        val loader = BasicDocumentLoader(UriUtils.cwdAsUri().resolve(path),
            processor, null, prop)
        val txtstream = FileInputStream(path)
        return loader.load(txtstream)
    }

    fun binaryDocument(path: String, contentType: MediaType): XProcDocument {
        val xmlCalabash = XmlCalabash.newInstance()
        val processor = xmlCalabash.saxonConfiguration.processor

        val prop = DocumentProperties()
        prop[Ns.contentType] = contentType
        val loader = BasicDocumentLoader(UriUtils.cwdAsUri().resolve(path),
            processor, null, prop)
        val imagestream = FileInputStream(path)
        return loader.load(imagestream)
    }

    @Test
    fun outputTestXml() {
        val xmlCalabash = XmlCalabash.newInstance()

        val baos = ByteArrayOutputStream()
        val mime = MimeOutputSequence(xmlCalabash, baos, false)

        val doc = xmlDocument("src/test/resources/dotless.xml")
        mime.addDocument("result", doc)
        mime.close()

        val loader = MimeDocumentLoader(xmlCalabash)
        val documents = loader.load(ByteArrayInputStream(baos.toByteArray()))

    }

    @Test
    fun outputTestAsciiJson() {
        val xmlCalabash = XmlCalabash.newInstance();
        val mime = MimeOutputSequence(xmlCalabash, System.out)

        val doc = jsonDocument("src/test/resources/content.json")
        mime.addDocument("result", doc)
        mime.close()
    }

    @Test
    fun outputTestUtf8Json() {
        val xmlCalabash = XmlCalabash.newInstance();
        val mime = MimeOutputSequence(xmlCalabash, System.out)

        val doc = jsonDocument("src/test/resources/dotless.json")
        mime.addDocument("result", doc)
        mime.close()
    }

    @Test
    fun outputAsciiText() {
        val xmlCalabash = XmlCalabash.newInstance();
        val mime = MimeOutputSequence(xmlCalabash, System.out)

        val doc = textDocument("src/test/resources/content.txt")
        mime.addDocument("result", doc)
        mime.close()
    }

    @Test
    fun outputBinary() {
        val xmlCalabash = XmlCalabash.newInstance();
        val mime = MimeOutputSequence(xmlCalabash, System.out)

        val imagedoc = binaryDocument("src/test/resources/dotless.jpg", MediaType.JPEG)
        mime.addDocument("result", imagedoc)

        mime.close()
    }

    @Test
    fun outputUtf8Text() {
        val xmlCalabash = XmlCalabash.newInstance();

        val baos = ByteArrayOutputStream()
        val mime = MimeOutputSequence(xmlCalabash, baos)

        val doc = textDocument("src/test/resources/dotless.txt", "ISO-8859-9")
        mime.addDocument("result", doc)
        mime.close()

        val loader = MimeDocumentLoader(xmlCalabash)
        val documents = loader.load(ByteArrayInputStream(baos.toByteArray()))
    }

    @Test
    fun allTest() {
        val xmlCalabash = XmlCalabash.newInstance();

        val baos = ByteArrayOutputStream()
        val mime = MimeOutputSequence(xmlCalabash, baos, true)

        mime.addDocument("result", xmlDocument("src/test/resources/dotless.xml"))
        mime.addDocument("result", jsonDocument("src/test/resources/content.json"))
        mime.addDocument("result", jsonDocument("src/test/resources/dotless.json"))
        mime.addDocument("result", textDocument("src/test/resources/content.txt"))
        mime.addDocument("alternate", binaryDocument("src/test/resources/dotless.jpg", MediaType.JPEG))
        mime.addDocument("alternate", textDocument("src/test/resources/dotless.txt", "ISO-8859-9"))

        mime.close()

        val loader = MimeDocumentLoader(xmlCalabash)
        val documents = loader.load(ByteArrayInputStream(baos.toByteArray()))
    }


}