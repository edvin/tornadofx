package tornadofx.testapps

import javafx.collections.FXCollections
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import tornadofx.*
import java.time.LocalDate


class TableViewSortFilterTestApp : App(TableViewSortFilterTest::class)

class TableViewSortFilterTest : View("Table Sort and Filter") {

	data class Person(val id: Int, var name: String, var birthday: LocalDate) {
		val age: Int 
			get() = birthday.until(LocalDate.now()).years
	}

	private val persons = FXCollections.observableArrayList(
			Person(1, "Samantha Stuart", LocalDate.of(1981, 12, 4)),
			Person(2, "Tom Marks", LocalDate.of(2001, 1, 23)),
			Person(3, "Stuart Gills", LocalDate.of(1989, 5, 23)),
			Person(3, "Nicole Williams", LocalDate.of(1998, 8, 11))
	)

	var table: TableView<Person> by singleAssign()
	var textfield: TextField by singleAssign()

	override val root = vbox {
		textfield = textfield()

		table = tableview {
			column("ID", Person::id)
			column("Name", Person::name)
			column("Birthday", Person::birthday)
			column("Age", Person::age)
		}
	}

	init {
		SortedFilteredList(persons).bindTo(table)
				.filterWhen(textfield.textProperty(), { query, item -> item.name.contains(query, true) })
	}
}
