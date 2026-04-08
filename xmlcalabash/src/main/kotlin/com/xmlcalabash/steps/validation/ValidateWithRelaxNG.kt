package com.xmlcalabash.steps.validation

import com.thaiopensource.util.PropertyMapBuilder
import com.thaiopensource.validate.SchemaReader
import com.thaiopensource.validate.ValidateProperty
import com.thaiopensource.validate.ValidationDriver
import com.thaiopensource.validate.auto.AutoSchemaReader
import com.thaiopensource.validate.prop.rng.RngProperty
import com.thaiopensource.validate.rng.CompactSchemaReader
import com.xmlcalabash.XmlCalabashBuildConfig
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.XmlToSax
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.Axis
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmNodeKind
import org.xml.sax.InputSource
import org.xmlresolver.utils.SaxProducer
import java.io.StringReader

open class ValidateWithRelaxNG(): AbstractValidationStep() {
    companion object {
        private val dtdAttributeValues = QName("dtd-attribute-values")
        private val dtdIdIdrefWarnings = QName("dtd-id-idref-warnings")
    }

    override fun run() {
        super.run()

        val document = queues["source"]!!.first()
        val schema = queues["schema"]!!.first()

        val compact = schema.contentClassification == MediaClassification.TEXT

        val dtdAttributeValues = booleanBinding(dtdAttributeValues) ?: false
        val dtdIdIdRefWarnings = booleanBinding(dtdIdIdrefWarnings) ?: false
        val assertValid = booleanBinding(Ns.assertValid) ?: true
        val reportFormat = stringBinding(Ns.reportFormat) ?: "xvrl"
        val parameters = qnameMapBinding(Ns.parameters)

        if (reportFormat != "xvrl") {
            throw stepConfig.exception(XProcError.xcUnsupportedReportFormat(reportFormat))
        }

        val report = Errors(stepConfig, document.baseURI, xvrlParameters(parameters))
        report.report.metadata.validator("Jing", XmlCalabashBuildConfig.DEPENDENCIES["org.relaxng:jing"] ?: "unknown")

        val language = if (compact) "RNC" else "RNG"
        if (stepConfig.baseUri != null && schema.baseURI != null
            && schema.baseURI.toString().startsWith(stepConfig.baseUri.toString())
            && schema.value is XdmNode) {
            // It looks like this one was inline...
            report.report.metadata.schema(schema.baseURI, NamespaceUri.of("http://relaxng.org/ns/structure/1.0"), language, null, schema.value as XdmNode)
        } else {
            report.report.metadata.schema(schema.baseURI, NamespaceUri.of("http://relaxng.org/ns/structure/1.0"), language)
        }

        val listener = CachingErrorListener(stepConfig, report)
        val properties = PropertyMapBuilder()
        properties.put(ValidateProperty.ERROR_HANDLER, listener)
        properties.put(ValidateProperty.URI_RESOLVER, stepConfig.documentManager.resolver.uriResolver)
        properties.put(ValidateProperty.ENTITY_RESOLVER, stepConfig.documentManager.resolver.entityResolver2)

        if (dtdIdIdRefWarnings) {
            RngProperty.CHECK_ID_IDREF.add(properties)
        }

        var sr: SchemaReader? = null
        var schemaInputSource: InputSource? = null

        if (compact) {
            sr = CompactSchemaReader.getInstance()
            // Hack
            val srdr = StringReader(schema.value.underlyingValue.stringValue)
            schemaInputSource = InputSource(srdr)
            schemaInputSource.systemId = schema.baseURI.toString()
        } else {
            sr = AutoSchemaReader()
            schemaInputSource = S9Api.xdmToInputSource(stepConfig, schema)
        }

        val driver = ValidationDriver(properties.toPropertyMap(), sr)

        val loaded = try {
            driver.loadSchema(schemaInputSource)
        } catch (ex: Exception) {
            if (schema.baseURI == null) {
                throw stepConfig.exception(XProcError.xcNotRelaxNG("Error loading schema"), ex)
            }
            throw stepConfig.exception(XProcError.xcNotRelaxNG(schema.baseURI!!, "Error loading schema"), ex)
        }

        if (!loaded) {
            val ex = listener.exceptions.firstOrNull()
            if (schema.baseURI == null) {
                throw stepConfig.exception(XProcError.xcNotRelaxNG("Error loading schema"), ex)
            }
            throw stepConfig.exception(XProcError.xcNotRelaxNG(schema.baseURI!!, "Error loading schema"), ex)
        }

        var valid = true
        if (!driver.validate(SaxProducer.adaptForJing(XmlToSax.asSaxProducer(stepConfig, document.value as XdmNode)))) {
            valid = false
            if (assertValid) {
                val xvrl = XProcDocument.ofXml(report.asXml(), stepConfig)
                throw stepConfig.exception(XProcError.xcNotSchemaValidRelaxNG(xvrl))
            }
        }

        if (valid && dtdAttributeValues) {
            val rngResolver = RelaxNGResolver(stepConfig.documentManager)
            val defaultValues: RelaxNGDefaultValues

            if (compact) {
                defaultValues = RNCDefaultValues(rngResolver, listener)
                sr = defaultValues.schemaReader()
                // Hack
                val srdr = StringReader(schema.value.underlyingValue.stringValue)
                schemaInputSource = InputSource(srdr)
                schemaInputSource.systemId = schema.baseURI.toString()
            } else {
                defaultValues = RNGDefaultValues(rngResolver, listener)
                sr = defaultValues.schemaReader()
                schemaInputSource = S9Api.xdmToInputSource(stepConfig, schema)
            }

            defaultValues.update(schemaInputSource)
            if (defaultValues.defaults.isNotEmpty()) {
                val builder = SaxonTreeBuilder(stepConfig)
                augment(builder, document.value as XdmNode, defaultValues.defaults)
                receiver.output("result", XProcDocument.ofXml(builder.result, stepConfig))
            } else {
                receiver.output("result", document)
            }
        } else {
            receiver.output("result", document)
        }

        receiver.output("report", XProcDocument.ofXml(report.asXml(), stepConfig))
    }

    private fun augment(builder: SaxonTreeBuilder, node: XdmNode, defaultAttributes: Map<QName, Map<QName, String>>) {
        when (node.getNodeKind()) {
            XdmNodeKind.ELEMENT -> {
                if (defaultAttributes.containsKey(node.nodeName)) {
                    val attmap = mutableMapOf<QName, String>()
                    for (attr in node.axisIterator(Axis.ATTRIBUTE)) {
                        attmap[attr.nodeName] = attr.stringValue
                    }
                    for ((attr, value) in defaultAttributes[node.nodeName]!!) {
                        if (!attmap.containsKey(attr)) {
                            attmap[attr] = value
                        }
                    }
                    builder.addStartElement(node, stepConfig.typeUtils.attributeMap(attmap))
                } else {
                    builder.addStartElement(node)
                }

                for (child in node.children()) {
                    augment(builder, child, defaultAttributes)
                }
                builder.addEndElement()
            }
            XdmNodeKind.DOCUMENT -> {
                builder.startDocument(node.baseURI)
                for (child in node.children()) {
                    augment(builder, child, defaultAttributes)
                }
                builder.endDocument()
            }
            else -> builder.addSubtree(node)
        }
    }

    override fun toString(): String = "p:validate-with-relax-ng"
}