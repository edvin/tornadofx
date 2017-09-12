package tornadofx.tests

import javafx.beans.binding.DoubleBinding
import javafx.beans.binding.FloatBinding
import javafx.beans.binding.IntegerBinding
import javafx.beans.binding.LongBinding
import javafx.beans.property.*
import javafx.collections.FXCollections
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Test
import tornadofx.*
import kotlin.test.assertEquals

class PropertiesTest {
    @Test
    fun primitiveOnChange() {
        val d = SimpleDoubleProperty()
        // Ensure getting the value does not throw an NPE
        d.onChange { assert((it + 7) is Double) }

        d.value = 100.0
        d.value = Double.POSITIVE_INFINITY
        d.value = Double.NaN
        d.value = null
    }

    @Test
    fun property_delegation() {
        // given:
        val fixture = object {
            val nameProperty = SimpleStringProperty("Alice")
            var name by nameProperty

            val ageProperty = SimpleDoubleProperty(1.0)
            var age by ageProperty

            val dirtyProperty = SimpleBooleanProperty(false)
            var dirty by dirtyProperty
        }
        // expect:
        assertEquals("Alice", fixture.name)
        assertEquals("Alice", fixture.nameProperty.value)

        assertEquals(1.0, fixture.age)
        assertEquals(1.0, fixture.ageProperty.value)

        assertEquals(false, fixture.dirty)
        assertEquals(false, fixture.dirtyProperty.value)

        // when:
        fixture.name = "Bob"
        fixture.age = 100.0
        fixture.dirty = true

        // expect:
        assertEquals("Bob", fixture.name)
        assertEquals("Bob", fixture.nameProperty.value)

        assertEquals(100.0, fixture.age)
        assertEquals(100.0, fixture.ageProperty.value)

        assertEquals(true, fixture.dirty)
        assertEquals(true, fixture.dirtyProperty.value)
    }

    @Test
    fun property_get_should_read_value() {
        // given:
        val fixture = object {
            val string by property<String>()
            val integer: Int? by property()
            val stringDefault by property("foo")
            val integerDefault by property(42)
        }

        // expect:
        assertEquals(null, fixture.string)
        assertEquals(null, fixture.integer)
        assertEquals("foo", fixture.stringDefault)
        assertEquals(42, fixture.integerDefault)
    }

    @Test
    fun property_set_should_write_value() {
        // given:
        val fixture = object {
            var string by property<String>()
            var integer: Int? by property()
            var stringDefault by property("foo")
            var integerDefault by property(42)
        }

        // when:
        fixture.string = "foo"
        fixture.integer = 42

        // then:
        assertEquals("foo", fixture.string)
        assertEquals("foo", fixture.stringDefault)
        assertEquals(42, fixture.integer)
        assertEquals(42, fixture.integerDefault)
    }

    class TestClass {
        var myProperty: String by singleAssign()
    }

    @Test
    fun failNoAssignment() {
        val instance = TestClass()
        var failed = false

        try {
            instance.myProperty
        } catch (e: Exception) {
            failed = true
        }
        assertTrue(failed)
    }

    @Test
    fun succeedAssignment() {
        val instance = TestClass()
        instance.myProperty = "foo"
        instance.myProperty
    }

    @Test
    fun failDoubleAssignment() {

        val instance = TestClass()
        var failed = false
        instance.myProperty = "foo"

        try {
            instance.myProperty = "bar"
        } catch (e: Exception) {
            failed = true
        }
        assertTrue(failed)
    }

    @Test
    fun pojoWritableObservable() {
        val person = JavaPerson()
        person.id = 1
        person.name = "John"

        val idObservable = person.observable(JavaPerson::getId, JavaPerson::setId)
        val nameObservable = person.observable<String>("name")

        idObservable.value = 44
        nameObservable.value = "Doe"

        Assert.assertEquals(44, person.id)
        Assert.assertEquals("Doe", person.name)

        person.id = 5
        Assert.assertEquals(5, idObservable.value)
    }

    @Test
    fun pojoWritableObservableGetterOnly() {
        val person = JavaPerson()
        person.id = 1
        person.name = "John"

        val idObservable = person.observable(JavaPerson::getId)
        val nameObservable = person.observable<String>("name")
        val idBinding = idObservable.integerBinding { idObservable.value }

        idObservable.value = 44
        nameObservable.value = "Doe"
        Assert.assertEquals(44, idBinding.value)
        Assert.assertEquals(44, person.id)
        Assert.assertEquals("Doe", person.name)

        person.id = 5
        // property change events on the pojo are propogated
        Assert.assertEquals(5, idBinding.value)
        Assert.assertEquals(5, idObservable.value)
    }

    @Test fun property_on_change() {
        var called = false
        val property = SimpleStringProperty("Hello World")
        property.onChange { called = true }
        property.value = "Changed"
        assertTrue(called)
    }

    @Test fun assign_if_null() {
        val has = SimpleObjectProperty("Hello")
        has.assignIfNull { "World" }
        assertEquals(has.value, "Hello")
        val hasNot = SimpleObjectProperty<String>()
        hasNot.assignIfNull { "World" }
        assertEquals(hasNot.value, "World")
    }

    @Test fun testDoubleToProperty() {
        val property = 5.0.toProperty()
        Assert.assertTrue(property is DoubleProperty)
        Assert.assertEquals(5.0, property.get(), .001)
    }

    @Test fun testFloatToProperty() {
        val property = 5.0f.toProperty()
        Assert.assertTrue(property is FloatProperty)
        Assert.assertEquals(5.0f, property.get(), .001f)
    }

    @Test fun testLongToProperty() {
        val property = 5L.toProperty()
        Assert.assertTrue(property is LongProperty)
        Assert.assertEquals(5, property.get())
    }

    @Test fun testIntToProperty() {
        val property = 5.toProperty()
        Assert.assertTrue(property is IntegerProperty)
        Assert.assertEquals(5, property.get())
    }

    @Test fun testBooleanToProperty() {
        val property = true.toProperty()
        Assert.assertTrue(property is BooleanProperty)
        Assert.assertTrue(property.get())
    }

    @Test fun testStringToProperty() {
        val property = "Hello World!".toProperty()
        Assert.assertTrue(property is StringProperty)
        Assert.assertEquals("Hello World!", property.get())
    }

    @Test fun testDoubleExpressionPlusNumber() {
        val property = 0.0.toProperty()

        val binding = property + 5
        Assert.assertEquals(5.0, binding.get(), .001)

        property.value -= 5f
        Assert.assertEquals(0.0, binding.get(), .001)
    }

    @Test fun testDoubleExpressionPlusNumberProperty() {
        val property1 = 0.0.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 + property2
        Assert.assertEquals(5.0, binding.get(), .001)

        property1.value -= 10
        Assert.assertEquals(-5.0, binding.get(), .001)

        property2.value = 0
        Assert.assertEquals(-10.0, binding.get(), .001)
    }

    @Test fun testDoublePropertyPlusAssignNumber() {
        val property = 0.0.toProperty()
        property += 5

        Assert.assertEquals(5.0, property.get(), .001)
    }

    @Test fun testDoublePropertyPlusAssignNumberProperty() {
        val property1 = 0.0.toProperty()
        val property2 = 5.toProperty()

        property1 += property2
        Assert.assertEquals(5.0, property1.get(), .001)
    }

    @Test fun testDoubleExpressionMinusNumber() {
        val property = 0.0.toProperty()

        val binding = property - 5
        Assert.assertEquals(-5.0, binding.get(), .001)

        property.value -= 5f
        Assert.assertEquals(-10.0, binding.get(), .001)
    }

    @Test fun testDoubleExpressionMinusNumberProperty() {
        val property1 = 0.0.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 - property2
        Assert.assertEquals(-5.0, binding.get(), .001)

        property1.value -= 10
        Assert.assertEquals(-15.0, binding.get(), .001)

        property2.value = 0
        Assert.assertEquals(-10.0, binding.get(), .001)
    }

    @Test fun testDoublePropertyMinusAssignNumber() {
        val property = 0.0.toProperty()
        property -= 5

        Assert.assertEquals(-5.0, property.get(), .001)
    }

    @Test fun testDoublePropertyMinusAssignNumberProperty() {
        val property1 = 0.0.toProperty()
        val property2 = 5.toProperty()

        property1 -= property2
        Assert.assertEquals(-5.0, property1.get(), .001)
    }

    @Test fun testDoublePropertyUnaryMinus() {
        val property = 1.0.toProperty()

        val binding = -property
        Assert.assertEquals(-1.0, binding.get(), .001)

        property += 1
        Assert.assertEquals(-2.0, binding.get(), .001)
    }

    @Test fun testDoubleExpressionTimesNumber() {
        val property = 2.0.toProperty()

        val binding = property * 5
        Assert.assertEquals(10.0, binding.get(), .001)

        property.value = 5.0
        Assert.assertEquals(25.0, binding.get(), .001)
    }

    @Test fun testDoubleExpressionTimesNumberProperty() {
        val property1 = 2.0.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 * property2
        Assert.assertEquals(10.0, binding.get(), .001)

        property1.value = 5.0
        Assert.assertEquals(25.0, binding.get(), .001)

        property2.value = 0
        Assert.assertEquals(0.0, binding.get(), .001)
    }

    @Test fun testDoublePropertyTimesAssignNumber() {
        val property = 1.0.toProperty()
        property *= 5

        Assert.assertEquals(5.0, property.get(), .001)
    }

    @Test fun testDoublePropertyTimesAssignNumberProperty() {
        val property1 = 1.0.toProperty()
        val property2 = 5.toProperty()

        property1 *= property2
        Assert.assertEquals(5.0, property1.get(), .001)
    }

