package tornadofx.testapps

import javafx.beans.property.SimpleStringProperty
import javafx.util.Callback
import makeAutocompletable
import tornadofx.*

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

class AutoCompleteComboBoxExtensionTestApp : App(AutoCompleteComboBoxExtensionTest::class)

class AutoCompleteComboBoxExtensionTest : View("AutoComplete comboBox extension test") {
    val itemsGlobal = listOf("Item 1", "Item 2", "Item 3")
    val selectedItem = SimpleStringProperty(itemsGlobal.first())

    override val root = form {
        setPrefSize(400.0, 400.0)
        fieldset {
            field("Default"){
                combobox(selectedItem, itemsGlobal) {
                    makeAutocompletable()
                }
            }
            field("Without custom Filter"){
                combobox(selectedItem, itemsGlobal) {
                    makeAutocompletable().autoCompleteFilter = Callback { if(it.isBlank()) itemsGlobal.observable() else listOf("Item 1", "Item 3").observable() }
                }
            }
        }
    }
}