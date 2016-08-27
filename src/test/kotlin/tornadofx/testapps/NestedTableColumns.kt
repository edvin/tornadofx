package tornadofx.testapps

import javafx.collections.FXCollections
import tornadofx.*

class NestedTableColumnsApp : App(NestedTableColumns::class)

class NestedTableColumns : View("Nested Table Columns") {
    data class Person(val name: String, val primaryEmail: String, val secondaryEmail: String)

    val people = FXCollections.observableArrayList(
            Person("John Doe", "john@doe.com", "john.doe@gmail.com"),
            Person("Jane Doe", "jane@doe.com", "jane.doe@gmail.com")
    )

    override val root = tableview(people) {
        column("Name", Person::name)
        nestedColumn("Email addresses") {
            column("Primary Email", Person::primaryEmail)
            column("Secondary Email", Person::secondaryEmail)
        }
    }
}