package tornadofx.tests

import javafx.beans.property.SimpleStringProperty
import tornadofx.*

class ListViewTestApp : App(ListViewTest::class)

class Item(value: String) {
    val valueProperty = SimpleStringProperty(value)
}

class MyItemViewModel : ItemViewModel<Item>() {
    val value = bind { item?.valueProperty }
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

class ListViewTest : View("ListCell Cache Test") {
    val items = listOf(Item("One"), Item("Two"))

    override val root = listview(items.observable()) {
        cellFragment(MyItemFragment::class)
    }
}