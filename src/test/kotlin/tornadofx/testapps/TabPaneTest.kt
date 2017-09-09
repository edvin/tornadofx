package tornadofx.testapps

import javafx.beans.property.SimpleBooleanProperty
import tornadofx.*

class TabPaneTestApp : App(TabPaneTest::class)

class TabPaneTest : View("TabPane Test") {
    override val root = tabpane {
        tab<TabOne>()
    }
}

/**
 * Check that the tab title updates when the input changes.
 * The Tab should not be closeable as it binds to the closeable property.
 */
class TabOne : View("Tab One") {
    override val root = textfield(titleProperty)
    override val closeable = SimpleBooleanProperty(false)
}
