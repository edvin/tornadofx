package tornadofx.tests

import javafx.scene.control.Label
import tornadofx.*

class Item(var value: String)

class ItemFragment : Fragment() {
    override val root = hbox { }
    var label: Label by singleAssign()
    lateinit var item: Item

    fun load(item: Item) {
        this.item = item
        label = root.label(item.value)
    }
}

class ListViewTestApp : App(ListViewTest::class)

class ListViewTest : View("ListCell Cache Test") {
    val items = listOf(Item("One"), Item("Two"))

    override val root = listview(items.observable()) {
        cellCache {
            val fragment = find(ItemFragment::class)
            fragment.load(it)
            fragment.root
        }
    }
}