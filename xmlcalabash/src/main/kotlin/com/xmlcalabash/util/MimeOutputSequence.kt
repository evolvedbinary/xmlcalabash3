package com.xmlcalabash.util

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.XmlCalabashBuildConfig
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXs
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.Serializer
import net.sf.saxon.s9api.XdmArray
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.type.StringConverter
import org.apache.commons.codec.net.QuotedPrintableCodec
import org.apache.hc.client5.http.entity.mime.ByteArrayBody
import org.apache.hc.client5.http.entity.mime.FormBodyPartBuilder
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder
import org.apache.hc.client5.http.entity.mime.StringBody
import org.apache.hc.core5.http.ContentType
import org.apache.logging.log4j.kotlin.logger
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

class MimeOutputSequence(private val xmlCalabash: XmlCalabash, val stream: OutputStream, val multiplex: Boolean = false) {
    companion object {
        fun encodeForOutput(contentType: MediaType, body: ByteArray, outputCharset: Charset): Pair<String, ByteArray> {
            val charseq = body.toString(Charset.forName("UTF-8"))
            val mimeContentType = ContentType.create(contentType.toStringWithoutParameters(), StandardCharsets.UTF_8.name())

            val encoder = outputCharset.newEncoder()
            if (encoder.canEncode(charseq)) {
                return Pair("", body)
            } else {
                var good = 0;
                var bad = 0;
                for ((index, ch) in charseq.withIndex()) {
                    if (encoder.canEncode(ch)) {
                        good++
                    } else {
                        bad++
                    }
                    if (index >= 1024) {
                        break
                    }
                }

                if (good > 0 && (good.toDouble() / (good + bad) > 0.80)) {
                    val codec = QuotedPrintableCodec(outputCharset)
                    val bytes = codec.encode(body)
                    return Pair("quoted-printable", bytes)
                } else {
                    return Pair("base64", Base64.getEncoder().encode(body))
                }
            }
        }
    }

    private val boundary = "B=_${UUID.randomUUID()}_${UUID.randomUUID()}".substring(0, 65)
    private val mp = MultipartEntityBuilder.create()
    private val outputCharset = Charset.forName(xmlCalabash.messagePrinter.encoding)
    //private val outputCharset = Charset.forName("US-ASCII")
    private var closed = false
    private var index = 1

    var pipelineUri: URI? = null
    var stepName: String? = null
    var port: String? = null

    init {
        mp.setStrictMode()
        mp.setBoundary(boundary);
        mp.setContentType(ContentType.create(MediaType.MULTIPART_MIXED.toString()))
        mp.setCharset(outputCharset)
    }

    fun close() {
        if (closed) {
            throw IllegalStateException("MimeOutputSequence is closed")
        }

        closed = true

        val entity = mp.build()

        val ps = PrintStream(stream)
        ps.print("Server: ${XmlCalabashBuildConfig.PRODUCT_NAME} version ${XmlCalabashBuildConfig.VERSION}\r\n")
        val format = SimpleDateFormat("EEE, dd LLL yyyy HH:mm:ss ZZZ");
        ps.print("Date: ${format.format(Date())}\r\n")
        if (pipelineUri != null) {
            ps.print("X-Pipeline: ${pipelineUri}\r\n")
        }
        if (stepName != null) {
            ps.print("X-Step: ${stepName}\r\n")
        }
        if (!multiplex && port != null) {
            ps.print("X-Port: ${port}\r\n")
        }
        ps.print("Content-Type: multipart/mixed; boundary=\"${boundary}\"\r\n")
        ps.print("\r\n")
        entity.writeTo(stream)
    }

    fun addDocument(port: String, document: XProcDocument) {
        if (closed) {
            throw IllegalStateException("MimeOutputSequence is closed")
        }

        if (document is XProcBinaryDocument) {
            addBinaryDocument(port, document)
            return
        }

        val contentType = document.contentType ?: MediaType.OCTET_STREAM
        if (contentType.classification() == MediaClassification.TEXT) {
            addTextDocument(port, document, document.value.underlyingValue.stringValue)
            return
        }

        if (contentType.classification() in listOf(MediaClassification.XML, MediaClassification.XHTML, MediaClassification.HTML)) {
            addMarkupDocument(port, document)
            return
        }

        // That leaves the JSON documents
        val externalSerialization = mutableMapOf<QName, XdmValue>()
        val configProps = xmlCalabash.config.serialization[contentType] ?: emptyMap()
        for ((name, value) in configProps) {
            val untypedValue = StringConverter.StringToUntypedAtomic().convert(XdmAtomicValue(value).underlyingValue)
            externalSerialization[name] = XdmAtomicValue.wrap(untypedValue)
        }
        externalSerialization[Ns.encoding] = XdmAtomicValue("UTF-8")

        val bodyStream = ByteArrayOutputStream()
        val writer = DocumentWriter(document, bodyStream, externalSerialization)
        writer.write()

        // Now we have a UTF-8 byte stream
        addUtf8Bytes(port, document, contentType, bodyStream.toByteArray())
    }

