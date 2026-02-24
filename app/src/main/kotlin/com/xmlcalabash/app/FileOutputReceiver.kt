package com.xmlcalabash.app

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.config.XmlCalabashOutput
import com.xmlcalabash.config.XmlCalabashTempOutput
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.XProcPipeline
import com.xmlcalabash.util.DefaultOutputReceiver
import com.xmlcalabash.util.MimeOutputSequence
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.*
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.Serializer
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.type.BuiltInAtomicType
import net.sf.saxon.type.StringConverter
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.io.FileOutputStream
import java.net.URI

class FileOutputReceiver(xmlCalabash: XmlCalabash,
                         pipeline: XProcPipeline,
                         val files: Map<String, XmlCalabashOutput>,
                         val manifest: XmlCalabashOutput?
): DefaultOutputReceiver(xmlCalabash, pipeline.outputManifold, pipeline.outputManifold.keys - files.keys) {
    companion object {
        val fqHref = FingerprintedQName("", NamespaceUri.NULL, "href")
        val fqContentType = FingerprintedQName("", NamespaceUri.NULL, "content-type")
        val fqName = FingerprintedQName("", NamespaceUri.NULL, "name")
        val fqPorts = FingerprintedQName("", NamespaceUri.NULL, "ports")
    }

    private val wroteTo = mutableMapOf<String, URI>()
    private val outputMime = mutableMapOf<String, MimeOutputSequence>()
    private var multiplexOutput: MimeOutputSequence? = null
    private val multiplexPorts = mutableListOf<String>()
    private val pipelineUri = pipeline.config.baseUri
    private val stepName = pipeline.stepName
    private val manifestMap = ManifestMap()

    init {
        for ((port, output) in files) {
            if (output.multiplex) {
                multiplexPorts.add(port)
            }
        }
    }

    override fun close() {
        super.close()
        for ((_, mime) in outputMime) {
            mime.close()
            mime.stream.close()
        }

        if (manifest != null) {
            val builder = SaxonTreeBuilder(xmlCalabash.saxonConfiguration.processor)
            builder.startDocument(null)
            builder.addStartElement(NsCx.manifest)

            lateinit var amap: AttributeMap
            for ((port, uris) in manifestMap.map) {
                amap = EmptyAttributeMap.getInstance()
                amap = amap.put(AttributeInfo(fqName, BuiltInAtomicType.UNTYPED_ATOMIC, port, null, 0))

                val outputName = if (files[port]?.multipartMixed == true) {
                    amap = amap.put(AttributeInfo(fqContentType, BuiltInAtomicType.UNTYPED_ATOMIC, MediaType.MULTIPART_MIXED.toString(), null, 0))
                    if (uris.first().second != CommandLine.STDIO_URI) {
                        amap = amap.put(AttributeInfo(fqHref, BuiltInAtomicType.UNTYPED_ATOMIC, uris.first().second.toString(), null, 0))
                    }
                    NsCx.part
                } else {
                    NsCx.output
                }

                builder.addStartElement(NsCx.port, amap)
                for (details in uris) {
                    amap = EmptyAttributeMap.getInstance()
                    if (details.first != null) {
                        amap = amap.put(AttributeInfo(fqContentType, BuiltInAtomicType.UNTYPED_ATOMIC, details.first.toString(), null, 0))
                    }
                    if (outputName != NsCx.part && details.second != CommandLine.STDIO_URI) {
                        amap = amap.put(AttributeInfo(fqHref, BuiltInAtomicType.UNTYPED_ATOMIC, details.second.toString(), null, 0))
                    }
                    builder.addStartElement(outputName, amap)
                    builder.addEndElement()
                }
                builder.addEndElement()
            }

            if (manifestMap.multiplexedPorts.isNotEmpty()) {
                amap = EmptyAttributeMap.getInstance()
                amap = amap.put(AttributeInfo(fqPorts, BuiltInAtomicType.UNTYPED_ATOMIC, multiplexPorts.joinToString(" "), null, 0))
                if (manifestMap.multiplexedOutput != CommandLine.STDIO_URI) {
                    amap = amap.put(AttributeInfo(fqHref, BuiltInAtomicType.UNTYPED_ATOMIC, manifestMap.multiplexedOutput.toString(), null, 0))
                }
                builder.addStartElement(NsCx.multiplex, amap)
                for (details in manifestMap.multiplexedPorts) {
                    amap = EmptyAttributeMap.getInstance()
                    amap = amap.put(AttributeInfo(fqName, BuiltInAtomicType.UNTYPED_ATOMIC, details.first, null, 0))
                    if (details.second != null) {
                        amap = amap.put(AttributeInfo(fqContentType, BuiltInAtomicType.UNTYPED_ATOMIC, details.second.toString(), null, 0))
                    }
                    builder.addStartElement(NsCx.part, amap)
                    builder.addEndElement()
                }
                builder.addEndElement()
            }
            builder.addEndElement()
            builder.endDocument()

            val fos = if (manifest.pattern == CommandLine.STDIO_NAME) {
                System.out
            } else {
                val outfile = File(manifest.pattern)
                outfile.parentFile.mkdirs()
                FileOutputStream(outfile)
            }

            val serializer = xmlCalabash.saxonConfiguration.processor.newSerializer(fos)
            serializer.setOutputProperty(Serializer.Property.METHOD, "xml")
            serializer.setOutputProperty(Serializer.Property.INDENT, "true")
            serializer.serializeNode(builder.result)

            if (fos != System.out) {
                fos.close()
            }
        }
    }

    override fun output(port: String, document: XProcDocument) {
        val output = files[port]!!

        if (output.multipartMixed) {
            if (port in outputMime) {
                manifestMap.add(port, output, document, wroteTo[port]!!)
                outputMime[port]!!.addDocument(port, document)
                return
            }
            if (multiplexOutput != null && port in multiplexPorts) {
                manifestMap.add(port, output, document)
                multiplexOutput!!.addDocument(port, document)
                return
            }
        }

        if (output.pattern == CommandLine.STDIO_NAME && !xmlCalabash.config.pipe) {
            super.output(port, document)
            return
        }

        val fos = if (output.pattern == CommandLine.STDIO_NAME) {
            manifestMap.add(port, output, document, CommandLine.STDIO_URI)
            logger.debug { "Writing ${port} to stdout" }
            System.out
        } else {
            val outfile = if (output is XmlCalabashTempOutput) {
                output.nextFile(port, document.contentType, xmlCalabash.config.saxonConfiguration.environment.contentTypes)
            } else {
                files[port]!!.nextFile()
            }
            outfile.parentFile.mkdirs()

            logger.debug { "Writing $port to ${outfile.absolutePath}" }

            var append = false
            if (wroteTo.contains(port)) {
                if (!output.isSequential()) {
                    append = true
                }
            }
            wroteTo[port] = outfile.toURI()

            manifestMap.add(port, output, document, outfile.toURI())
            FileOutputStream(outfile, append)
        }

        if (output.multipartMixed) {
            val mime = MimeOutputSequence(xmlCalabash, fos, output.multiplex)
            mime.pipelineUri = pipelineUri
            mime.stepName = stepName
            mime.port = port
            outputMime[port] = mime
            mime.addDocument(port, document)
            if (output.multiplex) {
                multiplexOutput = mime
            }
            return
        }

        val contentType = document.contentType
        val externalSerialization = mutableMapOf<QName, XdmValue>()
        val configProps = xmlCalabash.config.serialization[contentType] ?: emptyMap()
        for ((name, value) in configProps) {
            val untypedValue = StringConverter.StringToUntypedAtomic().convert(XdmAtomicValue(value).underlyingValue)
            externalSerialization[name] = XdmAtomicValue.wrap(untypedValue)
        }

        DocumentWriter(document, fos, externalSerialization).write()
        if (fos != System.out) {
            fos.close()
        }
    }

    private inner class ManifestMap() {
        val map = mutableMapOf<String, MutableList<Pair<MediaType?, URI>>>()
        var multiplexedOutput: URI? = null
        val multiplexedPorts = mutableListOf<Pair<String, MediaType?>>()

        fun add(port: String, output: XmlCalabashOutput, doc: XProcDocument, href: URI) {
            if (manifest == null) {
                return
            }

            if (output.multiplex) {
                multiplexedPorts.add(Pair(port, doc.contentType))
                multiplexedOutput = href
            } else {
                val list = map[port] ?: mutableListOf()
                list.add(Pair(doc.contentType, href))
                map[port] = list
            }
        }

        // This can only be called multiplexed ports when a second or later document is output
        fun add(port: String, output: XmlCalabashOutput, doc: XProcDocument) {
            if (manifest == null) {
                return
            }

            multiplexedPorts.add(Pair(port, doc.contentType))
        }
    }

}