package tornadofx

import org.junit.Assert
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PropertiesTest {
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
    }
}
