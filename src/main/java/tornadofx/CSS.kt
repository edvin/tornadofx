package tornadofx

import javafx.scene.paint.Color
import java.text.DecimalFormat
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class CssBlock {
    val selections = mutableListOf<Selection>()

    fun s(selector: String, block: Selection.() -> Unit): Selection {
        val selection = Selection(selector)
        selection.block()
        selections += selection
        return selection
    }
}

open class SelectionBlock : CssBlock() {
    val properties = mutableMapOf<String, Any>()

    // All Nodes
    var blendMode: FXBlendMode by cssprop("-fx-blend-mode")
    // TODO: -fx-cursor
    // TODO: -fx-effect
    var focusTraversable: Boolean by cssprop("-fx-focus-traversable")
    var opacity: Double by cssprop("-fx-opacity")
    var rotate: Double by cssprop("-fx-rotate")
    var scaleX: Double by cssprop("-fx-scale-x")
    var scaleY: Double by cssprop("-fx-scale-y")
    var scaleZ: Double by cssprop("-fx-scale-z")
    var translateX: Double by cssprop("-fx-translate-x")
    var translateY: Double by cssprop("-fx-translate-y")
    var translateZ: Double by cssprop("-fx-translate-z")
    // TODO: -fx-visibility
    // Unsorted
    var padding: BoxDimensions by cssprop("-fx-padding")
    var spacing: LinearDimension by cssprop("-fx-spacing")
    var fontSize: LinearDimension by cssprop("-fx-font-size")
    var textFill: Color? by cssprop("-fx-text-fill")
    var backgroundColor: Color? by cssprop("-fx-background-color")

    private inline fun <reified V> cssprop(key: String): ReadWriteProperty<SelectionBlock, V> {
        return object : ReadWriteProperty<SelectionBlock, V> {
            override fun getValue(thisRef: SelectionBlock, property: KProperty<*>) = properties[key] as V

            override fun setValue(thisRef: SelectionBlock, property: KProperty<*>, value: V) {
                if (value != null) {
                    // Ignore nulls
                    properties[key] = value as Any
                }
            }
        }
    }
}

class Mixin : SelectionBlock()

class Selection(val selector: String) : SelectionBlock() {
    operator fun Mixin.unaryPlus() {
        this@Selection.properties.putAll(properties)
        this@Selection.selections.addAll(selections)
    }

    fun render(current: String = ""): String {
        return buildString {
            val currentSelector = "$current$selector "
            append("$currentSelector{\n")
            for ((name, value) in properties) {
                append("    $name: ")
                when (value) {
                    is Color -> {
                        append(value.toCss())
                    }
                    else -> append(value)
                }
                append(";\n")
            }
            append("}\n")
            for (selection in selections) {
                append(selection.render(currentSelector))
            }
        }
    }

    override fun toString() = render()
}

abstract class Stylesheet : CssBlock() {
    open fun render() = buildString { selections.forEach { append(it) } }

    fun mixin(init: Mixin.() -> Unit): Mixin {
        val mixin = Mixin()
        mixin.init()
        return mixin
    }

    override fun toString() = render()
}

// Helpers

val fiveDigits = DecimalFormat("#.#####")

// Colors

fun SelectionBlock.c(colorString: String, opacity: Double = 1.0) = try {
    Color.web(colorString, opacity)
} catch (e: Exception) {
    null
}

fun SelectionBlock.c(red: Double, green: Double, blue: Double, opacity: Double = 1.0) = try {
    Color.color(red, green, blue, opacity)
} catch (e: Exception) {
    null
}

fun SelectionBlock.c(red: Int, green: Int, blue: Int, opacity: Double = 1.0) = try {
    Color.rgb(red, green, blue, opacity)
} catch (e: Exception) {
    null
}

fun Color.toCss() = "rgba(${(red * 255).toInt()}, ${(green * 255).toInt()}, ${(blue * 255).toInt()}, ${fiveDigits.format(opacity)})"

