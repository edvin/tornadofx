package tornadofx.tests

import javafx.beans.property.SimpleStringProperty
import tornadofx.*

class Item(var value: String)

class MyItemFragment : ItemFragment<Item>() {
    val itemValue = SimpleStringProperty()
    override val root = hbox {
        label(itemValue)
    }

    override fun updateItem(item: Item) {
        itemValue.set(item.value)
    }
}

class ListViewTestApp : App(ListViewTest::class)

class ListViewTest : View("ListCell Cache Test") {
    val items = listOf(Item("One"), Item("Two"))

    override val root = listview(items.observable()) {
        cellFragment(MyItemFragment::class)
    }
}