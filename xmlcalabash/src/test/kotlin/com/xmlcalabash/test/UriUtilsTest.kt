package com.xmlcalabash.test

import com.xmlcalabash.util.UriUtils
import com.xmlcalabash.util.Urify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.URI

class UriUtilsTest {
    private var osName = "anonymous"
    private var osSep = "/"
    private var osCwd = "/tmp"

    private fun mockOs(name: String, cwd: String?) {
        osName = Urify.osname
        osSep = Urify.filesep
        osCwd = Urify.cwd

        if (name == "Windows") {
            Urify.mockOs(name, "\\", cwd ?: osCwd)
        } else {
            Urify.mockOs(name, "/", cwd ?: osCwd)
        }
    }

    private fun unMockOs() {
        Urify.mockOs(osName, osSep, osCwd)
    }

    @Test
    fun relativeUriParent() {
        try {
            mockOs("macOS", "/tmp")
            val baseUri = URI("file:///path/to/some/file")
            val altUri = URI("file:/path/to/some/other/file")
            val rel = UriUtils.makeRelativeTo(baseUri, altUri)
            Assertions.assertEquals("other/file", rel.toString())
        } finally {
            unMockOs()
        }
    }

    @Test
    fun relativeUriChild() {
        try {
            mockOs("macOS", "/tmp")
            val baseUri = URI("file:///path/to/some/file/under/here")
            val altUri = URI("file:///path/to/some/file/here")
            val rel = UriUtils.makeRelativeTo(baseUri, altUri)
            Assertions.assertEquals("../here", rel.toString())
        } finally {
            unMockOs()
        }
    }

    @Test
    fun relativeUriSame() {
        try {
            mockOs("macOS", "/tmp")
            val baseUri = URI("file:/C:/path/to/some/file/here")
            val altUri = URI("file:/C:/path/to/some/file/there")
            val rel = UriUtils.makeRelativeTo(baseUri, altUri)
            Assertions.assertEquals("there", rel.toString())
        } finally {
            unMockOs()
        }
    }

    @Test
    fun relativeUriDifferentPaths() {
        try {
            mockOs("macOS", "/tmp")
            val baseUri = URI("file:///path/to/file")
            val altUri = URI("file:///other/path/to/file")
            val rel = UriUtils.makeRelativeTo(baseUri, altUri)
            Assertions.assertEquals("/other/path/to/file", rel.path)
        } finally {
            unMockOs()
        }
    }

    @Test
    fun relativeUriParentWindows() {
        try {
            mockOs("Windows", "/tmp")
            val baseUri = URI("file:/C:/path/to/some/file")
            val altUri = URI("file:/c:/path/to/some/other/file")
            val rel = UriUtils.makeRelativeTo(baseUri, altUri)
            Assertions.assertEquals("other/file", rel.toString())
        } finally {
            unMockOs()
        }
    }

    @Test
    fun relativeUriChildWindows() {
        try {
            mockOs("Windows", "/tmp")
            val baseUri = URI("file:/C:/path/to/some/file/under/here")
            val altUri = URI("file:/C:/path/to/some/file/here")
            val rel = UriUtils.makeRelativeTo(baseUri, altUri)
            Assertions.assertEquals("../here", rel.toString())
        } finally {
            unMockOs()
        }
    }

    @Test
    fun relativeUriSameWindows() {
        try {
            mockOs("Windows", "/tmp")
            val baseUri = URI("file:/c:/path/to/some/file/here")
            val altUri = URI("file:/c:/path/to/some/file/there")
            val rel = UriUtils.makeRelativeTo(baseUri, altUri)
            Assertions.assertEquals("there", rel.toString())
        } finally {
            unMockOs()
        }
    }

    @Test
    fun relativeUriDifferentPathsWindows() {
        try {
            mockOs("Windows", "/tmp")
            val baseUri = URI("file:/c:/path/to/file")
            val altUri = URI("file:/c:/other/path/to/file")
            val rel = UriUtils.makeRelativeTo(baseUri, altUri)
            Assertions.assertEquals("/c:/other/path/to/file", rel.path)
        } finally {
            unMockOs()
        }
    }

    @Test
    fun relativeUriDifferentDrivesWindows() {
        try {
            mockOs("Windows", "/tmp")
            val baseUri = URI("file:/c:/path/to/file")
            val altUri = URI("file:/d:/other/path/to/file")
            val rel = UriUtils.makeRelativeTo(baseUri, altUri)
            Assertions.assertEquals("/d:/other/path/to/file", rel.path)
        } finally {
            unMockOs()
        }
    }
}