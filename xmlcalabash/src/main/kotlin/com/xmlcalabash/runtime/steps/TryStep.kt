package com.xmlcalabash.runtime.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsFn
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.model.CompoundStepModel
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.NamespaceMap
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SaxonApiException
import net.sf.saxon.trans.XPathException

open class TryStep(config: XProcStepConfiguration, compound: CompoundStepModel): CompoundStep(config, compound) {
    override fun run() {
        if (runnables.isEmpty()) {
            instantiate()
        }

        var group: GroupStep? = null
        val catches = mutableListOf<TryCatchStep>()
        var finally: TryFinallyStep? = null
        stepsToRun.clear()

        for (step in runnables) {
            when (step) {
                is TryFinallyStep -> finally = step
                is TryCatchStep -> catches.add(step)
                is GroupStep -> group = step
                else -> stepsToRun.add(step)
            }
        }

        stepConfig.saxonConfig.newExecutionContext(stepConfig)
        try {
            head.runStep(this)

            var errorDocument: XProcDocument? = null
            var throwException: Exception? = null
            try {
                runSubpipeline()
                group!!.runStep(this)
            } catch (ex: Exception) {
                foot.cache.clear() // anything that accumulated so far? nope.

                throwException = ex
                val errorCode = if (ex is XProcException) {
                    ex.error.code
                } else {
                    stepConfig.warn { "Caught unwrapped exception: ${ex}" }
                    null
                }

                errorDocument = errorDocument(group!!, ex)

                for (step in catches) {
                    val codes = step.codes
                    if (codes.isEmpty() || (errorCode != null && codes.contains(errorCode))) {
                        throwException = null
                        try {
                            step.head.input("error", errorDocument)
                            step.runStep(this)
                        } catch (cex: Exception) {
                            throwException = cex
                        }
                        break;
                    }
                }
            }

            if (finally != null) {
                try {
                    if (errorDocument != null) {
                        finally.head.input("error", errorDocument)
                    }
                    finally.runStep(this)
                } catch (ex: Exception) {
                    if (throwException == null) {
                        throwException = ex
                    }
                }
            }

            if (throwException != null) {
                throw throwException
            }

            for ((port, documents) in foot.cache) {
                for (document in documents) {
                    foot.write(port, document)
                }
            }

            foot.runStep(this)
        } finally {
            stepConfig.saxonConfig.releaseExecutionContext()
        }
    }

    private fun getPrefix(map: Map<String,NamespaceUri>, uri: NamespaceUri, pref: String): String {
        for ((key, value) in map) {
            if (uri == value) {
                return key
            }
        }
        if (pref in map) {
            return S9Api.uniquePrefix(map.keys)
        }
        return pref
    }

