package tornadofx

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.geometry.*
import javafx.scene.Cursor
import javafx.scene.ImageCursor
import javafx.scene.Node
import javafx.scene.control.ContentDisplay
import javafx.scene.control.OverrunStyle
import javafx.scene.control.ScrollPane
import javafx.scene.effect.BlendMode
import javafx.scene.effect.DropShadow
import javafx.scene.effect.Effect
import javafx.scene.effect.InnerShadow
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.StrokeLineCap
import javafx.scene.shape.StrokeLineJoin
import javafx.scene.shape.StrokeType
import javafx.scene.text.*
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.logging.Logger
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

private val _log = lazy { Logger.getLogger("CSS") }
val log: Logger get() = _log.value

interface Rendered {
    fun render(): String
}

interface Scoped {
    fun append(rule: CssSubRule): CssRuleSet
    fun refine(rule: CssRule) = append(CssSubRule(rule, CssSubRule.Relation.REFINE))
    fun child(rule: CssRule) = append(CssSubRule(rule, CssSubRule.Relation.CHILD))
    fun descendant(rule: CssRule) = append(CssSubRule(rule, CssSubRule.Relation.DESCENDANT))
    fun adjacent(rule: CssRule) = append(CssSubRule(rule, CssSubRule.Relation.ADJACENT))
    fun sibling(rule: CssRule) = append(CssSubRule(rule, CssSubRule.Relation.SIBLING))
}

interface Selectable {
    fun toSelection(): CssSelection
    infix fun or(rule: CssRuleSet) = toSelection().addRule(rule)
    infix fun or(rule: CssRule) = toSelection().addRule(CssRuleSet(rule))
}

interface SelectionHolder {
    fun addSelection(selection: CssSelectionBlock)
    operator fun CssRule.invoke(op: CssSelectionBlock.() -> Unit) = CssRuleSet(this)(op)
    operator fun CssRuleSet.invoke(op: CssSelectionBlock.() -> Unit) = CssSelection(this)(op)
    operator fun CssSelection.invoke(op: CssSelectionBlock.() -> Unit): CssSelectionBlock {
        val selection = CssSelectionBlock(this).apply(op)
        addSelection(selection)
        return selection
    }
    fun s(selector: Selectable, op: CssSelectionBlock.() -> Unit) = selector.toSelection()(op)
    fun s(selection: String, op: CssSelectionBlock.() -> Unit) = selection(op)
    operator fun String.invoke(op: CssSelectionBlock.() -> Unit): CssSelectionBlock = TODO()
}

class Stylesheet2 : SelectionHolder, Rendered {
    companion object {
        // TODO: Elements?

        // Style classes used by JavaFX
        val accordion by cssclassrule()
        val arrow by cssclassrule()
        val arrowButton by cssclassrule()
        val axis by cssclassrule()
        val axisMinorTickMark by cssclassrule()
        val button by cssclassrule()
        val buttonBar by cssclassrule()
        val cell by cssclassrule()
        val chart by cssclassrule()
        val chartLegend by cssclassrule()
        val chartLegendItem by cssclassrule()
        val chartLegendSymbol by cssclassrule()
        val checkBox by cssclassrule()
        val choiceBox by cssclassrule()
        val colorPicker by cssclassrule()
        val columnHeader by cssclassrule()
        val comboBox by cssclassrule()
        val comboBoxBase by cssclassrule()
        val comboBoxPopup by cssclassrule()
        val content by cssclassrule()
        val contextMenu by cssclassrule()
        val datePicker by cssclassrule()
        val dialogPane by cssclassrule()
        val firstTitledPane by cssclassrule()
        val graphicContainer by cssclassrule()
        val headerPanel by cssclassrule()
        val htmlEditor by cssclassrule()
        val hyperlink by cssclassrule()
        val imageView by cssclassrule()
        val indexedCell by cssclassrule()
        val label by cssclassrule()
        val leftContainer by cssclassrule()
        val listCell by cssclassrule()
        val listView by cssclassrule()
        val mediaView by cssclassrule()
        val menu by cssclassrule()
        val menuBar by cssclassrule()
        val menuButton by cssclassrule()
        val menuItem by cssclassrule()
        val pagination by cssclassrule()
        val passwordField by cssclassrule()
        val progressBar by cssclassrule()
        val progressIndicator by cssclassrule()
        val radioButton by cssclassrule()
        val rightContainer by cssclassrule()
        val root by cssclassrule()
        val rootPopup by cssclassrule("root.popup")
        val scrollArrow by cssclassrule()
        val scrollBar by cssclassrule()
        val scrollPane by cssclassrule()
        val separator by cssclassrule()
        val slider by cssclassrule()
        val spinner by cssclassrule()
        val splitMenuButton by cssclassrule()
        val splitPane by cssclassrule()
        val tableView by cssclassrule()
        val tabPane by cssclassrule()
        val textArea by cssclassrule()
        val textField by cssclassrule()
        val textInput by cssclassrule()
        val toggleButton by cssclassrule()
        val toolBar by cssclassrule()
        val tooltip by cssclassrule()
        val treeCell by cssclassrule()
        val treeTableCell by cssclassrule()
        val treeTableView by cssclassrule()
        val treeView by cssclassrule()
        val webView by cssclassrule()

        // Style classes used by Form Builder
        val form by cssclassrule()
        val fieldset by cssclassrule()
        val legend by cssclassrule()
        val field by cssclassrule()
        val labelContainer by cssclassrule()
        val inputContainer by cssclassrule()

        // Pseudo classes used by JavaFX
        val armed by csspseudoclassrule()
        val disabled by csspseudoclassrule()
        val focused by csspseudoclassrule()
        val hover by csspseudoclassrule()
        val pressed by csspseudoclassrule()
        val showMnemonics by csspseudoclassrule()
    }

