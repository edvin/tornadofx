package tornadofx.testapps

import tornadofx.*
import tornadofx.EventBus.RunOn.BackgroundThread

class EventBusTestApp : App(EventBusTestView::class) {
    init {
        find<MyController>()
    }
}

class MyDataEvent(val data: List<String>) : FXEvent()
object GiveMeData : FXEvent(BackgroundThread)

class EventBusTestView : View("Data Event Table") {

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
        button("Load filtredItems") {
            setOnAction {
                fire(GiveMeData)
            }
        }
    }
}

class MyController : Controller() {
    init {
        subscribe<GiveMeData> {
            fire(MyDataEvent(listOf("Simulate", "Data", "Loaded", "From", "The", "Database")))
        }
    }
}