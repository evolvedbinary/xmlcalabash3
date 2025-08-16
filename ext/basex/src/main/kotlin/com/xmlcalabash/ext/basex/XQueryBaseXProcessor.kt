package com.xmlcalabash.ext.basex

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.api.Receiver
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import com.xmlcalabash.spi.XQueryProcessor
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.value.AtomicValue
import org.basex.api.client.ClientSession
import org.basex.core.Context
import org.basex.core.MainOptions
import org.basex.core.StaticOptions
import org.basex.core.cmd.Add
import org.basex.core.cmd.CreateDB
import org.basex.core.cmd.DropDB
import org.basex.io.serial.Serializer
import org.basex.io.serial.SerializerOptions
import org.basex.query.QueryProcessor
import org.basex.query.value.type.Type
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.collections.iterator

class XQueryBaseXProcessor(): XQueryProcessor {
    lateinit var stepConfig: XProcStepConfiguration
    lateinit var receiver: Receiver
    lateinit var stepParams: RuntimeStepParameters

    lateinit var sources: List<XProcDocument>
    lateinit var query: String
    lateinit var parameters: Map<QName, XdmValue>

    private val config = mutableMapOf<QName, String>()

    var host: String? = null
    var port = 1984
    var username: String? = null
    var password: String? = null

    override fun setup(stepConfig: XProcStepConfiguration, receiver: Receiver, stepParams: RuntimeStepParameters, config: Map<QName, String>) {
        this.stepConfig = stepConfig
        this.receiver = receiver
        this.stepParams = stepParams
        this.config.putAll(config)
    }

    override fun run(sources: List<XProcDocument>, query: XProcDocument, parameters: Map<QName, XdmValue>, version: String) {
        this.sources = sources
        this.query = query.value.underlyingValue.stringValue
        this.parameters = parameters

        host = parameters[NsCx.host]?.underlyingValue?.stringValue ?: config[Ns.host]
        port = (parameters[NsCx.port]?.underlyingValue?.stringValue ?: config[Ns.port])?.toInt() ?: 1984
        username = parameters[NsCx.username]?.underlyingValue?.stringValue ?: config[Ns.username]
        password = parameters[NsCx.password]?.underlyingValue?.stringValue ?: config[Ns.password]

        if (host == null) {
            if (username != null || password != null) {
                throw stepConfig.exception(XProcError.xdStepFailed("Host must be specified with username and password"))
            }
            localQuery()
            return
        }

        remoteQuery()
    }

    private fun remoteQuery() {
        if (sources.isNotEmpty()) {
            throw stepConfig.exception(XProcError.xdStepFailed("Sources are not supported when querying a BaseX server"))
        }

        if (username == null || password == null) {
            throw stepConfig.exception(XProcError.xdStepFailed("Username and password must be specified"))
        }

        val session = ClientSession(host, port, username, password)
        val query = session.query(query)

        for ((qname, value) in parameters) {
            if (qname.namespaceUri != NsCx.namespace) {
                val name = if (qname.namespaceUri == NamespaceUri.NULL) {
                    qname.localName
                } else {
                    "Q{${qname.namespaceUri}}${qname.localName}"
                }

                if (value.underlyingValue is AtomicValue) {
                    val avalue = value.underlyingValue as AtomicValue
                    val type = avalue.primitiveType.name
                    query.bind("\$${name}", value.underlyingValue.stringValue, "xs:${type}")
                } else {
                    query.bind("\$${name}", value.underlyingValue.stringValue)
                }
            }
        }

        while (query.more()) {
            val item = query.next()
            val type = query.type()
            sendTypedResult(item.toByteArray(StandardCharsets.UTF_8), type)
        }
    }

    private fun localQuery() {
        val dbname = "xmlcalabash_${UUID.randomUUID()}"
        val context = Context(false)
        org.basex.core.cmd.Set(MainOptions.MAINMEM, true).execute(context)
        CreateDB(dbname).execute(context)

        try {
            for (doc in sources) {
                val baos = ByteArrayOutputStream()
                val writer = DocumentWriter(doc, baos)
                writer.write()
                val serial = baos.toString(StandardCharsets.UTF_8)
                Add(doc.baseURI.toString(), serial).execute(context)
            }

            val qp =  QueryProcessor(query, context)

            bindExternalVariables(qp)
            sendResults(qp)
        } finally {
            DropDB(dbname).execute(context)
        }
    }

    private fun bindExternalVariables(qp: QueryProcessor) {
        for ((qname, value) in parameters) {
            if (qname.namespaceUri != NsCx.namespace) {
                val name = if (qname.namespaceUri == NamespaceUri.NULL) {
                    qname.localName
                } else {
                    "Q{${qname.namespaceUri}}${qname.localName}"
                }

                if (value.underlyingValue is AtomicValue) {
                    val avalue = value.underlyingValue as AtomicValue
                    val type = avalue.primitiveType.name
                    qp.variable("\$${name}", value.underlyingValue.stringValue, "xs:${type}")
                } else {
                    qp.variable("\$${name}", value.underlyingValue.stringValue)
                }
            }
        }
    }

    private fun sendResults(qp: QueryProcessor) {
        for (item in qp.value()) {
            if (item.type.id() >= Type.ID.NOD && item.type.id() <= Type.ID.SCA) {
                val baos = ByteArrayOutputStream()
                // FIXME: what should these be and how should they be specified?
                val sopts = SerializerOptions()
                val serializer = Serializer.get(baos, sopts)
                serializer.serialize(item)
                val serial = baos.toByteArray()

                sendTypedResult(serial, item.type)
            } else if (item.type.isStringOrUntyped) {
                val serial = item.string(null)
                sendTypedResult(serial, item.type)
            } else {
                val serial = item.string(null)
                sendTypedResult(serial, item.type)
            }
        }
    }

    private fun sendTypedResult(serial: ByteArray, type: Type) {
        if (type.id() >= Type.ID.NOD && type.id() <= Type.ID.SCA) {
            val loader = DocumentLoader(stepConfig, null)
            val stream = ByteArrayInputStream(serial)
            val doc = loader.load(stream, MediaType.XML)
            receiver.output("result", doc)
        } else if (type.isStringOrUntyped) {
            val loader = DocumentLoader(stepConfig, null)
            val stream = ByteArrayInputStream(serial)
            val doc = loader.load(stream, MediaType.TEXT)
            receiver.output("result", doc)
        } else {
            val loader = DocumentLoader(stepConfig, null)
            val stream = ByteArrayInputStream(serial)
            val doc = loader.load(stream, MediaType.JSON)
            receiver.output("result", doc)
        }
    }

    override fun reset() {
        // nop
    }

    override fun teardown() {
        // nop
    }
}