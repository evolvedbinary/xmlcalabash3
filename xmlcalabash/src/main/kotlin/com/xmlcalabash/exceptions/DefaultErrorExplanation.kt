package com.xmlcalabash.exceptions

import com.xmlcalabash.api.MessageReporter
import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.util.*
import net.sf.saxon.s9api.*
import org.apache.jena.sparql.function.library.localname
import org.xml.sax.InputSource
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import javax.xml.transform.sax.SAXSource

class DefaultErrorExplanation(override val reporter: MessageReporter): ErrorExplanation {
    override var showStacktrace = false
    override var messageWidth = 72;
    private val initialCwd = UriUtils.cwdAsUri()
    private val nl: String

    companion object {
        private var loaded = false
        private val messages = mutableListOf<ErrorExplanationTemplate>()
    }

    init {
        if (Urify.isWindows) {
            nl = "\r\n"
        } else {
            nl = "\n"
        }

        if (!loaded) {
            val uc = System.getProperty("user.country", "")
            val ul = System.getProperty("user.language", "")
            if (uc != "" && ul != "") {
                DefaultErrorExplanation::class.java.getResourceAsStream("/com/xmlcalabash/explain-errors.${ul}_${uc}.txt")
                    ?.let { loadExplanations(it) }
            }

            if (!loaded && ul != "") {
                DefaultErrorExplanation::class.java.getResourceAsStream("/com/xmlcalabash/explain-errors.${ul}.txt")
                    ?.let { loadExplanations(it) }
            }

            if (!loaded) {
                DefaultErrorExplanation::class.java.getResourceAsStream("/com/xmlcalabash/explain-errors.txt")
                    ?.let { loadExplanations(it) }
            }

            loaded = true
        }
    }

    override fun message(error: XProcError, includeDetails: Boolean): String {
        val errorMessage = if (error.code.namespaceUri in listOf(NsErr.namespace, NsCx.errorNamespace)) {
            val message = template(error.code, error.variant, error.details.size).message
            substitute(message, *error.details)
        } else {
            null
        }

        if (!includeDetails) {
            return errorMessage ?: "(no message for error)"
        }

        val details = mutableListOf<String>()
        for (detail in error.details) {
            // If this error was caused by p:error, then the detail will be a document sequence.
            // In all other cases, the detail will have been handled by substitute() above.
            if (detail is XProcDocument) {
                details.add(showDetail(detail, error.details.size > 1))
            }
        }

        val relerr = relativeLocation(error.errorLocation)
        val relinp = relativeLocation(error.inputLocation)
        val sb = StringBuilder()
        if (relerr != null) {
            sb.append("${error.code} at ${relerr}")
            if (relinp != null) {
                sb.append(" (in ${relinp})")
            }
        } else {
            if (relinp != null) {
                sb.append("${error.code} in ${relinp}")
            } else {
                sb.append("${error.code}")
            }
        }

        if (errorMessage == null) {
            if (details.isNotEmpty()
                && !details.first().contains("\n")
                && !details.first().contains("<")) {
                sb.append(": ").append(details.removeFirst())
            }
        } else {
            sb.append(": ").append(errorMessage)
        }

        val seen = mutableSetOf<Throwable>()
        var cause = error.throwable
        while (cause != null) {
            seen.add(cause)
            if (cause.message != null) {
                sb.append("\n").append("   cause: ${cause.message}")
            } else {
                sb.append("\n").append("   cause: ${cause}")
            }
            cause = cause.cause
            if (seen.contains(cause)) {
                cause = null
            }
        }

        for (detail in details) {
            sb.append("\n").append(detail)
        }

        for (detail in error.moreDetails) {
            sb.append("\n").append(detail)
        }

        return sb.toString()
    }