    @Test fun testDoubleExpressionDivNumber() {
        val property = 5.0.toProperty()

        val binding = property / 5
        Assert.assertEquals(1.0, binding.get(), .001)

        property.value = 10.0
        Assert.assertEquals(2.0, binding.get(), .001)
    }

    @Test fun testDoubleExpressionDivNumberProperty() {
        val property1 = 5.0.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 / property2
        Assert.assertEquals(1.0, binding.get(), .001)

        property1.value = 10.0
        Assert.assertEquals(2.0, binding.get(), .001)

        property2.value = 20
        Assert.assertEquals(0.5, binding.get(), .001)
    }

    @Test fun testDoublePropertyDivAssignNumber() {
        val property = 5.0.toProperty()
        property /= 5

        Assert.assertEquals(1.0, property.get(), .001)
    }

    @Test fun testDoublePropertyDivAssignNumberProperty() {
        val property1 = 5.0.toProperty()
        val property2 = 5.toProperty()

        property1 /= property2
        Assert.assertEquals(1.0, property1.get(), .001)
    }

    @Test fun testDoubleExpressionRemNumber() {
        val property = 6.0.toProperty()

        val binding = property % 5
        Assert.assertEquals(1.0, binding.get(), .001)

        property.value = 12.0
        Assert.assertEquals(2.0, binding.get(), .001)
    }

    @Test fun testDoubleExpressionRemNumberProperty() {
        val property1 = 6.0.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 % property2
        Assert.assertEquals(1.0, binding.get(), .001)

        property1.value = 12.0
        Assert.assertEquals(2.0, binding.get(), .001)

        property2.value = 11
        Assert.assertEquals(1.0, binding.get(), .001)
    }

    @Test fun testDoublePropertyRemAssignNumber() {
        val property = 6.0.toProperty()
        property %= 5

        Assert.assertEquals(1.0, property.get(), .001)
    }

    @Test fun testDoublePropertyRemAssignNumberProperty() {
        val property1 = 6.0.toProperty()
        val property2 = 5.toProperty()

        property1 %= property2
        Assert.assertEquals(1.0, property1.get(), .001)
    }

    @Test fun testDoublePropertyCompareToNumber() {
        val property = 5.0.toProperty()

        Assert.assertTrue(property > 4)
        Assert.assertTrue(property >= 5)
        Assert.assertTrue(property >= 4)
        Assert.assertTrue(property <= 5)
        Assert.assertTrue(property <= 5)
        Assert.assertTrue(property <= 6)
        Assert.assertTrue(property < 6)

        Assert.assertFalse(property > 6)
        Assert.assertFalse(property >= 6)
        Assert.assertFalse(property <= 4)
        Assert.assertFalse(property < 4)
    }

    @Test fun testDoublePropertyCompareToNumberProperty() {
        val property = 5.0.toProperty()


        Assert.assertTrue(property > 4.toProperty())
        Assert.assertTrue(property >= 5.toProperty())
        Assert.assertTrue(property >= 4.toProperty())
        Assert.assertTrue(property <= 5.toProperty())
        Assert.assertTrue(property <= 5.toProperty())
        Assert.assertTrue(property <= 6.toProperty())
        Assert.assertTrue(property < 6.toProperty())

        Assert.assertFalse(property > 6.toProperty())
        Assert.assertFalse(property >= 6.toProperty())
        Assert.assertFalse(property <= 4.toProperty())
        Assert.assertFalse(property < 4.toProperty())
    }

    @Test fun testFloatExpressionPlusNumber() {
        val property = 0.0f.toProperty()

        val binding = property + 5
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(5.0f, binding.get(), .001f)

        property.value -= 5f
        Assert.assertEquals(0.0f, binding.get(), .001f)
    }

    @Test fun testFloatExpressionPlusDouble() {
        val property = 0.0f.toProperty()

        val binding = property + 5.0
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(5.0, binding.get(), .001)

        property.value -= 5f
        Assert.assertEquals(0.0, binding.get(), .001)
    }

    @Test fun testFloatExpressionPlusNumberProperty() {
        val property1 = 0.0f.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 + property2
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(5.0f, binding.get(), .001f)

        property1.value -= 10f
        Assert.assertEquals(-5.0f, binding.get(), .001f)

        property2.value = 0
        Assert.assertEquals(-10.0f, binding.get(), .001f)
    }

    @Test fun testFloatExpressionPlusDoubleProperty() {
        val property1 = 0.0f.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 + property2
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(5.0, binding.get(), .001)

        property1.value -= 10f
        Assert.assertEquals(-5.0, binding.get(), .001)

        property2.value = 0.0
        Assert.assertEquals(-10.0, binding.get(), .001)
    }

    @Test fun testFloatPropertyPlusAssignNumber() {
        val property = 0.0f.toProperty()
        property += 5

        Assert.assertEquals(5.0f, property.get(), .001f)
    }

    @Test fun testFloatPropertyPlusAssignNumberProperty() {
        val property1 = 0.0f.toProperty()
        val property2 = 5.toProperty()

        property1 += property2
        Assert.assertEquals(5.0f, property1.get(), .001f)
    }

    @Test fun testFloatExpressionMinusNumber() {
        val property = 0.0f.toProperty()

        val binding = property - 5
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(-5.0f, binding.get(), .001f)

        property.value -= 5f
        Assert.assertEquals(-10.0f, binding.get(), .001f)
    }

    @Test fun testFloatExpressionMinusDouble() {
        val property = 0.0f.toProperty()

        val binding = property - 5.0
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(-5.0, binding.get(), .001)

        property.value -= 5f
        Assert.assertEquals(-10.0, binding.get(), .001)
    }

    @Test fun testFloatExpressionMinusNumberProperty() {
        val property1 = 0.0f.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 - property2
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(-5.0f, binding.get(), .001f)

        property1.value -= 10f
        Assert.assertEquals(-15.0f, binding.get(), .001f)

        property2.value = 0
        Assert.assertEquals(-10.0f, binding.get(), .001f)
    }

    @Test fun testFloatExpressionMinusDoubleProperty() {
        val property1 = 0.0f.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 - property2
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(-5.0, binding.get(), .001)

        property1.value -= 10f
        Assert.assertEquals(-15.0, binding.get(), .001)

        property2.value = 0.0
        Assert.assertEquals(-10.0, binding.get(), .001)
    }

    @Test fun testFloatPropertyMinusAssignNumber() {
        val property = 0.0f.toProperty()
        property -= 5

        Assert.assertEquals(-5.0f, property.get(), .001f)
    }

    @Test fun testFloatPropertyMinusAssignNumberProperty() {
        val property1 = 0.0f.toProperty()
        val property2 = 5.toProperty()

        property1 -= property2
        Assert.assertEquals(-5.0f, property1.get(), .001f)
    }

    @Test fun testFloatPropertyUnaryMinus() {
        val property = 1.0f.toProperty()

        val binding = -property
        Assert.assertEquals(-1.0f, binding.get(), .001f)

        property += 1
        Assert.assertEquals(-2.0f, binding.get(), .001f)
    }

    @Test fun testFloatExpressionTimesNumber() {
        val property = 2.0f.toProperty()

        val binding = property * 5
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(10.0f, binding.get(), .001f)

        property.value = 5f
        Assert.assertEquals(25.0f, binding.get(), .001f)
    }

    @Test fun testFloatExpressionTimesDouble() {
        val property = 2.0f.toProperty()

        val binding = property * 5.0
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(10.0, binding.get(), .001)

        property.value = 5f
        Assert.assertEquals(25.0, binding.get(), .001)
    }

    @Test fun testFloatExpressionTimesNumberProperty() {
        val property1 = 2.0f.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 * property2
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(10.0f, binding.get(), .001f)

        property1.value = 10f
        Assert.assertEquals(50.0f, binding.get(), .001f)

        property2.value = 0
        Assert.assertEquals(0.0f, binding.get(), .001f)
    }

    @Test fun testFloatExpressionTimesDoubleProperty() {
        val property1 = 2.0f.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 * property2
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(10.0, binding.get(), .001)

        property1.value = 10f
        Assert.assertEquals(50.0, binding.get(), .001)

        property2.value = 0.0
        Assert.assertEquals(0.0, binding.get(), .001)
    }

    @Test fun testFloatPropertyTimesAssignNumber() {
        val property = 1.0f.toProperty()
        property *= 5

        Assert.assertEquals(5.0f, property.get(), .001f)
    }

    @Test fun testFloatPropertyTimesAssignNumberProperty() {
        val property1 = 1.0f.toProperty()
        val property2 = 5.toProperty()

        property1 *= property2
        Assert.assertEquals(5.0f, property1.get(), .001f)
    }

    @Test fun testFloatExpressionDivNumber() {
        val property = 5.0f.toProperty()

        val binding = property / 5
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(1.0f, binding.get(), .001f)

        property.value = 10f
        Assert.assertEquals(2.0f, binding.get(), .001f)
    }

    @Test fun testFloatExpressionDivDouble() {
        val property = 5.0f.toProperty()

        val binding = property / 5.0
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(1.0, binding.get(), .001)

        property.value = 10f
        Assert.assertEquals(2.0, binding.get(), .001)
    }

    @Test fun testFloatExpressionDivNumberProperty() {
        val property1 = 5.0f.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 / property2
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(1.0f, binding.get(), .001f)

        property1.value = 10f
        Assert.assertEquals(2.0f, binding.get(), .001f)

        property2.value = 20
        Assert.assertEquals(0.5f, binding.get(), .001f)
    }

