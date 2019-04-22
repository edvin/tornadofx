package tornadofx.testapps

import javafx.beans.property.SimpleBooleanProperty
import tornadofx.*

class TabPaneTestApp : App(TabPaneTest::class)

class TabPaneTest : View("TabPane Test") {
    override val root = tabpane {
        tab<TabOne>()
        tab<TabTwo>()
        // This is not the typical use case for using index, but instead when tab is added programmatically,
        // ex.: duplicate the current tab
        tab(1, "Tab Three", TabThree().root)
        // Here we verify that the original implementation works correctly, i.e. adds the tab to the end of the list
        tab("Tab Four", TabFour().root)
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

class TabTwo : View("Tab Two") {
    override val root = textfield(titleProperty)
    override val closeable = SimpleBooleanProperty(false)
}

class TabThree : View("Tab Three") {
    override val root = textfield(titleProperty)
    override val closeable = SimpleBooleanProperty(false)
}

class TabFour : View("Tab Four") {
    override val root = textfield(titleProperty)
    override val closeable = SimpleBooleanProperty(false)
}
