package tornadofx.testapps

import javafx.scene.control.TreeItem
import tornadofx.*
import tornadofx.tests.JavaPerson

class PojoTreeTableColumnsApp : App(PojoTreeTableColumns::class)

class PojoTreeTableColumns : View("Pojo Tree Table Columns") {

    val people = observableListOf(
            JavaPerson("Mary Hanes", "IT Administration", "mary.hanes@contoso.com", "mary2.hanes@contoso.com", listOf(
                    JavaPerson("Jacob Mays", "IT Help Desk", "jacob.mays@contoso.com", "jacob2.mays@contoso.com"),
                    JavaPerson("John Ramsy", "IT Help Desk", "john.ramsy@contoso.com", "john2.ramsy@contoso.com"))),
            JavaPerson("Erin James", "Human Resources", "erin.james@contoso.com", "erin2.james@contoso.com", listOf(
                    JavaPerson("Erlick Foyes", "Customer Service", "erlick.foyes@contoso.com", "erlick2.foyes@contoso.com"),
                    JavaPerson("Steve Folley", "Customer Service", "steve.folley@contoso.com", "erlick2.foyes@contoso.com"),
                    JavaPerson("Larry Cable", "Customer Service", "larry.cable@contoso.com", "larry2.cable@contoso.com")))
    )

    // Create the root item that holds all top level employees
    val rootItem = TreeItem(JavaPerson().apply { name = "Employees by Manager"; employees = people })

    override val root = treetableview(rootItem) {
        prefWidth = 800.0

        column<JavaPerson, String>("Name", "name").contentWidth(50) // non type safe
        column("Department", JavaPerson::getDepartment).remainingWidth()
        nestedColumn("Email addresses") {
            column("Primary Email", JavaPerson::getPrimaryEmail)
            column("Secondary Email", JavaPerson::getSecondaryEmail)
        }

        // Always return employees under the current person
        populate { it.value.employees }

        // Expand the two first levels
        root.isExpanded = true
        root.children.withEach { isExpanded = true }
        smartResize()
    }
}