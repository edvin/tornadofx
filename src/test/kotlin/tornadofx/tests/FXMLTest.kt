package tornadofx.tests

import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.junit.Assert
import org.junit.Test
import org.testfx.api.FxToolkit
import tornadofx.Fragment
import tornadofx.View
import tornadofx.find

class FXMLTest {
    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    @Test(expected = IllegalArgumentException::class)
    fun wrongFXId() {
        object : Fragment() {
            override val root: VBox by fxml("/tornadofx/tests/FXMLTest.fxml")
            val myLabelx : Label by fxid()

            init {
                myLabelx.text = "Expect log warning and RuntimeException"
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun wrongFXType() {
        object : Fragment() {
            override val root: VBox by fxml("/tornadofx/tests/FXMLTest.fxml")
            val myLabel : TextField by fxid()

            init {
                myLabel.text = "Expect log warning and RuntimeException"
            }
        }
    }

    @Test
    fun nestedFXML() {
        val outside = find<Outside>()
        Assert.assertNotNull(outside.insideController.root)
    }
}

class Outside : View() {
    override val root: VBox by fxml("/tornadofx/tests/Outside.fxml")
    val insideController: Inside by fxid()
}

class Inside : View() {
    override val root: Button by fxml()
}