package tornadofx

import org.junit.Assert
import org.junit.Test

class StylesheetTests {


    @Test
    fun nestedModifier_1() {
        stylesheet {
            s(".label, .text") {
                +s(":hover, :armed") {
                    backgroundColor = c("blue", 0.25)
                }
            }


        } shouldEqual {
            """
            .label:hover, .text:hover, .label:armed, .text:armed {
                -fx-background-color: rgba(0, 0, 255, 0.25);
            }
            """
        }

    }

    private fun stylesheet(op: Stylesheet.() -> Unit) =
            Stylesheet().apply { op(this) }

    infix fun Stylesheet.shouldEqual(op: () -> String) {
        Assert.assertEquals(op().strip(), render().strip())
    }

    private fun String.strip() = replace(Regex("\\s+"), " ").trim()

}


