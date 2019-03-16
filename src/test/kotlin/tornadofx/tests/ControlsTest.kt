package tornadofx.tests

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.embed.swing.JFXPanel
import javafx.scene.layout.VBox
import javafx.util.StringConverter
import javafx.util.converter.NumberStringConverter
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.testfx.api.FxToolkit
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

    @Before
    fun setupFX() {
        FxToolkit.registerPrimaryStage()
    }

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

        val view = TestView()
        val property = SimpleIntegerProperty(12718)
        val label = view.label(property)
        val labelWithConverter = view.label(property, converter = NumberStringConverter(Locale.US))

        Assert.assertEquals("12718", label.text)
        Assert.assertEquals(NumberFormat.getNumberInstance(Locale.US).format(12718), labelWithConverter.text)
    }

    @Test
    fun testSliderRange() {
        val view  = TestView()
        val slider = view.slider(24..42)
        Assert.assertEquals(24.0, slider.min, 10e-5)
        Assert.assertEquals(42.0, slider.max, 10e-5)
    }

    @Test
    fun testSplitMenuButton() {
        val view = TestView()
        val button = view.splitmenubutton("Text") {
            item("Item Name")
        }

        Assert.assertEquals(button.text, "Text")
        Assert.assertEquals(button.items.first().text, "Item Name")
    }
}
