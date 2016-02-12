package tornadofx

import org.junit.Test
import kotlin.test.assertEquals

class PropertiesTest {

    internal class Fixtures {
        val string by property<String>()
        var stringVar by property<String>()
    }

    @Test
    fun `should get string property`() {
        val fixtures = Fixtures()

        // expect:
        assertEquals(fixtures.string, null)
    }

    @Test
    fun `should set string property`() {
        val fixtures = Fixtures()
        fixtures.stringVar = "foo"

        // expect:
        assertEquals(fixtures.stringVar, "foo")
    }

}
