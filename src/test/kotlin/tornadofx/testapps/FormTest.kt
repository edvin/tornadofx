package tornadofx.testapps

import javafx.geometry.Orientation
import tornadofx.*

class FormApp : App(FormView::class)

class FormView : View("My Form") {
    override val root = form {
        fieldset(title) {
            field("Name") {
                textfield()
            }
            field("Date Of _Birth") {
                textfield {
                    mnemonicTarget()
                }
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