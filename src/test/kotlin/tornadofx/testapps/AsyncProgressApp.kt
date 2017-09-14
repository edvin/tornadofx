package tornadofx.testapps

import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*

class AsyncProgressApp : App(AsyncProgressView::class)

class AsyncProgressView : SimpleView("Async Progress", {
    borderpane {
        setPrefSize(400.0, 300.0)

        center {
            button("Start") {
                action {
                    runAsync {
                        updateTitle("Doing some work")
                        for (i in 1..10) {
                            updateMessage("Working $i...")
                            if (i == 5)
                                updateTitle("Dome something else")
                            Thread.sleep(200)
                            updateProgress(i.toLong(), 10)
                        }
                    }
                }
            }
        }
        bottom {
            add<ProgressView>()
        }
    }
})

class ProgressView : View() {
    val status: TaskStatus by inject()

    override val root = vbox(4) {
        visibleWhen { status.running }
        style { borderColor += box(Color.LIGHTGREY, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT) }
        label(status.title).style { fontWeight = FontWeight.BOLD }
        hbox(4) {
            label(status.message)
            progressbar(status.progress)
            visibleWhen { status.running }
        }
    }
}

class AsyncProgressButtonView : SimpleView({
    stackpane {
        setPrefSize(400.0, 400.0)
        button("Click me") {
            action {
                runAsyncWithProgress {
                    Thread.sleep(2000)
                }
            }
        }
    }
})