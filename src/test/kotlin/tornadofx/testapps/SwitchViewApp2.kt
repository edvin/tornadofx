package tornadofx.testapps

import javafx.geometry.Pos
import javafx.scene.paint.Paint
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
                "Fade Through Black" to FadeThrough(time, c("black")),
                *Direction.values().map { "Slide $it" to Slide(time, it) }.toTypedArray(),
                *Direction.values().map { "Cover from $it" to Cover(time, it) }.toTypedArray(),
                *Direction.values().map { "Reveal $it" to Reveal(time, it) }.toTypedArray(),
                *Direction.values().map { "Metro $it" to Metro(time, it) }.toTypedArray(),
                *Direction.values().map { "Swap $it" to Swap(doubleTime, it) }.toTypedArray(),
                "NewsFlash" to NewsFlash(doubleTime, 2.0)
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
            val bg by cssproperty<Paint>("-fx-background-color")
            val nuke by cssproperty<Paint>()
        }

        init {
            box {
                prefWidth = 400.px
                prefHeight = 300.px
                spacing = 12.px
                alignment = Pos.CENTER
                s(label, button) {
                    textFill = c("white")
                    fontSize = 36.px
                    fontWeight = FontWeight.BOLD
                    alignment = Pos.BASELINE_CENTER
                }
                val boxMix = mixin {
                    bg force nuke
                    button {
                        bg force raw("derive(${nuke.name}, -10%)")
                        and(hover) {
                            bg force raw("derive(${nuke.name}, -15%)")
                        }
                    }
                }
                and(red) {
                    val c = c(.75, .5, .5)
                    nuke.value = c
                    +boxMix
                }
                and(blue) {
                    val c = c(.5, .5, .75)
                    nuke.value = c
                    +boxMix
                }
            }
        }
    }
}
