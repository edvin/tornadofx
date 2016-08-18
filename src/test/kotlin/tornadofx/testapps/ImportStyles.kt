package tornadofx.testapps

import javafx.geometry.Pos
import javafx.scene.effect.DropShadow
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*

/**
 * @author I58695 (2016-08-18)
 */
class ImportStyles : App(MainView::class, Styles::class) {
    init {
        dumpStylesheets()
    }

    class MainView : View() {
        override val root = vbox {
            addClass(box)
            label("Test").addClass(test)
            label("Test 2").addClass(test2)
        }
    }

    companion object {
        val box by cssclass()
        val test by cssclass()
        val test2 by cssclass()
        val color = Color.GRAY
        val textSize = 56.px
    }

    class Styles : Stylesheet(ParentStyles::class) {
        init {
            val text = mixin {
                fontSize = textSize
                fontWeight = FontWeight.BOLD
                maxWidth = infinity
                alignment = Pos.BASELINE_CENTER
            }
            test {
                +text
                textFill = color
                effect = DropShadow(3.0, 3.0, 3.0, Color.BLACK)

                add(hover) {
                    effect = DropShadow(5.0, 5.0, 5.0, Color.BLACK)
                }
            }

            test2 {
                +text
                textFill = c("yellow", 0.75)

                add(hover) {
                    textFill = Color.YELLOW
                    effect = DropShadow(20.0, 0.0, 0.0, Color.YELLOW)
                }
            }
        }
    }

    class ParentStyles : Stylesheet() {
        init {
            box {
                backgroundColor += color
                padding = box(textSize)
            }
        }
    }
}
