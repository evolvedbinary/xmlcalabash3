package com.xmlcalabash.ext.existdb

import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.spi.AtomicStepManager
import com.xmlcalabash.spi.AtomicStepProvider
import net.sf.saxon.s9api.QName

class ExistDbStepProvider: AtomicStepProvider, AtomicStepManager {
    companion object {
        private val EXISTDB = QName(NsCx.namespace, "exist-db")
    }

    override fun create(): AtomicStepManager {
        return this
    }

    override fun createStep(params: StepParameters): () -> XProcStep {
        return when (params.stepType) {
            EXISTDB -> { -> ExistDbStep() }
            else -> throw XProcError.xiImpossible("Unexpected step type: ${params.stepType}").exception()
        }
    }

    override fun stepAvailable(stepType: QName): Boolean {
        return stepType == EXISTDB
    }

    override fun stepTypes(): Set<QName> {
        return setOf(EXISTDB)
    }
}