package tornadofx

import javafx.beans.property.SimpleObjectProperty
import org.junit.Test
import kotlin.test.assertEquals

class PropertiesTest {

    @Test
    fun `should get string property`() {
        // given:
        val fixture = object {
            val string by property<String>()
        }

        // expect:
        assertEquals(fixture.string, null)
    }

    @Test
    fun `should set string property`() {
        // given:
        val fixture = object {
            var string by property<String>()
        }

        // when:
        fixture.string = "foo"

        // then:
        assertEquals(fixture.string, "foo")
    }

    @Test
    fun should_be_able_to_use_property_and_lazy() {
        // given:
        var wasLazyCalled = false
        val fixture = object {
            private val stringProperty by lazy {
                wasLazyCalled = true
                SimpleObjectProperty<String>("foo")
            }

            val string by property { stringProperty }
        }

        // expect:
        assertEquals(wasLazyCalled, false)
        assertEquals(fixture.string, "foo")
        assertEquals(wasLazyCalled, true)
    }

}
