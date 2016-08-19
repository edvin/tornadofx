package tornadofx.tests.resources

import javafx.scene.text.FontWeight
import tornadofx.Stylesheet

class TestStylesheet : Stylesheet() {
    init {
        label {
            fontWeight = FontWeight.BOLD
        }
    }
}