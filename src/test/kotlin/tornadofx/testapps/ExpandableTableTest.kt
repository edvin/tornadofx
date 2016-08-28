package tornadofx.testapps

import javafx.collections.ObservableList
import javafx.scene.control.Label
import javafx.scene.layout.Priority
import tornadofx.*
import java.time.LocalDate
import java.util.*

class AccordionViewApp : App(AccordionView::class)

class AccordionView : View() {
    override val root = vbox {
        accordion {
            titledpane("TitledPane1", Label("Label 1"))
            titledpane("TitledPane2", Label("Label 2"))
        }
    }
}
class ExpandableTableTestApp : App(ExpandableTableTest::class)

class ExpandableTableTest : View("Expandable Table") {
    // Makes sure equals/hashCode always returns the same value, or we can't track expanded state accurately
    class Room(val id: Int, val number: String, val type: String, val bed: String, val occupancy: ObservableList<Occupancy>)

    class Occupancy(val id: Int, val date: LocalDate, val customer: Int)

    val rooms = listOf(Room(1, "104", "Bedroom", "Queen", makeOccupancy(5)), Room(2, "105", "Bedroom", "King", makeOccupancy(5))).observable()

    override val root = tableview(rooms) {
        column("#", Room::id)
        column("Number", Room::number)
        column("Type", Room::type)
        column("Bed", Room::bed)
        rowExpander {
            tableview(it.occupancy) {
                column("Occupancy", Occupancy::id)
                column("Date", Occupancy::date)
                column("Customer", Occupancy::customer)
            }
        } toggleNode {
            Label(if (it) "Close" else "Open")
        }


    }

    private fun makeOccupancy(count: Int) = (0..count).map {
        Occupancy(Random().nextInt(100), LocalDate.now().minusDays(it.toLong()), Random().nextInt(100000))
    }.observable()

}