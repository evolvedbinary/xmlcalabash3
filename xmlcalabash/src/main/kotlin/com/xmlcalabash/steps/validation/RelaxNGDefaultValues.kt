package com.xmlcalabash.steps.validation

import com.thaiopensource.relaxng.pattern.DefaultValuesExtractor
import com.thaiopensource.relaxng.pattern.Pattern
import com.thaiopensource.resolver.Resolver
import com.thaiopensource.util.PropertyMapBuilder
import com.thaiopensource.validate.SchemaReader
import com.thaiopensource.validate.ValidateProperty
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException

abstract class RelaxNGDefaultValues(val resolver: Resolver, val errHandler: ErrorHandler) {
    abstract fun schemaReader(): SchemaReader
    internal val defaults = mutableMapOf<QName, MutableMap<QName,String>>()

    fun update(input: InputSource) {
        val builder = PropertyMapBuilder()

        // Set the resolver
        builder.put(ValidateProperty.RESOLVER, resolver);
        builder.put(ValidateProperty.ERROR_HANDLER, errHandler);

        val properties = builder.toPropertyMap()
        try {
            val sw = schemaReader().createSchema(input, properties) as RelaxNGSchemaReader.SchemaWrapper
            val start = sw.start
            val collector = DefaultValuesCollector(start)
            collector.parse()
        } catch (ex: Exception) {
            errHandler.warning(SAXParseException("Error loading defaults: {ex.message}", null, ex))
        } catch (ex: StackOverflowError) {
            errHandler.warning(SAXParseException("Error loading defaults: {ex.message}", null, null))
        }
    }

    inner class DefaultValuesCollector(val start: Pattern) : DefaultValuesExtractor.DefaultValuesListener {
        fun parse() {
            DefaultValuesExtractor(this).parsePattern(start)
        }

        override fun defaultValue(elementName: String, elementNamespace: String?,
                                  attributeName: String, attributeNamepsace: String?, value: String) {
            val ename = QName(NamespaceUri.of(elementNamespace ?: ""), elementName)
            val aname = QName(NamespaceUri.of(attributeNamepsace ?: ""), attributeName)

            if (!defaults.containsKey(ename)) {
                defaults[ename] = mutableMapOf()
            }
            defaults[ename]!![aname] = value
        }
    }
}