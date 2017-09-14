package tornadofx.testapps

import javafx.scene.control.Alert
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import tornadofx.*

class AccelTestApp : App(AccelTest::class)

class AccelTest : SimpleView({
    stackpane {
        add<AccelView>()
    }
})

class AccelView : SimpleView({
    button("Click me") {
        shortcut(KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_ANY, KeyCombination.SHIFT_ANY))
        action {
            alert(Alert.AlertType.INFORMATION, "Fire!", "You clicked.")
        }
    }
})