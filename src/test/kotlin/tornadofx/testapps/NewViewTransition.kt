package tornadofx.testapps

import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.input.MouseButton
import javafx.scene.paint.*
import javafx.scene.text.FontWeight
import tornadofx.*

class NewViewTransitionRoot : App(NewViewTransitionMain::class, NewViewTransitionStyles::class)

class NewViewTransitionVBox : App(VBoxRootView::class, NewViewTransitionStyles::class) {
    class VBoxRootView : View("Switching Sub Views In VBox") {
        override val root = vbox {
            label("Top").addClass(NewViewTransitionStyles.topLabel)
            this += NewViewTransitionMain::class
            label("Bottom").addClass(NewViewTransitionStyles.bottomLabel)
        }
    }
}

class NewViewTransitionBorderPane : App(BorderPaneRootView::class, NewViewTransitionStyles::class) {
    class BorderPaneRootView : View("Switching Sub Views In BorderPane") {
        override val root = borderpane {
            top = label("Top") { addClass(NewViewTransitionStyles.topLabel) }
            right = label("Right") { addClass(NewViewTransitionStyles.rightLabel) }
            bottom = label("Bottom") { addClass(NewViewTransitionStyles.bottomLabel) }
            left = label("Left") { addClass(NewViewTransitionStyles.leftLabel) }
            center(NewViewTransitionMain::class)
        }
    }
}

abstract class NewViewTransitionSwapView(private val name: String, cssClass: CssRule) : View("Switching Views On Scene Root") {
    val controller: NewViewTransitionController by inject()
    val nextTransition = SimpleStringProperty(controller.firstTransition)
    val nameLabel = label(name)
    val surpriseLabel = label("Surprise!")
    override val root = stackpane {
        vbox {
            addClass(NewViewTransitionStyles.box, cssClass)
            this += nameLabel
            button {
                textProperty().bind(nextTransition)
                setOnAction { swap() }
            }
        }
    }

    fun Node.rightClick(action: Node.() -> Unit) = this.setOnMouseClicked { if (it.button == MouseButton.SECONDARY) action() }

    init {
        val surprise = ViewTransition.Metro(0.25.seconds, ViewTransition.Direction.UP)
        nameLabel.rightClick { replaceWith(surpriseLabel, surprise) }
        surpriseLabel.rightClick { replaceWith(nameLabel, surprise.reversed()) }
    }

    abstract fun swap()
}

class NewViewTransitionMain : NewViewTransitionSwapView("Main", NewViewTransitionStyles.red) {
    val alt: NewViewTransitionAlternate by inject()
    override fun swap() {
        controller.swap(this, alt)
    }
}

class NewViewTransitionAlternate : NewViewTransitionSwapView("Alternate", NewViewTransitionStyles.blue) {
    val main: NewViewTransitionMain by inject()
    override fun swap() {
        controller.swap(this, main)
    }
}

class NewViewTransitionController : Controller() {
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
            "Fade" to ViewTransition.Fade(time),
            *fades.map { "${it.first} Fade" to ViewTransition.FadeThrough(doubleTime, it.second) }.toTypedArray(),
            *ViewTransition.Direction.values().map { "Slide $it" to ViewTransition.Slide(time, it) }.toTypedArray(),
            *ViewTransition.Direction.values().map { "Cover $it" to ViewTransition.Cover(time, it) }.toTypedArray(),
            *ViewTransition.Direction.values().map { "Reveal $it" to ViewTransition.Reveal(time, it) }.toTypedArray(),
            *ViewTransition.Direction.values().map { "Metro $it" to ViewTransition.Metro(time, it) }.toTypedArray(),
            *ViewTransition.Direction.values().map { "Swap $it" to ViewTransition.Swap(doubleTime, it) }.toTypedArray(),
            *ViewTransition.Direction.values().map { "Flip $it" to ViewTransition.Flip(time, it) }.toTypedArray(),
            "NewsFlash" to ViewTransition.NewsFlash(doubleTime, 2.0)
    )
    val firstTransition = transitions[0].first
    private var currentTransition = 0
    fun swap(current: NewViewTransitionSwapView, replacement: NewViewTransitionSwapView) {
        val t = transitions[currentTransition].second
        currentTransition = (currentTransition + 1) % transitions.size
        replacement.nextTransition.value = transitions[currentTransition].first
        current.replaceWith(replacement, t)
    }
}

class NewViewTransitionStyles : Stylesheet() {
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

        fun labelMix(gray: Double) = mixin {
            maxWidth = infinity
            maxHeight = infinity
            alignment = Pos.BASELINE_CENTER
            padding = box(12.px)
            fontSize = 24.px
            backgroundColor += Color.gray(gray)
            textFill = Color.gray(1 - gray)
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
            +labelMix(0.0)
        }
        rightLabel {
            +labelMix(0.25)
        }
        bottomLabel {
            +labelMix(1.0)
        }
        leftLabel {
            +labelMix(0.75)
        }
    }
}
