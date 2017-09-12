package tornadofx.testapps

import tornadofx.*

class BorderPaneTestApp : App(BorderPaneTest::class)

class BorderPaneTest : SimpleView("Border Pane Builder Test", {
    borderpane {
        // Direct access
        top = label("Top")
        // Builder target
        center {
            hbox {
                label("Center")
            }
        }
    }
})