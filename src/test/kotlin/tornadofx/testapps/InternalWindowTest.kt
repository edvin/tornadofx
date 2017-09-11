package tornadofx.testapps

import javafx.scene.control.Alert.AlertType.CONFIRMATION
import tornadofx.*

class InternalWindowTestApp : App(InternalWindowTest::class)

class InternalWindowTest : View("Internal Window") {
    override val root = stackpane {
        setPrefSize(600.0, 400.0)
        button("Open editor").action {
            openInternalWindow<Editor>(modal = false, icon = FX.icon)
        }

    }
}

class Editor : View("Editor") {
    override val root = form {
        prefWidth = 300.0

        fieldset("Editor") {
            field("First field") {
                textfield()
            }
            field("Second field") {
                textfield()
            }
            button("Save") {
                shortcut("Alt+S")
                action {
                    save()
                }
            }
        }
    }

    private fun save() {
        alert(CONFIRMATION, "Saved!", "You did it!")
        close()
    }
}