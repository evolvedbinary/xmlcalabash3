package com.xmlcalabash.steps.extension

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.steps.AbstractAtomicStep
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmValue

class MergePropertiesStep(): AbstractAtomicStep() {

    override fun run() {
        super.run()

        val source = queues["source"]!!.first()
        val alternate = queues["alternate"]!!.first()
        val mergeMaps = booleanBinding(QName("merge-maps"))!!

        val newProperties = DocumentProperties()
        val sourceProperties = source.properties.asMap()
        for ((name, value) in alternate.properties.asMap()) {
            if (sourceProperties.containsKey(name)) {
                val sourceMap = sourceProperties[name]!!
                if (mergeMaps && value is XdmMap && sourceMap is XdmMap) {
                    val combinedMap = mutableMapOf<XdmAtomicValue, XdmValue>()
                    combinedMap.putAll(stepConfig.typeUtils.asGenericMap(sourceMap))
                    for ((mname, mvalue) in stepConfig.typeUtils.asGenericMap(value)) {
                        if (mname !in combinedMap) {
                            combinedMap[mname] = mvalue
                        }
                    }
                    newProperties[name] = stepConfig.typeUtils.asGenericXdmMap(combinedMap)
                } else {
                    newProperties[name] = sourceProperties[name]
                }
            } else {
                newProperties[name] = value
            }
        }

        receiver.output("result", source.with(newProperties))
    }


    override fun toString(): String = "cx:merge-properties"
}