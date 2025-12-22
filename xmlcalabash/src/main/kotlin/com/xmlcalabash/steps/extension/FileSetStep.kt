package com.xmlcalabash.steps.extension

import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsXml
import com.xmlcalabash.namespace.NsXs
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.steps.file.FileStep
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.Urify
import com.xmlcalabash.util.fileselector.FSFile
import com.xmlcalabash.util.fileselector.FileSelector
import com.xmlcalabash.util.fileselector.Matcher
import com.xmlcalabash.util.fileselector.NsFs
import com.xmlcalabash.util.fileselector.mappers.ChainedMapper
import com.xmlcalabash.util.fileselector.mappers.CompositeMapper
import com.xmlcalabash.util.fileselector.mappers.CutDirsMapper
import com.xmlcalabash.util.fileselector.mappers.FirstMatchMapper
import com.xmlcalabash.util.fileselector.mappers.FlattenMapper
import com.xmlcalabash.util.fileselector.mappers.GlobMapper
import com.xmlcalabash.util.fileselector.mappers.IdentityMapper
import com.xmlcalabash.util.fileselector.mappers.Mapper
import com.xmlcalabash.util.fileselector.mappers.MergeMapper
import com.xmlcalabash.util.fileselector.mappers.PackageMapper
import com.xmlcalabash.util.fileselector.mappers.RegexpMapper
import com.xmlcalabash.util.fileselector.mappers.UnpackageMapper
import com.xmlcalabash.util.fileselector.selectors.AlwaysSelector
import com.xmlcalabash.util.fileselector.selectors.AndSelector
import com.xmlcalabash.util.fileselector.selectors.ContainsRegexpSelector
import com.xmlcalabash.util.fileselector.selectors.ContainsSelector
import com.xmlcalabash.util.fileselector.selectors.ContentTypeSelector
import com.xmlcalabash.util.fileselector.selectors.DateSelector
import com.xmlcalabash.util.fileselector.selectors.DependsSelector
import com.xmlcalabash.util.fileselector.selectors.DepthSelector
import com.xmlcalabash.util.fileselector.selectors.DifferentSelector
import com.xmlcalabash.util.fileselector.selectors.ExecutableSelector
import com.xmlcalabash.util.fileselector.selectors.FileType
import com.xmlcalabash.util.fileselector.selectors.FilenameSelector
import com.xmlcalabash.util.fileselector.selectors.MajoritySelector
import com.xmlcalabash.util.fileselector.selectors.NoneSelector
import com.xmlcalabash.util.fileselector.selectors.NotSelector
import com.xmlcalabash.util.fileselector.selectors.OrSelector
import com.xmlcalabash.util.fileselector.selectors.OwnedBySelector
import com.xmlcalabash.util.fileselector.selectors.PosixGroupSelector
import com.xmlcalabash.util.fileselector.selectors.Present
import com.xmlcalabash.util.fileselector.selectors.PresentSelector
import com.xmlcalabash.util.fileselector.selectors.ReadableSelector
import com.xmlcalabash.util.fileselector.selectors.Selector
import com.xmlcalabash.util.fileselector.selectors.SizeSelector
import com.xmlcalabash.util.fileselector.selectors.SymlinkSelector
import com.xmlcalabash.util.fileselector.selectors.TypeSelector
import com.xmlcalabash.util.fileselector.selectors.WhenRelative
import com.xmlcalabash.util.fileselector.selectors.WhenSize
import com.xmlcalabash.util.fileselector.selectors.WritableSelector
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.NamespaceMap
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.Axis
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmNodeKind
import net.sf.saxon.value.DateTimeValue
import net.sf.saxon.value.DayTimeDurationValue
import java.io.File
import java.net.URI

class FileSetStep(): FileStep(NsCx.fileset)  {
    lateinit var rootpath: String
    var defaultExcludes: Boolean? = null
    var caseSensitive: Boolean? = null
    var errorOnMissingDir: Boolean? = null
    var followSymlinks: Boolean? = null
    var detailed = false
    val includes = mutableListOf<String>()
    val excludes = mutableListOf<String>()
    private var nsmap = NamespaceMap.emptyMap()

    private lateinit var grouping: RootGrouping

    override fun run() {
        synchronized(this) {
            unsafeRun()
        }
    }

