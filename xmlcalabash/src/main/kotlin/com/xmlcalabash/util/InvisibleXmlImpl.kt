package com.xmlcalabash.util

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import org.nineml.coffeefilter.InvisibleXml
import org.nineml.coffeegrinder.exceptions.TreeWalkerException

open class InvisibleXmlImpl(val stepConfig: XProcStepConfiguration, val prefer: String) {
    companion object {
        private val nineml = mutableListOf<InvisibleXmlImpl?>()
        private val blitz = mutableListOf<InvisibleXmlImpl?>()
        private var loggedNineML = false
        private var loggedMarkupBlitz = false
    }

    private var usingImpl = ""
    open fun parse(grammar: XdmNode, input: String, failOnError: Boolean, parameters: Map<QName, XdmValue>): XProcDocument {
        val impl: InvisibleXmlImpl = loadImpl()

        try {
            return impl.parse(grammar, input, failOnError, parameters)
        } catch (ex: Exception) {
            if (ex is XProcException) {
                throw ex
            }
            // This is a terrible hack; the NineML library really needs to be improved!
            if (ex is NullPointerException && usingImpl == "nineml") {
                throw stepConfig.exception(XProcError.xcInvalidIxmlGrammar(), ex)
            }
            throw stepConfig.exception(XProcError.xcInvisibleXmlParseFailed(), ex)
        }
    }

    open fun parse(grammar: String?, input: String, failOnError: Boolean, parameters: Map<QName, XdmValue>): XProcDocument {
        val impl: InvisibleXmlImpl = loadImpl()

        try {
            return impl.parse(grammar, input, failOnError, parameters)
        } catch (ex: Exception) {
            if (ex is XProcException) {
                throw ex
            }
            // This is a terrible hack; the NineML library really needs to be improved!
            if (ex is NullPointerException && usingImpl == "nineml") {
                // We can't get the failed parse from here. So we parse it again. Grumble.
                val invisibleXml = InvisibleXml()
                val parser = if (grammar != null) {
                    invisibleXml.getParserFromIxml(grammar)
                } else {
                    invisibleXml.getParser()
                }
                if (parser.failedParse != null) {
                    val builder = stepConfig.processor.newDocumentBuilder()
                    builder.isLineNumbering = true
                    val bch = builder.newBuildingContentHandler()
                    try {
                        parser.failedParse!!.getTree(bch)
                        throw stepConfig.exception(XProcError.xcInvalidIxmlGrammar(bch.documentNode))
                    } catch (_: TreeWalkerException) {
                        if (parser.exception != null) {
                            throw stepConfig.exception(XProcError.xcInvalidIxmlGrammar(parser.exception!!.message ?: parser.exception!!.toString()))
                        }
                        throw stepConfig.exception(XProcError.xcInvalidIxmlGrammar())
                    }
                }

                throw stepConfig.exception(XProcError.xcInvalidIxmlGrammar(), ex)
            }
            throw stepConfig.exception(XProcError.xcInvisibleXmlParseFailed(), ex)
        }
    }

    private fun loadImpl(): InvisibleXmlImpl {
        usingImpl = ""
        var impl: InvisibleXmlImpl? = null

        try {
            when (prefer) {
                "nineml" -> {
                    usingImpl = "nineml"
                    impl = loadNineML()
                    if (impl != null) {
                        if (!loggedNineML || loggedMarkupBlitz) {
                            stepConfig.debug { "Using NineML for p:invisible-xml" }
                            loggedNineML = true
                        }
                    }
                }

                "blitz", "markup-blitz" -> {
                    usingImpl = "markup-blitz"
                    impl = loadMarkupBlitz()
                    if (impl != null) {
                        if (!loggedMarkupBlitz || loggedNineML) {
                            stepConfig.debug { "Using Markup Blitz for p:invisible-xml" }
                            loggedMarkupBlitz = true
                        }
                    }
                }

                else -> {
                    throw stepConfig.exception(XProcError.xdStepFailed("Unknown Invisible XML implementation: ${prefer}"))
                }
            }
        } catch (ex: Throwable) { // Throwable to catch NoClassDefFoundException...
            if (ex is XProcException) {
                throw ex
            }
            if (usingImpl == "nineml") {
                stepConfig.warn { "Failed to load NineML for Invisible XML parsing" }
            } else {
                stepConfig.warn { "Failed to load Markup Blitz for Invisible XML parsing" }
            }
        }

        if (impl == null) {
            // Try the other one
            try {
                if (usingImpl == "nineml") {
                    usingImpl = "markup-blitz"
                    impl = loadMarkupBlitz()
                    if (impl != null) {
                        if (!loggedMarkupBlitz || loggedNineML) {
                            stepConfig.debug { "Using Markup Blitz for p:invisible-xml" }
                            loggedMarkupBlitz = true
                        }
                    }
                } else {
                    usingImpl = "nineml"
                    impl = loadNineML()
                    if (impl != null) {
                        if (!loggedNineML || loggedMarkupBlitz) {
                            stepConfig.debug { "Using NineML for p:invisible-xml" }
                            loggedNineML = true
                        }
                    }
                }
            } catch (ex: Throwable) {
                if (ex is XProcException) {
                    throw ex
                }
            }
        }

        if (impl == null) {
            throw stepConfig.exception(XProcError.xdStepFailed("No Invisible XML implementation available"))
        }

        return impl
    }

    private fun loadNineML(): InvisibleXmlImpl? {
        if (nineml.isEmpty()) {
            val impl = InvisibleXmlNineML(stepConfig)
            nineml.add(impl)
        }
        return nineml.first()
    }

    private fun loadMarkupBlitz(): InvisibleXmlImpl? {
        if (blitz.isEmpty()) {
            val impl = InvisibleXmlMarkupBlitz(stepConfig)
            blitz.add(impl)
        }
        return blitz.first()
    }
}