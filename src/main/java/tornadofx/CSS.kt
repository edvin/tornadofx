package tornadofx

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.scene.Node

abstract class Stylesheet {
    abstract fun render(): String
}

class ObservableStyleClass(node: Node, val value: ObservableValue<String>) {
    val listener: ChangeListener<String>

    init {
        fun checkAdd(newValue: String?) {
            if (newValue != null && !node.hasClass(newValue)) node.addClass(newValue)
        }

        listener = ChangeListener { observableValue, oldValue, newValue ->
            if (oldValue != null) node.removeClass(oldValue)
            checkAdd(newValue)
        }

        checkAdd(value.value)
        value.addListener(listener)
    }

    fun dispose() = value.removeListener(listener)
}

fun Node.bindClass(value: ObservableValue<String>): ObservableStyleClass = ObservableStyleClass(this, value)