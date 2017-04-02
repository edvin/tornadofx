package tornadofx.testapps

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import tornadofx.*

class TodoItem(text: String? = null) {
    val textProperty = SimpleStringProperty(text)
    var text by textProperty

    val completedProperty = SimpleBooleanProperty(false)
    var completed by completedProperty

    override fun toString() = "[${if (completed) "X" else " "}] $text"
}

class TodoList : View("Todo List") {
    val todos = FXCollections.observableArrayList(TodoItem("Item 1"), TodoItem("item 2"))

    override val root = tableview(todos) {
        setPrefSize(300.0, 200.0)
        column("Completed", TodoItem::completedProperty).useCheckbox()
        column("Text", TodoItem::textProperty).makeEditable()
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

