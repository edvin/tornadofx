package tornadofx.testapps

import javafx.geometry.Orientation
import tornadofx.*

class FormApp : App(FormView::class)

class FormView : View("My Form") {
    override val root = form {
        fieldset("FieldSet", labelPosition = Orientation.VERTICAL) {
            wrapWidth = 340

            field("Field 1") {
                textarea {
                    prefRowCount = 2
                    mnemonicTarget()
                }
            }

            field("Field 2") {
                textarea {
                    prefRowCount = 10
                }
            }
        }
    }
}