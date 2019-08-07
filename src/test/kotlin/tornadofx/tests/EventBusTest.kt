package tornadofx.tests

import javafx.application.Platform
import javafx.stage.Stage
import org.junit.Test
import org.testfx.api.FxToolkit
import tornadofx.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertTrue

class EventBusTest {
    val primaryStage: Stage = FxToolkit.registerPrimaryStage()
    private val success = AtomicBoolean(true)
    private val latch = Latch()

    init {
        FxToolkit.setupFixture {
            Thread.setDefaultUncaughtExceptionHandler { _, t ->
                t.printStackTrace()
                success.set(false)
                latch.countDown()
            }
        }
    }

    val iterations: Int = 100
    val count = AtomicInteger(0)
    val view = object : View() {

        init {
            for (i in 1..iterations) {
                subscribe<FXEvent>(times = i) {
                    callOnUndock()
                    print(" ${count.getAndIncrement()} ")
                    callOnDock()
                }
            }
        }

        override val root = vbox {}
    }

    @Test
    fun testUnsubscribe() {
        Platform.runLater {
            view.callOnDock()
            repeat(iterations / 2) {
                view.fire(FXEvent())
                view.fire(FXEvent(EventBus.RunOn.BackgroundThread))
            }
            latch.countDown()
        }
        latch.await()
        assertTrue(success.get())
    }
}

