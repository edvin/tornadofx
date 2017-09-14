package tornadofx.testapps

import javafx.scene.paint.Color
import tornadofx.*

class PaintBackgroundTestApp : App(PaintBackgroundTest::class)

class PaintBackgroundTest : SimpleView({
    hbox {
        label("Hello Background Color") {
            background = Color.LIGHTBLUE.asBackground()
        }
    }
})