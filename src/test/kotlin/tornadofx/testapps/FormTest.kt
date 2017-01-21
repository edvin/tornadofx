package tornadofx.testapps

import javafx.geometry.Orientation.VERTICAL
import javafx.scene.layout.Priority
import tornadofx.*

class FormApp : App(FormView::class)

class FormView : View("My Form") {
    override val root = form {
        fieldset("FieldSet") {
            field("Field 1") {
                textarea {
                    prefRowCount = 2
                    mnemonicTarget()
                }
            }

            field("Field 2", VERTICAL) {
                textarea {
                    vgrow = Priority.ALWAYS
                }
            }
        }
    }
}