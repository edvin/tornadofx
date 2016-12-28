package tornadofx.testapps

import tornadofx.*

class SqueezeBoxTestApp : App(SqueezeBoxTestView::class)

class SqueezeBoxTestView : View("SqueezeBox Multiple Open Folds") {
    override val root = squeezebox {
        fold("Pane 1", expanded = true) {
            label("I'm inside 1")
            label("Me too!")
        }
        fold("Pane 2", expanded = true) {
            label("I'm inside 2")
        }
        fold("Pane 3") {
            label("I'm inside 3")
        }
        fold("Pane 4", expanded = true) {
            label("I'm inside 4")
        }
        fold("Pane 5") {
            label("I'm inside 5")
        }
    }
}