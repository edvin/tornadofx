package tornadofx

import javafx.collections.FXCollections

fun <T> List<T>.observable() = FXCollections.observableList(this)
