package tornadofx.testapps

import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.geometry.Orientation.VERTICAL
import javafx.geometry.Pos
import javafx.scene.control.TableView
import javafx.scene.text.FontWeight
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
            column("Number", Room::number)
            column("Static") {
                value { "Static text" }
            }
            column("Type", Room::type)
            column("Bed", Room::bed)

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

            fieldset("Table Resize Options", labelPosition = VERTICAL) {
                field("Resize Policy") {
                    combobox(model.resizePolicy, policies.keys.toList().observable())
                }

                table.columns.forEach { column ->
                    field(if (column.text.isEmpty()) "Expander" else column.text) {
                        val resizeTypeProperty = column.resizeTypeProperty()
                        // Reset to default on change
                        resizeTypeProperty.onChange {
                            column.minWidth = 10.0
                            column.maxWidth = 5000.0
                        }
                        val types = listOf(ResizeType.Default(), ResizeType.Content(), ResizeType.Fixed(75.0), ResizeType.Pct(0.0), ResizeType.Pref(75.0), ResizeType.Weight(1.0), ResizeType.Remaining())
                                .filterNot { rt -> resizeTypeProperty.value.javaClass.isAssignableFrom(rt.javaClass) }
                                .observable()
                        types.add(resizeTypeProperty.value)
                        types.sortBy { it.javaClass.toString() }

                        combobox(resizeTypeProperty, types) {
                            converter = ResizeTypeStringConverter()
                        }
                        hbox(10.0) {
                            alignment = Pos.BASELINE_CENTER
                            dynamicContent(resizeTypeProperty) { rt ->
                                when (rt) {
                                    is ResizeType.Fixed -> {
                                        label("Width") { style { fontWeight = FontWeight.BOLD } }
                                        textfield(rt.width.toString()) {
                                            textProperty().onChange {
                                                resizeTypeProperty.value = ResizeType.Fixed(it!!.toDouble())
                                                column.minWidth = rt.width
                                                column.maxWidth = rt.width
                                                SmartResize.POLICY.call(TableView.ResizeFeatures(table, null, 0.0))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
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

class ResizeTypeStringConverter : StringConverter<ResizeType>() {
    override fun toString(rt: ResizeType?): String {
        return rt.toString().substringAfter("$").substringBefore("@")
    }

    override fun fromString(string: String?): ResizeType {
        throw UnsupportedOperationException("not implemented")
    }

}

private fun makeOccupancy(count: Int) = (0..count).map {
    ExpandableTableTest.Occupancy(Random().nextInt(100), LocalDate.now().minusDays(it.toLong()), Random().nextInt(100000))
}.observable()