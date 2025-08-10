import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.util.BufferingReceiver
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.io.File

class SmokeTestBaseX {
    private fun runPipeline(name: String) {
        val calabash = XmlCalabash.newInstance()
        val parser = calabash.newXProcParser()
        val decl = parser.parse(File("src/test/resources/${name}.xpl").toURI())
        val runtime = decl.runtime()
        val exec = runtime.executable()

        val receiver = BufferingReceiver()
        exec.receiver = receiver
        try {
            exec.run()
        } catch (ex: Exception) {
            System.err.println(ex.message)
            fail()
        }

        for (result in receiver.outputs["result"]!!) {
            println(result.value)
        }
    }

    @Test
    fun testBaseX() {
        runPipeline("basex")
    }

    @Test
    fun testContext() {
        runPipeline("context")
    }

}