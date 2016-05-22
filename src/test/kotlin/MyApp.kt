
import javafx.scene.control.TreeItem
import javafx.scene.layout.VBox
import tornadofx.SingleViewApp
import tornadofx.cellFormat
import tornadofx.populate
import tornadofx.treeview

class MyApp: SingleViewApp() {
    override val root = VBox()

    data class Person(val name: String, val department: String)

    val persons = listOf(
            Person("Mary Hanes","Marketing"),
            Person("Steve Folley","Customer Service"),
            Person("John Ramsy","IT Help Desk"),
            Person("Erlick Foyes","Customer Service"),
            Person("Erin James","Marketing"),
            Person("Jacob Mays","IT Help Desk"),
            Person("Larry Cable","Customer Service")
    )

    init {
        with(root) {

            // Create Person objects for the departments with the department name as Person.name
            val departments = persons.map { it.department }.distinct().map { Person(it, "") }

            treeview<Person> {
                // Create root item
                root = TreeItem(Person("Departments", ""))

                // Make sure the text in each TreeItem is the name of the Person
                cellFormat { text = it.name }

                // Generate items. Children of the root item will contain departments
                populate { parent ->
                    if (parent == root) departments else persons.filter { it.department == parent.value.name }
                }
            }
        }
    }
}