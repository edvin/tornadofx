package tornadofx.tests

import javafx.beans.binding.BooleanExpression
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.collections.FXCollections
import javafx.scene.control.Label
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.testfx.api.FxToolkit
import tornadofx.*
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BindingTest {

    @Before
    fun setupFX() {
        FxToolkit.registerPrimaryStage()
    }

    @Test
    fun testBindingFormat() {
        val property = SimpleDoubleProperty(Math.PI)
        val label = Label()
        label.bind(property, format = DecimalFormat("#0.0", DecimalFormatSymbols.getInstance(Locale.ENGLISH)))

        assertEquals("3.1", label.text)
    }

    @Test
    fun observableListBinding() {
        val elements = FXCollections.observableArrayList("Hello", "World")
        val binding = stringBinding(elements, elements) { this.joinToString(" ") }
        val uiElement = Label().apply { bind(binding) }

        assertEquals("Hello World", uiElement.text)

        elements.setAll("Hello", "Changed")
        assertEquals("Hello Changed", uiElement.text)
    }

    @Test
    fun observableMapBinding() {
        val map = FXCollections.observableHashMap<String, Int>()
        val list = observableList<Char>()
        list.bind(map) { k, v -> k[v] }

        map["0123456"] = 3
        assertEquals(list, listOf('3'))

        map["0123456"] = 4
        assertEquals(list, listOf('4'))

        map["abcdefg"] = 3
        Assert.assertThat(list, containsInAnyOrder('d', '4'))
    }

    @Test
    fun nestedBinding() {
        val father = Person("Mr Father", 50)
        val stepFather = Person("Mr Step Father", 40)
        val child = Person("Firstborn Child", 18)
        val fatherName = child.parentProperty().select(Person::nameProperty)

        child.parent = father
        assertEquals("Mr Father", fatherName.value)

        fatherName.value = "Mister Father"
        assertEquals("Mister Father", father.name)

        child.parent = stepFather
        assertEquals("Mr Step Father", fatherName.value)
    }

    @Test
    fun booleanBinding() {
        val myList = FXCollections.observableArrayList<String>()
        val complete = booleanBinding(myList) { isNotEmpty() }

        assertFalse(complete.value)

        myList.add("One")
        assertTrue(complete.value)
    }

    @Test
    fun booleanListBinding() {
        val myList = FXCollections.observableArrayList<BooleanExpression>()
        val complete = booleanListBinding(myList) { this }

        assertFalse(complete.value)

        myList.add(SimpleBooleanProperty(true))
        assertTrue(complete.value)

        myList.add(SimpleBooleanProperty(false))
        assertFalse(complete.value)
    }
}
