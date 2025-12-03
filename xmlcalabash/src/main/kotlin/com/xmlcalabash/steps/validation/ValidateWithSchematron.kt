package com.xmlcalabash.steps.validation

import com.xmlcalabash.XmlCalabashBuildConfig
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.namespace.NsSchxslt
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SchematronImpl
import com.xmlcalabash.xvrl.XvrlReport
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue

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

        val xvrlParameters = xvrlParameters(parameters)

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
            report = xvrlReport(report, reportFormat, schema, xvrlParameters)
        }

        if (assertValid) {
            if (failed.isNotEmpty()) {
                val xvrl = if (reportFormat == "xvrl") {
                    report
                } else {
                    xvrlReport(report, "xvrl", schema, xvrlParameters)
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

    private fun xvrlReport(report: XdmNode, reportFormat: String, schema: XProcDocument, xvrlParameters: Map<String,String>): XdmNode {
        if (reportFormat != "xvrl") {
            return report
        }

        val xvrl = XvrlReport.fromSvrl(stepConfig, xvrlParameters, report)
        xvrl.metadata.validator("SchXslt2", XmlCalabashBuildConfig.DEPENDENCIES["schxslt2"] ?: "unknown")

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