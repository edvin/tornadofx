package tornadofx.tests

import javafx.scene.layout.StackPane
import javafx.scene.text.Text
import javafx.stage.Stage
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.testfx.api.FxAssert.verifyThat
import org.testfx.api.FxToolkit
import org.testfx.matcher.base.NodeMatchers
import tornadofx.text

class BuildersTest {
    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    lateinit var pane: StackPane

    @Before
    fun setup() {
        pane = StackPane()
    }

    @Test
    fun text_builder() {
        // expect:
        verifyThat(pane.text(), Matchers.instanceOf(Text::class.java))
        verifyThat(pane.text(), NodeMatchers.hasText(""))
        verifyThat(pane.text("foo"), NodeMatchers.hasText("foo"))
        verifyThat(pane.text() { text = "bar" }, NodeMatchers.hasText("bar"))
    }

}
