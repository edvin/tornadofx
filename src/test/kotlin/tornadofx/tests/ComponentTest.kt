package tornadofx.tests

import javafx.stage.Stage
import org.junit.Test
import org.testfx.api.FxToolkit
import tornadofx.Fragment
import tornadofx.singleAssign
import tornadofx.vbox
//import tornadofx.*
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class ComponentTest {

    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    @Test
    fun testParam2() {

        val mainFragment = MainFragment()

        try {
            mainFragment.subFragmentNoParam.booleanParam
            fail("IllegalStateException should have been thrown")
        } catch (e: IllegalStateException) {
            // param not set
        }

        assertTrue(mainFragment.subFragmentNoParam.booleanParamWithDefault,
                "parameter value should match default value")

        assertFalse(mainFragment.subFragmentWithParam.booleanParam,
                "parameter value should match parameter passed in")

        assertNull(mainFragment.subFragmentWithParam.nullableBooleanParam,
                "nullable parameter value should match parameter passed in")
    }

    class MainFragment : Fragment() {

        var subFragmentNoParam: SubFragment by singleAssign()

        var subFragmentWithParam: SubFragment by singleAssign()

        override val root = vbox {
            add<SubFragment> {
                subFragmentNoParam = this
            }
            add<SubFragment>(
                SubFragment::booleanParam to false,
                SubFragment::nullableBooleanParam to null
            ){
                subFragmentWithParam = this
            }
        }

    }

    class SubFragment : Fragment() {
        val booleanParam: Boolean by param()
        val booleanParamWithDefault: Boolean by param(defaultValue = true)
        val nullableBooleanParam: Boolean? by param()
        override val root = vbox()
    }

}