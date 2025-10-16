package com.xmlcalabash.util

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.exceptions.XProcError
import net.sf.saxon.s9api.SequenceType
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmEmptySequence
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.type.BuiltInAtomicType
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

class UriUtils {
    companion object {
        fun homeAsUri(): URI = dirAsUri(System.getProperty("user.home"))
        fun cwdAsUri(): URI = dirAsUri(System.getProperty("user.dir"))
        fun dirAsUri(dir: String): URI {
            return Paths.get(dir).toUri()
        }

        fun makeRelativeTo(base: URI, relative: URI): URI {
            if (relative.isOpaque || base.scheme != relative.scheme || base.authority != relative.authority) {
                return relative
            }

            val normParts = mutableListOf<String>()
            val relativeParts = mutableListOf<String>()

            normParts.addAll(path(base.normalize()).split("/"))
            relativeParts.addAll(path(relative.normalize()).split("/"))

            normParts.removeLast()
            val lastPart = relativeParts.removeLast()

            while (normParts.isNotEmpty() && relativeParts.isNotEmpty() && normParts.first() == relativeParts.first()) {
                normParts.removeFirst()
                relativeParts.removeFirst()
            }

            if (normParts.isEmpty()) {
                if (relativeParts.isEmpty()) {
                    return URI(lastPart)
                }
                return URI(relativeParts.joinToString("/") + "/" + lastPart)
            }

            val builder = StringBuilder()
            for (part in normParts) {
                builder.append("../")
            }
            builder.append(relativeParts.joinToString("/"))
            builder.append(lastPart)

            return URI(builder.toString())
        }

        fun couldBeWindowsPath(path: String): Boolean {
            if (!Urify.isWindows) {
                return false
            }

            // Because of the way java.net.URI deals with the path portion of a file:
            // URI on Windows, the path could be "C:..."
            if (path.length >= 2 && driveLetter(path[0]) && path[1] == ':') {
                return true
            }

            // or it could be "/C:...", both of those count as a Windows path.
            if (path.length >= 3 && path[0] == '/' && driveLetter(path[1]) && path[2] == ':') {
                return true
            }

            return !path.contains(':')
        }

        private fun driveLetter(cp: Char): Boolean {
            return (cp >= 'A' && cp <= 'Z') || (cp >= 'a' && cp <= 'z')
        }

        fun path(uri: URI): String {
            if (uri.scheme == "file") {
                if (uri.path == null) {
                    // If the path part is null, ???, just return everything after file:
                    // (This occurs for file:name (which isn't a valid file: URI) and returns "name".)
                    return uri.toString().substring(5);
                }
                return normalizePath(uri.path)
            }
            return uri.path
        }

        fun normalizePath(path: String): String {
            // #facepalm On windows, sometimes the path is /C:/path because
            // URI("file:///C:/path).path is /C:/path
            if (couldBeWindowsPath(path) && path.length > 2 && path[0] == '/' && path[2] == ':') {
                return path.substring(1).replace('\\', '/')
            } else {
                return path.replace('\\', '/')
            }
        }

        fun patchUriValue(config: StepConfiguration, value: XdmValue, asType: SequenceType): XdmValue {
            if (value == XdmEmptySequence.getInstance() || asType.underlyingSequenceType.primaryType != BuiltInAtomicType.ANY_URI) {
                return value
            }

            if (config.baseUri == null || ExtensionName.EAGER_URI_RESOLUTION !in config.xmlCalabashConfig.extensions) {
                return value
            }

            try {
                val absolute = UriUtils.resolve(config.baseUri!!, value.underlyingValue.stringValue)

                if (value.underlyingValue.stringValue != absolute.toString()) {
                    config.debug { "Made ${value.underlyingValue.stringValue} absolute: ${absolute}" }
                    return XdmAtomicValue(absolute!!)
                }

                return value
            } catch (ex: Exception) {
                throw config.exception(XProcError.xdInvalidUri(value.underlyingValue.stringValue), ex)
            }
        }

        fun resolve(path: URI?): URI {
            return resolve(path?.toString())
        }

        fun resolve(path: String?): URI {
            return resolve(cwdAsUri(), path)!!
        }

        fun resolve(baseUri: URI?, path: URI?): URI? {
            return resolve(baseUri, path?.toString())
        }

        fun resolve(baseUri: URI?, path: String?): URI? {
            if (baseUri == null) {
                return null
            }
            if (path == null) {
                return baseUri
            }
            if (couldBeWindowsPath(path)) {
                if (path.length > 1 && path[1] == ':') {
                    return URI("file:/${path.replace('\\', '/')}")
                }
                return baseUri.resolve(path.replace('\\', '/'))
            }
            return baseUri.resolve(path)
        }

        fun encodeForUri(value: String): String {
            val genDelims = ":/?#[]@"
            val subDelims = "!$'()*,;=" // N.B. no "&" and no "+" !
            val unreserved = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._~"
            val okChars = genDelims + subDelims + unreserved

            val encoded = StringBuilder()
            for (byte in value.toByteArray(StandardCharsets.UTF_8)) {
                // Whoever decided that bytes should be signed needs their head examined
                val bint = if (byte.toInt() < 0) {
                    byte.toInt() + 256
                } else {
                    byte.toInt()
                }
                val ch = Char(bint)
                if (okChars.indexOf(ch) >= 0) {
                    encoded.append(ch)
                } else {
                    if (ch == ' ') {
                        encoded.append("+")
                    } else {
                        encoded.append(String.format("%%%02X", ch.code))
                    }
                }
            }

            return encoded.toString()
        }

        fun escapeHtmlUri(value: String): String {
            val encoded = StringBuilder()
            for (byte in value.toByteArray(StandardCharsets.UTF_8)) {
                // Whoever decided that bytes should be signed needs their head examined
                val bint = if (byte.toInt() < 0) {
                    byte.toInt() + 256
                } else {
                    byte.toInt()
                }
                val ch = Char(bint)
                if (bint >= 32 && bint <= 126) {
                    encoded.append(ch)
                } else {
                    encoded.append(String.format("%%%02X", ch.code))
                }
            }

            return encoded.toString()
        }
    }
}