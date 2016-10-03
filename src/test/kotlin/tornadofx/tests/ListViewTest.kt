package tornadofx.tests

import javafx.scene.control.Label
import tornadofx.*

class Item(var value: String)

class MyItemFragment : ItemFragment<Item>() {
    override val root = hbox { }
    var label: Label by singleAssign()
    lateinit var item: Item

    override fun updateItem(item: Item) {
        this.item = item
        label = root.label(item.value)
    }
}

class ListViewTestApp : App(ListViewTest::class)

class ListViewTest : View("ListCell Cache Test") {
    val items = listOf(Item("One"), Item("Two"))

    override val root = listview(items.observable()) {
        cellFragment(MyItemFragment::class)
    }
}