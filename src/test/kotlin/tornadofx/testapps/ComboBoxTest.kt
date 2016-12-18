package tornadofx.testapps

import tornadofx.App
import tornadofx.View
import tornadofx.combobox
import tornadofx.stackpane

class ComboBoxTestApp : App(ComboboxTest::class)

class ComboboxTest : View("Combobox test") {
    override val root = stackpane {
        setPrefSize(400.0, 400.0)
        combobox(values = listOf("Number One", "Number two")) {
            cellFormat {
                text = "It be $it"
            }
        }
    }
}