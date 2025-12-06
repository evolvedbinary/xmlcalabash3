package com.xmlcalabash.steps

import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.namespace.Ns
import net.sf.saxon.s9api.Serializer
import net.sf.saxon.s9api.XdmArray
import net.sf.saxon.s9api.XdmItem
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import java.io.ByteArrayOutputStream

open class MessageStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val test = booleanBinding(Ns.test)!!
        if (test) {
            val value = options[Ns.select]!!.value
            for (item in value.iterator()) {
                stepConfig.info { serialize(item) }
            }
        }

        for (doc in queues["source"]!!) {
            receiver.output("result", doc)
        }
    }

    private fun serialize(item: XdmItem): String {
        when (item) {
            is XdmMap, is XdmArray -> {
                val baos = ByteArrayOutputStream()
                val serializer = stepConfig.processor.newSerializer(baos)
                serializer.setOutputProperty(Serializer.Property.METHOD, "json")
                serializer.setOutputProperty(Serializer.Property.INDENT, "yes")
                serializer.serializeXdmValue(item)
                return baos.toString("UTF-8")
            }
            is XdmNode -> {
                val baos = ByteArrayOutputStream()
                val serializer = stepConfig.processor.newSerializer(baos)
                serializer.setOutputProperty(Serializer.Property.METHOD, "xml")
                serializer.setOutputProperty(Serializer.Property.INDENT, "yes")
                serializer.serializeXdmValue(item)
                return baos.toString("UTF-8")
            }
            else -> {
                return item.stringValue
            }
        }
    }

    override fun toString(): String = "p:message"
}