    @Test fun testFloatExpressionDivDoubleProperty() {
        val property1 = 5.0f.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 / property2
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(1.0, binding.get(), .001)

        property1.value = 10f
        Assert.assertEquals(2.0, binding.get(), .001)

        property2.value = 20.0
        Assert.assertEquals(0.5, binding.get(), .001)
    }

    @Test fun testFloatPropertyDivAssignNumber() {
        val property = 5.0f.toProperty()
        property /= 5

        Assert.assertEquals(1.0f, property.get(), .001f)
    }

    @Test fun testFloatPropertyDivAssignNumberProperty() {
        val property1 = 5.0f.toProperty()
        val property2 = 5.toProperty()

        property1 /= property2
        Assert.assertEquals(1.0f, property1.get(), .001f)
    }

    @Test fun testFloatExpressionRemNumber() {
        val property = 6.0f.toProperty()

        val binding = property % 5
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(1.0f, binding.get(), .001f)

        property.value = 12.0f
        Assert.assertEquals(2.0f, binding.get(), .001f)
    }

    @Test fun testFloatExpressionRemDouble() {
        val property = 6.0f.toProperty()

        val binding = property % 5.0
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(1.0, binding.get(), .001)

        property.value = 12.0f
        Assert.assertEquals(2.0, binding.get(), .001)
    }

    @Test fun testFloatExpressionRemNumberProperty() {
        val property1 = 6.0f.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 % property2
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(1.0f, binding.get(), .001f)

        property1.value = 12f
        Assert.assertEquals(2.0f, binding.get(), .001f)

        property2.value = 11
        Assert.assertEquals(1.0f, binding.get(), .001f)
    }

    @Test fun testFloatExpressionRemDoubleProperty() {
        val property1 = 6.0f.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 % property2
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(1.0, binding.get(), .001)

        property1.value = 12f
        Assert.assertEquals(2.0, binding.get(), .001)

        property2.value = 11.0
        Assert.assertEquals(1.0, binding.get(), .001)
    }

    @Test fun testFloatPropertyRemAssignNumber() {
        val property = 6.0f.toProperty()
        property %= 5

        Assert.assertEquals(1.0f, property.get(), .001f)
    }

    @Test fun testFloatPropertyRemAssignNumberProperty() {
        val property1 = 6.0f.toProperty()
        val property2 = 5.toProperty()

        property1 %= property2
        Assert.assertEquals(1.0f, property1.get(), .001f)
    }

    @Test fun testFloatPropertyCompareToNumber() {
        val property = 5.0f.toProperty()

        Assert.assertTrue(property > 4)
        Assert.assertTrue(property >= 5)
        Assert.assertTrue(property >= 4)
        Assert.assertTrue(property <= 5)
        Assert.assertTrue(property <= 5)
        Assert.assertTrue(property <= 6)
        Assert.assertTrue(property < 6)

        Assert.assertFalse(property > 6)
        Assert.assertFalse(property >= 6)
        Assert.assertFalse(property <= 4)
        Assert.assertFalse(property < 4)
    }

    @Test fun testFloatPropertyCompareToNumberProperty() {
        val property = 5.0f.toProperty()


        Assert.assertTrue(property > 4.toProperty())
        Assert.assertTrue(property >= 5.toProperty())
        Assert.assertTrue(property >= 4.toProperty())
        Assert.assertTrue(property <= 5.toProperty())
        Assert.assertTrue(property <= 5.toProperty())
        Assert.assertTrue(property <= 6.toProperty())
        Assert.assertTrue(property < 6.toProperty())

        Assert.assertFalse(property > 6.toProperty())
        Assert.assertFalse(property >= 6.toProperty())
        Assert.assertFalse(property <= 4.toProperty())
        Assert.assertFalse(property < 4.toProperty())
    }

    @Test fun testIntegerExpressionPlusInt() {
        val property = 0.toProperty()

        val binding = property + 5
        Assert.assertTrue(binding is IntegerBinding)
        Assert.assertEquals(5, binding.get())

        property.value -= 5
        Assert.assertEquals(0, binding.get())
    }

    @Test fun testIntegerExpressionPlusLong() {
        val property = 0.toProperty()

        val binding = property + 5L
        Assert.assertTrue(binding is LongBinding)
        Assert.assertEquals(5L, binding.get())

        property.value -= 5
        Assert.assertEquals(0, binding.get())
    }

    @Test fun testIntegerExpressionPlusFloat() {
        val property = 0.toProperty()

        val binding = property + 5f
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(5f, binding.get(), .001f)

        property.value -= 5
        Assert.assertEquals(0f, binding.get(), .001f)
    }

    @Test fun testIntegerExpressionPlusDouble() {
        val property = 0.toProperty()

        val binding = property + 5.0
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(5.0, binding.get(), .001)

        property.value -= 5
        Assert.assertEquals(0.0, binding.get(), .001)
    }

    @Test fun testIntegerExpressionPlusIntegerProperty() {
        val property1 = 0.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 + property2
        Assert.assertTrue(binding is IntegerBinding)
        Assert.assertEquals(5, binding.get())

        property1.value -= 10
        Assert.assertEquals(-5, binding.get())

        property2.value = 0
        Assert.assertEquals(-10, binding.get())
    }

    @Test fun testIntegerExpressionPlusLongProperty() {
        val property1 = 0.toProperty()
        val property2 = 5L.toProperty()

        val binding = property1 + property2
        Assert.assertTrue(binding is LongBinding)
        Assert.assertEquals(5L, binding.get())

        property1.value -= 10
        Assert.assertEquals(-5L, binding.get())

        property2.value = 0L
        Assert.assertEquals(-10L, binding.get())
    }

    @Test fun testIntegerExpressionPlusFloatProperty() {
        val property1 = 0.toProperty()
        val property2 = 5f.toProperty()

        val binding = property1 + property2
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(5f, binding.get(), .001f)

        property1.value -= 10
        Assert.assertEquals(-5f, binding.get(), .001f)

        property2.value = 0f
        Assert.assertEquals(-10f, binding.get(), .001f)
    }

    @Test fun testIntegerExpressionPlusDoubleProperty() {
        val property1 = 0.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 + property2
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(5.0, binding.get(), .001)

        property1.value -= 10
        Assert.assertEquals(-5.0, binding.get(), .001)

        property2.value = 0.0
        Assert.assertEquals(-10.0, binding.get(), .001)
    }

    @Test fun testIntegerPropertyPlusAssignNumber() {
        val property = 0.toProperty()
        property += 5

        Assert.assertEquals(5, property.get())
    }

    @Test fun testIntegerPropertyPlusAssignNumberProperty() {
        val property1 = 0.toProperty()
        val property2 = 5.toProperty()

        property1 += property2
        Assert.assertEquals(5, property1.get())
    }

    @Test fun testIntegerExpressionMinusInt() {
        val property = 0.toProperty()

        val binding = property - 5
        Assert.assertTrue(binding is IntegerBinding)
        Assert.assertEquals(-5, binding.get())

        property.value -= 5
        Assert.assertEquals(-10, binding.get())
    }

    @Test fun testIntegerExpressionMinusLong() {
        val property = 0.toProperty()

        val binding = property - 5L
        Assert.assertTrue(binding is LongBinding)
        Assert.assertEquals(-5L, binding.get())

        property.value -= 5
        Assert.assertEquals(-10L, binding.get())
    }

    @Test fun testIntegerExpressionMinusFloat() {
        val property = 0.toProperty()

        val binding = property - 5f
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(-5f, binding.get(), .001f)

        property.value -= 5
        Assert.assertEquals(-10f, binding.get(), .001f)
    }

    @Test fun testIntegerExpressionMinusDouble() {
        val property = 0.toProperty()

        val binding = property - 5.0
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(-5.0, binding.get(), .001)

        property.value -= 5
        Assert.assertEquals(-10.0, binding.get(), .001)
    }

    @Test fun testIntegerExpressionMinusIntegerProperty() {
        val property1 = 0.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 - property2
        Assert.assertTrue(binding is IntegerBinding)
        Assert.assertEquals(-5, binding.get())

        property1.value -= 10
        Assert.assertEquals(-15, binding.get())

        property2.value = 0
        Assert.assertEquals(-10, binding.get())
    }

    @Test fun testIntegerExpressionMinusLongProperty() {
        val property1 = 0.toProperty()
        val property2 = 5L.toProperty()

        val binding = property1 - property2
        Assert.assertTrue(binding is LongBinding)
        Assert.assertEquals(-5L, binding.get())

        property1.value -= 10
        Assert.assertEquals(-15L, binding.get())

        property2.value = 0L
        Assert.assertEquals(-10L, binding.get())
    }

    @Test fun testIntegerExpressionMinusFloatProperty() {
        val property1 = 0.toProperty()
        val property2 = 5f.toProperty()

        val binding = property1 - property2
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(-5f, binding.get(), .001f)

        property1.value -= 10
        Assert.assertEquals(-15f, binding.get(), .001f)

        property2.value = 0f
        Assert.assertEquals(-10f, binding.get(), .001f)
    }

    @Test fun testIntegerExpressionMinusDoubleProperty() {
        val property1 = 0.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 - property2
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(-5.0, binding.get(), .001)

        property1.value -= 10
        Assert.assertEquals(-15.0, binding.get(), .001)

        property2.value = 0.0
        Assert.assertEquals(-10.0, binding.get(), .001)
    }

    @Test fun testIntegerPropertyMinusAssignNumber() {
        val property = 0.toProperty()
        property -= 5

        Assert.assertEquals(-5, property.get())
    }