    fun unsafeRun() {
        super.run()

        val source = queues["source"]!!.firstOrNull()
        val input = if (source != null) {
            S9Api.documentElement(source.value as XdmNode)
        } else {
            // Lots of things are simpler if input is always an element...
            val builder = SaxonTreeBuilder(stepConfig)
            builder.startDocument(stepConfig.baseUri)
            builder.addStartElement(NsFs.fileset)
            builder.addEndElement()
            builder.endDocument()
            S9Api.documentElement(builder.result)
        }
        val href = uriBinding(Ns.path)!!

        if (href.scheme != "file") {
            throw stepConfig.exception(XProcError.xcxFileUriRequired(href.toString()))
        }

        defaultExcludes = booleanBinding(NsFs.defaultExcludes)
        caseSensitive = booleanBinding(NsFs.caseSensitive)
        errorOnMissingDir = booleanBinding(NsFs.errorOnMissingDir)
        followSymlinks = booleanBinding(NsFs.followSymlinks)
        detailed = booleanBinding(Ns.detailed) ?: false

        nsmap = nsmap.put("c", NsC.namespace)
        if (detailed) {
            nsmap = nsmap.put("cx", NsCx.namespace)
        }

        grouping = RootGrouping(href)

        rootpath = href.path
        collectFileset(input, File(href.path))

        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(href)
        outputGrouping(builder, grouping)
        builder.endDocument()
        val xml = builder.result
        receiver.output("result", XProcDocument.ofXml(xml, stepConfig))
    }

    private fun collectFileset(input: XdmNode, path: File) {
        if (input.nodeName != NsFs.fileset) {
            throw stepConfig.exception(XProcError.xcxRootElementMustBeFileset())
        }

        allowedAttributes(input, listOf("includes", "excludes",
            "default-excludes", "case-sensitive", "error-on-missing-dir", "follow-symlinks"))

        defaultExcludes = conditionalBoolean(NsFs.defaultExcludes, defaultExcludes,
            input.getAttributeValue(NsFs.defaultExcludes), true)
        caseSensitive = conditionalBoolean(NsFs.caseSensitive, caseSensitive,
            input.getAttributeValue(NsFs.caseSensitive), true)
        errorOnMissingDir = conditionalBoolean(NsFs.errorOnMissingDir, errorOnMissingDir,
            input.getAttributeValue(NsFs.errorOnMissingDir), true)
        followSymlinks = conditionalBoolean(NsFs.followSymlinks, followSymlinks,
            input.getAttributeValue(NsFs.followSymlinks), true)

        if (errorOnMissingDir!! && !path.exists()) {
            throw stepConfig.exception(XProcError.xcxPathMustExist(path.absolutePath))
        }

        stringBinding(NsFs.includes)?.split("[\\s,]+".toRegex())?.let { includes.addAll(it) }
        input.getAttributeValue(NsFs.includes)?.split("[\\s,]+".toRegex())?.let { includes.addAll(it) }
        for (inc in input.children().filter { it.nodeName == NsFs.include}) {
            allowedAttributes(inc, listOf("name", "if", "unless"))
            val ifValue = inc.getAttributeValue(NsFs.`if`)
            val unlessValue = inc.getAttributeValue(NsFs.unless)
            if (booleanString(NsFs.`if`, ifValue, true) && !booleanString(NsFs.unless, unlessValue, false)) {
                includes.add(inc.getAttributeValue(Ns.name)!!)
            }
        }

        stringBinding(NsFs.excludes)?.split("[\\s,]+".toRegex())?.let { excludes.addAll(it) }
        input.getAttributeValue(NsFs.excludes)?.split("[\\s,]+".toRegex())?.let { excludes.addAll(it) }
        for (exc in input.children().filter { it.nodeName == NsFs.excludes}) {
            allowedAttributes(exc, listOf("name", "if", "unless"))
            val ifValue = exc.getAttributeValue(NsFs.`if`)
            val unlessValue = exc.getAttributeValue(NsFs.unless)
            if (booleanString(NsFs.`if`, ifValue, true) && !booleanString(NsFs.unless, unlessValue, false)) {
                excludes.add(exc.getAttributeValue(Ns.name)!!)
            }
        }

        val fs = FileSelector(includes, excludes, defaultExcludes!!)
        val fileset = if (path.isDirectory) {
            fs.directory(path)
        } else {
            fs.file(path)
        }

        val selectors = parseSelectors(input, true)
        val selector = if (selectors.isNotEmpty()) {
            AndSelector(selectors)
        } else {
            AlwaysSelector()
        }

        val files = fileset!!.files(selector)
        for (file in files) {
            updateGrouping(grouping, file)
        }
        grouping.condense()
    }

