package com.xmlcalabash.steps.extension

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXs
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.UriUtils
import io.pebbletemplates.pebble.PebbleEngine
import io.pebbletemplates.pebble.loader.FileLoader
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.*

class PebbleStep(): AbstractAtomicStep() {
    companion object {
        private val _defaultLocale = QName("defaultLocale")
        private val _strictVariables = QName("strictVariables")
        private val _literalDecimalTreatedAsInteger = QName("literalDecimalTreatedAsInteger")
        private val _literalNumbersAsBigDecimals = QName("literalNumbersAsBigDecimals")
        private val _maxRenderedSize = QName("maxRenderedSize")
        private val _newlineTrimming = QName("newlineTrimming")
        private val _autoEscaping = QName("autoEscaping")
        private val _defaultEscapingStrategy = QName("defaultEscapingStrategy")
        private val _context = QName("context")
    }

    override fun run() {
        super.run()

        val document = queues["source"]!!.first()
        val type = document.contentType?.classification() ?: MediaClassification.BINARY
        val text = when (type) {
            MediaClassification.TEXT -> document.value.underlyingValue.stringValue
            MediaClassification.XML, MediaClassification.XHTML, MediaClassification.HTML,
            MediaClassification.JSON, MediaClassification.TOML, MediaClassification.YAML -> {
                val baos = ByteArrayOutputStream()
                val writer = DocumentWriter(document, baos, mapOf(Ns.indent to XdmAtomicValue(false)))
                writer.write()
                baos.toString(StandardCharsets.UTF_8)
            }
            else -> {
                throw stepConfig.exception(XProcError.xdStepFailed("Cannot process binary documents"))
            }
        }

        val context = mutableMapOf<String, Any>()

        if (_context in options) {
            val map = options[_context]!!.value as XdmMap
            constructContext(context, map)
        }

        val prefix = stringBinding(Ns.prefix)

        val path = if (prefix != null) {
            Paths.get(prefix)
        } else if (document.baseURI != null) {
            val docPath = Paths.get(document.baseURI!!)
            docPath.parent ?: docPath
        } else {
            Paths.get(UriUtils.cwdAsUri().path)
        }

        val fileLoader = FileLoader(path.toString())

        val builder = PebbleEngine.Builder()
        builder.loader(fileLoader)

        stringBinding(_defaultLocale)?.let { builder.defaultLocale(Locale.forLanguageTag(it)) }
        booleanBinding(_strictVariables)?.let { builder.strictVariables(it) }
        booleanBinding(_literalNumbersAsBigDecimals)?.let { builder.literalNumbersAsBigDecimals(it) }
        booleanBinding(_literalDecimalTreatedAsInteger)?.let { builder.literalDecimalTreatedAsInteger(it) }
        integerBinding(_maxRenderedSize)?.let { builder.maxRenderedSize(it) }
        booleanBinding(_newlineTrimming)?.let { builder.newLineTrimming(it) }
        booleanBinding(_autoEscaping)?.let { builder.autoEscaping(it) }
        stringBinding(_defaultEscapingStrategy)?.let { builder.defaultEscapingStrategy(it) }

        val engine = builder.build()
        val template = engine.getLiteralTemplate(text)

        val writer = StringWriter()
        template.evaluate(writer, context)
        val result = writer.toString();

        val inputStream = ByteArrayInputStream(result.toByteArray())
        val loader = DocumentLoader(stepConfig, document.baseURI, document.properties)
        val resultDocument = loader.load(inputStream, document.contentType!!)

        receiver.output("result", resultDocument)
    }

    private fun constructContext(context: MutableMap<String,Any>, map: XdmMap) {
        val kmap = stepConfig.typeUtils.asGenericMap(map)
        for ((mkey, value) in kmap) {
            val key = when (mkey.primitiveTypeName) {
                NsXs.string -> {
                    mkey.value.toString()
                }
                NsXs.QName -> {
                    val qname = mkey.value as QName
                    if (qname.namespaceUri == NamespaceUri.NULL) {
                        qname.localName
                    } else {
                        throw IllegalArgumentException("QName keys must not be in a namespace")
                    }
                }
                else -> {
                    throw IllegalArgumentException("Keys must be strings or QNames in no namespace")
                }
            }

            context[key] = constructValue(value)
        }
    }

    private fun constructValue(value: XdmValue): Any {
        when (value) {
            is XdmMap -> {
                val submap = mutableMapOf<String, Any>()
                constructContext(submap, value)
                return submap
            }

            is XdmArray -> {
                val subarray = mutableListOf<Any>()
                for (item in value.underlyingValue.members()) {
                    subarray.add(XdmValue.wrap(item))
                }
                return subarray
            }

            is XdmNode -> {
                return value.stringValue
            }

            else -> {
                return value.underlyingValue.stringValue
            }
        }
    }

    override fun toString(): String = "cx:pebble"
}