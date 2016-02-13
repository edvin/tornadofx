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
    fun `property() in combination with lazy()`() {
        val fixture = object {
            private val stringProperty by lazy {
                println("in lazy lambda expr")
                SimpleObjectProperty<String>()
            }

            val string by property(stringProperty)
        }
    }

}
