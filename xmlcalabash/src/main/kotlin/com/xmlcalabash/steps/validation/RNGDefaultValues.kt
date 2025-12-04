package com.xmlcalabash.steps.validation

import com.thaiopensource.relaxng.parse.Parseable
import com.thaiopensource.relaxng.parse.sax.SAXParseable
import com.thaiopensource.relaxng.pattern.AnnotationsImpl
import com.thaiopensource.relaxng.pattern.CommentListImpl
import com.thaiopensource.relaxng.pattern.NameClass
import com.thaiopensource.relaxng.pattern.Pattern
import com.thaiopensource.resolver.Resolver
import com.thaiopensource.resolver.xml.sax.SAXResolver
import com.thaiopensource.util.PropertyMap
import com.thaiopensource.util.VoidValue
import com.thaiopensource.validate.SchemaReader
import org.xml.sax.ErrorHandler
import org.xml.sax.Locator
import javax.xml.transform.sax.SAXSource

class RNGDefaultValues(resolver: Resolver, errHandler: ErrorHandler): RelaxNGDefaultValues(resolver, errHandler) {
    override fun schemaReader(): SchemaReader {
        return XMLSchemaReader
    }

    object XMLSchemaReader: RelaxNGSchemaReader() {
        override fun createParseable(
            source: SAXSource,
            resolver: SAXResolver,
            eh: ErrorHandler,
            properties: PropertyMap
        ): Parseable<Pattern?, NameClass?, Locator?, VoidValue?, CommentListImpl?, AnnotationsImpl?> {
            if (source.xmlReader == null) {
                return SAXParseable(SAXSource(resolver.createXMLReader(), source.inputSource), resolver, eh)
            }
            return SAXParseable(source, resolver, eh)
        }
    }
}