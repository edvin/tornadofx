package tornadofx.tests

import tornadofx.*

class Item(val value: String)

class ItemFragment : Fragment() {
    override val root = hbox { }
    fun load(item: Item) {
        root.label(item.value)
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