    private fun relativeLocation(location: Location): String? {
        //println("relativeLocation(${location.baseUri}; ${initialCwd})")
        if (location.baseUri != null) {
            val rel = UriUtils.makeRelativeTo(initialCwd, location.baseUri).toString()
            if (location.lineNumber > 0) {
                if (location.columnNumber > 0) {
                    return "${rel}:${location.lineNumber}:${location.columnNumber}"
                }
                return "${rel}:${location.lineNumber}"
            }
            return rel
        }
        return null
    }

    override fun report(error: XProcError) {
        if (error.reports.isNotEmpty()) {
            for (report in error.reports) {
                // Having to make a new report from a report in order to turn on details seems ... suboptimal
                reporter.report(Verbosity.ERROR) { Report(Verbosity.ERROR) { message(error, true) } }
            }
        } else {
            reporter.report(Verbosity.ERROR) { Report(Verbosity.ERROR) { message(error, true) } }
        }

        if (showStacktrace && error.stackTrace.isNotEmpty()) {
            reporter.report(Verbosity.ERROR) { Report(Verbosity.ERROR) { "Stack trace:" } }
            var count = error.stackTrace.size
            for (frame in error.stackTrace) {
                if (frame.stepName.startsWith('!')) {
                    reporter.report(Verbosity.ERROR) { Report(Verbosity.ERROR) { "\t[${count}] <${frame.stepType}>" } }
                } else {
                    reporter.report(Verbosity.ERROR) { Report(Verbosity.ERROR) { "\t[${count}] <${frame.stepType} name=\"${frame.stepName}\">" } }
                }
                count--
            }
        }
    }

    override fun explanation(error: XProcError): String {
        val template = template(error.code, error.variant, error.details.size)
        if (template.found) {
            val message = template.explanation
            return wrapped(substitute(message, *error.details))
        }
        return ""
    }

    override fun reportExplanation(error: XProcError) {
        val message = explanation(error)
        if (message.isNotEmpty()) {
            reporter.report(Verbosity.ERROR) { Report(Verbosity.ERROR) { message } }
        }
    }

    private fun template(code: QName, variant: Int, count: Int): ErrorExplanationTemplate {
        val clark = code.clarkName
        val templates = messages.filter { it.code == clark }.filter { it.variant == variant }.filter { it.cardinality <= count }
        /* for debugging
        val templates = mutableListOf<ErrorExplanationTemplate>()
        for (message in messages) {
            if (message.code == clark && message.variant == variant && message.cardinality <= count) {
                templates.add(message)
            } else {
                println("NO: ${message.code}, ${message.variant}, ${message.cardinality}")
            }
        }
         */

        if (templates.isEmpty()) {
            return ErrorExplanationTemplate(clark, 1, "[No explanatory message for ${code}]", "[No explanation for ${code}]", false)
        } else {
            var maxCardinality = -1
            var maxTemplate: ErrorExplanationTemplate? = null
            for (template in templates) {
                if (template.cardinality == count) {
                    return template
                }
                if (template.cardinality > maxCardinality) {
                    maxCardinality = template.cardinality
                    maxTemplate = template
                }
            }
            return maxTemplate!!
        }
    }

    private fun substitute(text: String, vararg details: Any): String {
        // Multiline regex matching is fine, but groups only match up to the first newline ???
        val re = Regex("^(.*?)\\$(\\d+)(.*)$")
        val sb = StringBuilder()
        for ((index, line) in text.split("\n").iterator().withIndex()) {
            if (index > 0) {
                sb.append("\n")
            }
            var message = line
            var match = re.find(message)
            while (match != null) {
                val (preamble, token, postamble) = match.destructured
                val number = token.toInt() - 1

                sb.append(preamble)
                if (number < details.size) {
                    sb.append(stringify(details[number]))
                }

                message = postamble
                match = re.find(message)
            }
            sb.append(message)
        }
        return sb.toString()
    }

