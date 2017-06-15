package tornadofx.tests

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
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

        val idObservable = person.observable( JavaPerson::getId )
        val nameObservable = person.observable<String>("name")
        val idBinding = idObservable.integerBinding{ idObservable.value }

        idObservable.value = 44
        nameObservable.value = "Doe"
        Assert.assertEquals(44, idBinding.value )
        Assert.assertEquals(44, person.id)
        Assert.assertEquals("Doe", person.name)

        person.id = 5
        // property change events on the pojo are propogated
        Assert.assertEquals( 5, idBinding.value )
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
}

class DoublePropertyTest {
    val startDouble = 0.1
    val otherInt = 2
    val otherDouble = 2.1

    val otherIntegerProperty = SimpleIntegerProperty(otherInt)
    val otherDoubleProperty = SimpleDoubleProperty(otherDouble)

    val doubleAddResult = startDouble + otherDouble
    val integerAddResult = startDouble + otherInt

    @Test
    fun plus() {
        val property1 = SimpleDoubleProperty(startDouble)
        val otherProperty1 = property1 + otherDoubleProperty
        assertEquals(doubleAddResult, otherProperty1.value)

        val property2 = SimpleDoubleProperty(startDouble)
        val otherProperty2 = property2 + otherDouble
        assertEquals(doubleAddResult, otherProperty2.value)

        val property3 = SimpleDoubleProperty(startDouble)
        val otherProperty3 = property3 + otherIntegerProperty
        assertEquals(integerAddResult, otherProperty3.value)

        val property4 = SimpleDoubleProperty(startDouble)
        val otherProperty4 = property4 + otherInt
        assertEquals(integerAddResult, otherProperty4.value)
    }

    @Test
    fun plusAssign() {
        val property1: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty1 = property1
        property1 += otherDoubleProperty
        assert(otherProperty1 === property1)
        assertEquals(doubleAddResult, property1.value)

        val property2: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty2 = property2
        property2 += otherDouble
        assert(otherProperty2 === property2)
        assertEquals(doubleAddResult, property2.value)

        val property3: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty3 = property3
        property3 += otherIntegerProperty
        assert(otherProperty3 === property3)
        assertEquals(integerAddResult, property3.value)

        val property4: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty4 = property4
        property4 += otherInt
        assert(otherProperty4 === property4)
        assertEquals(integerAddResult, property4.value)
    }

    @Test
    fun inc() {
        var property: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty = property
        property++
        assert(otherProperty === property)
        assertEquals(startDouble + 1, property.value)
    }


    val doubleSubResult = startDouble - otherDouble
    val integerSubResult = startDouble - otherInt

    @Test
    fun minus() {
        val property1: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty1 = property1 - otherDoubleProperty
        assertEquals(doubleSubResult, otherProperty1.value)

        val property2: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty2 = property2 - otherDouble
        assertEquals(doubleSubResult, otherProperty2.value)

        val property3: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty3 = property3 - otherIntegerProperty
        assertEquals(integerSubResult, otherProperty3.value)

        val property4: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty4 = property4 - otherInt
        assertEquals(integerSubResult, otherProperty4.value)
    }

    @Test
    fun minusAssign() {
        val property1: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty1 = property1
        property1 -= otherDoubleProperty
        assert(otherProperty1 === property1)
        assertEquals(doubleSubResult, property1.value)

        val property2: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty2 = property2
        property2 -= otherDouble
        assert(otherProperty2 === property2)
        assertEquals(doubleSubResult, property2.value)

        val property3: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty3 = property3
        property3 -= otherIntegerProperty
        assert(otherProperty3 === property3)
        assertEquals(integerSubResult, property3.value)

        val property4: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty4 = property4
        property4 -= otherInt
        assert(otherProperty4 === property4)
        assertEquals(integerSubResult, property4.value)
    }

    @Test
    fun unaryMinus() {
        val property: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty = -property
        assertEquals(-startDouble, otherProperty.value)
    }

    @Test
    fun dec() {
        var property: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty = property
        property--
        assert(otherProperty === property)
        assertEquals(startDouble - 1, property.value)
    }

    val doubleTimesResult = startDouble * otherDouble
    val integerTimesResult = startDouble * otherInt

    @Test
    fun times() {
        val property1: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty1 = property1 * otherDoubleProperty
        assertEquals(doubleTimesResult, otherProperty1.value)

        val property2: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty2 = property2 * otherDouble
        assertEquals(doubleTimesResult, otherProperty2.value)

        val property3: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty3 = property3 * otherIntegerProperty
        assertEquals(integerTimesResult, otherProperty3.value)

        val property4: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty4 = property4 * otherInt
        assertEquals(integerTimesResult, otherProperty4.value)
    }

    @Test
    fun timesAssign() {
        val property1: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty1 = property1
        property1 *= otherDoubleProperty
        assert(otherProperty1 === property1)
        assertEquals(doubleTimesResult, property1.value)

        val property2: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty2 = property2
        property2 *= otherDouble
        assert(otherProperty2 === property2)
        assertEquals(doubleTimesResult, property2.value)

        val property3: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty3 = property3
        property3 *= otherIntegerProperty
        assert(otherProperty3 === property3)
        assertEquals(integerTimesResult, property3.value)

        val property4: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty4 = property4
        property4 *= otherInt
        assert(otherProperty4 === property4)
        assertEquals(integerTimesResult, property4.value)
    }

    val doubleDivResult = startDouble / otherDouble
    val integerDivResult = startDouble / otherInt

    @Test
    fun div() {
        val property1: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty1 = property1 / otherDoubleProperty
        assertEquals(doubleDivResult, otherProperty1.value)

        val property2: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty2 = property2 / otherDouble
        assertEquals(doubleDivResult, otherProperty2.value)

        val property3: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty3 = property3 / otherIntegerProperty
        assertEquals(integerDivResult, otherProperty3.value)

        val property4: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty4 = property4 / otherInt
        assertEquals(integerDivResult, otherProperty4.value)
    }

    @Test
    fun divAssign() {
        val property1: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty1 = property1
        property1 /= otherDoubleProperty
        assert(otherProperty1 === property1)
        assertEquals(doubleDivResult, property1.value)

        val property2: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty2 = property2
        property2 /= otherDouble
        assert(otherProperty2 === property2)
        assertEquals(doubleDivResult, property2.value)

        val property3: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty3 = property3
        property3 /= otherIntegerProperty
        assert(otherProperty3 === property3)
        assertEquals(integerDivResult, property3.value)

        val property4: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty4 = property4
        property4 /= otherInt
        assert(otherProperty4 === property4)
        assertEquals(integerDivResult, property4.value)
    }

