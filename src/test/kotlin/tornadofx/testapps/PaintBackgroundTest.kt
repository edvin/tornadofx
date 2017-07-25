package tornadofx.testapps

import javafx.scene.paint.Color
import tornadofx.*

class PaintBackgroundTestApp : App(PaintBackgroundTest::class)

class PaintBackgroundTest : View() {
    override val root = hbox {
        label("Hello Background Color") {
            background = Color.LIGHTBLUE.asBackground()
        }
    }
}