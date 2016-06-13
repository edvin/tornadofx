package tornadofx

fun main(args: Array<String>) {
    val test = Stylesheet2().apply {
        s(
                Rule.ClassRule("X")
                        .descendant(Rule.ClassRule("Y"))
                        .descendant(Rule.ClassRule("Z")),
                Rule.ClassRule("A")
                        .refine(Rule.ClassRule("B"))
                        .child(Rule.ClassRule("C"))
                        .descendant(Rule.ClassRule("D"))
                        .adjacent(Rule.ClassRule("E"))
                        .sibling(Rule.ClassRule("F"))
        ) {
            red = "yep"
            green = "nope"

            +s(
                    Rule.ClassRule("John").refine(Rule.PseudoClassRule("Doe")),
                    Rule.ClassRule("Jane").refine(Rule.PseudoClassRule("Doe"))
            ) {
                blue = "maybe"
            }
        }
    }
    println(test.render())
}

interface Rendered {
    fun render(): String
}

interface Scoped {
    fun append(rule: SubRule): RuleSet
    fun refine(rule: Rule) = append(SubRule(rule, SubRule.Relation.REFINE))
    fun child(rule: Rule) = append(SubRule(rule, SubRule.Relation.CHILD))
    fun descendant(rule: Rule) = append(SubRule(rule, SubRule.Relation.DESCENDANT))
    fun adjacent(rule: Rule) = append(SubRule(rule, SubRule.Relation.ADJACENT))
    fun sibling(rule: Rule) = append(SubRule(rule, SubRule.Relation.SIBLING))
}

open class Proper {
    val properties = mutableMapOf<String, String>()
    var red by properties
    var green by properties
    var blue by properties
}

class Selection2(vararg val rule: RuleSet) : Proper(), Rendered {
    companion object {
        fun String.merge(other: String, refine: Boolean) = if (refine) "$this$other" else "$this $other"

        fun List<String>.cartesian(parents: List<String>, refine: Boolean): List<String> {
            if (parents.size == 0) {
                return this;
            }
            return parents.asSequence().flatMap { parent -> asSequence().map { child -> parent.merge(child, refine) } }.toList()
        }
    }

    val selections = mutableMapOf<Selection2, Boolean>()  // If the boolean is true, this is a refine selection

    fun s(vararg rule: RuleSet, op: Selection2.() -> Unit): Selection2 {
        val selection = Selection2(*rule).apply(op)
        selections[selection] = false
        return selection
    }

    operator fun Selection2.unaryPlus() {
        this@Selection2.selections[this] = true
    }

    operator fun Selection2.unaryMinus() {
        this@Selection2.selections[this] = false
    }

    override fun render() = render(emptyList(), false)

    fun render(parents: List<String>, refine: Boolean): String = buildString {
        val ruleStrings = rule.map { it.render() }.cartesian(parents, refine)
        if (properties.size > 0) {
            append("${ruleStrings.joinToString()} {\n")
            for ((name, value) in properties) {
                append("    $name: $value;\n")
            }
            append("}\n\n")
        }
        for ((selection, refine) in selections) {
            append(selection.render(ruleStrings, refine))
        }
    }
}

class RuleSet(val rootRule: Rule, vararg val subRule: SubRule) : Scoped, Rendered {
    override fun render() = buildString {
        append(rootRule.render())
        subRule.forEach { append(it.render()) }
    }

    override fun append(rule: SubRule) = RuleSet(rootRule, *subRule, rule)
}

sealed class Rule(val value: String) : Scoped, Rendered {
    class ElementRule(value: String) : Rule(value) {
        override fun render() = value
    }

    class IdRule(value: String) : Rule(value) {
        override fun render() = "#$value"
    }

    class ClassRule(value: String) : Rule(value) {
        override fun render() = ".$value"
    }

    class PseudoClassRule(value: String) : Rule(value) {
        override fun render() = ":$value"
    }

    override fun append(rule: SubRule) = RuleSet(this, rule)
}

class SubRule(val rule: Rule, val relation: Relation) : Rendered {
    init {
        if (rule is Rule.ElementRule && relation == Relation.REFINE) {
            // ClassRule("test").refine(ElementRule("oops") => .testoops
            throw IllegalArgumentException("Refining with an element is not possible")
        }
    }

    override fun render() = "${relation.render()}${rule.render()}"

    enum class Relation(val symbol: String) : Rendered {
        REFINE(""),
        CHILD(" > "),
        DESCENDANT(" "),
        ADJACENT(" + "),
        SIBLING(" ~ ");

        override fun render() = symbol
    }

    operator fun component1() = rule
    operator fun component2() = relation
}

class Stylesheet2 : Rendered {
    val selections = mutableListOf<Selection2>()
    override fun render() = selections.joinToString(separator = "") { it.render() }

    fun s(vararg rule: RuleSet, op: Selection2.() -> Unit): Selection2 {
        val selection = Selection2(*rule).apply(op)
        selections += selection
        return selection
    }
}