    private fun conditionalBoolean(name: QName, current: Boolean?, selected: String?, default: Boolean): Boolean {
        if (current != null && selected != null) {
            throw stepConfig.exception(XProcError.xcxOptionCanOnlyBeSetOnce(name))
        }
        if (current != null) {
            return current
        }
        return booleanString(name, selected, default)
    }

    private fun booleanString(name: QName, value: String?, default: Boolean): Boolean {
        if (value == null) {
            return default
        }
        if (value in listOf("true", "yes", "1")) {
            return true
        }
        if (value in listOf("false", "no", "0")) {
            return false
        }
        throw stepConfig.exception(XProcError.xdBadType(name, value, "xs:boolean"))
    }

    private fun booleanAttribute(element: XdmNode, name: QName, default: Boolean): Boolean {
        val value = element.getAttributeValue(name)
        if (value == null) {
            return default
        }
        if (value in listOf("true", "yes", "1")) {
            return true
        }
        if (value in listOf("false", "no", "0")) {
            return false
        }
        throw stepConfig.exception(XProcError.xdBadType(name, value, "xs:boolean").atInput(Location(element)))
    }

    private fun includedFiles(path: File): List<File> {
        val files = mutableListOf<File>()
        if (path.isDirectory) {
            val dirs = mutableListOf<File>()
            for (file in path.listFiles()!!) {
                if (file.isDirectory) {
                    dirs.add(file)
                } else {
                    files.addAll(includedFiles(file))
                }
            }
            for (file in dirs) {
                files.addAll(includedFiles(file))
            }
        } else {
            val localpath = path.absolutePath.substring(rootpath.length)
            if (includes.isNotEmpty()) {
                var included = false
                for (include in includes) {
                    if (Matcher.matchesGlob(include, localpath)) {
                        included = true
                        break
                    }
                }
                if (!included) {
                    return listOf()
                }
            }

            if (defaultExcludes!! && defaultExclude(localpath)) {
                return listOf()
            }

            for (exclude in excludes) {
                if (Matcher.matchesGlob(exclude, localpath)) {
                    return listOf()
                }
            }

            files.add(path)
        }

        return files
    }

