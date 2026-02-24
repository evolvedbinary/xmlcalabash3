package com.xmlcalabash.io

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXs
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.type.StringConverter
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI
import java.net.URLDecoder
import java.util.*
import javax.mail.Session
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

class MimeDocumentLoader(xmlCalabash: XmlCalabash) {
    companion object {
        private val _a = QName("a")
    }

    private val processor = xmlCalabash.saxonConfiguration.processor

    fun loadMultiplexed(stream: InputStream, rename: Map<String,String>): Map<String,List<XProcDocument>> {
        val map = mutableMapOf<String,List<XProcDocument>>()
        load(stream, map, rename)
        return map
    }

    fun load(stream: InputStream): List<XProcDocument> {
        return load(stream, null, emptyMap())
    }

    private fun load(stream: InputStream, map: MutableMap<String,List<XProcDocument>>?, rename: Map<String,String>): List<XProcDocument> {
        val props = Properties()
        val session = Session.getInstance(props, null)
        val message = MimeMessage(session, stream)

        if (!message.isMimeType("multipart/mixed")) {
            throw IllegalArgumentException("Mime content isn't multipart/mixed: ${message.contentType}")
        }

        val documents = mutableListOf<XProcDocument>()
        val multipart = message.getContent() as MimeMultipart
        for (index in 0 until multipart.count) {
            var baseUri: URI? = null
            val properties = DocumentProperties()
            var contentType: MediaType? = null
            var port: String? = null

            val body = multipart.getBodyPart(index)
            for (header in body.allHeaders) {
                if (header.name.lowercase() == "content-disposition") {
                    if (header.value != "attachment") {
                        throw IllegalArgumentException("Content disposition isn't \"attachment\": ${header.value}")
                    }
                }
                if (header.name.lowercase() == "content-location") {
                    baseUri = URI(header.value)
                    properties[Ns.baseUri] = baseUri
                }
                if (header.name.lowercase() == "content-type") {
                    contentType = MediaType.parse(header.value)
                    properties[Ns.contentType] = header.value
                }
                if (header.name.lowercase() == "x-document-property") {
                    parseProperty(properties, header.value)
                }
                if (header.name.lowercase() == "x-port") {
                    port = rename[header.value] ?: header.value
                }
            }

            if (contentType == null) {
                throw IllegalArgumentException("Part does not have a content type")
            }

            val loader = BasicDocumentLoader(baseUri, processor, null, properties)
            val doc = loader.load(body.inputStream)

            if (map == null) {
                documents.add(doc)
            } else {
                if (port == null) {
                    throw IllegalArgumentException("Part does not have an X-Port header")
                }
                val list = mutableListOf<XProcDocument>()
                list.addAll(map[port] ?: emptyList())
                list.add(doc)
                map[port] = list
            }
        }

        return documents
    }

    val sprop = "^([^{}?]+)(\\?([a-z]+))?=(.*)$".toRegex()
    val cprop = "^Q\\{([^}]+)}([^?=]+)(\\?([a-z]+))?=(.*)$".toRegex()
    private fun parseProperty(properties: DocumentProperties, content: String) {
        lateinit var name: QName
        lateinit var type: String
        lateinit var value: String

        val cmatch = cprop.matchEntire(content)
        if (cmatch == null) {
            val smatch = sprop.matchEntire(content)
            if (smatch == null) {
                throw IllegalArgumentException("Unparsable document property: ${content}")
            }
            name = QName(smatch.groups[1]!!.value)
            type = smatch.groups[3]?.value ?: "string"
            value = URLDecoder.decode(smatch.groups[4]!!.value, "UTF-8")
        } else {
            val uri = cmatch.groups[1]!!.value
            val local = cmatch.groups[2]!!.value
            name = QName(NamespaceUri.of(uri), local)
            type = cmatch.groups[4]?.value ?: "string"
            value = URLDecoder.decode(cmatch.groups[5]!!.value, "UTF-8")
        }

        when (type) {
            "string" -> {
                properties[name] = XdmAtomicValue(value)
            }
            "xml" -> {
                val loader = BasicDocumentLoader(null, processor, null, properties)
                val bais = ByteArrayInputStream(value.toByteArray())
                val doc = loader.load(bais, MediaType.XML)
                properties[name] = doc.value
            }
            "json" -> {
                val loader = BasicDocumentLoader(null, processor, null, properties)
                val bais = ByteArrayInputStream(value.toByteArray())
                val doc = loader.load(bais, MediaType.JSON)
                properties[name] = doc.value
            }
            else -> {
                val compiler = processor.newXPathCompiler()
                compiler.declareVariable(_a)
                compiler.declareNamespace("xs", NsXs.namespace.toString())
                val exec = compiler.compile("\$a cast as xs:${type}")
                val selector = exec.load()
                val untyped = StringConverter.StringToUntypedAtomic().convert(XdmAtomicValue(value).underlyingValue)
                selector.setVariable(_a, XdmAtomicValue(untyped))
                val result = selector.evaluate()
                properties[name] = result
            }
        }
    }
}