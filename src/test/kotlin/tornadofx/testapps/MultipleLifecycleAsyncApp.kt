package tornadofx.testapps

import tornadofx.*

class MultipleLifecycleAsyncApp : App(MainView::class)

class MainView : View("Multiple Lifecycle Async") {
    val controller: MainController by inject()
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

class MainController : Controller() {
    fun onAction(message: String) {
        println(message)
    }
}