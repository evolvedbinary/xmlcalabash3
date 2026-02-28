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

            // You'd think that if they were file: URIs, the easy thing would be to use java.nio.Path's
            // relativize(), but you'd be wrong. That gets cranky about /c:/path filenames and throws
            // an exception of the base and relative parts are on different drives on Windows. Bah, humbug.

            val normParts = mutableListOf<String>()
            val relativeParts = mutableListOf<String>()

            normParts.addAll(path(base.normalize()).split("/"))
            relativeParts.addAll(path(relative.normalize()).split("/"))

            // It doesn't appear to be practical to determine if the filesystem(s) in question are
            // case sensitive or not. They're URIs, so I'm going to treat them as case sensitive,
            // except for the drive letter on Windows. That is case-insensitive. I don't have a
            // solid rationale for that, really, but it's better than calling C: and c: different.
            if (Urify.isWindows) {
                if (normParts.isNotEmpty() && normParts[0].length > 1 && normParts[0][1] == ':') {
                    normParts[0] = normParts[0].uppercase()
                }
                if (relativeParts.isNotEmpty() && relativeParts[0].length > 1 && relativeParts[0][1] == ':') {
                    relativeParts[0] = relativeParts[0].uppercase()
                }
            }

            //println("NRT: ${normParts.joinToString(" : ")}")
            //println("RRT: ${relativeParts.joinToString(" : ")}")

            if (Urify.isWindows) {
                // X:/path vs. Y:/path
                if (normParts.isNotEmpty() && relativeParts.isNotEmpty() && normParts[0] != relativeParts[0]) {
                    // They differ right from the start, no point trying to find a common ancestor
                    return relative
                }

                // C:/path vs C:/other
                if (normParts.size > 1 && relativeParts.size > 1 && normParts[1] != relativeParts[1]) {
                    // They differ right from the start, no point trying to find a common ancestor
                    return relative
                }

            } else {
                // /path/to/place vs /other/path/to/place
                // In this case, the first part is always ""
                if (normParts.size > 1 && relativeParts.size > 1 && normParts[0] == relativeParts[0]
                    && normParts[0] == "" && normParts[1] != relativeParts[1]) {
                    // They differ right from the start, no point trying to find a common ancestor
                    return relative
                }
            }

            normParts.removeLast()
            val lastPart = relativeParts.removeLast()

            while (normParts.isNotEmpty() && relativeParts.isNotEmpty() && normParts.first() == relativeParts.first()) {
                normParts.removeFirst()
                relativeParts.removeFirst()
            }

            //println("TNT: ${normParts.joinToString(" : ")}")
            //println("TRT: ${relativeParts.joinToString(" : ")}")

            if (Urify.isWindows && relativeParts.isNotEmpty() && relativeParts[0].length > 1 && relativeParts[0][1] == ':') {
                return URI(Urify.urify(relativeParts.joinToString("\\")))
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
            if (relativeParts.isNotEmpty()) {
                builder.append(relativeParts.joinToString("/"))
                builder.append("/")
            }
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