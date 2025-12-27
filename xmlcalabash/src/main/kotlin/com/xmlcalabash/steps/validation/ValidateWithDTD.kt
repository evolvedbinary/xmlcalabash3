package com.xmlcalabash.steps.validation

import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.xvrl.XvrlReport
import net.sf.saxon.lib.ErrorReporter
import net.sf.saxon.lib.Validation
import net.sf.saxon.s9api.*
import org.apache.logging.log4j.kotlin.logger
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import javax.xml.transform.sax.SAXSource

class ValidateWithDTD(): AbstractValidationStep() {
    lateinit var source: XProcDocument
    lateinit var report: XvrlReport

    override fun run() {
        super.run()

        val parameters = qnameMapBinding(Ns.parameters)
        val paramLno = parameters[NsCx.lineNumbering]?.underlyingValue?.effectiveBooleanValue() ?: false

        report = XvrlReport.newInstance(stepConfig, xvrlParameters(parameters))
        report.metadata.creator(stepConfig.saxonConfig.environment.productName,
            stepConfig.saxonConfig.environment.productVersion)

        source = queues["source"]!!.first()
        source.baseURI?.let { report.metadata.document(it) }

        val docElem = S9Api.documentElement(source.value as XdmNode)
        val inputLno = docElem.underlyingNode.lineNumber >= 1

        val assertValid = booleanBinding(Ns.assertValid) ?: true

        val stepSerialization = if (options[Ns.serialization] != null) {
            val map = options[Ns.serialization]!!
            if (map.value == XdmEmptySequence.getInstance()) {
                mapOf()
            } else {
                stepConfig.typeUtils.asMap(map.value as XdmMap)
            }
        } else {
            mapOf()
        }

        val documentSerialization = stepConfig.typeUtils.asMap(
            stepConfig.typeUtils.forceQNameKeys(
                (source.properties[Ns.serialization] ?: XdmMap()) as XdmMap
            )
        )

        val serialization = mutableMapOf<QName, XdmValue>()
        serialization.putAll(stepSerialization)
        serialization.putAll(documentSerialization)

        val docText = serializeSource(serialization)

        var result = source.value as XdmNode
        try {
            val parser = stepConfig.processor.newDocumentBuilder()
            parser.isDTDValidation = true

            val sconfig = stepConfig.saxonConfig.processor.underlyingConfiguration
            val options = sconfig.parseOptions
                .withErrorReporter(MyErrorReporter())
                .withDTDValidationMode(Validation.STRICT)
                .withLineNumbering(stepConfig.xmlCalabashConfig.lineNumbering || paramLno || inputLno)

            sconfig.parseOptions = options

            val destination = XdmDestination()
            val bais = ByteArrayInputStream(docText.toByteArray(StandardCharsets.UTF_8))
            val vsource = SAXSource(InputSource(bais))
            vsource.systemId = source.baseURI?.toString()
            parser.parse(vsource, destination)
            result = destination.xdmNode

            val props = DocumentProperties()
            props.set(Ns.baseUri, source.properties.baseURI)
            if (source.properties.get(Ns.serialization) != null) {
                props.set(Ns.serialization, source.properties.get(Ns.serialization))
            }
            receiver.output("result", XProcDocument.ofXml(result, stepConfig, MediaType.XML, props))
            receiver.output("report", XProcDocument.ofXml(report.asXml(), stepConfig, MediaType.XML, DocumentProperties()))
        } catch (ex: Exception) {
            report.detection("error", null, ex.message ?: "(no message)")
            val xvrl = XProcDocument.ofXml(report.asXml(), stepConfig)

            if (assertValid) {
                throw stepConfig.exception(XProcError.xcDtdValidationFailed(xvrl))
            }

            receiver.output("result", source)
            receiver.output("report", xvrl)
        }
    }

    override fun reset() {
        super.reset()
        source = XProcDocument.ofEmpty(stepConfig)
    }

    private fun serializeSource(serialization: Map<QName,XdmValue>): String {
        if (source.contentClassification in listOf(MediaClassification.XML, MediaClassification.XHTML, MediaClassification.HTML)) {
            val baos = ByteArrayOutputStream()
            DocumentWriter(source, baos, serialization).write()
            return baos.toString(StandardCharsets.UTF_8)
        } else {
            return source.value.underlyingValue.stringValue
        }
    }

    inner class MyErrorReporter : ErrorReporter {
        override fun report(error: XmlProcessingError?) {
            if (error == null) {
                stepConfig.warn { "DTD validation reported \"null\" error?" }
                return
            }

            report.detection(error)
        }
    }
}