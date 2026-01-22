package com.xmlcalabash.util

import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue

class AttributeValueTemplate(val template: ValueTemplate) {
    fun evaluate(stepConfig: XProcStepConfiguration, bindings: Map<QName, XdmValue>): String {
        return evaluate(stepConfig, null, bindings)
    }

    fun evaluate(stepConfig: XProcStepConfiguration, context: XdmNode?, bindings: Map<QName, XdmValue>): String {
        val sb = StringBuilder()
        for (index in template.value.indices) {
            if (index % 2 == 0) {
                sb.append(template.value[index])
            } else {
                val compiler = stepConfig.newXPathCompiler()
                for (name in bindings.keys) {
                    compiler.declareVariable(name)
                }

                val exec = compiler.compile(template.value[index])
                val select = exec.load()
                context?.let { select.contextItem = it }
                for ((name, value) in bindings) {
                    select.setVariable(name, value)
                }
                sb.append(select.evaluate().underlyingValue.stringValue)
            }
        }
        return sb.toString()
    }
}