    @Test fun testIntegerPropertyMinusAssignNumberProperty() {
        val property1 = 0.toProperty()
        val property2 = 5.toProperty()

        property1 -= property2
        Assert.assertEquals(-5, property1.get())
    }

    @Test fun testIntegerPropertyUnaryMinus() {
        val property = 1.toProperty()

        val binding = -property
        Assert.assertEquals(-1, binding.get())

        property += 1
        Assert.assertEquals(-2, binding.get())
    }

    @Test fun testIntegerExpressionTimesInt() {
        val property = 2.toProperty()

        val binding = property * 5
        Assert.assertTrue(binding is IntegerBinding)
        Assert.assertEquals(10, binding.get())

        property.value = 5
        Assert.assertEquals(25, binding.get())
    }

    @Test fun testIntegerExpressionTimesLong() {
        val property = 2.toProperty()

        val binding = property * 5L
        Assert.assertTrue(binding is LongBinding)
        Assert.assertEquals(10L, binding.get())

        property.value = 5
        Assert.assertEquals(25L, binding.get())
    }

    @Test fun testIntegerExpressionTimesFloat() {
        val property = 2.toProperty()

        val binding = property * 5f
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(10f, binding.get(), .001f)

        property.value = 5
        Assert.assertEquals(25f, binding.get(), .001f)
    }

    @Test fun testIntegerExpressionTimesDouble() {
        val property = 2.toProperty()

        val binding = property * 5.0
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(10.0, binding.get(), .001)

        property.value = 5
        Assert.assertEquals(25.0, binding.get(), .001)
    }

    @Test fun testIntegerExpressionTimesIntegerProperty() {
        val property1 = 2.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 * property2
        Assert.assertTrue(binding is IntegerBinding)
        Assert.assertEquals(10, binding.get())

        property1.value = 10
        Assert.assertEquals(50, binding.get())

        property2.value = 0
        Assert.assertEquals(0, binding.get())
    }

    @Test fun testIntegerExpressionTimesLongProperty() {
        val property1 = 2.toProperty()
        val property2 = 5L.toProperty()

        val binding = property1 * property2
        Assert.assertTrue(binding is LongBinding)
        Assert.assertEquals(10L, binding.get())

        property1.value = 10
        Assert.assertEquals(50L, binding.get())

        property2.value = 0L
        Assert.assertEquals(0L, binding.get())
    }

    @Test fun testIntegerExpressionTimesFloatProperty() {
        val property1 = 2.toProperty()
        val property2 = 5f.toProperty()

        val binding = property1 * property2
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(10f, binding.get(), .001f)

        property1.value = 10
        Assert.assertEquals(50f, binding.get(), .001f)

        property2.value = 0f
        Assert.assertEquals(0f, binding.get(), .001f)
    }

    @Test fun testIntegerExpressionTimesDoubleProperty() {
        val property1 = 2.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 * property2
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(10.0, binding.get(), .001)

        property1.value = 10
        Assert.assertEquals(50.0, binding.get(), .001)

        property2.value = 0.0
        Assert.assertEquals(0.0, binding.get(), .001)
    }

    @Test fun testIntegerPropertyTimesAssignNumber() {
        val property = 1.toProperty()
        property *= 5

        Assert.assertEquals(5, property.get())
    }

    @Test fun testIntegerPropertyTimesAssignNumberProperty() {
        val property1 = 1.toProperty()
        val property2 = 5.toProperty()

        property1 *= property2
        Assert.assertEquals(5, property1.get())
    }

    @Test fun testIntegerExpressionDivInt() {
        val property = 10.toProperty()

        val binding = property / 5
        Assert.assertTrue(binding is IntegerBinding)
        Assert.assertEquals(2, binding.get())

        property.value = 20
        Assert.assertEquals(4, binding.get())
    }

    @Test fun testIntegerExpressionDivLong() {
        val property = 10.toProperty()

        val binding = property / 5L
        Assert.assertTrue(binding is LongBinding)
        Assert.assertEquals(2L, binding.get())

        property.value = 20
        Assert.assertEquals(4L, binding.get())
    }

    @Test fun testIntegerExpressionDivFloat() {
        val property = 10.toProperty()

        val binding = property / 5f
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(2f, binding.get(), .001f)

        property.value = 20
        Assert.assertEquals(4f, binding.get(), .001f)
    }

    @Test fun testIntegerExpressionDivDouble() {
        val property = 10.toProperty()

        val binding = property / 5.0
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(2.0, binding.get(), .001)

        property.value = 20
        Assert.assertEquals(4.0, binding.get(), .001)
    }

    @Test fun testIntegerExpressionDivIntegerProperty() {
        val property1 = 10.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 / property2
        Assert.assertTrue(binding is IntegerBinding)
        Assert.assertEquals(2, binding.get())

        property1.value = 20
        Assert.assertEquals(4, binding.get())

        property2.value = 20
        Assert.assertEquals(1, binding.get())
    }

    @Test fun testIntegerExpressionDivLongProperty() {
        val property1 = 10.toProperty()
        val property2 = 5L.toProperty()

        val binding = property1 / property2
        Assert.assertTrue(binding is LongBinding)
        Assert.assertEquals(2L, binding.get())

        property1.value = 20
        Assert.assertEquals(4L, binding.get())

        property2.value = 20L
        Assert.assertEquals(1L, binding.get())
    }

    @Test fun testIntegerExpressionDivFloatProperty() {
        val property1 = 10.toProperty()
        val property2 = 5f.toProperty()

        val binding = property1 / property2
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(2f, binding.get(), .001f)

        property1.value = 20
        Assert.assertEquals(4f, binding.get(), .001f)

        property2.value = 20f
        Assert.assertEquals(1f, binding.get(), .001f)
    }

    @Test fun testIntegerExpressionDivDoubleProperty() {
        val property1 = 10.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 / property2
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(2.0, binding.get(), .001)

        property1.value = 20
        Assert.assertEquals(4.0, binding.get(), .001)

        property2.value = 20.0
        Assert.assertEquals(1.0, binding.get(), .001)
    }

    @Test fun testIntegerPropertyDivAssignNumber() {
        val property = 5.toProperty()
        property /= 5

        Assert.assertEquals(1, property.get())
    }

    @Test fun testIntegerPropertyDivAssignNumberProperty() {
        val property1 = 5.toProperty()
        val property2 = 5.toProperty()

        property1 /= property2
        Assert.assertEquals(1, property1.get())
    }

    @Test fun testIntegerExpressionRemInt() {
        val property = 6.toProperty()

        val binding = property % 5
        Assert.assertTrue(binding is IntegerBinding)
        Assert.assertEquals(1, binding.get())

        property.value = 12
        Assert.assertEquals(2, binding.get())
    }

    @Test fun testIntegerExpressionRemLong() {
        val property = 6.toProperty()

        val binding = property % 5L
        Assert.assertTrue(binding is LongBinding)
        Assert.assertEquals(1L, binding.get())

        property.value = 12
        Assert.assertEquals(2L, binding.get())
    }

    @Test fun testIntegerExpressionRemFloat() {
        val property = 6.toProperty()

        val binding = property % 5f
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(1f, binding.get(), .001f)

        property.value = 12
        Assert.assertEquals(2f, binding.get(), .001f)
    }

    @Test fun testIntegerExpressionRemDouble() {
        val property = 6.toProperty()

        val binding = property % 5.0
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(1.0, binding.get(), .001)

        property.value = 12
        Assert.assertEquals(2.0, binding.get(), .001)
    }

    @Test fun testIntegerExpressionRemIntegerProperty() {
        val property1 = 6.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 % property2
        Assert.assertTrue(binding is IntegerBinding)
        Assert.assertEquals(1, binding.get())

        property1.value = 12
        Assert.assertEquals(2, binding.get())

        property2.value = 11
        Assert.assertEquals(1, binding.get())
    }

    @Test fun testIntegerExpressionRemLongProperty() {
        val property1 = 6.toProperty()
        val property2 = 5L.toProperty()

        val binding = property1 % property2
        Assert.assertTrue(binding is LongBinding)
        Assert.assertEquals(1L, binding.get())

        property1.value = 12
        Assert.assertEquals(2L, binding.get())

        property2.value = 11L
        Assert.assertEquals(1L, binding.get())
    }

    @Test fun testIntegerExpressionRemFloatProperty() {
        val property1 = 6.toProperty()
        val property2 = 5f.toProperty()

        val binding = property1 % property2
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(1f, binding.get(), .001f)

        property1.value = 12
        Assert.assertEquals(2f, binding.get(), .001f)

        property2.value = 11f
        Assert.assertEquals(1f, binding.get(), .001f)
    }

    @Test fun testIntegerExpressionRemDoubleProperty() {
        val property1 = 6.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 % property2
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(1.0, binding.get(), .001)

        property1.value = 12
        Assert.assertEquals(2.0, binding.get(), .001)

        property2.value = 11.0
        Assert.assertEquals(1.0, binding.get(), .001)
    }

    @Test fun testIntegerPropertyRemAssignNumber() {
        val property = 6.toProperty()
        property %= 5

        Assert.assertEquals(1, property.get())
    }

    @Test fun testIntegerPropertyRemAssignNumberProperty() {
        val property1 = 6.toProperty()
        val property2 = 5.toProperty()

        property1 %= property2
        Assert.assertEquals(1, property1.get())
    }

    @Test fun testIntegerPropertyRangeToInt() {
        val property = 0.toProperty()
        val sequence = property..9

        var counter = 0
        for (i in sequence) {
            Assert.assertEquals(counter++, i.get())
        }

        Assert.assertEquals(10, counter)
    }

