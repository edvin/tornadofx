package tests.fragments

import javafx.scene.control.Label
import javafx.scene.layout.HBox
import tornadofx.Fragment

class MyFragment : Fragment() {
    override val root = HBox(Label("I'm a fragment!"))
}