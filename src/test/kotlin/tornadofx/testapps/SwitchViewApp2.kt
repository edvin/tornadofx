package tornadofx.testapps

import javafx.geometry.Pos
import javafx.scene.text.FontWeight
import tornadofx.*

class SwitchViewApp2 : App(View1::class, Styles::class) {
    class View1 : View("Main") {
        val controller: SwitchController by inject()
        override val root = stackpane {
            vbox {
                addClass(Styles.box, Styles.red)
                label("Main")
                button("Alternate") { setOnAction { replaceWith(View2::class, controller.transition) } }
            }
        }
    }

    class View2 : View("Alternate") {
        val controller: SwitchController by inject()
        override val root = stackpane {
            vbox {
                addClass(Styles.box, Styles.blue)
                label("Alternate")
                button("Main") {
                    addClass(Styles.blue)
                    setOnAction { replaceWith(View1::class, controller.transition) }
                }
            }
        }
    }

    class SwitchController : Controller() {
        private val time = .25.seconds
        private var currentTransition = 0
        private val transitions = listOf(
                Fade(time),
                Slide(time, Direction.UP),
                Slide(time, Direction.RIGHT),
                Slide(time, Direction.DOWN),
                Slide(time, Direction.LEFT),
                Cover(time, Direction.UP),
                Cover(time, Direction.RIGHT),
                Cover(time, Direction.DOWN),
                Cover(time, Direction.LEFT),
                Reveal(time, Direction.UP),
                Reveal(time, Direction.RIGHT),
                Reveal(time, Direction.DOWN),
                Reveal(time, Direction.LEFT),
                Metro(time, Direction.UP),
                Metro(time, Direction.RIGHT),
                Metro(time, Direction.DOWN),
                Metro(time, Direction.LEFT),
                Swap(time, Direction.UP),
                Swap(time, Direction.RIGHT),
                Swap(time, Direction.DOWN),
                Swap(time, Direction.LEFT)
        )
        val transition: ViewTransition2
            get() = transitions[currentTransition++ % transitions.size]
//        val transition: ViewTransition2 = Swap(2.seconds, Direction.RIGHT)
    }

    class Styles : Stylesheet() {
        companion object {
            val box by cssclass()
            val red by cssclass()
            val blue by cssclass()
        }

        init {
            box {
                prefWidth = 400.px
                prefHeight = 300.px
                spacing = 12.px
                alignment = Pos.CENTER
                s(label, button) {
                    fontSize = 36.px
                    fontWeight = FontWeight.BOLD
                    alignment = Pos.BASELINE_CENTER
                }
                and(red) {
                    backgroundColor += c(1.0, .75, .75)
                }
                and(blue) {
                    backgroundColor += c(.75, .75, 1.0)
                }
            }
        }
    }
}
