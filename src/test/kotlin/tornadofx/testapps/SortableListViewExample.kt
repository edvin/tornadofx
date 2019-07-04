package tornadofx.testapps

import javafx.beans.property.SimpleObjectProperty
import tornadofx.*
import java.util.*
import kotlin.math.roundToInt


class SortableListViewExample : View("Sortable List View") {
    private val comparatorProperty = SimpleObjectProperty<Comparator<Int>>(compareBy { it })
    private val numbers = SortedFilteredList<Int>(comparatorProperty = comparatorProperty)

    override val root = vbox {

        button("Add number").action {
            numbers.add((Math.random() * 100).roundToInt())
        }
        button("Reverse Order").action {
            comparatorProperty.set(comparatorProperty.get().reversed())
        }
        button("Show numbers that are  greater than  50").action {
            numbers.predicate = { it > 50 }
        }
        button("Show all numbers").action {
            numbers.predicate = { true }
        }

        scrollpane {
            listview(numbers) {
            }
        }
    }
}
