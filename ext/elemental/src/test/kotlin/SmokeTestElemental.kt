import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.util.BufferingReceiver
import net.sf.saxon.s9api.ItemType
import net.sf.saxon.s9api.ItemTypeFactory
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmArray
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmNodeKind
import net.sf.saxon.s9api.XdmValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input
import java.io.File
import java.io.StringReader
import javax.xml.transform.stream.StreamSource

class SmokeTestElemental {

    private val calabash = XmlCalabash.newInstance()

    private fun runPipeline(name: String): Map<String, MutableList<XProcDocument>> {
        val parser = calabash.newXProcParser()
        val decl = parser.parse(File("src/test/resources/${name}.xpl").toURI())
        val runtime = decl.runtime()
        val exec = runtime.executable()

        val receiver = BufferingReceiver()
        exec.receiver = receiver

        exec.run()

        return receiver.outputs
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "elemental-remote-cx",
        "elemental-remote-q"
    ])
    fun testElemental(name: String) {
        val outputPorts = runPipeline(name)
        val resultPort = outputPorts["result"]!!

        assertEquals(23, resultPort.size)

        var i = 0
        var value: XdmValue? = null

        value = resultPort[i++].value
        assertTrue(value is XdmNode)
        assertXmlEquals("<greeting>Hello, world.</greeting>", value as XdmNode)

        value = resultPort[i++].value
        assertTrue(value is XdmNode)
        assertXmlEquals("<st:greeting xmlns:st=\"http://smoke-test\">Hello, continent.</st:greeting>", value as XdmNode)

        value = resultPort[i++].value
        assertTrue(value is XdmNode)
        assertXmlEquals("<greeting>Hello, country.</greeting>", value as XdmNode)

        value = resultPort[i++].value
        assertTrue(value is XdmNode)
        assertAttributeEquals(QName("other", "http://other", "x"), "y", value as XdmNode)

        value = resultPort[i++].value
        assertTrue(value is XdmNode)
        assertAttributeEquals(QName("st", "http://smoke-test", "greeting"), "Hello, region.", value as XdmNode)

        value = resultPort[i++].value
        assertTrue(value is XdmNode)
        assertAttributeEquals(QName("greeting"), "Hello, state.", value as XdmNode)

        value = resultPort[i++].value
        assertTrue(value is XdmNode)
        assertTextEquals("greeting: Hello, town.", value as XdmNode)

        value = resultPort[i++].value
        assertTrue(value is XdmNode)
        assertCommentEquals("greeting: Hello, village.", value as XdmNode)

        value = resultPort[i++].value
        assertTrue(value is XdmNode)
        assertProcessingInstructionsEquals("greeting", "Hello", value as XdmNode)

        value = resultPort[i++].value
        assertTrue(value is XdmAtomicValue)
        assertValueEquals(ItemType.STRING, "greeting: Hello, hamlet.", value as XdmAtomicValue)

        value = resultPort[i++].value
        assertTrue(value is XdmAtomicValue)
        assertValueEquals(ItemType.INTEGER, "1234567890", value as XdmAtomicValue)

        value = resultPort[i++].value
        assertTrue(value is XdmAtomicValue)
        assertValueEquals(ItemType.NON_POSITIVE_INTEGER, "-123456789", value as XdmAtomicValue)

        value = resultPort[i++].value
        assertTrue(value is XdmAtomicValue)
        assertValueEquals(ItemType.FLOAT, "1.2", value as XdmAtomicValue)

        value = resultPort[i++].value
        assertTrue(value is XdmAtomicValue)
        assertValueEquals(ItemType.DOUBLE, "2.3", value as XdmAtomicValue)

        value = resultPort[i++].value
        assertTrue(value is XdmAtomicValue)
        assertValueEquals(ItemType.TIME, "02:00:00Z", value as XdmAtomicValue)

        value = resultPort[i++].value
        assertTrue(value is XdmAtomicValue)
        assertValueEquals(ItemType.DATE_TIME, "1981-02-04T02:00:00Z", value as XdmAtomicValue)

        value = resultPort[i++].value
        assertTrue(value is XdmAtomicValue)
        assertValueEquals(ItemType.DATE_TIME_STAMP, "1981-02-04T02:00:00Z", value as XdmAtomicValue)

        value = resultPort[i++].value
        assertTrue(value is XdmMap)
        assertMapEquals(ItemType.ANY_MAP, mapOf(
            XdmAtomicValue("x", ItemType.STRING) to XdmAtomicValue("y")
        ), value as XdmMap)

        value = resultPort[i++].value
        assertTrue(value is XdmMap)
        assertMapEquals(ItemType.ANY_MAP, mapOf(
                XdmAtomicValue("key1", ItemType.STRING) to XdmAtomicValue("greeting1"),
                XdmAtomicValue("key2", ItemType.STRING) to XdmAtomicValue("greeting2"),
                XdmAtomicValue("key3", ItemType.STRING) to asElement("<greeting>Hello, country.</greeting>")
            ), value as XdmMap)

        value = resultPort[i++].value
        assertTrue(value is XdmMap)
        assertMapEquals(ItemType.ANY_MAP, mapOf(
                XdmAtomicValue("key1", ItemType.STRING) to XdmAtomicValue("greeting1"),
                XdmAtomicValue("key2", ItemType.STRING) to XdmMap(mapOf(
                        XdmAtomicValue("key3", ItemType.STRING) to asElement("<greeting>Hello, country.</greeting>")
            ))
        ), value as XdmMap)

        value = resultPort[i++].value
        assertTrue(value is XdmArray)
        assertArrayEquals(ItemType.ANY_ARRAY, listOf(
            XdmValue(listOf(
                XdmAtomicValue("x")
            )),
            XdmValue(listOf(
                XdmAtomicValue("y")
            ))
        ), value as XdmArray)

        value = resultPort[i++].value
        assertTrue(value is XdmArray)
        assertArrayEquals(ItemType.ANY_ARRAY, listOf(
                XdmValue(listOf(
                    XdmAtomicValue("greeting"),
                    XdmAtomicValue("you")
                )),
                asElement("<greeting>Hello, country.</greeting>")
        ), value as XdmArray)

        value = resultPort[i++].value
        assertTrue(value is XdmArray)
        assertArrayEquals(ItemType.ANY_ARRAY, listOf(
            XdmValue(listOf(
                XdmAtomicValue("greeting")
            )),
            XdmArray(listOf(XdmAtomicValue("you")))
        ), value as XdmArray)
    }

    private fun assertXmlEquals(expectedNode: XdmNode, actualNode: XdmNode) {
        val serializer = calabash.saxonConfiguration.processor.newSerializer()
        val expected = serializer.serializeNodeToString(expectedNode)
        val actual = serializer.serializeNodeToString(actualNode)
        assertXmlEquals(expected, actual)
    }

    private fun assertXmlEquals(expected: String, actualNode: XdmNode) {
        val serializer = calabash.saxonConfiguration.processor.newSerializer()
        val actual = serializer.serializeNodeToString(actualNode)
        assertXmlEquals(expected, actual)
    }

    private fun assertXmlEquals(expected: String, actual: String) {
        val expectedSource = Input.fromString(expected).build()
        val actualSource = Input.fromString(actual).build()

        val diff = DiffBuilder.compare(expectedSource)
            .withTest(actualSource)
            .checkForSimilar()
            .build()

        assertFalse(diff.hasDifferences(), diff.toString())
    }

    private fun assertAttributeEquals(expectedName: QName, expectedValue: String, actualNode: XdmNode) {
        assertEquals(XdmNodeKind.ATTRIBUTE, actualNode.nodeKind)
        assertEquals(expectedName, actualNode.nodeName)
        assertEquals(expectedValue, actualNode.stringValue)
    }

    private fun assertTextEquals(expectedValue: String, actualNode: XdmNode) {
        assertEquals(XdmNodeKind.TEXT, actualNode.nodeKind)
        assertEquals(expectedValue, actualNode.stringValue)
    }

    private fun assertCommentEquals(expectedValue: String, actualNode: XdmNode) {
        assertEquals(XdmNodeKind.COMMENT, actualNode.nodeKind)
        assertEquals(expectedValue, actualNode.stringValue)
    }

    private fun assertProcessingInstructionsEquals(expectedTarget: String, expectedData: String, actualNode: XdmNode) {
        assertEquals(XdmNodeKind.PROCESSING_INSTRUCTION, actualNode.nodeKind)
        assertEquals(QName(expectedTarget), actualNode.nodeName)
        assertEquals(expectedData, actualNode.stringValue)
    }

    private fun assertValueEquals(expectedType: ItemType, expectedLexicalRepresentation: String, actualValue: XdmAtomicValue) {
        val actualType = ItemTypeFactory(calabash.saxonConfiguration.processor).getItemType(actualValue)
        assertEquals(expectedType, actualType)
        val expectedValue = XdmAtomicValue(expectedLexicalRepresentation, expectedType)
        assertEquals(expectedValue, actualValue)
    }

    private fun assertMapEquals(expectedType: ItemType, expectedEntries: Map<XdmAtomicValue, XdmValue>, actualMap: XdmMap) {
        val actualType = ItemTypeFactory(calabash.saxonConfiguration.processor).getItemType(actualMap)
        assertEquals(expectedType, actualType)
        val expectedMap = XdmMap(expectedEntries)
        assertMapEquals(expectedMap, actualMap)
    }

    private fun assertMapEquals(expectedMap: XdmMap, actualMap: XdmMap) {
        assertEquals(expectedMap.size(), actualMap.size())
        expectedMap.entrySet().forEach { expectedEntry ->
            val expectedKey = expectedEntry.key
            val expectedValue = expectedEntry.value

            val actualValue = actualMap.get(expectedKey)
            assertNotNull(actualValue)
            assertSequenceEquals(expectedValue, actualValue)
        }
    }

    private fun assertArrayEquals(expectedType: ItemType, expectedArray: List<XdmValue>, actualArray: XdmArray) {
        val actualType = ItemTypeFactory(calabash.saxonConfiguration.processor).getItemType(actualArray)
        assertEquals(expectedType, actualType)
        val expectedArray = XdmArray(expectedArray)
        assertArrayEquals(expectedArray, actualArray)
    }

    private fun assertArrayEquals(expectedArray: XdmArray, actualArray: XdmArray) {
        assertEquals(expectedArray.size(), actualArray.size())
        expectedArray.asList().forEachIndexed { expectedIndex, expectedValue ->
            val actualValue = actualArray[expectedIndex]
            assertNotNull(actualValue)
            assertSequenceEquals(expectedValue, actualValue)
        }
    }

    private fun assertSequenceEquals(expectedSequence: XdmValue, actualSequence: XdmValue) {
        assertEquals(expectedSequence.size(), actualSequence.size())
        expectedSequence.forEachIndexed { index, expectedItem ->
            val actualItem = actualSequence.itemAt(index)
            assertNotNull(actualItem)

            if (expectedItem is XdmNode && actualItem is XdmNode) {
                assertXmlEquals(expectedItem, actualItem)

            } else if (expectedItem is XdmMap && actualItem is XdmMap) {
                assertMapEquals(expectedItem, actualItem)

            } else if (expectedItem is XdmArray && actualItem is XdmArray) {
                assertArrayEquals(expectedItem, actualItem)

            } else {
                assertEquals(expectedItem, actualItem)
            }
        }
    }

    private fun asElement(xml: String) : XdmNode {
        val builder = calabash.saxonConfiguration.processor.newDocumentBuilder()
        return builder.build(StreamSource(StringReader(xml)))
    }
}
