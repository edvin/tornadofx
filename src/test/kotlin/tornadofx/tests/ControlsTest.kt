package tornadofx.tests

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.embed.swing.JFXPanel
import javafx.scene.layout.VBox
import javafx.util.converter.NumberStringConverter
import org.junit.Assert
import org.junit.Test
import tornadofx.View
import tornadofx.bind
import tornadofx.imageview
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

    @Test
    fun testImageview() {

        val view = TestView()
        val property = SimpleStringProperty(null)
        val imageView = view.imageview(property)

        Assert.assertNull(imageView.image)

        property.value = "/tornadofx/tests/person.png"

        Assert.assertNotNull(imageView.image)
    }
}