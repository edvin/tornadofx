package tornadofx.testapps

import javafx.geometry.Pos
import javafx.scene.effect.DropShadow
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.*
import javafx.scene.text.FontWeight
import javafx.util.StringConverter
import tornadofx.*

class TransitionDemoApp : App(TransitionDemo::class, TransitionDemoStyles::class) {
    class TransitionDemo : View("Transition Demo") {
        val controller: TransitionDemoController by inject()
        override val root = borderpane {
            left = vbox {
                addClass(TransitionDemoStyles.navigation)
                button("Home")
                button("Text")
                button("Study Cards")
                this += Pane().apply { VBox.setVgrow(this, Priority.ALWAYS) }
                val time = .25.seconds
                choicebox(listOf(
                        "Fade" to ViewTransition.Fade(time),
                        "Fade 2" to ViewTransition.FadeThrough(time),
                        "Slide" to ViewTransition.Slide(time),
                        "Cover" to ViewTransition.Cover(time),
                        "Reveal" to ViewTransition.Reveal(time)
                ).observable()) {
                    converter = object : StringConverter<Pair<String, ViewTransition>>() {
                        override fun fromString(string: String): Pair<String, ViewTransition> {
                            throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
                        }

                        override fun toString(value: Pair<String, ViewTransition>): String {
                            return value.first
                        }
                    }
                }
            }
            center(TransitionDemoMain::class)
        }
    }

    class TransitionDemoMain : View() {
        override val root = stackpane {
            addClass(TransitionDemoStyles.mainPage)
            vbox {
                addClass(TransitionDemoStyles.boxed)
                label("Welcome To The")
                label("Transition Demo").addClass(TransitionDemoStyles.big)
            }
        }
    }

    class TransitionDemoController : Controller() {
    }

    class TransitionDemoStyles : Stylesheet() {
        companion object {
            val navigation by cssclass()

            val mainPage by cssclass()
            val boxed by cssclass()
            val big by cssclass()

            val optionBox by cssclass()

            val bg = Color.gray(.25)
            val back = mixin {
                backgroundColor += bg
            }
        }

        init {
            root {
                prefWidth = 500.px
                prefHeight = 350.px
            }
            navigation {
                +back
                padding = box(5.px)
                spacing = 5.px

                s(button, choiceBox) {
                    +back
                    maxWidth = infinity
                    alignment = Pos.BASELINE_CENTER
                    fontSize = 14.px
                    fontWeight = FontWeight.BOLD
                    padding = box(5.px, 10.px)
                    textFill = Color.WHITE

                    and(hover) {
                        backgroundColor += bg.darker()
                        effect = DropShadow(3.0, 0.0, 3.0, Color.BLACK)

                        and(armed) {
                            effect = DropShadow(3.0, 0.0, 1.0, Color.BLACK)
                        }
                    }
                }
            }
            mainPage {
                backgroundColor += Color.gray(.75)
                label {
                    fontWeight = FontWeight.BOLD
                    fontSize = 20.px

                    and(big) {
                        fontSize = 24.px
                    }
                }
            }
            boxed {
                alignment = Pos.CENTER
            }
            form and optionBox {
                fontWeight = FontWeight.NORMAL
                prefWidth = 250.px
            }
        }
    }
}

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

abstract class NewViewTransitionSwapView(name: String, cssClass: CssRule) : View("Switching Views On Scene Root") {
    val controller: NewViewTransitionController by inject()
    val button = button(controller.firstTransition) { setOnAction { swap() } }
    override val root = stackpane {
        vbox {
            addClass(NewViewTransitionStyles.box, cssClass)
            label(name)
            this += button
        }
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
            *fades.map { "Fade Through ${it.first}" to ViewTransition.FadeThrough(doubleTime, it.second) }.toTypedArray(),
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
        replacement.button.text = transitions[currentTransition].first
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
