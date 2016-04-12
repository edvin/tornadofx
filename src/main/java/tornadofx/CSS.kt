package tornadofx

import javafx.scene.Parent

interface StyleChunk {
    val selections: MutableList<Selection>

    fun s(selector: String, block: Selection.() -> Unit): Selection {
        val selection = Selection(selector)
        selection.block()
        selections += selection
        return selection
    }
}

interface PropertyChunk {
    val properties: MutableMap<String, Any>

    fun prop(rule: String, value: Any, isFx: Boolean = true): Pair<String, Any> {
        val prop = Pair(if (isFx) "-fx-$rule" else rule, value)
        properties += prop
        return prop
    }
}

interface InnerChunk : StyleChunk, PropertyChunk

class Mixin() : InnerChunk {
    override val selections = mutableListOf<Selection>()
    override val properties = mutableMapOf<String, Any>()
}

class StyleSheet() : StyleChunk {
    override val selections = mutableListOf<Selection>()

    // Sizes
    val Number.px: String
        get() = "${this}px"
    val Number.mm: String
        get() = "${this}mm"
    val Number.cm: String
        get() = "${this}cm"
    val Number.inches: String
        get() = "${this}in"
    val Number.pt: String
        get() = "${this}pt"
    val Number.pc: String
        get() = "${this}pc"
    val Number.em: String
        get() = "${this}em"
    val Number.ex: String
        get() = "${this}ex"
    // Percent
    val Number.percent: String
        get() = "$this%"
    // Angle
    val Number.deg: String
        get() = "${this}deg"
    val Number.rad: String
        get() = "${this}rad"
    val Number.grad: String
        get() = "${this}grad"
    val Number.turn: String
        get() = "${this}turn"

    fun mixin(init: Mixin.() -> Unit): Mixin {
        val mixin = Mixin()
        mixin.init()
        return mixin
    }

    override fun toString(): String {
        return buildString {
            selections.forEach {
                append(it.toString())
                append("\n")
            }
        }
    }
}

class Selection(val selector: String) : InnerChunk {
    override val selections = mutableListOf<Selection>()
    override val properties = mutableMapOf<String, Any>()

    operator fun Mixin.unaryPlus() {
        this@Selection.selections.addAll(selections)
        this@Selection.properties.putAll(properties)
    }

    override fun toString(): String {
        return buildString {
            append("$selector {\n")
            for ((name, value) in properties) {
                append("    $name: $value;\n")
            }
            append("}\n\n")
            selections.forEach {
                append("$selector ")
                append(it.toString())
            }
        }
    }
}

fun Parent.css(init: StyleSheet.() -> Unit): StyleSheet {
    val stylesheet = StyleSheet()
    stylesheet.init()
    println(stylesheet)
    // TODO: Add stylesheet to root
    return stylesheet
}