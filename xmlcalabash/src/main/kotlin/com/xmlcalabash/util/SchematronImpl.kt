package com.xmlcalabash.util

import com.xmlcalabash.XmlCalabashBuildConfig
import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsSchxslt
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import javax.xml.transform.stream.StreamSource

class SchematronImpl(val stepConfig: StepConfiguration) {
    companion object {
        val transpilerResource = "/com/xmlcalabash/schxslt2-${XmlCalabashBuildConfig.SCHXSLT2}/transpile.xsl"
        var transpilerExec: XsltExecutable? = null
    }

    fun test(sourceXml: XdmNode, schemaXml: XdmNode, phase: String?, parameters: Map<QName, XdmValue>): List<XdmNode> {
        return failedAssertions(report(sourceXml, schemaXml, phase, parameters))
    }

    fun test(sourceValue: XdmValue, schemaXml: XdmNode, phase: String?, parameters: Map<QName, XdmValue>): List<XdmNode> {
        val failures = mutableListOf<XdmNode>()
        val iter = sourceValue.iterator()
        while (iter.hasNext()) {
            val item = iter.next()
            failures.addAll(failedAssertions(report(item, schemaXml, phase, parameters)))
        }
        return failures
    }

    fun report(sourceXml: XdmItem, schemaXml: XdmNode, phase: String?, parameters: Map<QName, XdmValue>): XdmNode {
        val schemaRoot = when (schemaXml.nodeKind) {
            XdmNodeKind.ELEMENT -> schemaXml
            else -> S9Api.documentElement(schemaXml)
        }

        val xschema = if (schemaXml.nodeKind == XdmNodeKind.DOCUMENT) {
            schemaXml
        } else {
            val patch = SaxonTreeBuilder(stepConfig)
            patch.startDocument(schemaRoot.baseURI)
            patch.addSubtree(schemaXml)
            patch.endDocument()
            patch.result
        }

        val schema = S9Api.adjustBaseUri(xschema, schemaXml.baseURI)
        val schemaAware = stepConfig.processor.isSchemaAware

        val staticParams = mutableMapOf<QName, XdmValue>()
        for ((name, value) in parameters) {
            if (name in NsSchxslt.staticParams) {
                staticParams[name] = value
            }
        }

        lateinit var transpiler: XsltTransformer
        synchronized(Companion) {
            val transpilerExec = loadExecutable(staticParams)
            transpiler = transpilerExec.load()
        }

        if (phase != null) {
            transpiler.setParameter(NsSchxslt.phase, XdmAtomicValue(phase))
        }

        for ((name, value) in parameters) {
            if (name in NsSchxslt.dynamicParams) {
                transpiler.setParameter(name, value)
            } else if (name !in NsSchxslt.staticParams) {
                stepConfig.info { "Ignoring unknown Schematron parameter ${name}" }
            }
        }

        var destination = XdmDestination()
        transpiler.initialContextNode = schema
        transpiler.destination = destination
        if (schema.baseURI != null) {
            transpiler.baseOutputURI = schema.baseURI.toString()
        }

        transpiler.transform()

        val compiledSchema = S9Api.adjustBaseUri(destination.xdmNode, schema.baseURI)
        val compiler = stepConfig.processor.newXsltCompiler()
        compiler.isSchemaAware = stepConfig.processor.isSchemaAware
        destination = XdmDestination()

        compiler.isSchemaAware = schemaAware

        val exec = compiler.compile(compiledSchema.asSource())
        val transformer = exec.load30()

        transformer.setStylesheetParameters(parameters)

        transformer.globalContextItem = sourceXml
        if (sourceXml is XdmNode && sourceXml.baseURI != null) {
            transformer.setBaseOutputURI(sourceXml.baseURI.toString())
        }

        transformer.applyTemplates(sourceXml, destination)
        return destination.xdmNode
    }

    fun failedAssertions(node: XdmNode): List<XdmNode> {
        val nsBindings = mapOf("svrl" to "http://purl.oclc.org/dsdl/svrl")
        val xpath = "//svrl:failed-assert|//svrl:successful-report"

        val xcomp = stepConfig.newXPathCompiler()
        for ((prefix, value) in nsBindings) {
            xcomp.declareNamespace(prefix, value)
        }

        val xexec = xcomp.compile(xpath)
        val selector = xexec.load()

        selector.contextItem = node

        val results = mutableListOf<XdmNode>()
        for (value in selector.iterator()) {
            results.add(value as XdmNode)
        }

        return results
    }

    private fun loadExecutable(staticParams: Map<QName,XdmValue>): XsltExecutable {
        if (staticParams.isEmpty()) {
            if (transpilerExec == null) {
                val stream = SchematronImpl::class.java.getResourceAsStream(transpilerResource)
                    ?: throw XProcError.xiCannotLoadResource(transpilerResource).exception()
                val source = StreamSource(stream)
                val compiler = stepConfig.processor.newXsltCompiler()
                compiler.isSchemaAware = stepConfig.processor.isSchemaAware
                transpilerExec = compiler.compile(source)
            }
            return transpilerExec!!
        }

        val stream = SchematronImpl::class.java.getResourceAsStream(transpilerResource)
            ?: throw XProcError.xiCannotLoadResource(transpilerResource).exception()
        val source = StreamSource(stream)
        val compiler = stepConfig.processor.newXsltCompiler()
        for ((name, value) in staticParams) {
            compiler.setParameter(name, value);
        }
        compiler.isSchemaAware = stepConfig.processor.isSchemaAware
        return compiler.compile(source)
    }
}