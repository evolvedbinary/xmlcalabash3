package com.xmlcalabash.namespace

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

object NsAcroForm {
    val namespace: NamespaceUri = NamespaceUri.of("http://xmlcalabash.com/ns/acro-form")

    val acroForm = QName(namespace, "f:acro-form")
    val field = QName(namespace, "f:field")
    val checkbox = QName(namespace, "f:checkbox")
    val radiobutton = QName(namespace, "f:radiobutton")
    val button = QName(namespace, "f:button")
    val value = QName(namespace, "f:value")
    val text = QName(namespace, "f:text")
    val combobox = QName(namespace, "f:combobox")
    val listbox = QName(namespace, "f:listbox")
    val choice = QName(namespace, "f:choice")
}