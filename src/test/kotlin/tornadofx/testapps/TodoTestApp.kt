package tornadofx.testapps

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.control.CheckBox
import javafx.scene.control.RadioButton
import javafx.scene.control.TableColumn
import javafx.scene.control.ToggleGroup
import tornadofx.*

class TodoItem(text: String? = null, completed: Boolean = true) {
    val textProperty = SimpleStringProperty(text)
    var text by textProperty

    val completedProperty = SimpleBooleanProperty(completed)
    var completed by completedProperty

    override fun toString() = "[${if (completed) "X" else " "}] $text"
}

enum class TodoFilter { All, Completed, Active }

class TodoList : View("Todo List") {
    val todos = SortedFilteredList(FXCollections.observableArrayList(TodoItem("Item 1"), TodoItem("Item 2"), TodoItem("Item 3", false), TodoItem("Item 4"), TodoItem("Item 5")))
    val todoFilter = SimpleObjectProperty(TodoFilter.All)

    override val root = borderpane {
        center {
            tableview(todos) {
                setPrefSize(300.0, 200.0)
                column("Completed", TodoItem::completedProperty) {
                    sortType = TableColumn.SortType.DESCENDING
                    sortOrder.add(this)
                    cellFormat {
                        graphic = cache {
                            alignment = Pos.CENTER
                            checkbox {
                                selectedProperty().bindBidirectional(itemProperty())

                                action {
                                    tableView.edit(index, tableColumn)
                                    commitEdit(!isSelected)
                                    sort()
                                }
                            }
                        }
                    }
                }
                column("Text", TodoItem::textProperty).makeEditable()

            }
        }
        bottom {
            hbox {
                togglegroup {
                    TodoFilter.values().forEach {
                        radiobutton(value = it)
                    }
                    bind(todoFilter)
                }
            }
            todoFilter.onChange {
                todos.refilter()
            }
        }
    }

    init {
        todos.predicate = {
            when (todoFilter.value) {
                TodoFilter.All -> true
                TodoFilter.Completed -> it.completed
                TodoFilter.Active -> !it.completed
            }
        }
    }
}

class TodoTestApp : WorkspaceApp(TodoList::class)

