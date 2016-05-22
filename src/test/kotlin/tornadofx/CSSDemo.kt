package tornadofx

import javafx.scene.layout.BorderStrokeStyle
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.StrokeLineCap
import javafx.scene.shape.StrokeLineJoin
import javafx.scene.shape.StrokeType

class CssDemo : App() {
    override val primaryView = MyView::class

    init {
        importStylesheet(Styles::class)
    }
}
class MyView: View() {
    override val root = VBox()

    init {
        with(root) {
            addClass(Styles.wrapper)

            label("Alice") {
                addClass(Styles.alice)
            }
            label("Bob") {
                addClass(Styles.bob)
            }
        }
    }
}

class Styles : Stylesheet() {
    companion object {
        // Define our styles
        val wrapper by cssclass()
        val bob by cssclass()
        val alice by cssclass()

        // Define our colors
        val dangerColor = c("#a94442")
        val hoverColor = c("#d49942")
    }

    init {

        val flat = mixin {
            backgroundColor = Color.GREEN
        }
        s(wrapper) {
            padding = box(10.px)
            spacing = 10.px
        }

        s(label) {
            + flat
            fontSize = 56.px
            padding = box(5.px, 10.px)
            maxWidth = infinity

            +s(bob, alice) {
                + flat
                borderColor = box(dangerColor)
                borderStyle = BorderStrokeStyle(StrokeType.INSIDE, StrokeLineJoin.MITER, StrokeLineCap.BUTT, 10.0, 0.0, listOf(25.0, 5.0))
                borderWidth = box(5.px)

                +s(hover) {
                    backgroundColor = hoverColor
                }
            }
            +s(alice) {
                +s(hover) {
                    underline = true
                    borderColor = box(c("blue"))
                    maxWidth = 100.px
                }
            }
        }
    }
}