package tornadofx.tests

import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.junit.Before
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

    lateinit var pane: Pane
    lateinit var vbox: VBox

    @Before
    fun setup() {
        pane = Pane()
    }

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
}