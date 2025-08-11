package com.xmlcalabash.spi

import java.net.URI
import java.util.ServiceLoader
import kotlin.collections.iterator

class XQueryProcessorServiceProvider {
    companion object {
        private val DEFAULT_PROVIDER = URI.create("https://xmlcalabash.com/")

        fun providers(): List<XQueryProcessorProvider> {
            val services = mutableListOf<XQueryProcessorProvider>()
            val loader = ServiceLoader.load(XQueryProcessorProvider::class.java)
            for (provider in loader.iterator()) {
                services.add(provider)
            }
            return services
        }

        fun provider(): XQueryProcessorProvider {
            return provider(DEFAULT_PROVIDER)
        }

        fun provider(providerUri: URI): XQueryProcessorProvider {
            val loader = ServiceLoader.load(XQueryProcessorProvider::class.java)
            for (provider in loader.iterator()) {
                if (provider.implementationUri == providerUri) {
                    return provider
                }
            }
            throw IllegalArgumentException("Provider ${providerUri} not found")
        }
    }
}