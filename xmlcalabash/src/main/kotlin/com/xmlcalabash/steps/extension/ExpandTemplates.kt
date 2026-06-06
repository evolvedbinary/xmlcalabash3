package com.xmlcalabash.steps.extension

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.runtime.LazyValue
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.ValueTemplateFilterXml
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SaxonApiException
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue

class ExpandTemplates(): AbstractAtomicStep()  {
    companion object {
        private val _variables = QName("variables")
    }

    lateinit var variables: Map<QName, XdmValue>

    override fun run() {
        super.run()

        val source = queues["source"]!!.first()
        variables = qnameMapBinding(_variables)

        val ctype = source.contentType ?: MediaType.XML
        val filter = ValueTemplateFilterXml(source.value as XdmNode, ctype, source.baseURI, stepParams.version)

        val bindings = mutableMapOf<QName, LazyValue>()
        for ((key, value) in variables) {
            val lazyValue = LazyValue(source, stepConfig, value)
            bindings[key] = lazyValue
        }

        val expanded = try {
            filter.expandValueTemplates(stepConfig, source, bindings)
        } catch (ex: Exception) {
            when (ex) {
                is SaxonApiException -> {
                    throw XProcError.xdValueTemplateError(ex.message ?: "").exception(ex)
                }
                else -> throw ex
            }
        }

        receiver.output("result", XProcDocument.ofXml(expanded, source.context, source.properties))
    }
}