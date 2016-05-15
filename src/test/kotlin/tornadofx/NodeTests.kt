package tornadofx

import javafx.application.Platform
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import org.junit.Before
import org.junit.Test
import org.testfx.api.FxToolkit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeTests {
    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    lateinit var pane: StackPane

    @Before
    fun setup() {
        pane = StackPane()
    }

    @Test
    fun make_editable_boolean_column() {
        val column = TableColumn<Any, Boolean>()
        column.makeEditable()
        assertTrue(column.cellFactory != null)
    }

    @Test
    fun findComponents() {
        val view1 = View1()
        val view2 = View2()

        view1 += view2

        view2.tagRoot()

        assertEquals(view2, view1.find<View2>())
    }

    class View1 : View() {
        override val root = HBox(Label("View 1"))
    }

    class View2 : View() {
        override val root = HBox(Label("View 2"))
    }
}