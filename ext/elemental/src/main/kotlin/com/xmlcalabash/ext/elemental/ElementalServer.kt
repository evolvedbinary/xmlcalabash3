package com.xmlcalabash.ext.elemental

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.s9api.QName

interface ElementalServer {

    data class QueryResult(val compilationTime: Long, val executionTime: Long, val umarshallingTime: Long, val result: List<XProcDocument>)

    /**
     * Execute an XQuery/XPath with Elemental
     */
    fun query(stepConfig: XProcStepConfiguration, sources: List<XProcDocument>, query: String, cacheQuery: Boolean = false, username: String = "admin", password: String = "", properties: Map<QName, String>?, variableBindings: Map<QName, XdmValue>?) : QueryResult
}