    private fun parseSelectors(input: XdmNode, allowIncludesAndExcludes: Boolean = false): List<Selector> {
        val selectors = mutableListOf<Selector>()
        val elements = mutableListOf<XdmNode>()
        for (child in input.children()) {
            when (child.nodeKind) {
                XdmNodeKind.TEXT -> {
                    if (child.underlyingNode.stringValue.trim().isNotEmpty()) {
                        throw stepConfig.exception(XProcError.xdStepFailed("Non-whitespace text node forbidden").atInput(Location(child)))
                    }
                }

                XdmNodeKind.ATTRIBUTE, XdmNodeKind.NAMESPACE -> Unit // Can't happen
                XdmNodeKind.ELEMENT -> elements.add(child)
                else -> Unit
            }
        }

        for (child in elements) {
            when (child.nodeName) {
                NsFs.include, NsFs.exclude -> {
                    if (!allowIncludesAndExcludes) {
                        throw stepConfig.exception(XProcError.xdStepFailed("Unexpected element: ${child.nodeName}").atInput(Location(child)))
                    }
                }

                NsFs.and -> {
                    allowedAttributes(child, listOf())
                    selectors.add(AndSelector(parseSelectors(child)))
                }

                NsFs.contains -> {
                    allowedAttributes(child, listOf("text", "case-sensitive", "ignore-whitespace", "encoding"))
                    val text = child.getAttributeValue(NsFs.text)
                        ?: throw stepConfig.exception(XProcError.xcxRequiredAttribute(NsFs.text).atInput(Location(child)))
                    val caseSensitive = booleanAttribute(child, NsFs.caseSensitive, true)
                    val ignoreWhitespace = booleanAttribute(child, NsFs.ignoreWhitespace, false)
                    val encoding = child.getAttributeValue(Ns.encoding) ?: "UTF-8"
                    selectors.add(ContainsSelector(stepConfig, text, caseSensitive, ignoreWhitespace, encoding))
                }

                NsFs.containsRegexp -> {
                    allowedAttributes(child, listOf("expression", "case-sensitive", "ignore-whitespace", "encoding"))
                    val expr = child.getAttributeValue(NsFs.expression)
                        ?: throw stepConfig.exception(XProcError.xcxRequiredAttribute(NsFs.expression).atInput(Location(child)))
                    val caseSensitive = booleanAttribute(child, NsFs.caseSensitive, true)
                    val multi = booleanAttribute(child, NsFs.multiLine, false)
                    val single = booleanAttribute(child, NsFs.singleLine, false)
                    val encoding = child.getAttributeValue(Ns.encoding) ?: "UTF-8"
                    selectors.add(ContainsRegexpSelector(stepConfig, expr, caseSensitive, encoding, multi, single))
                }

                NsFs.contentType -> {
                    allowedAttributes(child, listOf("content-types"))
                    val values = child.getAttributeValue(Ns.contentTypes)
                        ?: throw stepConfig.exception(XProcError.xcxRequiredAttribute(Ns.contentTypes).atInput(Location(child)))
                    val types = stepConfig.typeUtils.parseContentTypes(values)
                    selectors.add(ContentTypeSelector(stepConfig, types))
                }

                NsFs.date -> {
                    allowedAttributes(child, listOf("date-time", "when", "granularity"))
                    val dtString = child.getAttributeValue(NsFs.dateTime)
                        ?: throw stepConfig.exception(XProcError.xcxRequiredAttribute(NsFs.dateTime).atInput(Location(child)))

                    val dt = if (dtString.length == 10) {
                        stepConfig.typeUtils.xpathCastAs(XdmAtomicValue("${dtString}T00:00:00"), NsXs.dateTime)
                    } else {
                        stepConfig.typeUtils.xpathCastAs(XdmAtomicValue(dtString), NsXs.dateTime)
                    }

                    val threshold = (dt.underlyingValue as DateTimeValue).toJavaInstant()

                    val whenValue = when (child.getAttributeValue(NsFs.`when`)) {
                        null, "equal" -> WhenRelative.EQUAL
                        "before" -> WhenRelative.BEFORE
                        "after" -> WhenRelative.AFTER
                        else -> throw stepConfig.exception(XProcError.xcxInvalidValue("when", child.getAttributeValue(NsFs.`when`)).atInput(Location(child)))
                    }
                    val granularity = granularityAttribute(child.getAttributeValue(NsFs.granularity))
                    selectors.add(DateSelector(threshold, whenValue, granularity))
                }

                NsFs.depend -> {
                    allowedAttributes(child, listOf("target-dir", "granularity"))
                    val dir = child.getAttributeValue(NsFs.targetDir)
                        ?: throw stepConfig.exception(XProcError.xcxRequiredAttribute(NsFs.targetDir).atInput(Location(child)))

                    val href = child.baseURI.resolve(dir).toString()

                    val targetdir = URI(Urify.urify(href))
                    if (targetdir.scheme != "file") {
                        throw stepConfig.exception(XProcError.xdStepFailed("target-dir must be a file: URI").atInput(Location(child)))
                    }

                    val granularity = granularityAttribute(child.getAttributeValue(NsFs.granularity))
                    val mappers = parseMappers(child)
                    if (mappers.size > 1) {
                        throw stepConfig.exception(XProcError.xcxOnlyOneMapper().atInput(Location(child)))
                    }
                    selectors.add(
                        DependsSelector(
                            mappers.firstOrNull() ?: IdentityMapper(),
                            File(targetdir.path), granularity
                        )
                    )
                }

                NsFs.depth -> {
                    allowedAttributes(child, listOf("min", "max"))
                    val min = if (child.getAttributeValue(NsFs.min) != null) {
                        try {
                            child.getAttributeValue(NsFs.min)!!.toInt()
                        } catch (_: NumberFormatException) {
                            throw stepConfig.exception(XProcError.xdStepFailed("The min attribute must be an integer").atInput(Location(child)))
                        }
                    } else {
                        0
                    }

                    val max = if (child.getAttributeValue(NsFs.max) != null) {
                        try {
                            child.getAttributeValue(NsFs.max)!!.toInt()
                        } catch (_: NumberFormatException) {
                            throw stepConfig.exception(XProcError.xdStepFailed("The max attribute must be an integer").atInput(Location(child)))
                        }
                    } else {
                        Int.MAX_VALUE
                    }

                    if (min < 0 || max < 0) {
                        throw stepConfig.exception(XProcError.xdStepFailed("The min and max values must not be negative").atInput(Location(child)))
                    }

                    selectors.add(DepthSelector(min, max))
                }

                NsFs.different -> {
                    allowedAttributes(child, listOf("target-dir", "ignore-file-times", "ignore-contents", "granularity"))
                    val dir = child.getAttributeValue(NsFs.targetDir)
                        ?: throw stepConfig.exception(XProcError.xcxRequiredAttribute(NsFs.targetDir).atInput(Location(child)))

                    val href = child.baseURI.resolve(dir).toString()

                    val targetdir = URI(Urify.urify(href))
                    if (targetdir.scheme != "file") {
                        throw stepConfig.exception(XProcError.xdStepFailed("target-dir must be a file: URI").atInput(Location(child)))
                    }
                    val ignoreFileTimes = booleanAttribute(child, NsFs.ignoreFileTimes, true)
                    val ignoreContents = booleanAttribute(child, NsFs.ignoreContents, false)
                    val granularity = granularityAttribute(child.getAttributeValue(NsFs.granularity))

                    val mappers = parseMappers(child)
                    if (mappers.size > 1) {
                        throw stepConfig.exception(XProcError.xcxOnlyOneMapper().atInput(Location(child)))
                    }

                    selectors.add(
                        DifferentSelector(
                            mappers.firstOrNull() ?: IdentityMapper(),
                            File(targetdir.path), ignoreFileTimes, ignoreContents, granularity
                        )
                    )
                }

                NsFs.filename -> {
                    allowedAttributes(child, listOf("name", "regex", "case-sensitive", "negate"))
                    val name = child.getAttributeValue(Ns.name)
                    val regex = child.getAttributeValue(NsFs.regex)
                    val caseSensitive = booleanAttribute(child, NsFs.caseSensitive, caseSensitive!!)
                    val negate = booleanAttribute(child, NsFs.negate, false)

                    if (name == null && regex == null) {
                        throw stepConfig.exception(XProcError.xdStepFailed("At least one of name and regex is required").atInput(Location(child)))
                    }
                    if (name != null && regex != null) {
                        throw stepConfig.exception(XProcError.xdStepFailed("At most one of name and regex is required").atInput(Location(child)))
                    }

                    selectors.add(FilenameSelector(name, regex, caseSensitive, negate))
                }

                NsFs.executable -> {
                    allowedAttributes(child, listOf())
                    selectors.add(ExecutableSelector())
                }

                NsFs.majority -> {
                    allowedAttributes(child, listOf("allow-tie"))
                    val allowTie = booleanAttribute(child, NsFs.allowTie, true)
                    selectors.add(MajoritySelector(parseSelectors(child), allowTie))
                }

                NsFs.none -> {
                    allowedAttributes(child, listOf())
                    selectors.add(NoneSelector(parseSelectors(child)))
                }

                NsFs.not -> {
                    allowedAttributes(child, listOf())
                    val notSelectors = parseSelectors(child)
                    if (notSelectors.isEmpty() || notSelectors.size > 1) {
                        throw stepConfig.exception(XProcError.xcxOnlyOneSelector().atInput(Location(child)))
                    }
                    selectors.add(NotSelector(notSelectors.first()))
                }

                NsFs.or -> {
                    allowedAttributes(child, listOf())
                    selectors.add(OrSelector(parseSelectors(child)))
                }

                NsFs.ownedBy -> {
                    allowedAttributes(child, listOf("owner", "follow-symlinks"))
                    val owner = child.getAttributeValue(NsFs.owner)
                        ?: throw stepConfig.exception(XProcError.xcxRequiredAttribute(NsFs.owner).atInput(Location(child)))
                    val follow = booleanAttribute(child, NsFs.followSymlinks, followSymlinks!!)
                    selectors.add(OwnedBySelector(owner, follow))
                }

                NsFs.posixGroup -> {
                    allowedAttributes(child, listOf("group", "follow-symlinks"))
                    val group = child.getAttributeValue(NsFs.group)
                        ?: throw stepConfig.exception(XProcError.xcxRequiredAttribute(NsFs.group).atInput(Location(child)))
                    val follow = booleanAttribute(child, NsFs.followSymlinks, followSymlinks!!)
                    selectors.add(PosixGroupSelector(group, follow))
                }

                NsFs.present -> {
                    allowedAttributes(child, listOf("target-dir", "present"))
                    val dir = child.getAttributeValue(NsFs.targetDir)
                        ?: throw stepConfig.exception(XProcError.xcxRequiredAttribute(NsFs.targetDir).atInput(Location(child)))

                    val href = child.baseURI.resolve(dir).toString()

                    val targetdir = URI(Urify.urify(href))
                    if (targetdir.scheme != "file") {
                        throw stepConfig.exception(XProcError.xdStepFailed("target-dir must be a file: URI").atInput(Location(child)))
                    }

                    val present = when (child.getAttributeValue(NsFs.present)) {
                        null, "both" -> Present.BOTH
                        "srconly" -> Present.SRCONLY
                        else -> throw stepConfig.exception(XProcError.xcxInvalidValue("present", child.getAttributeValue(NsFs.present)).atInput(Location(child)))
                    }

                    val mappers = parseMappers(child)
                    if (mappers.size > 1) {
                        throw stepConfig.exception(XProcError.xcxOnlyOneMapper().atInput(Location(child)))
                    }

                    selectors.add(
                        PresentSelector(
                            mappers.firstOrNull() ?: IdentityMapper(),
                            File(targetdir.path), present
                        )
                    )
                }

                NsFs.readable -> {
                    allowedAttributes(child, listOf())
                    selectors.add(ReadableSelector())
                }

                NsFs.size -> {
                    allowedAttributes(child, listOf("value", "units", "when"))
                    val value = child.getAttributeValue(NsFs.value)?.toLong()
                        ?: throw stepConfig.exception(XProcError.xcxRequiredAttribute(NsFs.value).atInput(Location(child)))
                    val units = child.getAttributeValue(NsFs.units)
                    val whenSize = when(child.getAttributeValue(NsFs.`when`)) {
                        null, "equal" -> WhenSize.EQUAL
                        "less" -> WhenSize.LESS
                        "more" -> WhenSize.MORE
                        else -> throw stepConfig.exception(XProcError.xcxInvalidValue("size", child.getAttributeValue(NsFs.`when`)).atInput(Location(child)))
                    }
                    selectors.add(SizeSelector(value, units, whenSize))
                }

                NsFs.symlink -> {
                    allowedAttributes(child, listOf())
                    selectors.add(SymlinkSelector())
                }

                NsFs.writable -> {
                    allowedAttributes(child, listOf())
                    selectors.add(WritableSelector())
                }

                else -> throw stepConfig.exception(XProcError.xcxUnknownSelector(child.nodeName).atInput(Location(child)))
            }
        }

        return selectors
    }

