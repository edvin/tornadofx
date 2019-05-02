package tornadofx.testapps

import javafx.beans.property.SimpleIntegerProperty
import tornadofx.*
import java.util.concurrent.atomic.AtomicInteger

class PrimaryViewLeakApp : App(PrimaryViewLeakView::class)

class PrimaryViewLeakView : View() {

    companion object {
        val instanceCounter = AtomicInteger()
    }
    val instanceId = instanceCounter.getAndIncrement()
    val dockCounterProperty = SimpleIntegerProperty()
    var dockCounter by dockCounterProperty

    init {
        println("PrimaryLeakView.init()")
    }

    override val root = form {
        fieldset("Primary View Leak Test") {
            field("Instance ID") {
                label(instanceId.toString())
            }
            field("Dock Counter") {
                label(dockCounterProperty) {
                    style {
                        fontSize = 48.pt
                    }
                }
            }
        }
    }

    override fun onDock() {
        super.onDock()
        dockCounter++
        println("PrimaryLeakView.dockCounter: $dockCounter")
    }
}