package tornadofx

import org.junit.Assert
import org.junit.Test

import org.junit.Assert.*

class PreferencesKtTest {

    @Test
    fun testPreferences() {

        preferences("test app") {
            putBoolean("boolean key", true)
            put("string", "value")
        }

        var bool: Boolean = false
        var str: String = ""
        preferences {
            bool = getBoolean("boolean key", false)
            str = get("string", "")
        }
        Assert.assertEquals(true, bool)
        Assert.assertEquals("value", str)
    }
}