    val selections = mutableListOf<CssSelectionBlock>()

    override fun addSelection(selection: CssSelectionBlock) {
        selections += selection
    }

    override fun render() = selections.joinToString(separator = "") { it.render() }
}

open class Proper {
    val properties = linkedMapOf<String, Any>()

    // Font
    var font: Font by cssprop("-fx-font")
    var fontFamily: Array<String> by cssprop("-fx-font-family")
    var fontSize: LinearDimension by cssprop("-fx-font-size")
    var fontStyle: FontPosture by cssprop("-fx-font-style")
    var fontWeight: FontWeight by cssprop("-fx-font-weight")

    // Node
    var blendMode: BlendMode by cssprop("-fx-blend-mode")
    var cursor: Cursor by cssprop("-fx-cursor")
    var effect: Effect by cssprop("-fx-effect")
    var focusTraversable: Boolean by cssprop("-fx-focus-traversable")
    var opacity: Double by cssprop("-fx-opacity")
    var rotate: AngularDimension by cssprop("-fx-rotate")
    var scaleX: Number by cssprop("-fx-scale-x")
    var scaleY: Number by cssprop("-fx-scale-y")
    var scaleZ: Number by cssprop("-fx-scale-z")
    var translateX: LinearDimension by cssprop("-fx-translate-x")
    var translateY: LinearDimension by cssprop("-fx-translate-y")
    var translateZ: LinearDimension by cssprop("-fx-translate-z")
    var visibility: FXVisibility by cssprop("visibility")  // Intentionally not -fx-visibility

    // ImageView
    var image: String by cssprop("-fx-image")

    // DialogPane
    var graphic: String by cssprop("-fx-graphic")

    // FlowPane
    var hgap: LinearDimension by cssprop("-fx-hgap")
    var vgap: LinearDimension by cssprop("-fx-vgap")
    var alignment: Pos by cssprop("-fx-alignment")
    var columnHAlignment: HPos by cssprop("-fx-column-halignment")
    var rowVAlignment: VPos by cssprop("-fx-row-valignment")
    var orientation: Orientation by cssprop("-fx-orientation")

    // GridPane
    var gridLinesVisible: Boolean by cssprop("-fx-grid-lines-visible")

    // HBox
    var spacing: LinearDimension by cssprop("-fx-spacing")
    var fillHeight: Boolean by cssprop("-fx-fill-height")

