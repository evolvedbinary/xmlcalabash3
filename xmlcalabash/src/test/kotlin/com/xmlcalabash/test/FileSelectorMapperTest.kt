package com.xmlcalabash.test

import com.xmlcalabash.util.fileselector.mappers.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class FileSelectorMapperTest {
    @Test
    fun testChainedMapper() {
        val mapper = ChainedMapper(listOf(CutDirsMapper(2), GlobMapper("*.java", "*.xml")))
        val select = mapper.selects(listOf("a/file.java", "a/b/file.java", "a/b/c/file.JAVA", "a/b/c/d/file.java"))
        Assertions.assertEquals(listOf("file.xml", "c/d/file.xml"), select)
    }

    @Test
    fun testCompositeMapper() {
        val mapper = CompositeMapper(listOf(CutDirsMapper(2), GlobMapper("*.java", "*.xml")))
        val select = mapper.selects(listOf("a/file.java", "a/b/file.java", "a/b/c/file.JAVA", "a/b/c/d/file.java"))
        Assertions.assertEquals(listOf("file.java", "c/file.JAVA", "c/d/file.java", "a/file.xml", "a/b/file.xml", "a/b/c/d/file.xml"), select)
    }

    @Test
    fun testFirstMatchMapper() {
        val mapper = CompositeMapper(listOf(CutDirsMapper(5), GlobMapper("*.java", "*.xml"), GlobMapper("*.xml", "*.json")))
        val select = mapper.selects(listOf("a/file.java", "a/b/file.java", "a/b/c/file.JAVA", "a/b/c/d/file.java"))
        Assertions.assertEquals(listOf("a/file.xml", "a/b/file.xml", "a/b/c/d/file.xml"), select)
    }

    @Test
    fun testCutDirsMapper() {
        val mapper = CutDirsMapper(2)
        val select = mapper.selects(listOf("a/file", "a/b/file", "a/b/c/file", "a/b/c/d/file"))
        Assertions.assertEquals(listOf("file", "c/file", "c/d/file"), select)
    }

    @Test
    fun testFlattenMapper() {
        val mapper = FlattenMapper()
        val select = mapper.selects(listOf("a/file", "a/b/file", "a/b/c/file2", "a/b/c/d/file3"))
        Assertions.assertEquals(listOf("file", "file2", "file3"), select)
    }

    @Test
    fun testGlobMapper() {
        val mapper = GlobMapper("*.sgm", "*.xml")
        val select = mapper.selects(listOf("a/file.sgm", "/a/file.SGM", "/a/b/file.sgm", "a/b/c/file.json"))
        Assertions.assertEquals(listOf("a/file.xml", "/a/b/file.xml"), select)
    }

    @Test
    fun testGlobMapperCaseInsensitive() {
        val mapper = GlobMapper("*.sgm", "*.xml", false)
        val select = mapper.selects(listOf("a/file.sgm", "/a/file.SGM", "/a/b/file.sgm", "a/b/c/file.json"))
        Assertions.assertEquals(listOf("a/file.xml", "/a/file.xml", "/a/b/file.xml"), select)
    }

    @Test
    fun testIdentityMapper() {
        val mapper = IdentityMapper()
        val list = listOf("a/file.sgm", "/a/file.SGM", "/a/b/file.sgm", "a/b/c/file.json")
        val select = mapper.selects(list)
        Assertions.assertEquals(list, select)
    }

    @Test
    fun testMergeMapper() {
        val mapper = MergeMapper("merged")
        val list = listOf("a/file.sgm", "/a/file.SGM", "/a/b/file.sgm", "a/b/c/file.json")
        val select = mapper.selects(list)
        Assertions.assertEquals(listOf("merged"), select)
    }

    @Test
    fun testRegexMapper() {
        val mapper = RegexpMapper("^(.*)/([^/]+)/([^/]*)$", "\\1/\\2/\\2-\\3", false)
        val select = mapper.selects(listOf("A.java", "foo/bar/B.java", "C.properties", "Classes/dir/dir2/A.properties"))
        Assertions.assertEquals(listOf("foo/bar/bar-B.java", "Classes/dir/dir2/dir2-A.properties"), select)
    }

    @Test
    fun testRegexMapperCaseSensitive() {
        val mapper = RegexpMapper("^(.*)/([^/]+)/([^/]*java)$", "\\1/\\2/\\2-\\3")
        val select = mapper.selects(listOf("A.java", "foo/bar/B.JAVA"))
        Assertions.assertEquals(listOf<String>(), select)
    }

    @Test
    fun testRegexMapperCaseInsensitive() {
        val mapper = RegexpMapper("^(.*)/([^/]+)/([^/]*java)$", "\\1/\\2/\\2-\\3", false)
        val select = mapper.selects(listOf("A.java", "foo/bar/B.JAVA"))
        Assertions.assertEquals(listOf("foo/bar/bar-B.JAVA"), select)
    }

    @Test
    fun testPackageMapper() {
        val mapper = PackageMapper("(.*).java", "\\1.xml")
        val list = listOf("a/file.java", "/a/file.JAVA", "/a/b/file.java", "a/b/c/file.json")
        val select = mapper.selects(list)
        Assertions.assertEquals(listOf("a.file.xml", "a.b.file.xml"), select)
    }

    @Test
    fun testPackageMapperCaseInsensitive() {
        val mapper = PackageMapper("(.*).java", "\\1.xml", false)
        val list = listOf("a/file.java", "/a/file.JAVA", "/a/b/file.java", "a/b/c/file.json")
        val select = mapper.selects(list)
        Assertions.assertEquals(listOf("a.file.xml", "a.file.xml", "a.b.file.xml"), select)
    }


    @Test
    fun testUnpackageMapper() {
        val mapper = UnpackageMapper("(.*).java", "\\1.xml")
        val list = listOf("a.file.java", ".a.file.JAVA", ".a.b.file.java", "a.b.c.file.json")
        val select = mapper.selects(list)
        Assertions.assertEquals(listOf("a/file.xml", "/a/b/file.xml"), select)
    }

    @Test
    fun testUnpackageMapperCaseInsensitive() {
        val mapper = UnpackageMapper("(.*).java", "\\1.xml", false)
        val list = listOf("a.file.java", ".a.file.JAVA", ".a.b.file.java", "a.b.c.file.json")
        val select = mapper.selects(list)
        Assertions.assertEquals(listOf("a/file.xml", "/a/file.xml", "/a/b/file.xml"), select)
    }

}