    @Test fun testIntegerPropertyRangeToIntegerProperty() {
        val property1 = 0.toProperty()
        val property2 = 9.toProperty()
        val sequence = property1..property2

        var counter = 0
        for (i in sequence) {
            Assert.assertEquals(counter++, i.get())
        }

        Assert.assertEquals(10, counter)
    }

    @Test fun testIntegerPropertyRangeToLong() {
        val property = 0.toProperty()
        val sequence = property..9L

        var counter = 0L
        for (i in sequence) {
            Assert.assertEquals(counter++, i.get())
        }

        Assert.assertEquals(10L, counter)
    }

    @Test fun testIntegerPropertyRangeToLongProperty() {
        val property1 = 0.toProperty()
        val property2 = 9L.toProperty()
        val sequence = property1..property2

        var counter = 0L
        for (i in sequence) {
            Assert.assertEquals(counter++, i.get())
        }

        Assert.assertEquals(10L, counter)
    }

    @Test fun testIntegerPropertyCompareToNumber() {
        val property = 5.toProperty()

        Assert.assertTrue(property > 4)
        Assert.assertTrue(property >= 5)
        Assert.assertTrue(property >= 4)
        Assert.assertTrue(property <= 5)
        Assert.assertTrue(property <= 5)
        Assert.assertTrue(property <= 6)
        Assert.assertTrue(property < 6)

        Assert.assertFalse(property > 6)
        Assert.assertFalse(property >= 6)
        Assert.assertFalse(property <= 4)
        Assert.assertFalse(property < 4)
    }

    @Test fun testIntegerPropertyCompareToNumberProperty() {
        val property = 5.toProperty()


        Assert.assertTrue(property > 4.toProperty())
        Assert.assertTrue(property >= 5.toProperty())
        Assert.assertTrue(property >= 4.toProperty())
        Assert.assertTrue(property <= 5.toProperty())
        Assert.assertTrue(property <= 5.toProperty())
        Assert.assertTrue(property <= 6.toProperty())
        Assert.assertTrue(property < 6.toProperty())

        Assert.assertFalse(property > 6.toProperty())
        Assert.assertFalse(property >= 6.toProperty())
        Assert.assertFalse(property <= 4.toProperty())
        Assert.assertFalse(property < 4.toProperty())
    }

    @Test fun testLongExpressionPlusNumber() {
        val property = 0L.toProperty()

        val binding = property + 5
        Assert.assertTrue(binding is LongBinding)
        Assert.assertEquals(5L, binding.get())

        property.value -= 5L
        Assert.assertEquals(0L, binding.get())
    }

    @Test fun testLongExpressionPlusFloat() {
        val property = 0L.toProperty()

        val binding = property + 5f
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(5f, binding.get(), .001f)

        property.value -= 5L
        Assert.assertEquals(0f, binding.get(), .001f)
    }

    @Test fun testLongExpressionPlusDouble() {
        val property = 0L.toProperty()

        val binding = property + 5.0
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(5.0, binding.get(), .001)

        property.value -= 5L
        Assert.assertEquals(0.0, binding.get(), .001)
    }

    @Test fun testLongExpressionPlusNumberProperty() {
        val property1 = 0L.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 + property2
        Assert.assertTrue(binding is LongBinding)
        Assert.assertEquals(5L, binding.get())

        property1.value -= 10L
        Assert.assertEquals(-5L, binding.get())

        property2.value = 0
        Assert.assertEquals(-10L, binding.get())
    }

    @Test fun testLongExpressionPlusFloatProperty() {
        val property1 = 0L.toProperty()
        val property2 = 5f.toProperty()

        val binding = property1 + property2
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(5f, binding.get(), .001f)

        property1.value -= 10L
        Assert.assertEquals(-5f, binding.get(), .001f)

        property2.value = 0f
        Assert.assertEquals(-10f, binding.get(), .001f)
    }

    @Test fun testLongExpressionPlusDoubleProperty() {
        val property1 = 0L.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 + property2
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(5.0, binding.get(), .001)

        property1.value -= 10L
        Assert.assertEquals(-5.0, binding.get(), .001)

        property2.value = 0.0
        Assert.assertEquals(-10.0, binding.get(), .001)
    }

    @Test fun testLongPropertyPlusAssignNumber() {
        val property = 0L.toProperty()
        property += 5

        Assert.assertEquals(5L, property.get())
    }

    @Test fun testLongPropertyPlusAssignNumberProperty() {
        val property1 = 0L.toProperty()
        val property2 = 5.toProperty()

        property1 += property2
        Assert.assertEquals(5L, property1.get())
    }

    @Test fun testLongExpressionMinusNumber() {
        val property = 0L.toProperty()

        val binding = property - 5
        Assert.assertTrue(binding is LongBinding)
        Assert.assertEquals(-5L, binding.get())

        property.value -= 5L
        Assert.assertEquals(-10L, binding.get())
    }

    @Test fun testLongExpressionMinusFloat() {
        val property = 0L.toProperty()

        val binding = property - 5f
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(-5f, binding.get(), .001f)

        property.value -= 5L
        Assert.assertEquals(-10f, binding.get(), .001f)
    }

    @Test fun testLongExpressionMinusDouble() {
        val property = 0L.toProperty()

        val binding = property - 5.0
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(-5.0, binding.get(), .001)

        property.value -= 5L
        Assert.assertEquals(-10.0, binding.get(), .001)
    }

    @Test fun testLongExpressionMinusNumberProperty() {
        val property1 = 0L.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 - property2
        Assert.assertTrue(binding is LongBinding)
        Assert.assertEquals(-5L, binding.get())

        property1.value -= 10L
        Assert.assertEquals(-15L, binding.get())

        property2.value = 0
        Assert.assertEquals(-10L, binding.get())
    }

    @Test fun testLongExpressionMinusFloatProperty() {
        val property1 = 0L.toProperty()
        val property2 = 5f.toProperty()

        val binding = property1 - property2
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(-5f, binding.get(), .001f)

        property1.value -= 10L
        Assert.assertEquals(-15f, binding.get(), .001f)

        property2.value = 0f
        Assert.assertEquals(-10f, binding.get(), .001f)
    }

    @Test fun testLongExpressionMinusDoubleProperty() {
        val property1 = 0L.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 - property2
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(-5.0, binding.get(), .001)

        property1.value -= 10L
        Assert.assertEquals(-15.0, binding.get(), .001)

        property2.value = 0.0
        Assert.assertEquals(-10.0, binding.get(), .001)
    }

    @Test fun testLongPropertyMinusAssignNumber() {
        val property = 0L.toProperty()
        property -= 5

        Assert.assertEquals(-5L, property.get())
    }

    @Test fun testLongPropertyMinusAssignNumberProperty() {
        val property1 = 0L.toProperty()
        val property2 = 5.toProperty()

        property1 -= property2
        Assert.assertEquals(-5L, property1.get())
    }

    @Test fun testLongPropertyUnaryMinus() {
        val property = 1L.toProperty()

        val binding = -property
        Assert.assertEquals(-1L, binding.get())

        property += 1
        Assert.assertEquals(-2L, binding.get())
    }

    @Test fun testLongExpressionTimesNumber() {
        val property = 2L.toProperty()

        val binding = property * 5
        Assert.assertTrue(binding is LongBinding)
        Assert.assertEquals(10L, binding.get())

        property.value = 5L
        Assert.assertEquals(25L, binding.get())
    }

    @Test fun testLongExpressionTimesFloat() {
        val property = 2L.toProperty()

        val binding = property * 5f
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(10f, binding.get(), .001f)

        property.value = 5L
        Assert.assertEquals(25f, binding.get(), .001f)
    }

    @Test fun testLongExpressionTimesDouble() {
        val property = 2L.toProperty()

        val binding = property * 5.0
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(10.0, binding.get(), .001)

        property.value = 5L
        Assert.assertEquals(25.0, binding.get(), .001)
    }

    @Test fun testLongExpressionTimesNumberProperty() {
        val property1 = 2L.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 * property2
        Assert.assertTrue(binding is LongBinding)
        Assert.assertEquals(10L, binding.get())

        property1.value = 10L
        Assert.assertEquals(50L, binding.get())

        property2.value = 0
        Assert.assertEquals(0L, binding.get())
    }

    @Test fun testLongExpressionTimesFloatProperty() {
        val property1 = 2L.toProperty()
        val property2 = 5f.toProperty()

        val binding = property1 * property2
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(10f, binding.get(), .001f)

        property1.value = 10L
        Assert.assertEquals(50f, binding.get(), .001f)

        property2.value = 0f
        Assert.assertEquals(0f, binding.get(), .001f)
    }

    @Test fun testLongExpressionTimesDoubleProperty() {
        val property1 = 2L.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 * property2
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(10.0, binding.get(), .001)

        property1.value = 10L
        Assert.assertEquals(50.0, binding.get(), .001)

        property2.value = 0.0
        Assert.assertEquals(0.0, binding.get(), .001)
    }

    @Test fun testLongPropertyTimesAssignNumber() {
        val property = 1L.toProperty()
        property *= 5

        Assert.assertEquals(5L, property.get())
    }

    @Test fun testLongPropertyTimesAssignNumberProperty() {
        val property1 = 1L.toProperty()
        val property2 = 5.toProperty()

        property1 *= property2
        Assert.assertEquals(5L, property1.get())
    }

