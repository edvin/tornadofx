package tornadofx.tests

import org.junit.Ignore
import org.junit.Test
import org.testfx.api.FxToolkit
import tornadofx.*
import tornadofx.testapps.PrimaryViewLeakApp
import tornadofx.testapps.PrimaryViewLeakView
import kotlin.test.assertEquals

class PrimaryViewLeakTest {

    @Test
    @Ignore
    fun itShouldNotDockMultipleTimes() {
        val views = mutableListOf<PrimaryViewLeakView>()
        fun cycleApp(views: MutableList<PrimaryViewLeakView>) {
            FxToolkit.registerPrimaryStage()
            val app = FxToolkit.setupApplication { PrimaryViewLeakApp() }
            views += find<PrimaryViewLeakView>()
            FxToolkit.cleanupStages()
            FxToolkit.cleanupApplication(app)
        }

        for (i in 0..1) {
            cycleApp(views)
        }

        assertEquals(expected = 2, actual = views.size)
        views.forEach {
            assertEquals(expected = 1, actual = it.dockCounter)
        }
    }
}