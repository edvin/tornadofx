package tornadofx.testapps

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

    class MainView : SimpleView({
        vbox {
            addClass(box)
            label("Test").addClass(test)
        }
    })

    companion object {
        val box by cssclass()
        val test by cssclass()
    }

    class Styles : Stylesheet(ParentStyles::class) {
        init {
            test {
                fontSize = 36.px
                fontWeight = FontWeight.BOLD
                textFill = Color.WHITE
            }
        }
    }

    class ParentStyles : SimpleStylesheet({
        box {
            backgroundColor += Color.GRAY
            padding = box(10.px, 100.px)
        }
    })
}