// Dimensions

class LinearDimension(val value: Double, val units: Units) {
    override fun toString() = "${fiveDigits.format(value)}$units"

    enum class Units(val value: String) {
        px("px"),
        mm("mm"),
        cm("cm"),
        inches("in"),
        pt("pt"),
        pc("pc"),
        em("em"),
        ex("ex"),
        percent("%");

        override fun toString() = value
    }
}

val Number.px: LinearDimension get() = LinearDimension(this.toDouble(), LinearDimension.Units.px)
val Number.mm: LinearDimension get() = LinearDimension(this.toDouble(), LinearDimension.Units.mm)
val Number.cm: LinearDimension get() = LinearDimension(this.toDouble(), LinearDimension.Units.cm)
val Number.inches: LinearDimension get() = LinearDimension(this.toDouble(), LinearDimension.Units.inches)
val Number.pt: LinearDimension get() = LinearDimension(this.toDouble(), LinearDimension.Units.pt)
val Number.pc: LinearDimension get() = LinearDimension(this.toDouble(), LinearDimension.Units.pc)
val Number.em: LinearDimension get() = LinearDimension(this.toDouble(), LinearDimension.Units.em)
val Number.ex: LinearDimension get() = LinearDimension(this.toDouble(), LinearDimension.Units.ex)
val Number.percent: LinearDimension get() = LinearDimension(this.toDouble(), LinearDimension.Units.percent)

class BoxDimensions(val top: LinearDimension, val right: LinearDimension, val bottom: LinearDimension, val left: LinearDimension) {
    override fun toString() = "$top $right $bottom $left"
}

fun SelectionBlock.box(dimensions: LinearDimension): BoxDimensions = BoxDimensions(dimensions, dimensions, dimensions, dimensions)
fun SelectionBlock.box(vertical: LinearDimension, horizontal: LinearDimension) = BoxDimensions(vertical, horizontal, vertical, horizontal)
fun SelectionBlock.box(top: LinearDimension, right: LinearDimension, bottom: LinearDimension, left: LinearDimension) = BoxDimensions(top, right, bottom, left)

class AngularDimension(val value: Double, val units: Units) {
    override fun toString() = "${fiveDigits.format(value)}$units"

    enum class Units(val value: String) {
        deg("deg"),
        rad("rad"),
        grad("grad"),
        turn("turn");

        override fun toString() = value
    }
}

val Number.deg: AngularDimension get() = AngularDimension(this.toDouble(), AngularDimension.Units.deg)
val Number.rad: AngularDimension get() = AngularDimension(this.toDouble(), AngularDimension.Units.rad)
val Number.grad: AngularDimension get() = AngularDimension(this.toDouble(), AngularDimension.Units.grad)
val Number.turn: AngularDimension get() = AngularDimension(this.toDouble(), AngularDimension.Units.turn)

class TemporalDimensions(val value: Double, val units: Units) {
    override fun toString() = "${fiveDigits.format(value)}$units"

    enum class Units(val value: String) {
        s("s"),
        ms("ms");

        override fun toString() = value
    }
}

val Number.s: TemporalDimensions get() = TemporalDimensions(this.toDouble(), TemporalDimensions.Units.s)
val Number.ms: TemporalDimensions get() = TemporalDimensions(this.toDouble(), TemporalDimensions.Units.ms)

// Enums

enum class FXBlendMode(val value: String) {
    add("add"),
    blue("blue"),
    colorBurn("color-burn"),
    colorDodge("color-dodge"),
    darken("darken"),
    difference("difference"),
    exclusion("exclusion"),
    green("green"),
    hardLight("hard-light"),
    lighten("lighten"),
    multiply("multiply"),
    overlay("overlay"),
    red("red"),
    screen("screen"),
    softLight("soft-light"),
    srcAtop("src-atop"),
    srcIn("src-in"),
    srcOut("src-out"),
    srcOver("src-over");

    override fun toString() = value
}
