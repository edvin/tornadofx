package tornadofx.testapps

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.scene.control.TableView
import tornadofx.*
import java.util.*

class TableViewDirtyTestApp : WorkspaceApp(TableViewDirtyTest::class)

class TableViewDirtyTest : View("TableView Dirty Test") {
    val customers = FXCollections.observableArrayList(Customer("Thomas", "Nield"), Customer("Edvin", "Syse"))
    var table: TableView<Customer> by singleAssign()

    override val root = borderpane {
        center {
            tableview(customers) {
                table = this
                prefHeight = 200.0
                column("First Name", Customer::firstNameProperty).makeEditable()
                column("Last Name", Customer::lastNameProperty).makeEditable()
                enableCellEditing()
                regainFocusAfterEdit()
                enableDirtyTracking()
                contextmenu {
                    item(stringBinding(selectionModel.selectedCells) { "Rollback ${selectedColumn?.text}" }) {
                        disableWhen { editModel.selectedItemDirty.not() }
                        setOnAction { editModel.rollback(selectedItem, selectedColumn) }
                    }
                    item(stringBinding(selectionModel.selectedCells) { "Commit ${selectedColumn?.text}" }) {
                        disableWhen { editModel.selectedItemDirty.not() }
                        setOnAction { editModel.commit(selectedItem, selectedColumn) }
                    }
                }
                columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
            }
        }
        bottom {
            hbox(10) {
                paddingAll = 4
                button("Commit row") {
                    disableWhen { table.editModel.selectedItemDirty.not() }
                    setOnAction { table.editModel.commitSelected() }
                }
                button("Rollback row") {
                    disableWhen { table.editModel.selectedItemDirty.not() }
                    setOnAction { table.editModel.rollbackSelected() }
                }
            }
        }


    }

    override fun onSave() {
        table.editModel.commit()
    }

    override fun onRefresh() {
        table.editModel.rollback()
    }

    class Customer(firstName: String, lastName: String) {
        val idProperty = SimpleObjectProperty<UUID>(UUID.randomUUID())
        var id by idProperty

        val firstNameProperty = SimpleStringProperty(firstName)
        var firstName by firstNameProperty

        val lastNameProperty = SimpleStringProperty(lastName)
        val lastName by lastNameProperty

        override fun toString() = "$firstName $lastName"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as Customer

            if (id != other.id) return false

            return true
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }
    }
}
