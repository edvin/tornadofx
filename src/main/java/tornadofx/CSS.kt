package tornadofx

abstract class Stylesheet {
    abstract fun render(): String
}

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

class Selection(val selector: String) : InnerChunk {
    override val selections = mutableListOf<Selection>()
    override val properties = mutableMapOf<String, Any>()

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

abstract class SS2 : Stylesheet(), StyleChunk {
    override val selections = mutableListOf<Selection>()

    override fun render() = toString()

    override fun toString(): String {
        return buildString {
            for (selection in selections) {
                append(selection)
            }
        }
    }
}