package com.xmlcalabash.steps.pagedmedia.pagedjs

import com.xmlcalabash.api.CssProcessor
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import org.apache.logging.log4j.kotlin.logger
import java.io.*
import java.net.URI
import java.nio.file.Paths

class CssPagedjs: CssProcessor  {
    companion object {
        val _exePath = QName("exePath")
        val _debug = QName("debug")
        val _landscape = QName("landscape")
        val _pageSize = QName("page-size")
        val _width = QName("width")
        val _height = QName("height")
        val _forceTransparentBackground = QName("forceTransparentBackground")
        val _timeout = QName("timeout")
        val _html = QName("html")
        val _blockLocal = QName("blockLocal")
        val _blockRemote = QName("blockRemote")
        val _allowedPath = QName("allowedPath")
        val _allowedDomain = QName("allowedDomain")
        val _outlineTags = QName("outline-tags")
        val _browserEndpoint = QName("browserEndpoint")
        val _browserArgs = QName("browserArgs")
        val _additionalScript = QName("additionalScript")
        val _media = QName("media")
        val _style = QName("style")
        val _warn = QName("warn")
        val _extraHeader = QName("extra-header")

        val booleanOptions = listOf(_debug, _landscape, _forceTransparentBackground, _html, _blockLocal, _blockRemote, _warn)

        val stringOptions = listOf(_exePath, _pageSize, _width, _height, _timeout, _outlineTags,
            _browserEndpoint, _browserArgs, _media)

        val listOptions = listOf(_allowedPath, _allowedDomain, _additionalScript, _style, _extraHeader)

        val defaultBooleanOptions = mutableMapOf<QName,Boolean>()
        val defaultStringOptions = mutableMapOf<QName,String>()
        val defaultListOptions = mutableMapOf<QName,String>()

        fun configure(formatter: URI, properties: Map<QName, String>) {
            if (formatter != PagedjsManager.pagedJsCssFormatter) {
                throw IllegalArgumentException("Unsupported formatter: ${formatter}")
            }

            for ((key, value) in properties) {
                if (key in stringOptions) {
                    defaultStringOptions[key] = value
                } else if (key in listOptions) {
                    defaultListOptions[key] = value
                } else if (key in booleanOptions) {
                    defaultBooleanOptions[key] = value.toBooleanStrict()
                } else {
                    logger.warn("Unsupported Paged.js property: ${key}")
                }
            }

            if (defaultStringOptions[_exePath] == null) {
                val prop = System.getProperty("com.xmlcalabash.css.pagedjs.exepath")
                if (prop != null) {
                    defaultStringOptions[_exePath] = prop
                } else {
                    val exeName = if (System.getProperty("os.name").startsWith("Windows")) {
                        "pagedjs-cli.exe"
                    } else {
                        "pagedjs-cli"
                    }

                    var found: String? = null
                    for (path in System.getenv("PATH").split(File.pathSeparator)) {
                        val exe = Paths.get(path, exeName).toFile()
                        if (exe.exists() && exe.canExecute()) {
                            found = exe.absolutePath
                            break
                        }
                    }
                    if (found != null) {
                        defaultStringOptions[_exePath] = found
                    }
                }
            }
        }
    }

    lateinit var stepConfig: XProcStepConfiguration
    lateinit var options: Map<QName, XdmValue>
    var exePath = ""
    var primarySS: String? = null
    val userSS = mutableListOf<String>()
    val tempFiles = mutableListOf<File>()

    val commandLine = mutableListOf<String>()

    override fun name(): String {
        return "Paged.js"
    }

    override fun initialize(stepConfig: XProcStepConfiguration, baseURI: URI, options: Map<QName, XdmValue>) {
        this.stepConfig = stepConfig
        this.options = options
        tempFiles.clear()

        exePath = options[_exePath]?.underlyingValue?.stringValue ?: defaultStringOptions[_exePath] ?: ""
        if (exePath == "") {
            throw stepConfig.exception(XProcError.xdStepFailed("Cannot find pagedjs-cli executable"))
        }
    }

    private fun addStylesheet(uri: URI) {
        if (primarySS == null) {
            primarySS = uri.toString()
        } else {
            userSS.add(uri.toString())
        }
    }

