package com.xmlcalabash.steps.validation

import com.thaiopensource.resolver.BasicResolver
import com.thaiopensource.resolver.Identifier
import com.thaiopensource.resolver.Input
import com.thaiopensource.resolver.Resolver
import com.xmlcalabash.io.DocumentManager

class RelaxNGResolver(val manager: DocumentManager): Resolver {
    override fun resolve(id: Identifier, input: Input) {
        val req = manager.resolver.getRequest(id.uriReference, id.base)
        val resp = manager.resolver.resolve(req)
        if (resp.isResolved) {
            input.uri = resp.resolvedURI.toString()
            input.byteStream = resp.inputStream
            input.encoding = resp.encoding
        }
    }

    override fun open(input: Input?) {
        BasicResolver.getInstance().open(input)
    }
}