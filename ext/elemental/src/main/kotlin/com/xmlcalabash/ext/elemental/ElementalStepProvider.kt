package com.xmlcalabash.ext.elemental

import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.spi.AtomicStepManager
import com.xmlcalabash.spi.AtomicStepProvider
import net.sf.saxon.s9api.QName

class ElementalStepProvider: AtomicStepProvider, AtomicStepManager {
    companion object {
        private val ELEMENTAL = QName(NsCx.namespace, "elemental")
    }

    override fun create(): AtomicStepManager {
        return this
    }

    override fun createStep(params: StepParameters): () -> XProcStep {
        return when (params.stepType) {
            ELEMENTAL -> { -> ElementalStep() }
            else -> throw XProcError.xiImpossible("Unexpected step type: ${params.stepType}").exception()
        }
    }

    override fun stepAvailable(stepType: QName): Boolean {
        return stepType == ELEMENTAL
    }

    override fun stepTypes(): Set<QName> {
        return setOf(ELEMENTAL)
    }
}