    @Test fun testLongExpressionDivNumber() {
        val property = 10L.toProperty()

        val binding = property / 5
        Assert.assertTrue(binding is LongBinding)
        Assert.assertEquals(2L, binding.get())

        property.value = 20L
        Assert.assertEquals(4L, binding.get())
    }

    @Test fun testLongExpressionDivFloat() {
        val property = 10L.toProperty()

        val binding = property / 5f
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(2f, binding.get(), .001f)

        property.value = 20L
        Assert.assertEquals(4f, binding.get(), .001f)
    }

    @Test fun testLongExpressionDivDouble() {
        val property = 10L.toProperty()

        val binding = property / 5.0
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(2.0, binding.get(), .001)

        property.value = 20L
        Assert.assertEquals(4.0, binding.get(), .001)
    }

    @Test fun testLongExpressionDivNumberProperty() {
        val property1 = 10L.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 / property2
        Assert.assertTrue(binding is LongBinding)
        Assert.assertEquals(2L, binding.get())

        property1.value = 20L
        Assert.assertEquals(4L, binding.get())

        property2.value = 20
        Assert.assertEquals(1L, binding.get())
    }

    @Test fun testLongExpressionDivFloatProperty() {
        val property1 = 10L.toProperty()
        val property2 = 5f.toProperty()

        val binding = property1 / property2
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(2f, binding.get(), .001f)

        property1.value = 20L
        Assert.assertEquals(4f, binding.get(), .001f)

        property2.value = 20f
        Assert.assertEquals(1f, binding.get(), .001f)
    }

    @Test fun testLongExpressionDivDoubleProperty() {
        val property1 = 10L.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 / property2
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(2.0, binding.get(), .001)

        property1.value = 20L
        Assert.assertEquals(4.0, binding.get(), .001)

        property2.value = 20.0
        Assert.assertEquals(1.0, binding.get(), .001)
    }

    @Test fun testLongPropertyDivAssignNumber() {
        val property = 5L.toProperty()
        property /= 5

        Assert.assertEquals(1L, property.get())
    }

    @Test fun testLongPropertyDivAssignNumberProperty() {
        val property1 = 5L.toProperty()
        val property2 = 5.toProperty()

        property1 /= property2
        Assert.assertEquals(1L, property1.get())
    }

    @Test fun testLongExpressionRemNumber() {
        val property = 6L.toProperty()

        val binding = property % 5
        Assert.assertTrue(binding is LongBinding)
        Assert.assertEquals(1L, binding.get())

        property.value = 12L
        Assert.assertEquals(2L, binding.get())
    }

    @Test fun testLongExpressionRemFloat() {
        val property = 6L.toProperty()

        val binding = property % 5f
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(1f, binding.get(), .001f)

        property.value = 12L
        Assert.assertEquals(2f, binding.get(), .001f)
    }

    @Test fun testLongExpressionRemDouble() {
        val property = 6L.toProperty()

        val binding = property % 5.0
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(1.0, binding.get(), .001)

        property.value = 12L
        Assert.assertEquals(2.0, binding.get(), .001)
    }

    @Test fun testLongExpressionRemNumberProperty() {
        val property1 = 6L.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 % property2
        Assert.assertTrue(binding is LongBinding)
        Assert.assertEquals(1L, binding.get())

        property1.value = 12L
        Assert.assertEquals(2L, binding.get())

        property2.value = 11
        Assert.assertEquals(1L, binding.get())
    }

    @Test fun testLongExpressionRemFloatProperty() {
        val property1 = 6L.toProperty()
        val property2 = 5f.toProperty()

        val binding = property1 % property2
        Assert.assertTrue(binding is FloatBinding)
        Assert.assertEquals(1f, binding.get(), .001f)

        property1.value = 12L
        Assert.assertEquals(2f, binding.get(), .001f)

        property2.value = 11f
        Assert.assertEquals(1f, binding.get(), .001f)
    }

    @Test fun testLongExpressionRemDoubleProperty() {
        val property1 = 6L.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 % property2
        Assert.assertTrue(binding is DoubleBinding)
        Assert.assertEquals(1.0, binding.get(), .001)

        property1.value = 12L
        Assert.assertEquals(2.0, binding.get(), .001)

        property2.value = 11.0
        Assert.assertEquals(1.0, binding.get(), .001)
    }

    @Test fun testLongPropertyRemAssignNumber() {
        val property = 6L.toProperty()
        property %= 5

        Assert.assertEquals(1L, property.get())
    }

    @Test fun testLongPropertyRemAssignNumberProperty() {
        val property1 = 6L.toProperty()
        val property2 = 5.toProperty()

        property1 %= property2
        Assert.assertEquals(1L, property1.get())
    }

    @Test fun testLongPropertyRangeToInt() {
        val property = 0L.toProperty()
        val sequence = property..9

        var counter = 0L
        for (i in sequence) {
            Assert.assertEquals(counter++, i.get())
        }

        Assert.assertEquals(10L, counter)
    }

    @Test fun testLongPropertyRangeToIntegerProperty() {
        val property1 = 0L.toProperty()
        val property2 = 9.toProperty()
        val sequence = property1..property2

        var counter = 0L
        for (i in sequence) {
            Assert.assertEquals(counter++, i.get())
        }

        Assert.assertEquals(10L, counter)
    }

    @Test fun testLongPropertyRangeToLong() {
        val property = 0L.toProperty()
        val sequence = property..9L

        var counter = 0L
        for (i in sequence) {
            Assert.assertEquals(counter++, i.get())
        }

        Assert.assertEquals(10L, counter)
    }

    @Test fun testLongPropertyRangeToLongProperty() {
        val property1 = 0L.toProperty()
        val property2 = 9L.toProperty()
        val sequence = property1..property2

        var counter = 0L
        for (i in sequence) {
            Assert.assertEquals(counter++, i.get())
        }

        Assert.assertEquals(10L, counter)
    }

    @Test fun testLongPropertyCompareToNumber() {
        val property = 5L.toProperty()

        Assert.assertTrue(property > 4)
        Assert.assertTrue(property >= 5)
        Assert.assertTrue(property >= 4)
        Assert.assertTrue(property <= 5)
        Assert.assertTrue(property <= 5)
        Assert.assertTrue(property <= 6)
        Assert.assertTrue(property < 6)

        Assert.assertFalse(property > 6)
        Assert.assertFalse(property >= 6)
        Assert.assertFalse(property <= 4)
        Assert.assertFalse(property < 4)
    }

    @Test fun testLongPropertyCompareToNumberProperty() {
        val property = 5L.toProperty()


        Assert.assertTrue(property > 4.toProperty())
        Assert.assertTrue(property >= 5.toProperty())
        Assert.assertTrue(property >= 4.toProperty())
        Assert.assertTrue(property <= 5.toProperty())
        Assert.assertTrue(property <= 5.toProperty())
        Assert.assertTrue(property <= 6.toProperty())
        Assert.assertTrue(property < 6.toProperty())

        Assert.assertFalse(property > 6.toProperty())
        Assert.assertFalse(property >= 6.toProperty())
        Assert.assertFalse(property <= 4.toProperty())
        Assert.assertFalse(property < 4.toProperty())
    }

    @Test fun testNumberExpressionGtInt() {
        val property = (-1).toProperty()

        val binding = property gt 0
        Assert.assertFalse(binding.get())

        property.value = 0
        Assert.assertFalse(binding.get())

        property.value = 1
        Assert.assertTrue(binding.get())
    }

    @Test fun testNumberExpressionGtLong() {
        val property = (-1).toProperty()

        val binding = property gt 0L
        Assert.assertFalse(binding.get())

        property.value = 0
        Assert.assertFalse(binding.get())

        property.value = 1
        Assert.assertTrue(binding.get())
    }

    @Test fun testNumberExpressionGtFloat() {
        val property = (-1).toProperty()

        val binding = property gt 0f
        Assert.assertFalse(binding.get())

        property.value = 0
        Assert.assertFalse(binding.get())

        property.value = 1
        Assert.assertTrue(binding.get())
    }

    @Test fun testNumberExpressionGtDouble() {
        val property = (-1).toProperty()

        val binding = property gt 0.0
        Assert.assertFalse(binding.get())

        property.value = 0
        Assert.assertFalse(binding.get())

        property.value = 1
        Assert.assertTrue(binding.get())
    }

    @Test fun testNumberExpressionGtNumberProperty() {
        val property = (-1).toProperty()

        val binding = property gt 0.0.toProperty()
        Assert.assertFalse(binding.get())

        property.value = 0
        Assert.assertFalse(binding.get())

        property.value = 1
        Assert.assertTrue(binding.get())
    }

    @Test fun testNumberExpressionGeInt() {
        val property = (-1).toProperty()

        val binding = property ge 0
        Assert.assertFalse(binding.get())

        property.value = 0
        Assert.assertTrue(binding.get())

        property.value = 1
        Assert.assertTrue(binding.get())
    }

    @Test fun testNumberExpressionGeLong() {
        val property = (-1).toProperty()

        val binding = property ge 0L
        Assert.assertFalse(binding.get())

        property.value = 0
        Assert.assertTrue(binding.get())

        property.value = 1
        Assert.assertTrue(binding.get())
    }

    @Test fun testNumberExpressionGeFloat() {
        val property = (-1).toProperty()

        val binding = property ge 0f
        Assert.assertFalse(binding.get())

        property.value = 0
        Assert.assertTrue(binding.get())

        property.value = 1
        Assert.assertTrue(binding.get())
    }

    @Test fun testNumberExpressionGeDouble() {
        val property = (-1).toProperty()

        val binding = property ge 0.0
        Assert.assertFalse(binding.get())

        property.value = 0
        Assert.assertTrue(binding.get())

        property.value = 1
        Assert.assertTrue(binding.get())
    }

