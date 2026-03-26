import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.util.BufferingReceiver
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmNodeKind
import org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import javax.xml.XMLConstants

class SmokeTestElemental {

    private fun runPipeline(name: String): Map<String, MutableList<XProcDocument>> {
        val calabash = XmlCalabash.newInstance()
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

        assertEquals(1, resultPort.size)

        val value = resultPort[0].value
        assertTrue(value is XdmNode)
        var node = value as XdmNode
        assertEquals(XdmNodeKind.DOCUMENT, node.nodeKind)

        var children = node.children()!!.toList()
        assertEquals(1, children.size)
        node = children.get(0)
        assertEquals(XdmNodeKind.ELEMENT, node.nodeKind)
        val nodeName = node.nodeName!!

        // NOTE(AR) Requires Elemental 6.10.0 or 7.6.0 (or newer) to pass
        assertEquals(XMLConstants.NULL_NS_URI, nodeName.namespace!!)

        assertEquals(XMLConstants.DEFAULT_NS_PREFIX, nodeName.prefix)
        assertEquals("greeting", nodeName.localName);

        children = node.children()!!.toList()
        assertEquals(1, children.size)
        node = children.get(0)
        assertEquals(XdmNodeKind.TEXT, node.nodeKind)
        assertEquals("Hello, world.", node.toString())
    }
}