    val doubleRemResult = startDouble % otherDouble
    val integerRemResult = startDouble % otherInt

    @Test
    fun rem() {
        val property1: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty1 = property1 % otherDoubleProperty
        assertEquals(doubleRemResult, otherProperty1.value)

        val property2: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty2 = property2 % otherDouble
        assertEquals(doubleRemResult, otherProperty2.value)

        val property3: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty3 = property3 % otherIntegerProperty
        assertEquals(integerRemResult, otherProperty3.value)

        val property4: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty4 = property4 % otherInt
        assertEquals(integerRemResult, otherProperty4.value)
    }

    @Test
    fun remAssign() {
        val property1: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty1 = property1
        property1 %= otherDoubleProperty
        assert(otherProperty1 === property1)
        assertEquals(doubleRemResult, property1.value)

        val property2: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty2 = property2
        property2 %= otherDouble
        assert(otherProperty2 === property2)
        assertEquals(doubleRemResult, property2.value)

        val property3: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty3 = property3
        property3 %= otherIntegerProperty
        assert(otherProperty3 === property3)
        assertEquals(integerRemResult, property3.value)

        val property4: DoubleProperty = SimpleDoubleProperty(startDouble)
        val otherProperty4 = property4
        property4 %= otherInt
        assert(otherProperty4 === property4)
        assertEquals(integerRemResult, property4.value)
    }

    @Test
    fun compare() {
        assertEquals(true, SimpleDoubleProperty(1.0) > SimpleDoubleProperty(0.0))
        assertEquals(true, SimpleDoubleProperty(1.0) > 0.0)

        assertEquals(false, SimpleDoubleProperty(0.0) > SimpleDoubleProperty(0.0))
        assertEquals(false, SimpleDoubleProperty(0.0) > 0.0)

        assertEquals(false, SimpleDoubleProperty(-1.0) > SimpleDoubleProperty(0.0))
        assertEquals(false, SimpleDoubleProperty(-1.0) > 0.0)

        assertEquals(true, SimpleDoubleProperty(1.0) >= SimpleDoubleProperty(0.0))
        assertEquals(true, SimpleDoubleProperty(1.0) >= 0.0)

        assertEquals(true, SimpleDoubleProperty(0.0) >= SimpleDoubleProperty(0.0))
        assertEquals(true, SimpleDoubleProperty(0.0) >= 0.0)

        assertEquals(false, SimpleDoubleProperty(-1.0) >= SimpleDoubleProperty(0.0))
        assertEquals(false, SimpleDoubleProperty(-1.0) >= 0.0)

        assertEquals(false, SimpleDoubleProperty(1.0) <= SimpleDoubleProperty(0.0))
        assertEquals(false, SimpleDoubleProperty(1.0) <= 0.0)

        assertEquals(true, SimpleDoubleProperty(0.0) <= SimpleDoubleProperty(0.0))
        assertEquals(true, SimpleDoubleProperty(0.0) <= 0.0)

        assertEquals(true, SimpleDoubleProperty(-1.0) <= SimpleDoubleProperty(0.0))
        assertEquals(true, SimpleDoubleProperty(-1.0) <= 0.0)

        assertEquals(false, SimpleDoubleProperty(1.0) < SimpleDoubleProperty(0.0))
        assertEquals(false, SimpleDoubleProperty(1.0) < 0.0)

        assertEquals(false, SimpleDoubleProperty(0.0) < SimpleDoubleProperty(0.0))
        assertEquals(false, SimpleDoubleProperty(0.0) < 0.0)

        assertEquals(true, SimpleDoubleProperty(-1.0) < SimpleDoubleProperty(0.0))
        assertEquals(true, SimpleDoubleProperty(-1.0) < 0.0)


        assertEquals(true, SimpleDoubleProperty(1.0) > SimpleIntegerProperty(0))
        assertEquals(true, SimpleDoubleProperty(1.0) > 0)

        assertEquals(false, SimpleDoubleProperty(0.0) > SimpleIntegerProperty(0))
        assertEquals(false, SimpleDoubleProperty(0.0) > 0)

        assertEquals(false, SimpleDoubleProperty(-1.0) > SimpleIntegerProperty(0))
        assertEquals(false, SimpleDoubleProperty(-1.0) > 0)

        assertEquals(true, SimpleDoubleProperty(1.0) >= SimpleIntegerProperty(0))
        assertEquals(true, SimpleDoubleProperty(1.0) >= 0)

        assertEquals(true, SimpleDoubleProperty(0.0) >= SimpleIntegerProperty(0))
        assertEquals(true, SimpleDoubleProperty(0.0) >= 0)

        assertEquals(false, SimpleDoubleProperty(-1.0) >= SimpleIntegerProperty(0))
        assertEquals(false, SimpleDoubleProperty(-1.0) >= 0)

        assertEquals(false, SimpleDoubleProperty(1.0) <= SimpleIntegerProperty(0))
        assertEquals(false, SimpleDoubleProperty(1.0) <= 0)

        assertEquals(true, SimpleDoubleProperty(0.0) <= SimpleIntegerProperty(0))
        assertEquals(true, SimpleDoubleProperty(0.0) <= 0)

        assertEquals(true, SimpleDoubleProperty(-1.0) <= SimpleIntegerProperty(0))
        assertEquals(true, SimpleDoubleProperty(-1.0) <= 0)

        assertEquals(false, SimpleDoubleProperty(1.0) < SimpleIntegerProperty(0))
        assertEquals(false, SimpleDoubleProperty(1.0) < 0)

        assertEquals(false, SimpleDoubleProperty(0.0) < SimpleIntegerProperty(0))
        assertEquals(false, SimpleDoubleProperty(0.0) < 0)

        assertEquals(true, SimpleDoubleProperty(-1.0) < SimpleIntegerProperty(0))
        assertEquals(true, SimpleDoubleProperty(-1.0) < 0)
    }
}

class FloatPropertyTest {
    val startFloat = 0.1f
    val otherInt = 2
    val otherFloat = 2.1f

    val otherIntegerProperty = SimpleIntegerProperty(otherInt)
    val otherFloatProperty = SimpleFloatProperty(otherFloat)

