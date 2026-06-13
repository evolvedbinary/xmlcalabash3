package com.xmlcalabash.steps.validation

import com.xmlcalabash.XmlCalabashBuildConfig
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.namespace.NsSchxslt
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SchematronImpl
import com.xmlcalabash.xvrl.XvrlReport
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import org.apache.logging.log4j.kotlin.logger

open class ValidateWithSchematron(): AbstractValidationStep() {
    companion object {
        private val s_schematron = QName(NamespaceUri.of("http://purl.oclc.org/dsdl/schematron"), "s:schema")
        private val _phase = QName("phase")
    }

    override fun run() {
        super.run()

        val document = queues["source"]!!.first()
        val schema = queues["schema"]!!.first()

        val parameters = mutableMapOf<QName, XdmValue>()
        parameters.putAll(qnameMapBinding(Ns.parameters))

        val xvrlParameters = mutableMapOf<String, String>()

        if (NsC.xvrl in parameters) {
            val xdmMap = stepConfig.typeUtils.forceQNameKeys(parameters.get(NsC.xvrl) as XdmMap)
            for (key in xdmMap.keySet()) {
                val value = xdmMap.get(key);
                val pname = key.qNameValue
                if (pname.namespaceUri == NsXvrl.namespace) {
                    if (pname.localName in listOf("default-severity", "serialization-format", "language", "map-to-severity", "xpath-notation")) {
                        xvrlParameters[pname.localName] = value.underlyingValue.toString()
                    } else {
                        stepConfig.warn { "Ignoring unknown XVRL parameter: ${pname}" }
                    }
                }
            }

            parameters.remove(NsC.xvrl)
            for ((key, value) in parameters) {
                if (key.namespaceUri == NsXvrl.namespace) {
                    stepConfig.warn { "Ignoring xvrl: parameters when c:xvrl is present" }
                    break
                }
            }
        } else {
            xvrlParameters.putAll(xvrlParameters(parameters))
        }

        if ((xvrlParameters["serialization-format"] ?: "xml") != "xml") {
            val format = xvrlParameters["serialization-format"]
            stepConfig.warn { "The XVRL serialization-format \"${format}\" is not supported, using XML" }
            xvrlParameters.remove("serialization-format")
        }

        if ((xvrlParameters["xpath-notation"] != null)) {
            stepConfig.warn { "The XVRL xpath-notation parameter is not supported" }
            xvrlParameters.remove("xpath-notation")
        }

        val compileParams = mutableMapOf<QName, XdmValue>()
        val dynamicParams = mutableMapOf<QName, XdmValue>()

        if (NsC.compile in parameters) {
            val xdmMap = stepConfig.typeUtils.forceQNameKeys(parameters.get(NsC.compile) as XdmMap)
            for (key in xdmMap.keySet()) {
                val value = xdmMap.get(key);
                compileParams[key.qNameValue] = value
            }
            parameters.remove(NsC.compile)
            dynamicParams.putAll(parameters)
        } else {
            compileParams.putAll(parameters)
            dynamicParams.putAll(parameters)
        }

        val assertValid = booleanBinding(Ns.assertValid) ?: true
        val phase = stringBinding(_phase)
        val reportFormat = stringBinding(Ns.reportFormat) ?: "svrl"

        if (reportFormat != "svrl" && reportFormat != "xvrl") {
            throw stepConfig.exception(XProcError.xcUnsupportedReportFormat(reportFormat))
        }

        if (phase != null && NsSchxslt.phase in parameters) {
            if (phase != parameters[NsSchxslt.phase]!!.underlyingValue.stringValue) {
                throw stepConfig.exception(XProcError.xdStepFailed("Conflicting phases specifed: ${phase} and ${parameters[NsSchxslt.phase]!!}"))
            }
        }

        val impl = SchematronImpl(stepConfig)

        val tron = S9Api.documentElement(schema.value as XdmNode)
        if (tron.nodeName != s_schematron) {
            throw stepConfig.exception(XProcError.xcNotSchematronSchema(tron.nodeName))
        }

        var report = impl.report(document.value as XdmNode, schema.value as XdmNode, phase, compileParams, dynamicParams)
        val failed = impl.failedAssertions(report)

        if (reportFormat == "xvrl") {
            report = xvrlReport(document, report, reportFormat, schema, xvrlParameters)
        }

        if (assertValid) {
            if (failed.isNotEmpty()) {
                val xvrl = if (reportFormat == "xvrl") {
                    report
                } else {
                    xvrlReport(document, report, "xvrl", schema, xvrlParameters)
                }
                val doc = XProcDocument.ofXml(xvrl, document.context)
                if (document.baseURI == null) {
                    throw stepConfig.exception(XProcError.xcNotSchemaValidSchematron(doc))
                }
                throw stepConfig.exception(XProcError.xcNotSchemaValidSchematron(doc, document.baseURI!!))
            }
        }

        receiver.output("report", XProcDocument.ofXml(report, stepConfig))
        receiver.output("result", document)
    }

    private fun xvrlReport(document: XProcDocument, report: XdmNode, reportFormat: String, schema: XProcDocument, xvrlParameters: Map<String,String>): XdmNode {
        if (reportFormat != "xvrl") {
            return report
        }

        val xvrl = XvrlReport.fromSvrl(stepConfig, xvrlParameters, report)
        xvrl.metadata.validator("SchXslt2", XmlCalabashBuildConfig.SCHXSLT2)
        xvrl.metadata.document(document.baseURI)

        if (stepConfig.baseUri != null && schema.baseURI != null
            && schema.baseURI.toString().startsWith(stepConfig.baseUri.toString())
            && schema.value is XdmNode) {
            // It looks like this one was inline...
            xvrl.metadata.schema(schema.baseURI, NamespaceUri.of("http://purl.oclc.org/dsdl/schematron"), "Schematron", null, schema.value as XdmNode)
        } else {
            xvrl.metadata.schema(schema.baseURI, NamespaceUri.of("http://purl.oclc.org/dsdl/schematron"), "Schematron")
        }

        return xvrl.asXml()
    }

    override fun toString(): String = "p:validate-with-schematron"
}