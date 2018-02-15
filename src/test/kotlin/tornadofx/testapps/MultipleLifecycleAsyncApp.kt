package tornadofx.testapps

import javafx.beans.property.SimpleIntegerProperty
import tornadofx.*

class MultipleLifecycleAsyncView : View("Multiple Lifecycle Async") {
    val counterProperty = SimpleIntegerProperty()
    var counter by counterProperty
    override val root = pane {
        button("Increment on background thread and main thread") {
            action {
                runAsync {
                    counter++
                } success {
                    counter++
                }
            }
        }
    }
}