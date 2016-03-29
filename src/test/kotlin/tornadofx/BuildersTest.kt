package tornadofx

import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import javafx.stage.Stage
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.testfx.api.FxAssert.verifyThat
import org.testfx.api.FxRobot
import org.testfx.api.FxToolkit
import org.testfx.matcher.base.NodeMatchers
import java.nio.file.Paths

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

    @Test
    fun wiki_example_1() {
        class Example1 : View() {
            override val root = VBox()

            init {
                with(root) {
                    hbox {
                        label("First Name") {
                            hboxConstraints { margin = Insets(5.0) }
                        }
                        textfield {
                            hboxConstraints { margin = Insets(5.0) }
                            useMaxWidth = true
                        }
                    }
                    hbox {
                        label("Last Name") {
                            hboxConstraints { margin = Insets(5.0) }
                        }
                        textfield {
                            hboxConstraints { margin = Insets(5.0) }
                            useMaxWidth = true
                        }
                    }
                }
            }
        }

        FxToolkit.setupFixture {
            val root = Example1().root
            primaryStage.scene = Scene(root)
            primaryStage.show()
        }

        val robot = FxRobot()
        robot.robotContext().captureSupport.saveImage(
            robot.capture(primaryStage.scene.root), Paths.get("example-1.png"))
    }

}
