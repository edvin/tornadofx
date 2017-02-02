package tornadofx.testapps

import javafx.beans.property.SimpleStringProperty
import tornadofx.App
import tornadofx.View
import tornadofx.combobox
import tornadofx.stackpane

class ComboBoxTestApp : App(ComboboxTest::class)

class ComboboxTest : View("Combobox test") {
    val items = listOf("Item 1", "Item 2", "Item 3")
    val selectedItem = SimpleStringProperty(items.first())

    override val root = stackpane {
        setPrefSize(400.0, 400.0)
        combobox(selectedItem, items) {
            cellFormat {
                text = it
            }
        }
    }
}