    val floatAddResult = startFloat + otherFloat
    val integerAddResult = startFloat + otherInt

    @Test
    fun plus() {
        val property = SimpleFloatProperty(startFloat)
        val otherProperty1 = property + otherFloatProperty
        assertEquals(floatAddResult, otherProperty1.value)

        val otherProperty2 = property + otherFloat
        assertEquals(floatAddResult, otherProperty2.value)

        val otherProperty3 = property + otherIntegerProperty
        assertEquals(integerAddResult, otherProperty3.value)

        val otherProperty4 = property + otherInt
        assertEquals(integerAddResult, otherProperty4.value)
    }

    @Test
    fun plusAssign() {
        val property1: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty1 = property1
        property1 += otherFloatProperty
        assert(otherProperty1 === property1)
        assertEquals(floatAddResult, property1.value)

        val property2: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty2 = property2
        property2 += otherFloat
        assert(otherProperty2 === property2)
        assertEquals(floatAddResult, property2.value)

        val property3: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty3 = property3
        property3 += otherIntegerProperty
        assert(otherProperty3 === property3)
        assertEquals(integerAddResult, property3.value)

        val property4: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty4 = property4
        property4 += otherInt
        assert(otherProperty4 === property4)
        assertEquals(integerAddResult, property4.value)
    }

    @Test
    fun inc() {
        var property: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty = property
        property++
        assert(otherProperty === property)
        assertEquals(startFloat + 1, property.value)
    }


    val floatSubResult = startFloat - otherFloat
    val integerSubResult = startFloat - otherInt

    @Test
    fun minus() {
        val property1: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty1 = property1 - otherFloatProperty
        assertEquals(floatSubResult, otherProperty1.value)

        val property2: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty2 = property2 - otherFloat
        assertEquals(floatSubResult, otherProperty2.value)

        val property3: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty3 = property3 - otherIntegerProperty
        assertEquals(integerSubResult, otherProperty3.value)

        val property4: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty4 = property4 - otherInt
        assertEquals(integerSubResult, otherProperty4.value)
    }

    @Test
    fun minusAssign() {
        val property1: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty1 = property1
        property1 -= otherFloatProperty
        assert(otherProperty1 === property1)
        assertEquals(floatSubResult, property1.value)

        val property2: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty2 = property2
        property2 -= otherFloat
        assert(otherProperty2 === property2)
        assertEquals(floatSubResult, property2.value)

        val property3: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty3 = property3
        property3 -= otherIntegerProperty
        assert(otherProperty3 === property3)
        assertEquals(integerSubResult, property3.value)

        val property4: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty4 = property4
        property4 -= otherInt
        assert(otherProperty4 === property4)
        assertEquals(integerSubResult, property4.value)
    }

    @Test
    fun unaryMinus() {
        val property: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty = -property
        assertEquals(-startFloat, otherProperty.value)
    }

    @Test
    fun dec() {
        var property: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty = property
        property--
        assert(otherProperty === property)
        assertEquals(startFloat - 1, property.value)
    }

    val floatTimesResult = startFloat * otherFloat
    val integerTimesResult = startFloat * otherInt

    @Test
    fun times() {
        val property1: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty1 = property1 * otherFloatProperty
        assertEquals(floatTimesResult, otherProperty1.value)

        val property2: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty2 = property2 * otherFloat
        assertEquals(floatTimesResult, otherProperty2.value)

        val property3: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty3 = property3 * otherIntegerProperty
        assertEquals(integerTimesResult, otherProperty3.value)

        val property4: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty4 = property4 * otherInt
        assertEquals(integerTimesResult, otherProperty4.value)
    }

    @Test
    fun timesAssign() {
        val property1: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty1 = property1
        property1 *= otherFloatProperty
        assert(otherProperty1 === property1)
        assertEquals(floatTimesResult, property1.value)

        val property2: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty2 = property2
        property2 *= otherFloat
        assert(otherProperty2 === property2)
        assertEquals(floatTimesResult, property2.value)

        val property3: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty3 = property3
        property3 *= otherIntegerProperty
        assert(otherProperty3 === property3)
        assertEquals(integerTimesResult, property3.value)

        val property4: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty4 = property4
        property4 *= otherInt
        assert(otherProperty4 === property4)
        assertEquals(integerTimesResult, property4.value)
    }

    val floatDivResult = startFloat / otherFloat
    val integerDivResult = startFloat / otherInt

    @Test
    fun div() {
        val property1: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty1 = property1 / otherFloatProperty
        assertEquals(floatDivResult, otherProperty1.value)

        val property2: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty2 = property2 / otherFloat
        assertEquals(floatDivResult, otherProperty2.value)

        val property3: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty3 = property3 / otherIntegerProperty
        assertEquals(integerDivResult, otherProperty3.value)

        val property4: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty4 = property4 / otherInt
        assertEquals(integerDivResult, otherProperty4.value)
    }

    @Test
    fun divAssign() {
        val property1: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty1 = property1
        property1 /= otherFloatProperty
        assert(otherProperty1 === property1)
        assertEquals(floatDivResult, property1.value)

        val property2: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty2 = property2
        property2 /= otherFloat
        assert(otherProperty2 === property2)
        assertEquals(floatDivResult, property2.value)

        val property3: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty3 = property3
        property3 /= otherIntegerProperty
        assert(otherProperty3 === property3)
        assertEquals(integerDivResult, property3.value)

        val property4: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty4 = property4
        property4 /= otherInt
        assert(otherProperty4 === property4)
        assertEquals(integerDivResult, property4.value)
    }

    val floatRemResult = startFloat % otherFloat
    val integerRemResult = startFloat % otherInt

    @Test
    fun rem() {
        val property1: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty1 = property1 % otherFloatProperty
        assertEquals(floatRemResult, otherProperty1.value)

        val property2: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty2 = property2 % otherFloat
        assertEquals(floatRemResult, otherProperty2.value)

        val property3: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty3 = property3 % otherIntegerProperty
        assertEquals(integerRemResult, otherProperty3.value)

        val property4: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty4 = property4 % otherInt
        assertEquals(integerRemResult, otherProperty4.value)
    }

