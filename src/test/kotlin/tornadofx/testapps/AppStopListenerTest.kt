package tornadofx.testapps

import javafx.application.Application
import tornadofx.*

class AppStopListenerApp : App(AppStopListenerView::class)

class AppStopListenerView : View("My View") {

    private val controller by inject<AppStopListenerController>()

    override val root = stackpane {

        setPrefSize(800.0, 600.0)

        label("AppStopListenerView")

        controller.doSomething()
    }
}

class AppStopListenerController : Controller(), AppStopListener {

    fun doSomething() {
        println("do something...")
    }

    override fun onStop() {

        println("do something when the app is stop...")
    }
}

fun main(args: Array<String>) {
    Application.launch(AppStopListenerApp::class.java)
}