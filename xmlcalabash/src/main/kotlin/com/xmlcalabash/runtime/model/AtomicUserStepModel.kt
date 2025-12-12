package com.xmlcalabash.runtime.model

import com.xmlcalabash.datamodel.StaticOptionDetails
import com.xmlcalabash.graph.AtomicModel
import com.xmlcalabash.graph.Model
import com.xmlcalabash.runtime.XProcRuntime
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.api.RuntimePort
import com.xmlcalabash.runtime.steps.AbstractStep
import com.xmlcalabash.runtime.steps.CompoundStep
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.ValidationMode

class AtomicUserStepModel(runtime: XProcRuntime, model: AtomicModel, private val impl: CompoundStepModel): AtomicStepModel(runtime, model) {
    private var _type = type
    val stepType: QName
        get() = _type

    override fun initialize(model: Model) {
        super.initialize(model)

        impl.userStep = this
        extensionAttributes.putAll(model.step.extensionAttributes)

        for ((name, moption) in model.options) {
            if (moption.staticValue != null) {
                val details = StaticOptionDetails(model.step.stepConfig, name, moption.asType, moption.values, moption.staticValue!!)
                staticOptions[name] = details
            }
        }
    }

    override fun runnable(config: XProcStepConfiguration): () -> AbstractStep {
        val step = synchronized(impl) {
            impl._inputs.clear()
            impl._inputs.putAll(inputs)

            for ((name, option) in options) {
                impl._inputs["Q{${name.namespaceUri}}${name.localName}"] = RuntimePort(option)
            }

            val headPorts = impl.head.outputs.keys.toList()
            impl.head._outputs.clear()
            for (name in headPorts) {
                impl.head._outputs[name] = impl.inputs[name]!!
            }

            val instance = CompoundStep.newInstance(config, impl)
            instance.stepType = stepType
            instance.stepName = name

            // Validation mode on an instance overrides the default...
            if (stepConfig.validationMode != ValidationMode.DEFAULT) {
                instance.head.stepConfig.validationMode = stepConfig.validationMode
            }

            instance
        }

        for ((port, flange) in outputs) {
            if (flange.weldedShut) {
                step.head.weldedPorts.add(port)
            }
        }

        for ((port, flange) in inputs) {
            if (flange.unbound) {
                step.head.unboundInputs.add(port)
            }
            if (flange.weldedShut) {
                step.head.weldedPorts.add(port)
            }
        }

        step.staticOptions.putAll(staticOptions)
        step.head.staticOptions.putAll(staticOptions)
        step._threadGroup = threadGroup

        return { step }
    }

    override fun toString(): String {
        return "${stepType}/${name}"
    }
}