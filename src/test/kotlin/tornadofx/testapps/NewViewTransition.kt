package tornadofx.testapps

import javafx.animation.Animation
import javafx.animation.Interpolator
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.StackPane
import javafx.scene.paint.*
import javafx.scene.text.FontWeight
import javafx.util.Duration
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
            add<NewViewTransitionMain>()
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
            right = label("Right") { addClass(NewViewTransitionStyles.greenLabel) }
            bottom = label("Bottom") { addClass(NewViewTransitionStyles.lightLabel) }
            left = label("Left") { addClass(NewViewTransitionStyles.greenLabel) }
            center<NewViewTransitionMain>()
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
                action { swap() }
            }
        }
    }

    init {
        val surprise = ViewTransition.Flip(.1.seconds, true)
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
            "Green" to NewViewTransitionStyles.nukeGreen,
            "Fade" to LinearGradient(0.0, 0.0, 0.0, 1.0, true, CycleMethod.NO_CYCLE, Stop(0.0, Color.BLACK), Stop(1.0, Color.WHITE)),
            "Crazy" to RadialGradient(0.0, 0.0, 150.0, 100.0, 75.0, false, CycleMethod.REPEAT,
                    Stop(0.0, NewViewTransitionStyles.nukeRed), Stop(0.33, NewViewTransitionStyles.nukeRed),
                    Stop(0.33, NewViewTransitionStyles.nukeGreen), Stop(0.66, NewViewTransitionStyles.nukeGreen),
                    Stop(0.66, NewViewTransitionStyles.nukeBlue), Stop(1.0, NewViewTransitionStyles.nukeBlue)
            )
    )

    private inline infix fun String.eachWay(constructor: (ViewTransition.Direction) -> ViewTransition): Array<Pair<String, ViewTransition>> {
        return ViewTransition.Direction.values().map { "$this $it" to constructor(it) }.toTypedArray()
    }

    private val transitions = listOf(
            "None" to null,
            "Fade" to ViewTransition.Fade(time),
            *fades.map { "${it.first} Fade" to ViewTransition.FadeThrough(doubleTime, it.second) }.toTypedArray(),
            *"Slide" eachWay { ViewTransition.Slide(time, it) },
            *"Cover" eachWay { ViewTransition.Cover(time, it) },
            *"Reveal" eachWay { ViewTransition.Reveal(time, it) },
            *"Metro" eachWay { ViewTransition.Metro(time, it) },
            *"Swap" eachWay { ViewTransition.Swap(doubleTime, it) },
            *"Custom" eachWay { CustomViewTransition(doubleTime, it) },
            "Flip Horizontal" to ViewTransition.Flip(time, false),
            "Flip Vertical" to ViewTransition.Flip(time, true),
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
        val greenLabel by cssclass()

        val nukeRed = c(.75, .5, .5)
        val nukeGreen = c(0.5, 0.75, 0.5)
        val nukeBlue = c(.5, .5, .75)

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
                nuke.value = nukeRed
                +boxMix
            }
            and(blue) {
                nuke.value = nukeBlue
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
        greenLabel {
            +labelMix
            backgroundColor += nukeGreen
            textFill = Color.WHITE
        }
    }
}

class CustomViewTransition(val duration: Duration, val direction: Direction = ViewTransition.Direction.LEFT) : ViewTransition.ReversibleViewTransition<CustomViewTransition>() {
    val halfSpeed = duration.divide(2.0)!!
    val scale = when (direction) {
        Direction.UP, Direction.DOWN -> point(1, 0)
        Direction.LEFT, Direction.RIGHT -> point(0, 1)
    }

    override fun create(current: Node, replacement: Node, stack: StackPane): Animation {
        val bounds = current.boundsInLocal
        val destination = when (direction) {
            Direction.UP -> point(0, -bounds.height / 2)
            Direction.RIGHT -> point(bounds.width / 2, 0)
            Direction.DOWN -> point(0, bounds.height / 2)
            Direction.LEFT -> point(-bounds.width / 2, 0)
        }
        return replacement.transform(
                halfSpeed, destination.multiply(-1.0), 0.0, scale, 1.0,
                easing = Interpolator.LINEAR, reversed = true, play = false
        ) and current.transform(
                halfSpeed, destination, 0.0, scale, 1.0,
                easing = Interpolator.LINEAR, play = false
        )
    }

    override fun reversed() = CustomViewTransition(duration, direction.reversed())

    override fun onComplete(removed: Node, replacement: Node) {
        removed.translateX = 0.0
        removed.translateY = 0.0
        removed.scaleX = 1.0
        removed.scaleY = 1.0
    }
}
