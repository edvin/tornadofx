package tornadofx.tests

import org.junit.Assert
import org.junit.Test
import tornadofx.Controller

class PreferencesTest {

    @Test
    fun testPreferences() {
        object : Controller() {
            init {
                preferences("test app") {
                    putBoolean("boolean key", true)
                    put("string", "value")
                }

                var bool: Boolean = false
                var str: String = ""
                preferences("test app") {
                    bool = getBoolean("boolean key", false)
                    str = get("string", "")
                }
                Assert.assertEquals(true, bool)
                Assert.assertEquals("value", str)
            }
        }
    }
}
