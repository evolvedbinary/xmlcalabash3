package com.xmlcalabash.runtime.api

import com.xmlcalabash.datamodel.ConnectionInstruction
import com.xmlcalabash.io.MediaType
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode

open class RuntimePort(val name: String, val unbound: Boolean, val primary: Boolean, val sequence: Boolean, val contentTypes: List<MediaType>, initialSerialization: XdmMap = XdmMap()) {
    val assertions = mutableListOf<XdmNode>()
    val defaultBindings = mutableListOf<ConnectionInstruction>()
    var serialization = initialSerialization

    constructor(port: RuntimePort): this(port.name, port.unbound, port.primary, port.sequence, port.contentTypes, port.serialization) {
        assertions.addAll(port.assertions)
        defaultBindings.addAll(port.defaultBindings)
    }

    constructor(port: RuntimeOption): this(port.name.eqName, true, false, true, emptyList(), XdmMap()) {
    }

    override fun toString(): String {
        return name
    }
}

