package tornadofx

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

    var fontSize: Any by properties

    fun prop(rule: String, value: Any, isFx: Boolean = true): Pair<String, Any> {
        val prop = Pair(if (isFx) "-fx-$rule" else rule, value)
        properties += prop
        return prop
    }
}

class Selection(val selector: String) : PropertyChunk() {
    override fun toString(): String {
        return buildString {
            append("$selector {\n")
            for ((name, value) in properties) {
                append("  $name: $value;\n")
            }
            append("}\n\n")
            for (selection in selections) {
                append("$selector $selection")
            }
        }
    }
}

abstract class Stylesheet : StyleChunk() {
    fun render() = buildString { selections.forEach { append(it) } }

    override fun toString() = render()
}