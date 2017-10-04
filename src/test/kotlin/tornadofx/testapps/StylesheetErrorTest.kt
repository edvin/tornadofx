package tornadofx.testapps

import javafx.scene.paint.Color
import tornadofx.App
import tornadofx.Stylesheet
import tornadofx.View
import tornadofx.addClass
import tornadofx.button
import tornadofx.px
import tornadofx.stackpane

class StylesheetErrorTest : App(StylesheetErrorView::class, Styles::class)

class StylesheetErrorView : View() {
    override val root = stackpane {
        button("Click here").addClass("my-button")
    }
}

class Styles : Stylesheet() {
    init {
        root {
            baseColor = Color.DARKSLATEGRAY
            prefWidth = 300.px
            prefHeight = 120.px
        }
        text {
            // cannot refer to unset property
            stroke = baseColor
        }
    }
}
