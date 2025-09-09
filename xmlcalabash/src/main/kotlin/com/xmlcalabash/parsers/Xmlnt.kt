package com.xmlcalabash.parsers

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import net.sf.saxon.event.ReceivingContentHandler
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmMap
import org.xml.sax.helpers.AttributesImpl
import org.xmlresolver.sources.ResolverInputSource
import java.net.URI
import java.nio.charset.Charset
import java.util.Stack
import kotlin.collections.iterator

class Xmlnt(val stepConfig: StepConfiguration, val preserveEntities: Boolean, puastart: Char) {
    private var prolog = ""
    private var characterMap = mutableMapOf<Char, String>()
    private var entityMap = mutableMapOf<String, Char>()
    private var parserContext = "document"
    private var nextpua = puastart

    // N.B. This "parser" doesn't attempt to maintain a document locator. It probably should
    // but it's messy and complicated so I'm not bothering right now.

    fun parse(xml: String, baseUri: URI?): XProcDocument {
        val builder = stepConfig.processor.newDocumentBuilder()
        val bch = builder.newBuildingContentHandler()
        val handler = XmlEventHandler(bch as ReceivingContentHandler)
        handler.baseUri = baseUri
        val xml = XML(xml, handler)
        bch.startDocument()
        xml.parse_document()
        bch.endDocument()

        val properties = DocumentProperties()
        if (characterMap.isEmpty()) {
            properties[NsCx.xmlnt] = ""
        } else {
            if (prolog.startsWith("<?xml")) {
                val pos = prolog.indexOf("?>")
                properties[NsCx.xmlnt] = prolog.substring(pos+2)
            } else {
                properties[NsCx.xmlnt] = prolog
            }
            var charMap = XdmMap()
            for ((ch, name) in characterMap) {
                charMap = charMap.put(XdmAtomicValue("${ch}"), XdmAtomicValue(name))
            }
            val serialProp = XdmMap()
            properties[Ns.serialization] = serialProp.put(XdmAtomicValue(Ns.useCharacterMaps), charMap)
        }

        val node = bch.documentNode
        return XProcDocument.ofXml(node, stepConfig, properties)
    }

