package tornadofx.testapps

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import tornadofx.*
import java.util.*

class TableViewDirtyTestApp : App(TableViewDirtyTest::class)

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
                    menuitem("Rollback").properties["name"] = "rollback"
                    menuitem("Commit").properties["name"] = "commit"
                }
                setOnContextMenuRequested { event ->
                    val dirtyState = editModel.getDirtyState(selectedItem)
                    val selectedCell = selectionModel.selectedCells.firstOrNull()
                    val rollbackItem = contextMenu.items.find { it.properties["name"] == "rollback" }!!
                    val commitItem = contextMenu.items.find { it.properties["name"] == "commit" }!!

                    if (selectedCell != null && dirtyState.isDirtyColumn(selectedCell.tableColumn)) {
                        with (contextMenu) {
                            with (rollbackItem) {
                                isDisable = false
                                text = "Rollback ${selectedCell.tableColumn.text}"
                                setOnAction {
                                    dirtyState.rollback(selectedCell.tableColumn)
                                }
                            }
                            with (commitItem) {
                                isDisable = false
                                text = "Commit ${selectedCell.tableColumn.text}"
                                setOnAction {
                                    dirtyState.commit(selectedCell.tableColumn)
                                }
                            }
                        }
                    } else {
                        with (rollbackItem) {
                            isDisable = true
                            text = "Rollback column"
                        }
                        with (commitItem) {
                            isDisable = true
                            text = "Commit column"
                        }
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
                    setOnAction {
                        table.editModel.commitSelected()
                    }
                }
                button("Rollback row") {
                    disableWhen { table.editModel.selectedItemDirty.not() }
                    setOnAction {
                        table.editModel.rollbackSelected()
                    }
                }
                button("Rollback all") {
                    setOnAction {
                        table.editModel.rollback()
                    }
                }
                button("Commit all") {
                    setOnAction {
                        table.editModel.rollback()
                    }
                }
            }
        }
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