    // Region
    var backgroundColor: MultiValue<Paint> by cssprop("-fx-background-color")
    var backgroundInsets: MultiValue<CssBox<LinearDimension>> by cssprop("-fx-background-insets")
    var backgroundRadius: MultiValue<CssBox<LinearDimension>> by cssprop("-fx-background-radius")
    var backgroundImage: MultiValue<String> by cssprop("-fx-background-image")
    var backgroundPosition: MultiValue<BackgroundPosition> by cssprop("-fx-background-position")
    var backgroundRepeat: MultiValue<Pair<BackgroundRepeat, BackgroundRepeat>> by cssprop("-fx-background-repeat")
    var backgroundSize: MultiValue<BackgroundSize> by cssprop("-fx-background-size")
    var borderColor: MultiValue<CssBox<Paint?>> by cssprop("-fx-border-color")
    var borderInsets: MultiValue<CssBox<LinearDimension>> by cssprop("-fx-border-radius")
    var borderRadius: MultiValue<CssBox<LinearDimension>> by cssprop("-fx-border-radius")
    var borderStyle: MultiValue<BorderStrokeStyle> by cssprop("-fx-border-style")
    var borderWidth: MultiValue<CssBox<LinearDimension>> by cssprop("-fx-border-width")
    var borderImageSource: MultiValue<String> by cssprop("-fx-border-image-source")
    var borderImageInsets: MultiValue<CssBox<LinearDimension>> by cssprop("-fx-border-image-insets")
    var borderImageRepeat: MultiValue<Pair<BorderRepeat, BorderRepeat>> by cssprop("-fx-border-image-repeat")
    var borderImageSlice: MultiValue<BorderImageSlice> by cssprop("-fx-border-image-slice")
    var borderImageWidth: CssBox<LinearDimension> by cssprop("-fx-border-image-width")
    var padding: CssBox<LinearDimension> by cssprop("-fx-padding")
    var positionShape: Boolean by cssprop("-fx-position-shape")
    var scaleShape: Boolean by cssprop("-fx-scale-shape")
    var shape: String by cssprop("-fx-shape")
    var snapToPixel: Boolean by cssprop("-fx-snap-to-pixel")
    var minHeight: LinearDimension by cssprop("-fx-min-height")
    var prefHeight: LinearDimension by cssprop("-fx-pref-height")
    var maxHeight: LinearDimension by cssprop("-fx-max-height")
    var minWidth: LinearDimension by cssprop("-fx-min-width")
    var prefWidth: LinearDimension by cssprop("-fx-pref-width")
    var maxWidth: LinearDimension by cssprop("-fx-max-width")

    // TilePane
    var prefRows: Int by cssprop("-fx-pref-rows")
    var prefColumns: Int by cssprop("-fx-pref-columns")
    var prefTileWidth: LinearDimension by cssprop("-fx-pref-tile-width")
    var prefTileHeight: LinearDimension by cssprop("-fx-pref-tile-height")
    var tileAlignment: Pos by cssprop("-fx-tile-alignment")

    // VBox
    var fillWidth: Boolean by cssprop("-fx-fill-width")

    // Shape
    var fill: Paint by cssprop("-fx-fill")
    var smooth: Boolean by cssprop("-fx-smooth")
    var stroke: Paint by cssprop("-fx-stroke")
    var strokeType: StrokeType by cssprop("-fx-stroke-type")
    var strokeDashArray: Array<LinearDimension> by cssprop("-fx-stroke-dash-array")
    var strokeDashOffset: LinearDimension by cssprop("-fx-stroke-dash-offset")
    var strokeLineCap: StrokeLineCap by cssprop("-fx-stroke-line-cap")
    var strokeLineJoin: StrokeLineJoin by cssprop("-fx-stroke-line-join")
    var strokeMiterLimit: Double by cssprop("-fx-stroke-miter-limit")
    var strokeWidth: LinearDimension by cssprop("-fx-stroke-width")

    // Rectangle
    var arcHeight: LinearDimension by cssprop("-fx-arc-height")
    var arcWidth: LinearDimension by cssprop("-fx-arc-width")

    // Text
    var fontSmoothingType: FontSmoothingType by cssprop("-fx-font-smoothing-type")
    var strikethrough: Boolean by cssprop("-fx-strikethrough")
    var textAlignment: TextAlignment by cssprop("-fx-text-alignment")
    var textOrigin: VPos by cssprop("-fx-text-origin")
    var underline: Boolean by cssprop("-fx-underline")

    // WebView
    var contextMenuEnabled: Boolean by cssprop("-fx-context-menu-enabled")
    var fontScale: Number by cssprop("-fx-font-scale")

    // Cell
    var cellSize: LinearDimension by cssprop("-fx-cell-size")

    // ColorPicker
    var colorLabelVisible: Boolean by cssprop("-fx-color-label-visible")