    @Test
    fun remAssign() {
        val property1: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty1 = property1
        property1 %= otherFloatProperty
        assert(otherProperty1 === property1)
        assertEquals(floatRemResult, property1.value)

        val property2: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty2 = property2
        property2 %= otherFloat
        assert(otherProperty2 === property2)
        assertEquals(floatRemResult, property2.value)

        val property3: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty3 = property3
        property3 %= otherIntegerProperty
        assert(otherProperty3 === property3)
        assertEquals(integerRemResult, property3.value)

        val property4: FloatProperty = SimpleFloatProperty(startFloat)
        val otherProperty4 = property4
        property4 %= otherInt
        assert(otherProperty4 === property4)
        assertEquals(integerRemResult, property4.value)
    }

    @Test
    fun compare() {
        assertEquals(true, SimpleFloatProperty(1.0f) > SimpleFloatProperty(0.0f))
        assertEquals(true, SimpleFloatProperty(1.0f) > 0.0f)

        assertEquals(false, SimpleFloatProperty(0.0f) > SimpleFloatProperty(0.0f))
        assertEquals(false, SimpleFloatProperty(0.0f) > 0.0f)

        assertEquals(false, SimpleFloatProperty(-1.0f) > SimpleFloatProperty(0.0f))
        assertEquals(false, SimpleFloatProperty(-1.0f) > 0.0f)

        assertEquals(true, SimpleFloatProperty(1.0f) >= SimpleFloatProperty(0.0f))
        assertEquals(true, SimpleFloatProperty(1.0f) >= 0.0f)

        assertEquals(true, SimpleFloatProperty(0.0f) >= SimpleFloatProperty(0.0f))
        assertEquals(true, SimpleFloatProperty(0.0f) >= 0.0f)

        assertEquals(false, SimpleFloatProperty(-1.0f) >= SimpleFloatProperty(0.0f))
        assertEquals(false, SimpleFloatProperty(-1.0f) >= 0.0f)

        assertEquals(false, SimpleFloatProperty(1.0f) <= SimpleFloatProperty(0.0f))
        assertEquals(false, SimpleFloatProperty(1.0f) <= 0.0f)

        assertEquals(true, SimpleFloatProperty(0.0f) <= SimpleFloatProperty(0.0f))
        assertEquals(true, SimpleFloatProperty(0.0f) <= 0.0f)

        assertEquals(true, SimpleFloatProperty(-1.0f) <= SimpleFloatProperty(0.0f))
        assertEquals(true, SimpleFloatProperty(-1.0f) <= 0.0f)

        assertEquals(false, SimpleFloatProperty(1.0f) < SimpleFloatProperty(0.0f))
        assertEquals(false, SimpleFloatProperty(1.0f) < 0.0f)

        assertEquals(false, SimpleFloatProperty(0.0f) < SimpleFloatProperty(0.0f))
        assertEquals(false, SimpleFloatProperty(0.0f) < 0.0f)

        assertEquals(true, SimpleFloatProperty(-1.0f) < SimpleFloatProperty(0.0f))
        assertEquals(true, SimpleFloatProperty(-1.0f) < 0.0f)


        assertEquals(true, SimpleFloatProperty(1.0f) > SimpleIntegerProperty(0))
        assertEquals(true, SimpleFloatProperty(1.0f) > 0)

        assertEquals(false, SimpleFloatProperty(0.0f) > SimpleIntegerProperty(0))
        assertEquals(false, SimpleFloatProperty(0.0f) > 0)

        assertEquals(false, SimpleFloatProperty(-1.0f) > SimpleIntegerProperty(0))
        assertEquals(false, SimpleFloatProperty(-1.0f) > 0)

        assertEquals(true, SimpleFloatProperty(1.0f) >= SimpleIntegerProperty(0))
        assertEquals(true, SimpleFloatProperty(1.0f) >= 0)

        assertEquals(true, SimpleFloatProperty(0.0f) >= SimpleIntegerProperty(0))
        assertEquals(true, SimpleFloatProperty(0.0f) >= 0)

        assertEquals(false, SimpleFloatProperty(-1.0f) >= SimpleIntegerProperty(0))
        assertEquals(false, SimpleFloatProperty(-1.0f) >= 0)

        assertEquals(false, SimpleFloatProperty(1.0f) <= SimpleIntegerProperty(0))
        assertEquals(false, SimpleFloatProperty(1.0f) <= 0)

        assertEquals(true, SimpleFloatProperty(0.0f) <= SimpleIntegerProperty(0))
        assertEquals(true, SimpleFloatProperty(0.0f) <= 0)

        assertEquals(true, SimpleFloatProperty(-1.0f) <= SimpleIntegerProperty(0))
        assertEquals(true, SimpleFloatProperty(-1.0f) <= 0)

        assertEquals(false, SimpleFloatProperty(1.0f) < SimpleIntegerProperty(0))
        assertEquals(false, SimpleFloatProperty(1.0f) < 0)

        assertEquals(false, SimpleFloatProperty(0.0f) < SimpleIntegerProperty(0))
        assertEquals(false, SimpleFloatProperty(0.0f) < 0)

        assertEquals(true, SimpleFloatProperty(-1.0f) < SimpleIntegerProperty(0))
        assertEquals(true, SimpleFloatProperty(-1.0f) < 0)
    }
}

class IntegerPropertyTest {
    val startInt = 1
    val otherInt = 2
    val otherDouble = 2.1

    val otherIntegerProperty = SimpleIntegerProperty(otherInt)
    val otherDoubleProperty = SimpleDoubleProperty(otherDouble)

    val doubleAddResult = startInt + otherDouble
    val integerAddResult = startInt + otherInt

    @Test
    fun plus() {
        val property1 = SimpleIntegerProperty(startInt)
        val otherProperty1 = property1 + otherDoubleProperty
        assertEquals(doubleAddResult, otherProperty1.value)

        val property2 = SimpleIntegerProperty(startInt)
        val otherProperty2 = property2 + otherDouble
        assertEquals(doubleAddResult, otherProperty2.value)

        val property3 = SimpleIntegerProperty(startInt)
        val otherProperty3 = property3 + otherIntegerProperty
        assertEquals(integerAddResult, otherProperty3.value)


        val property4 = SimpleIntegerProperty(startInt)
        val otherProperty4 = property4 + otherInt
        assertEquals(integerAddResult, otherProperty4.value)
    }