    private fun parseMappers(input: XdmNode): List<Mapper> {
        val mappers = mutableListOf<Mapper>()
        val elements = mutableListOf<XdmNode>()
        for (child in input.children()) {
            when (child.nodeKind) {
                XdmNodeKind.TEXT -> {
                    if (child.underlyingNode.stringValue.trim().isNotEmpty()) {
                        throw stepConfig.exception(XProcError.xdStepFailed("Non-whitespace text node forbidden").atInput(Location(child)))
                    }
                }
                XdmNodeKind.ATTRIBUTE, XdmNodeKind.NAMESPACE -> Unit // Can't happen
                XdmNodeKind.ELEMENT -> elements.add(child)
                else -> Unit
            }
        }

        for (child in elements) {
            when (child.nodeName) {
                NsFs.chainedMapper -> {
                    allowedAttributes(child, listOf())
                    mappers.add(ChainedMapper(parseMappers(child)))
                }

                NsFs.compositeMapper, NsFs.mapper -> {
                    allowedAttributes(child, listOf())
                    mappers.add(CompositeMapper(parseMappers(child)))
                }

                NsFs.cutDirsMapper -> {
                    allowedAttributes(child, listOf("dirs"))
                    val dirs = child.getAttributeValue(NsFs.dirs)?.toInt()
                        ?: throw stepConfig.exception(XProcError.xcxRequiredAttribute(NsFs.dirs).atInput(Location(child)))
                    mappers.add(CutDirsMapper(dirs))
                }
                NsFs.firstMatchMapper -> {
                    allowedAttributes(child, listOf())
                    mappers.add(FirstMatchMapper(parseMappers(child)))
                }
                NsFs.flattenMapper -> {
                    allowedAttributes(child, listOf())
                    mappers.add(FlattenMapper())
                }
                NsFs.globMapper -> {
                    allowedAttributes(child, listOf("to", "from", "case-sensitive"))
                    val from = child.getAttributeValue(Ns.from)
                        ?: throw stepConfig.exception(XProcError.xcxRequiredAttribute(Ns.from).atInput(Location(child)))
                    val to = child.getAttributeValue(Ns.to)
                        ?: throw stepConfig.exception(XProcError.xcxRequiredAttribute(Ns.to).atInput(Location(child)))
                    val caseSensitive = booleanAttribute(child, NsFs.caseSensitive, true)
                    mappers.add(GlobMapper(from, to, caseSensitive))
                }
                NsFs.identityMapper -> {
                    allowedAttributes(child, listOf())
                    mappers.add(IdentityMapper())
                }
                NsFs.mergeMapper -> {
                    allowedAttributes(child, listOf("to"))
                    val to = child.getAttributeValue(Ns.to)
                        ?: throw stepConfig.exception(XProcError.xcxRequiredAttribute(Ns.to).atInput(Location(child)))
                    mappers.add(MergeMapper(to))
                }
                NsFs.packageMapper -> {
                    allowedAttributes(child, listOf("to", "from"))
                    val from = child.getAttributeValue(Ns.from)
                        ?: throw stepConfig.exception(XProcError.xcxRequiredAttribute(Ns.from).atInput(Location(child)))
                    val to = child.getAttributeValue(Ns.to)
                        ?: throw stepConfig.exception(XProcError.xcxRequiredAttribute(Ns.to).atInput(Location(child)))
                    mappers.add(PackageMapper(from, to))
                }
                NsFs.regexpMapper -> {
                    allowedAttributes(child, listOf("to", "from"))
                    val from = child.getAttributeValue(Ns.from)
                        ?: throw stepConfig.exception(XProcError.xcxRequiredAttribute(Ns.from).atInput(Location(child)))
                    val to = child.getAttributeValue(Ns.to)
                        ?: throw stepConfig.exception(XProcError.xcxRequiredAttribute(Ns.to).atInput(Location(child)))
                    mappers.add(RegexpMapper(from, to))
                }
                NsFs.unpackageMapper -> {
                    allowedAttributes(child, listOf("to", "from"))
                    val from = child.getAttributeValue(Ns.from)
                        ?: throw stepConfig.exception(XProcError.xcxRequiredAttribute(Ns.from).atInput(Location(child)))
                    val to = child.getAttributeValue(Ns.to)
                        ?: throw stepConfig.exception(XProcError.xcxRequiredAttribute(Ns.to).atInput(Location(child)))
                    mappers.add(UnpackageMapper(from, to))
                }

                else -> throw stepConfig.exception(XProcError.xcxUnknownMapper(child.nodeName).atInput(Location(child)))
            }
        }

        return mappers
    }

