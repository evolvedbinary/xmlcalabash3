package com.xmlcalabash.functions

import com.xmlcalabash.datamodel.*
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.XProcRuntime
import com.xmlcalabash.util.BufferingReceiver
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.expr.Expression
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.lib.ExtensionFunctionCall
import net.sf.saxon.lib.ExtensionFunctionDefinition
import net.sf.saxon.ma.arrays.ArrayItem
import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.ma.map.MapType
import net.sf.saxon.om.GroundedValue
import net.sf.saxon.om.NodeInfo
import net.sf.saxon.om.Sequence
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.s9api.HostLanguage
import net.sf.saxon.s9api.Location
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmEmptySequence
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmNodeKind
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.s9api.XmlProcessingError
import net.sf.saxon.trans.XPathException
import net.sf.saxon.type.BuiltInAtomicType
import net.sf.saxon.value.Cardinality
import net.sf.saxon.value.EmptySequence
import net.sf.saxon.value.SequenceType

class PipelineFunction(private val decl: DeclareStepInstruction): ExtensionFunctionDefinition() {
    private val fname = StructuredQName(decl.type!!.prefix, decl.type!!.namespaceUri, decl.type!!.localName)
    private val inputs = decl.children.filterIsInstance<InputInstruction>()
    private val outputs = decl.children.filterIsInstance<OutputInstruction>()
    private val options = decl.children.filterIsInstance<OptionInstruction>()
    private var argumentTypes: Array<out SequenceType>? = null

    override fun getFunctionQName(): StructuredQName? {
        return fname
    }

    override fun getMinimumNumberOfArguments(): Int {
        return inputs.size
    }

    override fun getMaximumNumberOfArguments(): Int {
        if (options.isEmpty()) {
            return inputs.size
        }
        return inputs.size + 1
    }

    override fun getArgumentTypes(): Array<out SequenceType?>? {
        if (argumentTypes != null) {
            return argumentTypes!!
        }

        val argumentList = mutableListOf<SequenceType>()
        for (input in inputs) {
            val seqtype = portSequenceType(input)
            argumentList.add(seqtype)
        }

        if (options.isNotEmpty()) {
            val maptype = MapType(BuiltInAtomicType.ANY_ATOMIC, SequenceType.ANY_SEQUENCE)
            argumentList.add(SequenceType(maptype, Cardinality.fromOccurrenceIndicator("?")))
        }

        argumentTypes = arrayOf(*argumentList.toTypedArray())
        return argumentTypes
    }

    override fun getResultType(sequenceTypes: Array<out SequenceType?>?): SequenceType? {
        return SequenceType.SINGLE_ITEM
    }

    override fun makeCallExpression(): ExtensionFunctionCall? {
        return FunctionCall()
    }

    private fun portSequenceType(binding: PortBindingContainer): SequenceType {
        val seqtype = if (binding.contentTypes.none { it.inclusive }) {
            SequenceType.SINGLE_ITEM
        } else {
            var onlyNodes = true
            for (ctype in binding.contentTypes.filter { it.inclusive }) {
                onlyNodes =
                    onlyNodes && (ctype.classification() in listOf(MediaClassification.XML, MediaClassification.XHTML, MediaClassification.HTML, MediaClassification.TEXT))
            }
            if (onlyNodes) {
                SequenceType.SINGLE_NODE
            } else {
                SequenceType.SINGLE_ITEM
            }
        }

        if (binding.sequence == true) {
            if (seqtype == SequenceType.SINGLE_ITEM) {
                return SequenceType.ANY_SEQUENCE
            }
            return SequenceType.NODE_SEQUENCE
        }

        return seqtype
    }

