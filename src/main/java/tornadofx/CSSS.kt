package tornadofx

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

class Mixin(): InnerChunk {
    override val selections = mutableListOf<Selection>()
    override val properties = mutableMapOf<String, Any>()
}

class StyleSheet() : StyleChunk {
    override val selections = mutableListOf<Selection>()

    val Int.px: String
        get() = "${this}px"
    val Int.percent: String
        get() = "$this%"
    val Double.percent: String
        get() = "$this%"

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

fun css(init: StyleSheet.() -> Unit): StyleSheet {
    val style = StyleSheet()
    style.init()
    return style
}

fun mixin(init: Mixin.() -> Unit): Mixin {
    val mixin = Mixin()
    mixin.init()
    return mixin
}