    private fun granularityAttribute(granularity: String?): Int? {
        if (granularity == null) {
            return null
        }

        try {
            return granularity.toInt()
        } catch (ex: NumberFormatException) {
            val duration = stepConfig.typeUtils.xpathCastAs(XdmAtomicValue(granularity), NsXs.dayTimeDuration)
            val sec = (duration.underlyingValue as DayTimeDurationValue).lengthInSeconds
            return Math.floor(sec * 1000).toInt()
        }
    }

    private fun allowedAttributes(node: XdmNode, attributes: List<String>) {
        for (attr in node.axisIterator(Axis.ATTRIBUTE)) {
            if (attr.nodeName.namespaceUri == NamespaceUri.NULL) {
                if (attr.nodeName.localName !in attributes) {
                    stepConfig.warn { "Invalid attribute ${attr.nodeName.localName} on ${node.nodeName}" }
                }
            }
        }
    }

    private fun defaultExclude(path: String): Boolean {
        val lastSlash = path.lastIndexOf('/')
        val filename = if (lastSlash >= 0) {
            path.substring(lastSlash + 1)
        } else {
            path
        }

        // This is hardcoded because it's faster...

        if (filename in listOf(
                ".DS_Store",
                ".bzr", ".bzrignore",
                ".git", ".gitattributes", ".gitignore", ".gitmodules",
                ".hg", ".hgignore", ".hgsub", ".hgsubstate", ".hgtags",
                ".svn",
                "CVS", ".cvsignore",
                "SCCS",
                "vssver.scc"
            )) {
            return true
        }

        if (filename.endsWith("~")                                          // **/*~
            || filename.startsWith(".#")                                    // **/.#*
            || filename.startsWith("._")                                    // **/._*
            || (filename.startsWith("#") && filename.endsWith("#")) // **/#*#
            || (filename.startsWith("%") && filename.endsWith("%")) // **/%*%
        ) {
            return true
        }

        return path.contains("/CVS/")
                || path.contains("/SCCS/")
                || path.contains("/.svn/")
                || path.contains("/.git/")
                || path.contains("/.hg/")
                || path.contains("/.bzr/")
    }

