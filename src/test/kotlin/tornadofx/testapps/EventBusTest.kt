package tornadofx.testapps

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tornadofx.EventBus.RunOn.BackgroundThread
import java.time.LocalDateTime
import kotlin.concurrent.timer

class EventBusTestApp : App(EventBusTestView::class) {
    init {
        find<MyController>()
    }
}

class MyDataEvent(val data: List<String>) : FXEvent()
object GiveMeData : FXEvent(BackgroundThread)

data class MyEvent(val text: String) : FXEvent()

class EventBusTestView : View("Data Event Table") {
    val labelText = SimpleStringProperty("")
    val regProperty = SimpleObjectProperty<EventRegistration>()
    var reg by regProperty


    override val root = borderpane {
        top {
            hbox(10) {
                button("Load data").action {
                    fire(GiveMeData)
                }
                button("Subscribe label") {
                    setOnAction {
                        reg = subscribe<MyEvent> {
                            labelText.value = it.text
                        }
                    }
                    enableWhen { regProperty.isNull }
                }
                button("Unsubscribe label") {
                    setOnAction {
                        reg!!.unsubscribe()
                        reg = null
                    }
                    enableWhen { regProperty.isNotNull }
                }
            }
        }
        center {
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
        }
        bottom {
            label(labelText)
        }
    }

    init {
        timer(daemon = true, period = 1000) {
            fire(MyEvent(LocalDateTime.now().toString()))
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