package com.xmlcalabash.ext.elemental

import com.xmlcalabash.documents.DocumentProperties
import net.sf.saxon.s9api.XdmItem

class ElementalQueryTypedResponseDocument(val xqueryCompilationTime: Long, val xqueryExecutionTime: Long, val unmarshallingTime: Long, val results: List<XdmItem>, val documentProperties: DocumentProperties = DocumentProperties())