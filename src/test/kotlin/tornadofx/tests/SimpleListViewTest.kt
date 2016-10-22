package tornadofx.tests

import javafx.beans.property.SimpleStringProperty
import tornadofx.*

class SimpleItem(var value: String)

class MySimpleItemFragment : ListCellFragment<SimpleItem>() {
    val itemValue = SimpleStringProperty()
    override val root = hbox {
        label(itemValue)
    }
}

class SimpleListViewTestApp : App(SimpleListViewTest::class)

class SimpleListViewTest : View("Simple ListCell Cache Test") {
    val items = listOf(SimpleItem("One"), SimpleItem("Two"))

    override val root = listview(items.observable()) {
        cellFragment(MySimpleItemFragment::class)
        cellFormat { text = it.value }
    }
}