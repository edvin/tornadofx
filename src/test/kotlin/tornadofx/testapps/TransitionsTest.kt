package tornadofx.testapps

import javafx.animation.Interpolator
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import tornadofx.*

class TransitionsTestApp : App(TransitionsTestView::class)

class TransitionsTestView : View() {
    val r1 = Rectangle(20.0, 20.0, Color.RED)
    val r2 = Rectangle(20.0, 20.0, Color.YELLOW)
    val r3 = Rectangle(20.0, 20.0, Color.GREEN)
    val r4 = Rectangle(20.0, 20.0, Color.BLUE)

    override val root = vbox {
        button("Animate").action {
            sequentialTransition {
                timeline {
                    keyframe(0.5.seconds) {
                        keyvalue(r1.translateXProperty(), 50.0, interpolator = Interpolator.EASE_BOTH)
                    }
                }
                timeline {
                    keyframe(0.5.seconds) {
                        keyvalue(r2.translateXProperty(), 100.0, interpolator = Interpolator.EASE_BOTH)
                    }
                }
                timeline {
                    keyframe(0.5.seconds) {
                        keyvalue(r3.translateXProperty(), 150.0, interpolator = Interpolator.EASE_BOTH)
                    }
                }
                timeline {
                    keyframe(0.5.seconds) {
                        keyvalue(r4.translateXProperty(), 200.0, interpolator = Interpolator.EASE_BOTH)
                    }
                }
            }
        }
        pane {
            add(r1)
            add(r2)
            add(r3)
            add(r4)
        }
    }
}

