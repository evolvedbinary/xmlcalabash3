package com.xmlcalabash.steps.pagedmedia.fop

import com.xmlcalabash.api.FoProcessor
import com.xmlcalabash.api.MessageReporter
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.Report
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.Verbosity
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import org.apache.fop.apps.FopFactory
import org.apache.fop.apps.FopFactoryBuilder
import org.apache.fop.configuration.DefaultConfigurationBuilder
import org.apache.fop.events.Event
import org.apache.fop.events.EventFormatter
import org.apache.fop.events.EventListener
import org.apache.fop.events.model.EventSeverity
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.io.OutputStream
import java.net.URI
import java.text.DateFormat
import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.SAXResult
import javax.xml.transform.sax.SAXSource

class FoFop(): FoProcessor {
    companion object {
        private val supportedContentTypes = listOf(
            MediaType.PDF,
            MediaType.PNG,
            MediaType.TEXT,
            MediaType.parse("application/X-fop-areatree"),
            MediaType.parse("application/postscript"),
            MediaType.parse("application/rtf"),
            MediaType.parse("application/vnd.hp-PCL"),
            MediaType.parse("application/x-afp"),
            MediaType.parse("image/svg+xml"),
            MediaType.parse("image/tiff")
        )

        private val _UserConfig = QName("UserConfig")
        private val _StrictFOValidation = QName("StrictFOValidation")
        private val _BreakIndentInheritanceOnReferenceAreaBoundary = QName("BreakIndentInheritanceOnReferenceAreaBoundary")
        private val _SourceResolution = QName("SourceResolution")
        private val _Base14KerningEnabled = QName("Base14KerningEnabled")
        private val _PageHeight = QName("PageHeight")
        private val _PageWidth = QName("PageWidth")
        private val _TargetResolution = QName("TargetResolution")
        private val _StrictUserConfigValidation = QName("StrictUserConfigValidation")
        private val _StrictValidation = QName("StrictValidation")
        private val _UseCache = QName("UseCache")

        private val _Accessibility = QName("Accessibility")
        private val _Author = QName("Author")
        private val _ConserveMemoryPolicy = QName("ConserveMemoryPolicy")
        private val _CreationDate = QName("CreationDate")
        private val _Creator = QName("Creator")
        private val _Keywords = QName("Keywords")
        private val _LocatorEnabled = QName("LocatorEnabled")
        private val _Producer = QName("Producer")
        private val _Subject = QName("Subject")
        private val _Title = QName("Title")

        private val stringOptions = listOf(_UserConfig, _PageHeight, _PageWidth,
            _Author, _Creator, _Keywords, _Producer, _Subject, _Title, _CreationDate)
        private val booleanOptions = listOf(_StrictFOValidation, _BreakIndentInheritanceOnReferenceAreaBoundary,
            _Base14KerningEnabled, _StrictUserConfigValidation, _StrictValidation, _UseCache, _Accessibility,
            _ConserveMemoryPolicy, _LocatorEnabled)
        private val floatOptions = listOf(_SourceResolution, _TargetResolution)

        private val defaultStringOptions = mutableMapOf<QName, String>()
        private val defaultBooleanOptions = mutableMapOf<QName, Boolean>()
        private val defaultFloatOptions = mutableMapOf<QName, Float>()

        fun configure(formatter: URI, properties: Map<QName, String>) {
            if (formatter != FopManager.fopXslFormatter) {
                throw IllegalArgumentException("Unsupported formatter: ${formatter}")
            }

            for ((key, value) in properties) {
                if (key in stringOptions) {
                    defaultStringOptions[key] = value
                } else if (key in floatOptions) {
                    defaultFloatOptions[key] = value.toFloat()
                } else if (key in booleanOptions) {
                    defaultBooleanOptions[key] = value.toBooleanStrict()
                } else {
                    logger.warn("Unsupported FOP property: ${key}")
                }
            }
        }
    }

    lateinit var stepConfig: XProcStepConfiguration
    lateinit var options: Map<QName, XdmValue>
    lateinit var fopFactory: FopFactory

    override fun name(): String {
        return "Apache FOP"
    }