    private fun addBinaryDocument(port: String, document: XProcBinaryDocument) {
        val contentType = document.contentType ?: MediaType.OCTET_STREAM
        val charset = document.encoding

        var mimePart = getMimePart(port, document)

        val mimeContentType = ContentType.create(contentType.toStringWithoutParameters(), charset)
        mimePart = mimePart.addField("Content-Transfer-Encoding", "base64")
        mimePart.setBody(StringBody(Base64.getMimeEncoder().encodeToString(document.binaryValue), mimeContentType))

        mp.addPart(mimePart.build())
    }

    private fun addMarkupDocument(port: String, document: XProcDocument) {
        val contentType = document.contentType ?: MediaType.OCTET_STREAM
        val charset = document.encoding

        val externalSerialization = mutableMapOf<QName, XdmValue>()
        val configProps = xmlCalabash.config.serialization[contentType] ?: emptyMap()
        for ((name, value) in configProps) {
            val untypedValue = StringConverter.StringToUntypedAtomic().convert(XdmAtomicValue(value).underlyingValue)
            externalSerialization[name] = XdmAtomicValue.wrap(untypedValue)
        }
        externalSerialization[Ns.encoding] = XdmAtomicValue(outputCharset.name())

        val bodyStream = ByteArrayOutputStream()
        val writer = DocumentWriter(document, bodyStream, externalSerialization)
        writer.write()

        val mimePart = getMimePart(port, document)

        val mimeContentType = ContentType.create(contentType.toStringWithoutParameters(), outputCharset.name())
        mimePart.setBody(ByteArrayBody(bodyStream.toByteArray(), mimeContentType))

        mp.addPart(mimePart.build())
    }

    private fun addTextDocument(port: String, document: XProcDocument, text: String) {
        val contentType = document.contentType ?: MediaType.OCTET_STREAM
        addUtf8Bytes(port, document, contentType, text.toByteArray())
    }

    // TODO: refactor this method so it uses encodeForOutput
    private fun addUtf8Bytes(port: String, document: XProcDocument, contentType: MediaType, body: ByteArray) {
        val charseq = body.toString(Charset.forName("UTF-8"))
        val mimeContentType = ContentType.create(contentType.toStringWithoutParameters(), StandardCharsets.UTF_8.name())

        var mimePart = getMimePart(port, document)

        val encoder = outputCharset.newEncoder()
        if (encoder.canEncode(charseq)) {
            mimePart.setBody(ByteArrayBody(body, mimeContentType))
        } else {
            var good = 0;
            var bad = 0;
            for ((index, ch) in charseq.withIndex()) {
                if (encoder.canEncode(ch)) {
                    good++
                } else {
                    bad++
                }
                if (index >= 1024) {
                    break
                }
            }

            if (good > 0 && (good.toDouble() / (good + bad) > 0.80)) {
                mimePart = mimePart.addField("Content-Transfer-Encoding", "quoted-printable")
                val codec = QuotedPrintableCodec(outputCharset)
                val bytes = codec.encode(body)
                mimePart.setBody(ByteArrayBody(bytes, mimeContentType))
            } else {
                mimePart = mimePart.addField("Content-Transfer-Encoding", "base64")
                mimePart.setBody(StringBody(Base64.getMimeEncoder().encodeToString(body), mimeContentType))
            }
        }

        mp.addPart(mimePart.build())
    }

    private fun getMimePart(port: String, document: XProcDocument): FormBodyPartBuilder {
        var mimePart = FormBodyPartBuilder.create()
        mimePart.setName("part{$index++}")
        mimePart = mimePart.addField("Content-Disposition", "attachment")
        if (document.baseURI != null) {
            mimePart = mimePart.addField("Content-Location", document.baseURI!!.toString())
        }
        mimePart.addField("X-Port", port)

        val ts = TypeSerializer(xmlCalabash.saxonConfiguration.processor)
        for ((name, value) in document.properties.asMap()) {
            when (name) {
                Ns.baseUri, Ns.contentType, Ns.encoding -> Unit
                else -> {
                    val baos = ByteArrayOutputStream()
                    val serializer = xmlCalabash.saxonConfiguration.processor.newSerializer(baos)
                    serializer.setOutputProperty(Serializer.Property.METHOD, "json")
                    serializer.serializeXdmValue(ts.unmarshal(value))
                    val fieldValue = String(baos.toByteArray(), StandardCharsets.UTF_8)
                    mimePart = mimePart.addField("X-Document-Property", "${name.eqName}=${fieldValue}")
                }
            }
        }

        return mimePart
    }
}