    @Test fun testNumberExpressionGeNumberProperty() {
        val property = (-1).toProperty()

        val binding = property ge 0.0.toProperty()
        Assert.assertFalse(binding.get())

        property.value = 0
        Assert.assertTrue(binding.get())

        property.value = 1
        Assert.assertTrue(binding.get())
    }

    @Test fun testNumberExpressionEqInt() {
        val property = (-1).toProperty()

        val binding = property eq 0
        Assert.assertFalse(binding.get())

        property.value = 0
        Assert.assertTrue(binding.get())

        property.value = 1
        Assert.assertFalse(binding.get())
    }

    @Test fun testNumberExpressionEqLong() {
        val property = (-1).toProperty()

        val binding = property eq 0L
        Assert.assertFalse(binding.get())

        property.value = 0
        Assert.assertTrue(binding.get())

        property.value = 1
        Assert.assertFalse(binding.get())
    }

    @Test fun testNumberExpressionEqNumberProperty() {
        val property = (-1).toProperty()

        val binding = property eq 0.0.toProperty()
        Assert.assertFalse(binding.get())

        property.value = 0
        Assert.assertTrue(binding.get())

        property.value = 1
        Assert.assertFalse(binding.get())
    }

    @Test fun testNumberExpressionLeInt() {
        val property = (-1).toProperty()

        val binding = property le 0
        Assert.assertTrue(binding.get())

        property.value = 0
        Assert.assertTrue(binding.get())

        property.value = 1
        Assert.assertFalse(binding.get())
    }

    @Test fun testNumberExpressionLeLong() {
        val property = (-1).toProperty()

        val binding = property le 0L
        Assert.assertTrue(binding.get())

        property.value = 0
        Assert.assertTrue(binding.get())

        property.value = 1
        Assert.assertFalse(binding.get())
    }

    @Test fun testNumberExpressionLeFloat() {
        val property = (-1).toProperty()

        val binding = property le 0f
        Assert.assertTrue(binding.get())

        property.value = 0
        Assert.assertTrue(binding.get())

        property.value = 1
        Assert.assertFalse(binding.get())
    }

    @Test fun testNumberExpressionLeDouble() {
        val property = (-1).toProperty()

        val binding = property le 0.0
        Assert.assertTrue(binding.get())

        property.value = 0
        Assert.assertTrue(binding.get())

        property.value = 1
        Assert.assertFalse(binding.get())
    }

    @Test fun testNumberExpressionLeNumberProperty() {
        val property = (-1).toProperty()

        val binding = property le 0.0.toProperty()
        Assert.assertTrue(binding.get())

        property.value = 0
        Assert.assertTrue(binding.get())

        property.value = 1
        Assert.assertFalse(binding.get())
    }

    @Test fun testNumberExpressionLtInt() {
        val property = (-1).toProperty()

        val binding = property lt 0
        Assert.assertTrue(binding.get())

        property.value = 0
        Assert.assertFalse(binding.get())

        property.value = 1
        Assert.assertFalse(binding.get())
    }

    @Test fun testNumberExpressionLtLong() {
        val property = (-1).toProperty()

        val binding = property lt 0L
        Assert.assertTrue(binding.get())

        property.value = 0
        Assert.assertFalse(binding.get())

        property.value = 1
        Assert.assertFalse(binding.get())
    }

    @Test fun testNumberExpressionLtFloat() {
        val property = (-1).toProperty()

        val binding = property lt 0f
        Assert.assertTrue(binding.get())

        property.value = 0
        Assert.assertFalse(binding.get())

        property.value = 1
        Assert.assertFalse(binding.get())
    }

    @Test fun testNumberExpressionLtDouble() {
        val property = (-1).toProperty()

        val binding = property lt 0.0
        Assert.assertTrue(binding.get())

        property.value = 0
        Assert.assertFalse(binding.get())

        property.value = 1
        Assert.assertFalse(binding.get())
    }

    @Test fun testNumberExpressionLtNumberProperty() {
        val property = (-1).toProperty()

        val binding = property lt 0.0.toProperty()
        Assert.assertTrue(binding.get())

        property.value = 0
        Assert.assertFalse(binding.get())

        property.value = 1
        Assert.assertFalse(binding.get())
    }

    @Test fun testBooleanExpressionNot() {
        val property = true.toProperty()

        val binding = !property
        Assert.assertFalse(binding.get())

        property.value = false
        Assert.assertTrue(binding.get())
    }

    @Test fun testBooleanExpressionAndBoolean() {
        val property = true.toProperty()

        val binding = property and true
        Assert.assertTrue(binding.get())

        property.value = false
        Assert.assertFalse(binding.get())
    }

    @Test fun testBooleanExpressionAndBooleanProperty() {
        val property1 = true.toProperty()
        val property2 = true.toProperty()

        val binding = property1 and property2
        Assert.assertTrue(binding.get())

        property1.value = false
        Assert.assertFalse(binding.get())

        property1.value = true
        Assert.assertTrue(binding.get())

        property2.value = false
        Assert.assertFalse(binding.get())
    }

    @Test fun testBooleanExpressionOrBoolean() {
        val property = false.toProperty()

        val binding = property or false
        Assert.assertFalse(binding.get())

        property.value = true
        Assert.assertTrue(binding.get())
    }

    @Test fun testBooleanExpressionOrBooleanProperty() {
        val property1 = false.toProperty()
        val property2 = false.toProperty()

        val binding = property1 or property2
        Assert.assertFalse(binding.get())

        property1.value = true
        Assert.assertTrue(binding.get())

        property2.value = true
        Assert.assertTrue(binding.get())
    }

    @Test fun testBooleanExpressionXorBoolean() {
        val property = false.toProperty()

        val binding = property xor true
        Assert.assertTrue(binding.get())

        property.value = true
        Assert.assertFalse(binding.get())
    }

    @Test fun testBooleanExpressionXorBooleanProperty() {
        val property1 = false.toProperty()
        val property2 = false.toProperty()

        val binding = property1 xor property2
        Assert.assertFalse(binding.get())

        property1.value = true
        Assert.assertTrue(binding.get())

        property2.value = true
        Assert.assertFalse(binding.get())
    }

    @Test fun testBooleanExpressionEqBoolean() {
        val property = false.toProperty()

        val binding = property eq false
        Assert.assertTrue(binding.get())

        property.value = true
        Assert.assertFalse(binding.get())
    }

    @Test fun testBooleanExpressionEqBooleanProperty() {
        val property1 = false.toProperty()
        val property2 = false.toProperty()

        val binding = property1 eq property2
        Assert.assertTrue(binding.get())

        property1.value = true
        Assert.assertFalse(binding.get())

        property2.value = true
        Assert.assertTrue(binding.get())
    }

    @Test fun testStringExpressionPlusAny() {
        val property = "Hello ".toProperty()

        val binding = property + "World!"
        Assert.assertTrue(binding.get() == "Hello World!")

        property.value = "Bye "
        Assert.assertTrue(binding.get() == "Bye World!")
    }

    @Test fun testStringPropertyPlusAssignAny() {
        val property = "Hello ".toProperty()

        Assert.assertTrue(property.get() == "Hello ")

        property += "World!"
        Assert.assertTrue(property.get() == "Hello World!")
    }

    @Test fun testStringExpressionGetInt() {
        val property = "Hello World!".toProperty()

        val binding = property[0]
        Assert.assertEquals('H', binding.value)

        property.value = "Bye World!"
        Assert.assertEquals('B', binding.value)
    }

    @Test fun testStringExpressionGetIntProperty() {
        val property = "Hello World!".toProperty()
        val indexProperty = 0.toProperty()

        val binding = property[indexProperty]
        Assert.assertEquals('H', binding.value)

        property.value = "Bye World!"
        Assert.assertEquals('B', binding.value)

        indexProperty.value = 1
        Assert.assertEquals('y', binding.value)
    }

    @Test fun testStringExpressionGetIntToInt() {
        val property = "foo()".toProperty()

        val binding = property[0, 3]
        Assert.assertEquals("foo", binding.get())

        property.value = "bar()"
        Assert.assertEquals("bar", binding.get())
    }

    @Test fun testStringExpressionGetIntegerPropertyToInt() {
        val property = "foo()".toProperty()
        val startIndex = 0.toProperty()

        val binding = property[startIndex, 3]
        Assert.assertEquals("foo", binding.get())

        property.value = "bar()"
        Assert.assertEquals("bar", binding.get())

        startIndex.value = 1
        Assert.assertEquals("ar", binding.get())
    }

    @Test fun testStringExpressionGetIntToIntegerProperty() {
        val property = "foo()".toProperty()
        val endIndex = 3.toProperty()

        val binding = property[0, endIndex]
        Assert.assertEquals("foo", binding.get())

        property.value = "bar()"
        Assert.assertEquals("bar", binding.get())

        endIndex.value = 5
        Assert.assertEquals("bar()", binding.get())
    }

    @Test fun testStringExpressionGetIntegerPropertyToIntegerProperty() {
        val property = "foo()".toProperty()
        val startIndex = 0.toProperty()
        val endIndex = 3.toProperty()

        val binding = property[startIndex, endIndex]
        Assert.assertEquals("foo", binding.get())

        property.value = "bar()"
        Assert.assertEquals("bar", binding.get())

        startIndex.value = 3
        endIndex.value = 5
        Assert.assertEquals("()", binding.get())
    }

