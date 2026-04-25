package com.xmlcalabash.ext.elemental

import com.evolvedbinary.j8fu.function.ConsumerE
import com.xmlcalabash.datamodel.DocumentContextImpl
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.ext.elemental.ElementalServer.QueryResult
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.event.ContentHandlerProxy
import net.sf.saxon.om.FingerprintedQName
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.s9api.ItemType
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmArray
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmItem
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmNodeKind
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.str.StringView
import net.sf.saxon.tree.util.Orphan
import net.sf.saxon.type.BuiltInAtomicType
import net.sf.saxon.value.AnyURIValue
import net.sf.saxon.value.AtomicValue
import net.sf.saxon.value.Base64BinaryValue
import net.sf.saxon.value.BigIntegerValue
import net.sf.saxon.value.BooleanValue
import net.sf.saxon.value.DateTimeValue
import net.sf.saxon.value.DateValue
import net.sf.saxon.value.DayTimeDurationValue
import net.sf.saxon.value.DecimalValue
import net.sf.saxon.value.DoubleValue
import net.sf.saxon.value.DurationValue
import net.sf.saxon.value.FloatValue
import net.sf.saxon.value.GDayValue
import net.sf.saxon.value.GMonthDayValue
import net.sf.saxon.value.GMonthValue
import net.sf.saxon.value.GYearMonthValue
import net.sf.saxon.value.GYearValue
import net.sf.saxon.value.HexBinaryValue
import net.sf.saxon.value.IntegerValue
import net.sf.saxon.value.NotationValue
import net.sf.saxon.value.NumericValue
import net.sf.saxon.value.QNameValue
import net.sf.saxon.value.TimeValue
import net.sf.saxon.value.YearMonthDurationValue
import org.exist.repo.AutoDeploymentTrigger
import org.exist.source.StringSource
import org.exist.test.ExistEmbeddedServer
import org.exist.xquery.XPathException
import org.exist.xquery.XQueryUtil
import org.exist.xquery.value.Type
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.net.URI
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.Properties
import java.util.Optional
import javax.xml.XMLConstants
import javax.xml.datatype.DatatypeConstants
import javax.xml.datatype.XMLGregorianCalendar
import kotlin.io.path.toPath
import kotlin.math.floor

class LocalElementalServer(val elementalConfigurationProperties: Map<String, String> = defaultProperties, val elementalConfigFile: URI? = null) : ElementalServer {

    companion object {
        private val defaultProperties = mapOf(
            AutoDeploymentTrigger.AUTODEPLOY_PROPERTY to "off",
            ExistEmbeddedServer.USE_TEMPORARY_STORAGE_PROPERTY to "true"
        )
    }

    // TODO(AR) should we ensure this is a singleton? do we want multiple instances of Elemental running - or just one that executes as many queries as needed? - consult with NDW abut what's best to do in Calabash

    private val server by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        // Will be created only when the property is accessed.
        val configFile : Path? = elementalConfigFile?.toPath()
        val configProperties = Properties()
        configProperties.putAll(elementalConfigurationProperties)

        val startStartup = System.currentTimeMillis()
        val elemental = ExistEmbeddedServer(null, configFile, configProperties, false, false)
        elemental.startDb() // TODO(AR) when is it best to start and stop the server? ideally we want to share one server across multiple p:xquery calls - consult with NDW abut what's best to do in Calabash
        val endStartup = System.currentTimeMillis()
        val startupTime = endStartup - startStartup  // TODO(AR) where to report Elemental startup time? - this will be slow the first time the server is started (dynamically via p:xquery ???) - consult with NDW abut what's best to do in Calabash

