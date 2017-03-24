package tornadofx.testapps

import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.paint.Color
import tornadofx.*

class PaddingPropertyTestApp : App(PaddingPropertyTest::class)

class PaddingPropertyTest : View("Slide Padding") {
    val prop = SimpleIntegerProperty(1)

    override val root = vbox {
        setPrefSize(400.0, 150.0)
        style { backgroundColor += Color.CADETBLUE }
        slider(0.0, 100.0, 20.0) {
            this@vbox.paddingAllProperty.bind(valueProperty())
        }
        spinner(true, prop)
    }
}