    @Test fun testStringExpressionUnaryMinus() {
        val property = "god a ward".toProperty()

        val binding = -property
        Assert.assertEquals("draw a dog", binding.get())

        property.value = "dog a ward"
        Assert.assertEquals("draw a god", binding.get())
    }

    @Test fun testStringExpressionCompareToString() {
        val property = "Bravo".toProperty()

        Assert.assertTrue(property > "Alpha")
        Assert.assertTrue(property >= "Alpha")
        Assert.assertTrue(property >= "Bravo")

        Assert.assertTrue(property <= "Bravo")
        Assert.assertTrue(property <= "Charlie")
        Assert.assertTrue(property < "Charlie")

        Assert.assertFalse(property < "Alpha")
        Assert.assertFalse(property <= "Alpha")

        Assert.assertFalse(property >= "Charlie")
        Assert.assertFalse(property > "Charlie")
    }

    @Test fun testStringExpressionCompareToStringProperty() {
        val property = "Bravo".toProperty()

        Assert.assertTrue(property > "Alpha".toProperty())
        Assert.assertTrue(property >= "Alpha".toProperty())
        Assert.assertTrue(property >= "Bravo".toProperty())

        Assert.assertTrue(property <= "Bravo".toProperty())
        Assert.assertTrue(property <= "Charlie".toProperty())
        Assert.assertTrue(property < "Charlie".toProperty())

        Assert.assertFalse(property < "Alpha".toProperty())
        Assert.assertFalse(property <= "Alpha".toProperty())

        Assert.assertFalse(property >= "Charlie".toProperty())
        Assert.assertFalse(property > "Charlie".toProperty())
    }

    @Test fun testStringExpressionGtString() {
        val property = "Bravo".toProperty()

        val binding = property gt "Alpha"
        Assert.assertTrue(binding.get())

        property.value = "Alpha"
        Assert.assertFalse(binding.get())
    }

    @Test fun testStringExpressionGtStringProperty() {
        val property1 = "Charlie".toProperty()
        val property2 = "Bravo".toProperty()

        val binding = property1 gt property2
        Assert.assertTrue(binding.get())

        property1.value = "Bravo"
        Assert.assertFalse(binding.get())

        property2.value = "Alpha"
        Assert.assertTrue(binding.get())
    }

    @Test fun testStringExpressionGeString() {
        val property = "Charlie".toProperty()

        val binding = property ge "Bravo"
        Assert.assertTrue(binding.get())

        property.value = "Bravo"
        Assert.assertTrue(binding.get())

        property.value = "Alpha"
        Assert.assertFalse(binding.get())
    }

    @Test fun testStringExpressionGeStringProperty() {
        val property1 = "Charlie".toProperty()
        val property2 = "Bravo".toProperty()

        val binding = property1 ge property2
        Assert.assertTrue(binding.get())

        property1.value = "Bravo"
        Assert.assertTrue(binding.get())

        property2.value = "Alpha"
        Assert.assertTrue(binding.get())

        property2.value = "Charlie"
        Assert.assertFalse(binding.get())
    }

    @Test fun testStringExpressionEqString() {
        val property = "Bravo".toProperty()

        val binding = property eq "Bravo"
        Assert.assertTrue(binding.get())

        property.value = "Alpha"
        Assert.assertFalse(binding.get())
    }

    @Test fun testStringExpressionEqStringProperty() {
        val property1 = "Bravo".toProperty()
        val property2 = "Bravo".toProperty()

        val binding = property1 eq property2
        Assert.assertTrue(binding.get())

        property1.value = "Alpha"
        Assert.assertFalse(binding.get())

        property2.value = "Alpha"
        Assert.assertTrue(binding.get())
    }

    @Test fun testStringExpressionLeString() {
        val property = "Alpha".toProperty()

        val binding = property le "Bravo"
        Assert.assertTrue(binding.get())

        property.value = "Bravo"
        Assert.assertTrue(binding.get())

        property.value = "Charlie"
        Assert.assertFalse(binding.get())
    }

    @Test fun testStringExpressionLeStringProperty() {
        val property1 = "Alpha".toProperty()
        val property2 = "Bravo".toProperty()

        val binding = property1 le property2
        Assert.assertTrue(binding.get())

        property1.value = "Bravo"
        Assert.assertTrue(binding.get())

        property2.value = "Charlie"
        Assert.assertTrue(binding.get())

        property2.value = "Alpha"
        Assert.assertFalse(binding.get())
    }

    @Test fun testStringExpressionLtString() {
        val property = "Alpha".toProperty()

        val binding = property lt "Bravo"
        Assert.assertTrue(binding.get())

        property.value = "Bravo"
        Assert.assertFalse(binding.get())

        property.value = "Charlie"
        Assert.assertFalse(binding.get())
    }

    @Test fun testStringExpressionLtStringProperty() {
        val property1 = "Alpha".toProperty()
        val property2 = "Bravo".toProperty()

        val binding = property1 lt property2
        Assert.assertTrue(binding.get())

        property1.value = "Bravo"
        Assert.assertFalse(binding.get())

        property2.value = "Charlie"
        Assert.assertTrue(binding.get())

        property2.value = "Alpha"
        Assert.assertFalse(binding.get())
    }

    @Test fun testStringExpressionEqIgnoreCaseString() {
        val property = "Hello World!".toProperty()

        val binding = property eqIgnoreCase "hello world!"
        Assert.assertTrue(binding.get())

        property.value = "Bye World!"
        Assert.assertFalse(binding.get())
    }

    @Test fun testStringExpressionEqIgnoreCaseStringProperty() {
        val property1 = "Hello World!".toProperty()
        val property2 = "hello world!".toProperty()

        val binding = property1 eqIgnoreCase property2
        Assert.assertTrue(binding.get())

        property1.value = "bye world!"
        Assert.assertFalse(binding.get())

        property2.value = "Bye World!"
        Assert.assertTrue(binding.get())
    }

    @Test fun propertyFromMapKey() {
        val map = mutableMapOf("hello" to "world", "number" to 42)
        val helloProperty = map.toProperty("hello") { SimpleStringProperty(it as String?) }
        val numberProperty = map.toProperty("number") { SimpleIntegerProperty(it as Int) }
        helloProperty.value = "there"
        numberProperty.value = 43
        Assert.assertEquals("there", map["hello"])
        Assert.assertEquals(43, map["number"])
    }

//    class ListHolder {
//        val listProperty: ListProperty<String> = SimpleListProperty<String>(FXCollections.observableArrayList())
//        var list: MutableList<String> by listProperty
//    }
//
//    @Test fun listPropertyDelegateModifyList() {
//        val listHolder = ListHolder()
//        var notified = false
//        listHolder.listProperty.addListener { _, _, _-> notified = true }
//
//        listHolder.list.add("Test")
//        Assert.assertTrue(notified)
//
//        notified = false
//        listHolder.list.remove("Test")
//        Assert.assertTrue(notified)
//
//        notified = false
//        listHolder.list.addAll(arrayOf("1", "2"))
//        Assert.assertTrue(notified)
//
//        notified = false
//        listHolder.list.clear()
//        Assert.assertTrue(notified)
//    }
//
//    @Test fun listPropertyDelegateChangeList() {
//        val listHolder = ListHolder()
//        var notified = false
//        listHolder.listProperty.addListener { _, _, _-> notified = true }
//
//        listHolder.list = mutableListOf("Test")
//        Assert.assertTrue(notified)
//    }
//
//    class SetHolder {
//        val setProperty: SetProperty<String> = SimpleSetProperty<String>(FXCollections.observableSet())
//        var set: MutableSet<String> by setProperty
//    }
//
//    @Test fun setPropertyDelegateModifySet() {
//        val setHolder = SetHolder()
//        var notified = false
//        setHolder.setProperty.addListener { _, _, _-> notified = true }
//
//        setHolder.set.add("Test")
//        Assert.assertTrue(notified)
//
//        notified = false
//        setHolder.set.remove("Test")
//        Assert.assertTrue(notified)
//
//        notified = false
//        setHolder.set.addAll(arrayOf("1", "2"))
//        Assert.assertTrue(notified)
//
//        notified = false
//        setHolder.set.clear()
//        Assert.assertTrue(notified)
//    }
//
//    @Test fun setPropertyDelegateChangeSet() {
//        val setHolder = SetHolder()
//        var notified = false
//        setHolder.setProperty.addListener { _, _, _-> notified = true }
//
//        setHolder.set = mutableSetOf("Test")
//        Assert.assertTrue(notified)
//    }
//
//    class MapHolder {
//        val mapProperty: MapProperty<Int, String> = SimpleMapProperty<Int, String>(FXCollections.observableHashMap())
//        var map: MutableMap<Int, String> by mapProperty
//    }
//
//    @Test fun mapPropertyDelegateModifyMap() {
//        val mapHolder = MapHolder()
//        var notified = false
//        mapHolder.mapProperty.addListener { _, _, _-> notified = true }
//
//        mapHolder.map.put(0, "Test")
//        Assert.assertTrue(notified)
//
//        notified = false
//        mapHolder.map.remove(0)
//        Assert.assertTrue(notified)
//
//        notified = false
//        mapHolder.map.putAll(mapOf(1 to "1", 2 to "2"))
//        Assert.assertTrue(notified)
//
//        notified = false
//        mapHolder.map.clear()
//        Assert.assertTrue(notified)
//    }
//
//    @Test fun mapPropertyDelegateChangeMap() {
//        val mapHolder = MapHolder()
//        var notified = false
//        mapHolder.mapProperty.addListener { _, _, _-> notified = true }
//
//        mapHolder.map = mutableMapOf(0 to "Test")
//        Assert.assertTrue(notified)
//    }
}