        elemental
    }

    // TODO(AR) could create additional XProc step integrations for steps to store are retrieve documents etc? - perhaps this could be done by URI resolution from existing steps? - consult with NDW abut what's best to do in Calabash

    override fun query(stepConfig: XProcStepConfiguration, sources: List<XProcDocument>, query: String, cacheQuery: Boolean, username: String, password: String, properties: Map<QName, String>?, variableBindings: Map<QName, XdmValue>?) : QueryResult {
        val brokerPool = server.brokerPool
        val subject = brokerPool.securityManager.authenticate(username, password)

        val broker = brokerPool.get(Optional.of(subject))
        broker.use {

            val contextSequence: org.exist.xquery.value.Sequence?
            if (!sources.isEmpty()) {
                val xdmValue: XdmValue = sources[0].value
                if (!xdmValue.isEmptySequence) {
                    val xdmItem = xdmValue.itemAt(0)
                    val item = toElementalXdm(stepConfig, org.exist.xquery.XQueryContext(), xdmItem)
                    contextSequence = org.exist.xquery.value.ValueSequence(item)
                } else {
                    contextSequence = null
                }
            } else {
                contextSequence = null
            }

            // Set Default Collection in Elemental
            val defaultCollection: List<XdmItem>
            if (!sources.isEmpty()) {
                defaultCollection = sources.map(XProcDocument::value).flatMap{ xdmValue -> xdmValue.stream().asList() }
            } else {
                defaultCollection = emptyList()
            }

            stepConfig.debug { "Elemental local database query: ${query}"}

            val elementalQueryResult = XQueryUtil.query(broker, StringSource(query), cacheQuery, contextSequence, convertProperties(properties), setupXqueryContextPreExecution(stepConfig, defaultCollection, variableBindings ?: emptyMap()), null, null)

            val startUnmarshalling = System.currentTimeMillis()
            val xprocDocuments = toXProcDocuments(stepConfig, elementalQueryResult.result)
            val unmarshallingTime = System.currentTimeMillis() - startUnmarshalling

            return QueryResult(elementalQueryResult.compilationTime, elementalQueryResult.executionTime, unmarshallingTime, xprocDocuments)
        }
    }

    private fun setupXqueryContextPreExecution(stepConfig: XProcStepConfiguration, defaultCollection: List<XdmItem>, variableBindings: Map<QName, XdmValue>) : ConsumerE<org.exist.xquery.XQueryContext, XPathException>? {

        return ConsumerE { xqueryContext ->
            // Set the Default Collection
            val sequence: org.exist.xquery.value.Sequence
            if (!defaultCollection.isEmpty()) {
                sequence = org.exist.xquery.value.ValueSequence(defaultCollection.size)
                defaultCollection.forEach { xdmItem ->
                    val item = toElementalXdm(stepConfig, xqueryContext, xdmItem)
                    sequence.add(item)
                }
            } else {
                sequence = org.exist.xquery.value.Sequence.EMPTY_SEQUENCE
            }
            xqueryContext.addDynamicallyAvailableCollection("", { broker, txn, uri -> sequence })

            // Bind external variables
            for ((qname, value) in variableBindings) {
                val variableName = org.exist.dom.QName(qname.localName, qname.namespaceUri.toString(), qname.prefix)
                val variableValue = toElementalXdm(stepConfig, xqueryContext, value)
                xqueryContext.declareVariable(variableName, variableValue)
            }
        }
    }

    private fun toElementalXdm(stepConfig: XProcStepConfiguration, xqueryContext: org.exist.xquery.XQueryContext, value: XdmValue) : org.exist.xquery.value.Sequence {
        val items = value.map { xdmItem ->
            toElementalXdm(stepConfig, xqueryContext, xdmItem)
        }
        val sequence = org.exist.xquery.value.ValueSequence(items.size)
        sequence.setIsOrdered(false) // NOTE(AR) we add the items in order, so we don't need ValueSequence to enforce an ordering
        items.forEach { item ->
            sequence.add(item)
        }
        return sequence
    }

    private fun toElementalXdm(stepConfig: XProcStepConfiguration, xqueryContext: org.exist.xquery.XQueryContext, xdmItem: XdmItem) : org.exist.xquery.value.Item {
        if (xdmItem.isAtomicValue()) {
            val atomicValue = xdmItem.underlyingValue as AtomicValue
            val atomicType = atomicValue.primitiveType

            return when (atomicType) {

                BuiltInAtomicType.UNTYPED_ATOMIC -> org.exist.xquery.value.UntypedAtomicValue(xdmItem.stringValue)

                BuiltInAtomicType.DATE_TIME -> {
                    val saxonDateTime = atomicValue as DateTimeValue
                    if (saxonDateTime.hasTimezone()) {
                        org.exist.xquery.value.DateTimeValue(saxonDateTime.toZonedDateTime())
                    } else {
                        org.exist.xquery.value.DateTimeValue(saxonDateTime.toLocalDateTime())
                    }
                }

                BuiltInAtomicType.DATE_TIME_STAMP -> org.exist.xquery.value.DateTimeStampValue((atomicValue as DateTimeValue).toZonedDateTime())

                BuiltInAtomicType.DATE -> {
                    val saxonDate = atomicValue as DateValue
                    val timezone: Int
                    if (saxonDate.hasTimezone()) {
                        timezone = saxonDate.timezoneInMinutes
                    } else {
                        timezone = DatatypeConstants.FIELD_UNDEFINED
                    }
                    org.exist.xquery.value.DateValue(
                        null,
                        saxonDate.year,
                        saxonDate.month.toInt(),
                        saxonDate.day.toInt(),
                        timezone
                    )
                }

                BuiltInAtomicType.TIME -> {
                    val saxonTime = atomicValue as TimeValue
                    val timezone: Int
                    if (saxonTime.hasTimezone()) {
                        timezone = saxonTime.timezoneInMinutes
                    } else {
                        timezone = DatatypeConstants.FIELD_UNDEFINED
                    }
                    org.exist.xquery.value.TimeValue(
                        null,
                        saxonTime.hour.toInt(),
                        saxonTime.minute.toInt(),
                        saxonTime.second.toInt(),
                        saxonTime.microsecond * 1000,
                        timezone
                    )
                }

                BuiltInAtomicType.DURATION -> {
                    val saxonDuration = atomicValue as DurationValue
                    val duration = org.exist.xquery.value.TimeUtils.getInstance().newDuration(
                        saxonDuration.signum() > -1,
                        saxonDuration.years,
                        saxonDuration.months,
                        saxonDuration.days,
                        saxonDuration.hours,
                        saxonDuration.minutes,
                        saxonDuration.seconds
                    )
                    org.exist.xquery.value.DurationValue(duration)
                }

                BuiltInAtomicType.YEAR_MONTH_DURATION -> {
                    val saxonYearMonthDuration = atomicValue as YearMonthDurationValue
                    val duration = org.exist.xquery.value.TimeUtils.getInstance().newDurationYearMonth(
                        saxonYearMonthDuration.signum() > -1,
                        saxonYearMonthDuration.years,
                        saxonYearMonthDuration.months
                    )
                    org.exist.xquery.value.YearMonthDurationValue(duration)
                }

                BuiltInAtomicType.DAY_TIME_DURATION -> {
                    val saxonDayTimeDuration = atomicValue as DayTimeDurationValue
                    val duration = org.exist.xquery.value.TimeUtils.getInstance().newDurationDayTime(
                        saxonDayTimeDuration.signum() > -1,
                        saxonDayTimeDuration.days,
                        saxonDayTimeDuration.hours,
                        saxonDayTimeDuration.minutes,
                        saxonDayTimeDuration.seconds
                    )
                    org.exist.xquery.value.DayTimeDurationValue(duration)
                }

                BuiltInAtomicType.FLOAT -> org.exist.xquery.value.FloatValue((atomicValue as FloatValue).floatValue)

                BuiltInAtomicType.DOUBLE -> org.exist.xquery.value.DoubleValue((atomicValue as DoubleValue).doubleValue)

                BuiltInAtomicType.DECIMAL -> org.exist.xquery.value.DecimalValue((atomicValue as DecimalValue).decimalValue)

                BuiltInAtomicType.INTEGER -> toElementalXdmInteger(atomicValue)

                BuiltInAtomicType.NON_POSITIVE_INTEGER -> toElementalXdmInteger(atomicValue, Type.NON_POSITIVE_INTEGER)

                BuiltInAtomicType.NEGATIVE_INTEGER -> toElementalXdmInteger(atomicValue, Type.NEGATIVE_INTEGER)

                BuiltInAtomicType.LONG -> org.exist.xquery.value.IntegerValue((atomicValue as IntegerValue).longValue(), Type.LONG)

                BuiltInAtomicType.INT -> org.exist.xquery.value.IntegerValue((atomicValue as IntegerValue).longValue(), Type.INT)

                BuiltInAtomicType.SHORT -> org.exist.xquery.value.IntegerValue((atomicValue as IntegerValue).longValue(), Type.SHORT)

                BuiltInAtomicType.BYTE -> org.exist.xquery.value.IntegerValue((atomicValue as IntegerValue).longValue(), Type.BYTE)

                BuiltInAtomicType.NON_NEGATIVE_INTEGER -> toElementalXdmInteger(atomicValue, Type.NON_NEGATIVE_INTEGER)

                BuiltInAtomicType.UNSIGNED_LONG -> toElementalXdmInteger(atomicValue, Type.UNSIGNED_LONG)

                BuiltInAtomicType.UNSIGNED_INT -> toElementalXdmInteger(atomicValue, Type.UNSIGNED_INT)

                BuiltInAtomicType.UNSIGNED_SHORT -> toElementalXdmInteger(atomicValue, Type.UNSIGNED_SHORT)

                BuiltInAtomicType.UNSIGNED_BYTE -> toElementalXdmInteger(atomicValue, Type.UNSIGNED_BYTE)

                BuiltInAtomicType.POSITIVE_INTEGER -> toElementalXdmInteger(atomicValue, Type.POSITIVE_INTEGER)

                BuiltInAtomicType.G_YEAR_MONTH -> {
                    val gYearMonthValue = atomicValue as GYearMonthValue
                    val timezone: Int
                    if (gYearMonthValue.hasTimezone()) {
                        timezone = gYearMonthValue.timezoneInMinutes
                    } else {
                        timezone = DatatypeConstants.FIELD_UNDEFINED
                    }
                    val xmlGregorianCalendar = org.exist.xquery.value.TimeUtils.getInstance().newXMLGregorianCalendar(
                        gYearMonthValue.year,
                        gYearMonthValue.month.toInt(),
                        DatatypeConstants.FIELD_UNDEFINED,
                        DatatypeConstants.FIELD_UNDEFINED,
                        DatatypeConstants.FIELD_UNDEFINED,
                        DatatypeConstants.FIELD_UNDEFINED,
                        DatatypeConstants.FIELD_UNDEFINED,
                        timezone
                    )
                    org.exist.xquery.value.GYearMonthValue(xmlGregorianCalendar);
                }

                BuiltInAtomicType.G_YEAR -> {
                    val gYearValue = atomicValue as GYearValue
                    val timezone: Int
                    if (gYearValue.hasTimezone()) {
                        timezone = gYearValue.timezoneInMinutes
                    } else {
                        timezone = DatatypeConstants.FIELD_UNDEFINED
                    }
                    val xmlGregorianCalendar = org.exist.xquery.value.TimeUtils.getInstance().newXMLGregorianCalendar(
                        gYearValue.year,
                        DatatypeConstants.FIELD_UNDEFINED,
                        DatatypeConstants.FIELD_UNDEFINED,
                        DatatypeConstants.FIELD_UNDEFINED,
                        DatatypeConstants.FIELD_UNDEFINED,
                        DatatypeConstants.FIELD_UNDEFINED,
                        DatatypeConstants.FIELD_UNDEFINED,
                        timezone
                    )
                    org.exist.xquery.value.GYearValue(xmlGregorianCalendar);
                }

                BuiltInAtomicType.G_MONTH_DAY -> {
                    val gMonthDayValue = atomicValue as GMonthDayValue
                    val timezone: Int
                    if (gMonthDayValue.hasTimezone()) {
                        timezone = gMonthDayValue.timezoneInMinutes
                    } else {
                        timezone = DatatypeConstants.FIELD_UNDEFINED
                    }
                    val xmlGregorianCalendar = org.exist.xquery.value.TimeUtils.getInstance().newXMLGregorianCalendar(
                        DatatypeConstants.FIELD_UNDEFINED,
                        gMonthDayValue.month.toInt(),
                        gMonthDayValue.day.toInt(),
                        DatatypeConstants.FIELD_UNDEFINED,
                        DatatypeConstants.FIELD_UNDEFINED,
                        DatatypeConstants.FIELD_UNDEFINED,
                        DatatypeConstants.FIELD_UNDEFINED,
                        timezone
                    )
                    org.exist.xquery.value.GMonthDayValue(xmlGregorianCalendar);
                }

                BuiltInAtomicType.G_DAY -> {
                    val gDayValue = atomicValue as GDayValue
                    val timezone: Int
                    if (gDayValue.hasTimezone()) {
                        timezone = gDayValue.timezoneInMinutes
                    } else {
                        timezone = DatatypeConstants.FIELD_UNDEFINED
                    }
                    val xmlGregorianCalendar = org.exist.xquery.value.TimeUtils.getInstance().newXMLGregorianCalendar(
                        DatatypeConstants.FIELD_UNDEFINED,
                        DatatypeConstants.FIELD_UNDEFINED,
                        gDayValue.day.toInt(),
                        DatatypeConstants.FIELD_UNDEFINED,
                        DatatypeConstants.FIELD_UNDEFINED,
                        DatatypeConstants.FIELD_UNDEFINED,
                        DatatypeConstants.FIELD_UNDEFINED,
                        timezone
                    )
                    org.exist.xquery.value.GDayValue(xmlGregorianCalendar);
                }

                BuiltInAtomicType.G_MONTH -> {
                    val gMonthValue = atomicValue as GMonthValue
                    val timezone: Int
                    if (gMonthValue.hasTimezone()) {
                        timezone = gMonthValue.timezoneInMinutes
                    } else {
                        timezone = DatatypeConstants.FIELD_UNDEFINED
                    }
                    val xmlGregorianCalendar = org.exist.xquery.value.TimeUtils.getInstance().newXMLGregorianCalendar(
                        DatatypeConstants.FIELD_UNDEFINED,
                        gMonthValue.month.toInt(),
                        DatatypeConstants.FIELD_UNDEFINED,
                        DatatypeConstants.FIELD_UNDEFINED,
                        DatatypeConstants.FIELD_UNDEFINED,
                        DatatypeConstants.FIELD_UNDEFINED,
                        DatatypeConstants.FIELD_UNDEFINED,
                        timezone
                    )
                    org.exist.xquery.value.GDayValue(xmlGregorianCalendar);
                }

                BuiltInAtomicType.STRING -> org.exist.xquery.value.StringValue(xdmItem.stringValue)

                BuiltInAtomicType.NORMALIZED_STRING -> org.exist.xquery.value.StringValue(
                    xdmItem.stringValue,
                    Type.NORMALIZED_STRING
                )

                BuiltInAtomicType.TOKEN -> org.exist.xquery.value.StringValue(xdmItem.stringValue, Type.TOKEN)

                BuiltInAtomicType.LANGUAGE -> org.exist.xquery.value.StringValue(xdmItem.stringValue, Type.LANGUAGE)

                BuiltInAtomicType.NMTOKEN -> org.exist.xquery.value.StringValue(xdmItem.stringValue, Type.NMTOKEN)

                BuiltInAtomicType.NAME -> org.exist.xquery.value.StringValue(xdmItem.stringValue, Type.NAME)

                BuiltInAtomicType.NCNAME -> org.exist.xquery.value.StringValue(xdmItem.stringValue, Type.NCNAME)

                BuiltInAtomicType.ID -> org.exist.xquery.value.StringValue(xdmItem.stringValue, Type.ID)

                BuiltInAtomicType.IDREF -> org.exist.xquery.value.StringValue(xdmItem.stringValue, Type.IDREF)

                BuiltInAtomicType.ENTITY -> org.exist.xquery.value.StringValue(xdmItem.stringValue, Type.ENTITY)

                BuiltInAtomicType.BOOLEAN -> org.exist.xquery.value.BooleanValue.valueOf((atomicValue as BooleanValue).booleanValue)

                BuiltInAtomicType.BASE64_BINARY -> {
                    val data = (atomicValue as Base64BinaryValue).binaryValue
                    org.exist.xquery.value.Base64BinaryDocument.getInstance(xqueryContext, ByteArrayInputStream(data))
                }

                BuiltInAtomicType.HEX_BINARY -> {
                    val data = (atomicValue as HexBinaryValue).binaryValue
                    org.exist.xquery.value.BinaryValueFromInputStream.getInstance(
                        xqueryContext,
                        org.exist.xquery.value.HexBinaryValueType(),
                        ByteArrayInputStream(data)
                    )
                }

                BuiltInAtomicType.ANY_URI -> org.exist.xquery.value.AnyURIValue((atomicValue as AnyURIValue).stringValue)

                BuiltInAtomicType.QNAME -> {
                    val saxonQname = atomicValue as QNameValue
                    val qname =
                        org.exist.dom.QName(saxonQname.localName, saxonQname.namespaceURI.toString(), saxonQname.prefix)
                    org.exist.xquery.value.QNameValue(null, qname)
                }

                BuiltInAtomicType.NOTATION -> {
                    val saxonNotation = atomicValue as NotationValue
                    val qname = org.exist.dom.QName(
                        saxonNotation.localName,
                        saxonNotation.namespaceURI.toString(),
                        saxonNotation.prefix
                    )
                    org.exist.xquery.value.QNameValue(null, qname) // TODO(AR) how to set as xs:notation in Elemental?
                }

                else -> throw IllegalStateException("Undefined XDM Atomic Value type conversion from Saxon to Elemental for: " + atomicType.typeName.clarkName)
            }

        } else if (xdmItem is XdmNode) {
            val nodeKind = xdmItem.nodeKind

            return when (nodeKind) {

                XdmNodeKind.ATTRIBUTE -> {
                    val memtreeBuilder = org.exist.dom.memtree.MemTreeBuilder(xqueryContext)
                    memtreeBuilder.startDocument()
                    val qname = org.exist.dom.QName(
                        xdmItem.nodeName.localName,
                        xdmItem.nodeName.namespaceUri.toString(),
                        xdmItem.nodeName.prefix
                    )
                    val attrNum = memtreeBuilder.addAttribute(qname, xdmItem.typedValue.toString())
                    memtreeBuilder.endDocument()
                    memtreeBuilder.document.getAttribute(attrNum)
                }

                XdmNodeKind.COMMENT -> {
                    val memtreeBuilder = org.exist.dom.memtree.MemTreeBuilder(xqueryContext)
                    memtreeBuilder.startDocument()
                    val commentNum = memtreeBuilder.comment(xdmItem.typedValue.toString())
                    memtreeBuilder.endDocument()
                    memtreeBuilder.document.getNode(commentNum)
                }

                XdmNodeKind.DOCUMENT -> {
                    val saxAdapter = org.exist.dom.memtree.SAXAdapter(xqueryContext)
                    val contentHandlerProxy = ContentHandlerProxy(saxAdapter)
                    contentHandlerProxy.pipelineConfiguration = stepConfig.processor.getUnderlyingConfiguration().makePipelineConfiguration()
                    contentHandlerProxy.open()
                    xdmItem.getUnderlyingNode().copy(contentHandlerProxy, 0, xdmItem.underlyingValue.saveLocation())
                    contentHandlerProxy.close()
                    saxAdapter.document
                }

                XdmNodeKind.ELEMENT -> {
                    val saxAdapter = org.exist.dom.memtree.SAXAdapter(xqueryContext)
                    val contentHandlerProxy = ContentHandlerProxy(saxAdapter)
                    contentHandlerProxy.pipelineConfiguration = stepConfig.processor.getUnderlyingConfiguration().makePipelineConfiguration()
                    contentHandlerProxy.open()
                    xdmItem.getUnderlyingNode().copy(contentHandlerProxy, 0, xdmItem.underlyingValue.saveLocation())
                    contentHandlerProxy.close()
                    saxAdapter.document.documentElement as org.exist.dom.memtree.ElementImpl
                }

                XdmNodeKind.PROCESSING_INSTRUCTION -> {
                    val memtreeBuilder = org.exist.dom.memtree.MemTreeBuilder(xqueryContext)
                    memtreeBuilder.startDocument()
                    val piNum =
                        memtreeBuilder.processingInstruction(xdmItem.nodeName.localName, xdmItem.typedValue.toString())
                    memtreeBuilder.endDocument()
                    memtreeBuilder.document.getNode(piNum)
                }

                XdmNodeKind.TEXT -> {
                    val memtreeBuilder = org.exist.dom.memtree.MemTreeBuilder(xqueryContext)
                    memtreeBuilder.startDocument()
                    val textNum = memtreeBuilder.comment(xdmItem.typedValue.toString())
                    memtreeBuilder.endDocument()
                    memtreeBuilder.document.getNode(textNum)
                }

                else -> throw IllegalStateException("Undefined XDM Node type conversion from Saxon to Elemental for: " + nodeKind.name)
            }

        } else if (xdmItem is XdmArray) {
            val arrayList = ArrayList<org.exist.xquery.value.Sequence>(xdmItem.arrayLength())
            for (i in 0 ..< xdmItem.arrayLength()) {
                val sequence = toElementalXdm(stepConfig, xqueryContext, xdmItem.get(i))
                arrayList.add(sequence)
            }
            return org.exist.xquery.functions.array.ArrayType(null, xqueryContext, arrayList)

        } else if (xdmItem is XdmMap) {
            val linearMap = org.exist.xquery.functions.map.MapType.newLinearMap<org.exist.xquery.value.Sequence>(null)
            var mapKeyType: Int? = null
            xdmItem.entrySet().forEach { entry ->
                val key = toElementalXdm(stepConfig, xqueryContext, entry.key) as org.exist.xquery.value.AtomicValue
                if (mapKeyType == null) {
                    mapKeyType = key.type
                } else {
                    mapKeyType = Type.getCommonSuperType(mapKeyType, key.type)
                }
                val value = toElementalXdm(stepConfig, xqueryContext, entry.value)
                linearMap.put(key, value)
            }
            return org.exist.xquery.functions.map.MapType(null, xqueryContext, linearMap.forked(), mapKeyType)

        } else {
            throw IllegalStateException("Undefined XDM type conversion from Saxon to Elemental for: $xdmItem")
        }
    }

    private fun toElementalXdmInteger(atomicValue: AtomicValue, elementalType: Int = Type.INTEGER) : org.exist.xquery.value.IntegerValue {
        if (atomicValue is BigIntegerValue) {
            return org.exist.xquery.value.IntegerValue(atomicValue.asBigInteger(), elementalType)
        } else {
            return org.exist.xquery.value.IntegerValue((atomicValue as IntegerValue).longValue(), elementalType)
        }
    }

    private fun toXProcDocuments(stepConfig: XProcStepConfiguration, sequence: org.exist.xquery.value.Sequence) : List<XProcDocument> {
        return toSaxonXdm(stepConfig, sequence).map { item ->
            XProcDocument.ofValue(item, DocumentContextImpl(stepConfig.saxonConfig))
        }
    }

    private fun toSaxonXdm(stepConfig: XProcStepConfiguration, sequence: org.exist.xquery.value.Sequence) : List<XdmItem> {
        val xdmItems = ArrayList<XdmItem>(sequence.itemCount)
        val sequenceIterator = sequence.iterate()
        while (sequenceIterator.hasNext()) {
            val elementalItem = sequenceIterator.nextItem()
            val saxonItem = toSaxonXdm(stepConfig, elementalItem)
            xdmItems.add(saxonItem)
        }
        return xdmItems;
    }

    private fun toSaxonXdm(stepConfig: XProcStepConfiguration, item: org.exist.xquery.value.Item) : XdmItem {
        val itemType = item.type
        return when (itemType) {

            Type.UNTYPED_ATOMIC -> XdmAtomicValue(item.stringValue, ItemType.UNTYPED_ATOMIC)

            Type.DATE_TIME -> {
                if ((item as org.exist.xquery.value.DateTimeValue).hasTimezone()) {
                    XdmAtomicValue(DateTimeValue.fromCalendar(item.calendar.toGregorianCalendar(), true))
                } else {
                    XdmAtomicValue.makeAtomicValue(item.toJavaObject(LocalDateTime::class.java))
                }
            }

            Type.DATE_TIME_STAMP -> XdmAtomicValue(DateTimeValue.fromZonedDateTime(item.toJavaObject(ZonedDateTime::class.java)))

            Type.DATE -> {
                val calendar = (item as org.exist.xquery.value.DateValue).toJavaObject(XMLGregorianCalendar::class.java)
                XdmAtomicValue(DateValue(calendar.toGregorianCalendar(), calendar.timezone))
            }

            Type.TIME -> {
                val calendar = (item as org.exist.xquery.value.TimeValue).toJavaObject(XMLGregorianCalendar::class.java)
                XdmAtomicValue(TimeValue(calendar.toGregorianCalendar(), calendar.timezone))
            }

            Type.DURATION -> {
                val durationValue = item as org.exist.xquery.value.DurationValue
                val sign: Int = durationValue.getPart(org.exist.xquery.value.DurationValue.SIGN)
                val positive: Boolean = sign > -1
                val years: Int = durationValue.getPart(org.exist.xquery.value.DurationValue.YEAR) * sign
                val months: Int = durationValue.getPart(org.exist.xquery.value.DurationValue.MONTH) * sign
                val days: Int = durationValue.getPart(org.exist.xquery.value.DurationValue.DAY) * sign
                val hours: Int = durationValue.getPart(org.exist.xquery.value.DurationValue.HOUR) * sign
                val minutes: Int = durationValue.getPart(org.exist.xquery.value.DurationValue.MINUTE) * sign
                val secondsAndMicroseconds: Double = durationValue.getSeconds() * sign
                val seconds: Long = secondsAndMicroseconds.toLong()
                val microseconds: Int = ((secondsAndMicroseconds - floor(secondsAndMicroseconds)) * 100).toInt()
                XdmAtomicValue(
                    DurationValue(
                        positive,
                        years,
                        months,
                        days,
                        hours,
                        minutes,
                        seconds,
                        microseconds,
                    )
                )
            }

            Type.YEAR_MONTH_DURATION -> {
                val yearMonthDurationValue = item as org.exist.xquery.value.YearMonthDurationValue
                val sign: Int = yearMonthDurationValue.getPart(org.exist.xquery.value.DurationValue.SIGN)
                val years: Int = yearMonthDurationValue.getPart(org.exist.xquery.value.DurationValue.YEAR)
                val months: Int = yearMonthDurationValue.getPart(org.exist.xquery.value.DurationValue.MONTH)
                XdmAtomicValue(YearMonthDurationValue.fromMonths(((years * 12) + months) * sign))
            }

            Type.DAY_TIME_DURATION -> {
                val dayTimeDurationValue = item as org.exist.xquery.value.DayTimeDurationValue
                val sign: Int = dayTimeDurationValue.getPart(org.exist.xquery.value.DurationValue.SIGN)
                val days: Int = dayTimeDurationValue.getPart(org.exist.xquery.value.DurationValue.DAY) * sign
                val hours: Int = dayTimeDurationValue.getPart(org.exist.xquery.value.DurationValue.HOUR) * sign
                val minutes: Int = dayTimeDurationValue.getPart(org.exist.xquery.value.DurationValue.MINUTE) * sign
                val secondsAndMicroseconds: Double = dayTimeDurationValue.getSeconds() * sign
                val seconds: Long = secondsAndMicroseconds.toLong()
                val microseconds: Int = ((secondsAndMicroseconds - floor(secondsAndMicroseconds)) * 100).toInt()
                XdmAtomicValue(DayTimeDurationValue(days, hours, minutes, seconds, microseconds))
            }

            Type.FLOAT -> XdmAtomicValue((item as org.exist.xquery.value.FloatValue).value)

            Type.DOUBLE -> XdmAtomicValue((item as org.exist.xquery.value.DoubleValue).double)

            Type.DECIMAL -> XdmAtomicValue((item as org.exist.xquery.value.DecimalValue).value)

            Type.INTEGER -> XdmAtomicValue(IntegerValue.makeIntegerValue(item.toJavaObject(BigInteger::class.java)))

            Type.NON_POSITIVE_INTEGER -> toSaxonXdmInteger(item, BuiltInAtomicType.NON_POSITIVE_INTEGER)

            Type.NEGATIVE_INTEGER -> toSaxonXdmInteger(item, BuiltInAtomicType.NEGATIVE_INTEGER)

            Type.LONG -> XdmAtomicValue((item as org.exist.xquery.value.IntegerValue).value)

            Type.INT -> XdmAtomicValue((item as org.exist.xquery.value.IntegerValue).int)

            Type.SHORT -> XdmAtomicValue(item.toJavaObject(Short::class.java))

            Type.BYTE -> XdmAtomicValue(item.toJavaObject(Byte::class.java))

            Type.NON_NEGATIVE_INTEGER -> toSaxonXdmInteger(item, BuiltInAtomicType.NON_NEGATIVE_INTEGER)

            Type.UNSIGNED_LONG -> toSaxonXdmInteger(item, BuiltInAtomicType.UNSIGNED_LONG)

            Type.UNSIGNED_INT -> toSaxonXdmInteger(item, BuiltInAtomicType.UNSIGNED_INT)

            Type.UNSIGNED_SHORT -> toSaxonXdmInteger(item, BuiltInAtomicType.UNSIGNED_SHORT)

            Type.UNSIGNED_BYTE -> toSaxonXdmInteger(item, BuiltInAtomicType.UNSIGNED_BYTE)

            Type.POSITIVE_INTEGER -> toSaxonXdmInteger(item, BuiltInAtomicType.POSITIVE_INTEGER)

            Type.G_YEAR_MONTH -> {
                val gYearMonthValue = item as org.exist.xquery.value.GYearMonthValue
                val years: Int = gYearMonthValue.getPart(org.exist.xquery.value.GYearMonthValue.YEAR)
                val months: Byte = gYearMonthValue.getPart(org.exist.xquery.value.GYearMonthValue.MONTH).toByte()
                val tzInMinutes: Int = gYearMonthValue.calendar.timezone
                XdmAtomicValue(GYearMonthValue(years, months, tzInMinutes, false))
            }

            Type.G_YEAR -> {
                val gYearValue = item as org.exist.xquery.value.GYearValue
                val years: Int = gYearValue.getPart(org.exist.xquery.value.GYearValue.YEAR)
                val tzInMinutes: Int = gYearValue.calendar.timezone
                XdmAtomicValue(GYearValue(years, tzInMinutes, false))
            }

            Type.G_MONTH_DAY -> {
                val gMonthDayValue = item as org.exist.xquery.value.GMonthDayValue
                val months: Byte = gMonthDayValue.getPart(org.exist.xquery.value.GYearValue.MONTH).toByte()
                val days: Byte = gMonthDayValue.getPart(org.exist.xquery.value.GYearValue.DAY).toByte()
                val tzInMinutes: Int = gMonthDayValue.calendar.timezone
                XdmAtomicValue(GMonthDayValue(months, days, tzInMinutes))
            }

            Type.G_DAY -> {
                val gDayValue = item as org.exist.xquery.value.GDayValue
                val days: Byte = gDayValue.getPart(org.exist.xquery.value.GYearValue.DAY).toByte()
                val tzInMinutes: Int = gDayValue.calendar.timezone
                XdmAtomicValue(GDayValue(days, tzInMinutes))
            }

            Type.G_MONTH -> {
                val gMonthValue = item as org.exist.xquery.value.GMonthValue
                val months: Byte = gMonthValue.getPart(org.exist.xquery.value.GYearValue.MONTH).toByte()
                val tzInMinutes: Int = gMonthValue.calendar.timezone
                XdmAtomicValue(GMonthValue(months, tzInMinutes))
            }

            Type.STRING -> XdmAtomicValue.makeAtomicValue(item.toString())

            Type.NORMALIZED_STRING -> XdmAtomicValue(item.toString(), ItemType.NORMALIZED_STRING)

            Type.TOKEN -> XdmAtomicValue(item.toString(), ItemType.TOKEN)

            Type.LANGUAGE -> XdmAtomicValue(item.toString(), ItemType.LANGUAGE)

            Type.NMTOKEN -> XdmAtomicValue(item.toString(), ItemType.NMTOKEN)

            Type.NAME -> XdmAtomicValue(item.toString(), ItemType.NAME)

            Type.NCNAME -> XdmAtomicValue(item.toString(), ItemType.NCNAME)

            Type.ID -> XdmAtomicValue(item.toString(), ItemType.ID)

            Type.IDREF -> XdmAtomicValue(item.toString(), ItemType.IDREF)

            Type.ENTITY -> XdmAtomicValue(item.toString(), ItemType.ENTITY)

            Type.BOOLEAN -> XdmAtomicValue((item as org.exist.xquery.value.BooleanValue).value)

            Type.BASE64_BINARY -> {
                val data = (item as org.exist.xquery.value.BinaryValue).toJavaObject(ByteArray::class.java)
                XdmAtomicValue(Base64BinaryValue(data))
            }

            Type.HEX_BINARY -> {
                val data = (item as org.exist.xquery.value.BinaryValue).toJavaObject(ByteArray::class.java)
                XdmAtomicValue(HexBinaryValue(data))
            }

            Type.ANY_URI -> XdmAtomicValue((item as org.exist.xquery.value.AnyURIValue).toURI())

            Type.QNAME -> {
                val qname = (item as org.exist.xquery.value.QNameValue).qName
                XdmAtomicValue(QNameValue(qname.prefix, NamespaceUri.of(qname.namespaceURI), qname.localPart))
            }

            Type.NOTATION -> {
                val qname = (item as org.exist.xquery.value.QNameValue).qName
                XdmAtomicValue(NotationValue(qname.prefix, NamespaceUri.of(qname.namespaceURI), qname.localPart, true))
            }

            Type.NUMERIC -> XdmAtomicValue(NumericValue.parseNumber(item.stringValue))

            // NOTE(AR) unimplemented
//            Type.UNTYPED ->

            Type.ATTRIBUTE -> {
                val attr = item as org.w3c.dom.Attr
                val saxonAttr = Orphan(stepConfig.saxonConfig.configuration)
                saxonAttr.setNodeKind(net.sf.saxon.type.Type.ATTRIBUTE)
                val attrQname = FingerprintedQName(StructuredQName(attr.prefix ?: XMLConstants.DEFAULT_NS_PREFIX, attr.namespaceURI ?: XMLConstants.NULL_NS_URI, attr.localName))
                saxonAttr.setNodeName(attrQname)
                saxonAttr.setStringValue(StringView.of(attr.value))
                XdmNode(saxonAttr)
            }

            Type.COMMENT -> {
                val comment = item as org.w3c.dom.Comment
                val saxonComment = Orphan(stepConfig.saxonConfig.configuration)
                saxonComment.setNodeKind(net.sf.saxon.type.Type.COMMENT)
                saxonComment.setStringValue(StringView.of(comment.data))
                XdmNode(saxonComment)
            }

            Type.DOCUMENT -> {
                val document = item as org.w3c.dom.Document
                val documentBuilder = stepConfig.processor.newDocumentBuilder()
                documentBuilder.wrap(document)
            }

            Type.ELEMENT -> {
                val element = item as org.w3c.dom.Element
                val documentBuilder = stepConfig.processor.newDocumentBuilder()
                documentBuilder.wrap(element)
            }

            // NOTE(AR) unimplemented
//            Type.NAMESPACE ->

            Type.PROCESSING_INSTRUCTION -> {
                val pi = item as org.w3c.dom.ProcessingInstruction
                val saxonPi = Orphan(stepConfig.saxonConfig.configuration)
                saxonPi.setNodeKind(net.sf.saxon.type.Type.PROCESSING_INSTRUCTION)
                saxonPi.setNodeName(FingerprintedQName.fromClarkName(pi.target))
                saxonPi.setStringValue(StringView.of(pi.data))
                XdmNode(saxonPi)
            }

            Type.TEXT -> {
                val text = item as org.w3c.dom.Text
                val saxonText = Orphan(stepConfig.saxonConfig.configuration)
                saxonText.setNodeKind(net.sf.saxon.type.Type.TEXT)
                saxonText.setStringValue(StringView.of(text.data))
                XdmNode(saxonText)
            }

            // NOTE(AR) unimplemented
//            Type.FUNCTION ->

            Type.ARRAY_ITEM -> {
                val arrayValue = (item as org.exist.xquery.functions.array.ArrayType)
                val xdmValues: Array<XdmValue?> = arrayOfNulls(arrayValue.size)
                for (i in 0 ..< arrayValue.size) {
                    xdmValues[i] = XdmValue(toSaxonXdm(stepConfig, arrayValue.get(i)))
                }
                XdmArray(xdmValues)
            }

            Type.MAP_ITEM -> {
                val mapValue = item as org.exist.xquery.functions.map.MapType
                val mapValueKeys = mapValue.keys()
                val map : MutableMap<XdmAtomicValue, XdmValue> = mutableMapOf()
                for (i in 0 ..< mapValueKeys.itemCount) {
                    val key = mapValueKeys.itemAt(i)
                    val value = mapValue.get(key.atomize())

                    val saxonKey = toSaxonXdm(stepConfig, key) as XdmAtomicValue
                    val saxonValue = XdmValue(toSaxonXdm(stepConfig, value))
                    map.put(saxonKey, saxonValue)
                }
                XdmMap(map)
            }

            else -> throw IllegalStateException("Undefined XDM type conversion from Elemental to Saxon for: " + Type.getTypeName(itemType))
        }
    }

    private fun toSaxonXdmInteger(item: org.exist.xquery.value.Item, saxonType: BuiltInAtomicType) : XdmAtomicValue {
        val integerValue = IntegerValue.makeIntegerValue(item.toJavaObject(BigInteger::class.java))
        return XdmAtomicValue(integerValue.copyAsSubType(saxonType))
    }

    private fun convertProperties(propertiesMap: Map<QName, String>?) : Properties? {
        if (propertiesMap == null) {
            return null
        }

        val properties = Properties()
        properties.putAll(propertiesMap)
        return properties
    }
}