package tornadofx.testapps

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.scene.control.SelectionMode
import javafx.scene.control.TableView
import javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY
import javafx.scene.text.FontWeight
import tornadofx.*
import java.util.*

class TableViewDirtyTestApp : WorkspaceApp(TableViewDirtyTest::class)

class TableViewDirtyTest : View("Dirty Tables") {
    val customers = FXCollections.observableArrayList(Customer("Thomas", "Nield"), Customer("Matthew", "Turnblom"), Customer("Edvin", "Syse"))
    var table: TableView<Customer> by singleAssign()

    override val root = borderpane {
        center {
            tableview(customers) {
                table = this
                prefHeight = 200.0

                column("First Name", Customer::firstNameProperty) {
                    makeEditable()
                    cellDecorator {
                        style {
                            fontWeight = FontWeight.BOLD
                        }
                    }
                }
                column("Last Name", Customer::lastNameProperty).makeEditable()
                enableCellEditing()
                regainFocusAfterEdit()
                enableDirtyTracking()
                selectOnDrag()
                contextmenu {
                    item(stringBinding(selectionModel.selectedCells) { "Rollback ${selectedColumn?.text}" }) {
                        disableWhen { editModel.selectedItemDirty.not() }
                        action { editModel.rollback(selectedItem, selectedColumn) }
                    }
                    item(stringBinding(selectionModel.selectedCells) { "Commit ${selectedColumn?.text}" }) {
                        disableWhen { editModel.selectedItemDirty.not() }
                        action { editModel.commit(selectedItem, selectedColumn) }
                    }
                }
                selectionModel.selectionMode = SelectionMode.MULTIPLE
                columnResizePolicy = CONSTRAINED_RESIZE_POLICY
            }
        }
        bottom {
            hbox(10) {
                paddingAll = 4
                button("Commit row") {
                    disableWhen { table.editModel.selectedItemDirty.not() }
                    action { table.editModel.commitSelected() }
                }
                button("Rollback row") {
                    disableWhen { table.editModel.selectedItemDirty.not() }
                    action { table.editModel.rollbackSelected() }
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

    init {
        icon = FX.icon
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
