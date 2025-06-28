package com.xmlcalabash.runtime.steps

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.model.CompoundStepModel
import com.xmlcalabash.util.UriUtils

class PipelineStep(config: XProcStepConfiguration, compound: CompoundStepModel): GroupStep(config,compound) {
    override fun runStep(parent: CompoundStep) {
        if (runnables.isEmpty()) {
            instantiate()
        }

        for (step in runnables.filterIsInstance<AtomicOptionStep>()) {
            if (step.externalName !in params.options) {
                // I don't think this can happen, but just in case...
                // If you attempt to pass an option that isn't expected on the step, just ignore it
                continue
            }

            if (step.externalName in staticOptions) {
                val details = staticOptions[step.externalName]!!
                var value = details.staticValue.evaluate(details.stepConfig)

                value = UriUtils.patchUriValue(stepConfig, value, details.asType)

                val document = XProcDocument.ofValue(value, details.stepConfig, MediaType.OCTET_STREAM, DocumentProperties())
                step.externalValue = document
            } else if (step.externalName in head.options) {
                val list = head.options[step.externalName]!!
                if (list.size != 1) {
                    throw stepConfig.exception(XProcError.xiImpossible("Dynamic option is a collection?"))
                }
                step.externalValue = list.first()
            }
        }

        super.runStep(parent)
    }
}