package tornadofx.tests

import javafx.scene.control.Label
import javafx.stage.Stage
import org.junit.Test
import org.testfx.api.FxToolkit
import tornadofx.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals


class ScopeTests {
    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    class C : Controller()

    class F : Fragment() {
        override val root = label()
        val c : C by inject()
        val vm: VM by inject()
    }

    class VM : ViewModel(), Injectable

    @Test
    fun instanceCheck() {
        val scope1 = Scope()

        val obj_a = find(C::class, scope1)
        val obj_a1 = find(C::class, scope1)

        assertEquals(obj_a, obj_a1)

        val scope2 = Scope()

        val obj_a2 = find(C::class, scope2)

        assertNotEquals(obj_a, obj_a2)
    }

    @Test
    fun controllerAndViewModelPerScopeInInjectedFragments() {
        val scope1 = Scope()

        val f1 = find(F::class, scope1)
        val f2 = find(F::class, scope1)

        assertEquals(f1.c, f2.c)
        assertEquals(f1.vm, f2.vm)

        val scope2 = Scope()

        val f3 = find(F::class, scope2)

        assertNotEquals(f1.c, f3.c)
        assertNotEquals(f1.vm, f3.vm)
    }

}