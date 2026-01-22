package com.xmlcalabash.resourcecache

import com.xmlcalabash.datamodel.CompileEnvironment
import com.xmlcalabash.runtime.steps.Consumer
import com.xmlcalabash.util.Report
import com.xmlcalabash.util.Verbosity
import java.net.URI

class CompiledResourceCache(val environment: CompileEnvironment) {
    private val cache = mutableMapOf<String, CompiledResource>()

    fun contains(name: String, resource: URI): Boolean {
        synchronized(cache) {
            return key(name, resource) in cache
        }
    }

    fun get(name: String, resource: URI): CompiledResource? {
        val key = key(name, resource)
        synchronized(cache) {
            environment.messageReporter.debug { Report(Verbosity.DEBUG, "Using cached resource: ${key}") }
            return cache[key]
        }
    }

    fun put(name: String, resource: URI, rsrc: CompiledResource) {
        val key = key(name, resource)
        synchronized(cache) {
            if (cache.containsKey(key)) {
                throw IllegalStateException("Resource already exists")
            }
            environment.messageReporter.debug { Report(Verbosity.DEBUG, "Caching compiled resource: ${key}") }
            cache[key] = rsrc
        }
    }

    private fun key(name: String, resource: URI): String {
        return "${name}|${resource}"
    }
}