    // Control
    var skin: KClass<*> by cssprop("-fx-skin")

    // DatePicker
    var showWeekNumbers: Boolean by cssprop("-fx-show-week-numbers")

    // Labeled
    var textOverrun: OverrunStyle by cssprop("-fx-text-overrun")
    var wrapText: Boolean by cssprop("-fx-wrap-text")
    var contentDisplay: ContentDisplay by cssprop("-fx-content-display")
    var graphicTextGap: LinearDimension by cssprop("-fx-graphic-text-gap")
    var labelPadding: CssBox<LinearDimension> by cssprop("-fx-label-padding")
    var textFill: Paint by cssprop("-fx-text-fill")
    var ellipsisString: String by cssprop("-fx-ellipsis-string")

    // MenuBar
    var useSystemMenuBar: Boolean by cssprop("-fx-use-system-menu-bar")

    // Pagination
    var maxPageIndicatorCount: Int by cssprop("-fx-max-page-indicator-count")
    var arrowsVisible: Boolean by cssprop("-fx-arrows-visible")
    var tooltipVisible: Boolean by cssprop("-fx-tooltip-visible")
    var pageInformationVisible: Boolean by cssprop("-fx-page-information-visible")
    var pageInformationAlignment: Side by cssprop("-fx-page-information-alignment")

    // ProgressBar
    var indeterminateBarLength: LinearDimension by cssprop("-fx-indeterminate-bar-length")
    var indeterminateBarEscape: Boolean by cssprop("-fx-indeterminate-bar-escape")
    var indeterminateBarFlip: Boolean by cssprop("-fx-indeterminate-bar-flip")
    var indeterminateBarAnimationTime: Number by cssprop("-fx-indeterminate-bar-animation-time")

    // ProgressIndicator
    var indeterminateSegmentCount: Int by cssprop("-fx-indeterminate-SegmentCount")
    var progressColor: Paint by cssprop("-fx-progress-color")
    var spinEnabled: Boolean by cssprop("-fx-spin-enabled")

    // ScrollBar
    var blockIncrement: LinearDimension by cssprop("-fx-block-increment")
    var unitIncrement: LinearDimension by cssprop("-fx-unit-increment")

    // ScrollPane
    var fitToWidth: Boolean by cssprop("-fx-fit-to-width")
    var fitToHeight: Boolean by cssprop("-fx-fit-to-height")
    var pannable: Boolean by cssprop("-fx-pannable")
    var hBarPolicy: ScrollPane.ScrollBarPolicy by cssprop("-fx-hbar-policy")
    var vBarPolicy: ScrollPane.ScrollBarPolicy by cssprop("-fx-vbar-policy")

    // Separator
    var hAlignment: HPos by cssprop("-fx-halignment")
    var vAlignment: VPos by cssprop("-fx-valignment")

    // Slider
    var showTickLabels: Boolean by cssprop("-fx-show-tick-labels")
    var showTickMarks: Boolean by cssprop("-fx-show-tick-marks")
    var majorTickUnit: Double by cssprop("-fx-major-tick-unit")
    var minorTickCount: Int by cssprop("-fx-minor-tick-count")
    var snapToTicks: Boolean by cssprop("-fx-snap-to-ticks")

    // TabPane
    var tabMaxHeight: LinearDimension by cssprop("-fx-tab-max-height")
    var tabMinHeight: LinearDimension by cssprop("-fx-tab-min-height")
    var tabMaxWidth: LinearDimension by cssprop("-fx-tab-max-width")
    var tabMinWidth: LinearDimension by cssprop("-fx-tab-min-width")
    var openTabAnimation: FXTabAnimation by cssprop("-fx-open-tab-animation")
    var closeTabAnimation: FXTabAnimation by cssprop("-fx-close-tab-animation")

    // TableColumnHeader
    var size: LinearDimension by cssprop("-fx-size")

    // TableView
    var fixedCellSize: LinearDimension by cssprop("-fx-fixed-cell-size")

    // TextArea
    var prefColumnCount: Int by cssprop("-fx-pref-column-count")
    var prefRowCount: Int by cssprop("-fx-pref-row-count")

    // TextInputControl
    var promptTextFill: Paint by cssprop("-fx-prompt-text-fill")
    var highlightFill: Paint by cssprop("-fx-highlight-fill")
    var highlightTextFill: Paint by cssprop("-fx-highlight-text-fill")
    var displayCaret: Boolean by cssprop("-fx-display-caret")

