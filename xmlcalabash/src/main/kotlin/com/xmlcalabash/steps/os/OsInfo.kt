package com.xmlcalabash.steps.os

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmEmptySequence
import net.sf.saxon.s9api.XdmValue

class OsInfo(): AbstractAtomicStep() {
    companion object {
        private val mapping = mapOf(
            "file.separator" to "file-separator",
            "path.separator" to "path-separator",
            "os.arch" to "os-architecture",
            "os.name" to "os-name",
            "os.version" to "os-version",
            "user.dir" to "cwd",
            "user.name" to "user-name",
            "user.home" to "user-home"
        )
    }

    var onlyStandardProperties = false

    init {
        expectedExtensionAttributes.addAll(listOf(NsCx.onlyStandard))
    }

    override fun extensionAttributes(attributes: Map<QName, String>, staticOptions: Map<QName, XdmValue>) {
        super.extensionAttributes(attributes, staticOptions)
        onlyStandardProperties = extensionAttributeBooleanValue(attributes, NsCx.onlyStandard, staticOptions)
    }

    override fun run() {
        super.run()

        val attr = mutableMapOf<QName, String?>()
        for ((pname, pvalue) in System.getProperties()) {
            val name = pname.toString()
            val value = pvalue.toString()

            if (mapping.contains(name)) {
                attr[QName(mapping[name])] = value
            }

            if (!onlyStandardProperties) {
                val qname = QName(NsCx.namespace, "cx:${name.replace(".", "-")}")
                attr[qname] = value
            }
        }

        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(null)
        builder.addStartElement(NsC.result, stepConfig.typeUtils.attributeMap(attr))

        for ((name, value) in System.getenv()) {
            builder.addStartElement(NsC.environment, stepConfig.typeUtils.attributeMap(mapOf(
                Ns.name to name,
                Ns.value to value
            )))
            builder.addEndElement()
        }

        builder.addEndElement()
        builder.endDocument()

        val properties = DocumentProperties(mapOf(Ns.baseUri to XdmEmptySequence.getInstance()))
        receiver.output("result", XProcDocument.ofXml(builder.result, stepConfig, properties))
    }

    override fun toString(): String = "p:os-info"
}