package com.xmlcalabash.io

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.datamodel.InstructionConfiguration
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXs
import com.xmlcalabash.util.Report
import com.xmlcalabash.util.TypeSerializer
import com.xmlcalabash.util.Verbosity
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

class MimeDocumentLoader(private val xmlCalabash: XmlCalabash, private val stepConfig: InstructionConfiguration? = null) {
    constructor(stepConfig: InstructionConfiguration): this(stepConfig.xmlCalabash, stepConfig)

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

        var ignoreProperties = false
        for (header in message.allHeaders) {
            if (header.name.lowercase() == "server") {
                // Server: XML Calabash version 3.0.44
                val value = header.value.lowercase()
                if (value.startsWith("xml calabash version 3.0")) {
                    var version = value.substring(25).trim()
                    if (version.contains("-")) {
                        version = version.substring(0, version.indexOf("-"))
                    }
                    try {
                        val num = version.toInt()
                        if (num < 45) {
                            ignoreProperties = true
                            xmlCalabash.config.messageReporter.report(Verbosity.WARN) {
                                Report(Verbosity.WARN, { "Document properties ignored on multiplexed input (format changed in version 3.0.45)" })
                            }
                        }
                    } catch (ex: NumberFormatException) {
                        // nop
                    }
                }
            }
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
                if (!ignoreProperties && header.name.lowercase() == "x-document-property") {
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
                // This is multipart/mixed input
                documents.add(doc)
            } else {
                if (port == null) {
                    throw IllegalArgumentException("Part does not have an X-Port header")
                }
                stepConfig?.debug { "Multiplexed input for ${port}: ${baseUri}" }
                val list = mutableListOf<XProcDocument>()
                list.addAll(map[port] ?: emptyList())
                list.add(doc)
                map[port] = list
            }
        }

        return documents
    }

    val ts = TypeSerializer(xmlCalabash.saxonConfiguration.processor)
    val sprop = "^([^{}=]+)=(.*)$".toRegex()
    val cprop = "^Q\\{([^}]+)}([^=]+)=(.*)$".toRegex()
    private fun parseProperty(properties: DocumentProperties, content: String) {
        lateinit var name: QName
        lateinit var value: String

        val cmatch = cprop.matchEntire(content)
        if (cmatch == null) {
            val smatch = sprop.matchEntire(content)
                ?: throw IllegalArgumentException("Unparsable document property: ${content}")
            name = QName(smatch.groups[1]!!.value)
            value = URLDecoder.decode(smatch.groups[2]!!.value, "UTF-8")
        } else {
            val uri = cmatch.groups[1]!!.value
            val local = cmatch.groups[2]!!.value
            name = QName(NamespaceUri.of(uri), local)
            value = URLDecoder.decode(cmatch.groups[3]!!.value, "UTF-8")
        }

        properties[name] = ts.marshal(value)
    }
}