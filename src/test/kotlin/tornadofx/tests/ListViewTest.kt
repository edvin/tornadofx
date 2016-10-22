package tornadofx.tests

import javafx.beans.property.SimpleStringProperty
import tornadofx.*

class Item(value: String) {
    val valueProperty = SimpleStringProperty( value )
}

class MyItemViewModel : ItemViewModel<Item>() {
    val value = bind(autocommit = true) { item?.valueProperty }
}

class MyItemFragment : ListCellFragment<Item>() {
    val model = MyItemViewModel()
    override val root = hbox {
        label(model.value)
    }
    init {
        // When the item for this fragment is changed, rebind the model
        model.itemProperty.bind(itemProperty)
    }
}

class ListViewTestApp : App(ListViewTest::class)

class ListViewTest : View("ListCell Cache Test") {
    val items = listOf(Item("One"), Item("Two"))

    override val root = listview(items.observable()) {
        cellFragment(MyItemFragment::class)
    }
}