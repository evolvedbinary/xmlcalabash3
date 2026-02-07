package com.xmlcalabash.util

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsSaxon
import net.sf.saxon.expr.parser.XPathParser
import net.sf.saxon.s9api.Message
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XmlProcessingError
import net.sf.saxon.trans.XPathException
import net.sf.saxon.tree.AttributeLocation

open class Report(val severity: Verbosity, val message: () -> String) {
    private var _location: Location = Location.NULL
    private var _inputLocation: Location = Location.NULL
    private var _cause: Throwable? = null
    private var _extraDetail = mutableMapOf<QName,String>()

    constructor(severity: Verbosity, message: () -> String, location: Location): this(severity, message) {
        _location = location
    }

    constructor(severity: Verbosity, message: () -> String, location: Location, inputLocation: Location): this(severity, message) {
        _location = location
        _inputLocation = inputLocation
    }

    constructor(severity: Verbosity, message: () -> String, location: Location, cause: Throwable): this(severity, message) {
        _location = location
        _cause = cause
    }

    constructor(severity: Verbosity, message: () -> String, location: Location, inputLocation: Location, cause: Throwable): this(severity, message) {
        _location = location
        _inputLocation = inputLocation
        _cause = cause
    }

    constructor(severity: Verbosity, message: () -> String, cause: Throwable): this(severity, message) {
        _cause = cause
    }

    constructor(severity: Verbosity, stepConfig: StepConfiguration, message: Message): this(severity, { message.toString() }) {
        _location = stepConfig.location
        _inputLocation = Location(message.location)
        message.location.publicId?.let { _extraDetail[Ns.publicIdentifier] = it }
        if (message.isTerminate) {
            _extraDetail[Ns.terminate]
        }
    }
    
    constructor(severity: Verbosity, stepConfig: StepConfiguration, error: XmlProcessingError)
            : this(severity, { if (error.cause is XPathException && error.cause.message != null) error.cause.message!! else error.message }) {
        _location = stepConfig.location
        _inputLocation = Location(error.location)

        error.location.publicId?.let { _extraDetail[Ns.publicIdentifier] = it }

        _extraDetail[NsSaxon.hostLanguage] = "${error.hostLanguage}"
        _extraDetail[NsSaxon.static] = "${error.isStaticError}"
        _extraDetail[NsSaxon.type] = "${error.isTypeError}"
        if (error.errorCode != null) {
            _extraDetail[Ns.code] = "Q{${error.errorCode.namespaceUri}}${error.errorCode.localName}"
        }

        if (error.failingExpression != null) {
            _extraDetail[NsSaxon.expression] = "${error.failingExpression}"
        } else {
            if (error.location is XPathParser.NestedLocation
                && (error.location as XPathParser.NestedLocation).containingLocation is AttributeLocation) {
                val al = (error.location as XPathParser.NestedLocation).containingLocation as AttributeLocation
                if (al.elementName != null && al.attributeName != null) {
                    _extraDetail[NsSaxon.expression] = "${al.elementName}/@${al.attributeName}"
                }
            }
        }

        error.failingExpression?.let { _extraDetail[NsSaxon.expression] = "${it}" }
        error.path?.let { _extraDetail[Ns.path] = it }
        error.terminationMessage?.let { _extraDetail[NsSaxon.terminationMessage] = it }
        if (error.isAlreadyReported) {
            _extraDetail[NsSaxon.alreadyReported] = "${error.isAlreadyReported}"
        }
    }

    val location: Location
        get() = _location
    val inputLocation: Location
        get() = _inputLocation
    val cause: Throwable?
        get() = _cause
    val extraDetail: Map<QName, String>
        get() = _extraDetail

    internal fun addDetail(detail: QName, value: String) {
        _extraDetail[detail] = value
    }

    // Note: This is disconnected from the message localization infrastructure. Alas.
    override fun toString(): String {
        val code = if (extraDetail[Ns.code] != null) {
            val s = extraDetail[Ns.code]!!
            if (s.startsWith("Q{http://www.w3.org/2005/xqt-errors}")) {
                s.substringAfter("}")
            } else {
                s
            }
        } else {
            null
        }

        val language = when (extraDetail[NsSaxon.hostLanguage]) {
            "XSLT" -> "XSLT"
            "XQUERY" -> "XQuery"
            "XPATH" -> "XPath"
            "XML_SCHEMA" -> "XML Schema"
            "XSLT_PATTERN" -> "XSLT Pattern"
            else -> null
        }

        val sb = StringBuilder()
        if (language != null) {
            sb.append(language).append(" ")
            if (code != null) {
                sb.append("error ").append(code)
            }
            if (NsSaxon.expression in extraDetail) {
                sb.append(" in expression").append(extraDetail[NsSaxon.expression]!!)
            }
            sb.append(": ")
        } else {
            if (code != null) {
                sb.append("Error ").append(code).append(": ")
            }
        }

        sb.append(message())
        return sb.toString()
    }
}