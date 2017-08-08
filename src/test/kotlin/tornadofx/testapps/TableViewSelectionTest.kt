package tornadofx.testapps

import javafx.scene.control.TabPane
import javafx.scene.control.TreeItem
import tornadofx.*
import tornadofx.tests.JavaPerson

class TableViewSelectionTestApp : App(TableViewSelectionTest::class)

class TableViewSelectionTest : View("Table Selection Test") {
    val people = listOf(
            JavaPerson("Mary Hanes", "IT Administration", "mary.hanes@contoso.com", "mary2.hanes@contoso.com"),
            JavaPerson("Erin James", "Human Resources", "erin.james@contoso.com", "erin2.james@contoso.com")
    ).observable()

    override val root = tabpane {
        prefWidth = 800.0
        tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

        tab("Table") {
            tableview(people) {
                column("Name", JavaPerson::getName).contentWidth(50.0)
                column("Department", JavaPerson::getDepartment).remainingWidth()
                nestedColumn("Email addresses") {
                    column("Primary Email", JavaPerson::getPrimaryEmail)
                    column("Secondary Email", JavaPerson::getSecondaryEmail)
                }

                selectionModel.isCellSelectionEnabled = true
                smartResize()

                contextmenu {
                    item("Print selected").action { println(selectedValue.toString()) }
                    item("Print selected position").action { println("(${selectedCell!!.column}, ${selectedCell!!.row})") }
                    item("Print selected column").action { println(selectedColumn!!.text) }
                    separator()
                    item("Print employee's name").action { println(selectedItem!!.name) }
                    item("Print employee's department").action { println(selectedItem!!.department) }
                    item("Print employee's primary email").action { println(selectedItem!!.primaryEmail) }
                    item("Print employee's secondary email").action { println(selectedItem!!.secondaryEmail) }
                }
            }
        }

        tab("Tree Table") {
            val rootItem = TreeItem(JavaPerson().apply { name = "Employees"; employees = people })

            treetableview(rootItem) {
                column("Name", JavaPerson::getName).contentWidth(50.0)
                column("Department", JavaPerson::getDepartment).remainingWidth()
                nestedColumn("Email addresses") {
                    column("Primary Email", JavaPerson::getPrimaryEmail)
                    column("Secondary Email", JavaPerson::getSecondaryEmail)
                }

                populate { it.value.employees }

                selectionModel.isCellSelectionEnabled = true
                root.isExpanded = true
                root.children.forEach { it.isExpanded = true }
                smartResize()

                contextmenu {
                    item("Print selected").action { println(selectedValue.toString()) }
                    item("Print selected position").action { println("(${selectedCell!!.column}, ${selectedCell!!.row})") }
                    item("Print selected column").action { println(selectedColumn!!.text) }
                    separator()
                    item("Print employee's name").action { println(selectedItem!!.name) }
                    item("Print employee's department").action { println(selectedItem!!.department) }
                    item("Print employee's primary email").action { println(selectedItem!!.primaryEmail) }
                    item("Print employee's secondary email").action { println(selectedItem!!.secondaryEmail) }
                }
            }
        }
    }
}