    @Test
    fun plusAssign() {
        val property1: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty1 = property1
        property1 += otherIntegerProperty
        assert(otherProperty1 === property1)
        assertEquals(integerAddResult, property1.value)

        val property2: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty2 = property2
        property2 += otherDouble
        assert(otherProperty2 === property2)
        assertEquals(integerAddResult, property2.value)

        val property3: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty3 = property3
        property3 += otherIntegerProperty
        assert(otherProperty3 === property3)
        assertEquals(integerAddResult, property3.value)

        val property4: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty4 = property4
        property4 += otherInt
        assert(otherProperty4 === property4)
        assertEquals(integerAddResult, property4.value)
    }

    @Test
    fun inc() {
        var property: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty = property
        property++
        assert(otherProperty === property)
        assertEquals(startInt + 1, property.value)
    }


    val doubleSubResult = startInt - otherDouble
    val integerSubResult = startInt - otherInt

    @Test
    fun minus() {
        val property1: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty1 = property1 - otherDoubleProperty
        assertEquals(doubleSubResult, otherProperty1.value)

        val property2: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty2 = property2 - otherDouble
        assertEquals(doubleSubResult, otherProperty2.value)

        val property3: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty3 = property3 - otherIntegerProperty
        assertEquals(integerSubResult, otherProperty3.value)

        val property4: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty4 = property4 - otherInt
        assertEquals(integerSubResult, otherProperty4.value)
    }

    @Test
    fun minusAssign() {
        val property1: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty1 = property1
        property1 -= otherDoubleProperty
        assert(otherProperty1 === property1)
        assertEquals(integerSubResult, property1.value)

        val property2: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty2 = property2
        property2 -= otherDouble
        assert(otherProperty2 === property2)
        assertEquals(integerSubResult, property2.value)

        val property3: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty3 = property3
        property3 -= otherIntegerProperty
        assert(otherProperty3 === property3)
        assertEquals(integerSubResult, property3.value)

        val property4: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty4 = property4
        property4 -= otherInt
        assert(otherProperty4 === property4)
        assertEquals(integerSubResult, property4.value)
    }

    @Test
    fun unaryMinus() {
        val property: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty = -property
        assertEquals(-startInt, otherProperty.value)
    }

    @Test
    fun dec() {
        var property: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty = property
        property--
        assert(otherProperty === property)
        assertEquals(startInt - 1, property.value)
    }

    val doubleTimesResult = startInt * otherDouble
    val integerTimesResult = startInt * otherInt

    @Test
    fun times() {
        val property1: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty1 = property1 * otherDoubleProperty
        assertEquals(doubleTimesResult, otherProperty1.value)

        val property2: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty2 = property2 * otherDouble
        assertEquals(doubleTimesResult, otherProperty2.value)

        val property3: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty3 = property3 * otherIntegerProperty
        assertEquals(integerTimesResult, otherProperty3.value)

        val property4: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty4 = property4 * otherInt
        assertEquals(integerTimesResult, otherProperty4.value)
    }

    @Test
    fun timesAssign() {
        val property1: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty1 = property1
        property1 *= otherDoubleProperty
        assert(otherProperty1 === property1)
        assertEquals(integerTimesResult, property1.value)

        val property2: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty2 = property2
        property2 *= otherDouble
        assert(otherProperty2 === property2)
        assertEquals(integerTimesResult, property2.value)

        val property3: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty3 = property3
        property3 *= otherIntegerProperty
        assert(otherProperty3 === property3)
        assertEquals(integerTimesResult, property3.value)

        val property4: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty4 = property4
        property4 *= otherInt
        assert(otherProperty4 === property4)
        assertEquals(integerTimesResult, property4.value)
    }

    val doubleDivResult = startInt / otherDouble
    val integerDivResult = startInt / otherInt

    @Test
    fun div() {
        val property1: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty1 = property1 / otherDoubleProperty
        assertEquals(doubleDivResult, otherProperty1.value)

        val property2: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty2 = property2 / otherDouble
        assertEquals(doubleDivResult, otherProperty2.value)

        val property3: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty3 = property3 / otherIntegerProperty
        assertEquals(integerDivResult, otherProperty3.value)

        val property4: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty4 = property4 / otherInt
        assertEquals(integerDivResult, otherProperty4.value)
    }

    @Test
    fun divAssign() {
        val property1: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty1 = property1
        property1 /= otherDoubleProperty
        assert(otherProperty1 === property1)
        assertEquals(integerDivResult, property1.value)

        val property2: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty2 = property2
        property2 /= otherDouble
        assert(otherProperty2 === property2)
        assertEquals(integerDivResult, property2.value)

        val property3: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty3 = property3
        property3 /= otherIntegerProperty
        assert(otherProperty3 === property3)
        assertEquals(integerDivResult, property3.value)

        val property4: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty4 = property4
        property4 /= otherInt
        assert(otherProperty4 === property4)
        assertEquals(integerDivResult, property4.value)
    }

    val doubleRemResult = startInt % otherDouble
    val integerRemResult = startInt % otherInt

    @Test
    fun rem() {
        val property1: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty1 = property1 % otherDoubleProperty
        assertEquals(doubleRemResult, otherProperty1.value)

        val property2: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty2 = property2 % otherDouble
        assertEquals(doubleRemResult, otherProperty2.value)

        val property3: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty3 = property3 % otherIntegerProperty
        assertEquals(integerRemResult, otherProperty3.value)

        val property4: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty4 = property4 % otherInt
        assertEquals(integerRemResult, otherProperty4.value)
    }

    @Test
    fun remAssign() {
        val property1: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty1 = property1
        property1 %= otherDoubleProperty
        assert(otherProperty1 === property1)
        assertEquals(integerRemResult, property1.value)

        val property2: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty2 = property2
        property2 %= otherDouble
        assert(otherProperty2 === property2)
        assertEquals(integerRemResult, property2.value)

        val property3: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty3 = property3
        property3 %= otherIntegerProperty
        assert(otherProperty3 === property3)
        assertEquals(integerRemResult, property3.value)

        val property4: IntegerProperty = SimpleIntegerProperty(startInt)
        val otherProperty4 = property4
        property4 %= otherInt
        assert(otherProperty4 === property4)
        assertEquals(integerRemResult, property4.value)
    }

