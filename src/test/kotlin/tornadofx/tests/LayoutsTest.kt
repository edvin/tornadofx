package tornadofx.tests

import javafx.scene.Scene
import javafx.scene.control.ScrollPane
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.junit.Test
import org.testfx.api.FxToolkit
import tornadofx.*
import kotlin.test.assertEquals

/**
 *
 * @author Anindya Chatterjee
 */
class RegionTest {
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
}