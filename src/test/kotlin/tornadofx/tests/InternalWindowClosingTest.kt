package tornadofx.tests

import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.stage.Stage
import org.junit.Test
import org.testfx.api.FxToolkit
import tornadofx.*
import kotlin.test.*

/**
 * Tests if it's possible to open an [InternalWindow] instance and then close it using [UIComponent.close]
 */
class InternalWindowClosingTest {
    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    @Test
    fun testClosing() {
        val owner = Pane()

        val view = object : View() {
            override val root = pane()
            fun executeClose() { close() }
        }

        FxToolkit.setupFixture {
            primaryStage.scene = Scene(owner)
            primaryStage.show()
            val iw = InternalWindow(null, true, true, true);
            iw.open(view, owner)
            assertNotNull(iw.scene)
            assertTrue(view.isDocked)
            view.executeClose()
            assertNull(iw.scene)
            assertFalse(view.isDocked)
        }
    }
}