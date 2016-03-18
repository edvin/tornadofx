package tornadofx

import javafx.scene.control.TableColumn
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import org.junit.Before
import org.junit.Test
import org.testfx.api.FxToolkit
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
}