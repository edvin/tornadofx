package tornadofx.testapps

import javafx.collections.ObservableList
import javafx.scene.layout.Background.EMPTY
import tornadofx.*
import java.time.LocalDate
import java.util.*

class ExpandableTableTest : View("Smart Resize Demo") {
    class Room(val id: Int, val number: String, val type: String, val bed: String, val occupancy: ObservableList<Occupancy>)
    class Occupancy(val id: Int, val date: LocalDate, val customer: Int)

    val rooms = mutableListOf(
            Room(1, "104", "Bedroom", "Queen", makeOccupancy(5)),
            Room(2, "105", "Bedroom", "King", makeOccupancy(5)),
            Room(3, "106", "Bedroom", "King", makeOccupancy(5)),
            Room(4, "107", "Suite", "Queen", makeOccupancy(5)),
            Room(4, "108", "Bedroom", "King", makeOccupancy(5)),
            Room(4, "109", "Conference Room", "Queen", makeOccupancy(5)),
            Room(4, "110", "Bedroom", "Queen", makeOccupancy(5)),
            Room(4, "111", "Playroom", "King", makeOccupancy(5)),
            Room(4, "112", "Bedroom", "Queen", makeOccupancy(5))
    ).observable()

    override val root = tableview(rooms) {
        prefWidth = 600.0

        column("#", Room::id).contentWidth(10.0, true, true)
        column("Number", Room::number).weightedWidth(1.0)
        column("Bed", Room::bed).apply {
            weightedWidth(2.0)
            cellFormat {
                graphic = cache {
                    textfield(itemProperty()) {
                        isEditable = false
                        paddingAll = 0
                        background = EMPTY
                    }
                }
            }
        }
        column("Type", Room::type).weightedWidth(2.0)

        smartResize()

        rowExpander {
            tableview(it.occupancy) {
                column("Occupancy", Occupancy::id)
                column("Date", Occupancy::date)
                column("Customer", Occupancy::customer)
                prefHeight = 100.0
            }
        }
    }

    init {
        runAsync {
            Thread.sleep(5000)
        } ui {
            root.items.add(Room(4, "113", "Suite", "King size long description", makeOccupancy(5)))
            SmartResize.POLICY.requestResize(root)
        }
    }

}


private fun makeOccupancy(count: Int) = (0..count).map {
    ExpandableTableTest.Occupancy(Random().nextInt(100), LocalDate.now().minusDays(it.toLong()), Random().nextInt(100000))
}.observable()