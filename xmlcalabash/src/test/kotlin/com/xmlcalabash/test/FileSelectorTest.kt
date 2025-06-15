package com.xmlcalabash.test

import com.xmlcalabash.XmlCalabashBuilder
import com.xmlcalabash.datamodel.CompileEnvironment
import com.xmlcalabash.runtime.RuntimeEnvironment
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.fileselector.FileSelector
import com.xmlcalabash.util.fileselector.selectors.OwnedBySelector
import com.xmlcalabash.util.fileselector.selectors.PosixGroupSelector
import com.xmlcalabash.util.fileselector.selectors.PosixPermissionsSelector
import com.xmlcalabash.util.fileselector.selectors.SymlinkSelector
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.fail
import java.io.File
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermission
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileSelectorTest {
    companion object {
        val rootpath = File("${System.getProperty("user.dir")}/build/tmp-data")
        val builder = XmlCalabashBuilder()
        val xmlCalabash = builder.build()
        private val parser = xmlCalabash.newXProcParser()
        val instructionConfig = parser.builder.stepConfig
        val compileEnvironment = CompileEnvironment("", xmlCalabash)
        val environment = RuntimeEnvironment("ANEPISODE", compileEnvironment)
        val stepConfig = XProcStepConfiguration(xmlCalabash.saxonConfiguration,instructionConfig, environment)
    }

    @BeforeAll
    fun beforeAll() {
        val path = rootpath
        path.deleteRecursively()
        path.mkdirs()

        val file = path.resolve("original.txt")
        makeFile(file, "some data")

        try {
            Files.setPosixFilePermissions(file.toPath(),
                setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE))
        } catch (_: Exception) {
            // just ignore it
        }

        val dirpath = rootpath.toPath()
        val target = file.toPath()
        val link = dirpath.resolve("link.txt")
        Files.createSymbolicLink(link, target)
    }

    @AfterAll
    fun afterAll() {
        rootpath.deleteRecursively()
    }

    fun makeFile(file: File, contents: String, dateTime: Instant = Instant.now()) {
        val dir = file.parentFile
        dir.mkdirs()
        PrintStream(file).use { stream ->
            stream.print(contents)
        }
        file.setLastModified(dateTime.toEpochMilli())
    }

    @Test
    fun testSymbolicLinkSelector() {
        try {
            val fs = FileSelector()
            val dir = fs.directory(rootpath)
            val selector = SymlinkSelector()
            val files = dir.files(selector).map { it.localpath }
            Assertions.assertEquals(1, files.size)
            Assertions.assertEquals("/link.txt", files.first())
        } catch (ex: Exception) {
            fail()
        }
    }

    @Test
    fun testOwnedBySelector() {
        try {
            val file = rootpath.toPath().resolve("original.txt")
            val owner = Files.getOwner(file).name

            val fs = FileSelector()
            val dir = fs.directory(rootpath)
            val selector = OwnedBySelector(owner, true)
            val files = dir.files(selector).map { it.localpath }
            Assertions.assertEquals(2, files.size)
            Assertions.assertTrue(files.contains("/original.txt"))
            Assertions.assertTrue(files.contains("/link.txt"))
        } catch (ex: Exception) {
            fail()
        }
    }

    // To setup this test, pause execution at the start, add a file called
    // owner.txt to the directory and change the owner of original.txt to
    // someone else.
    @Disabled
    fun testOwnedBySelectorFollow() {
        try {
            val file = rootpath.toPath().resolve("owner.txt")
            val owner = Files.getOwner(file).name

            val fs = FileSelector()
            val dir = fs.directory(rootpath)
            val selector = OwnedBySelector(owner, true)
            val files = dir.files(selector).map { it.localpath }
            Assertions.assertEquals(1, files.size)
            Assertions.assertTrue(files.contains("/owner.txt"))
        } catch (ex: Exception) {
            fail()
        }
    }

    // To setup this test, pause execution at the start, add a file called
    // owner.txt to the directory and change the owner of original.txt to
    // someone else.
    @Disabled
    fun testOwnedBySelectorNoFollow() {
        try {
            val file = rootpath.toPath().resolve("owner.txt")
            val owner = Files.getOwner(file).name

            val fs = FileSelector()
            val dir = fs.directory(rootpath)
            val selector = OwnedBySelector(owner, false)
            val files = dir.files(selector).map { it.localpath }
            Assertions.assertEquals(2, files.size)
            Assertions.assertTrue(files.contains("/link.txt"))
            Assertions.assertTrue(files.contains("/owner.txt"))
        } catch (ex: Exception) {
            fail()
        }
    }

    @Test
    fun testGroupSelector() {
        var group = ""
        val file = rootpath.toPath().resolve("original.txt")
        try {
            val attr = Files.readAttributes(file, PosixFileAttributes::class.java)
            group = attr.group().name
        } catch (ex: Exception) {
            // Okay, must not support posix attributes
            return
        }

        try {
            val fs = FileSelector()
            val dir = fs.directory(rootpath)
            val selector = PosixGroupSelector(group, true)
            val files = dir.files(selector).map { it.localpath }
            Assertions.assertEquals(2, files.size)
            Assertions.assertTrue(files.contains("/original.txt"))
            Assertions.assertTrue(files.contains("/link.txt"))
        } catch (ex: Exception) {
            fail()
        }
    }

    // To setup this test, pause execution at the start, add a file called
    // owner.txt to the directory and change the group of original.txt to
    // someone else.
    @Disabled
    fun testGroupSelectorFollow() {
        var group = ""
        val file = rootpath.toPath().resolve("owner.txt")
        try {
            val attr = Files.readAttributes(file, PosixFileAttributes::class.java)
            group = attr.group().name
        } catch (ex: Exception) {
            // Okay, must not support posix attributes
            return
        }

        try {
            val fs = FileSelector()
            val dir = fs.directory(rootpath)
            val selector = PosixGroupSelector(group, true)
            val files = dir.files(selector).map { it.localpath }
            Assertions.assertEquals(1, files.size)
            Assertions.assertTrue(files.contains("/owner.txt"))
        } catch (ex: Exception) {
            fail()
        }
    }

    // To setup this test, pause execution at the start, add a file called
    // owner.txt to the directory and change the group of original.txt to
    // someone else.
    @Disabled
    fun testGroupSelectorNoFollow() {
        var group = ""
        val file = rootpath.toPath().resolve("owner.txt")
        try {
            val attr = Files.readAttributes(file, PosixFileAttributes::class.java)
            group = attr.group().name
        } catch (ex: Exception) {
            // Okay, must not support posix attributes
            return
        }

        try {
            val fs = FileSelector()
            val dir = fs.directory(rootpath)
            val selector = PosixGroupSelector(group, false)
            val files = dir.files(selector).map { it.localpath }
            Assertions.assertEquals(2, files.size)
            Assertions.assertTrue(files.contains("/link.txt"))
            Assertions.assertTrue(files.contains("/owner.txt"))
        } catch (ex: Exception) {
            fail()
        }
    }

    @Test
    fun testPermissionsSelector() {
        val file = rootpath.toPath().resolve("original.txt")
        try {
            Files.readAttributes(file, PosixFileAttributes::class.java)
        } catch (_: Exception) {
            // Okay, must not support posix attributes
            return
        }

        // The original.txt will have been created with rw-rw---- permissions
        try {
            val fs = FileSelector()
            val dir = fs.directory(rootpath)
            val selector = PosixPermissionsSelector("rw-rw----", true)
            val files = dir.files(selector).map { it.localpath }
            Assertions.assertEquals(2, files.size)
            Assertions.assertTrue(files.contains("/original.txt"))
            Assertions.assertTrue(files.contains("/link.txt"))
        } catch (ex: Exception) {
            fail()
        }
    }

    @Test
    fun testPermissionsSelectorNoFollow() {
        val file = rootpath.toPath().resolve("original.txt")
        try {
            Files.readAttributes(file, PosixFileAttributes::class.java)
        } catch (_: Exception) {
            // Okay, must not support posix attributes
            return
        }

        // The original.txt will have been created with rw-rw---- permissions
        try {
            val fs = FileSelector()
            val dir = fs.directory(rootpath)
            val selector = PosixPermissionsSelector("rw-rw----", false)
            val files = dir.files(selector).map { it.localpath }
            Assertions.assertEquals(1, files.size)
            Assertions.assertTrue(files.contains("/original.txt"))
        } catch (ex: Exception) {
            fail()
        }
    }
}
