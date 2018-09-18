package tornadofx.tests

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.image.ImageView
import javafx.util.StringConverter
import javafx.util.converter.NumberStringConverter
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.testfx.api.FxToolkit
import tornadofx.*
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ControlsTest {

    companion object {
        private fun testView(op: Parent.() -> Unit): View {
            return object : View() {
                override val root = vbox(op = op)
            }
        }
    }

    @Before
    fun setupFX() {
        FxToolkit.registerPrimaryStage()
    }


    // ================================================================
    // text

    @Test fun `text with string literal`() {
        testView {
            val text = text("foo")

            assertEquals("foo", text.text)
            text.text = "bar"
            assertEquals("bar", text.text)
        }
    }

    @Test fun `text with string observable`() {
        testView {
            val stringProperty = SimpleStringProperty("foo")
            val text = text(stringProperty)

            assertEquals("foo", text.text)
            stringProperty.value = "bar"
            assertEquals("bar", text.text)
        }
    }


    // ================================================================
    // label

    @Test fun `label with string literal`() {
        testView {
            val label = label("foo")

            assertEquals("foo", label.text)
            label.text = "bar"
            assertEquals("bar", label.text)
        }
    }

    @Test fun `label with graphic`() {
        testView {
            val label = label(graphic = imageview())

            assertTrue(label.graphic is ImageView)
        }
    }

    @Test fun `label with string observable`() {
        testView {
            val stringProperty = SimpleStringProperty("Hello World!")

            val label1 = label(stringProperty)
            val label2 = label(stringProperty, converter = object : StringConverter<String>() {
                override fun toString(string: String?) = string?.toUpperCase() ?: ""
                override fun fromString(string: String?) = throw NotImplementedError()
            })

            assertEquals("Hello World!", label1.text)
            assertEquals("HELLO WORLD!", label2.text)

            stringProperty.value = "foobar"

            assertEquals("foobar", label1.text)
            assertEquals("FOOBAR", label2.text)
        }
    }

    @Test fun `label with integer observable`() {
        testView {
            val integerProperty = SimpleIntegerProperty(12718)

            val label1 = label(integerProperty)
            val label2 = label(integerProperty, converter = NumberStringConverter(Locale.US))

            assertEquals("12718", label1.text)
            assertEquals(NumberFormat.getNumberInstance(Locale.US).format(12718), label2.text)

            integerProperty.value = 205926523

            assertEquals("205926523", label1.text)
            assertEquals(NumberFormat.getNumberInstance(Locale.US).format(205926523), label2.text)
        }
    }

    @Test fun `label with string and graphic observable`() {
        testView {
            val stringProperty = SimpleStringProperty("foo")
            val graphicProperty = imageview().toProperty<Node>()

            val label = label(stringProperty, graphicProperty)

            assertEquals("foo", label.text)
            assertTrue(label.graphic is ImageView)

            stringProperty.value = "bar"
            graphicProperty.value = null

            assertEquals("bar", label.text)
            assertEquals(null, label.graphic)
        }
    }


    // ================================================================
    // textfield

    @Test fun `textfield with string literal`() {
        testView {
            val textfield = textfield("foo")

            assertEquals("foo", textfield.text)
            textfield.text = "bar"
            assertEquals("bar", textfield.text)
        }
    }

    @Test fun `textfield with string observable`() {
        testView {
            val stringProperty = SimpleStringProperty("Hello World!")

            val textfield1 = textfield(stringProperty)
            val textfield2 = textfield(stringProperty, converter = object : StringConverter<String>() {
                override fun toString(string: String?) = string?.toUpperCase() ?: ""
                override fun fromString(string: String?) = throw NotImplementedError()
            })

            assertEquals("Hello World!", textfield1.text)
            assertEquals("HELLO WORLD!", textfield2.text)

            stringProperty.value = "foobar"

            assertEquals("foobar", textfield1.text)
            assertEquals("FOOBAR", textfield2.text)
        }
    }

    @Test fun `textfield with integer observable`() {
        testView {
            val integerProperty = SimpleIntegerProperty(673223)

            val textfield1 = textfield(integerProperty)
            val textfield2 = textfield(integerProperty, converter = NumberStringConverter(Locale.US))

            assertEquals("673223", textfield1.text) // FIXME This test fails. Actual: 673,223
            assertEquals(NumberFormat.getNumberInstance(Locale.US).format(673223), textfield2.text)

            integerProperty.value = 10876345

            assertEquals("10876345", textfield1.text) // FIXME This test also fails. Actual: 10,876,345
            assertEquals(NumberFormat.getNumberInstance(Locale.US).format(10876345), textfield2.text)
        }
    }

    @Test fun `textfield with double observable`() {
        testView {
            val doubleProperty = SimpleDoubleProperty(3.14)

            val textfield1 = textfield(doubleProperty)
            val textfield2 = textfield(doubleProperty, NumberStringConverter(DecimalFormat("#0.0", DecimalFormatSymbols.getInstance(Locale.ENGLISH))))

            assertEquals("3.14", textfield1.text)
            assertEquals("3.1", textfield2.text)

            doubleProperty.value = 2.71

            assertEquals("2.71", textfield1.text)
            assertEquals("2.7", textfield2.text)
        }
    }


    // ================================================================
    // passwordfield

    @Test fun `passwordfield with string literal`() {
        testView {
            val passwordfield = passwordfield("1234abcd")

            assertEquals("1234abcd", passwordfield.text)
            passwordfield.text = "passwd"
            assertEquals("passwd", passwordfield.text)
        }
    }

    @Test fun `passwordfield with string observable`() {
        testView {
            val stringProperty = SimpleStringProperty("supersecretpassword")
            val passwordfield = passwordfield(stringProperty)

            assertEquals("supersecretpassword", passwordfield.text)
            stringProperty.value = "qwerty1234"
            assertEquals("qwerty1234", passwordfield.text)
        }
    }


    // ================================================================
    // textarea

    @Test fun `textarea with string literal`() {
        testView {
            val textarea = textarea("Lorem ipsum dolor sit amet.")

            assertEquals("Lorem ipsum dolor sit amet.", textarea.text)
            textarea.text = "In sed mi semper, mollis."
            assertEquals("In sed mi semper, mollis.", textarea.text)
        }
    }

    @Test fun `textarea with string observable`() {
        testView {
            val stringProperty = SimpleStringProperty("Ut eget aliquam nisi. Mauris.")

            val textarea1 = textarea(stringProperty)
            val textarea2 = textarea(stringProperty, converter = object : StringConverter<String>() {
                override fun toString(string: String?) = string?.toUpperCase() ?: ""
                override fun fromString(string: String?) = throw NotImplementedError()
            })

            assertEquals("Ut eget aliquam nisi. Mauris.", textarea1.text)
            assertEquals("UT EGET ALIQUAM NISI. MAURIS.", textarea2.text)

            stringProperty.value = "Nullam dapibus ipsum risus, nec."

            assertEquals("Nullam dapibus ipsum risus, nec.", textarea1.text)
            assertEquals("NULLAM DAPIBUS IPSUM RISUS, NEC.", textarea2.text)
        }
    }

    @Test fun `textarea with integer observable`() {
        testView {
            val integerProperty = SimpleIntegerProperty(445582)
            val textarea = textarea(integerProperty, converter = NumberStringConverter(Locale.US))

            assertEquals(NumberFormat.getNumberInstance(Locale.US).format(445582), textarea.text)
            integerProperty.value = 23456122
            assertEquals(NumberFormat.getNumberInstance(Locale.US).format(23456122), textarea.text)
        }
    }


    // ================================================================
    // datepicker

    @Test fun `datepicker with date observable`() {
        testView {
            val dateProperty = SimpleObjectProperty(LocalDate.of(2016, 2, 15))
            val datepicker = datepicker(dateProperty)

            assertEquals(LocalDate.of(2016, 2, 15), datepicker.value)
            dateProperty.value = LocalDate.of(2017, 11, 28)
            assertEquals(LocalDate.of(2017, 11, 28), datepicker.value)
        }
    }


    // ================================================================
    // progressindicator

    @Test fun `progressindicator with double observable`() {
        testView {
            val doubleProperty = SimpleDoubleProperty(42.5)
            val progressindicator = progressindicator(doubleProperty)

            Assert.assertEquals(42.5, progressindicator.progress, 10e-5)
            doubleProperty.value = 24.5
            Assert.assertEquals(24.5, progressindicator.progress, 10e-5)
        }
    }


    // ================================================================
    // progressbar

    @Test fun `progressbar with double literal`() {
        testView {
            val progressbar = progressbar(0.64)

            Assert.assertEquals(0.64, progressbar.progress, 10e-5)
            progressbar.progress = 3.14
            Assert.assertEquals(3.14, progressbar.progress, 10e-5)
        }
    }

    @Test fun `progressbar with double observable`() {
        testView {
            val doubleProperty = SimpleIntegerProperty(32)
            val progressbar = progressbar(doubleProperty)

            Assert.assertEquals(32.0, progressbar.progress, 10e-5)
            doubleProperty.value = 75
            Assert.assertEquals(75.0, progressbar.progress, 10e-5)
        }
    }


    // ================================================================
    // slider

    @Test fun `slider with double literals`() {
        testView {
            slider(24..42, 33, Orientation.VERTICAL).also {
                Assert.assertEquals(24.0, it.min, 10e-5)
                Assert.assertEquals(42.0, it.max, 10e-5)
                Assert.assertEquals(33.0, it.value, 10e-5)
                assertEquals(Orientation.VERTICAL, it.orientation)
            }

            slider(0.24, 0.42, 0.33, Orientation.HORIZONTAL).also {
                Assert.assertEquals(0.24, it.min, 10e-5)
                Assert.assertEquals(0.42, it.max, 10e-5)
                Assert.assertEquals(0.33, it.value, 10e-5)
                assertEquals(Orientation.HORIZONTAL, it.orientation)
            }
        }
    }


    // ================================================================
    // imageview

    @Test fun testImageView() {
        testView {
            imageview().also {
                assertNull(it.image)
            }

            imageview("/tornadofx/tests/person.png").also {
                assertNotNull(it.image)
            }

            val property = SimpleStringProperty()
            val imageview = imageview(property)

            assertNull(imageview.image)
            property.value = "/tornadofx/tests/person.png"
            assertNotNull(imageview.image)
        }
    }
}
