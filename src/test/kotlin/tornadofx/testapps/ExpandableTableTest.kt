package tornadofx.testapps

import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.scene.control.TableView
import javafx.util.Callback
import javafx.util.StringConverter
import tornadofx.*
import java.time.LocalDate
import java.util.*

class ExpandableTableTestApp : App(ExpandableTableTest::class)

class ExpandableTableTest : View("Expandable Table") {
    val policies = mapOf(
            "CONSTRAINED_RESIZE_POLICY" to TableView.CONSTRAINED_RESIZE_POLICY,
            "UNCONSTRAINED_RESIZE_POLICY" to TableView.UNCONSTRAINED_RESIZE_POLICY,
            "SMART_RESIZE_POLICY" to SmartResize.POLICY
    )

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

    private var table: TableView<Room> by singleAssign()

    private val model = object : ViewModel() {
        val resizePolicy = bind { SimpleStringProperty("UNCONSTRAINED_RESIZE_POLICY") }
    }

    override val root = hbox {
        prefWidth = 1000.0

        table = tableview(rooms) {
            prefWidth = 9999.0

            column("#", Room::id)

            column("Number", Room::number) {
                remainingWidth()
                minWidth(75.0)
            }
            column("Fixed") {
                value { "FIXED 75" }
                fixedWidth(75.0)
            }
            column("Type", Room::type) {
                remainingWidth()
                minWidth(150.0)
            }
            column("Bed", Room::bed) {
                contentWidth()
                minWidth(90.0)
            }

            columnResizePolicy = SmartResize.POLICY

            rowExpander {
                tableview(it.occupancy) {
                    column("Occupancy", Occupancy::id)
                    column("Date", Occupancy::date)
                    column("Customer", Occupancy::customer)
                    prefHeight = 100.0
                }
            }
        }

        form {
            minWidth = 400.0
            maxWidth = 400.0

            fieldset("Table Resize Options") {

                field("Resize Policy") {
                    combobox(model.resizePolicy, policies.keys.toList().observable())
                }
            }
        }
    }

    init {
        model.resizePolicy.onChange {
            table.columnResizePolicy = when (it) {
                "CONSTRAINED_RESIZE_POLICY" -> TableView.CONSTRAINED_RESIZE_POLICY
                "UNCONSTRAINED_RESIZE_POLICY" -> TableView.UNCONSTRAINED_RESIZE_POLICY
                "SMART_RESIZE_POLICY" -> SmartResize.POLICY
                else -> throw Error("Unknown policy")
            }
        }
    }
}

private fun makeOccupancy(count: Int) = (0..count).map {
    ExpandableTableTest.Occupancy(Random().nextInt(100), LocalDate.now().minusDays(it.toLong()), Random().nextInt(100000))
}.observable()