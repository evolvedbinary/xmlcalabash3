package com.xmlcalabash.io

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature
import com.xmlcalabash.datamodel.DocumentContext
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.TypeUtils
import net.sf.saxon.event.ReceiverOption
import net.sf.saxon.event.SequenceCopier
import net.sf.saxon.lib.SerializerFactory
import net.sf.saxon.om.NodeName
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.s9api.*
import net.sf.saxon.serialize.CharacterMap
import net.sf.saxon.serialize.CharacterMapIndex
import net.sf.saxon.serialize.Emitter
import net.sf.saxon.serialize.XMLEmitter
import net.sf.saxon.value.QNameValue
import net.sf.saxon.z.IntHashMap
import org.apache.xml.security.c14n.Canonicalizer
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.*
import javax.xml.transform.stream.StreamResult

class DocumentWriter(val doc: XProcDocument,
                     val stream: OutputStream,
                     externalSerialization: Map<QName, XdmValue> = emptyMap()): Marshaller(doc.context) {
    companion object {
        val cmapName = QName(NsCx.namespace, "cx:character-map-name")
        val cmapStructuredName = StructuredQName("cx", NsCx.namespace.toString(), "character-map-name")
        private var c14nInit = false
    }

    private val _params = mutableMapOf<QName, XdmValue>()
    val inType = doc.contentType?.classification() ?: MediaClassification.BINARY
    val serializationParameters: Map<QName, XdmValue>
        get() = _params

    init {
        val inputMap = doc.properties.getSerialization()
        for (key in inputMap.keySet()) {
            val value = inputMap.get(key)
            val qvalue = key.underlyingValue
            val qkey = if (qvalue is QNameValue) {
                QName(qvalue.prefix, qvalue.namespaceURI.toString(), qvalue.localName)
            } else {
                throw RuntimeException("Expected map of QName keys")
            }
            _params[qkey] = value
        }
        _params.putAll(externalSerialization)
    }

    operator fun get(name: QName): XdmValue? {
        return _params[name]
    }

    operator fun set(name: QName, value: XdmValue) {
        _params[name] = value
    }

    operator fun set(name: QName, value: String) {
        _params[name] = XdmAtomicValue(value)
    }

    operator fun set(name: QName, value: Boolean) {
        _params[name] = XdmAtomicValue(value)
    }

    fun write() {
        when (inType) {
            MediaClassification.XML, MediaClassification.XHTML, MediaClassification.HTML -> {
                writeMarkup()
            }
            MediaClassification.JSON, MediaClassification.YAML, MediaClassification.TOML -> {
                writeJson()
            }
            MediaClassification.TEXT -> {
                writeText()
            }
            MediaClassification.BINARY -> {
                writeOther()
            }
        }
    }

    private fun writeMarkup() {
        if (!_params.containsKey(Ns.method)) {
            val docClass = doc.contentType?.classification() ?: MediaClassification.XML
            when (docClass) {
                MediaClassification.XML -> _params.put(Ns.method, XdmAtomicValue("xml"))
                MediaClassification.XHTML -> _params.put(Ns.method, XdmAtomicValue("xhtml"))
                MediaClassification.HTML -> _params.put(Ns.method, XdmAtomicValue("html"))
                else -> {
                    throw XProcError.xiImpossible("Called writeMarkup for ${doc.contentType}").exception()
                }
            }
        }

        val savesf = docContext.processor.underlyingConfiguration.serializerFactory
        val c14n = Ns.canonical in serializationParameters

        if (doc.properties[NsCx.xmlnt] != null) {
            if (c14n) {
                throw XProcError.xdInvalidSerializationC14N().exception()
            }
            val prolog = doc.properties[NsCx.xmlnt]!!.underlyingValue.stringValue
            docContext.processor.underlyingConfiguration.serializerFactory = XmlntSerializerFactory(docContext, prolog)
        }

        if (c14n) {
            val baos = ByteArrayOutputStream()
            val serializer = docContext.processor.newSerializer(baos)
            setSerializationProperties(serializer)
            serializeValue(serializer, doc.value)

            if (!c14nInit) {
                org.apache.xml.security.Init.init();
                c14nInit = true
            }

            val canon = Canonicalizer.getInstance(Canonicalizer.ALGO_ID_C14N_WITH_COMMENTS)
            canon.canonicalize(baos.toByteArray(), stream, true)
        } else {
            val serializer = docContext.processor.newSerializer(stream)
            setSerializationProperties(serializer)
            serializeValue(serializer, doc.value)
        }

        docContext.processor.underlyingConfiguration.serializerFactory = savesf
    }

    private fun writeJson() {
        if (!_params.containsKey(Ns.method)) {
            val docClass = doc.contentType?.classification() ?: MediaClassification.XML
            when (docClass) {
                MediaClassification.JSON -> _params.put(Ns.method, XdmAtomicValue("json"))
                MediaClassification.YAML -> _params.put(Ns.method, XdmAtomicValue(NsCx.yaml))
                MediaClassification.TOML -> _params.put(Ns.method, XdmAtomicValue(NsCx.toml))
                else -> _params.put(Ns.method, XdmAtomicValue("json"))
            }
        }

        val serMethod = _params[Ns.method]!!
        val method = if (serMethod.underlyingValue is QNameValue) {
            val sname = serMethod.underlyingValue as QNameValue
            QName(sname.prefix, sname.namespaceURI.toString(), sname.localName)
        } else {
            val typeUtils = TypeUtils(docContext)
            typeUtils.parseQName(serMethod.underlyingValue.stringValue)
        }

        if (method == Ns.json || method == Ns.adaptive) {
            val serializer = docContext.processor.newSerializer(stream)
            setSerializationProperties(serializer)
            serializeValue(serializer, doc.value)
            return
        }

        if (method != NsCx.yaml && method != NsCx.toml) {
            throw XProcError.xdInvalidSerialization("${method}").exception()
        }

        val saveParams = mutableMapOf<QName, XdmValue>()
        saveParams.putAll(_params)
        _params.clear()

        val baos = ByteArrayOutputStream()
        val jsonSerializer = docContext.processor.newSerializer(baos)
        _params[Ns.method] = XdmAtomicValue("json")
        _params[Ns.encoding] = XdmAtomicValue("UTF-8")
        setSerializationProperties(jsonSerializer)
        jsonSerializer.serializeXdmValue(doc.value)

        _params.clear()
        _params.putAll(saveParams)

        val jsonReader = ObjectMapper(JsonFactory())
        val obj = jsonReader.readValue(baos.toString(StandardCharsets.UTF_8), Object::class.java)

        val text = if (method == NsCx.yaml) {
            val yamlWriter = ObjectMapper(YAMLFactory().disable(Feature.WRITE_DOC_START_MARKER))
            yamlWriter.writeValueAsString(obj)
        } else { // must be TOML
            val tomlWriter = ObjectMapper(TomlFactory())
            tomlWriter.writeValueAsString(obj)
        }

        val builder = SaxonTreeBuilder(doc.context.processor)
        builder.startDocument(doc.baseURI)
        builder.addText(text)
        builder.endDocument()

        val serializer = docContext.processor.newSerializer(stream)
        _params[Ns.method] = XdmAtomicValue("text")
        setSerializationProperties(serializer)
        serializeValue(serializer, builder.result)
    }

    private fun writeText() {
        if (!_params.containsKey(Ns.method)) {
            _params[Ns.method] = XdmAtomicValue("text")
        }

        val serializer = docContext.processor.newSerializer(stream)
        _params[Ns.method] = XdmAtomicValue("text")
        setSerializationProperties(serializer)
        serializeValue(serializer, doc.value)
    }

    private fun writeOther() {
        if (doc is XProcBinaryDocument) {
            stream.write(doc.binaryValue)
        } else {
            stream.write(doc.value.toString().toByteArray(StandardCharsets.UTF_8))
        }
    }

    private fun serializeValue(serializer: Serializer, value: XdmValue) {
        if (_params.containsKey(Ns.useCharacterMaps) && value is XdmNode) {
            // This is the most bizarre, around-the-houses thing imaginable because Saxon's
            // serializer.serializeNode() ignores character maps. This sort of brute forces
            // our way down a code path that doesn't ignore them.
            val sresult = StreamResult(stream)
            val sf: SerializerFactory = docContext.processor.underlyingConfiguration.serializerFactory
            val tr = sf.getReceiver(sresult, serializer.serializationProperties)
            SequenceCopier.copySequence(value.underlyingValue.iterate(), tr)
            stream.flush()
        } else {
            serializer.serializeXdmValue(value)
        }
    }

    private fun setSerializationProperties(serializer: Serializer) {
        // I have to thread a strange needle here where the character map can't be represented
        // as a map in the call to the serializer.
        val cmapIndex = makeCharacterMapIndex()
        if (!cmapIndex.isEmpty()) {
            serializer.setCharacterMap(cmapIndex)
        }

        try {
            for ((name, value) in _params) {
                // Ignore canonical; it's not (yet) supported by Saxon and is handled above
                if (name == Ns.canonical) {
                    continue
                }

                if (value.underlyingValue is QNameValue) {
                    val qname = (value.underlyingValue as QNameValue)
                } else {
                    // Ignore the empty sequence...
                    if (value != XdmEmptySequence.getInstance()) {
                        serializer.setOutputProperty(name, value.underlyingValue.stringValue)
                    }
                }
            }
        } catch (ex: Exception) {
            throw XProcError.xdInvalidSerializationProperty().exception(ex)
        }
    }

    private fun makeCharacterMapIndex(): CharacterMapIndex {
        // I have to thread a strange needle here where the character map can't be represented
        // as a map in the call to the serializer.
        val useCharacterMaps = _params.remove(Ns.useCharacterMaps)
        val cmapIndex = CharacterMapIndex()
        if (useCharacterMaps != null) {
            val xdmMap = useCharacterMaps as XdmMap
            val hashmap = IntHashMap<String>()
            for (key in xdmMap.keySet()) {
                val cp = key.underlyingValue.stringValue.codePointAt(0)
                val value = xdmMap.get(key).underlyingValue.stringValue
                hashmap.put(cp, value)
            }
            val cmap = CharacterMap(cmapStructuredName, hashmap)
            cmapIndex.putCharacterMap(cmapStructuredName, cmap)
            _params[Ns.useCharacterMaps] = XdmAtomicValue(cmapName)
        }
        return cmapIndex
    }

    private class XmlntSerializerFactory(docContext: DocumentContext, val prolog: String): SerializerFactory(docContext.processor.underlyingConfiguration) {
        override fun newXMLEmitter(properties: Properties?): Emitter {
            return NonconformantXmlEmitter(properties, prolog)
        }
    }

    private class NonconformantXmlEmitter(val properties: Properties?, val prolog: String): XMLEmitter() {
        override fun writeDeclaration() {
            if (!declarationIsWritten) {
                super.writeDeclaration()
                writer.write(prolog)
            }
        }

        override fun writeAttribute(elCode: NodeName, attname: String, value: String, properties: Int) {
            val prop = properties or ReceiverOption.DISABLE_ESCAPING and (ReceiverOption.USE_NULL_MARKERS.inv())

            // I'm not sure what this business with null markers is...
            val entref = "\u0000([^\u0000]+)\u0000".toRegex()
            var match = entref.find(value)
            if (match != null) {
                val sb = StringBuilder()
                var normalvalue = value
                while (match != null) {
                    sb.append(normalvalue.substring(0, match.range.first))
                    sb.append("&")
                    sb.append(match.groupValues[1])
                    sb.append(";")
                    normalvalue = normalvalue.substring(match.range.last+1)
                    match = entref.find(normalvalue)
                }
                normalvalue = sb.toString() + normalvalue
                super.writeAttribute(elCode, attname, normalvalue, prop)
            } else {
                super.writeAttribute(elCode, attname, value, prop)
            }
        }
    }
}