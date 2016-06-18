package tornadofx

import javafx.scene.layout.Pane
import javafx.scene.paint.*
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import org.junit.Assert
import org.junit.Test
import tornadofx.Stylesheet.Companion.armed
import tornadofx.Stylesheet.Companion.hover
import tornadofx.Stylesheet.Companion.label
import kotlin.test.assertEquals

class StylesheetTests {
    val vbox by cssclass()
    val wrapper by cssclass()

    val text by cssclass()
    val box by cssclass()

    val a by cssclass()
    val b by cssclass()
    val c by cssclass()
    val d by cssclass()
    val e by cssclass()
    val f by cssclass()
    val g by cssclass()
    val h by cssclass()
    val i by cssclass()

    val base by cssproperty<Paint>("-fx-base")

    @Test
    fun splitting() {
        stylesheet {
            "label     >.lab   #la:l          ,.label,           #fred    " {
                textFill = Color.BLANCHEDALMOND
                add(":hover") {
                    backgroundColor += Color.CHARTREUSE
                    base.value = c("green")
                }
            }
        } shouldEqual {
            """
            label > .lab #la:l, .label, #fred {
                -fx-text-fill: rgba(255, 235, 205, 1);
            }

            label > .lab #la:l:hover, .label:hover, #fred:hover {
                -fx-background-color: rgba(127, 255, 0, 1);
                -fx-base: rgba(0, 128, 0, 1);
            }
            """
        }
    }

    @Test
    fun cartesian() {
        stylesheet {
            s(a, b, c) {
                s(d, e, f) {
                    s(g, h, i) {
                        textFill = Color.BLUE
                    }
                }
            }
        } shouldEqual {
            """
            .a .d .g, .a .d .h, .a .d .i, .a .e .g, .a .e .h, .a .e .i, .a .f .g, .a .f .h, .a .f .i, .b .d .g, .b .d .h, .b .d .i, .b .e .g, .b .e .h, .b .e .i, .b .f .g, .b .f .h, .b .f .i, .c .d .g, .c .d .h, .c .d .i, .c .e .g, .c .e .h, .c .e .i, .c .f .g, .c .f .h, .c .f .i {
                -fx-text-fill: rgba(0, 0, 255, 1);
            }
            """
        }

        stylesheet {
            s(a, b, c) {
                add(d, e, f) {
                    add(g, h, i) {
                        textFill = Color.BLUE
                    }
                }
            }
        } shouldEqual {
            """
            .a.d.g, .a.d.h, .a.d.i, .a.e.g, .a.e.h, .a.e.i, .a.f.g, .a.f.h, .a.f.i, .b.d.g, .b.d.h, .b.d.i, .b.e.g, .b.e.h, .b.e.i, .b.f.g, .b.f.h, .b.f.i, .c.d.g, .c.d.h, .c.d.i, .c.e.g, .c.e.h, .c.e.i, .c.f.g, .c.f.h, .c.f.i {
                -fx-text-fill: rgba(0, 0, 255, 1);
            }
            """
        }

        stylesheet {
            s(a, b, c) {
                add(d, e, f) {
                    s(g, h, i) {
                        textFill = Color.BLUE
                    }
                }
            }
        } shouldEqual {
            """
            .a.d .g, .a.d .h, .a.d .i, .a.e .g, .a.e .h, .a.e .i, .a.f .g, .a.f .h, .a.f .i, .b.d .g, .b.d .h, .b.d .i, .b.e .g, .b.e .h, .b.e .i, .b.f .g, .b.f .h, .b.f .i, .c.d .g, .c.d .h, .c.d .i, .c.e .g, .c.e .h, .c.e .i, .c.f .g, .c.f .h, .c.f .i {
                -fx-text-fill: rgba(0, 0, 255, 1);
            }
            """
        }
    }

    @Test
    fun multiValue() {
        stylesheet {
            label {
                backgroundColor = multi(Color.WHITE, Color.BLUE)
            }
        } shouldEqual {
            ".label { -fx-background-color: rgba(255, 255, 255, 1), rgba(0, 0, 255, 1); }"
        }
    }

    @Test
    fun singleValue() {
        stylesheet {
            label {
                backgroundColor += Color.WHITE
            }
        } shouldEqual {
            ".label { -fx-background-color: rgba(255, 255, 255, 1); }"
        }
    }

    @Test
    fun selectorOrder() {
        stylesheet {
            vbox child wrapper contains label {
                backgroundColor += Color.WHITE
            }
        } shouldEqual {
            ".vbox > .wrapper .label { -fx-background-color: rgba(255, 255, 255, 1); }"
        }
    }

    @Test
    fun multiSelect() {
        stylesheet {
            s(vbox child wrapper contains label, label) {
                textFill = Color.BLUE
            }
        } shouldEqual {
            """
            .vbox > .wrapper .label, .label {
                -fx-text-fill: rgba(0, 0, 255, 1);
            }
            """
        }
    }

    @Test
    fun nestedModifier_1() {
        stylesheet {
            s(label, text) {
                add(hover, armed) {
                    backgroundColor += c("blue", 0.25)
                }
            }
        } shouldEqual {
            """
        .label:hover, .label:armed, .text:hover, .text:armed {
            -fx-background-color: rgba(0, 0, 255, 0.25);
        }
        """
        }
    }

    @Test
    fun gradientsWithErrorColor() {
        stylesheet {
            val hover = mixin {
                add(hover) {
                    backgroundColor += RadialGradient(90.0, 0.5, 0.5, 0.5, 0.25, true, CycleMethod.REPEAT, Stop(0.0, Color.WHITE), Stop(0.5, c("error")), Stop(1.0, Color.BLACK))
                }
            }
            val wrap = mixin {
                padding = box(1.em)
                borderColor += box(LinearGradient(0.0, 0.0, 10.0, 10.0, false, CycleMethod.REFLECT, Stop(0.0, Color.RED), Stop(1.0, c(0.0, 1.0, 0.0))))
                borderWidth += box(5.px)
                backgroundRadius += box(25.px)
                borderRadius += box(25.px)
                +hover
            }

            box {
                +wrap
                backgroundColor += RadialGradient(90.0, 0.5, 0.5, 0.5, 0.25, true, CycleMethod.REPEAT, Stop(0.0, Color.WHITE), Stop(1.0, Color.BLACK))
                spacing = 5.px

                label {
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

    @Test
    fun inlineStyle() {
        val node = Pane()
        node.style {
            backgroundColor += Color.RED
        }
        assertEquals("-fx-background-color: rgba(255, 0, 0, 1);", node.style)
        node.style(append = true) {
            padding = box(10.px)
        }
        assertEquals("-fx-background-color: rgba(255, 0, 0, 1); -fx-padding: 10px 10px 10px 10px;", node.style)
    }

    private fun stylesheet(op: Stylesheet.() -> Unit) = Stylesheet().apply(op)
    infix fun Stylesheet.shouldEqual(op: () -> String) = Assert.assertEquals(op().strip(), render().strip())
    private fun String.strip() = replace(Regex("\\s+"), " ").trim()

    /**
     * This is just a compile test to make sure box and c are not moved
     */
    class StylesheetFunctionsTest : Stylesheet() {
        companion object {
            val aColor = c("#335566")
            val boxDims = box(20.px)
        }
    }
}
