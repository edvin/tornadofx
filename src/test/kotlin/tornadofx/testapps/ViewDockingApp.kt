package tornadofx.testapps

import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.Scene
import javafx.stage.Stage
import tornadofx.*
import java.util.concurrent.atomic.AtomicInteger

class ViewDockingApp : App(PrimaryDockingView::class)

open class PrimaryDockingView : View() {

    companion object {
        val instanceCounter = AtomicInteger()
    }
    val instanceId = instanceCounter.getAndIncrement()
    val dockCounterProperty = SimpleIntegerProperty()
    var dockCounter by dockCounterProperty
    val undockCounterProperty = SimpleIntegerProperty()
    var undockCounter by undockCounterProperty

    init {
        println("$tag.init()")
    }

    open val tag: String
        get() = "PrimaryDockingView"

    override val root = vbox {
        form {
            fieldset {
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
                field("Undock Counter") {
                    label(undockCounterProperty) {
                        style {
                            fontSize = 48.pt
                        }
                    }
                }
            }
        }
    }

    override fun onDock() {
        super.onDock()
        dockCounter++
        println("$tag.dockCounter: $dockCounter")
    }

    override fun onUndock() {
        super.onUndock()
        undockCounter++
        println("$tag.undockCounter: $undockCounter")
    }
}

class SecondaryDockingView : PrimaryDockingView() {

    override val tag: String
        get() = "SecondaryDockingView"
}

class NoPrimaryViewDockingApp : App() {

    override fun start(stage: Stage) {
        super.start(stage)
        stage.scene = Scene(find<PrimaryDockingView>().root)
        stage.show()
    }
}