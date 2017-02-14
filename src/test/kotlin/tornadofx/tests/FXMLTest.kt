package tornadofx.tests

import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.junit.Assert
import org.junit.Test
import org.testfx.api.FxToolkit
import tornadofx.App
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

    @Test
    fun simpleFormFXML() {
        val simpleForm = find<SimpleForm>()
        Assert.assertNotNull(simpleForm.tfField1)
        Assert.assertNotNull(simpleForm.tfField2)
        Assert.assertNotNull(simpleForm.tfField3)
    }

    @Test
    fun composedFormFXML() {

        val composedForm = find<ComposedForm>()

        Assert.assertNotNull(composedForm.searchPane)
        Assert.assertNotNull(composedForm.listPane)

        // searchPane > HBox > tfSearch
        Assert.assertTrue(
            (composedForm.searchPane.children[0] as Parent).childrenUnmodifiable[1] is TextField )

        // listPane > AnchorPane > lvItems
        Assert.assertEquals(ListView::class.java,
                (composedForm.listPane.children[0] as Parent).childrenUnmodifiable[0].javaClass)

    }

}

class Outside : View() {
    override val root: VBox by fxml("/tornadofx/tests/Outside.fxml")
    val insideController: Inside by fxid()
}

class Inside : View() {
    override val root: Button by fxml()
}

class SimpleForm : View() {
    override val root: VBox by fxml("/tornadofx/tests/SimpleForm.fxml")
    val tfField1 : TextField by fxid()
    val tfField2 : TextField by fxid()
    val tfField3 : TextField by fxid()
    fun ok() {}
    fun cancel() {}
}

class ComposedForm : View() {
    override val root : VBox by fxml("/tornadofx/tests/ComposedForm.fxml")
    val searchPane : Pane by fxid()
    val listPane : Pane by fxid()
    fun ok() {}
    fun cancel() {}
}

class ComposedFormListController : View() {
    override val root : AnchorPane by fxml("/tornadofx/test/ComposedFormList.fxml")
}

class ComposedFormSearchController : View() {
    override val root : HBox by fxml("/torandofx/test/ComposedFormSearch.fxml")
    val tfSearch : TextField by fxid()
    fun search() {
        println("search=" + tfSearch.text)
    }
}

class ComposedFormApp : App(ComposedForm::class)