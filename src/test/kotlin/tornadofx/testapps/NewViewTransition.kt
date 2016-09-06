package tornadofx.testapps

import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.paint.*
import javafx.scene.text.FontWeight
import tornadofx.*

/**
 * Run the demo as the root of the scene
 */
class NewViewTransitionRoot : App(NewViewTransitionMain::class, NewViewTransitionStyles::class)

/**
 * Run the demo in a VBox
 *
 * Notice the z-ordering is based on the index withing the VBox
 */
class NewViewTransitionVBox : App(VBoxRootView::class, NewViewTransitionStyles::class) {
    class VBoxRootView : View("Switching Sub Views In VBox") {
        override val root = vbox {
            label("Top").addClass(NewViewTransitionStyles.darkLabel)
            this += NewViewTransitionMain::class
            label("Bottom").addClass(NewViewTransitionStyles.lightLabel)
        }
    }
}

/**
 * Run the demo in a BorderPane
 *
 * Notice the z-ordering is based on when the node was added to the scene graph
 */
class NewViewTransitionBorderPane : App(BorderPaneRootView::class, NewViewTransitionStyles::class) {
    class BorderPaneRootView : View("Switching Sub Views In BorderPane") {
        override val root = borderpane {
            top = label("Top") { addClass(NewViewTransitionStyles.darkLabel) }
            right = label("Right") { addClass(NewViewTransitionStyles.redLabel) }
            bottom = label("Bottom") { addClass(NewViewTransitionStyles.lightLabel) }
            left = label("Left") { addClass(NewViewTransitionStyles.redLabel) }
            center(NewViewTransitionMain::class)
        }
    }
}

abstract class NewViewTransitionSwapView(name: String, cssClass: CssRule) : View("Switching Views On Scene Root") {
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

    init {
        val surprise = ViewTransition.Flip(.1.seconds, ViewTransition.Direction.DOWN)
        nameLabel.setOnMouseClicked { nameLabel.replaceWith(surpriseLabel, surprise) }
        surpriseLabel.setOnMouseClicked { surpriseLabel.replaceWith(nameLabel, surprise) }
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
            "Fade" to LinearGradient(0.0, 0.0, 0.0, 1.0, true, CycleMethod.NO_CYCLE, Stop(0.0, Color.BLACK), Stop(1.0, Color.WHITE)),
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
            "Explode" to ViewTransition.Explode(time),
            "Implode" to ViewTransition.Implode(time),
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
        val darkLabel by cssclass()
        val lightLabel by cssclass()
        val redLabel by cssclass()

        val boxMix = mixin {
            bg force nuke
            button {
                bg force raw("derive(${nuke.name}, -10%)")
                and(hover) {
                    bg force raw("derive(${nuke.name}, -15%)")
                }
            }
        }

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
            and(red) {
                nuke.value = Color(.75, .5, .5, 1.0)
                +boxMix
            }
            and(blue) {
                nuke.value = Color(.5, .5, .75, 1.0)
                +boxMix
            }
        }
        darkLabel {
            +labelMix
            backgroundColor += Color.BLACK
            textFill = Color.WHITE
        }
        lightLabel {
            +labelMix
            backgroundColor += Color.WHITE
            textFill = Color.BLACK
        }
        redLabel {
            +labelMix
            backgroundColor += Color.RED
            textFill = Color.WHITE
        }
    }
}
