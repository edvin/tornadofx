package tornadofx.tests

import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import javafx.stage.Stage
import org.junit.Test
import org.testfx.api.FxToolkit
import tornadofx.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 *
 * @author Anindya Chatterjee
 */
class RegionTest {
    val GridPaneRowIdKey = "TornadoFX.GridPaneRowId"
    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    lateinit var vbox: VBox

    @Test
    fun testFitToParentSize() {
        FxToolkit.setupFixture {
            val root = Pane().apply {
                vbox {
                    this@RegionTest.vbox = this
                    fitToParentSize()
                }
                setPrefSize(400.0, 160.0)
            }
            primaryStage.scene = Scene(root)
            primaryStage.show()

            assertEquals(root.width, vbox.width)
            assertEquals(root.height, vbox.height)
        }
    }

    @Test
    fun testFitToSize() {
        FxToolkit.setupFixture {
            val root = ScrollPane().apply {
                content = vbox {
                    this@RegionTest.vbox = this
                }
                setPrefSize(400.0, 160.0)
            }

            vbox.fitToSize(root)
            primaryStage.scene = Scene(root)
            primaryStage.show()

            assertEquals(root.width, vbox.width)
            assertEquals(root.height, vbox.height)
        }
    }

    @Test
    fun testEmptyGridPane() {
        FxToolkit.setupFixture {
            val root = GridPane().apply {
            }

            assertFalse(root.properties.containsKey(GridPaneRowIdKey))
            assertEquals(root.children, emptyList<Node>().observable())
        }
    }

    @Test
    fun testGridPaneAddRows() {
        lateinit var label: Label
        lateinit var text1: Text
        lateinit var text2: Text
        lateinit var text3: Text
        lateinit var vbox: VBox
        FxToolkit.setupFixture {
            val root = GridPane().apply {
                row {
                    label = label()
                }
                row {
                    text1 = text()
                    text2 = text()
                    text3 = text()
                }
                row {
                    vbox = vbox()
                }

            }

            assertEquals(root.properties[GridPaneRowIdKey], 2)
            assertEquals(GridPane.getRowIndex(label), 0)
            assertEquals(GridPane.getColumnIndex(label), 0)
            assertEquals(GridPane.getRowIndex(text1), 1)
            assertEquals(GridPane.getColumnIndex(text1), 0)
            assertEquals(GridPane.getRowIndex(text2), 1)
            assertEquals(GridPane.getColumnIndex(text2), 1)
            assertEquals(GridPane.getRowIndex(text3), 1)
            assertEquals(GridPane.getColumnIndex(text3), 2)
            assertEquals(GridPane.getRowIndex(vbox), 2)
            assertEquals(GridPane.getColumnIndex(vbox), 0)
            assertEquals(root.children, listOf(label, text1, text2, text3, vbox).observable())
        }
    }

    @Test
    fun testGridPaneRemoveNoRow() {
        val label = Label()
        FxToolkit.setupFixture {
            val root = GridPane().apply {
            }

            root.removeRow(label)

            assertFalse(root.properties.containsKey(GridPaneRowIdKey))
            assertEquals(root.children, emptyList<Node>().observable())
        }
    }

    @Test
    fun testGridPaneRemoveSingleRow() {
        lateinit var label: Label
        FxToolkit.setupFixture {
            val root = GridPane().apply {
                row {
                    label = label()
                }
            }

            root.removeRow(label)

            assertFalse(root.properties.containsKey(GridPaneRowIdKey))
            assertNull(GridPane.getRowIndex(label))
            assertNull(GridPane.getColumnIndex(label))
            assertEquals(root.children, emptyList<Node>().observable())
        }
    }

    @Test
    fun testGridPaneRemoveMiddleRow() {
        lateinit var label: Label
        lateinit var text1: Text
        lateinit var text2: Text
        lateinit var text3: Text
        lateinit var vbox: VBox
        FxToolkit.setupFixture {
            val root = GridPane().apply {
                row {
                    label = label()
                }
                row {
                    text1 = text()
                    text2 = text()
                    text3 = text()
                }
                row {
                    vbox = vbox()
                }

            }

            root.removeRow(text1)

            assertEquals(root.properties[GridPaneRowIdKey], 1)
            assertEquals(GridPane.getRowIndex(label), 0)
            assertEquals(GridPane.getColumnIndex(label), 0)
            assertNull(GridPane.getRowIndex(text1))
            assertNull(GridPane.getColumnIndex(text1))
            assertNull(GridPane.getRowIndex(text2))
            assertNull(GridPane.getColumnIndex(text2))
            assertNull(GridPane.getRowIndex(text3))
            assertNull(GridPane.getColumnIndex(text3))
            assertEquals(GridPane.getRowIndex(vbox), 1)
            assertEquals(GridPane.getColumnIndex(vbox), 0)
            assertEquals(root.children, listOf<Node>(label, vbox).observable())
        }
    }

    @Test
    fun testGridPaneRemoveAllRows() {
        lateinit var label: Label
        lateinit var text1: Text
        lateinit var text2: Text
        lateinit var text3: Text
        lateinit var vbox: VBox
        FxToolkit.setupFixture {
            val root = GridPane().apply {
                row {
                    label = label()
                }
                row {
                    text1 = text()
                    text2 = text()
                    text3 = text()
                }
                row {
                    vbox = vbox()
                }

            }

            root.removeAllRows()

            assertFalse(root.properties.containsKey(GridPaneRowIdKey))
            assertNull(GridPane.getRowIndex(label))
            assertNull(GridPane.getColumnIndex(label))
            assertNull(GridPane.getRowIndex(text1))
            assertNull(GridPane.getColumnIndex(text1))
            assertNull(GridPane.getRowIndex(text2))
            assertNull(GridPane.getColumnIndex(text2))
            assertNull(GridPane.getRowIndex(text3))
            assertNull(GridPane.getColumnIndex(text3))
            assertNull(GridPane.getRowIndex(vbox))
            assertNull(GridPane.getColumnIndex(vbox))
            assertEquals(root.children, emptyList<Node>().observable())
        }
    }
}