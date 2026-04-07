package com.xmlcalabash.util

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.api.Receiver
import com.xmlcalabash.runtime.api.RuntimePort
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SaxonApiException
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.type.StringConverter
import java.io.ByteArrayOutputStream
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.charset.Charset
import java.util.*

open class DefaultOutputReceiver(val xmlCalabash: XmlCalabash,
                                 val outputManifold: Map<String, RuntimePort>,
                                 val ports: Set<String> = outputManifold.keys): Receiver {
    constructor(config: XProcStepConfiguration, outputManifold: Map<String, RuntimePort>): this(config.xmlCalabash, outputManifold)

    // You would think that the very nature of a default character set was that it would be...the
    // default. But no. If you don't do this trick where you create a print stream from the
    // stderr file descriptor with the right "default" encoding, then you get...UTF-8 always, maybe?
    // Hard to tell. But this seems to work.
    private val stream = PrintStream(FileOutputStream(FileDescriptor.out), true, xmlCalabash.messagePrinter.encoding)
    private val outputCharset = Charset.forName(xmlCalabash.messagePrinter.encoding)

    private var port = ""
    private var totals = mutableMapOf<String, Int>()
    private val parts = mutableListOf<MimePart>()

    open fun close() {
        // nop
    }

    override fun output(port: String, document: XProcDocument) {
        this.port = port
        totals[port] = totals.getOrDefault(port, 0) + 1
        write(port, document, totals[port]!!, -1)
    }

    fun write(port: String, document: XProcDocument, position: Int, total: Int) {
        val runtimePort = outputManifold[port]
        val decorate = runtimePort?.sequence == true || ports.size > 1

        val part = writeDocument(document)

        val header = if (total > 0) {
            "=== ${port} :: ${position}/${total} :: ${document.baseURI} ===".padEnd(72, '=')
        } else {
            "=== ${port} :: ${position} :: ${document.baseURI} ===".padEnd(72, '=')
        }

        if (decorate) {
            stream.println(header)
        }

        when (part.encode) {
            "base64" -> {
                stream.println("=== UTF-8 :: base64 ===".padEnd(header.length, '='))
                stream.print(Base64.getMimeEncoder().encodeToString(part.body))
            }
            "quoted-printable" -> {
                stream.println("=== UTF-8 :: quoted-printable ===".padEnd(header.length, '='))
                stream.print(part.body.toString(outputCharset))
            }
            else -> {
                stream.print(part.body.toString(outputCharset))
            }
        }

        if (decorate) {
            if (part.body.size > 0 && part.body[part.body.size-1].toInt() != 10) {
                stream.println()
            }
            stream.println("".padEnd(header.length, '='))
        }
    }

    private fun writeDocument(document: XProcDocument): MimePart {
        val contentType = document.contentType

        val externalSerialization = mutableMapOf<QName, XdmValue>()
        val configProps = xmlCalabash.config.serialization[contentType] ?: emptyMap()
        for ((name, value) in configProps) {
            val untypedValue = StringConverter.StringToUntypedAtomic().convert(XdmAtomicValue(value).underlyingValue)
            externalSerialization[name] = XdmAtomicValue.wrap(untypedValue)
        }

        if (document is XProcBinaryDocument) {
            return MimePart(contentType ?: MediaType.OCTET_STREAM, document.binaryValue, "base64")
        }

        var charset = outputCharset.name()
        externalSerialization[Ns.encoding] = XdmAtomicValue(charset)

        lateinit var bodybytes: ByteArray
        try {
            val bodyStream = ByteArrayOutputStream()
            val writer = DocumentWriter(document, bodyStream, externalSerialization)
            writer.write()
            bodybytes = bodyStream.toByteArray()
        } catch (ex: SaxonApiException) {
            val bodyStream = ByteArrayOutputStream()
            charset = "UTF-8"
            externalSerialization[Ns.encoding] = XdmAtomicValue(charset)
            val writer = DocumentWriter(document, bodyStream, externalSerialization)
            writer.write()
            bodybytes = bodyStream.toByteArray()
        }

        val (encoding, bytes)
            = MimeOutputSequence.encodeForOutput(contentType ?: MediaType.OCTET_STREAM, bodybytes, outputCharset)

        return MimePart(contentType ?: MediaType.OCTET_STREAM, bytes, encoding)
    }

    data class MimePart(val contentType: MediaType, val body: ByteArray, val encode: String) {
        // I'm never going to compare them...
    }
}