    private inner class XmlEventHandler(val contentHandler: ReceivingContentHandler): EventHandler {
        var baseUri: URI? = null

        private lateinit var xml: String

        private var prologBuilder: StringBuilder? = StringBuilder()

        private var doctypeName: String? = null
        private val nonterminalStack = Stack<String>()
        private val elementStack = Stack<Element>()
        private var attributeName: String = "";
        private var attributes = mutableMapOf<String, String>()
        private var entityName: String = ""
        private var externalSystem: String? = null
        private var externalPublic: String? = null
        private var systemIdentifier: String? = null
        private var publicIdentifier: String? = null
        private var gentities = mutableMapOf<String, Entity>()
        private var pentities = mutableMapOf<String, Entity>()
        private var pentity = false
        private var ignoreStart = -1
        private var ignorePE = "IGNORE"

        constructor(handler: XmlEventHandler): this(handler.contentHandler) {
            // Yes, copy the object references...
            gentities = handler.gentities
            pentities = handler.pentities
            baseUri = handler.baseUri
        }

        override fun reset(xml: CharSequence?) {
            this.xml = xml.toString()
        }

        override fun startNonterminal(name: String?, begin: Int) {
            //println("S ${name}")
            when (name) {
                "element" -> attributes.clear()
                "ignoreSect" -> ignoreStart = begin
                else -> Unit
            }
            nonterminalStack.push(name)
        }

        override fun endNonterminal(name: String?, end: Int) {
            //println("E ${name}")
            when (name) {
                "prolog" -> {
                    val saveContext = parserContext
                    parserContext = "internal"
                    prolog = prologBuilder.toString()
                    prologBuilder = null
                    if (externalSystem != null) {
                        parserContext = "external"

                        val systemId = if (baseUri != null) {
                            baseUri!!.resolve(externalSystem!!)
                        } else {
                            URI(externalSystem!!)
                        }

                        val saveBaseUri = baseUri
                        baseUri = systemId

                        val xml = readXml(systemId, externalPublic)
                        val handler = XmlEventHandler(this)
                        val parser = XMLesub(xml, handler)
                        parser.parse_extSubset()

                        baseUri = saveBaseUri
                        externalSystem = null
                    }
                    parserContext = saveContext
                }
                "ExternalID" -> {
                    val type = nonterminalStack[nonterminalStack.size - 3]
                    when (type) {
                        "GEDecl" -> {
                            if (!gentities.containsKey(entityName)) {
                                gentities[entityName] = ExternalEntity(entityName, systemIdentifier!!, publicIdentifier)
                            }
                        }
                        "PEDecl" -> {
                            if (!pentities.containsKey(entityName)) {
                                pentities[entityName] = ExternalEntity(entityName, systemIdentifier!!, publicIdentifier)
                            }
                        }
                        "prolog" -> Unit
                        else -> Unit
                    }
                }
                "ignoreSect" -> {
                    when (ignorePE) {
                        "IGNORE" -> Unit
                        "INCLUDE" -> {
                            var section = xml.substring(ignoreStart, end - 3) // Remove ]]>
                            section = section.substring(3); // Remove <![
                            val pos = section.indexOf('[')
                            section = section.substring(pos+1)

                            val handler = XmlEventHandler(this)
                            val parser = XMLesub(section, handler)
                            parser.parse_extSubset()
                        }
                        else -> throw RuntimeException("Invalid marked section marker: $ignorePE")
                    }
                }
                else -> Unit
            }
            nonterminalStack.pop()
        }

        override fun terminal(name: String?, begin: Int, end: Int) {
            //println("T ${name} ::${xml.substring(begin, end)}::")
            when (name) {
                "Name" -> {
                    val type = nonterminalStack.peek()
                    when (type) {
                        "element" -> elementStack.push(Element(xml.substring(begin, end), mutableMapOf<String,String>()))
                        "Attribute" -> attributeName = xml.substring(begin, end)
                        "ETag" -> Unit
                        "doctypedecl" -> doctypeName = xml.substring(begin, end)
                        "GEDecl" -> {
                            pentity = false
                            entityName = xml.substring(begin, end)
                        }
                        "PEDecl" -> {
                            pentity = true
                            entityName = xml.substring(begin, end)
                        }
                        "EntityRef" -> {
                            val name = xml.substring(begin, end)
                            if (!preserveEntities) {
                                expandEntity(name)
                            } else {
                                encodeEntity(name)
                            }
                        }
                        else -> Unit
                    }
                }
                "AttValue" -> {
                    val value = parseAttributeValue(xml.substring(begin, end))
                    if (attributes.containsKey(attributeName)) {
                        throw RuntimeException("Duplicate attribute: $attributeName")
                    }
                    attributes[attributeName] = value
                }
                "CharData" -> {
                    val text = xml.substring(begin, end)
                    contentHandler.characters(text.toCharArray(), 0, end - begin)
                }
                "Comment" -> {
                    if (parserContext == "document") {
                        val comment = xml.substring(begin+4, end-3)
                        contentHandler.comment(comment.toCharArray(), 0, comment.length)
                    }
                }
                "PI" -> {
                    if (parserContext == "document") {
                        val pi = xml.substring(begin + 2, end - 2)
                        val parts = pi.split("[ \t\r\n]+".toRegex())
                        val name = parts[0]
                        val data = if (pi.length > name.length) pi.substring(name.length) else ""

                        // Don't put the XML declaration into the document
                        if (nonterminalStack.size == 3 && nonterminalStack[1] == "prolog") {
                            if (prologBuilder.toString().isNotEmpty()) {
                                contentHandler.processingInstruction(name, data)
                            }
                        } else {
                            contentHandler.processingInstruction(name, data)
                        }
                    }
                }
                "EntityValue" -> {
                    val value = xml.substring(begin+1, end-1)
                    if (nonterminalStack.get(nonterminalStack.size - 2) == "GEDecl") {
                        if (!gentities.containsKey(entityName)) {
                            gentities[entityName] = InternalEntity(entityName, expandParameterEntities(value))
                        }
                    } else {
                        if (!pentities.containsKey(entityName)) {
                            pentities[entityName] = InternalEntity(entityName, expandParameterEntities(value))
                        }
                    }
                }
                "'>'", "'/>'" -> {
                    if (nonterminalStack.peek() == "element") {
                        val element = elementStack.pop()
                        val ename = qname(element.name)
                        if (nonterminalStack.peek() != "ETag") {
                            // End of start tag...
                            for ((name, parsedvalue) in attributes) {
                                val value = parsedvalue.substring(1, parsedvalue.length - 1)
                                if (name == "xmlns" || name.startsWith("xmlns:")) {
                                    val parts = name.split(":")
                                    val prefix = if (parts.size == 1) {
                                        ""
                                    } else {
                                        parts.get(1)
                                    }
                                    if (prefix == "xml" || prefix == "xmlns") {
                                        throw RuntimeException("Cannot define prefix ${prefix}")
                                    }
                                    element.namespaces[prefix] = value
                                }
                            }

                            val actualattr = mutableMapOf<QName, String>()
                            for ((lexical, parsedvalue) in attributes) {
                                val value = parsedvalue.substring(1, parsedvalue.length - 1)
                                if (lexical != "xmlns" && !lexical.startsWith("xmlns:")) {
                                    val name = qname(lexical)
                                    if (actualattr.containsKey(name)) {
                                        throw RuntimeException("Duplicate attribute: $name")
                                    }
                                    actualattr[name] = value
                                }
                            }

                            for ((name, value) in element.namespaces) {
                                if (!hasBinding(name, value)) {
                                    contentHandler.startPrefixMapping(name, value)
                                }
                            }

                            elementStack.push(element)

                            val attr = AttributesImpl()
                            for ((name, value) in actualattr) {
                                attr.addAttribute(name.namespaceUri.toString(), name.localName, name.toString(), "CDATA", value)
                            }
                            contentHandler.startElement(ename.namespaceUri.toString(), ename.localName, ename.toString(), attr)
                        }

                        if (name == "'/>'") {
                            contentHandler.endElement(ename.namespaceUri.toString(), ename.localName, ename.toString())
                            for ((name, value) in element.namespaces) {
                                if (!hasBinding(name, value)) {
                                    contentHandler.endPrefixMapping(name)
                                }
                            }
                        }
                    }

                    if (nonterminalStack.peek() == "ETag") {
                        val element = elementStack.pop()
                        val ename = qname(element.name)
                        contentHandler.endElement(ename.namespaceUri.toString(), ename.localName, ename.toString())
                        for ((name, value) in element.namespaces) {
                            if (!hasBinding(name, value)) {
                                contentHandler.endPrefixMapping(name)
                            }
                        }
                    }
                }
                "SystemLiteral" -> {
                    if (nonterminalStack.size == 4 && nonterminalStack[2] == "doctypedecl") {
                        externalSystem = xml.substring(begin+1, end-1)
                    } else {
                        systemIdentifier = xml.substring(begin+1, end-1)
                    }
                }
                "PubidLiteral" -> {
                    if (nonterminalStack.size == 4 && nonterminalStack[2] == "doctypedecl") {
                        externalPublic = xml.substring(begin+1, end-1)
                    } else {
                        publicIdentifier = xml.substring(begin+1, end-1)
                    }
                }
                "PEReference" -> {
                    val entity = xml.substring(begin, end)
                    val xml = expandParameterEntities(entity)
                    if (nonterminalStack.peek() == "ignoreSect") {
                        ignorePE = xml
                    } else {
                        if (xml.contains("<")) {
                            val handler = XmlEventHandler(this)
                            val parser = XMLesub(xml, handler)
                            parser.parse_extSubset()
                        } else {
                            contentHandler.characters(xml.toCharArray(), 0, xml.length)
                        }
                    }
                }
                "'&'" -> {
                    if (nonterminalStack.peek() == "EntityRef") {
                        pentity = false
                    }
                }
                "'%'" -> {
                    if (nonterminalStack.peek() == "EntityRef") {
                        pentity = true
                    }
                }
                else -> Unit
            }

            if (prologBuilder != null) {
                prologBuilder!!.append(xml.substring(begin, end))
            }
        }

        override fun whitespace(begin: Int, end: Int) {
            // nop
        }

        private fun readXml(systemIdentifier: URI, publicIdentifier: String?): String {
            // Must load as binary because we need to look for an encoding declaration...
            val source = stepConfig.documentManager.resolveEntity(publicIdentifier, systemIdentifier.toString())
            var bytes = if (source is ResolverInputSource) {
                source.byteStream.readBytes()
            } else {
                throw stepConfig.exception(XProcError.xdDoesNotExist(systemIdentifier.toString(), "Failed to read resource"))
            }

            val textdecl = DocumentLoader.textDeclaration(bytes)
            val charset = if (textdecl != null && textdecl.startsWith("<?xml")) {
                val encoding = "\\sencoding\\s*=\\s*[\"']([^\"']+)[\"']".toRegex()
                val match = encoding.find(textdecl)

                bytes = bytes.sliceArray(textdecl.length until bytes.size)

                if (match != null) {
                    match.groupValues[1]
                } else {
                    "UTF-8"
                }
            } else {
                "UTF-8"
            }

            return bytes.toString(Charset.forName(charset))
        }

        private fun expandEntity(name: String) {
            val entity = gentities[name] ?: throw RuntimeException("Unknown entity: $name")
            val handler = XmlEventHandler(this)
            when (entity) {
                is InternalEntity -> {
                    val parser = XMLepe(entity.value, handler)
                    parser.parse_extParsedEnt()
                }
                is ExternalEntity -> {
                    val systemId = if (baseUri != null) {
                        baseUri!!.resolve(entity.systemIdentifier)
                    } else {
                        URI(entity.systemIdentifier)
                    }

                    val saveBaseUri = baseUri
                    baseUri = systemId

                    val xml = readXml(systemId, entity.publicIdentifier)
                    val parser = XMLepe(xml, handler)
                    parser.parse_extParsedEnt()

                    baseUri = saveBaseUri
                }
            }
        }

        private fun encodeEntity(name: String) {
            if (!entityMap.containsKey(name)) {
                characterMap[nextpua] = "&${name};"
                entityMap[name] = nextpua
                nextpua++
            }
            val chars = CharArray(1)
            chars[0] = entityMap[name]!!
            contentHandler.characters(chars, 0, 1)
        }

        private fun parseAttributeValue(value: String): String {
            val entityre = "&[^;]+;".toRegex()
            val sb = StringBuilder()
            var rest = value
            var result = entityre.find(rest)
            while (result != null) {
                if (result.range.first > 0) {
                    sb.append(rest.substring(0, result.range.first))
                }
                val entity = rest.substring(result.range.first + 1, result.range.last)
                when (entity) {
                    "amp" -> sb.append("&")
                    "lt" -> sb.append("<")
                    "gt" -> sb.append(">")
                    "apos" -> sb.append("'")
                    "quot" -> sb.append("\"")
                    else -> {
                        if (preserveEntities) {
                            if (!entityMap.containsKey(entity)) {
                                characterMap[nextpua] = entity
                                entityMap[entity] = nextpua
                                nextpua++
                            }
                            sb.append(entityMap[entity])
                        } else {
                            val decl = gentities[entity]
                            if (decl is InternalEntity) {
                                sb.append(decl.value)
                            } else {
                                throw RuntimeException("Unknown entity: $entity")
                            }
                        }
                    }
                }
                rest = rest.substring(result.range.last + 1)
                result = entityre.find(rest)
            }
            sb.append(rest)
            return sb.toString()
        }

        private fun expandParameterEntities(value: String): String {
            val entityre = "%[^;]+;".toRegex()
            val sb = StringBuilder()
            var rest = value
            var result = entityre.find(rest)
            while (result != null) {
                while (result != null) {
                    if (result.range.first > 0) {
                        sb.append(rest.substring(0, result.range.first))
                    }
                    val name = rest.substring(result.range.first + 1, result.range.last)
                    val entity = pentities[name] ?: throw RuntimeException("Unknown parameter entity: $name")
                    val replacement = when (entity) {
                        is InternalEntity -> {
                            entity.value
                        }
                        is ExternalEntity -> {
                            val systemId = if (baseUri != null) {
                                baseUri!!.resolve(entity.systemIdentifier)
                            } else {
                                URI(entity.systemIdentifier)
                            }

                            readXml(systemId, entity.publicIdentifier)
                        }
                        else -> throw RuntimeException("Unknown entity: $entity")
                    }
                    sb.append(replacement)
                    rest = rest.substring(result.range.last + 1)
                    result = entityre.find(rest)
                }
                sb.append(rest)
                rest = sb.toString()
                result = entityre.find(rest)
            }
            return rest
        }

        private fun qname(lexical: String): QName {
            if (lexical.contains(":")) {
                val parts = lexical.split(":")
                return QName(binding(parts[0]), lexical)
            } else {
                return QName(lexical)
            }
        }

        private fun hasBinding(prefix: String, uri: String): Boolean {
            var depth = elementStack.size - 1
            while (depth >= 0) {
                val element = elementStack[depth]
                val binding = element.namespaces[prefix]
                if (binding != null) {
                    return binding == uri
                }
                depth--
            }
            return false
        }

        private fun binding(prefix: String): String {
            var depth = elementStack.size - 1
            while (depth >= 0) {
                val element = elementStack[depth]
                val uri = element.namespaces[prefix]
                if (uri != null) {
                    return uri
                }
                depth--
            }
            throw RuntimeException("No binding for $prefix")
        }
    }

    private data class Element(val name: String, val namespaces: MutableMap<String,String>)

    private abstract class Entity(val name: String)
    private class InternalEntity(name: String, val value: String) : Entity(name)
    private class ExternalEntity(name: String, val systemIdentifier: String, val publicIdentifier: String?): Entity(name)
}