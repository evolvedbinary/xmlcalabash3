package com.xmlcalabash.steps

import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.resourcecache.CompiledResourceType
import com.xmlcalabash.resourcecache.CompiledXQueryResource
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import com.xmlcalabash.spi.XQueryProcessor
import com.xmlcalabash.util.*
import net.sf.saxon.event.PipelineConfiguration
import net.sf.saxon.event.Receiver
import net.sf.saxon.lib.SaxonOutputKeys
import net.sf.saxon.s9api.*
import net.sf.saxon.serialize.SerializationProperties
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.charset.StandardCharsets

open class XQuerySaxonProcessor(): XQueryProcessor {
    lateinit var stepConfig: XProcStepConfiguration
    lateinit var receiver: com.xmlcalabash.runtime.api.Receiver
    lateinit var stepParams: RuntimeStepParameters
    lateinit var errorReporter: SaxonErrorReporter

    val sources = mutableListOf<XProcDocument>()

    lateinit var query: XProcDocument

    val parameters = mutableMapOf<QName,XdmValue>()

    var goesBang: XProcError? = null
    var cachedQuery = false

    private var primaryDestination: Destination? = null
    private var outputProperties = mutableMapOf<QName, XdmValue>()

    override fun setup(stepConfig: XProcStepConfiguration, receiver: com.xmlcalabash.runtime.api.Receiver, stepParams: RuntimeStepParameters, cacheQuery: Boolean, config: Map<QName, String>) {
        this.stepConfig = stepConfig
        this.receiver = receiver
        this.stepParams = stepParams
        cachedQuery = cacheQuery

        errorReporter = SaxonErrorReporter(stepConfig)
        stepConfig.saxonConfig.configuration.setErrorReporterFactory { config -> errorReporter }
    }

    override fun run(sources: List<XProcDocument>, query: XProcDocument, parameters: Map<QName, XdmValue>, version: String) {
        this.sources.addAll(sources)
        this.query = query
        this.parameters.putAll(parameters)

        when (version) {
            "3.1" -> xquery31()
            "3.0" -> xquery30()
            else -> throw stepConfig.exception(XProcError.xcXQueryVersionNotAvailable(version))
        }
    }

    override fun reset() {
        sources.clear()
        parameters.clear()
        query = XProcDocument.ofEmpty(stepConfig)
    }

    override fun teardown() {
        reset()
    }

    private fun xquery30() {
        for (doc in sources) {
            val ctype = doc.contentType ?: MediaType.OCTET_STREAM
            if (ctype.classification() !in listOf(MediaClassification.XML, MediaClassification.XHTML,
                    MediaClassification.HTML, MediaClassification.TEXT)) {
                throw stepConfig.exception(XProcError.xcXQueryInputNot30Compatible(ctype))
            }
        }

        for ((name, value) in parameters) {
            when (value) {
                is XdmAtomicValue, is XdmNode -> Unit
                is XdmMap -> throw stepConfig.exception(XProcError.xcXQueryInvalidParameterType(name, "map"))
                is XdmArray -> throw stepConfig.exception(XProcError.xcXQueryInvalidParameterType(name, "array"))
                is XdmFunctionItem -> throw stepConfig.exception(XProcError.xcXQueryInvalidParameterType(name, "function"))
                else -> stepConfig.debug { "Unexpected parameter type: ${value} passed to p:xquery"}
            }
        }

        runXQueryProcessor()
    }

    private fun xquery31() {
        runXQueryProcessor()
    }

