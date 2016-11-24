package tornadofx.tests

import tornadofx.*

class SimpleItem(var value: String)

class SimpleListViewTestApp : App(SimpleListViewTest::class)

class SimpleListViewTest : View("Simple ListCell Cache Test") {
    val items = listOf(SimpleItem("One"), SimpleItem("Two"), SimpleItem("Three"))

    override val root = listview(items.observable()) {
        cellFormat { text = it.value }
    }
}