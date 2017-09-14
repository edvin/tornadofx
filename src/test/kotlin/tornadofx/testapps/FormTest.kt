package tornadofx.testapps

import tornadofx.*

class FormApp : App(FormView::class)

class FormView : SimpleView("My Form", {
    form {
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
})