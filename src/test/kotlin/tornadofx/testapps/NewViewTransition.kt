package tornadofx.testapps

import javafx.geometry.Pos
import javafx.scene.paint.*
import javafx.scene.text.FontWeight
import tornadofx.*

class NewViewTransitionRoot : App(Main::class, Styles::class)

class NewViewTransitionVBox : App(VBoxRootView::class, Styles::class) {
    class VBoxRootView : View("Switching Sub Views In VBox") {
        override val root = vbox {
            label("Top").addClass(Styles.topLabel)
            this += Main::class
            label("Bottom").addClass(Styles.bottomLabel)
        }
    }
}

class NewViewTransitionBorderPane : App(BorderPaneRootView::class, Styles::class) {
    class BorderPaneRootView : View("Switching Sub Views In BorderPane") {
        override val root = borderpane {
            top = label("Top") { addClass(Styles.topLabel) }
            right = label("Right") { addClass(Styles.rightLabel) }
            bottom = label("Bottom") { addClass(Styles.bottomLabel) }
            left = label("Left") { addClass(Styles.leftLabel) }
            center(Main::class)
        }
    }
}

abstract class SwapView(cssClass: CssRule) : View("Switching Views") {
    val controller: SwitchController by inject()
    val button = button(controller.firstTransition) { setOnAction { swap() } }
    override val root = stackpane {
        vbox {
            addClass(Styles.box, cssClass)
            label(this@SwapView.javaClass.simpleName)
            this += button
        }
    }

    abstract fun swap()
}

class Main : SwapView(Styles.red) {
    val alt: Alternate by inject()
    override fun swap() {
        controller.swap(this, alt)
    }
}

class Alternate : SwapView(Styles.blue) {
    val main: Main by inject()
    override fun swap() {
        controller.swap(this, main)
    }
}

class SwitchController : Controller() {
    private val time = .25.seconds
    private val doubleTime = time.multiply(2.0)
    private val fades = listOf(
            "Black" to Color.BLACK,
            "White" to Color.WHITE,
            "Red" to Color.RED,
            "Fade" to LinearGradient(0.0, 1.0, 1.0, 0.0, true, CycleMethod.NO_CYCLE, Stop(0.0, Color.BLACK), Stop(1.0, Color.WHITE)),
            "Crazy" to RadialGradient(0.0, 0.0, 150.0, 100.0, 75.0, false, CycleMethod.REPEAT,
                    Stop(0.0, Color.RED), Stop(0.33, Color.RED),
                    Stop(0.33, Color.GREEN), Stop(0.66, Color.GREEN),
                    Stop(0.66, Color.BLUE), Stop(1.0, Color.BLUE)
            )
    )

    private val transitions = listOf(
            "None" to null,
            "Fade" to Fade(time),
            *fades.map { "Fade Through ${it.first}" to FadeThrough(doubleTime, it.second) }.toTypedArray(),
            *Direction.values().map { "Slide $it" to Slide(time, it) }.toTypedArray(),
            *Direction.values().map { "Cover $it" to Cover(time, it) }.toTypedArray(),
            *Direction.values().map { "Reveal $it" to Reveal(time, it) }.toTypedArray(),
            *Direction.values().map { "Metro $it" to Metro(time, it) }.toTypedArray(),
            *Direction.values().map { "Swap $it" to Swap(doubleTime, it) }.toTypedArray(),
            "NewsFlash" to NewsFlash(doubleTime, 2.0)
    )
    val firstTransition = transitions[0].first
    private var currentTransition = 0
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
        val topLabel by cssclass()
        val rightLabel by cssclass()
        val bottomLabel by cssclass()
        val leftLabel by cssclass()
        val labelMix = mixin {
            maxWidth = infinity
            maxHeight = infinity
            alignment = Pos.BASELINE_CENTER
            padding = box(12.px)
            fontSize = 24.px
        }
    }

    init {
        box {
            prefWidth = 400.px
            prefHeight = 300.px
            spacing = 12.px
            alignment = Pos.CENTER
            s(label, button) {
                textFill = Color.WHITE
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
                nuke.value = Color(.75, .5, .5, 1.0)
                +boxMix
            }
            and(blue) {
                nuke.value = Color(.5, .5, .75, 1.0)
                +boxMix
            }
        }
        topLabel {
            +labelMix
            backgroundColor += Color.BLACK
            textFill = Color.WHITE
        }
        rightLabel {
            +labelMix
            backgroundColor += Color.gray(.25)
            textFill = Color.gray(.75)
        }
        bottomLabel {
            +labelMix
            backgroundColor += Color.WHITE
        }
        leftLabel {
            +labelMix
            backgroundColor += Color.gray(.75)
            textFill = Color.gray(.25)
        }
    }
}
