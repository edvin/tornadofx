package tornadofx.tests

import javafx.beans.binding.DoubleBinding
import javafx.beans.binding.FloatBinding
import javafx.beans.binding.IntegerBinding
import javafx.beans.binding.LongBinding
import javafx.beans.property.*
import org.junit.Assert
import org.junit.Test
import tornadofx.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PropertiesTest {

    @Test fun `onChange primitive`() {
        val d = SimpleDoubleProperty()
        // Ensure getting the value does not throw an NPE
        d.onChange { assert((it + 7) is Double) }

        d.value = 100.0
        d.value = Double.POSITIVE_INFINITY
        d.value = Double.NaN
        d.value = null
    }

    @Test fun `onChange property`() {
        var called = false
        val property = SimpleStringProperty("Hello World")

        property.onChange { called = true }
        property.value = "Changed"

        assertTrue(called)
    }


    @Test fun `property delegation`() {
        // given:
        val fixture = object {
            val nameProperty = SimpleStringProperty("Alice")
            var name by nameProperty

            val ageProperty = SimpleDoubleProperty(1.0)
            var age: Double by ageProperty

            val dirtyProperty = SimpleBooleanProperty(false)
            var dirty: Boolean by dirtyProperty
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

    @Test fun `property get should read value`() {
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

    @Test fun `property set should write value`() {
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


    private class TestClass {
        var myProperty: String by singleAssign()
    }

    @Test fun `singleAssign no assignment fails`() {
        val instance = TestClass()

        assertFailsWith<UninitializedPropertyAccessException> { instance.myProperty }
    }

    @Test fun `singleAssign successful assignment`() {
        val instance = TestClass()

        instance.myProperty = "foo"
        assertEquals("foo", instance.myProperty)
    }

    @Test fun `singleAssign double assignment fails`() {
        val instance = TestClass()

        instance.myProperty = "foo"
        assertFailsWith<RuntimeException> { instance.myProperty = "bar" }
    }


    @Test fun pojoWritableObservable() {
        val person = JavaPerson().apply {
            id = 1
            name = "John"
        }

        val idObservable = person.observable(JavaPerson::getId, JavaPerson::setId)
        val nameObservable = person.observable<String>("name")

        idObservable.value = 44
        nameObservable.value = "Doe"
        assertEquals(44, person.id)
        assertEquals("Doe", person.name)

        // when:
        person.id = 5
        assertEquals(5, idObservable.value)
    }

    @Test fun pojoWritableObservableGetterOnly() {
        val person = JavaPerson().apply {
            id = 1
            name = "John"
        }

        val idObservable = person.observable(JavaPerson::getId)
        val idBinding = idObservable.integerBinding { idObservable.value }
        val nameObservable = person.observable<String>("name")

        idObservable.value = 44
        nameObservable.value = "Doe"
        assertEquals(44, person.id)
        assertEquals(44, idBinding.value)
        assertEquals("Doe", person.name)

        person.id = 5
        // property change events on the pojo are propagated
        assertEquals(5, idObservable.value)
        assertEquals(5, idBinding.value)
    }


    @Test fun assignIfNull() {
        val has = SimpleObjectProperty("Hello")
        has.assignIfNull { "World" }
        assertEquals(has.value, "Hello")

        val hasNot = SimpleObjectProperty<String>()
        hasNot.assignIfNull { "World" }
        assertEquals(hasNot.value, "World")
    }


    // ================================================================
    // toProperty

    @Test fun `Int toProperty`() {
        val property = 5.toProperty()
        assertEquals(5, property.get())
    }

    @Test fun `Long toProperty`() {
        val property = 5L.toProperty()
        assertEquals(5, property.get())
    }

    @Test fun `Float toProperty`() {
        val property = 5.0f.toProperty()
        Assert.assertEquals(5.0f, property.get(), 10e-5f)
    }

    @Test fun `Double toProperty`() {
        val property = 5.0.toProperty()
        Assert.assertEquals(5.0, property.get(), 10e-5)
    }

    @Test fun `Boolean toProperty`() {
        val property = true.toProperty()
        assertTrue(property.get())
    }

    @Test fun `String toProperty`() {
        val property = "Hello World!".toProperty()
        assertEquals("Hello World!", property.get())
    }

    @Test fun `Map toProperty`() {
        val map = mutableMapOf("hello" to "world", "number" to 42)
        val helloProperty = map.toProperty("hello") { SimpleStringProperty(it as String) }
        val numberProperty = map.toProperty("number") { SimpleIntegerProperty(it as Int) }

        helloProperty.value = "there"
        numberProperty.value = 43

        assertEquals("there", map["hello"])
        assertEquals(43, map["number"])
    }


    // ================================================================
    // Double Properties

    @Test fun `DoubleExpression plus Number`() {
        val property = 0.0.toProperty()

        val binding = property + 5
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(5.0, binding.get(), 10e-5)

        property.value -= 5f
        Assert.assertEquals(0.0, binding.get(), 10e-5)
    }

    @Test fun `DoubleExpression plus NumberProperty`() {
        val property1 = 0.0.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 + property2
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(5.0, binding.get(), 10e-5)

        property1.value -= 10
        Assert.assertEquals(-5.0, binding.get(), 10e-5)

        property2.value = 0
        Assert.assertEquals(-10.0, binding.get(), 10e-5)
    }

    @Test fun `DoubleProperty plusAssign Number`() {
        val property = 0.0.toProperty()

        property += 5
        Assert.assertEquals(5.0, property.get(), 10e-5)
    }

    @Test fun `DoubleProperty plusAssign NumberProperty`() {
        val property1 = 0.0.toProperty()
        val property2 = 5.toProperty()

        property1 += property2
        Assert.assertEquals(5.0, property1.get(), 10e-5)
    }

    @Test fun `DoubleExpression minus Number`() {
        val property = 0.0.toProperty()

        val binding = property - 5
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(-5.0, binding.get(), 10e-5)

        property.value -= 5f
        Assert.assertEquals(-10.0, binding.get(), 10e-5)
    }

    @Test fun `DoubleExpression minus NumberProperty`() {
        val property1 = 0.0.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 - property2
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(-5.0, binding.get(), 10e-5)

        property1.value -= 10
        Assert.assertEquals(-15.0, binding.get(), 10e-5)

        property2.value = 0
        Assert.assertEquals(-10.0, binding.get(), 10e-5)
    }

    @Test fun `DoubleProperty minusAssign Number`() {
        val property = 0.0.toProperty()

        property -= 5
        Assert.assertEquals(-5.0, property.get(), 10e-5)
    }

    @Test fun `DoubleProperty minusAssign NumberProperty`() {
        val property1 = 0.0.toProperty()
        val property2 = 5.toProperty()

        property1 -= property2
        Assert.assertEquals(-5.0, property1.get(), 10e-5)
    }

    @Test fun `DoubleProperty unaryMinus`() {
        val property = 1.0.toProperty()

        val binding = -property
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(-1.0, binding.get(), 10e-5)

        property += 1
        Assert.assertEquals(-2.0, binding.get(), 10e-5)
    }

    @Test fun `DoubleExpression times Number`() {
        val property = 2.0.toProperty()

        val binding = property * 5
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(10.0, binding.get(), 10e-5)

        property.value = 5.0
        Assert.assertEquals(25.0, binding.get(), 10e-5)
    }

    @Test fun `DoubleExpression times NumberProperty`() {
        val property1 = 2.0.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 * property2
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(10.0, binding.get(), 10e-5)

        property1.value = 5.0
        Assert.assertEquals(25.0, binding.get(), 10e-5)

        property2.value = 0
        Assert.assertEquals(0.0, binding.get(), 10e-5)
    }

    @Test fun `DoubleProperty timesAssign Number`() {
        val property = 1.0.toProperty()

        property *= 5
        Assert.assertEquals(5.0, property.get(), 10e-5)
    }

    @Test fun `DoubleProperty timesAssign NumberProperty`() {
        val property1 = 1.0.toProperty()
        val property2 = 5.toProperty()

        property1 *= property2
        Assert.assertEquals(5.0, property1.get(), 10e-5)
    }

    @Test fun `DoubleExpression div Number`() {
        val property = 5.0.toProperty()

        val binding = property / 5
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(1.0, binding.get(), 10e-5)

        property.value = 10.0
        Assert.assertEquals(2.0, binding.get(), 10e-5)
    }

    @Test fun `DoubleExpression div NumberProperty`() {
        val property1 = 5.0.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 / property2
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(1.0, binding.get(), 10e-5)

        property1.value = 10.0
        Assert.assertEquals(2.0, binding.get(), 10e-5)

        property2.value = 20
        Assert.assertEquals(0.5, binding.get(), 10e-5)
    }

    @Test fun `DoubleProperty divAssign Number`() {
        val property = 5.0.toProperty()

        property /= 5
        Assert.assertEquals(1.0, property.get(), 10e-5)
    }

    @Test fun `DoubleProperty divAssign NumberProperty`() {
        val property1 = 5.0.toProperty()
        val property2 = 5.toProperty()

        property1 /= property2
        Assert.assertEquals(1.0, property1.get(), 10e-5)
    }

    @Test fun `DoubleExpression rem Number`() {
        val property = 6.0.toProperty()

        val binding = property % 5
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(1.0, binding.get(), 10e-5)

        property.value = 12.0
        Assert.assertEquals(2.0, binding.get(), 10e-5)
    }

    @Test fun `DoubleExpression rem NumberProperty`() {
        val property1 = 6.0.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 % property2
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(1.0, binding.get(), 10e-5)

        property1.value = 12.0
        Assert.assertEquals(2.0, binding.get(), 10e-5)

        property2.value = 11
        Assert.assertEquals(1.0, binding.get(), 10e-5)
    }

    @Test fun `DoubleProperty remAssign Number`() {
        val property = 6.0.toProperty()

        property %= 5
        Assert.assertEquals(1.0, property.get(), 10e-5)
    }

    @Test fun `DoubleProperty remAssign NumberProperty`() {
        val property1 = 6.0.toProperty()
        val property2 = 5.toProperty()

        property1 %= property2
        Assert.assertEquals(1.0, property1.get(), 10e-5)
    }

    @Test fun `DoubleProperty compareTo Number`() {
        val property = 5.0.toProperty()

        assertTrue(property > 4)
        assertTrue(property >= 4)
        assertTrue(property >= 5)
        assertTrue(property <= 5)
        assertTrue(property <= 6)
        assertTrue(property < 6)

        assertFalse(property > 6)
        assertFalse(property >= 6)
        assertFalse(property <= 4)
        assertFalse(property < 4)
    }

    @Test fun `DoubleProperty compareTo NumberProperty`() {
        val property = 5.0.toProperty()

        assertTrue(property > 4.toProperty())
        assertTrue(property >= 4.toProperty())
        assertTrue(property >= 5.toProperty())
        assertTrue(property <= 5.toProperty())
        assertTrue(property <= 6.toProperty())
        assertTrue(property < 6.toProperty())

        assertFalse(property > 6.toProperty())
        assertFalse(property >= 6.toProperty())
        assertFalse(property <= 4.toProperty())
        assertFalse(property < 4.toProperty())
    }


    // ================================================================
    // Float Properties

    @Test fun `FloatExpression plus Number`() {
        val property = 0.0f.toProperty()

        val binding = property + 5
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(5.0f, binding.get(), 10e-5f)

        property.value -= 5f
        Assert.assertEquals(0.0f, binding.get(), 10e-5f)
    }

    @Test fun `FloatExpression plus Double`() {
        val property = 0.0f.toProperty()

        val binding = property + 5.0
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(5.0, binding.get(), 10e-5)

        property.value -= 5f
        Assert.assertEquals(0.0, binding.get(), 10e-5)
    }

    @Test fun `FloatExpression plus NumberProperty`() {
        val property1 = 0.0f.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 + property2
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(5.0f, binding.get(), 10e-5f)

        property1.value -= 10f
        Assert.assertEquals(-5.0f, binding.get(), 10e-5f)

        property2.value = 0
        Assert.assertEquals(-10.0f, binding.get(), 10e-5f)
    }

    @Test fun `FloatExpression plus DoubleProperty`() {
        val property1 = 0.0f.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 + property2
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(5.0, binding.get(), 10e-5)

        property1.value -= 10f
        Assert.assertEquals(-5.0, binding.get(), 10e-5)

        property2.value = 0.0
        Assert.assertEquals(-10.0, binding.get(), 10e-5)
    }

    @Test fun `FloatProperty plusAssign Number`() {
        val property = 0.0f.toProperty()
        property += 5

        Assert.assertEquals(5.0f, property.get(), 10e-5f)
    }

    @Test fun `FloatProperty plusAssign NumberProperty`() {
        val property1 = 0.0f.toProperty()
        val property2 = 5.toProperty()

        property1 += property2
        Assert.assertEquals(5.0f, property1.get(), 10e-5f)
    }

    @Test fun `FloatExpression minus Number`() {
        val property = 0.0f.toProperty()

        val binding = property - 5
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(-5.0f, binding.get(), 10e-5f)

        property.value -= 5f
        Assert.assertEquals(-10.0f, binding.get(), 10e-5f)
    }

    @Test fun `FloatExpression minus Double`() {
        val property = 0.0f.toProperty()

        val binding = property - 5.0
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(-5.0, binding.get(), 10e-5)

        property.value -= 5f
        Assert.assertEquals(-10.0, binding.get(), 10e-5)
    }

    @Test fun `FloatExpression minus NumberProperty`() {
        val property1 = 0.0f.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 - property2
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(-5.0f, binding.get(), 10e-5f)

        property1.value -= 10f
        Assert.assertEquals(-15.0f, binding.get(), 10e-5f)

        property2.value = 0
        Assert.assertEquals(-10.0f, binding.get(), 10e-5f)
    }

    @Test fun `FloatExpression minus DoubleProperty`() {
        val property1 = 0.0f.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 - property2
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(-5.0, binding.get(), 10e-5)

        property1.value -= 10f
        Assert.assertEquals(-15.0, binding.get(), 10e-5)

        property2.value = 0.0
        Assert.assertEquals(-10.0, binding.get(), 10e-5)
    }

    @Test fun `FloatProperty minusAssign Number`() {
        val property = 0.0f.toProperty()
        property -= 5

        Assert.assertEquals(-5.0f, property.get(), 10e-5f)
    }

    @Test fun `FloatProperty minusAssign NumberProperty`() {
        val property1 = 0.0f.toProperty()
        val property2 = 5.toProperty()

        property1 -= property2
        Assert.assertEquals(-5.0f, property1.get(), 10e-5f)
    }

    @Test fun `FloatProperty unaryMinus`() {
        val property = 1.0f.toProperty()

        val binding = -property
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(-1.0f, binding.get(), 10e-5f)

        property += 1
        Assert.assertEquals(-2.0f, binding.get(), 10e-5f)
    }

    @Test fun `FloatExpression times Number`() {
        val property = 2.0f.toProperty()

        val binding = property * 5
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(10.0f, binding.get(), 10e-5f)

        property.value = 5f
        Assert.assertEquals(25.0f, binding.get(), 10e-5f)
    }

    @Test fun `FloatExpression times Double`() {
        val property = 2.0f.toProperty()

        val binding = property * 5.0
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(10.0, binding.get(), 10e-5)

        property.value = 5f
        Assert.assertEquals(25.0, binding.get(), 10e-5)
    }

    @Test fun `FloatExpression times NumberProperty`() {
        val property1 = 2.0f.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 * property2
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(10.0f, binding.get(), 10e-5f)

        property1.value = 10f
        Assert.assertEquals(50.0f, binding.get(), 10e-5f)

        property2.value = 0
        Assert.assertEquals(0.0f, binding.get(), 10e-5f)
    }

    @Test fun `FloatExpression times DoubleProperty`() {
        val property1 = 2.0f.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 * property2
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(10.0, binding.get(), 10e-5)

        property1.value = 10f
        Assert.assertEquals(50.0, binding.get(), 10e-5)

        property2.value = 0.0
        Assert.assertEquals(0.0, binding.get(), 10e-5)
    }

    @Test fun `FloatProperty timesAssign Number`() {
        val property = 1.0f.toProperty()
        property *= 5

        Assert.assertEquals(5.0f, property.get(), 10e-5f)
    }

    @Test fun `FloatProperty timesAssign NumberProperty`() {
        val property1 = 1.0f.toProperty()
        val property2 = 5.toProperty()

        property1 *= property2
        Assert.assertEquals(5.0f, property1.get(), 10e-5f)
    }

    @Test fun `FloatExpression div Number`() {
        val property = 5.0f.toProperty()

        val binding = property / 5
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(1.0f, binding.get(), 10e-5f)

        property.value = 10f
        Assert.assertEquals(2.0f, binding.get(), 10e-5f)
    }

    @Test fun `FloatExpression div Double`() {
        val property = 5.0f.toProperty()

        val binding = property / 5.0
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(1.0, binding.get(), 10e-5)

        property.value = 10f
        Assert.assertEquals(2.0, binding.get(), 10e-5)
    }

    @Test fun `FloatExpression div NumberProperty`() {
        val property1 = 5.0f.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 / property2
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(1.0f, binding.get(), 10e-5f)

        property1.value = 10f
        Assert.assertEquals(2.0f, binding.get(), 10e-5f)

        property2.value = 20
        Assert.assertEquals(0.5f, binding.get(), 10e-5f)
    }

    @Test fun `FloatExpression div DoubleProperty`() {
        val property1 = 5.0f.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 / property2
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(1.0, binding.get(), 10e-5)

        property1.value = 10f
        Assert.assertEquals(2.0, binding.get(), 10e-5)

        property2.value = 20.0
        Assert.assertEquals(0.5, binding.get(), 10e-5)
    }

    @Test fun `FloatProperty divAssign Number`() {
        val property = 5.0f.toProperty()
        property /= 5

        Assert.assertEquals(1.0f, property.get(), 10e-5f)
    }

    @Test fun `FloatProperty divAssign NumberProperty`() {
        val property1 = 5.0f.toProperty()
        val property2 = 5.toProperty()

        property1 /= property2
        Assert.assertEquals(1.0f, property1.get(), 10e-5f)
    }

    @Test fun `FloatExpression rem Number`() {
        val property = 6.0f.toProperty()

        val binding = property % 5
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(1.0f, binding.get(), 10e-5f)

        property.value = 12.0f
        Assert.assertEquals(2.0f, binding.get(), 10e-5f)
    }

    @Test fun `FloatExpression rem Double`() {
        val property = 6.0f.toProperty()

        val binding = property % 5.0
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(1.0, binding.get(), 10e-5)

        property.value = 12.0f
        Assert.assertEquals(2.0, binding.get(), 10e-5)
    }

    @Test fun `FloatExpression rem NumberProperty`() {
        val property1 = 6.0f.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 % property2
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(1.0f, binding.get(), 10e-5f)

        property1.value = 12f
        Assert.assertEquals(2.0f, binding.get(), 10e-5f)

        property2.value = 11
        Assert.assertEquals(1.0f, binding.get(), 10e-5f)
    }

    @Test fun `FloatExpression rem DoubleProperty`() {
        val property1 = 6.0f.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 % property2
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(1.0, binding.get(), 10e-5)

        property1.value = 12f
        Assert.assertEquals(2.0, binding.get(), 10e-5)

        property2.value = 11.0
        Assert.assertEquals(1.0, binding.get(), 10e-5)
    }

    @Test fun `FloatProperty remAssign Number`() {
        val property = 6.0f.toProperty()
        property %= 5

        Assert.assertEquals(1.0f, property.get(), 10e-5f)
    }

    @Test fun `FloatProperty remAssign NumberProperty`() {
        val property1 = 6.0f.toProperty()
        val property2 = 5.toProperty()

        property1 %= property2
        Assert.assertEquals(1.0f, property1.get(), 10e-5f)
    }

    @Test fun `FloatProperty compareTo Number`() {
        val property = 5.0f.toProperty()

        assertTrue(property > 4)
        assertTrue(property >= 4)
        assertTrue(property >= 5)
        assertTrue(property <= 5)
        assertTrue(property <= 6)
        assertTrue(property < 6)

        assertFalse(property > 6)
        assertFalse(property >= 6)
        assertFalse(property <= 4)
        assertFalse(property < 4)
    }

    @Test fun `FloatProperty compareTo NumberProperty`() {
        val property = 5.0f.toProperty()

        assertTrue(property > 4.toProperty())
        assertTrue(property >= 4.toProperty())
        assertTrue(property >= 5.toProperty())
        assertTrue(property <= 5.toProperty())
        assertTrue(property <= 6.toProperty())
        assertTrue(property < 6.toProperty())

        assertFalse(property > 6.toProperty())
        assertFalse(property >= 6.toProperty())
        assertFalse(property <= 4.toProperty())
        assertFalse(property < 4.toProperty())
    }


    // ================================================================
    // Integer Properties

    @Test fun `IntegerExpression plus Int`() {
        val property = 0.toProperty()

        val binding = property + 5
        assertTrue(binding is IntegerBinding)
        assertEquals(5, binding.get())

        property.value -= 5
        assertEquals(0, binding.get())
    }

    @Test fun `IntegerExpression plus Long`() {
        val property = 0.toProperty()

        val binding = property + 5L
        assertTrue(binding is LongBinding)
        assertEquals(5L, binding.get())

        property.value -= 5
        assertEquals(0, binding.get())
    }

    @Test fun `IntegerExpression plus Float`() {
        val property = 0.toProperty()

        val binding = property + 5f
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(5f, binding.get(), 10e-5f)

        property.value -= 5
        Assert.assertEquals(0f, binding.get(), 10e-5f)
    }

    @Test fun `IntegerExpression plus Double`() {
        val property = 0.toProperty()

        val binding = property + 5.0
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(5.0, binding.get(), 10e-5)

        property.value -= 5
        Assert.assertEquals(0.0, binding.get(), 10e-5)
    }

    @Test fun `IntegerExpression plus IntegerProperty`() {
        val property1 = 0.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 + property2
        assertTrue(binding is IntegerBinding)
        assertEquals(5, binding.get())

        property1.value -= 10
        assertEquals(-5, binding.get())

        property2.value = 0
        assertEquals(-10, binding.get())
    }

    @Test fun `IntegerExpression plus LongProperty`() {
        val property1 = 0.toProperty()
        val property2 = 5L.toProperty()

        val binding = property1 + property2
        assertTrue(binding is LongBinding)
        assertEquals(5L, binding.get())

        property1.value -= 10
        assertEquals(-5L, binding.get())

        property2.value = 0L
        assertEquals(-10L, binding.get())
    }

    @Test fun `IntegerExpression plus FloatProperty`() {
        val property1 = 0.toProperty()
        val property2 = 5f.toProperty()

        val binding = property1 + property2
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(5f, binding.get(), 10e-5f)

        property1.value -= 10
        Assert.assertEquals(-5f, binding.get(), 10e-5f)

        property2.value = 0f
        Assert.assertEquals(-10f, binding.get(), 10e-5f)
    }

    @Test fun `IntegerExpression plus DoubleProperty`() {
        val property1 = 0.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 + property2
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(5.0, binding.get(), 10e-5)

        property1.value -= 10
        Assert.assertEquals(-5.0, binding.get(), 10e-5)

        property2.value = 0.0
        Assert.assertEquals(-10.0, binding.get(), 10e-5)
    }

    @Test fun `IntegerProperty plusAssign Number`() {
        val property = 0.toProperty()
        property += 5

        assertEquals(5, property.get())
    }

    @Test fun `IntegerProperty plusAssign NumberProperty`() {
        val property1 = 0.toProperty()
        val property2 = 5.toProperty()

        property1 += property2
        assertEquals(5, property1.get())
    }

    @Test fun `IntegerExpression minus Int`() {
        val property = 0.toProperty()

        val binding = property - 5
        assertTrue(binding is IntegerBinding)
        assertEquals(-5, binding.get())

        property.value -= 5
        assertEquals(-10, binding.get())
    }

    @Test fun `IntegerExpression minus Long`() {
        val property = 0.toProperty()

        val binding = property - 5L
        assertTrue(binding is LongBinding)
        assertEquals(-5L, binding.get())

        property.value -= 5
        assertEquals(-10L, binding.get())
    }

    @Test fun `IntegerExpression minus Float`() {
        val property = 0.toProperty()

        val binding = property - 5f
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(-5f, binding.get(), 10e-5f)

        property.value -= 5
        Assert.assertEquals(-10f, binding.get(), 10e-5f)
    }

    @Test fun `IntegerExpression minus Double`() {
        val property = 0.toProperty()

        val binding = property - 5.0
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(-5.0, binding.get(), 10e-5)

        property.value -= 5
        Assert.assertEquals(-10.0, binding.get(), 10e-5)
    }

    @Test fun `IntegerExpression minus IntegerProperty`() {
        val property1 = 0.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 - property2
        assertTrue(binding is IntegerBinding)
        assertEquals(-5, binding.get())

        property1.value -= 10
        assertEquals(-15, binding.get())

        property2.value = 0
        assertEquals(-10, binding.get())
    }

    @Test fun `IntegerExpression minus LongProperty`() {
        val property1 = 0.toProperty()
        val property2 = 5L.toProperty()

        val binding = property1 - property2
        assertTrue(binding is LongBinding)
        assertEquals(-5L, binding.get())

        property1.value -= 10
        assertEquals(-15L, binding.get())

        property2.value = 0L
        assertEquals(-10L, binding.get())
    }

    @Test fun `IntegerExpression minus FloatProperty`() {
        val property1 = 0.toProperty()
        val property2 = 5f.toProperty()

        val binding = property1 - property2
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(-5f, binding.get(), 10e-5f)

        property1.value -= 10
        Assert.assertEquals(-15f, binding.get(), 10e-5f)

        property2.value = 0f
        Assert.assertEquals(-10f, binding.get(), 10e-5f)
    }

    @Test fun `IntegerExpression minus DoubleProperty`() {
        val property1 = 0.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 - property2
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(-5.0, binding.get(), 10e-5)

        property1.value -= 10
        Assert.assertEquals(-15.0, binding.get(), 10e-5)

        property2.value = 0.0
        Assert.assertEquals(-10.0, binding.get(), 10e-5)
    }

    @Test fun `IntegerProperty minusAssign Number`() {
        val property = 0.toProperty()
        property -= 5

        assertEquals(-5, property.get())
    }

    @Test fun `IntegerProperty minusAssign NumberProperty`() {
        val property1 = 0.toProperty()
        val property2 = 5.toProperty()

        property1 -= property2
        assertEquals(-5, property1.get())
    }

    @Test fun `IntegerProperty unaryMinus`() {
        val property = 1.toProperty()

        val binding = -property
        assertTrue(binding is IntegerBinding)
        assertEquals(-1, binding.get())

        property += 1
        assertEquals(-2, binding.get())
    }

    @Test fun `IntegerExpression times Int`() {
        val property = 2.toProperty()

        val binding = property * 5
        assertTrue(binding is IntegerBinding)
        assertEquals(10, binding.get())

        property.value = 5
        assertEquals(25, binding.get())
    }

    @Test fun `IntegerExpression times Long`() {
        val property = 2.toProperty()

        val binding = property * 5L
        assertTrue(binding is LongBinding)
        assertEquals(10L, binding.get())

        property.value = 5
        assertEquals(25L, binding.get())
    }

    @Test fun `IntegerExpression times Float`() {
        val property = 2.toProperty()

        val binding = property * 5f
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(10f, binding.get(), 10e-5f)

        property.value = 5
        Assert.assertEquals(25f, binding.get(), 10e-5f)
    }

    @Test fun `IntegerExpression times Double`() {
        val property = 2.toProperty()

        val binding = property * 5.0
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(10.0, binding.get(), 10e-5)

        property.value = 5
        Assert.assertEquals(25.0, binding.get(), 10e-5)
    }

    @Test fun `IntegerExpression times IntegerProperty`() {
        val property1 = 2.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 * property2
        assertTrue(binding is IntegerBinding)
        assertEquals(10, binding.get())

        property1.value = 10
        assertEquals(50, binding.get())

        property2.value = 0
        assertEquals(0, binding.get())
    }

    @Test fun `IntegerExpression times LongProperty`() {
        val property1 = 2.toProperty()
        val property2 = 5L.toProperty()

        val binding = property1 * property2
        assertTrue(binding is LongBinding)
        assertEquals(10L, binding.get())

        property1.value = 10
        assertEquals(50L, binding.get())

        property2.value = 0L
        assertEquals(0L, binding.get())
    }

    @Test fun `IntegerExpression times FloatProperty`() {
        val property1 = 2.toProperty()
        val property2 = 5f.toProperty()

        val binding = property1 * property2
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(10f, binding.get(), 10e-5f)

        property1.value = 10
        Assert.assertEquals(50f, binding.get(), 10e-5f)

        property2.value = 0f
        Assert.assertEquals(0f, binding.get(), 10e-5f)
    }

    @Test fun `IntegerExpression times DoubleProperty`() {
        val property1 = 2.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 * property2
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(10.0, binding.get(), 10e-5)

        property1.value = 10
        Assert.assertEquals(50.0, binding.get(), 10e-5)

        property2.value = 0.0
        Assert.assertEquals(0.0, binding.get(), 10e-5)
    }

    @Test fun `IntegerProperty timesAssign Number`() {
        val property = 1.toProperty()
        property *= 5

        assertEquals(5, property.get())
    }

    @Test fun `IntegerProperty timesAssign NumberProperty`() {
        val property1 = 1.toProperty()
        val property2 = 5.toProperty()

        property1 *= property2
        assertEquals(5, property1.get())
    }

    @Test fun `IntegerExpression div Int`() {
        val property = 10.toProperty()

        val binding = property / 5
        assertTrue(binding is IntegerBinding)
        assertEquals(2, binding.get())

        property.value = 20
        assertEquals(4, binding.get())
    }

    @Test fun `IntegerExpression div Long`() {
        val property = 10.toProperty()

        val binding = property / 5L
        assertTrue(binding is LongBinding)
        assertEquals(2L, binding.get())

        property.value = 20
        assertEquals(4L, binding.get())
    }

    @Test fun `IntegerExpression div Float`() {
        val property = 10.toProperty()

        val binding = property / 5f
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(2f, binding.get(), 10e-5f)

        property.value = 20
        Assert.assertEquals(4f, binding.get(), 10e-5f)
    }

    @Test fun `IntegerExpression div Double`() {
        val property = 10.toProperty()

        val binding = property / 5.0
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(2.0, binding.get(), 10e-5)

        property.value = 20
        Assert.assertEquals(4.0, binding.get(), 10e-5)
    }

    @Test fun `IntegerExpression div IntegerProperty`() {
        val property1 = 10.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 / property2
        assertTrue(binding is IntegerBinding)
        assertEquals(2, binding.get())

        property1.value = 20
        assertEquals(4, binding.get())

        property2.value = 20
        assertEquals(1, binding.get())
    }

    @Test fun `IntegerExpression div LongProperty`() {
        val property1 = 10.toProperty()
        val property2 = 5L.toProperty()

        val binding = property1 / property2
        assertTrue(binding is LongBinding)
        assertEquals(2L, binding.get())

        property1.value = 20
        assertEquals(4L, binding.get())

        property2.value = 20L
        assertEquals(1L, binding.get())
    }

    @Test fun `IntegerExpression div FloatProperty`() {
        val property1 = 10.toProperty()
        val property2 = 5f.toProperty()

        val binding = property1 / property2
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(2f, binding.get(), 10e-5f)

        property1.value = 20
        Assert.assertEquals(4f, binding.get(), 10e-5f)

        property2.value = 20f
        Assert.assertEquals(1f, binding.get(), 10e-5f)
    }

    @Test fun `IntegerExpression div DoubleProperty`() {
        val property1 = 10.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 / property2
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(2.0, binding.get(), 10e-5)

        property1.value = 20
        Assert.assertEquals(4.0, binding.get(), 10e-5)

        property2.value = 20.0
        Assert.assertEquals(1.0, binding.get(), 10e-5)
    }

    @Test fun `IntegerProperty divAssign Number`() {
        val property = 5.toProperty()
        property /= 5

        assertEquals(1, property.get())
    }

    @Test fun `IntegerProperty divAssign NumberProperty`() {
        val property1 = 5.toProperty()
        val property2 = 5.toProperty()

        property1 /= property2
        assertEquals(1, property1.get())
    }

    @Test fun `IntegerExpression rem Int`() {
        val property = 6.toProperty()

        val binding = property % 5
        assertTrue(binding is IntegerBinding)
        assertEquals(1, binding.get())

        property.value = 12
        assertEquals(2, binding.get())
    }

    @Test fun `IntegerExpression rem Long`() {
        val property = 6.toProperty()

        val binding = property % 5L
        assertTrue(binding is LongBinding)
        assertEquals(1L, binding.get())

        property.value = 12
        assertEquals(2L, binding.get())
    }

    @Test fun `IntegerExpression rem Float`() {
        val property = 6.toProperty()

        val binding = property % 5f
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(1f, binding.get(), 10e-5f)

        property.value = 12
        Assert.assertEquals(2f, binding.get(), 10e-5f)
    }

    @Test fun `IntegerExpression rem Double`() {
        val property = 6.toProperty()

        val binding = property % 5.0
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(1.0, binding.get(), 10e-5)

        property.value = 12
        Assert.assertEquals(2.0, binding.get(), 10e-5)
    }

    @Test fun `IntegerExpression rem IntegerProperty`() {
        val property1 = 6.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 % property2
        assertTrue(binding is IntegerBinding)
        assertEquals(1, binding.get())

        property1.value = 12
        assertEquals(2, binding.get())

        property2.value = 11
        assertEquals(1, binding.get())
    }

    @Test fun `IntegerExpression rem LongProperty`() {
        val property1 = 6.toProperty()
        val property2 = 5L.toProperty()

        val binding = property1 % property2
        assertTrue(binding is LongBinding)
        assertEquals(1L, binding.get())

        property1.value = 12
        assertEquals(2L, binding.get())

        property2.value = 11L
        assertEquals(1L, binding.get())
    }

    @Test fun `IntegerExpression rem FloatProperty`() {
        val property1 = 6.toProperty()
        val property2 = 5f.toProperty()

        val binding = property1 % property2
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(1f, binding.get(), 10e-5f)

        property1.value = 12
        Assert.assertEquals(2f, binding.get(), 10e-5f)

        property2.value = 11f
        Assert.assertEquals(1f, binding.get(), 10e-5f)
    }

    @Test fun `IntegerExpression rem DoubleProperty`() {
        val property1 = 6.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 % property2
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(1.0, binding.get(), 10e-5)

        property1.value = 12
        Assert.assertEquals(2.0, binding.get(), 10e-5)

        property2.value = 11.0
        Assert.assertEquals(1.0, binding.get(), 10e-5)
    }

    @Test fun `IntegerProperty remAssign Number`() {
        val property = 6.toProperty()
        property %= 5

        assertEquals(1, property.get())
    }

    @Test fun `IntegerProperty remAssign NumberProperty`() {
        val property1 = 6.toProperty()
        val property2 = 5.toProperty()

        property1 %= property2
        assertEquals(1, property1.get())
    }

    @Test fun `IntegerProperty rangeTo Int`() {
        val property = 0.toProperty()
        val sequence = property..9

        var counter = 0
        for (i in sequence) {
            assertEquals(counter++, i.get())
        }

        assertEquals(10, counter)
    }

    @Test fun `IntegerProperty rangeTo IntegerProperty`() {
        val property1 = 0.toProperty()
        val property2 = 9.toProperty()
        val sequence = property1..property2

        var counter = 0
        for (i in sequence) {
            assertEquals(counter++, i.get())
        }

        assertEquals(10, counter)
    }

    @Test fun `IntegerProperty rangeTo Long`() {
        val property = 0.toProperty()
        val sequence = property..9L

        var counter = 0L
        for (i in sequence) {
            assertEquals(counter++, i.get())
        }

        assertEquals(10L, counter)
    }

    @Test fun `IntegerProperty rangeTo LongProperty`() {
        val property1 = 0.toProperty()
        val property2 = 9L.toProperty()
        val sequence = property1..property2

        var counter = 0L
        for (i in sequence) {
            assertEquals(counter++, i.get())
        }

        assertEquals(10L, counter)
    }

    @Test fun `IntegerProperty compareTo Number`() {
        val property = 5.toProperty()

        assertTrue(property > 4)
        assertTrue(property >= 4)
        assertTrue(property >= 5)
        assertTrue(property <= 5)
        assertTrue(property <= 6)
        assertTrue(property < 6)

        assertFalse(property > 6)
        assertFalse(property >= 6)
        assertFalse(property <= 4)
        assertFalse(property < 4)
    }

    @Test fun `IntegerProperty compareTo NumberProperty`() {
        val property = 5.toProperty()

        assertTrue(property > 4.toProperty())
        assertTrue(property >= 4.toProperty())
        assertTrue(property >= 5.toProperty())
        assertTrue(property <= 5.toProperty())
        assertTrue(property <= 6.toProperty())
        assertTrue(property < 6.toProperty())

        assertFalse(property > 6.toProperty())
        assertFalse(property >= 6.toProperty())
        assertFalse(property <= 4.toProperty())
        assertFalse(property < 4.toProperty())
    }


    @Test fun `LongExpression plus Number`() {
        val property = 0L.toProperty()

        val binding = property + 5
        assertTrue(binding is LongBinding)
        assertEquals(5L, binding.get())

        property.value -= 5L
        assertEquals(0L, binding.get())
    }

    @Test fun `LongExpression plus Float`() {
        val property = 0L.toProperty()

        val binding = property + 5f
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(5f, binding.get(), 10e-5f)

        property.value -= 5L
        Assert.assertEquals(0f, binding.get(), 10e-5f)
    }

    @Test fun `LongExpression plus Double`() {
        val property = 0L.toProperty()

        val binding = property + 5.0
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(5.0, binding.get(), 10e-5)

        property.value -= 5L
        Assert.assertEquals(0.0, binding.get(), 10e-5)
    }

    @Test fun `LongExpression plus NumberProperty`() {
        val property1 = 0L.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 + property2
        assertTrue(binding is LongBinding)
        assertEquals(5L, binding.get())

        property1.value -= 10L
        assertEquals(-5L, binding.get())

        property2.value = 0
        assertEquals(-10L, binding.get())
    }

    @Test fun `LongExpression plus FloatProperty`() {
        val property1 = 0L.toProperty()
        val property2 = 5f.toProperty()

        val binding = property1 + property2
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(5f, binding.get(), 10e-5f)

        property1.value -= 10L
        Assert.assertEquals(-5f, binding.get(), 10e-5f)

        property2.value = 0f
        Assert.assertEquals(-10f, binding.get(), 10e-5f)
    }

    @Test fun `LongExpression plus DoubleProperty`() {
        val property1 = 0L.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 + property2
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(5.0, binding.get(), 10e-5)

        property1.value -= 10L
        Assert.assertEquals(-5.0, binding.get(), 10e-5)

        property2.value = 0.0
        Assert.assertEquals(-10.0, binding.get(), 10e-5)
    }

    @Test fun `LongProperty plusAssign Number`() {
        val property = 0L.toProperty()
        property += 5

        assertEquals(5L, property.get())
    }

    @Test fun `LongProperty plusAssign NumberProperty`() {
        val property1 = 0L.toProperty()
        val property2 = 5.toProperty()

        property1 += property2
        assertEquals(5L, property1.get())
    }

    @Test fun `LongExpression minus Number`() {
        val property = 0L.toProperty()

        val binding = property - 5
        assertTrue(binding is LongBinding)
        assertEquals(-5L, binding.get())

        property.value -= 5L
        assertEquals(-10L, binding.get())
    }

    @Test fun `LongExpression minus Float`() {
        val property = 0L.toProperty()

        val binding = property - 5f
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(-5f, binding.get(), 10e-5f)

        property.value -= 5L
        Assert.assertEquals(-10f, binding.get(), 10e-5f)
    }

    @Test fun `LongExpression minus Double`() {
        val property = 0L.toProperty()

        val binding = property - 5.0
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(-5.0, binding.get(), 10e-5)

        property.value -= 5L
        Assert.assertEquals(-10.0, binding.get(), 10e-5)
    }

    @Test fun `LongExpression minus NumberProperty`() {
        val property1 = 0L.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 - property2
        assertTrue(binding is LongBinding)
        assertEquals(-5L, binding.get())

        property1.value -= 10L
        assertEquals(-15L, binding.get())

        property2.value = 0
        assertEquals(-10L, binding.get())
    }

    @Test fun `LongExpression minus FloatProperty`() {
        val property1 = 0L.toProperty()
        val property2 = 5f.toProperty()

        val binding = property1 - property2
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(-5f, binding.get(), 10e-5f)

        property1.value -= 10L
        Assert.assertEquals(-15f, binding.get(), 10e-5f)

        property2.value = 0f
        Assert.assertEquals(-10f, binding.get(), 10e-5f)
    }

    @Test fun `LongExpression minus DoubleProperty`() {
        val property1 = 0L.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 - property2
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(-5.0, binding.get(), 10e-5)

        property1.value -= 10L
        Assert.assertEquals(-15.0, binding.get(), 10e-5)

        property2.value = 0.0
        Assert.assertEquals(-10.0, binding.get(), 10e-5)
    }

    @Test fun `LongProperty minusAssign Number`() {
        val property = 0L.toProperty()
        property -= 5

        assertEquals(-5L, property.get())
    }

    @Test fun `LongProperty minusAssign NumberProperty`() {
        val property1 = 0L.toProperty()
        val property2 = 5.toProperty()

        property1 -= property2
        assertEquals(-5L, property1.get())
    }

    @Test fun `LongProperty unaryMinus`() {
        val property = 1L.toProperty()

        val binding = -property
        assertTrue(binding is LongBinding)
        assertEquals(-1L, binding.get())

        property += 1
        assertEquals(-2L, binding.get())
    }

    @Test fun `LongExpression times Number`() {
        val property = 2L.toProperty()

        val binding = property * 5
        assertTrue(binding is LongBinding)
        assertEquals(10L, binding.get())

        property.value = 5L
        assertEquals(25L, binding.get())
    }

    @Test fun `LongExpression times Float`() {
        val property = 2L.toProperty()

        val binding = property * 5f
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(10f, binding.get(), 10e-5f)

        property.value = 5L
        Assert.assertEquals(25f, binding.get(), 10e-5f)
    }

    @Test fun `LongExpression times Double`() {
        val property = 2L.toProperty()

        val binding = property * 5.0
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(10.0, binding.get(), 10e-5)

        property.value = 5L
        Assert.assertEquals(25.0, binding.get(), 10e-5)
    }

    @Test fun `LongExpression times NumberProperty`() {
        val property1 = 2L.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 * property2
        assertTrue(binding is LongBinding)
        assertEquals(10L, binding.get())

        property1.value = 10L
        assertEquals(50L, binding.get())

        property2.value = 0
        assertEquals(0L, binding.get())
    }

    @Test fun `LongExpression times FloatProperty`() {
        val property1 = 2L.toProperty()
        val property2 = 5f.toProperty()

        val binding = property1 * property2
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(10f, binding.get(), 10e-5f)

        property1.value = 10L
        Assert.assertEquals(50f, binding.get(), 10e-5f)

        property2.value = 0f
        Assert.assertEquals(0f, binding.get(), 10e-5f)
    }

    @Test fun `LongExpression times DoubleProperty`() {
        val property1 = 2L.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 * property2
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(10.0, binding.get(), 10e-5)

        property1.value = 10L
        Assert.assertEquals(50.0, binding.get(), 10e-5)

        property2.value = 0.0
        Assert.assertEquals(0.0, binding.get(), 10e-5)
    }

    @Test fun `LongProperty timesAssign Number`() {
        val property = 1L.toProperty()
        property *= 5

        assertEquals(5L, property.get())
    }

    @Test fun `LongProperty timesAssign NumberProperty`() {
        val property1 = 1L.toProperty()
        val property2 = 5.toProperty()

        property1 *= property2
        assertEquals(5L, property1.get())
    }

    @Test fun `LongExpression div Number`() {
        val property = 10L.toProperty()

        val binding = property / 5
        assertTrue(binding is LongBinding)
        assertEquals(2L, binding.get())

        property.value = 20L
        assertEquals(4L, binding.get())
    }

    @Test fun `LongExpression div Float`() {
        val property = 10L.toProperty()

        val binding = property / 5f
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(2f, binding.get(), 10e-5f)

        property.value = 20L
        Assert.assertEquals(4f, binding.get(), 10e-5f)
    }

    @Test fun `LongExpression div Double`() {
        val property = 10L.toProperty()

        val binding = property / 5.0
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(2.0, binding.get(), 10e-5)

        property.value = 20L
        Assert.assertEquals(4.0, binding.get(), 10e-5)
    }

    @Test fun `LongExpression div NumberProperty`() {
        val property1 = 10L.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 / property2
        assertTrue(binding is LongBinding)
        assertEquals(2L, binding.get())

        property1.value = 20L
        assertEquals(4L, binding.get())

        property2.value = 20
        assertEquals(1L, binding.get())
    }

    @Test fun `LongExpression div FloatProperty`() {
        val property1 = 10L.toProperty()
        val property2 = 5f.toProperty()

        val binding = property1 / property2
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(2f, binding.get(), 10e-5f)

        property1.value = 20L
        Assert.assertEquals(4f, binding.get(), 10e-5f)

        property2.value = 20f
        Assert.assertEquals(1f, binding.get(), 10e-5f)
    }

    @Test fun `LongExpression div DoubleProperty`() {
        val property1 = 10L.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 / property2
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(2.0, binding.get(), 10e-5)

        property1.value = 20L
        Assert.assertEquals(4.0, binding.get(), 10e-5)

        property2.value = 20.0
        Assert.assertEquals(1.0, binding.get(), 10e-5)
    }

    @Test fun `LongProperty divAssign Number`() {
        val property = 5L.toProperty()
        property /= 5

        assertEquals(1L, property.get())
    }

    @Test fun `LongProperty divAssign NumberProperty`() {
        val property1 = 5L.toProperty()
        val property2 = 5.toProperty()

        property1 /= property2
        assertEquals(1L, property1.get())
    }

    @Test fun `LongExpression rem Number`() {
        val property = 6L.toProperty()

        val binding = property % 5
        assertTrue(binding is LongBinding)
        assertEquals(1L, binding.get())

        property.value = 12L
        assertEquals(2L, binding.get())
    }

    @Test fun `LongExpression rem Float`() {
        val property = 6L.toProperty()

        val binding = property % 5f
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(1f, binding.get(), 10e-5f)

        property.value = 12L
        Assert.assertEquals(2f, binding.get(), 10e-5f)
    }

    @Test fun `LongExpression rem Double`() {
        val property = 6L.toProperty()

        val binding = property % 5.0
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(1.0, binding.get(), 10e-5)

        property.value = 12L
        Assert.assertEquals(2.0, binding.get(), 10e-5)
    }

    @Test fun `LongExpression rem NumberProperty`() {
        val property1 = 6L.toProperty()
        val property2 = 5.toProperty()

        val binding = property1 % property2
        assertTrue(binding is LongBinding)
        assertEquals(1L, binding.get())

        property1.value = 12L
        assertEquals(2L, binding.get())

        property2.value = 11
        assertEquals(1L, binding.get())
    }

    @Test fun `LongExpression rem FloatProperty`() {
        val property1 = 6L.toProperty()
        val property2 = 5f.toProperty()

        val binding = property1 % property2
        assertTrue(binding is FloatBinding)
        Assert.assertEquals(1f, binding.get(), 10e-5f)

        property1.value = 12L
        Assert.assertEquals(2f, binding.get(), 10e-5f)

        property2.value = 11f
        Assert.assertEquals(1f, binding.get(), 10e-5f)
    }

    @Test fun `LongExpression rem DoubleProperty`() {
        val property1 = 6L.toProperty()
        val property2 = 5.0.toProperty()

        val binding = property1 % property2
        assertTrue(binding is DoubleBinding)
        Assert.assertEquals(1.0, binding.get(), 10e-5)

        property1.value = 12L
        Assert.assertEquals(2.0, binding.get(), 10e-5)

        property2.value = 11.0
        Assert.assertEquals(1.0, binding.get(), 10e-5)
    }

    @Test fun `LongProperty remAssign Number`() {
        val property = 6L.toProperty()
        property %= 5

        assertEquals(1L, property.get())
    }

    @Test fun `LongProperty remAssign NumberProperty`() {
        val property1 = 6L.toProperty()
        val property2 = 5.toProperty()

        property1 %= property2
        assertEquals(1L, property1.get())
    }

    @Test fun `LongProperty rangeTo Int`() {
        val property = 0L.toProperty()
        val sequence = property..9

        var counter = 0L
        for (i in sequence) {
            assertEquals(counter++, i.get())
        }

        assertEquals(10L, counter)
    }

    @Test fun `LongProperty rangeTo IntegerProperty`() {
        val property1 = 0L.toProperty()
        val property2 = 9.toProperty()
        val sequence = property1..property2

        var counter = 0L
        for (i in sequence) {
            assertEquals(counter++, i.get())
        }

        assertEquals(10L, counter)
    }

    @Test fun `LongProperty rangeTo Long`() {
        val property = 0L.toProperty()
        val sequence = property..9L

        var counter = 0L
        for (i in sequence) {
            assertEquals(counter++, i.get())
        }

        assertEquals(10L, counter)
    }

    @Test fun `LongProperty rangeTo LongProperty`() {
        val property1 = 0L.toProperty()
        val property2 = 9L.toProperty()
        val sequence = property1..property2

        var counter = 0L
        for (i in sequence) {
            assertEquals(counter++, i.get())
        }

        assertEquals(10L, counter)
    }

    @Test fun `LongProperty compareTo Number`() {
        val property = 5L.toProperty()

        assertTrue(property > 4)
        assertTrue(property >= 4)
        assertTrue(property >= 5)
        assertTrue(property <= 5)
        assertTrue(property <= 6)
        assertTrue(property < 6)

        assertFalse(property > 6)
        assertFalse(property >= 6)
        assertFalse(property <= 4)
        assertFalse(property < 4)
    }

    @Test fun `LongProperty compareTo NumberProperty`() {
        val property = 5L.toProperty()

        assertTrue(property > 4.toProperty())
        assertTrue(property >= 4.toProperty())
        assertTrue(property >= 5.toProperty())
        assertTrue(property <= 5.toProperty())
        assertTrue(property <= 6.toProperty())
        assertTrue(property < 6.toProperty())

        assertFalse(property > 6.toProperty())
        assertFalse(property >= 6.toProperty())
        assertFalse(property <= 4.toProperty())
        assertFalse(property < 4.toProperty())
    }


    @Test fun `NumberExpression gt Int`() {
        val property = (-1).toProperty()

        val binding = property gt 0
        assertFalse(binding.get())

        property.value = 0
        assertFalse(binding.get())

        property.value = 1
        assertTrue(binding.get())
    }

    @Test fun `NumberExpression gt Long`() {
        val property = (-1).toProperty()

        val binding = property gt 0L
        assertFalse(binding.get())

        property.value = 0
        assertFalse(binding.get())

        property.value = 1
        assertTrue(binding.get())
    }

    @Test fun `NumberExpression gt Float`() {
        val property = (-1).toProperty()

        val binding = property gt 0f
        assertFalse(binding.get())

        property.value = 0
        assertFalse(binding.get())

        property.value = 1
        assertTrue(binding.get())
    }

    @Test fun `NumberExpression gt Double`() {
        val property = (-1).toProperty()

        val binding = property gt 0.0
        assertFalse(binding.get())

        property.value = 0
        assertFalse(binding.get())

        property.value = 1
        assertTrue(binding.get())
    }

    @Test fun `NumberExpression gt NumberProperty`() {
        val property = (-1).toProperty()

        val binding = property gt 0.0.toProperty()
        assertFalse(binding.get())

        property.value = 0
        assertFalse(binding.get())

        property.value = 1
        assertTrue(binding.get())
    }

    @Test fun `NumberExpression ge Int`() {
        val property = (-1).toProperty()

        val binding = property ge 0
        assertFalse(binding.get())

        property.value = 0
        assertTrue(binding.get())

        property.value = 1
        assertTrue(binding.get())
    }

    @Test fun `NumberExpression ge Long`() {
        val property = (-1).toProperty()

        val binding = property ge 0L
        assertFalse(binding.get())

        property.value = 0
        assertTrue(binding.get())

        property.value = 1
        assertTrue(binding.get())
    }

    @Test fun `NumberExpression ge Float`() {
        val property = (-1).toProperty()

        val binding = property ge 0f
        assertFalse(binding.get())

        property.value = 0
        assertTrue(binding.get())

        property.value = 1
        assertTrue(binding.get())
    }

    @Test fun `NumberExpression ge Double`() {
        val property = (-1).toProperty()

        val binding = property ge 0.0
        assertFalse(binding.get())

        property.value = 0
        assertTrue(binding.get())

        property.value = 1
        assertTrue(binding.get())
    }

    @Test fun `NumberExpression ge NumberProperty`() {
        val property = (-1).toProperty()

        val binding = property ge 0.0.toProperty()
        assertFalse(binding.get())

        property.value = 0
        assertTrue(binding.get())

        property.value = 1
        assertTrue(binding.get())
    }

    @Test fun `NumberExpression eq Int`() {
        val property = (-1).toProperty()

        val binding = property eq 0
        assertFalse(binding.get())

        property.value = 0
        assertTrue(binding.get())

        property.value = 1
        assertFalse(binding.get())
    }

    @Test fun `NumberExpression eq Long`() {
        val property = (-1).toProperty()

        val binding = property eq 0L
        assertFalse(binding.get())

        property.value = 0
        assertTrue(binding.get())

        property.value = 1
        assertFalse(binding.get())
    }

    @Test fun `NumberExpression eq NumberProperty`() {
        val property = (-1).toProperty()

        val binding = property eq 0.0.toProperty()
        assertFalse(binding.get())

        property.value = 0
        assertTrue(binding.get())

        property.value = 1
        assertFalse(binding.get())
    }

    @Test fun `NumberExpression le Int`() {
        val property = (-1).toProperty()

        val binding = property le 0
        assertTrue(binding.get())

        property.value = 0
        assertTrue(binding.get())

        property.value = 1
        assertFalse(binding.get())
    }

    @Test fun `NumberExpression le Long`() {
        val property = (-1).toProperty()

        val binding = property le 0L
        assertTrue(binding.get())

        property.value = 0
        assertTrue(binding.get())

        property.value = 1
        assertFalse(binding.get())
    }

    @Test fun `NumberExpression le Float`() {
        val property = (-1).toProperty()

        val binding = property le 0f
        assertTrue(binding.get())

        property.value = 0
        assertTrue(binding.get())

        property.value = 1
        assertFalse(binding.get())
    }

    @Test fun `NumberExpression le Double`() {
        val property = (-1).toProperty()

        val binding = property le 0.0
        assertTrue(binding.get())

        property.value = 0
        assertTrue(binding.get())

        property.value = 1
        assertFalse(binding.get())
    }

    @Test fun `NumberExpression le NumberProperty`() {
        val property = (-1).toProperty()

        val binding = property le 0.0.toProperty()
        assertTrue(binding.get())

        property.value = 0
        assertTrue(binding.get())

        property.value = 1
        assertFalse(binding.get())
    }

    @Test fun `NumberExpression lt Int`() {
        val property = (-1).toProperty()

        val binding = property lt 0
        assertTrue(binding.get())

        property.value = 0
        assertFalse(binding.get())

        property.value = 1
        assertFalse(binding.get())
    }

    @Test fun `NumberExpression lt Long`() {
        val property = (-1).toProperty()

        val binding = property lt 0L
        assertTrue(binding.get())

        property.value = 0
        assertFalse(binding.get())

        property.value = 1
        assertFalse(binding.get())
    }

    @Test fun `NumberExpression lt Float`() {
        val property = (-1).toProperty()

        val binding = property lt 0f
        assertTrue(binding.get())

        property.value = 0
        assertFalse(binding.get())

        property.value = 1
        assertFalse(binding.get())
    }

    @Test fun `NumberExpression lt Double`() {
        val property = (-1).toProperty()

        val binding = property lt 0.0
        assertTrue(binding.get())

        property.value = 0
        assertFalse(binding.get())

        property.value = 1
        assertFalse(binding.get())
    }

    @Test fun `NumberExpression lt NumberProperty`() {
        val property = (-1).toProperty()

        val binding = property lt 0.0.toProperty()
        assertTrue(binding.get())

        property.value = 0
        assertFalse(binding.get())

        property.value = 1
        assertFalse(binding.get())
    }


    @Test fun `BooleanExpression not`() {
        val property = true.toProperty()

        val binding = !property
        assertFalse(binding.get())

        property.value = false
        assertTrue(binding.get())
    }

    @Test fun `BooleanExpression and Boolean`() {
        val property = true.toProperty()

        val binding = property and true
        assertTrue(binding.get())

        property.value = false
        assertFalse(binding.get())
    }

    @Test fun `BooleanExpression and BooleanProperty`() {
        val property1 = true.toProperty()
        val property2 = true.toProperty()

        val binding = property1 and property2
        assertTrue(binding.get())

        property1.value = false
        assertFalse(binding.get())

        property1.value = true
        assertTrue(binding.get())

        property2.value = false
        assertFalse(binding.get())
    }

    @Test fun `BooleanExpression or Boolean`() {
        val property = false.toProperty()

        val binding = property or false
        assertFalse(binding.get())

        property.value = true
        assertTrue(binding.get())
    }

    @Test fun `BooleanExpression or BooleanProperty`() {
        val property1 = false.toProperty()
        val property2 = false.toProperty()

        val binding = property1 or property2
        assertFalse(binding.get())

        property1.value = true
        assertTrue(binding.get())

        property2.value = true
        assertTrue(binding.get())
    }

    @Test fun `BooleanExpression xor Boolean`() {
        val property = false.toProperty()

        val binding = property xor true
        assertTrue(binding.get())

        property.value = true
        assertFalse(binding.get())
    }

    @Test fun `BooleanExpression xor BooleanProperty`() {
        val property1 = false.toProperty()
        val property2 = false.toProperty()

        val binding = property1 xor property2
        assertFalse(binding.get())

        property1.value = true
        assertTrue(binding.get())

        property2.value = true
        assertFalse(binding.get())
    }

    @Test fun `BooleanExpression eq Boolean`() {
        val property = false.toProperty()

        val binding = property eq false
        assertTrue(binding.get())

        property.value = true
        assertFalse(binding.get())
    }

    @Test fun `BooleanExpression eq BooleanProperty`() {
        val property1 = false.toProperty()
        val property2 = false.toProperty()

        val binding = property1 eq property2
        assertTrue(binding.get())

        property1.value = true
        assertFalse(binding.get())

        property2.value = true
        assertTrue(binding.get())
    }


    @Test fun `StringExpression plus Any`() {
        val property = "Hello ".toProperty()
        val binding = property + "World!"

        assertEquals("Hello World!", binding.get())

        property.value = "Bye "

        assertEquals("Bye World!", binding.get())
    }

    @Test fun `StringProperty plusAssign Any`() {
        val property = "Hello ".toProperty()

        assertEquals("Hello ", property.get())

        property += "World!"

        assertEquals("Hello World!", property.get())
    }

    @Test fun `StringExpression unaryMinus`() {
        val property = "god a ward".toProperty()

        val binding = -property
        assertEquals("draw a dog", binding.get())

        property.value = "dog a ward"
        assertEquals("draw a god", binding.get())
    }

    @Test fun `StringExpression compareTo String`() {
        val property = "Bravo".toProperty()

        assertTrue(property > "Alpha")
        assertTrue(property >= "Alpha")
        assertTrue(property >= "Bravo")
        assertTrue(property <= "Bravo")
        assertTrue(property <= "Charlie")
        assertTrue(property < "Charlie")

        assertFalse(property < "Alpha")
        assertFalse(property <= "Alpha")
        assertFalse(property >= "Charlie")
        assertFalse(property > "Charlie")
    }

    @Test fun `StringExpression compareTo StringProperty`() {
        val property = "Bravo".toProperty()

        assertTrue(property > "Alpha".toProperty())
        assertTrue(property >= "Alpha".toProperty())
        assertTrue(property >= "Bravo".toProperty())
        assertTrue(property <= "Bravo".toProperty())
        assertTrue(property <= "Charlie".toProperty())
        assertTrue(property < "Charlie".toProperty())

        assertFalse(property < "Alpha".toProperty())
        assertFalse(property <= "Alpha".toProperty())
        assertFalse(property >= "Charlie".toProperty())
        assertFalse(property > "Charlie".toProperty())
    }

    @Test fun `StringExpression ge tInt`() {
        val property = "Hello World!".toProperty()

        val binding = property[0]
        assertEquals('H', binding.value)

        property.value = "Bye World!"
        assertEquals('B', binding.value)
    }

    @Test fun `StringExpression ge tIntProperty`() {
        val property = "Hello World!".toProperty()
        val indexProperty = 0.toProperty()

        val binding = property[indexProperty]
        assertEquals('H', binding.value)

        property.value = "Bye World!"
        assertEquals('B', binding.value)

        indexProperty.value = 1
        assertEquals('y', binding.value)
    }

    @Test fun `StringExpression ge tIntToInt`() {
        val property = "foo()".toProperty()

        val binding = property[0, 3]
        assertEquals("foo", binding.get())

        property.value = "bar()"
        assertEquals("bar", binding.get())
    }

    @Test fun `StringExpression ge tIntegerPropertyToInt`() {
        val property = "foo()".toProperty()
        val startIndex = 0.toProperty()

        val binding = property[startIndex, 3]
        assertEquals("foo", binding.get())

        property.value = "bar()"
        assertEquals("bar", binding.get())

        startIndex.value = 1
        assertEquals("ar", binding.get())
    }

    @Test fun `StringExpression ge tIntToIntegerProperty`() {
        val property = "foo()".toProperty()
        val endIndex = 3.toProperty()

        val binding = property[0, endIndex]
        assertEquals("foo", binding.get())

        property.value = "bar()"
        assertEquals("bar", binding.get())

        endIndex.value = 5
        assertEquals("bar()", binding.get())
    }

    @Test fun `StringExpression ge tIntegerPropertyToIntegerProperty`() {
        val property = "foo()".toProperty()
        val startIndex = 0.toProperty()
        val endIndex = 3.toProperty()

        val binding = property[startIndex, endIndex]
        assertEquals("foo", binding.get())

        property.value = "bar()"
        assertEquals("bar", binding.get())

        startIndex.value = 3
        endIndex.value = 5
        assertEquals("()", binding.get())
    }

    @Test fun `StringExpression gt String`() {
        val property = "Bravo".toProperty()

        val binding = property gt "Alpha"
        assertTrue(binding.get())

        property.value = "Alpha"
        assertFalse(binding.get())
    }

    @Test fun `StringExpression gt StringProperty`() {
        val property1 = "Charlie".toProperty()
        val property2 = "Bravo".toProperty()

        val binding = property1 gt property2
        assertTrue(binding.get())

        property1.value = "Bravo"
        assertFalse(binding.get())

        property2.value = "Alpha"
        assertTrue(binding.get())
    }

    @Test fun `StringExpression ge String`() {
        val property = "Charlie".toProperty()

        val binding = property ge "Bravo"
        assertTrue(binding.get())

        property.value = "Bravo"
        assertTrue(binding.get())

        property.value = "Alpha"
        assertFalse(binding.get())
    }

    @Test fun `StringExpression ge StringProperty`() {
        val property1 = "Charlie".toProperty()
        val property2 = "Bravo".toProperty()

        val binding = property1 ge property2
        assertTrue(binding.get())

        property1.value = "Bravo"
        assertTrue(binding.get())

        property2.value = "Alpha"
        assertTrue(binding.get())

        property2.value = "Charlie"
        assertFalse(binding.get())
    }

    @Test fun `StringExpression eq String`() {
        val property = "Bravo".toProperty()

        val binding = property eq "Bravo"
        assertTrue(binding.get())

        property.value = "Alpha"
        assertFalse(binding.get())
    }

    @Test fun `StringExpression eq StringProperty`() {
        val property1 = "Bravo".toProperty()
        val property2 = "Bravo".toProperty()

        val binding = property1 eq property2
        assertTrue(binding.get())

        property1.value = "Alpha"
        assertFalse(binding.get())

        property2.value = "Alpha"
        assertTrue(binding.get())
    }

    @Test fun `StringExpression eqIgnoreCase String`() {
        val property = "Hello World!".toProperty()

        val binding = property eqIgnoreCase "hello world!"
        assertTrue(binding.get())

        property.value = "Bye World!"
        assertFalse(binding.get())
    }

    @Test fun `StringExpression eqIgnoreCase StringProperty`() {
        val property1 = "Hello World!".toProperty()
        val property2 = "hello world!".toProperty()

        val binding = property1 eqIgnoreCase property2
        assertTrue(binding.get())

        property1.value = "bye world!"
        assertFalse(binding.get())

        property2.value = "Bye World!"
        assertTrue(binding.get())
    }

    @Test fun `StringExpression le String`() {
        val property = "Alpha".toProperty()

        val binding = property le "Bravo"
        assertTrue(binding.get())

        property.value = "Bravo"
        assertTrue(binding.get())

        property.value = "Charlie"
        assertFalse(binding.get())
    }

    @Test fun `StringExpression le StringProperty`() {
        val property1 = "Alpha".toProperty()
        val property2 = "Bravo".toProperty()

        val binding = property1 le property2
        assertTrue(binding.get())

        property1.value = "Bravo"
        assertTrue(binding.get())

        property2.value = "Charlie"
        assertTrue(binding.get())

        property2.value = "Alpha"
        assertFalse(binding.get())
    }

    @Test fun `StringExpression lt String`() {
        val property = "Alpha".toProperty()

        val binding = property lt "Bravo"
        assertTrue(binding.get())

        property.value = "Bravo"
        assertFalse(binding.get())

        property.value = "Charlie"
        assertFalse(binding.get())
    }

    @Test fun `StringExpression lt StringProperty`() {
        val property1 = "Alpha".toProperty()
        val property2 = "Bravo".toProperty()

        val binding = property1 lt property2
        assertTrue(binding.get())

        property1.value = "Bravo"
        assertFalse(binding.get())

        property2.value = "Charlie"
        assertTrue(binding.get())

        property2.value = "Alpha"
        assertFalse(binding.get())
    }

//    class ListHolder {
//        val listProperty: ListProperty<String> = SimpleListProperty<String>(FXCollections.observableArrayList())
//        var list: MutableList<String> by listProperty
//    }
//
//    @fun `listPropertyDelegateModifyList`() {
//        val listHolder = ListHolder()
//        var notified = false
//        listHolder.listProperty.addListener { _, _, _-> notified = true }
//
//        listHolder.list.add("Test")
//        assertTrue(notified)
//
//        notified = false
//        listHolder.list.remove("Test")
//        assertTrue(notified)
//
//        notified = false
//        listHolder.list.addAll(arrayOf("1", "2"))
//        assertTrue(notified)
//
//        notified = false
//        listHolder.list.clear()
//        assertTrue(notified)
//    }
//
//    @fun `listPropertyDelegateChangeList`() {
//        val listHolder = ListHolder()
//        var notified = false
//        listHolder.listProperty.addListener { _, _, _-> notified = true }
//
//        listHolder.list = mutableListOf("Test")
//        assertTrue(notified)
//    }
//
//    class SetHolder {
//        val setProperty: SetProperty<String> = SimpleSetProperty<String>(FXCollections.observableSet())
//        var set: MutableSet<String> by setProperty
//    }
//
//    @fun `setPropertyDelegateModifySet`() {
//        val setHolder = SetHolder()
//        var notified = false
//        setHolder.setProperty.addListener { _, _, _-> notified = true }
//
//        setHolder.set.add("Test")
//        assertTrue(notified)
//
//        notified = false
//        setHolder.set.remove("Test")
//        assertTrue(notified)
//
//        notified = false
//        setHolder.set.addAll(arrayOf("1", "2"))
//        assertTrue(notified)
//
//        notified = false
//        setHolder.set.clear()
//        assertTrue(notified)
//    }
//
//    @fun `setPropertyDelegateChangeSet`() {
//        val setHolder = SetHolder()
//        var notified = false
//        setHolder.setProperty.addListener { _, _, _-> notified = true }
//
//        setHolder.set = mutableSetOf("Test")
//        assertTrue(notified)
//    }
//
//    class MapHolder {
//        val mapProperty: MapProperty<Int, String> = SimpleMapProperty<Int, String>(FXCollections.observableHashMap())
//        var map: MutableMap<Int, String> by mapProperty
//    }
//
//    @fun `mapPropertyDelegateModifyMap`() {
//        val mapHolder = MapHolder()
//        var notified = false
//        mapHolder.mapProperty.addListener { _, _, _-> notified = true }
//
//        mapHolder.map.put(0, "Test")
//        assertTrue(notified)
//
//        notified = false
//        mapHolder.map.remove(0)
//        assertTrue(notified)
//
//        notified = false
//        mapHolder.map.putAll(mapOf(1 to "1", 2 to "2"))
//        assertTrue(notified)
//
//        notified = false
//        mapHolder.map.clear()
//        assertTrue(notified)
//    }
//
//    @fun `mapPropertyDelegateChangeMap`() {
//        val mapHolder = MapHolder()
//        var notified = false
//        mapHolder.mapProperty.addListener { _, _, _-> notified = true }
//
//        mapHolder.map = mutableMapOf(0 to "Test")
//        assertTrue(notified)
//    }
}
