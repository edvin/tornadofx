package tornadofx.tests

import org.junit.Test
import tornadofx.Scope
import kotlin.test.assertNotEquals

class ScopeTests {

    @Test
    fun scopeEquals() {
        val scope1 = Scope()
        val scope2 = Scope()
        assertNotEquals(scope1, scope2)
    }
}