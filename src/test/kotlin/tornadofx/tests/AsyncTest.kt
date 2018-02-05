package tornadofx.tests

import javafx.scene.control.Button
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import org.junit.Assert
import org.junit.Test
import org.testfx.api.FxToolkit
import tornadofx.*
import java.util.concurrent.CountDownLatch
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AsyncTest {
    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    @Test
    fun runAsyncWithOverlay() {
        //Initially container has embedded node
        val container = BorderPane()
        val node = Pane()
        val mask = Pane()

        container.center = node
        assertEquals(container.center, node)

        //This latch will be used to control asynchronously executed task, it's a part of the test
        val latch = Latch()
        assertTrue(latch.locked)

        //This is a standard CountDownLatch and will be used to synchronize test thread with application thread
        val appThreadLatch = CountDownLatch(1)

        //ui is needed here because only after task is executed we can check if overlay is removed propertly
        //and it can only be used from within a UIComponent, so we need a temporary object
        val component = object : Controller() {
            fun run() = node.runAsyncWithOverlay(latch, null, mask) ui { appThreadLatch.countDown() }
        }
        component.run()

        //Until latch is released, a node is replaced with StackPane containing both the original component and the mask
        assertNotEquals(node, container.center)
        assertTrue(container.center is StackPane)
        assertTrue((container.center as StackPane).children.containsAll(setOf(node, mask)))

        //The working thread (test thread in this case) proceeds, which should result in removing overlay
        latch.release()
        assertFalse(latch.locked)

        //Waiting until we have confirmed post-task ui execution
        appThreadLatch.await()

        //At this point overlay should be removed and node should be again in its original place
        assertEquals(node, container.center)
    }

    @Test
    fun latch() {
        //Latch set for 5 concurrent tasks
        val count = 5
        val latch = Latch(count)
        val button = Button()

        assertFalse(button.disabledProperty().value)

        //Button should stay disabled until all tasks are over
        button.disableWhen(latch.lockedProperty())

        (1..count).forEach {
            assertTrue(button.disabledProperty().value)
            assertTrue(latch.locked)
            assertTrue(latch.lockedProperty().value)
            latch.countDown()
            //Latch count should decrease after each iteration
            Assert.assertEquals(count, (it + latch.count).toInt())
        }

        assertFalse(button.disabledProperty().value)
        assertFalse(latch.locked)

        //Should have no effect anyway
        latch.release()
        assertFalse(button.disabledProperty().value)
        assertFalse(latch.locked)
    }

    @Test
    fun runAsync() {
        tornadofx.runAsync(daemon = true) {
            assertTrue { Thread.currentThread().isDaemon }
        }
        tornadofx.runAsync {
            assertFalse { Thread.currentThread().isDaemon }
        }
    }

}