package tornadofx.testapps

import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*

class RemoveTestApp : App(RemoveTest::class)

class RemoveTest : View("Remove Test") {
    val remove = SimpleBooleanProperty(false)

    override val root = vbox {
        setPrefSize(400.0, 200.0)
        form {
            label("I'm above the removable element (which might be removed of course)")
            label("Remove me!") {
                removeWhen { remove }
                style {
                    fontWeight = FontWeight.BOLD
                    textFill = Color.RED
                }
            }
            label("I'm below the removable element (which might be removed of course)")
            fieldset {
                field("Remove") {
                    combobox(remove, values = listOf(true, false))
                }
            }
        }
    }
}