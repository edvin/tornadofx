package tornadofx.testapps

import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.text.FontWeight
import tornadofx.*

class SwitchViewApp2 : App(View1::class, Styles::class) {
    class View1 : View() {
        val controller: SwitchController by inject()
        override val root = stackpane {
            vbox {
                addClass(Styles.box, Styles.red)
                label("Main")
                button("Alternate") { setOnAction { replaceWith(View2::class, controller.transition) } }
            }
        }

        init {
            titleProperty.bind(controller.transitionProperty)
        }
    }

    class View2 : View() {
        val controller: SwitchController by inject()
        override val root = stackpane {
            vbox {
                addClass(Styles.box, Styles.blue)
                label("Alternate")
                button("Main") { setOnAction { replaceWith(View1::class, controller.transition) } }
            }
        }

        init {
            titleProperty.bind(controller.transitionProperty)
        }
    }

    class SwitchController : Controller() {
        private val time = .25.seconds
        private val doubleTime = time.multiply(2.0)
        private var currentTransition = 0
        private val transitions = listOf(
                "Fade" to Fade(time),
                "Slide Up" to Slide(time, Direction.UP),
                "Slide Right" to Slide(time, Direction.RIGHT),
                "Slide Down" to Slide(time, Direction.DOWN),
                "Slide Left" to Slide(time, Direction.LEFT),
                "Cover Up" to Cover(time, Direction.UP),
                "Cover Right" to Cover(time, Direction.RIGHT),
                "Cover Down" to Cover(time, Direction.DOWN),
                "Cover Left" to Cover(time, Direction.LEFT),
                "Reveal Up" to Reveal(time, Direction.UP),
                "Reveal Right" to Reveal(time, Direction.RIGHT),
                "Reveal Down" to Reveal(time, Direction.DOWN),
                "Reveal Left" to Reveal(time, Direction.LEFT),
                "Metro Up" to Metro(time, Direction.UP),
                "Metro Right" to Metro(time, Direction.RIGHT),
                "Metro Down" to Metro(time, Direction.DOWN),
                "Metro Left" to Metro(time, Direction.LEFT),
                "Swap Up" to Swap(doubleTime, Direction.UP),
                "Swao Right" to Swap(doubleTime, Direction.RIGHT),
                "Swap Down" to Swap(doubleTime, Direction.DOWN),
                "Swap Left" to Swap(doubleTime, Direction.LEFT)
        )
        val transitionProperty = SimpleStringProperty("Next Transition: ${transitions[0].first}")
        val transition: ViewTransition2
            get() {
                val t = transitions[currentTransition].second
                currentTransition = (currentTransition + 1) % transitions.size
                transitionProperty.value = "Next Transition: ${transitions[currentTransition].first}"
                return t
            }
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
