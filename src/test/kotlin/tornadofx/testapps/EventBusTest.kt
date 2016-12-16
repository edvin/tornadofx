package tornadofx.testapps

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import tornadofx.*

object DataLoaded : FXEvent()

class EventBusTestApp : App(EventBusTestView::class)

class EventBusTestView : View("Data Event Table") {
    val ctrl: MyController by inject()

    override val root = vbox {
        tableview(ctrl.myList) {
            column("Value", String::class) {
                value { it.value }
            }
            subscribe(DataLoaded) {
                selectionModel.select(0)
                requestFocus()
            }
        }
        button("Load data") {
            setOnAction {
                runAsync {
                    ctrl.loadData()
                }
            }
        }
    }
}

class MyController : Controller() {
    val myList: ObservableList<String> = FXCollections.observableArrayList<String>()

    fun loadData() {
        myList.setAll("Simulate", "Data", "Loaded", "From", "The", "Database")
        fire(DataLoaded)
    }
}