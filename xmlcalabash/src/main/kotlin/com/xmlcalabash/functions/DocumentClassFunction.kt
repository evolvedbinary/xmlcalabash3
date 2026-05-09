package com.xmlcalabash.functions

import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.util.MediaClassification
import net.sf.saxon.expr.Expression
import net.sf.saxon.expr.StaticContext
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.lib.ExtensionFunctionCall
import net.sf.saxon.lib.ExtensionFunctionDefinition
import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.om.GroundedValue
import net.sf.saxon.om.NodeInfo
import net.sf.saxon.om.Sequence
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.type.Type
import net.sf.saxon.value.EmptySequence
import net.sf.saxon.value.SequenceType
import net.sf.saxon.value.StringValue
import kotlin.sequences.map

class DocumentClassFunction(private val config: SaxonConfiguration): ExtensionFunctionDefinition() {
    override fun getFunctionQName(): StructuredQName {
        return StructuredQName("p", NsP.namespace, "document-class")
    }

    override fun getArgumentTypes(): Array<SequenceType> {
        return arrayOf(SequenceType.OPTIONAL_ITEM)
    }

    override fun getResultType(suppliedArgumentTypes: Array<out SequenceType>?): SequenceType {
        return SequenceType.OPTIONAL_ITEM
    }

    override fun makeCallExpression(): ExtensionFunctionCall {
        return DocClassImpl()
    }

    inner class DocClassImpl: ExtensionFunctionCall() {
        private var staticContext: StaticContext? = null

        override fun supplyStaticContext(context: StaticContext?, locationId: Int, arguments: Array<out Expression>?) {
            staticContext = context
        }

        override fun call(context: XPathContext?, arguments: Array<out Sequence>?): Sequence {
            val dynamicContext = config.getExecutionContext()

            val item = arguments!![0].head()
            val ctype = when (item) {
                is StringValue -> {
                    item.stringValue
                }
                is MapItem -> {
                    val key = XdmAtomicValue(Ns.contentType)
                    item.get(key.underlyingValue)?.stringValue
                }
                else -> {
                    val map = dynamicContext.getProperties(item)
                    val key = XdmAtomicValue(Ns.contentType)
                    map?.get(key)?.underlyingValue?.stringValue
                }
            }

            if (item == null) {
                return EmptySequence.getInstance()
            }

            try {
                val mtype = MediaType.parse(ctype)
                val mclass = mtype!!.classification()
                return StringValue(
                    when (mclass) {
                        MediaClassification.XML,
                        MediaClassification.HTML,
                        MediaClassification.XHTML,
                        MediaClassification.JSON,
                        MediaClassification.TEXT -> mclass.toString().lowercase()

                        MediaClassification.YAML,
                        MediaClassification.TOML -> "json"

                        MediaClassification.BINARY -> "binary"
                    }
                )
            } catch (_: Exception) {
                return EmptySequence.getInstance()
            }
        }
    }
}