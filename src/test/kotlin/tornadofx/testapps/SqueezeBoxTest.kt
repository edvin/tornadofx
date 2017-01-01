package tornadofx.testapps

import javafx.geometry.Pos
import tornadofx.*

class SqueezeBoxTestApp : App(SqueezeBoxTestView::class)

object AddFoldEvent : FXEvent()

class SqueezeBoxTestView : View("SqueezeBox Multiple Open Folds") {
    override val root = vbox(5.0) {
        setPrefSize(300.0, 600.0)

        button("Add node") {
            setOnAction {
                fire(AddFoldEvent)
            }
        }

        squeezebox {
            fold("Pane 1", expanded = true) {
                stackpane {
                    vbox {
                        alignment = Pos.CENTER
                        label("I'm inside 1")
                        label("Me too!")
                    }
                }
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
            subscribe<AddFoldEvent> {
                fold("Another fold by subscription") {
                    stackpane {
                        label("Yo!")
                    }
                }
            }
        }
    }
}