    // TitlePane
    var animated: Boolean by cssprop("-fx-animated")
    var collapsible: Boolean by cssprop("-fx-collapsible")

    // TreeCell
    var indent: LinearDimension by cssprop("-fx-indent")

    // Axis
    var side: Side by cssprop("-fx-side")
    var tickLength: LinearDimension by cssprop("-fx-tick-length")
    var tickLabelFont: Font by cssprop("-fx-tick-label-font")
    var tickLabelFill: Paint by cssprop("-fx-tick-label-fill")
    var tickLabelGap: LinearDimension by cssprop("-fx-tick-label-gap")
    var tickMarkVisible: Boolean by cssprop("-fx-tick-mark-visible")
    var tickLabelsVisible: Boolean by cssprop("-fx-tick-labels-visible")

    // BarChar
    var barGap: LinearDimension by cssprop("-fx-bar-gap")
    var categoryGap: LinearDimension by cssprop("-fx-category-group")

    // CategoryAxis
    var startMargin: LinearDimension by cssprop("-fx-start-margin")
    var endMargin: LinearDimension by cssprop("-fx-end-margin")
    var gapStartAndEnd: Boolean by cssprop("-fx-gap-start-and-end")

    // Chart
    var legendSide: Side by cssprop("-fx-legend-side")
    var legendVisible: Boolean by cssprop("-fx-legend-visible")
    var titleSide: Side by cssprop("-fx-title-side")

    // LineChart
    var createSymbols: Boolean by cssprop("-fx-create-symbols")

    // NumberAxis
    var tickUnit: Number by cssprop("-fx-tick-unit")

    // PieChart
    var clockwise: Boolean by cssprop("-fx-clockwise")
    var pieLabelVisible: Boolean by cssprop("-fx-pie-label-visible")
    var labelLineLength: LinearDimension by cssprop("-fx-label-line-length")
    var startAngle: AngularDimension by cssprop("-fx-start-angle")

    // ValueAxis
    var minorTickLength: LinearDimension by cssprop("-fx-minor-tick-length")
    var minorTickVisible: Boolean by cssprop("-fx-minor-tick-visible")

    // XYChart
    var alternativeColumnFillVisible: Boolean by cssprop("-fx-alternative-column-fill-visible")
    var alternativeRowFillVisible: Boolean by cssprop("-fx-alternative-row-fill-visible")
    var horizontalGridLinesVisible: Boolean by cssprop("-fx-horizontal-grid-lines-visible")
    var horizontalZeroLineVisible: Boolean by cssprop("-fx-horizontal-zero-line-visible")
    var verticalGridLinesVisible: Boolean by cssprop("-fx-vertical-grid-lines-visible")
    var verticalZeroLineVisible: Boolean by cssprop("-fx-vertical-zero-line-visible")

    private inline fun <reified V : Any> cssprop(key: String): ReadWriteProperty<Proper, V> {
        return object : ReadWriteProperty<Proper, V> {
            override fun getValue(thisRef: Proper, property: KProperty<*>): V {
                if (!properties.containsKey(key) && MultiValue::class.java.isAssignableFrom(V::class.java))
                    properties[key] = MultiValue<V>()
                return properties[key] as V
            }

            override fun setValue(thisRef: Proper, property: KProperty<*>, value: V) {
                properties[key] = value as Any
            }
        }
    }

