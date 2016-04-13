package tornadofx

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class StyleChunk {
    val selections = mutableListOf<Selection>()

    fun s(selector: String, block: Selection.() -> Unit): Selection {
        val selection = Selection(selector)
        selection.block()
        selections += selection
        return selection
    }
}

open class PropertyChunk : StyleChunk() {
    val properties = mutableMapOf<String, Any>()

    var fontSize: Any by map(properties, "-fx-font-size")
    var textFill: Any by map(properties, "-fx-text-fill")
    var backgroundColor: Any by map(properties, "-fx-background-color")

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
        get() = "${this}%"
    // Angle
    val Number.deg: String
        get() = "${this}deg"
    val Number.rad: String
        get() = "${this}rad"
    val Number.grad: String
        get() = "${this}grad"
    val Number.turn: String
        get() = "${this}turn"

    fun prop(rule: String, value: Any, isFx: Boolean = true): Pair<String, Any> {
        val prop = Pair(if (isFx) "-fx-$rule" else rule, value)
        properties += prop
        return prop
    }
}

class Mixin : PropertyChunk()

class Selection(val selector: String) : PropertyChunk() {
    operator fun Mixin.unaryPlus() {
        this@Selection.properties.putAll(properties)
        this@Selection.selections.addAll(selections)
    }

    fun render(current: String = ""): String {
        return buildString {
            val current = "$current$selector "
            append("$current{\n")
            for ((name, value) in properties) {
                append("    $name: $value;\n")
            }
            append("}\n")
            for (selection in selections) {
                append(selection.render(current))
            }
        }
    }

    override fun toString() = render()
}

abstract class Stylesheet : StyleChunk() {
    open fun render() = buildString { selections.forEach { append(it) } }

    fun mixin(init: Mixin.() -> Unit): Mixin {
        val mixin = Mixin()
        mixin.init()
        return mixin
    }

    override fun toString() = render()
}

// Helpers

fun <K, V> K.map(properties: MutableMap<String, V>, key: String): ReadWriteProperty<K, V> {
    return object : ReadWriteProperty<K, V> {
        override fun getValue(thisRef: K, property: KProperty<*>) = properties[key]!!

        override fun setValue(thisRef: K, property: KProperty<*>, value: V) {
            properties[key] = value
        }
    }
}