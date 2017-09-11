package tornadofx.testapps

import tornadofx.*
import java.time.LocalDateTime

class ParamTestApp : App(ParamCallerView::class)

class ParamCallerView : View() {
    override val root = button("Open modal with param") {
        action {
            find<ParamReceiverView>(mapOf("name" to "Param ${LocalDateTime.now()}")).openModal()
        }
    }
}

class ParamReceiverView : View() {
    val name: String by param()

    override val root = vbox()

    override fun onDock() {
        with(root) {
            label(name)
        }
        currentStage?.sizeToScene()
    }
}