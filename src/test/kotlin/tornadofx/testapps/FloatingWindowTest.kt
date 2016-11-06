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
            setPrefSize(400.0, 400.0)
            setOnAction {
                openFloatingWindow()
            }
        }
    }

    private fun openFloatingWindow() {
        val containerView = find(ExpandableTableTest::class)
        FloatingWindow(containerView).openOver(root.lookup(".button"))
    }
}