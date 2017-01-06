package tornadofx.tests

import javafx.stage.Stage
import org.junit.Test
import org.testfx.api.FxToolkit
import tornadofx.*
import kotlin.test.*

class ComponentTests {

    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    @Test
    fun testParam2() {

        val mainFragment = MainFragment()

        try {
            mainFragment.subFragmentNoParam.booleanParam
            fail("IllegalStateException should have been thrown")
        } catch ( e: IllegalStateException ){
            // param not set
        }

        assertTrue(mainFragment.subFragmentNoParam.booleanParamWithDefault,
                "parameter value should match default value")

        assertFalse(mainFragment.subFragmentWithParam.booleanParam,
                "parameter value should match parameter passed in")

    }

    @Test
    fun myTest() {

        val component = object: Fragment(){
            override val root = vbox{
                add( SubFragment::class )
            }

        }

    }

    class MainFragment : Fragment() {

        var subFragmentNoParam: SubFragment by singleAssign()

        var subFragmentWithParam: SubFragment by singleAssign()

        override val root = vbox{
            add( SubFragment::class ){
                subFragmentNoParam = this
            }
            add( SubFragment::class, "booleanParam" to false ){
                subFragmentWithParam = this
            }
        }

    }

    class SubFragment : Fragment() {

        val booleanParam : Boolean by param()
        val booleanParamWithDefault : Boolean by param( defaultValue = true )

        override val root = vbox()
    }

}