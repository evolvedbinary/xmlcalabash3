package com.xmlcalabash.namespace

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

object NsEXProc {
    val namespace: NamespaceUri = NamespaceUri.of("http://exproc.org/ns/steps")

    val expandTemplates = QName(namespace, "ex:expand-templates")
    val epubcheck = QName(namespace, "ex:epubcheck")
}