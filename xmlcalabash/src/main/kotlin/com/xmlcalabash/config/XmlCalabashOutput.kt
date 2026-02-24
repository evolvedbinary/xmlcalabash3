package com.xmlcalabash.config

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.util.UriUtils
import com.xmlcalabash.util.Urify
import java.io.File
import java.net.URI
import java.util.UnknownFormatConversionException

/**
 * Describe how output filenames should be constructed.
 *
 * This method returns a sequence of filenames from a pattern. Within the pattern, a percent sign (%)
 * followed by zero or more digits, followed by a latin small letter o (o), a latin small
 * letter d (d), a latin small letter x (x), or a latin capital letter x (X) will be replaced with
 * a sequence number. Files are numbered from 1.
 *
 * If the trailing letter is "o", the number will be formatted in octal, if the trailing digit is "d",
 * the number will be formatted in decimal, if the trailing letter is "x" or "X", the number will
 * be formatted in hexadecimal with either lower- or upper-case alphabetic digits.
 *
 * If one or more leading digits are provided, the number will be padded to that length.
 *
 * If "%%" appears in the pattern, it will be replaced by a single "%" in the generated filename.
 *
 * In other words the pattern `hello%%world%03x.xml` will produce a sequence of filenames:
 * `hello%world001.xml`, `hello%world002.xml`, ... `hello%worldfff.xml`, `hello%world1000.xml`, etc.
 *
 * The result of performing the sequential substutition on the pattern string must result in a valid filename for
 * the platform where the process is running.
 *
 * @param pattern the pattern string.
 */
open class XmlCalabashOutput(val port: String?, val pattern: String, val multipartMixed: Boolean = false, val multiplex: Boolean = false) {
    private val regex = "(%%)|(%[0-9]*[odxX]?)".toRegex()
    private var nextId = 1
    private var currentFilename: String = pattern

    init {
        if (multipartMixed && isSequential()) {
            throw IllegalArgumentException("No pattern is allowed when multipart/mixed output is requested")
        }
    }

    fun withPort(port: String): XmlCalabashOutput {
        return XmlCalabashOutput(port, pattern, multipartMixed, multiplex)
    }

    /**
     * Is this output filename going to produce a sequence of names?
     *
     * If the input pattern does not contain any substrings that can be used for sequential
     * numbering, the output filename is not sequential.
     */
    fun isSequential(): Boolean {
        var result: MatchResult? = regex.find(pattern) ?: return false

        if (result!!.value != "%%") {
            return true
        }

        result = result.next()
        while (result != null) {
            if (result.value != "%%") {
                return true
            }
            result = result.next()
        }

        return false
    }

    /**
     * Get the next file in the sequence.
     *
     * If [isSequential] is false, the same name will be returned each time.
     */
    fun nextFile(): File {
        currentFilename = pattern
        var result: MatchResult? = regex.find(pattern)
        if (result != null) {
            val sb = StringBuilder()
            if (result.range.first > 0) {
                sb.append(pattern.substring(0, result.range.first))
            }
            if (result.value == "%%") {
                sb.append("%")
            } else {
                try {
                    sb.append(String.format(result.value, nextId))
                } catch (ex: UnknownFormatConversionException) {
                    throw XProcError.xiCliInvalidTemplate(result.value).exception(ex)
                }
            }

            var nextPos = result.range.last + 1
            result = result.next()
            while (result != null) {
                if (result.range.first > nextPos) {
                    sb.append(pattern.substring(nextPos, result.range.first))
                }
                if (result.value == "%%") {
                    sb.append("%")
                } else {
                    sb.append(String.format(result.value, nextId))
                }
                nextPos = result.range.last + 1
                result = result.next()
            }

            if (nextPos < pattern.length) {
                sb.append(pattern.substring(nextPos))
            }

            currentFilename = sb.toString()
        }

        // This more than slightly ugly. We're going to pass the string through urify, so
        // any % that remains has to be re-encoded so it can be re-decoded. :-(
        val uri = URI(Urify.urify(currentFilename.replace("%", "%25")))
        if (uri.scheme != "file") {
            throw XProcError.xiUnwritableOutputFile(currentFilename).exception()
        }
        currentFilename = UriUtils.normalizePath(uri.path).replace("%25", "%")

        nextId++
        val file = File(currentFilename)
        if (file.exists() && file.isDirectory) {
            throw XProcError.xiUnwritableOutputFile(currentFilename).exception()
        }
        if (file.exists() && !file.canWrite()) {
            throw XProcError.xiUnwritableOutputFile(currentFilename).exception()
        }

        return file
    }

    override fun hashCode(): Int {
        val code = pattern.hashCode() + (3 * multipartMixed.hashCode()) + (5 * multiplex.hashCode())
        if (port != null) {
            return code + (11 * port.hashCode())
        }
        return code
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as XmlCalabashOutput
        return port == other.port && pattern == other.pattern
                && multipartMixed == other.multipartMixed && multiplex == other.multiplex
    }

    override fun toString(): String {
        return "${port ?: ""}: ${pattern}"
    }
}