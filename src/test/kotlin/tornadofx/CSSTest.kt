package tornadofx

import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.stage.Stage

class CSSTest : SingleViewApp("CSS Test") {
    override val root = HBox(Label("Hello CSS"))

    init {
        with (root) {
            addClass("hbox")
            setMinSize(400.0, 400.0)
        }

    }

    override fun start(stage: Stage) {
//        importStylesheet(MyTestStylesheet::class)
        stage.reloadStylesheetsOnFocus()
        super.start(stage)
    }

    class MyTestStylesheet : Stylesheet() {
        override fun render() = """

.root {
    -fx-background: red;
    -fx-font-size: 30px;
    -fx-font-weight: bold;
}

.hbox {
    -fx-padding: 30px;
}


        """
    }
}
