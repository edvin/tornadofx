package tornadofx.testapps

import tornadofx.*

class FloatingWindowTestApp : App(FloatingWindowTest::class)

class FloatingWindowTest : View("Floating Window") {
    override val root = form {
        setPrefSize(800.0, 600.0)

        fieldset("Information") {
            field("First field") {
                textfield()
            }
            field("Second field") {
                textfield()
            }
        }
        button("Open floating window") {
            setOnAction {
                openFloatingWindow()
            }
        }
    }

    private fun openFloatingWindow() {
        openInternalWindow(ExpandableTableTest::class)
    }
}