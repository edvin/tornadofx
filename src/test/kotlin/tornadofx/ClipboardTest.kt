package tornadofx

import javafx.scene.input.DataFormat
import javafx.stage.Stage
import org.junit.Assert
import org.junit.Test
import org.testfx.api.FxToolkit

class ClipboardTest {

    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    val CustomDataFormat = DataFormat("myFormat")

    @Test
    fun testClipboard() {
        val string1 = "TornadoFX Clipboard Test"
        val string2 = "TornadoFX Clipboard Test 2"

        FX.runAndWait {
            object : Controller() {
                init {
                    clipboard.setContent {
                        putString(string1)
                        put(CustomDataFormat, 42)
                    }

                    Assert.assertEquals(string1, clipboard.string)
                    Assert.assertEquals(42, clipboard.getContent(CustomDataFormat))

                    clipboard.putString(string2)
                    Assert.assertEquals(string2, clipboard.string)
                    Assert.assertNull(clipboard.getContent(CustomDataFormat))
                }
            }
        }

    }
}
