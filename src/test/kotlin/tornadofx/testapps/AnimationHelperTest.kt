package tornadofx.testapps

import javafx.animation.Animation
import javafx.animation.Interpolator
import javafx.scene.paint.Color
import tornadofx.*

class ShapeTransitionTest : App(Main::class) {
    class Main : View("Shape Transition Test") {
        var switch = true
        val nextFill get() = if (switch) c(1.0, 0.0, 0.0) else c(0.0, 0.0, 1.0)
        val nextStroke get() = if (switch) c(0.0, 0.0, 1.0) else c(1.0, 0.0, 0.0)
        override val root = stackpane {
            background = Color.BLACK.asBackground()
            val round = circle(radius = 100.0) {
                fill = nextFill
                stroke = nextStroke
                switch = !switch
                strokeWidth = 20.0
            }
            val orbit = circle(0.0, 0.0, 10.0) {
                animateFill(0.33.seconds, Color.ORANGE, Color.YELLOW) {
                    isAutoReverse = true
                    cycleCount = Animation.INDEFINITE
                }
            }.follow(3.seconds, round, Interpolator.LINEAR) { cycleCount = Animation.INDEFINITE }
            button("Click Me") {
                action {
                    isDisable = true
                    val startFill = round.fill as? Color ?: Color.BLACK
                    val endFill = nextFill
                    val startStroke = round.stroke as? Color ?: Color.BLACK
                    val endStroke = nextStroke
                    switch = !switch
                    listOf(
                            pause(0.75.seconds, play = false) { setOnFinished { orbit.rate = -orbit.rate } },
                            listOf(
                                    round.animateFill(0.25.seconds, startFill, endFill, play = false),
                                    round.animateStroke(0.25.seconds, startStroke, endStroke, play = false)
                            ).playParallel(false)
                    ).playSequential { setOnFinished { isDisable = false } }
                }
            }
        }
    }
}
