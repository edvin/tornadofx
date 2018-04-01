package tornadofx.testapps

import javafx.scene.layout.BorderStrokeStyle
import javafx.scene.shape.StrokeLineCap
import javafx.scene.shape.StrokeLineJoin
import javafx.scene.shape.StrokeType
import tornadofx.*

class BrokenLineApp : App(BrokenLineView::class, BrokenStyles::class)
class BrokenLineView : View("Broken Line Test") {
    override val root = stackpane {
        line {
            addClass(BrokenStyles.blue)
            endXProperty().bind(this@stackpane.widthProperty())
            endYProperty().bind(this@stackpane.heightProperty())
        }
        line {
            addClass(BrokenStyles.blue)
            endXProperty().bind(this@stackpane.widthProperty())
            startYProperty().bind(this@stackpane.heightProperty())
        }
        label("This is a label with a border").addClass(BrokenStyles.red)
    }
}

class BrokenStyles : Stylesheet() {
    companion object {
        val red by cssclass()
        val blue by cssclass()
    }

    init {
        root {
            padding = box(25.px)
            backgroundColor += c("white")
        }
        red {
            val radius = box(50.px)
            backgroundColor += c("white", 0.9)
            backgroundRadius += radius
            borderColor += box(c("red"))
            borderWidth += box(5.px)
            borderRadius += radius
            borderStyle += BorderStrokeStyle(
                StrokeType.CENTERED,
                StrokeLineJoin.MITER,
                StrokeLineCap.SQUARE,
                10.0,
                0.0,
                listOf(5.0, 15.0, 0.0, 15.0)
            )
            padding = box(50.px)
        }
        blue {
            stroke = c("dodgerblue")
            strokeWidth = 5.px
            strokeType = StrokeType.CENTERED
            strokeLineCap = StrokeLineCap.SQUARE
            strokeDashArray = listOf(25.px, 15.px, 0.px, 15.px)
        }
    }
}