    fun <T> toCss(value: T): String {
        when (value) {
            null -> return ""  // This should only happen in a container (such as box(), Array<>(), Pair())
            is MultiValue<*> -> return value.elements.joinToString { toCss(it) }
            is FontWeight -> return "${value.weight}"  // Needs to come before `is Enum<*>`
            is Enum<*> -> return value.toString().toLowerCase().replace("_", "-")
            is Font -> return "${if (value.style == "Regular") "normal" else value.style} ${value.size}pt ${toCss(value.family)}"
            is Cursor -> return if (value is ImageCursor) {
                value.image.javaClass.getDeclaredField("url").let {
                    it.isAccessible = true
                    it.get(value.image).toString()
                }
            } else {
                value.toString()
            }
            is BackgroundPosition -> return "${value.horizontalSide} ${value.horizontalPosition.pos(value.isHorizontalAsPercentage)} " +
                    "${value.verticalSide} ${value.verticalPosition.pos(value.isVerticalAsPercentage)}"
            is BackgroundSize -> return if (value.isContain) "contain" else if (value.isCover) "cover" else buildString {
                append(if (value.width == BackgroundSize.AUTO) "auto" else value.width.pos(value.isWidthAsPercentage))
                append(" ")
                append(if (value.height == BackgroundSize.AUTO) "auto" else value.height.pos(value.isHeightAsPercentage))
            }
            is BorderStrokeStyle -> return when (value) {
                BorderStrokeStyle.NONE -> "none"
                BorderStrokeStyle.DASHED -> "dashed"
                BorderStrokeStyle.DOTTED -> "dotted"
                BorderStrokeStyle.SOLID -> "solid"
                else -> buildString {
                    // FIXME: This may not actually render what the user expects, but I can't find documentation to fix it
                    append("segments(${value.dashArray.joinToString(separator = " ")}) ")
                    append(toCss(value.type))
                    append(" line-join ${toCss(value.lineJoin)} ")
                    if (value.lineJoin == StrokeLineJoin.MITER) {
                        append(value.miterLimit)
                    }
                    append(" line-cap ${toCss(value.lineCap)}")
                }
            }
            is BorderImageSlice -> return "${toCss(value.widths)}" + if (value.filled) " fill" else ""
            is Array<*> -> return value.joinToString { toCss(it) }
            is Pair<*, *> -> return "${toCss(value.first)} ${toCss(value.second)}"
            is KClass<*> -> return value.simpleName ?: "none"
            is String -> return "\"$value\""
            is Effect -> {
                // JavaFX currently only supports DropShadow and InnerShadow in CSS
                when (value) {
                    is DropShadow -> return "dropshadow(${toCss(value.blurType)}, ${value.color.css}, " +
                            "${value.radius}, ${value.spread}, ${value.offsetX}, ${value.offsetY})"
                    is InnerShadow -> return "dropshadow(${toCss(value.blurType)}, ${value.color.css}, " +
                            "${value.radius}, ${value.choke}, ${value.offsetX}, ${value.offsetY})"
                    else -> return "none"
                }
            }
            is Color -> return value.css
            is Paint -> return value.toString().replace(Regex("0x[0-9a-f]{8}")) { Color.web(it.groupValues[0]).css }
        }
        return value.toString()
    }
}

class CssSelectionBlock(val selection: CssSelection) : Proper(), SelectionHolder, Rendered {
    private val selections = mutableMapOf<CssSelectionBlock, Boolean>()  // If the boolean is true, this is a refine selection

    override fun addSelection(selection: CssSelectionBlock) {
        selections[selection] = false
    }

    operator fun CssSelectionBlock.unaryPlus() {
        this@CssSelectionBlock.selections[this] = true
    }

    operator fun CssSelectionBlock.unaryMinus() {
        this@CssSelectionBlock.selections[this] = false
    }

    override fun render() = render(emptyList(), false)

    fun render(parents: List<String>, refine: Boolean): String = buildString {
        val ruleStrings = selection.strings(parents, refine)
        if (properties.size > 0) {
            append("${ruleStrings.joinToString()} {\n")
            for ((name, value) in properties) {
                append("    $name: ${toCss(value)};\n")
            }
            append("}\n\n")
        }
        for ((selection, refine) in selections) {
            append(selection.render(ruleStrings, refine))
        }
    }
}

class CssSelection(vararg val rule: CssRuleSet) : Selectable {
    companion object {
        fun String.merge(other: String, refine: Boolean) = if (refine) "$this$other" else "$this $other"

        fun List<String>.cartesian(parents: List<String>, refine: Boolean): List<String> {
            if (parents.size == 0) {
                return this;
            }
            return parents.asSequence().flatMap { parent -> asSequence().map { child -> parent.merge(child, refine) } }.toList()
        }
    }

    override fun toSelection() = this

    fun strings(parents: List<String>, refine: Boolean) = rule.map { it.render() }.cartesian(parents, refine)

    fun addRule(cssRuleSet: CssRuleSet) = CssSelection(*rule, cssRuleSet)
}

class CssRuleSet(val rootRule: CssRule, vararg val subRule: CssSubRule) : Selectable, Scoped, Rendered {
    override fun render() = buildString {
        append(rootRule.render())
        subRule.forEach { append(it.render()) }
    }

