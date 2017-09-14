package tornadofx.testapps

import javafx.scene.paint.Color
import tornadofx.*

class PaddingPropertyTestApp : App(PaddingPropertyTest::class)

class PaddingPropertyTest : SimpleView("Slide Padding", {
    vbox {
        setPrefSize(400.0, 150.0)
        style { backgroundColor += Color.CADETBLUE }
        slider(0.0, 100.0, 20.0) {
            this@vbox.paddingAllProperty.bind(valueProperty())
        }
    }
})