    override fun initialize(context: XProcStepConfiguration, baseURI: URI, options: Map<QName, XdmValue>) {
        this.stepConfig = context
        this.options = options

        // Our base URI is the base URI of the document. But FOP stupidly doesn't resolve
        // against that URI, it just assumes it'll end in '/' and that it can concatenate
        // things on to it. #headdesk
        // We have to mangle the base URI into the string that FOP can use for concatenation.
        val fopBaseUri = if (baseURI.isAbsolute && baseURI.scheme in listOf("http", "https", "file", "ftp")) {
            // Maybe other schemes would also work, maybe it isn't absolutely necessary that
            // the URI is absolute. But I think it always will be. We're just working around a bug in FOP so
            // my patience may be running a bit thin here.
            if (baseURI.path.endsWith('/') || !baseURI.path.contains('/')) {
                baseURI
            } else {
                var uristr = baseURI.toString()
                val pos = uristr.lastIndexOf('/')
                uristr = uristr.substring(0, pos+1)
                if (baseURI.query != null) {
                    uristr += "?${baseURI.query}"
                }
                URI(uristr)
            }
        } else {
            baseURI
        }

        // Only FOP 2.x is supported
        val userConfig = options[_UserConfig]?.underlyingValue?.stringValue ?: defaultStringOptions[_UserConfig]
        val fopBuilder = if (userConfig != null) {
            val cfgBuilder = DefaultConfigurationBuilder()
            val cfg = cfgBuilder.buildFromFile(File(userConfig))
            FopFactoryBuilder(fopBaseUri).setConfiguration(cfg)
        } else {
            FopFactoryBuilder(fopBaseUri)
        }

        for (key in options.keys) {
            if (key != _CreationDate && key !in stringOptions && key !in floatOptions && key !in booleanOptions) {
                context.warn { "Unsupported FOP property: ${key}" }
            }
        }

        for (key in stringOptions) {
            val value = options[key]?.underlyingValue?.stringValue ?: defaultStringOptions[key]
            if (value != null) {
                when (key) {
                    _PageHeight -> fopBuilder.setPageHeight(value)
                    _PageWidth -> fopBuilder.setPageWidth(value)
                    else -> Unit
                }
            }
        }

        for (key in booleanOptions) {
            val value = options[key]?.underlyingValue?.effectiveBooleanValue() ?: defaultBooleanOptions[key]
            if (value != null) {
                when (key) {
                    _StrictFOValidation -> fopBuilder.setStrictFOValidation(value)
                    _BreakIndentInheritanceOnReferenceAreaBoundary -> fopBuilder.setBreakIndentInheritanceOnReferenceAreaBoundary(value)
                    _Base14KerningEnabled -> fopBuilder.fontManager.isBase14KerningEnabled = value
                    _StrictUserConfigValidation -> fopBuilder.setStrictUserConfigValidation(value)
                    _StrictValidation -> fopBuilder.setStrictUserConfigValidation(value)
                    _UseCache -> {
                        if (!value) {
                            fopBuilder.fontManager.disableFontCache()
                        }
                    }
                    else -> Unit
                }
            }
        }

        for (key in floatOptions) {
            val value = options[key]?.underlyingValue?.stringValue?.toFloat() ?: defaultFloatOptions[key]
            if (value != null) {
                when (key) {
                    _SourceResolution -> fopBuilder.setSourceResolution(value)
                    _TargetResolution -> fopBuilder.setTargetResolution(value)
                    else -> Unit
                }
            }
        }

        fopFactory = fopBuilder.build()
    }

    override fun format(document: XProcDocument, contentType: MediaType, out: OutputStream) {
        if (!supportedContentTypes.contains(contentType)) {
            throw stepConfig.exception(XProcError.xcUnsupportedContentType(contentType))
        }

        val fodoc = S9Api.xdmToInputSource(stepConfig, document)
        fodoc.systemId = document.baseURI.toString()
        val source = SAXSource(fodoc)

        val userAgent = fopFactory.newFOUserAgent()

        userAgent.eventBroadcaster.addEventListener(FopEventListener(stepConfig.messageReporter))

        for (key in stringOptions) {
            val value = options[key]?.underlyingValue?.stringValue ?: defaultStringOptions[key]
            if (value != null) {
                when (key) {
                    _Author -> userAgent.author = value
                    _Creator -> userAgent.creator = value
                    _Keywords -> userAgent.keywords = value
                    _Producer -> userAgent.producer = value
                    _Subject -> userAgent.subject = value
                    _Title -> userAgent.title = value
                    _CreationDate -> {
                        val df = DateFormat.getDateInstance()
                        val d = df.parse(value)
                        userAgent.setCreationDate(d)
                    }
                    else -> Unit
                }
            }
        }

        for (key in booleanOptions) {
            val value = options[key]?.underlyingValue?.effectiveBooleanValue() ?: defaultBooleanOptions[key]
            if (value != null) {
                when (key) {
                    _Accessibility -> userAgent.setAccessibility(value)
                    _ConserveMemoryPolicy -> userAgent.setConserveMemoryPolicy(value)
                    _LocatorEnabled -> userAgent.isLocatorEnabled = value
                    else -> Unit
                }
            }
        }

        for (key in floatOptions) {
            val value = options[key]?.underlyingValue?.stringValue?.toFloat() ?: defaultFloatOptions[key]
            if (value != null) {
                when (key) {
                    _TargetResolution -> userAgent.targetResolution = value
                    else -> Unit
                }
            }
        }

        val fop = userAgent.newFop(contentType.toString(), out)
        val defHandler = fop.defaultHandler

        val transformerFactory = TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()
        transformer.transform(source, SAXResult(defHandler))
    }

    private class FopEventListener(val reporter: MessageReporter): EventListener {
        override fun processEvent(event: Event?) {
            if (event == null) {
                // I assume this never actually happens...
                return
            }

            val message = EventFormatter.format(event);
            when (event.severity) {
                EventSeverity.FATAL, EventSeverity.ERROR -> {
                    reporter.error { Report(Verbosity.ERROR, message) }
                }
                EventSeverity.WARN -> {
                    reporter.warn { Report(Verbosity.WARN, message) }
                }
                EventSeverity.INFO -> {
                    // One man's info is another man's debug...
                    reporter.debug { Report(Verbosity.DEBUG, message) }
                }
            }
        }
    }

}