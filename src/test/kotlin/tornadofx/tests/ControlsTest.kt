package tornadofx.tests

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.embed.swing.JFXPanel
import javafx.scene.layout.VBox
import javafx.util.converter.NumberStringConverter
import org.junit.Test
import tornadofx.View
import tornadofx.bind
import tornadofx.textfield

/**
 * @author carl
 */

class TestView : View() {
    override val root = VBox()
}

class ControlsTest {

    @Test
    fun testTextfield() {

        JFXPanel()

        val view = TestView()

        view.textfield(SimpleStringProperty("carl"))

        view.textfield(SimpleIntegerProperty(101), NumberStringConverter())

        view.textfield() {
            bind(SimpleIntegerProperty(102))
        }

        view.textfield(SimpleIntegerProperty(103))  // w. fix 184

        view.textfield(SimpleDoubleProperty(100.131))  // also fixed 184
    }
}