    @Test
    fun sequence() {
        val sequence1 = SimpleIntegerProperty(2)..SimpleIntegerProperty(10)
        assertEquals(9, sequence1.count())
        assertEquals(2, sequence1.elementAt(0).value)
        assertEquals(10, sequence1.elementAt(8).value)

        val sequence2 = SimpleIntegerProperty(2)..10
        assertEquals(9, sequence2.count())
        assertEquals(2, sequence2.elementAt(0).value)
        assertEquals(10, sequence2.elementAt(8).value)

        val sequence3 = SimpleIntegerProperty(2)..SimpleLongProperty(10L)
        assertEquals(9, sequence3.count())
        assertEquals(2, sequence3.elementAt(0).value)
        assertEquals(10, sequence3.elementAt(8).value)

        val sequence4 = SimpleIntegerProperty(2)..10L
        assertEquals(9, sequence4.count())
        assertEquals(2, sequence4.elementAt(0).value)
        assertEquals(10, sequence4.elementAt(8).value)
    }

    @Test
    fun compare() {
        assertEquals(true, SimpleIntegerProperty(1) > SimpleIntegerProperty(0))
        assertEquals(true, SimpleIntegerProperty(1) > 0)

        assertEquals(false, SimpleIntegerProperty(0) > SimpleIntegerProperty(0))
        assertEquals(false, SimpleIntegerProperty(0) > 0)

        assertEquals(false, SimpleIntegerProperty(-1) > SimpleIntegerProperty(0))
        assertEquals(false, SimpleIntegerProperty(-1) > 0)

        assertEquals(true, SimpleIntegerProperty(1) >= SimpleIntegerProperty(0))
        assertEquals(true, SimpleIntegerProperty(1) >= 0)

        assertEquals(true, SimpleIntegerProperty(0) >= SimpleIntegerProperty(0))
        assertEquals(true, SimpleIntegerProperty(0) >= 0)

        assertEquals(false, SimpleIntegerProperty(-1) >= SimpleIntegerProperty(0))
        assertEquals(false, SimpleIntegerProperty(-1) >= 0)

        assertEquals(false, SimpleIntegerProperty(1) <= SimpleIntegerProperty(0))
        assertEquals(false, SimpleIntegerProperty(1) <= 0)

        assertEquals(true, SimpleIntegerProperty(0) <= SimpleIntegerProperty(0))
        assertEquals(true, SimpleIntegerProperty(0) <= 0)

        assertEquals(true, SimpleIntegerProperty(-1) <= SimpleIntegerProperty(0))
        assertEquals(true, SimpleIntegerProperty(-1) <= 0)

        assertEquals(false, SimpleIntegerProperty(1) < SimpleIntegerProperty(0))
        assertEquals(false, SimpleIntegerProperty(1) < 0)

        assertEquals(false, SimpleIntegerProperty(0) < SimpleIntegerProperty(0))
        assertEquals(false, SimpleIntegerProperty(0) < 0)

        assertEquals(true, SimpleIntegerProperty(-1) < SimpleIntegerProperty(0))
        assertEquals(true, SimpleIntegerProperty(-1) < 0)


        assertEquals(true, SimpleIntegerProperty(1) > SimpleDoubleProperty(0.0))
        assertEquals(true, SimpleIntegerProperty(1) > 0.0)

        assertEquals(false, SimpleIntegerProperty(0) > SimpleDoubleProperty(0.0))
        assertEquals(false, SimpleIntegerProperty(0) > 0.0)

        assertEquals(false, SimpleIntegerProperty(-1) > SimpleDoubleProperty(0.0))
        assertEquals(false, SimpleIntegerProperty(-1) > 0.0)

        assertEquals(true, SimpleIntegerProperty(1) >= SimpleDoubleProperty(0.0))
        assertEquals(true, SimpleIntegerProperty(1) >= 0.0)

        assertEquals(true, SimpleIntegerProperty(0) >= SimpleDoubleProperty(0.0))
        assertEquals(true, SimpleIntegerProperty(0) >= 0.0)

        assertEquals(false, SimpleIntegerProperty(-1) >= SimpleDoubleProperty(0.0))
        assertEquals(false, SimpleIntegerProperty(-1) >= 0.0)

        assertEquals(false, SimpleIntegerProperty(1) <= SimpleDoubleProperty(0.0))
        assertEquals(false, SimpleIntegerProperty(1) <= 0.0)

        assertEquals(true, SimpleIntegerProperty(0) <= SimpleDoubleProperty(0.0))
        assertEquals(true, SimpleIntegerProperty(0) <= 0.0)

        assertEquals(true, SimpleIntegerProperty(-1) <= SimpleDoubleProperty(0.0))
        assertEquals(true, SimpleIntegerProperty(-1) <= 0.0)

        assertEquals(false, SimpleIntegerProperty(1) < SimpleDoubleProperty(0.0))
        assertEquals(false, SimpleIntegerProperty(1) < 0.0)

        assertEquals(false, SimpleIntegerProperty(0) < SimpleDoubleProperty(0.0))
        assertEquals(false, SimpleIntegerProperty(0) < 0.0)

        assertEquals(true, SimpleIntegerProperty(-1) < SimpleDoubleProperty(0.0))
        assertEquals(true, SimpleIntegerProperty(-1) < 0.0)
    }
}

class LongPropertyTest {
    val startLong = 1L
    val otherLong = 2L
    val otherDouble = 2.1

    val otherLongProperty = SimpleLongProperty(otherLong)
    val otherDoubleProperty = SimpleDoubleProperty(otherDouble)

    val doubleAddResult = startLong + otherDouble
    val longAddResult = startLong + otherLong

    @Test
    fun plus() {
        val property = SimpleLongProperty(startLong)
        val otherProperty1 = property + otherDoubleProperty
        assertEquals(doubleAddResult, otherProperty1.value)

        val otherProperty2 = property + otherDouble
        assertEquals(doubleAddResult, otherProperty2.value)

        val otherProperty3 = property + otherLongProperty
        assertEquals(longAddResult, otherProperty3.value)

        val otherProperty4 = property + otherLong
        assertEquals(longAddResult, otherProperty4.value)
    }

    @Test
    fun plusAssign() {
        val property1: LongProperty = SimpleLongProperty(startLong)
        val otherProperty1 = property1
        property1 += otherLongProperty
        assert(otherProperty1 === property1)
        assertEquals(longAddResult, property1.value)

        val property2: LongProperty = SimpleLongProperty(startLong)
        val otherProperty2 = property2
        property2 += otherDouble
        assert(otherProperty2 === property2)
        assertEquals(longAddResult, property2.value)

        val property3: LongProperty = SimpleLongProperty(startLong)
        val otherProperty3 = property3
        property3 += otherLongProperty
        assert(otherProperty3 === property3)
        assertEquals(longAddResult, property3.value)

        val property4: LongProperty = SimpleLongProperty(startLong)
        val otherProperty4 = property4
        property4 += otherLong
        assert(otherProperty4 === property4)
        assertEquals(longAddResult, property4.value)
    }