    override fun toSelection() = CssSelection(this)
    override fun append(rule: CssSubRule) = CssRuleSet(rootRule, *subRule, rule)
}

sealed class CssRule(val value: String) : Selectable, Scoped, Rendered {
    class ElementRule(value: String) : CssRule(value) {
        override fun render() = value
    }

    class IdRule(value: String) : CssRule(value) {
        override fun render() = "#$value"
    }

    class ClassRule(value: String) : CssRule(value) {
        override fun render() = ".$value"
    }

    class PseudoClassRule(value: String) : CssRule(value) {
        override fun render() = ":$value"
    }

    override fun toSelection() = CssSelection(CssRuleSet(this))
    override fun append(rule: CssSubRule) = CssRuleSet(this, rule)
}

class CssSubRule(val rule: CssRule, val relation: Relation) : Rendered {
    init {
        if (rule is CssRule.ElementRule && relation == Relation.REFINE) {
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

// Inline CSS

class InlineCss : Proper(), Rendered {
    override fun render() = properties.entries.joinToString(separator = " ") { "${it.key}: ${toCss(it.value)};" }
}

fun Node.style2(append: Boolean = false, op: InlineCss.() -> Unit) {
    val block = InlineCss().apply(op)

    if (append && style.isNotBlank())
        style += block.render()
    else
        style = block.render().trim()
}

// Delegates

fun csselementrule(value: String? = null) = CssElementDelegate(value)
fun cssidrule(value: String? = null) = CssIdDelegate(value)
fun cssclassrule(value: String? = null) = CssClassDelegate(value)
fun csspseudoclassrule(value: String? = null) = CssPseudoClassDelegate(value)

class CssElementDelegate(val name: String?) : ReadOnlyProperty<Any, CssRule.ElementRule> {
    override fun getValue(thisRef: Any, property: KProperty<*>) = CssRule.ElementRule(name ?: property.name)
}

class CssIdDelegate(val name: String?) : ReadOnlyProperty<Any, CssRule.IdRule> {
    override fun getValue(thisRef: Any, property: KProperty<*>) = CssRule.IdRule(name ?: property.name)
}

class CssClassDelegate(val name: String?) : ReadOnlyProperty<Any, CssRule.ClassRule> {
    override fun getValue(thisRef: Any, property: KProperty<*>) = CssRule.ClassRule(name ?: property.name)
}

class CssPseudoClassDelegate(val name: String?) : ReadOnlyProperty<Any, CssRule.PseudoClassRule> {
    override fun getValue(thisRef: Any, property: KProperty<*>) = CssRule.PseudoClassRule(name ?: property.name)
}

// Dimensions

internal fun dimStr(value: Double, units: String) = when (value) {
    Double.POSITIVE_INFINITY, Double.MAX_VALUE -> "infinity"
    Double.NEGATIVE_INFINITY, Double.MIN_VALUE -> "-infinity"
    Double.NaN -> "0$units"
    else -> "${fiveDigits.format(value)}$units"
}

class LinearDimension(val value: Double, val units: Units) {
    override fun toString() = dimStr(value, units.toString())

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

val infinity = LinearDimension(Double.POSITIVE_INFINITY, LinearDimension.Units.px)

val Number.px: LinearDimension get() = LinearDimension(this.toDouble(), LinearDimension.Units.px)
val Number.mm: LinearDimension get() = LinearDimension(this.toDouble(), LinearDimension.Units.mm)
val Number.cm: LinearDimension get() = LinearDimension(this.toDouble(), LinearDimension.Units.cm)
val Number.inches: LinearDimension get() = LinearDimension(this.toDouble(), LinearDimension.Units.inches)
val Number.pt: LinearDimension get() = LinearDimension(this.toDouble(), LinearDimension.Units.pt)
val Number.pc: LinearDimension get() = LinearDimension(this.toDouble(), LinearDimension.Units.pc)
val Number.em: LinearDimension get() = LinearDimension(this.toDouble(), LinearDimension.Units.em)
val Number.ex: LinearDimension get() = LinearDimension(this.toDouble(), LinearDimension.Units.ex)
val Number.percent: LinearDimension get() = LinearDimension(this.toDouble(), LinearDimension.Units.percent)

class AngularDimension(val value: Double, val units: Units) {
    override fun toString() = dimStr(value, units.toString())
    enum class Units { deg, rad, grad, turn; }
}

val Number.deg: AngularDimension get() = AngularDimension(this.toDouble(), AngularDimension.Units.deg)
val Number.rad: AngularDimension get() = AngularDimension(this.toDouble(), AngularDimension.Units.rad)
val Number.grad: AngularDimension get() = AngularDimension(this.toDouble(), AngularDimension.Units.grad)
val Number.turn: AngularDimension get() = AngularDimension(this.toDouble(), AngularDimension.Units.turn)

// Enums

enum class FXVisibility { VISIBLE, HIDDEN, COLLAPSE, INHERIT; }
enum class FXTabAnimation { GROW, NONE; }

// Misc

val fiveDigits = DecimalFormat("#.#####", DecimalFormatSymbols.getInstance(Locale.ENGLISH))

val Color.css: String
    get() = "rgba(${(red * 255).toInt()}, ${(green * 255).toInt()}, ${(blue * 255).toInt()}, ${fiveDigits.format(opacity)})"

fun <T : Node> T.setId(cssId: CSSId): T {
    id = cssId.name
    return this
}

// Style Class

fun Node.hasClass(cssClass: CSSClass) = hasClass(cssClass.name)
fun <T : Node> T.addClass(cssClass: CSSClass) = addClass(cssClass.name)
fun <T : Node> T.removeClass(cssClass: CSSClass) = removeClass(cssClass.name)
fun <T : Node> T.toggleClass(cssClass: CSSClass, predicate: Boolean) = toggleClass(cssClass.name, predicate)
@Suppress("UNCHECKED_CAST")
fun <T : Node> Node.select(selector: CSSSelector) = lookup(selector.toString()) as T

@Suppress("UNCHECKED_CAST")
fun <T : Node> Node.selectAll(selector: CSSSelector) = (lookupAll(selector.toString()) as Set<T>).toList()

fun Iterable<Node>.addClass(cssClass: CSSClass) = forEach { it.addClass(cssClass) }
fun Iterable<Node>.removeClass(cssClass: CSSClass) = forEach { it.removeClass(cssClass) }
fun Iterable<Node>.toggleClass(cssClass: CSSClass, predicate: Boolean) = forEach { it.toggleClass(cssClass, predicate) }

class ObservableStyleClass(node: Node, val value: ObservableValue<CSSClass>) {
    val listener: ChangeListener<CSSClass>

    init {
        fun checkAdd(newValue: CSSClass?) {
            if (newValue != null && !node.hasClass(newValue)) node.addClass(newValue)
        }
        listener = ChangeListener { observableValue, oldValue, newValue ->
            if (oldValue != null) node.removeClass(oldValue)
            checkAdd(newValue)
        }
        checkAdd(value.value)
        value.addListener(listener)
    }

    fun dispose() = value.removeListener(listener)
}

fun Node.bindClass(value: ObservableValue<CSSClass>): ObservableStyleClass = ObservableStyleClass(this, value)

// Containers

fun <T> multi(vararg elements: T) = MultiValue(elements)

open class CssBox<T>(val top: T, val right: T, val bottom: T, val left: T) {
    override fun toString() = "${toCss(top)} ${toCss(right)} ${toCss(bottom)} ${toCss(left)}"
}

fun <T> box(all: T) = CssBox(all, all, all, all)
fun <T> box(vertical: T, horizontal: T) = CssBox(vertical, horizontal, vertical, horizontal)
fun <T> box(top: T, right: T, bottom: T, left: T) = CssBox(top, right, bottom, left)

fun c(colorString: String, opacity: Double = 1.0) = try {
    Color.web(colorString, opacity)
} catch (e: Exception) {
    log.warning("Error parsing color c('$colorString', opacity=$opacity)")
    Color.MAGENTA
}

fun c(red: Double, green: Double, blue: Double, opacity: Double = 1.0) = try {
    Color.color(red, green, blue, opacity)
} catch (e: Exception) {
    log.warning("Error parsing color c(red=$red, green=$green, blue=$blue, opacity=$opacity)")
    Color.MAGENTA
}

fun c(red: Int, green: Int, blue: Int, opacity: Double = 1.0) = try {
    Color.rgb(red, green, blue, opacity)
} catch (e: Exception) {
    log.warning("Error parsing color c(red=$red, green=$green, blue=$blue, opacity=$opacity)")
    Color.MAGENTA
}
