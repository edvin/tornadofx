package tornadofx.tests

import javafx.stage.Stage
import org.junit.Test
import org.testfx.api.FxToolkit
import tornadofx.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue


class ScopeTests {
    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    class PersonModel() : ItemViewModel<Person>()

    class PersonScope : Scope() {
        val model = PersonModel()
    }

    class C : Controller()

    class F : Fragment() {
        override val root = label()
        override val scope = super.scope as PersonScope
        val c : C by inject()
        val vm: VM by inject()
    }

    class VM : ViewModel(), Injectable

    @Test
    fun instanceCheck() {
        val scope1 = PersonScope()

        val obj_a = find(C::class, scope1)
        val obj_a1 = find(C::class, scope1)

        assertEquals(obj_a, obj_a1)

        val scope2 = PersonScope()

        val obj_a2 = find(C::class, scope2)

        assertNotEquals(obj_a, obj_a2)
    }

    @Test
    fun controllerAndViewModelPerScopeInInjectedFragments() {
        val scope1 = PersonScope()

        val f1 = find(F::class, scope1)
        val f2 = find(F::class, scope1)

        assertEquals(f1.c, f2.c)
        assertEquals(f1.vm, f2.vm)

        val scope2 = PersonScope()

        val f3 = find(F::class, scope2)

        assertTrue(f1.scope.model is PersonModel)
        assertNotEquals(f1.c, f3.c)
        assertNotEquals(f1.vm, f3.vm)
    }

}