    private fun outputGrouping(builder: SaxonTreeBuilder, group: AbstractGrouping) {
        if (group is RootGrouping) {
            if (group.groupings.isEmpty()) {
                val pos = group.baseURI.toString().lastIndexOf('/')
                val amap = stepConfig.typeUtils.attributeMap(mapOf(
                    NsXml.base to "${group.baseURI}/",
                    Ns.name to group.baseURI.toString().substring(pos+1)
                ))
                builder.addStartElement(NsC.directory, amap)
                builder.addEndElement()
            } else {
                val root = group.groupings.values.first()
                val baseURI = Urify.urify("/${root.dir}")
                val pos = baseURI.lastIndexOf('/')
                val amap = stepConfig.typeUtils.attributeMap(mapOf(
                    NsXml.base to "${baseURI}/",
                    Ns.name to baseURI.substring(pos+1)
                ))
                outputGroupingDirectory(builder, root, amap)
            }
        } else if (group is Grouping) {
            outputGroupingDirectory(builder, group)
        }
    }

    private fun outputGroupingDirectory(builder: SaxonTreeBuilder, group: Grouping, overrideMap: AttributeMap? = null) {
        val amap = overrideMap
            ?: stepConfig.typeUtils.attributeMap(mapOf(
                NsXml.base to "${group.dir}/",
                Ns.name to group.dir
            ))

        builder.addStartElement(NsC.directory, amap, nsmap)
        for ((_, grouping) in group.groupings) {
            outputGrouping(builder, grouping)
        }

        for (file in group.files) {
            val pos = file.localpath.lastIndexOf('/')
            val name = if (pos >= 0) {
                file.localpath.substring(pos+1)
            } else {
                file.localpath
            }

            val ctype = stepConfig.documentManager.mimetypesFileTypeMap.getContentType(file.localpath)
            val atts = fileAttributes(file.path, detailed, MediaType.parse(ctype), file.path.parentFile)

            builder.addStartElement(NsC.file, atts)
            builder.addEndElement()
        }
        builder.addEndElement()
    }