    inner class FunctionCall(): ExtensionFunctionCall() {
        private var runtime: XProcRuntime? = null

        override fun call(context: XPathContext, sequences: Array<out Sequence?>?): Sequence? {
            if (runtime == null) {
                runtime = decl.runtime()
            }

            val exec = runtime!!.executable()
            val receiver = BufferingReceiver()
            exec.receiver = receiver

            if (sequences != null) {
                for ((index, input) in inputs.withIndex()) {
                    val items = sequences.elementAt(index)!!.materialize()
                    for (itemindex in 0..<items.length) {
                        val item = items.itemAt(itemindex)
                        when (item) {
                            is NodeInfo -> {
                                val node = XdmNode(item)
                                val doc = if (node.nodeKind == XdmNodeKind.DOCUMENT) {
                                    XProcDocument.ofXml(node, decl.stepConfig)
                                } else {
                                    val builder = SaxonTreeBuilder(node.processor)
                                    builder.startDocument(node.baseURI)
                                    builder.addSubtree(node)
                                    builder.endDocument()
                                    XProcDocument.ofXml(builder.result, decl.stepConfig)
                                }
                                exec.input(input.port, doc)
                            }
                            is MapItem -> {
                                val map = decl.stepConfig.typeUtils.asXdmMap(item)
                                val doc = XProcDocument.ofValue(map, decl.stepConfig)
                                exec.input(input.port, doc)
                            }
                            is ArrayItem -> {
                                val array = decl.stepConfig.typeUtils.asXdmArray(item)
                                val doc = XProcDocument.ofValue(array, decl.stepConfig)
                                exec.input(input.port, doc)
                            }
                            is GroundedValue -> {
                                val doc = XProcDocument.ofValue(XdmValue.wrap(item), decl.stepConfig)
                                exec.input(input.port, doc)
                            }

                            else -> throw IllegalArgumentException("Failed to convert item to input: ${item}")
                        }
                    }
                }

                if (sequences.size > inputs.size) {
                    val item = sequences.last()
                    if (item is MapItem) {
                        val map = decl.stepConfig.typeUtils.asMap(decl.stepConfig.typeUtils.forceQNameKeys(item))
                        for ((name, value) in map) {
                            exec.option(name, XProcDocument.ofValue(value, decl.stepConfig))
                        }
                    }
                }
            }

            try {
                exec.run()
            } catch (ex: Exception) {
                // Wrap the exception in an XPathException so that try/catch in XQuery or XSLT will work
                when (ex) {
                    is XProcException -> {
                        val error = ex.error
                        val sb = StringBuilder()
                        when (error.details.size) {
                            0 -> Unit
                            1 -> sb.append(errorDetail(error.details[0]))
                            else -> {
                                sb.append("[")
                                for (index in error.details.indices) {
                                    if (index > 0) {
                                        sb.append(", ")
                                    }
                                    sb.append(errorDetail(error.details[index]))
                                }
                                sb.append("]")
                            }
                        }
                        // Passing in null causes Saxon to report a more useful location...
                        val loc = null // error.location.asSaxonLocation()
                        val perr = FakeXmlProcessingError(error.code, sb.toString(), error.exception(), loc)
                        throw XPathException.fromXmlProcessingError(perr)
                    }
                    else -> {
                        throw XPathException("Pipeline execution failed", ex)
                    }
                }
            }

            var map = XdmMap()
            for (output in outputs) {
                var value: XdmValue = XdmEmptySequence.getInstance()
                if (output.port in receiver.outputs) {
                    for (document in receiver.outputs[output.port]!!) {
                        value = value.append(document.value)
                    }
                }
                map = map.put(XdmAtomicValue(output.port), value)
            }

            return map.underlyingValue
        }

        private fun errorDetail(detail: Any): String {
            when (detail) {
                is XProcBinaryDocument -> {
                    return "[binary document]"
                }
                is XProcDocument -> {
                    return detail.value.toString()
                }
                else -> {
                    return detail.toString()
                }
            }
        }
    }

    private class FakeXmlProcessingError(val code: QName, val msg: String, val ex: Throwable?, val loc: Location?): XmlProcessingError {
        private var alreadyReported = false

        override fun getHostLanguage(): HostLanguage {
            return HostLanguage.UNKNOWN
        }

        override fun isStaticError(): Boolean {
            return false
        }

        override fun isTypeError(): Boolean {
            return false
        }

        override fun getErrorCode(): QName {
            return code
        }

        override fun getMessage(): String {
            return msg
        }

        override fun getLocation(): Location? {
            return loc
        }

        override fun getFailingExpression(): Expression? {
            return null
        }

        override fun isWarning(): Boolean {
            return false
        }

        override fun getPath(): String? {
            return null
        }

        override fun getCause(): Throwable? {
            return ex
        }

        override fun asWarning(): XmlProcessingError? {
            return null
        }

        override fun setTerminationMessage(s: String?) {
            throw IllegalStateException("You cannot set the termination message")
        }

        override fun getTerminationMessage(): String? {
            return message
        }

        override fun isAlreadyReported(): Boolean {
            return alreadyReported
        }

        override fun setAlreadyReported(reported: Boolean) {
            alreadyReported = reported
        }
    }
}