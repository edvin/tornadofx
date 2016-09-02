package tornadofx.testapps

import javafx.collections.ObservableList
import javafx.scene.control.Label
import tornadofx.*
import tornadofx.ResizeType.*
import java.time.LocalDate
import java.util.*

class ExpandableTableTestApp : App(ExpandableTableTest::class)

class ExpandableTableTest : View("Expandable Table") {
    // Makes sure equals/hashCode always returns the same value, or we can't track expanded state accurately
    class Room(val id: Int, val number: String, val type: String, val bed: String, val occupancy: ObservableList<Occupancy>)

    class Occupancy(val id: Int, val date: LocalDate, val customer: Int)

    val rooms = listOf(
            Room(1, "104", "Bedroom", "Queen", makeOccupancy(5)),
            Room(2, "105", "Bedroom", "King", makeOccupancy(5)),
            Room(3, "106", "Bedroom", "King", makeOccupancy(5)),
            Room(4, "107", "Suite", "Queen", makeOccupancy(5)),
            Room(4, "108", "Bedroom", "King", makeOccupancy(5)),
            Room(4, "109", "Conference Room", "Queen", makeOccupancy(5)),
            Room(4, "110", "Bedroom", "Queen", makeOccupancy(5)),
            Room(4, "111", "Playroom", "King", makeOccupancy(5)),
            Room(4, "112", "Bedroom", "Queen", makeOccupancy(5)),
            Room(4, "113", "Suite", "King", makeOccupancy(5))
    ).observable()

    override val root = tableview(rooms) {
        prefWidth = 800.0

        column("#", Room::id) resize Default()
        column("Number", Room::number) resize Pref(200.0)
        column("Type", Room::type) resize Pct(90.0)
        column("Bed", Room::bed) resize Remaining()

        columnResizePolicy = SmartColumnResize.POLICY

        rowExpander {
            tableview(it.occupancy) {
                column("Occupancy", Occupancy::id)
                column("Date", Occupancy::date)
                column("Customer", Occupancy::customer)
                prefHeight = 100.0
            }
        }
    }

    private fun makeOccupancy(count: Int) = (0..count).map {
        Occupancy(Random().nextInt(100), LocalDate.now().minusDays(it.toLong()), Random().nextInt(100000))
    }.observable()

}