    @Test
    fun inc() {
        var property: LongProperty = SimpleLongProperty(startLong)
        val otherProperty = property
        property++
        assert(otherProperty === property)
        assertEquals(startLong + 1, property.value)
    }


    val doubleSubResult = startLong - otherDouble
    val longSubResult = startLong - otherLong

    @Test
    fun minus() {
        val property1: LongProperty = SimpleLongProperty(startLong)
        val otherProperty1 = property1 - otherDoubleProperty
        assertEquals(doubleSubResult, otherProperty1.value)

        val property2: LongProperty = SimpleLongProperty(startLong)
        val otherProperty2 = property2 - otherDouble
        assertEquals(doubleSubResult, otherProperty2.value)

        val property3: LongProperty = SimpleLongProperty(startLong)
        val otherProperty3 = property3 - otherLongProperty
        assertEquals(longSubResult, otherProperty3.value)

        val property4: LongProperty = SimpleLongProperty(startLong)
        val otherProperty4 = property4 - otherLong
        assertEquals(longSubResult, otherProperty4.value)
    }

    @Test
    fun minusAssign() {
        val property1: LongProperty = SimpleLongProperty(startLong)
        val otherProperty1 = property1
        property1 -= otherDoubleProperty
        assert(otherProperty1 === property1)
        assertEquals(longSubResult, property1.value)

        val property2: LongProperty = SimpleLongProperty(startLong)
        val otherProperty2 = property2
        property2 -= otherDouble
        assert(otherProperty2 === property2)
        assertEquals(longSubResult, property2.value)

        val property3: LongProperty = SimpleLongProperty(startLong)
        val otherProperty3 = property3
        property3 -= otherLongProperty
        assert(otherProperty3 === property3)
        assertEquals(longSubResult, property3.value)

        val property4: LongProperty = SimpleLongProperty(startLong)
        val otherProperty4 = property4
        property4 -= otherLong
        assert(otherProperty4 === property4)
        assertEquals(longSubResult, property4.value)
    }

    @Test
    fun unaryMinus() {
        val property: LongProperty = SimpleLongProperty(startLong)
        val otherProperty = -property
        assertEquals(-startLong, otherProperty.value)
    }

    @Test
    fun dec() {
        var property: LongProperty = SimpleLongProperty(startLong)
        val otherProperty = property
        property--
        assert(otherProperty === property)
        assertEquals(startLong - 1, property.value)
    }

    val doubleTimesResult = startLong * otherDouble
    val longTimesResult = startLong * otherLong

    @Test
    fun times() {
        val property1: LongProperty = SimpleLongProperty(startLong)
        val otherProperty1 = property1 * otherDoubleProperty
        assertEquals(doubleTimesResult, otherProperty1.value)

        val property2: LongProperty = SimpleLongProperty(startLong)
        val otherProperty2 = property2 * otherDouble
        assertEquals(doubleTimesResult, otherProperty2.value)

        val property3: LongProperty = SimpleLongProperty(startLong)
        val otherProperty3 = property3 * otherLongProperty
        assertEquals(longTimesResult, otherProperty3.value)

        val property4: LongProperty = SimpleLongProperty(startLong)
        val otherProperty4 = property4 * otherLong
        assertEquals(longTimesResult, otherProperty4.value)
    }

    @Test
    fun timesAssign() {
        val property1: LongProperty = SimpleLongProperty(startLong)
        val otherProperty1 = property1
        property1 *= otherDoubleProperty
        assert(otherProperty1 === property1)
        assertEquals(longTimesResult, property1.value)

        val property2: LongProperty = SimpleLongProperty(startLong)
        val otherProperty2 = property2
        property2 *= otherDouble
        assert(otherProperty2 === property2)
        assertEquals(longTimesResult, property2.value)

        val property3: LongProperty = SimpleLongProperty(startLong)
        val otherProperty3 = property3
        property3 *= otherLongProperty
        assert(otherProperty3 === property3)
        assertEquals(longTimesResult, property3.value)

        val property4: LongProperty = SimpleLongProperty(startLong)
        val otherProperty4 = property4
        property4 *= otherLong
        assert(otherProperty4 === property4)
        assertEquals(longTimesResult, property4.value)
    }

    val doubleDivResult = startLong / otherDouble
    val longDivResult = startLong / otherLong

    @Test
    fun div() {
        val property1: LongProperty = SimpleLongProperty(startLong)
        val otherProperty1 = property1 / otherDoubleProperty
        assertEquals(doubleDivResult, otherProperty1.value)

        val property2: LongProperty = SimpleLongProperty(startLong)
        val otherProperty2 = property2 / otherDouble
        assertEquals(doubleDivResult, otherProperty2.value)

        val property3: LongProperty = SimpleLongProperty(startLong)
        val otherProperty3 = property3 / otherLongProperty
        assertEquals(longDivResult, otherProperty3.value)

        val property4: LongProperty = SimpleLongProperty(startLong)
        val otherProperty4 = property4 / otherLong
        assertEquals(longDivResult, otherProperty4.value)
    }

    @Test
    fun divAssign() {
        val property1: LongProperty = SimpleLongProperty(startLong)
        val otherProperty1 = property1
        property1 /= otherDoubleProperty
        assert(otherProperty1 === property1)
        assertEquals(longDivResult, property1.value)

        val property2: LongProperty = SimpleLongProperty(startLong)
        val otherProperty2 = property2
        property2 /= otherDouble
        assert(otherProperty2 === property2)
        assertEquals(longDivResult, property2.value)

        val property3: LongProperty = SimpleLongProperty(startLong)
        val otherProperty3 = property3
        property3 /= otherLongProperty
        assert(otherProperty3 === property3)
        assertEquals(longDivResult, property3.value)

        val property4: LongProperty = SimpleLongProperty(startLong)
        val otherProperty4 = property4
        property4 /= otherLong
        assert(otherProperty4 === property4)
        assertEquals(longDivResult, property4.value)
    }

    val doubleRemResult = startLong % otherDouble
    val longRemResult = startLong % otherLong