    private fun stringify(any: Any): String {
        when (any) {
            is List<*> -> {
                val sb = StringBuilder()
                sb.append("[")
                for (index in any.indices) {
                    if (index > 0) {
                        sb.append(", ")
                    }
                    sb.append(stringify(any[index]!!))
                }
                sb.append("]")
                return sb.toString()
            }
            is XdmNode -> {
                if (any.nodeKind == XdmNodeKind.ELEMENT || any.nodeKind == XdmNodeKind.DOCUMENT) {
                    val baos = ByteArrayOutputStream()
                    val serializer = any.processor.newSerializer(baos)
                    serializer.setOutputProperty(Serializer.Property.METHOD, "xml")
                    serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes")
                    serializer.setOutputProperty(Serializer.Property.INDENT, "yes")
                    serializer.serializeNode(any)
                    return "\n${baos.toString(StandardCharsets.UTF_8)}"
                } else {
                    return any.toString()
                }
            }
            else -> return any.toString()
        }
    }

    private fun loadExplanations(stream: InputStream) {
        val namespaces = mutableMapOf<String,String>()
        var code = ""
        var variant = 1
        var message = ""
        var explanation = ""

        namespaces[""] = "http://xmlcalabash.com/ns/ERROR"

        val reader = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8))
        for (line in reader.readLines()) {
            if (line == "") {
                if (code != "") {
                    messages.add(ErrorExplanationTemplate(code, variant, message, explanation))
                    code = ""
                    variant = 1
                    message = ""
                    explanation = ""
                }
            } else {
                var match = Regex("^\\s*namespace\\s+(\\S+)\\s*=\\s*(.*)\\s*").find(line)
                if (match != null) {
                    val (prefix, uri) = match.destructured
                    namespaces[prefix] = uri
                } else {
                    var qcode: QName? = null
                    if (code == "") {
                        match = Regex("^([^{]\\S*):(\\S+)\\s*/\\s*(\\d+)\\s*$").find(line)
                        if (match != null) {
                            val (prefix, localName, vcode) = match.destructured
                            variant = vcode.toInt()
                            if (namespaces.containsKey(prefix)) {
                                qcode = QName(prefix, namespaces[prefix], localName)
                            } else {
                                // TODO: log error
                                qcode = QName("", namespaces[""], localName)
                            }
                        } else {
                            match = Regex("^([^{]\\S*):(\\S+)\\s*$").find(line)
                            if (match != null) {
                                val (prefix, localName) = match.destructured
                                variant = 1
                                if (namespaces.containsKey(prefix)) {
                                    qcode = QName(prefix, namespaces[prefix], localName)
                                } else {
                                    // TODO: log error
                                    qcode = QName("", namespaces[""], localName)
                                }
                            } else {
                                match = Regex("^([^:\\s]+)\\s*/\\s*(\\d+)\\s*$").find(line)
                                if (match != null) {
                                    val (bcode, vcode) = match.destructured
                                    variant = vcode.toInt()
                                    qcode = QName(NsErr.namespace, bcode)
                                } else {
                                    match = Regex("^([^:\\s]+)\\s*$").find(line)
                                    if (match != null) {
                                        variant = 1
                                        qcode = QName(NsErr.namespace, line)
                                    } else {
                                        match = Regex("^\\{(.*)}\\s*(\\S+)\\s*/\\s*(\\d+)\\s*$").find(line)
                                        if (match != null) {
                                            val (uri, localName, vcode) = match.destructured
                                            variant = vcode.toInt()
                                            qcode = QName(uri, localName)
                                        } else {
                                            match = Regex("^\\{(.*)}\\s*(\\S+)\\s*$").find(line)
                                            if (match != null) {
                                                val (uri, localName) = match.destructured
                                                variant = 1
                                                qcode = QName(uri, localName)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (qcode != null) {
                            code = qcode.clarkName
                        } else {
                            code = "????"
                        }
                    } else {
                        if (message == "") {
                            message = line
                        } else {
                            if (explanation == "") {
                                explanation = line
                            } else {
                                explanation += "\n${line}"
                            }
                        }
                    }
                }
           }
        }

        if (code != "") {
            messages.add(ErrorExplanationTemplate(code, variant, message, explanation))
        }
    }

    private fun showDetail(doc: XProcDocument, alwaysSerialize: Boolean): String {
        var message: String? = null
        if (doc is XProcBinaryDocument) {
            // This can't actually happen, but just in case it does...
            message = "...binary message cannot be displayed..."
        } else if (doc.contentType?.classification() == MediaClassification.TEXT) {
            message = doc.value.underlyingValue.stringValue
        } else if (doc.contentType?.classification() == MediaClassification.XML) {
            val root = if (S9Api.isTextDocument(doc.value as XdmNode)) {
                doc.value as XdmNode
            } else {
                S9Api.documentElement(doc.value as XdmNode)
            }

            if (root.nodeName in listOf(NsXvrl.reports, NsXvrl.report)) {
                return showXvrl(doc)
            }

            var markup = alwaysSerialize
            for (node in root.axisIterator(Axis.CHILD)) {
                if (node.nodeKind != XdmNodeKind.TEXT) {
                    markup = true
                    break
                }
            }
            if (!markup) {
                message = root.underlyingValue.stringValue
            }
        }

        if (message == null) {
            val baos = ByteArrayOutputStream()
            val writer = DocumentWriter(doc, baos)
            writer.set(Ns.omitXmlDeclaration, "true")
            writer.set(Ns.indent, "true")
            writer.write()
            message = baos.toString(reporter.messagePrinter.encoding)
        }

        if (message == null) {
            message = "...no explanation provided..."
        }

        return message.trim()
    }

    private fun showXvrl(doc: XProcDocument): String {
        val sb = StringBuilder()
        val node = doc.value as XdmNode

        // This is a slightly odd context, we don't have access to a step configuration
        // so we can't call on the document manager to load the stylesheet. But we
        // can make sure we get the same one...
        val stylesheet = InternalDocumentResolver.URI_MAP[InternalDocumentResolver.XVRL_TO_TEXT]!!

        var styleStream = DefaultErrorExplanation::class.java.getResourceAsStream(stylesheet)
        var styleSource = SAXSource(InputSource(styleStream))
        styleSource.systemId = "/com/xmlcalabash/format-report.xsl"
        var xsltCompiler = node.processor.newXsltCompiler()
        xsltCompiler.isSchemaAware = node.processor.isSchemaAware
        var xsltExec = xsltCompiler.compile(styleSource)

        var transformer = xsltExec.load30()
        transformer.globalContextItem = node
        val xmlResult = XdmDestination()
        transformer.applyTemplates(node.asSource(), xmlResult)

        val baos = ByteArrayOutputStream()
        val writer = DocumentWriter(XProcDocument.ofText(xmlResult.xdmNode, doc.context), baos)
        writer.set(Ns.method, "text")
        writer.set(Ns.encoding, reporter.messagePrinter.encoding)
        writer.write()
        val text = baos.toString(StandardCharsets.UTF_8)
        return text
    }

    private fun wrapped(text: String): String {
        val sb = StringBuilder()
        var width = 0;
        var oneline = text.replace("\n", " ")
        var pos = oneline.indexOf(" ");
        while (pos >= 0) {
            val seg = oneline.take(pos + 1);
            oneline = oneline.substring(pos + 1)
            sb.append(seg)
            width += seg.length
            if (width + seg.length >= messageWidth) {
                sb.append(nl)
                width = 0
            }
            pos = oneline.indexOf(" ")
        }
        if (oneline.isNotEmpty()) {
            if (width + oneline.length >= messageWidth) {
                sb.append(nl)
            }
            sb.append(oneline)
        }

        return sb.toString()
    }

    class ErrorExplanationTemplate(val code: String, val variant: Int,
                                   val message: String, val explanation: String,
                                   val found: Boolean = true) {
        val cardinality: Int = message.count { ch -> ch == '\$' }
        override fun toString(): String {
            return "${code}/${variant} (${cardinality}): ${message}"
        }
    }
}