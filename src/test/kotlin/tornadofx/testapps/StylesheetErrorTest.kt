package tornadofx.testapps

import tornadofx.App
import tornadofx.Stylesheet
import tornadofx.View
import tornadofx.addClass
import tornadofx.button
import tornadofx.stackpane

class StylesheetErrorTest : App(StylesheetErrorView::class, Styles::class)

class StylesheetErrorView : View() {
    override val root = stackpane {
        button("Click here").addClass("my-button")
    }
}

class Styles : Stylesheet() {
    init {
        text {
            // cannot refer to unset property
            stroke = baseColor
        }
    }
}
