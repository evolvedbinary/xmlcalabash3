package com.xmlcalabash.io

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.parsers.Xmlnt
import com.xmlcalabash.spi.ContentTypeLoader
import com.xmlcalabash.spi.ContentTypeLoaderServiceProvider
import com.xmlcalabash.tracing.TraceListener
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.UriUtils
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SaxonApiException
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmValue
import java.io.*
import java.net.URI
import java.nio.charset.Charset
import java.time.ZoneOffset
import java.util.*
import kotlin.math.min

class DocumentLoader(val stepConfig: StepConfiguration,
                     val href: URI?,
                     val documentProperties: DocumentProperties = DocumentProperties(),
                     val parameters: Map<QName,XdmValue> = mapOf(),
                     val originalURI: URI? = null) {
    companion object {
        val cx_can_read = QName(NsCx.namespace, "cx:can-read")
        val cx_can_write = QName(NsCx.namespace, "cx:can-write")
        val cx_can_execute = QName(NsCx.namespace, "cx:can-execute")
        val cx_is_hidden = QName(NsCx.namespace, "cx:is-hidden")
        val cx_last_modified = QName(NsCx.namespace, "cx:last-modified")
        val cx_size = QName(NsCx.namespace, "cx:size")
        private var contentTypeLoaders: List<ContentTypeLoader>? = null

        fun textDeclaration(bytes: ByteArray): String? {
            if (bytes[0].toInt() == '<'.code && bytes[1].toInt() == '?'.code) {
                val sb = StringBuilder()
                var pos = 0;
                var pch: Char = bytes[0].toInt().toChar()
                while (pos < min(bytes.size, 1024)) {
                    val ch = bytes[pos++].toInt().toChar()
                    sb.append(ch)
                    if (pch == '?' && ch == '>') {
                        return sb.toString()
                    }
                    pch = ch
                }
            }
            return null
        }
    }

    var mediaType = MediaType.XML
    val properties: DocumentProperties = DocumentProperties()
    var absURI: URI = UriUtils.cwdAsUri()

    fun load(): XProcDocument {
        if (href == null) {
            throw stepConfig.exception(XProcError.xiImpossible("Attempt to load document with no URI"))
        }

        absURI = if (href.isAbsolute) {
            href
        } else {
            if (stepConfig.baseUri != null) {
                stepConfig.baseUri!!.resolve(href)
            } else {
                UriUtils.cwdAsUri().resolve(href)
            }
        }

        if (absURI.scheme == "file") {
            try {
                return loadFile()
            } catch (ex: Exception) {
                when (ex) {
                    is FileNotFoundException -> throw stepConfig.exception(XProcError.xdDoesNotExist(UriUtils.path(absURI), ex.message ?: "???"), ex)
                    is IOException -> throw stepConfig.exception(XProcError.xdIsNotReadable(UriUtils.path(absURI), ex.message ?: "???"), ex)
                    else -> throw ex
                }
            }
        }

        if (absURI.scheme == "http" || absURI.scheme == "https") {
            val start = System.nanoTime()
            val req = InternetProtocolRequest(stepConfig, absURI)
            req.overrideContentType = documentProperties.contentType
            val resp = req.execute("GET")

            if (resp.statusCode >= 500) {
                throw stepConfig.exception(
                    XProcError.xdIsNotReadable(
                        absURI.toString(),
                        "HTTP response code: ${resp.statusCode}"
                    )
                )
            }
            if (resp.statusCode >= 400) {
                throw stepConfig.exception(
                    XProcError.xdDoesNotExist(
                        absURI.toString(),
                        "HTTP response code: ${resp.statusCode}"
                    )
                )
            }
            if (resp.response.size != 1) {
                throw stepConfig.exception(XProcError.xiDocumentReturnedMultipart())
            }
            val end = System.nanoTime()

            for (monitor in stepConfig.environment.monitors) {
                if (monitor is TraceListener) {
                    monitor.getResource(end - start, resp.responseUri, originalURI ?: absURI, false, false)
                }
            }

            return resp.response.first()
        }

        throw stepConfig.exception(XProcError.xdIsNotReadable(href.toString(), "Unsupported scheme."))
    }

    private fun loadFile(): XProcDocument {
        if (absURI.host != null) {
            throw stepConfig.exception(XProcError.xiFileOnOtherHost(absURI, absURI.host));
        }

        val start = System.nanoTime()
        val file = File(UriUtils.path(absURI))

        mediaType = if (documentProperties.has(Ns.contentType)) {
            MediaType.parse(documentProperties[Ns.contentType]!!.underlyingValue.stringValue)
        } else {
            val fileMediaType = stepConfig.documentManager.mimetypesFileTypeMap.getContentType(absURI.toString())
            MediaType.parse(fileMediaType)
        }

        properties.setAll(documentProperties)
        properties[Ns.contentType] = mediaType

        if (!properties.has(Ns.baseUri)) {
            properties[Ns.baseUri] = file.toURI()
        }

        properties[cx_can_read] = file.canRead()
        properties[cx_can_write] = file.canWrite()
        properties[cx_can_execute] = file.canExecute()
        properties[cx_is_hidden] = file.isHidden
        properties[cx_size] = XdmAtomicValue(file.length())

        val lmDate = Date(file.lastModified())
        properties[cx_last_modified] = XdmAtomicValue(lmDate.toInstant().atZone(ZoneOffset.UTC))

        // Because of the slightly hacky dependency on BasicDocumentLoader, make sure
        // that line numbering is reflected in the parameters...
        val params = mutableMapOf<QName, XdmValue>()
        params.putAll(parameters)
        if (stepConfig.xmlCalabashConfig.lineNumbering && NsCx.lineNumbering !in parameters) {
            params[NsCx.lineNumbering] = XdmAtomicValue(true)
        }

        try {
            val basicLoader = BasicDocumentLoader(absURI, stepConfig.processor, stepConfig.documentManager, properties, params)
            val stream = FileInputStream(file)
            val doc = basicLoader.load(stream, mediaType)
            stream.close()
            return doc
        } finally {
            val end = System.nanoTime()
            for (monitor in stepConfig.environment.monitors) {
                if (monitor is TraceListener) {
                    monitor.getResource(end - start, absURI, originalURI, false, false)
                }
            }
        }
    }

    fun load(stream: InputStream, mediaType: MediaType, charset: Charset? = null): XProcDocument {
        return load(href, stream, mediaType, charset)
    }

    private fun load(uri: URI?, stream: InputStream, overrideMediaType: MediaType, charset: Charset? = null): XProcDocument {
        // If we got here via the InlineInstruction, we may not have started with load(), so we
        // need to make sure that the absURI (which will be used for the base URI of the document)
        // is correct.
        if (uri != null) {
            absURI = if (uri.isAbsolute) {
                uri
            } else {
                if (stepConfig.baseUri != null) {
                    stepConfig.baseUri!!.resolve(uri)
                } else {
                    UriUtils.cwdAsUri().resolve(uri)
                }
            }
        }

        if (contentTypeLoaders == null) {
            val list = mutableListOf<ContentTypeLoader>()
            list.add(RdfLoader())
            for (provider in ContentTypeLoaderServiceProvider.providers()) {
                list.add(provider.create())
            }
            contentTypeLoaders = list
        }

        for (loader in contentTypeLoaders!!) {
            if (overrideMediaType in loader.contentTypes()) {
                return loader.load(stepConfig, uri, stream, overrideMediaType, mediaType.charset() ?: charset)
            }
        }

        // Is this supposed to be loaded with xmlnt?
        val xmlnt: String? = if (parameters.containsKey(NsCx.xmlnt)) {
            val value = parameters[NsCx.xmlnt]!!
            when (value.underlyingValue.stringValue) {
                "true" -> "attributes"
                "entities" -> "entities"
                "attributes" -> "attributes"
                else -> {
                    throw stepConfig.exception(XProcError.xdStepFailed("Can't specify parameter cx:xmlnt=${value.underlyingValue.stringValue}}"))
                }
            }
        } else {
            null
        }

        mediaType = overrideMediaType
        properties.setAll(documentProperties)
        properties[Ns.contentType] = mediaType
        if (href != null && !properties.has(Ns.baseUri)) {
            properties[Ns.baseUri] = href
        }

        val classification = mediaType.classification()

        if ((classification != MediaClassification.XML && classification != MediaClassification.XHTML) && xmlnt != null) {
            throw stepConfig.exception(XProcError.xdStepFailed("Can't specify cx:xmlnt parser for non-XML resources"))
        }

        // Because of the slightly hacky dependency on BasicDocumentLoader, make sure
        // that line numbering is reflected in the parameters...
        val params = mutableMapOf<QName, XdmValue>()
        params.putAll(parameters)
        if (stepConfig.xmlCalabashConfig.lineNumbering && NsCx.lineNumbering !in parameters) {
            params[NsCx.lineNumbering] = XdmAtomicValue(true)
        }

        try {
             return when (classification) {
                 MediaClassification.XML, MediaClassification.XHTML -> {
                     if (xmlnt == null) {
                         val loader = BasicDocumentLoader(uri, stepConfig.processor, stepConfig.documentManager, properties, params)
                         loader.load(stream, mediaType, mediaType.charset())
                     } else {
                         loadXmlnt(uri,  xmlnt == "entities", stream)
                     }
                 }
                 else -> {
                     val loader = BasicDocumentLoader(absURI, stepConfig.processor, stepConfig.documentManager, properties, params)
                     loader.load(stream, mediaType, charset)
                 }
            }
        } catch (ex: SaxonApiException) {
            if (href != null) {
                throw stepConfig.exception(XProcError.xdNotWellFormed(href), ex)
            }
            throw stepConfig.exception(XProcError.xdNotWellFormed(), ex)
        } catch (ex: XProcException) {
            throw stepConfig.exception(ex.error, ex.cause)
        }
    }

    private fun loadXmlnt(uri: URI?, preserveEntities: Boolean, stream: InputStream): XProcDocument {
        val startChar = if (parameters.containsKey(NsCx.xmlntStartchar)) {
            parameters.getValue(NsCx.xmlntStartchar).underlyingValue.stringValue
        } else {
            "\uE000"
        }

        if (startChar.length > 1) {
            throw stepConfig.exception(XProcError.xdStepFailed("Xmlnt start character must be a single character: ${startChar}"))
        }

        val parser = Xmlnt(stepConfig, preserveEntities, startChar[0])

        var bytes = stream.readAllBytes()
        val textdecl = textDeclaration(bytes)

        val charset = if (textdecl != null && textdecl.startsWith("<?xml")) {
            val encoding = "\\sencoding\\s*=\\s*[\"']([^\"']+)[\"']".toRegex()
            val match = encoding.find(textdecl)

            bytes = bytes.sliceArray(textdecl.length until bytes.size)

            if (match != null) {
                match.groupValues[1]
            } else {
                "UTF-8"
            }
        } else {
            "UTF-8"
        }

        val xml = bytes.toString(Charset.forName(charset))
        return parser.parse(xml, uri)
    }
}