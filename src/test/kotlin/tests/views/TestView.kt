package tests.views

import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import tests.controllers.MyController
import tests.fragments.MyFragment
import tornadofx.*

class TestView : View() {
    override val root = HBox(Label("Hello from the other side"))
    val myController: MyController by inject()

    init {
        title = "My TestView"

        root.apply {
            spacing = 10.0
            alignment = Pos.BASELINE_LEFT

            button("Click me") {
                setOnAction {
                    find(MyFragment::class).openModal()
                }
            }

            button() {
                text = "Test"
            }

            label("Hello there")

            gridpane {
                hgap = 15.0

                row { label("One"); textfield { promptText = "Input number one" } }
                row { label("Two"); textfield { promptText = "Input number two" } }
            }
        }

    }

}