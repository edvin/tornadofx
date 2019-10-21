package tornadofx.tests

import org.junit.Test
import org.testfx.api.FxToolkit
import tornadofx.*
import tornadofx.testapps.NoPrimaryViewDockingApp
import tornadofx.testapps.ViewDockingApp
import tornadofx.testapps.PrimaryDockingView
import tornadofx.testapps.SecondaryDockingView
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ViewDockingTests {

    @Test
    fun itShouldDock() {
        FxToolkit.registerPrimaryStage()
        val app = FxToolkit.setupApplication { ViewDockingApp() }

        assertEquals(
                expected = 1,
                actual = find<PrimaryDockingView>().dockCounter,
                message = "view should have docked exactly once"
        )

        FxToolkit.cleanupStages()
        FxToolkit.cleanupApplication(app)
    }

    @Test
    fun itShouldUndock() {
        FxToolkit.registerPrimaryStage()
        val app = FxToolkit.setupApplication { ViewDockingApp() }
        val view = find<PrimaryDockingView>()
        FxToolkit.cleanupStages()
        FxToolkit.cleanupApplication(app)

        assertEquals(
                expected = 1,
                actual = view.undockCounter,
                message = "view should have undocked exactly once"
        )
    }

    @Test
    fun itShouldDockAndUndockAgainAfterWindowHiddenAndShown() {
        val stage = FxToolkit.registerPrimaryStage()
        val app = FxToolkit.setupApplication { ViewDockingApp() }
        val view = find<PrimaryDockingView>()

        FX.runAndWait { stage.hide() }
        FX.runAndWait { stage.show() }

        FxToolkit.cleanupStages()
        FxToolkit.cleanupApplication(app)

        assertEquals(
                expected = 2,
                actual = view.dockCounter,
                message = "view should have docked exactly twice"
        )
        assertEquals(
                expected = 2,
                actual = view.undockCounter,
                message = "view should have undocked exactly twice"
        )
    }

    @Test
    fun itShouldDockAndUndockChildView() {
        FxToolkit.registerPrimaryStage()
        val app = FxToolkit.setupApplication { ViewDockingApp() }
        val primaryView = find<PrimaryDockingView>()
        val secondaryView = find<SecondaryDockingView>()

        FX.runAndWait { primaryView.root.add(secondaryView.root) }

        assertEquals(
                expected = 1,
                actual = secondaryView.dockCounter,
                message = "view should have docked exactly once"
        )

        FX.runAndWait { secondaryView.removeFromParent() }

        assertEquals(
                expected = 1,
                actual = secondaryView.undockCounter,
                message = "view should have undocked exactly once"
        )

        FxToolkit.cleanupStages()
        FxToolkit.cleanupApplication(app)
    }

    @Test
    fun itShouldNotLeakOnDock() {
        // proof of fix for https://github.com/edvin/tornadofx/issues/973
        val views = mutableListOf<PrimaryDockingView>()

        for (i in 0..1) {
            FxToolkit.registerPrimaryStage()
            val app = FxToolkit.setupApplication { ViewDockingApp() }
            views += find<PrimaryDockingView>()
            FxToolkit.cleanupStages()
            FxToolkit.cleanupApplication(app)
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
            assertEquals(
                    expected = 1,
                    actual = it.dockCounter,
                    message = "It should dock exactly once"
            )
        }
    }

    @Test
    fun itShouldNotUninitNoPrimaryViewSpecifiedComponent() {
        // proof of fix for https://github.com/edvin/tornadofx/issues/991
        FxToolkit.registerPrimaryStage()
        val app = FxToolkit.setupApplication { NoPrimaryViewDockingApp() }
        val primaryDockingView = find<PrimaryDockingView>()

        FxToolkit.cleanupStages()
        FxToolkit.cleanupApplication(app)

        assertEquals(
                expected = 1,
                actual = primaryDockingView.dockCounter,
                message = "It should dock exactly once"
        )
        assertEquals(
                expected = 1,
                actual = primaryDockingView.undockCounter,
                message = "It should undock exactly once"
        )
    }
}
