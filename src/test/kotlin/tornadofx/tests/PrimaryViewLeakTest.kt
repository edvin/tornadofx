package tornadofx.tests

import org.junit.Test
import org.testfx.api.FxToolkit
import tornadofx.*
import tornadofx.testapps.PrimaryViewLeakApp
import tornadofx.testapps.PrimaryViewLeakView
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PrimaryViewLeakTest {

    @Test
    fun itShouldNotLeakOnDock() {
        val views = mutableListOf<PrimaryViewLeakView>()
        fun cycleApp(views: MutableList<PrimaryViewLeakView>) {
            println("registering stage...")
            FxToolkit.registerPrimaryStage()
            println("setting up application...")
            val app = FxToolkit.setupApplication { PrimaryViewLeakApp() }
            views += find<PrimaryViewLeakView>()
            println("cleaning up stages...")
            FxToolkit.cleanupStages()
            println("cleaning up app...")
            FxToolkit.cleanupApplication(app)
        }

        for (i in 0..1) {
            cycleApp(views)
        }

        assertEquals(
                expected = 2,
                actual = views.size,
                message = "view size should be 2"
        )
        assertNotEquals(
                illegal = views[0].instanceId,
                actual = views[1].instanceId,
                message = "view instance IDs should be unique"
        )
        views.forEach {
            println("Asserting on view: ${it.instanceId}")
            assertEquals(
                    expected = 1,
                    actual = it.dockCounter,
                    message = "It should dock exactly once"
            )
        }
    }
}