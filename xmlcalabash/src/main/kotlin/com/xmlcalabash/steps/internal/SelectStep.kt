package com.xmlcalabash.steps.internal

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.parameters.SelectStepParameters
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.S9Api

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

                if (doc.contentClassification != document.contentClassification) {
                    props.remove(Ns.serialization)
                }
                receiver.output("result", doc.with(props))
            }
        }
    }

    override fun toString(): String = "cx:select"
}