    private fun errorDocument(step: AbstractStep, exception: Exception): XProcDocument {
        var codePrefix = ""
        var causeCode: QName? = null

        // There's some risk of namespace collisions here; should work around that but not today
        val bindings = mutableMapOf<String, NamespaceUri>()
        bindings["c"] = NsC.namespace
        bindings["cx"] = NsCx.namespace
        bindings["fnerr"] = NsFn.errorNamespace

        if (exception is XProcException) {
            val type = exception.error.stackTrace[0]?.stepType
            if (type != null) {
                if (type.prefix.isNotEmpty()) {
                    bindings[type.prefix] = type.namespaceUri
                }
            }

            if (exception.error.code.namespaceUri != NamespaceUri.NULL) {
                val pfx = if (exception.error.code.prefix == "") "errpfx" else exception.error.code.prefix
                codePrefix = getPrefix(bindings, exception.error.code.namespaceUri, pfx)
                bindings[codePrefix] = exception.error.code.namespaceUri
            }

            var cause: Throwable? = exception.cause
            if (cause is SaxonApiException && cause.cause is XPathException) {
                cause = cause.cause as XPathException
            }
            if (cause is XPathException && cause.errorCodeQName != null) {
                val pfx = if (cause.errorCodeQName.prefix == "") "cpfx" else cause.errorCodeQName.prefix
                val causePrefix = getPrefix(bindings, cause.errorCodeQName.namespaceUri, pfx)
                bindings[causePrefix] = cause.errorCodeQName.namespaceUri
                causeCode = QName(cause.errorCodeQName.namespaceUri, "${causePrefix}:${cause.errorCodeQName.localPart}")
            }
        }

        var nsmap = NamespaceMap.emptyMap()
        for ((key, value) in bindings) {
            nsmap = nsmap.put(key, value)
        }

        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(null)
        builder.addStartElement(NsC.errors, step.stepConfig.typeUtils.stringAttributeMap(emptyMap()), nsmap)

        val attr = mutableMapOf<String, String?>()

        if (exception is XProcException) {
            val error = exception.error
            attr["name"] = error.stackTrace[0]?.stepName
            attr["type"] = error.stackTrace[0]?.stepType.toString()

            if (codePrefix == "") {
                attr["code"] = error.code.toString()
            } else {
                attr["code"] = "${codePrefix}:${error.code.localName}"
            }

            attr["href"] = error.errorLocation.baseUri?.toString()
            if (error.errorLocation.lineNumber > 0) {
                attr["line"] = error.errorLocation.lineNumber.toString()
            }
            if (error.errorLocation.columnNumber > 0) {
                attr["column"] = error.errorLocation.columnNumber.toString()
            }

            if (causeCode != null) {
                attr["cause"] = "${causeCode}"
            }

            builder.addStartElement(NsC.error, step.stepConfig.typeUtils.stringAttributeMap(attr), nsmap)

            if (error.inputLocation.baseUri != null || error.inputLocation.lineNumber > 0) {
                attr.clear()
                attr["href"] = error.inputLocation.baseUri?.toString()
                if (error.inputLocation.lineNumber > 0) {
                    attr["line"] = error.inputLocation.lineNumber.toString()
                }
                if (error.inputLocation.columnNumber > 0) {
                    attr["column"] = error.inputLocation.columnNumber.toString()
                }
                builder.addStartElement(NsCx.inputLocation, step.stepConfig.typeUtils.stringAttributeMap(attr), nsmap)
                builder.addEndElement()
            }

            builder.addStartElement(NsCx.message)
            builder.addText(stepConfig.environment.errorExplanation.message(exception.error, false))
            builder.addEndElement()

            val explanation = stepConfig.environment.errorExplanation.explanation(exception.error)
            if (explanation.isNotBlank()) {
                builder.addStartElement(NsCx.explanation)
                builder.addText(explanation)
                builder.addEndElement()
            }

            if (exception.cause != null && exception.cause!!.message != null) {
                builder.addStartElement(NsCx.cause)
                builder.addText(exception.cause!!.message!!)
                builder.addEndElement()
            }

            if (error.details.isNotEmpty()) {
                for (doc in error.details) {
                    if (doc is XProcDocument) {
                        builder.addSubtree(doc.value)
                    }
                }
            }

            if (error.stackTrace.isNotEmpty()) {
                builder.addStartElement(NsCx.stackTrace, step.stepConfig.typeUtils.stringAttributeMap(emptyMap()), nsmap)

                for (frame in error.stackTrace) {
                    attr.clear()
                    attr["type"] = frame.stepType.toString()
                    attr["name"] = frame.stepName
                    builder.addStartElement(NsCx.stackFrame, step.stepConfig.typeUtils.stringAttributeMap(attr), nsmap)
                    builder.addEndElement()
                }

                builder.addEndElement()
            }

            builder.addEndElement()
        } else {
            builder.addStartElement(NsC.error, step.stepConfig.typeUtils.stringAttributeMap(attr), nsmap)
            builder.addText(exception.message ?: "")
            builder.addEndElement()
        }

        builder.addEndElement()
        builder.endDocument()
        return XProcDocument.ofXml(builder.result, step.stepConfig)
    }
}