package tornadofx.tests

import javafx.css.PseudoClass
import javafx.css.Styleable
import javafx.scene.control.*
import javafx.scene.effect.BlurType
import javafx.scene.effect.InnerShadow
import javafx.scene.layout.Pane
import javafx.scene.paint.*
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.stage.Stage
import org.junit.Assert
import org.junit.Test
import org.testfx.api.FxToolkit
import tornadofx.*
import tornadofx.Stylesheet.Companion.armed
import tornadofx.Stylesheet.Companion.barChart
import tornadofx.Stylesheet.Companion.hover
import tornadofx.Stylesheet.Companion.imageView
import tornadofx.Stylesheet.Companion.label
import tornadofx.Stylesheet.Companion.star
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StylesheetTest {
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
    val multiProp by cssproperty<MultiValue<Paint>>()

    val lumpyElement by csselement()
    val lumpyId by cssid()
    val lumpyClass by cssclass()
    val lumpyPseudoClass by csspseudoclass()

    val TestBox by cssclass()
    val TestBox2 by cssclass("TestBox")

    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    val renderedProp by cssproperty<String> { "${it.toUpperCase()}!!!" }
    val renderedBool by cssproperty<Bool> { it.name }
    val renderedMulti by cssproperty<MultiValue<String>> { it.elements.joinToString { "${it.toUpperCase()}!!!" } }

    enum class Bool { TRUE, FALSE, FILE_NOT_FOUND }

    @Test
    fun colorLadderTest() {
        val stops = arrayOf(
                Stop(0.0, Color.TRANSPARENT),
                Stop(1.0, Color.WHITE),
                Stop(0.5, Color.RED),  // Should be left side of 0.2
                Stop(0.5, Color.GREEN),  // Should be ignored
                Stop(0.5, Color.BLUE)  // Should be right side of 0.2
        )
        Color.hsb(0.0, 1.0, 0.3).ladder(*stops) shouldEqual Color(0.6, 0.0, 0.0, 0.6)
        Color.hsb(0.0, 1.0, 0.6).ladder(*stops) shouldEqual Color(0.2, 0.2, 1.0, 1.0)
        Color.hsb(0.0, 1.0, 0.4999).ladder(*stops) shouldEqual Color.RED
        Color.hsb(0.0, 1.0, 0.5).ladder(*stops) shouldEqual Color.BLUE
    }

    @Test
    fun colorDeriveTest() {
        val color = Color.hsb(0.0, 1.0, 0.5, 0.8)
        color.derive(0.5) shouldEqual Color(0.75, 0.5, 0.5, 0.8)
        color.derive(-0.5) shouldEqual Color(0.25, 0.0, 0.0, 0.8)
    }

    @Test
    fun useCustomRenderer() {
        stylesheet {
            label {
                renderedProp.value = "bang"
                renderedBool.value = Bool.FILE_NOT_FOUND
                renderedMulti.value += "car"
                renderedMulti.value += "truck"
                renderedMulti.value += "van"
                renderedMulti.value += "suv"
            }
        }.shouldEqual {
            """
            .label {
                rendered-prop: BANG!!!;
                rendered-bool: FILE_NOT_FOUND;
                rendered-multi: CAR!!!, TRUCK!!!, VAN!!!, SUV!!!;
            }
            """
        }
    }

    @Test
    fun addRemovePseudoClass() {
        val node = Label()
        node.addClass(lumpyPseudoClass)
        assertTrue(node.hasClass(lumpyPseudoClass))

        assertFalse(node.styleClass.contains(lumpyPseudoClass.name))
        assertTrue(node.pseudoClassStates.contains(PseudoClass.getPseudoClass(lumpyPseudoClass.name)))

        node.toggleClass(lumpyPseudoClass, false)
        assertFalse(node.hasClass(lumpyPseudoClass))
    }

    @Test
    fun cssStringSnake() {
        stylesheet {
            "HBox > .labelThing" {
                textFill = c("red")
            }
        }.shouldEqual {
            """
            HBox > .labelThing {
                -fx-text-fill: rgba(255, 0, 0, 1);
            }
            """
        }
    }

    @Test
    fun snakeCase() {
        stylesheet {
            TestBox {
                textFill = c("red")
            }
            TestBox2 {
                textFill = c("green")
            }
        } shouldEqual {
            """
            .test-box {
                -fx-text-fill: rgba(255, 0, 0, 1);
            }
            .TestBox {
                -fx-text-fill: rgba(0, 128, 0, 1);
            }
            """
        }
    }

    @Test
    fun uriStyleTest() {
        stylesheet {
            label {
                image = URI("/image.png")
                backgroundImage += URI("/back1.jpg")
                backgroundImage += URI("/back2.gif")
            }
        } shouldEqual {
            """
            .label {
                -fx-image: url("/image.png");
                -fx-background-image: url("/back1.jpg"), url("/back2.gif");
            }
            """
        }
    }

    @Test
    fun dimensionalAnalysis() {
        val base = 10.px
        val num = 2
        val dim = 2.px
        val disjoint = 5.mm

        assert((-10).px == -base)

        assert(12.px == base + num)
        assert(8.px == base - num)
        assert(20.px == base * num)
        assert(5.px == base / num)
        assert(0.px == base.mod(num))
        assert(3.px == base.mod(7))

        assert(12.px == base + dim)
        assert(8.px == base - dim)

        assert(12.px == num + base)
        assert(-8.px == num - base)
        assert(20.px == num * base)

        assertFails { base + disjoint }
        assertFails { base - disjoint }
    }

    @Test
    fun unsafeProperties() {
        stylesheet {
            label {
                unsafe("-fx-background", raw("-fx-control-inner-background"))
                textFill = Color.GREEN
                unsafe(base, raw("green"))
                unsafe("base", Color.GREEN)
                multiProp force base
            }
        } shouldEqual {
            """
            .label {
                -fx-text-fill: rgba(0, 128, 0, 1);
                -fx-background: -fx-control-inner-background;
                -fx-base: green;
                base: rgba(0, 128, 0, 1);
                multi-prop: -fx-base;
            }
            """
        }
    }

    @Test
    fun starTest() {
        stylesheet {
            box child star {
                textFill = Color.BLUE
            }
        } shouldEqual {
            """
            .box > * {
                -fx-text-fill: rgba(0, 0, 255, 1);
            }
            """
        }
    }

    @Test
    fun lumpySnakes() {
        stylesheet {
            s(lumpyElement, lumpyId, lumpyClass, lumpyPseudoClass) {
                multiProp.value += Color.RED
            }
        } shouldEqual {
            """
            lumpy-element, #lumpy-id, .lumpy-class, :lumpy-pseudo-class {
                multi-prop: rgba(255, 0, 0, 1);
            }
            """
        }
    }

    @Test
    fun multiProp() {
        stylesheet {
            val mix = mixin {
                multiProp.value += Color.GREEN
            }
            label {
                multiProp.value += Color.RED
                +mix
                multiProp.value += Color.BLUE
            }
        } shouldEqual {
            """
            .label {
                multi-prop: rgba(255, 0, 0, 1), rgba(0, 128, 0, 1), rgba(0, 0, 255, 1);
            }
            """
        }
    }

    @Test
    fun propertySelectionScope() {
        stylesheet {
            label {
                and(":hover") {
                    base.value = c("blue")
                }
                base.value = c("red")
            }
        } shouldEqual {
            """
            .label {
                -fx-base: rgba(255, 0, 0, 1);
            }
            .label:hover {
                -fx-base: rgba(0, 0, 255, 1);
            }
            """
        }
    }

    @Test
    fun splitting() {
        stylesheet {
            "label     >.lab   #la:l          ,.label,.-la-la~*:red,           #fred    " {
                textFill = Color.BLANCHEDALMOND
                and(":hover") {
                    backgroundColor += Color.CHARTREUSE
                    base.value = c("green")
                }
            }
        } shouldEqual {
            """
            label > .lab #la:l, .label, .-la-la ~ *:red, #fred {
                -fx-text-fill: rgba(255, 235, 205, 1);
            }

            label > .lab #la:l:hover, .label:hover, .-la-la ~ *:red:hover, #fred:hover {
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
                and(d, e, f) {
                    and(g, h, i) {
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
                and(d, e, f) {
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
                and(hover, armed) {
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
                and(hover) {
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
        val arrayOfStyleables = arrayOf<Styleable>(Pane(), MenuItem("Click Me"), Tooltip("Click"), Tab("This Tab"), TableColumn<Int, String>())
        for (node in arrayOfStyleables) {
            node.style {
                backgroundColor += Color.RED
            }
            assertEquals("-fx-background-color: rgba(255, 0, 0, 1);", node.style)
            node.style(append = true) {
                padding = box(10.px)
            }
            assertEquals("-fx-background-color: rgba(255, 0, 0, 1); -fx-padding: 10px 10px 10px 10px;", node.style)
        }
    }

    @Test
    fun innerShadowRendering() {
        stylesheet {
            s(imageView) {
                effect = InnerShadow(BlurType.GAUSSIAN, Color.GREENYELLOW, 7.0, 1.0, 1.0, 1.0)
            }
        } shouldEqual {
            """
            .image-view {
                -fx-effect: innershadow(gaussian, rgba(173, 255, 47, 1), 7.0, 1.0, 1.0, 1.0);
            }
            """
        }
    }

    @Test
    fun barFillTest() {
        stylesheet {
            s(barChart) {
                barFill = Color.RED
            }
        } shouldEqual {
            """
            .bar-chart {
                -fx-bar-fill: rgba(255, 0, 0, 1);
            }
            """
        }
    }

    private fun stylesheet(op: Stylesheet.() -> Unit) = Stylesheet().apply(op)
    infix fun Stylesheet.shouldEqual(op: () -> String) = Assert.assertEquals(op().strip(), render().strip())
    infix fun Color.shouldEqual(other: Color) = Assert.assertEquals(other.toString(), toString())
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