    override fun addStylesheet(document: XProcDocument) {
        if (document.contentClassification != MediaClassification.TEXT) {
            stepConfig.error { "Ignoring non-text CSS sytlesheet: ${document.baseURI}" }
            return
        }

        val temp = File.createTempFile("xmlcalabash-pagedjs", ".css")
        temp.deleteOnExit()
        tempFiles.add(temp)

        stepConfig.debug { "css-formatter css: ${temp.absolutePath}" }

        val cssout = PrintStream(temp)
        cssout.print(document.value.underlyingValue.stringValue)
        cssout.close()

        addStylesheet(temp.toURI())
    }

    override fun format(document: XProcDocument, contentType: MediaType, out: OutputStream) {
        if (contentType != MediaType.PDF) {
            throw stepConfig.exception(XProcError.xcUnsupportedContentType(contentType))
        }

        commandLine.add(exePath)

        for (name in booleanOptions) {
            val value = options[name]?.underlyingValue?.effectiveBooleanValue() ?: defaultBooleanOptions[name]
            if (value != null && value) {
                commandLine.add("--${name.localName}")
            }
        }

        for (name in stringOptions) {
            if (name != _exePath) {
                val value = options[name]?.underlyingValue?.stringValue ?: defaultStringOptions[name]
                if (value != null) {
                    commandLine.add("--${name.localName}")
                    commandLine.add(value)
                }
            }
        }

        for (name in listOptions) {
            val value = options[name]?.underlyingValue?.stringValue ?: defaultListOptions[name]
            if (value != null) {
                for (item in value.trim().split("\\s+".toRegex())) {
                    commandLine.add("--${name.localName}")
                    commandLine.add(item)
                }
            }
        }

        val tempXml = File.createTempFile("xmlcalabash-pagedjs", ".html")
        tempXml.deleteOnExit()
        tempFiles.add(tempXml)

        commandLine.add(tempXml.absolutePath)

        stepConfig.debug { "css-formatter source: ${tempXml.absolutePath}" }

        val fos = FileOutputStream(tempXml)
        DocumentWriter(document, fos).write()
        fos.close()

        val tempPdf = File.createTempFile("xmlcalabash-pagedjs", ".pdf")
        tempPdf.deleteOnExit()
        tempFiles.add(tempPdf)

        commandLine.add("--output")
        commandLine.add(tempPdf.absolutePath)

        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()

        stepConfig.debug { commandLine.joinToString(" ") }

        //println("RUN: ${commandLine.joinToString(" ")}")

        val builder = ProcessBuilder(commandLine)

        val rc = try {
            val process = builder.start()

            val stdoutReader = ProcessOutputReader(process.inputStream, stdout)
            val stderrReader = ProcessOutputReader(process.errorStream, stderr)

            val stdoutThread = Thread(stdoutReader)
            val stderrThread = Thread(stderrReader)

            stdoutThread.start()
            stderrThread.start()

            val localrc = process.waitFor()
            stdoutThread.join()
            stderrThread.join()

            localrc
        } catch (ex: Exception) {
            ex.printStackTrace()
            throw stepConfig.exception(XProcError.xcOsExecFailed())
        }

        if (rc != 0) {
            stepConfig.warn { stdout.toString() }
            stepConfig.warn { stderr.toString() }
            throw stepConfig.exception(XProcError.xdStepFailed("Paged.js failed: ${rc}"))
        }

        val readPdf = FileInputStream(tempPdf)
        val buffer = ByteArray(4096)
        var len = readPdf.read(buffer)
        while (len >= 0) {
            out.write(buffer, 0, len)
            len = readPdf.read(buffer)
        }
        readPdf.close()

        while (tempFiles.isNotEmpty()) {
            val temp = tempFiles.removeAt(0)
            try {
                temp.delete()
            } catch (ex: Exception) {
                // nop
            }
        }
    }

    inner class ProcessOutputReader(val stream: InputStream, val buffer: ByteArrayOutputStream): Runnable {
        val tree = SaxonTreeBuilder(stepConfig)

        override fun run() {
            tree.startDocument(null)
            tree.addStartElement(NsC.result)
            val reader = InputStreamReader(stream)
            val buf = CharArray(4096)
            var len = reader.read(buf)
            while (len >= 0) {
                if (len == 0) {
                    Thread.sleep(250)
                } else {
                    // This is the most efficient way? Really!?
                    for (pos in 0 until len) {
                        buffer.write(buf[pos].code)
                    }
                }
                len = reader.read(buf)
            }
        }
    }
}