package tornadofx.testapps

import tornadofx.*

class EventBusTestApp : App(EventBusTestView::class)

class MyDataEvent(val data: List<String>) : FXEvent()

class EventBusTestView : View("Data Event Table") {
    val ctrl: MyController by inject()

    override val root = vbox {
        tableview<String> {
            column("Value", String::class) {
                value { it.value }
            }
            subscribe<MyDataEvent> {
                items.setAll(it.data)
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
    fun loadData() {
        fire(MyDataEvent(listOf("Simulate", "Data", "Loaded", "From", "The", "Database")))
    }
}