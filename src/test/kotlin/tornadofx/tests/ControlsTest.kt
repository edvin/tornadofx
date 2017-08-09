package tornadofx.tests

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.embed.swing.JFXPanel
import javafx.scene.layout.VBox
import javafx.util.StringConverter
import javafx.util.converter.NumberStringConverter
import org.junit.Assert
import org.junit.Test
import org.testfx.api.FxToolkit
import org.testfx.toolkit.ToolkitService
import tornadofx.*
import java.text.NumberFormat
import java.util.*

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

    @Test
    fun testLabelWithStringObservable() {
        val view = TestView()
        val property = SimpleStringProperty("Daan")
        val label = view.label(property)
        val labelWithConverter = view.label(property, converter = object : StringConverter<String>() {
            override fun toString(string: String?) = string?.toUpperCase() ?: ""
            override fun fromString(string: String?) = throw NotImplementedError()
        })

        Assert.assertEquals("Daan", label.text)
        Assert.assertEquals("DAAN", labelWithConverter.text)
    }

    @Test
    fun testLabelWithIntegerObservable() {
        FxToolkit.registerPrimaryStage()

        val view = TestView()
        val property = SimpleIntegerProperty(12718)
        val label = view.label(property)
        val labelWithConverter = view.label(property, converter = NumberStringConverter(Locale.US))

        Assert.assertEquals("12718", label.text)
        Assert.assertEquals(NumberFormat.getNumberInstance(Locale.US).format(12718), labelWithConverter.text)
    }
}
