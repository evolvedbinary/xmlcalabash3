package com.xmlcalabash.steps.internal

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.parameters.SelectStepParameters
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmNodeKind

open class SelectStep(val params: SelectStepParameters): AbstractAtomicStep() {
    override fun run() {
        super.run()
        val documents = queues["source"]!!

        for (document in documents) {
            params.select.reset()
            params.select.contextItem = document

            for (vname in params.select.variableRefs) {
                if (options.containsKey(vname)) {
                    params.select.setBinding(vname, options[vname]!!.value)
                }
            }

            val result = params.select.evaluate(params.select.stepConfig)

            for (doc in S9Api.makeDocuments(params.select.stepConfig, result)) {
                val props = DocumentProperties(document.properties)
                for ((name, value) in doc.properties.asMap()) {
                    props[name] = value
                }

                // If we changed document kinds or if the result wasn't a node
                // (for example, if it was an atomic value), discard serialization
                // But only for 3.2 at the moment because of PR #126
                if ((stepConfig.version >= 3.2 && result !is XdmNode)
                     || doc.contentClassification != document.contentClassification) {
                    props.remove(Ns.serialization)
                }

                receiver.output("result", doc.with(props))
            }
        }
    }

    override fun toString(): String = "cx:select"
}