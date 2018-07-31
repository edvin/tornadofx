package tornadofx.testapps

import javafx.scene.text.FontWeight
import javafx.stage.Stage
import tornadofx.*

class ExternalStylesheetTestApp : App(ExternalStylesheetTestView::class) {
    override fun start(stage: Stage) {
        importStylesheet("https://raw.githubusercontent.com/edvin/tornadofx/master/src/test/resources/teststyles.css")
        super.start(stage)
    }
}

class ExternalStylesheetTestView: View("External Stylesheet") {
    override val root = label(title).addClass("testclass")
}