    private fun updateGrouping(grouping: AbstractGrouping, file: FSFile) {
        val uri = Urify.urify(file.path.absolutePath)
        val parts = mutableListOf<String>()
        parts.addAll(uri.substring(8).split("/"))
        if (parts.size > 0 && parts[0].length > 1 && parts[0][1] == ':') {
            // Assume this is C:/path/to/thing
            val initialPath = parts.removeAt(0)
            parts.add(0, "${initialPath}/${parts.removeAt(0)}")
        }

        var group = grouping
        while (parts.size > 1) {
            val first = parts.removeAt(0)
            if (first !in group.groupings) {
                val newgroup = Grouping(first)
                group.groupings[first] = newgroup
            }
            group = group.groupings[first]!!
        }
        group.files.add(file)
    }


    override fun toString(): String = "cx:fileset"

    private abstract class AbstractGrouping() {
        val groupings = mutableMapOf<String, Grouping>()
        val files = mutableListOf<FSFile>()
        abstract fun condense()
    }

    private class RootGrouping(val baseURI: URI): AbstractGrouping() {
        override fun condense() {
            if (groupings.isNotEmpty()) {
                // There's always a single root.
                val grouping = groupings.values.first()
                grouping.condense()
                groupings.clear()
                groupings["/"] = grouping
            }
        }
    }

    private class Grouping(var dir: String): AbstractGrouping() {
        override fun condense() {
            for ((_, grouping) in groupings) {
                grouping.condense()
            }
            if (groupings.size == 1 && files.isEmpty()) {
                val subgroup = groupings.values.first()
                dir = "${dir}/${subgroup.dir}"
                groupings.clear()
                groupings.putAll(subgroup.groupings)
                files.clear()
                files.addAll(subgroup.files)
            }
        }
    }
}