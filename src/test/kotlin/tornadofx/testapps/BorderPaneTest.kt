package tornadofx.testapps

import tornadofx.*

class BorderPaneTestApp : App(BorderPaneTest::class)

class BorderPaneTest : View("Border Pane Builder Test") {
    override val root = borderpane {
        // Direct access
        top = label("Top")
        // Builder target
        center {
            hbox {
                label("Center")
            }
        }
    }
}