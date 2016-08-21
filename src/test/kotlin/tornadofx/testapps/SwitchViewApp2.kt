package tornadofx.testapps

import javafx.animation.FadeTransition
import javafx.animation.ParallelTransition
import javafx.util.Duration
import tornadofx.*

class SwitchViewApp2 : App(View1::class, Styles::class) {
    class View1 : View("View 1") {
        override val root = vbox {
            button("Go To View 2").setOnAction {
//                replaceWith(View2::class, fade)
                replaceWith(View2::class, ViewTransition.SlideIn)
            }
        }
    }

    class View2 : View("View 2") {
        override val root = vbox {
            button("Go To View 1").setOnAction {
//                replaceWith(View1::class, fade)
                replaceWith(View2::class, ViewTransition.SlideOut)
            }
        }
    }

    class Styles : Stylesheet() {
        init {
            root {
                padding = box(50.px)
            }
        }
    }

    companion object {
        val fade = Fade(1.seconds)
    }
}

class Fade(duration: Duration) : ViewTransition2(true, { current, replacement ->
    replacement.root.opacity = 0.0
    ParallelTransition(
            FadeTransition(duration, current.root).apply { toValue = 0.0 },
            FadeTransition(duration, replacement.root).apply { toValue = 1.0 }
    )
})
