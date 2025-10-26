package com.xmlcalabash.namespace

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

object NsSchxslt {
    val namespace = NamespaceUri.of("http://dmaus.name/ns/2023/schxslt")

    val debug = QName(namespace, "schxslt:debug")
    val streamable = QName(namespace, "schxslt:streamable")
    val locationFunction = QName(namespace, "schxslt:location-function")
    val failEarly = QName(namespace, "schxslt:fail-early")
    val terminateValidationOnError = QName(namespace, "schxslt:terminate-validation-on-error")
    val reportActivePattern = QName(namespace, "schxslt:report-active-pattern")
    val reportFiredRule = QName(namespace, "schxslt:report-fired-rule")
    val reportSuppressedRule = QName(namespace, "schxslt:report-suppressed-rule")
    val phase = QName(namespace, "schxslt:phase")
    val expandText = QName(namespace, "schxslt:expand-text")

    val staticParams = setOf(debug, streamable, locationFunction, failEarly, terminateValidationOnError,
        reportActivePattern, reportFiredRule, reportSuppressedRule)
    val dynamicParams = setOf(phase, expandText)
}