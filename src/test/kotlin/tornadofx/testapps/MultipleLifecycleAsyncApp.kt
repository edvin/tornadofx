package tornadofx.testapps

import tornadofx.*

class MultipleLifecycleAsyncApp : App(MultipleLifecycleAsyncView::class)

class MultipleLifecycleAsyncView : View("Multiple Lifecycle Async") {
    val controller: MultipleLifecycleAsyncController by inject()
    override val root = pane {
        button("Robot-click to repeat bug") {
            id = "bug"
            action {
                runAsync {
                    controller.onAction("button clicked")
                }
            }
        }
    }
}

class MultipleLifecycleAsyncController : Controller() {
    fun onAction(message: String) {
        println(message)
    }
}