    @Test
    fun rem() {
        val property1: LongProperty = SimpleLongProperty(startLong)
        val otherProperty1 = property1 % otherDoubleProperty
        assertEquals(doubleRemResult, otherProperty1.value)

        val property2: LongProperty = SimpleLongProperty(startLong)
        val otherProperty2 = property2 % otherDouble
        assertEquals(doubleRemResult, otherProperty2.value)

        val property3: LongProperty = SimpleLongProperty(startLong)
        val otherProperty3 = property3 % otherLongProperty
        assertEquals(longRemResult, otherProperty3.value)

        val property4: LongProperty = SimpleLongProperty(startLong)
        val otherProperty4 = property4 % otherLong
        assertEquals(longRemResult, otherProperty4.value)
    }

    @Test
    fun remAssign() {
        val property1: LongProperty = SimpleLongProperty(startLong)
        val otherProperty1 = property1
        property1 %= otherDoubleProperty
        assert(otherProperty1 === property1)
        assertEquals(longRemResult, property1.value)

        val property2: LongProperty = SimpleLongProperty(startLong)
        val otherProperty2 = property2
        property2 %= otherDouble
        assert(otherProperty2 === property2)
        assertEquals(longRemResult, property2.value)

        val property3: LongProperty = SimpleLongProperty(startLong)
        val otherProperty3 = property3
        property3 %= otherLongProperty
        assert(otherProperty3 === property3)
        assertEquals(longRemResult, property3.value)

        val property4: LongProperty = SimpleLongProperty(startLong)
        val otherProperty4 = property4
        property4 %= otherLong
        assert(otherProperty4 === property4)
        assertEquals(longRemResult, property4.value)
    }

    @Test
    fun sequence() {
        val sequence1 = SimpleLongProperty(2)..SimpleLongProperty(10L)
        assertEquals(9, sequence1.count())
        assertEquals(2, sequence1.elementAt(0).value)
        assertEquals(10, sequence1.elementAt(8).value)

        val sequence2 = SimpleLongProperty(2)..10L
        assertEquals(9, sequence2.count())
        assertEquals(2, sequence2.elementAt(0).value)
        assertEquals(10, sequence2.elementAt(8).value)

        val sequence3 = SimpleLongProperty(2)..SimpleIntegerProperty(10)
        assertEquals(9, sequence3.count())
        assertEquals(2, sequence3.elementAt(0).value)
        assertEquals(10, sequence3.elementAt(8).value)

        val sequence4 = SimpleLongProperty(2)..10
        assertEquals(9, sequence4.count())
        assertEquals(2, sequence4.elementAt(0).value)
        assertEquals(10, sequence4.elementAt(8).value)
    }

    @Test
    fun compare() {
        assertEquals(true, SimpleLongProperty(1) > SimpleLongProperty(0))
        assertEquals(true, SimpleLongProperty(1) > 0)

        assertEquals(false, SimpleLongProperty(0) > SimpleLongProperty(0))
        assertEquals(false, SimpleLongProperty(0) > 0)

        assertEquals(false, SimpleLongProperty(-1) > SimpleLongProperty(0))
        assertEquals(false, SimpleLongProperty(-1) > 0)

        assertEquals(true, SimpleLongProperty(1) >= SimpleLongProperty(0))
        assertEquals(true, SimpleLongProperty(1) >= 0)

        assertEquals(true, SimpleLongProperty(0) >= SimpleLongProperty(0))
        assertEquals(true, SimpleLongProperty(0) >= 0)

        assertEquals(false, SimpleLongProperty(-1) >= SimpleLongProperty(0))
        assertEquals(false, SimpleLongProperty(-1) >= 0)

        assertEquals(false, SimpleLongProperty(1) <= SimpleLongProperty(0))
        assertEquals(false, SimpleLongProperty(1) <= 0)

        assertEquals(true, SimpleLongProperty(0) <= SimpleLongProperty(0))
        assertEquals(true, SimpleLongProperty(0) <= 0)

        assertEquals(true, SimpleLongProperty(-1) <= SimpleLongProperty(0))
        assertEquals(true, SimpleLongProperty(-1) <= 0)

        assertEquals(false, SimpleLongProperty(1) < SimpleLongProperty(0))
        assertEquals(false, SimpleLongProperty(1) < 0)

        assertEquals(false, SimpleLongProperty(0) < SimpleLongProperty(0))
        assertEquals(false, SimpleLongProperty(0) < 0)

        assertEquals(true, SimpleLongProperty(-1) < SimpleLongProperty(0))
        assertEquals(true, SimpleLongProperty(-1) < 0)


        assertEquals(true, SimpleLongProperty(1) > SimpleDoubleProperty(0.0))
        assertEquals(true, SimpleLongProperty(1) > 0.0)

        assertEquals(false, SimpleLongProperty(0) > SimpleDoubleProperty(0.0))
        assertEquals(false, SimpleLongProperty(0) > 0.0)

        assertEquals(false, SimpleLongProperty(-1) > SimpleDoubleProperty(0.0))
        assertEquals(false, SimpleLongProperty(-1) > 0.0)

        assertEquals(true, SimpleLongProperty(1) >= SimpleDoubleProperty(0.0))
        assertEquals(true, SimpleLongProperty(1) >= 0.0)

        assertEquals(true, SimpleLongProperty(0) >= SimpleDoubleProperty(0.0))
        assertEquals(true, SimpleLongProperty(0) >= 0.0)

        assertEquals(false, SimpleLongProperty(-1) >= SimpleDoubleProperty(0.0))
        assertEquals(false, SimpleLongProperty(-1) >= 0.0)

        assertEquals(false, SimpleLongProperty(1) <= SimpleDoubleProperty(0.0))
        assertEquals(false, SimpleLongProperty(1) <= 0.0)

        assertEquals(true, SimpleLongProperty(0) <= SimpleDoubleProperty(0.0))
        assertEquals(true, SimpleLongProperty(0) <= 0.0)

        assertEquals(true, SimpleLongProperty(-1) <= SimpleDoubleProperty(0.0))
        assertEquals(true, SimpleLongProperty(-1) <= 0.0)

        assertEquals(false, SimpleLongProperty(1) < SimpleDoubleProperty(0.0))
        assertEquals(false, SimpleLongProperty(1) < 0.0)

        assertEquals(false, SimpleLongProperty(0) < SimpleDoubleProperty(0.0))
        assertEquals(false, SimpleLongProperty(0) < 0.0)

        assertEquals(true, SimpleLongProperty(-1) < SimpleDoubleProperty(0.0))
        assertEquals(true, SimpleLongProperty(-1) < 0.0)
    }
}