package tornadofx.tests

import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import org.junit.Before
import org.junit.Test
import org.testfx.api.FxToolkit
import tornadofx.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeTest {
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
        val view1 = find<View1>()
        val view2 = find<View2>()
        val view3 = find<View3>()

        view1 += view2
        view1 += view3

        assertEquals(view2, view1.lookup<View2>())
        assertEquals(view3, view1.lookup<View3>())
        assertEquals(2, view1.findAll<View>().size)
    }

    class View1 : View() { override val root = HBox(Label("View 1")) }
    class View2 : View() { override val root = HBox(Label("View 2")) }
    class View3 : View() { override val root = HBox(Label("View 3")) }
}