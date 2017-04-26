package tornadofx.testapps

import javafx.geometry.Orientation
import tornadofx.*

class FormApp : App(FormView::class)

class FormView : View("My Form") {
    override val root = form {
        fieldset(labelPosition = Orientation.VERTICAL) {
            field("Name") {
                textfield()
            }
            field("Date Of Birth") {
                textfield()
            }
            field("Height") {
                textfield()
            }
            field("Weight") {
                textfield()
            }
            buttonbar {
                button("Save")
            }
        }
    }
}