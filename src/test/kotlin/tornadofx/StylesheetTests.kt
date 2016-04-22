package tornadofx

import javafx.scene.paint.*
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
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

    @Test
    fun gradientsWithErrorColor() {
        stylesheet {
            val hover = mixin {
                +s(":hover") {
                    backgroundColor = RadialGradient(90.0, 0.5, 0.5, 0.5, 0.25, true, CycleMethod.REPEAT, Stop(0.0, Color.WHITE), Stop(0.5, c("error")), Stop(1.0, Color.BLACK))
                }
            }
            val wrap = mixin {
                padding = box(1.em)
                borderColor = box(LinearGradient(0.0, 0.0, 10.0, 10.0, false, CycleMethod.REFLECT, Stop(0.0, Color.RED), Stop(1.0, c(0.0, 1.0, 0.0))))
                borderWidth = box(5.px)
                backgroundRadius = box(25.px)
                borderRadius = box(25.px)
                +hover
            }

            s(".box") {
                +wrap
                backgroundColor = RadialGradient(90.0, 0.5, 0.5, 0.5, 0.25, true, CycleMethod.REPEAT, Stop(0.0, Color.WHITE), Stop(1.0, Color.BLACK))
                spacing = 5.px

                s(".label") {
                    +wrap
                    font = Font.font(14.0)
                    fontWeight = FontWeight.BOLD
                    textFill = c("white")
                    rotate = .95.turn
                    translateX = .5.inches
                    minHeight = 6.em
                    scaleX = 2
                    scaleY = .75
                }
            }
        } shouldEqual {
            """
            .box {
                -fx-padding: 1em 1em 1em 1em;
                -fx-border-color: linear-gradient(from 0.0px 0.0px to 10.0px 10.0px, reflect, rgba(255, 0, 0, 1) 0.0%, rgba(0, 255, 0, 1) 100.0%) linear-gradient(from 0.0px 0.0px to 10.0px 10.0px, reflect, rgba(255, 0, 0, 1) 0.0%, rgba(0, 255, 0, 1) 100.0%) linear-gradient(from 0.0px 0.0px to 10.0px 10.0px, reflect, rgba(255, 0, 0, 1) 0.0%, rgba(0, 255, 0, 1) 100.0%) linear-gradient(from 0.0px 0.0px to 10.0px 10.0px, reflect, rgba(255, 0, 0, 1) 0.0%, rgba(0, 255, 0, 1) 100.0%);
                -fx-border-width: 5px 5px 5px 5px;
                -fx-background-radius: 25px 25px 25px 25px;
                -fx-border-radius: 25px 25px 25px 25px;
                -fx-background-color: radial-gradient(focus-angle 90.0deg, focus-distance 50.0% , center 50.0% 50.0%, radius 25.0%, repeat, rgba(255, 255, 255, 1) 0.0%, rgba(0, 0, 0, 1) 100.0%);
                -fx-spacing: 5px;
            }
            .box:hover {
                -fx-background-color: radial-gradient(focus-angle 90.0deg, focus-distance 50.0% , center 50.0% 50.0%, radius 25.0%, repeat, rgba(255, 255, 255, 1) 0.0%, rgba(255, 0, 255, 1) 50.0%, rgba(0, 0, 0, 1) 100.0%);
            }
            .box .label {
                -fx-padding: 1em 1em 1em 1em;
                -fx-border-color: linear-gradient(from 0.0px 0.0px to 10.0px 10.0px, reflect, rgba(255, 0, 0, 1) 0.0%, rgba(0, 255, 0, 1) 100.0%) linear-gradient(from 0.0px 0.0px to 10.0px 10.0px, reflect, rgba(255, 0, 0, 1) 0.0%, rgba(0, 255, 0, 1) 100.0%) linear-gradient(from 0.0px 0.0px to 10.0px 10.0px, reflect, rgba(255, 0, 0, 1) 0.0%, rgba(0, 255, 0, 1) 100.0%) linear-gradient(from 0.0px 0.0px to 10.0px 10.0px, reflect, rgba(255, 0, 0, 1) 0.0%, rgba(0, 255, 0, 1) 100.0%);
                -fx-border-width: 5px 5px 5px 5px;
                -fx-background-radius: 25px 25px 25px 25px;
                -fx-border-radius: 25px 25px 25px 25px;
                -fx-font: normal 14.0pt "System";
                -fx-font-weight: 700;
                -fx-text-fill: rgba(255, 255, 255, 1);
                -fx-rotate: 0.95turn;
                -fx-translate-x: 0.5in;
                -fx-min-height: 6em;
                -fx-scale-x: 2;
                -fx-scale-y: 0.75;
            }
            .box .label:hover {
                -fx-background-color: radial-gradient(focus-angle 90.0deg, focus-distance 50.0% , center 50.0% 50.0%, radius 25.0%, repeat, rgba(255, 255, 255, 1) 0.0%, rgba(255, 0, 255, 1) 50.0%, rgba(0, 0, 0, 1) 100.0%);
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


