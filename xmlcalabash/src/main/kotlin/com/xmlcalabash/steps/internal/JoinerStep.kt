package com.xmlcalabash.steps.internal

import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.api.Receiver
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import com.xmlcalabash.steps.AbstractAtomicStep

open class JoinerStep(): AbstractAtomicStep() {
    val inputPorts = mutableListOf<String>()
    val outputPorts = mutableListOf<String>()

    override fun setup(stepConfig: XProcStepConfiguration, receiver: Receiver, stepParams: RuntimeStepParameters) {
        super.setup(stepConfig, receiver, stepParams)
        inputPorts.clear()
        inputPorts.addAll(stepParams.inputs.keys)
        outputPorts.clear()
        outputPorts.addAll(stepParams.outputs.keys)
    }

    override fun run() {
        super.run()
        for (iport in inputPorts) {
            for (oport in outputPorts) {
                for (doc in queues[iport] ?: emptyList()) {
                    receiver.output(oport, doc)
                }
            }
        }
    }

    override fun reset() {
        super.reset()
    }

    override fun toString(): String = "cx:joiner"
}