    private fun runXQueryProcessor() {
        val document = sources.firstOrNull()

        val processor = stepConfig.processor
        val underlyingConfig = processor.underlyingConfiguration
        // FIXME: runtime.getConfigurer().getSaxonConfigurer().configXQuery(config);

        val collectionFinder = underlyingConfig.collectionFinder
        val unparsedTextURIResolver = underlyingConfig.unparsedTextURIResolver

        underlyingConfig.setDefaultCollection(XProcCollectionFinder.DEFAULT)
        underlyingConfig.setCollectionFinder(XProcCollectionFinder(sources, collectionFinder))

        val manager = stepConfig.processor.getSchemaManager()
        if (manager != null) {
            manager.errorReporter = errorReporter
            manager.schemaURIResolver = XsdResolver(stepConfig)
        }

        val exec = try {
            getCompiledQuery()
        } catch (ex: XProcException) {
            throw ex
        } catch (ex: Exception) {
            underlyingConfig.collectionFinder = collectionFinder
            if (goesBang != null) {
                throw goesBang!!.exception()
            }
            val err = XProcError.xcXQueryCompileError(ex.message ?: "(no error message)", ex)
            err.updateReports(errorReporter.errorMessages)
            throw stepConfig.exception(err, ex)
        }

        lateinit var queryEval: XQueryEvaluator
        synchronized(exec) {
            queryEval = exec.load()
        }
        queryEval.errorReporter = errorReporter
        queryEval.setUnparsedTextResolver(unparsedTextURIResolver)

        for ((param, value) in parameters) {
            queryEval.setExternalVariable(param, value)
        }

        if (document != null) {
            queryEval.setContextItem(document.value as XdmItem)
        }

        val result = MyDestination(outputProperties)
        queryEval.setDestination(result)

        queryEval.setSchemaValidationMode(ValidationMode.DEFAULT)

        try {
            queryEval.run()
            for (document in S9Api.makeDocuments(stepConfig, queryEval.evaluate())) {
                receiver.output("result", document)
            }
        } catch (ex: Throwable) {
            // Generally speaking, we can get more useful information from the error reporter
            val error = errorReporter.errorMessages.lastOrNull()
            val location = error?.location ?: Location.NULL
            if (ex.message == error?.message()) {
                throw stepConfig.exception(XProcError.xcXQueryEvalError(ex.message ?: "null", location, error?.message()), ex)
            }
            throw stepConfig.exception(XProcError.xcXQueryEvalError(ex.message ?: "null", location), ex)
        } finally {
            underlyingConfig.collectionFinder = collectionFinder
        }
    }

    private fun getCompiledQuery(): XQueryExecutable {
        if (cachedQuery) {
            if (query.baseURI != null && stepConfig.compiledResourceCache.contains(stepParams.stepName, query.baseURI!!)) {
                val rsrc = stepConfig.compiledResourceCache.get(stepParams.stepName, query.baseURI!!)!!
                if (rsrc.type == CompiledResourceType.XQUERY) {
                    return (rsrc as CompiledXQueryResource).exec
                }
            }
        }

        val processor = stepConfig.processor
        val compiler = processor.newXQueryCompiler()
        compiler.isSchemaAware = processor.isSchemaAware
        compiler.errorReporter = errorReporter
        compiler.moduleURIResolver = stepConfig.environment.documentManager
        compiler.baseURI = query.baseURI

        var xquery = query.value.underlyingValue.stringValue
        if (query.contentClassification == MediaClassification.XML) {
            val root = try {
                S9Api.documentElement(query.value as XdmNode)
            } catch (ex: IllegalArgumentException) {
                throw stepConfig.exception(XProcError.xdStepFailed("XQuery query input is ${query.contentType}, but ${ex.message}"), ex)
            }
            if (root.nodeName != NsC.query) {
                val baos = ByteArrayOutputStream()
                val writer = DocumentWriter(query, baos)
                writer[Ns.encoding] = "UTF-8"
                writer[Ns.omitXmlDeclaration] = true
                writer.write()
                xquery = baos.toString(StandardCharsets.UTF_8)
            }
        }

        val exec = compiler.compile(xquery)

        if (cachedQuery && query.baseURI != null) {
            stepConfig.compiledResourceCache.put(stepParams.stepName, query.baseURI!!, CompiledXQueryResource(exec))
        }

        return exec
    }

    inner class MyDestination(var map: MutableMap<QName,XdmValue>): RawDestination() {
        private var destination: Destination? = null
        private var destBase: URI? = null

        override fun setDestinationBaseURI(baseURI: URI) {
            destBase = baseURI
            if (destination != null) {
                destination!!.setDestinationBaseURI(baseURI)
            }
        }

        override fun getDestinationBaseURI(): URI? {
            return destBase
        }

        override fun getReceiver(pipe: PipelineConfiguration, params: SerializationProperties): Receiver {
            val tree = params.getProperty(SaxonOutputKeys.BUILD_TREE)

            val pnames = params.properties.propertyNames()
            while (pnames.hasMoreElements()) {
                val name: String = pnames.nextElement() as String
                val qname = if (name.startsWith("{")) {
                    ValueUtils.parseClarkName(name)
                } else {
                    QName(name)
                }
                val value = params.properties.getProperty(name) as String
                if (value == "yes" || value == "no") {
                    map.put(qname, XdmAtomicValue(value == "yes"))
                } else {
                    map.put(qname, XdmAtomicValue(value))
                }
            }

            val dest = if (tree == "yes") {
                XdmDestination()
            } else {
                RawDestination()
            }

            if (destBase != null) {
                dest.setDestinationBaseURI(destBase)
            }

            destination = dest
            primaryDestination = dest
            return dest.getReceiver(pipe, params)
        }

        override fun closeAndNotify() {
            if (destination != null) {
                destination!!.closeAndNotify()
            }
        }

        override fun close() {
            if (destination != null) {
                destination!!.close()
            }
        }
    }
}