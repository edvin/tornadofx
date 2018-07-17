package tornadofx.testapps

import tornadofx.*
import java.time.LocalDate


class TableViewSortFilterTestApp : App(TableViewSortFilterTest::class)

class TableViewSortFilterTest : View("Table Sort and Filter") {
    data class Person(val id: Int, var name: String, var birthday: LocalDate) {
        val age: Int
            get() = birthday.until(LocalDate.now()).years
    }

    private val persons = listOf(
            Person(1, "Samantha Stuart", LocalDate.of(1981, 12, 4)),
            Person(2, "Tom Marks", LocalDate.of(2001, 1, 23)),
            Person(3, "Stuart Gills", LocalDate.of(1989, 5, 23)),
            Person(3, "Nicole Williams", LocalDate.of(1998, 8, 11))
    )

    val data = SortedFilteredList<Person>()

    override val root = vbox {
        textfield {
            data.filterWhen(textProperty(), { query, item -> item.name.contains(query, true) })
        }

        tableview(data) {
            readonlyColumn("ID", Person::id)
            column("Name", Person::name)
            nestedColumn("DOB") {
                readonlyColumn("Birthday", Person::birthday)
                readonlyColumn("Age", Person::age).contentWidth()
            }
            smartResize()
        }

        hbox(5) {
            label("Filter count:")
            label(data.sizeProperty.stringBinding { "$it records matching filter"})
        }
    }

    init {
        // Simulate data access
        data.asyncItems { persons }
    }
}
