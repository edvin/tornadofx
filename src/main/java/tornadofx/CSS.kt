package tornadofx

import javafx.beans.value.ObservableValue
import javafx.scene.Node

abstract class Stylesheet {
    abstract fun render(): String
}

class ObservableStyleClass(node: Node, value: ObservableValue<String>) {
    init {
        value.addListener { observableValue, oldValue, newValue ->
            if (oldValue != null) node.removeClass(oldValue)
            if (newValue != null && !node.hasClass(newValue)) node.addClass(newValue)
        }
    }
}

fun Node.bindClass(value: ObservableValue<String>): ObservableStyleClass = ObservableStyleClass(this, value)