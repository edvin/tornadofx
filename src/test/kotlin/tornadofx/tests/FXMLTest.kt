package tornadofx.tests

import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.junit.Test
import org.testfx.api.FxToolkit
import tornadofx.Fragment

class FXMLTest {
    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    @Test(expected = IllegalArgumentException::class)
    fun wrongFXId() {
        object : Fragment() {
            override val root: VBox by fxml("/FXMLTest.fxml")
            val myLabelx : Label by fxid()

            init {
                myLabelx.text = "Expect log warning and RuntimeException"
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun wrongFXType() {
        object : Fragment() {
            override val root: VBox by fxml("/FXMLTest.fxml")
            val myLabel : TextField by fxid()

            init {
                myLabel.text = "Expect log warning and RuntimeException"
            }
        }
    }
}