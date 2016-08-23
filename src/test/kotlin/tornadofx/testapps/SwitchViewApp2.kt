package tornadofx.testapps

import javafx.geometry.Pos
import javafx.scene.text.FontWeight
import tornadofx.*

class SwitchViewApp2 : App(Main::class, Styles::class) {
    abstract class SwapView(name: String, cssClass: CssRule) : View(name) {
        val controller: SwitchController by inject()
        val button = button("Fade") { setOnAction { swap() } }
        override val root = stackpane {
            vbox {
                addClass(Styles.box, cssClass)
                label(name)
                this += button
            }
        }

        abstract fun swap()
    }

    class Main : SwapView("Main", Styles.red) {
        val alt: Alt by inject()
        override fun swap() {
            controller.swap(this, alt)
        }
    }

    class Alt : SwapView("Alternate", Styles.blue) {
        val main: Main by inject()
        override fun swap() {
            controller.swap(this, main)
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
                "Cover From Up" to Cover(time, Direction.UP),
                "Cover From Right" to Cover(time, Direction.RIGHT),
                "Cover From Down" to Cover(time, Direction.DOWN),
                "Cover From Left" to Cover(time, Direction.LEFT),
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

        fun swap(current: SwapView, replacement: SwapView) {
            val t = transitions[currentTransition].second
            currentTransition = (currentTransition + 1) % transitions.size
            replacement.button.text = transitions[currentTransition].first
            current.replaceWith(replacement, t)
        }
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
