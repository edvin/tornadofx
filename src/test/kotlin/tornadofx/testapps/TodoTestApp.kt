package tornadofx.testapps

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.scene.control.RadioButton
import javafx.scene.control.ToggleGroup
import tornadofx.*

class TodoItem(text: String? = null) {
    val textProperty = SimpleStringProperty(text)
    var text by textProperty

    val completedProperty = SimpleBooleanProperty(false)
    var completed by completedProperty

    override fun toString() = "[${if (completed) "X" else " "}] $text"
}

enum class TodoFilter { All, Completed, Active }

class TodoList : View("Todo List") {
    val todos = FXCollections.observableArrayList(TodoItem("Item 1"), TodoItem("item 2"))
    val todoFilter = SimpleObjectProperty(TodoFilter.All)

    override val root = borderpane {
        center {
            tableview(todos) {
                setPrefSize(300.0, 200.0)
                column("Completed", TodoItem::completedProperty).useCheckbox()
                column("Text", TodoItem::textProperty).makeEditable()
            }
        }
        bottom {
            hbox {
                togglegroup {
                    TodoFilter.values().forEach { radiobutton(value = it) }
                    bind(todoFilter)
                }
            }
            todoFilter.onChange {
                println("New value: $it")
            }
        }
    }

    private fun ToggleGroup.selectValue(newValue: TodoFilter?) {
        toggles.map { it as RadioButton }
                .find { it.text == newValue?.name }
                ?.isSelected = true
    }

    override fun onDock() {
        with(workspace) {
            button("Print todos").action {
                println(todos.joinToString("\n"))
            }
        }
    }
}

class TodoTestApp : WorkspaceApp(TodoList::class)

