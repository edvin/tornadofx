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
import java.net.URL
import java.nio.charset.StandardCharsets
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
    fun and(rule: CssRule) = append(CssSubRule(rule, CssSubRule.Relation.REFINE))
    fun child(rule: CssRule) = append(CssSubRule(rule, CssSubRule.Relation.CHILD))
    fun contains(rule: CssRule) = append(CssSubRule(rule, CssSubRule.Relation.DESCENDANT))
    fun next(rule: CssRule) = append(CssSubRule(rule, CssSubRule.Relation.ADJACENT))
    fun sibling(rule: CssRule) = append(CssSubRule(rule, CssSubRule.Relation.SIBLING))
}

interface Selectable {
    fun toSelection(): CssSelector
    infix fun or(rule: CssRuleSet) = toSelection().addRule(rule)
    infix fun or(rule: CssRule) = toSelection().addRule(CssRuleSet(rule))
}

interface SelectionHolder {
    fun addSelection(selection: CssSelection)
    fun removeSelection(selection: CssSelection)
    operator fun Selectable.invoke(op: CssSelectionBlock.() -> Unit): CssSelection {
        val selection = CssSelection(toSelection(), op)
        addSelection(selection)
        return selection
    }

    fun s(selector: Selectable, op: CssSelectionBlock.() -> Unit) = selector.toSelection()(op)
    fun s(selection: String, op: CssSelectionBlock.() -> Unit) = selection(op)
    operator fun String.invoke(op: CssSelectionBlock.() -> Unit): CssSelectionBlock = TODO()

    infix fun Selectable.or(selection: CssSelection): CssSelection {
        removeSelection(selection)
        val newSelection = CssSelection(CssSelector(*toSelection().rule, *selection.selector.rule)) { mix(selection.block) }
        addSelection(newSelection)
        return newSelection
    }

    fun Scoped.and(rule: CssRule, op: CssSelectionBlock.() -> Unit) = and(rule)(op)
    fun Scoped.child(rule: CssRule, op: CssSelectionBlock.() -> Unit) = child(rule)(op)
    fun Scoped.contains(rule: CssRule, op: CssSelectionBlock.() -> Unit) = contains(rule)(op)
    fun Scoped.next(rule: CssRule, op: CssSelectionBlock.() -> Unit) = next(rule)(op)
    fun Scoped.sibling(rule: CssRule, op: CssSelectionBlock.() -> Unit) = sibling(rule)(op)
}

open class Stylesheet : SelectionHolder, Rendered {
    companion object {
        // TODO: Elements?

        // Style classes used by JavaFX
        val accordion by cssclass()
        val arrow by cssclass()
        val arrowButton by cssclass()
        val axis by cssclass()
        val axisMinorTickMark by cssclass()
        val button by cssclass()
        val buttonBar by cssclass()
        val cell by cssclass()
        val chart by cssclass()
        val chartLegend by cssclass()
        val chartLegendItem by cssclass()
        val chartLegendSymbol by cssclass()
        val checkBox by cssclass()
        val choiceBox by cssclass()
        val colorPicker by cssclass()
        val columnHeader by cssclass()
        val comboBox by cssclass()
        val comboBoxBase by cssclass()
        val comboBoxPopup by cssclass()
        val content by cssclass()
        val contextMenu by cssclass()
        val datePicker by cssclass()
        val dialogPane by cssclass()
        val firstTitledPane by cssclass()
        val graphicContainer by cssclass()
        val headerPanel by cssclass()
        val htmlEditor by cssclass()
        val hyperlink by cssclass()
        val imageView by cssclass()
        val indexedCell by cssclass()
        val label by cssclass()
        val leftContainer by cssclass()
        val listCell by cssclass()
        val listView by cssclass()
        val mediaView by cssclass()
        val menu by cssclass()
        val menuBar by cssclass()
        val menuButton by cssclass()
        val menuItem by cssclass()
        val pagination by cssclass()
        val passwordField by cssclass()
        val progressBar by cssclass()
        val progressIndicator by cssclass()
        val radioButton by cssclass()
        val rightContainer by cssclass()
        val root by cssclass()
        val rootPopup by cssclass("root.popup")
        val scrollArrow by cssclass()
        val scrollBar by cssclass()
        val scrollPane by cssclass()
        val separator by cssclass()
        val slider by cssclass()
        val spinner by cssclass()
        val splitMenuButton by cssclass()
        val splitPane by cssclass()
        val tableView by cssclass()
        val tabPane by cssclass()
        val textArea by cssclass()
        val textField by cssclass()
        val textInput by cssclass()
        val toggleButton by cssclass()
        val toolBar by cssclass()
        val tooltip by cssclass()
        val treeCell by cssclass()
        val treeTableCell by cssclass()
        val treeTableView by cssclass()
        val treeView by cssclass()
        val webView by cssclass()

        // Style classes used by Form Builder
        val form by cssclass()
        val fieldset by cssclass()
        val legend by cssclass()
        val field by cssclass()
        val labelContainer by cssclass()
        val inputContainer by cssclass()

        // Pseudo classes used by JavaFX
        val armed by csspseudoclass()
        val disabled by csspseudoclass()
        val focused by csspseudoclass()
        val hover by csspseudoclass()
        val pressed by csspseudoclass()
        val showMnemonics by csspseudoclass()
    }

    val selections = mutableListOf<CssSelection>()

    override fun addSelection(selection: CssSelection) {
        selections += selection
    }

    override fun removeSelection(selection: CssSelection) {
        selections -= selection
    }

    override fun render() = selections.joinToString(separator = "") { it.render() }

    val base64URL: URL get() {
        val content = Base64.getEncoder().encodeToString(render().toByteArray(StandardCharsets.UTF_8))
        return URL("css://$content:64")
    }
}

open class PropertyHolder {
    companion object {
        fun Double.pos(relative: Boolean) = if (relative) "${fiveDigits.format(this * 100)}%" else "${fiveDigits.format(this)}px"
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

    private inline fun <reified V : Any> cssprop(key: String): ReadWriteProperty<PropertyHolder, V> {
        return object : ReadWriteProperty<PropertyHolder, V> {
            override fun getValue(thisRef: PropertyHolder, property: KProperty<*>): V {
                if (!properties.containsKey(key) && MultiValue::class.java.isAssignableFrom(V::class.java))
                    properties[key] = MultiValue<V>()
                return properties[key] as V
            }

            override fun setValue(thisRef: PropertyHolder, property: KProperty<*>, value: V) {
                properties[key] = value as Any
            }
        }
    }
}

class CssSelection(val selector: CssSelector, op: CssSelectionBlock.() -> Unit) : Rendered {
    val block = CssSelectionBlock().apply(op)

    override fun render() = render(emptyList(), false)

    fun render(parents: List<String>, refine: Boolean): String = buildString {
        val ruleStrings = selector.strings(parents, refine)
        if (block.properties.size > 0) {
            append("${ruleStrings.joinToString()} {\n")
            for ((name, value) in block.properties) {
                append("    $name: ${PropertyHolder.toCss(value)};\n")
            }
            append("}\n\n")
        }
        for ((selection, refine) in block.selections) {
            append(selection.render(ruleStrings, refine))
        }
    }
}

class CssSelector(vararg val rule: CssRuleSet) : Selectable {
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

    fun addRule(cssRuleSet: CssRuleSet) = CssSelector(*rule, cssRuleSet)
}

class CssSelectionBlock() : PropertyHolder(), SelectionHolder {
    val selections = mutableMapOf<CssSelection, Boolean>()  // If the boolean is true, this is a refine selection

    override fun addSelection(selection: CssSelection) {
        selections[selection] = false
    }

    override fun removeSelection(selection: CssSelection) {
        selections.remove(selection)
    }

    operator fun CssSelection.unaryPlus(): CssSelection {
        this@CssSelectionBlock.selections[this] = true
        return this
    }

    operator fun CssSelection.unaryMinus(): CssSelection {
        this@CssSelectionBlock.selections[this] = false
        return this
    }

    operator fun CssSelectionBlock.unaryPlus() {
        this@CssSelectionBlock.mix(this)
    }

    fun mix(mixin: CssSelectionBlock) {
        properties.putAll(mixin.properties)
        selections.putAll(mixin.selections)
    }
}

fun mixin(op: CssSelectionBlock.() -> Unit) = CssSelectionBlock().apply(op)

class CssRuleSet(val rootRule: CssRule, vararg val subRule: CssSubRule) : Selectable, Scoped, Rendered {
    override fun render() = buildString {
        append(rootRule.render())
        subRule.forEach { append(it.render()) }
    }

    override fun toSelection() = CssSelector(this)
    override fun append(rule: CssSubRule) = CssRuleSet(rootRule, *subRule, rule)
}

class CssRule(val prefix: String, val name: String) : Selectable, Scoped, Rendered {
    companion object {
        fun elem(value: String) = CssRule("", value)
        fun id(value: String) = CssRule("#", value)
        fun c(value: String) = CssRule(".", value)
        fun pc(value: String) = CssRule(":", value)
    }

    override fun render() = "$prefix$name"
    override fun toSelection() = CssRuleSet(this).toSelection()
    override fun append(rule: CssSubRule) = CssRuleSet(this, rule)
}

class CssSubRule(val rule: CssRule, val relation: Relation) : Rendered {
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

class InlineCss : PropertyHolder(), Rendered {
    override fun render() = properties.entries.joinToString("") { " ${it.key}: ${toCss(it.value)};" }
}

fun Node.style2(append: Boolean = false, op: InlineCss.() -> Unit) {
    val block = InlineCss().apply(op)

    if (append && style.isNotBlank())
        style += block.render()
    else
        style = block.render().trim()
}

// Delegates

fun csselement(value: String? = null) = CssElementDelegate(value)
fun cssid(value: String? = null) = CssIdDelegate(value)
fun cssclass(value: String? = null) = CssClassDelegate(value)
fun csspseudoclass(value: String? = null) = CssPseudoClassDelegate(value)

class CssElementDelegate(val name: String?) : ReadOnlyProperty<Any, CssRule> {
    override fun getValue(thisRef: Any, property: KProperty<*>) = CssRule.elem(name ?: property.name)
}

class CssIdDelegate(val name: String?) : ReadOnlyProperty<Any, CssRule> {
    override fun getValue(thisRef: Any, property: KProperty<*>) = CssRule.id(name ?: property.name)
}

class CssClassDelegate(val name: String?) : ReadOnlyProperty<Any, CssRule> {
    override fun getValue(thisRef: Any, property: KProperty<*>) = CssRule.c(name ?: property.name)
}

class CssPseudoClassDelegate(val name: String?) : ReadOnlyProperty<Any, CssRule> {
    override fun getValue(thisRef: Any, property: KProperty<*>) = CssRule.pc(name ?: property.name)
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

fun <T : Node> T.setId(cssId: CssRule): T {
    id = cssId.name
    return this
}

// Style Class

fun Node.hasClass(cssClass: CssRule) = hasClass(cssClass.name)
fun <T : Node> T.addClass(cssClass: CssRule) = addClass(cssClass.name)
fun <T : Node> T.removeClass(cssClass: CssRule) = removeClass(cssClass.name)
fun <T : Node> T.toggleClass(cssClass: CssRule, predicate: Boolean) = toggleClass(cssClass.name, predicate)
@Suppress("UNCHECKED_CAST")
fun <T : Node> Node.select(selector: CssSelector) = lookup(selector.toString()) as T

@Suppress("UNCHECKED_CAST")
fun <T : Node> Node.selectAll(selector: CssSelector) = (lookupAll(selector.toString()) as Set<T>).toList()

fun Iterable<Node>.addClass(cssClass: CssRule) = forEach { it.addClass(cssClass) }
fun Iterable<Node>.removeClass(cssClass: CssRule) = forEach { it.removeClass(cssClass) }
fun Iterable<Node>.toggleClass(cssClass: CssRule, predicate: Boolean) = forEach { it.toggleClass(cssClass, predicate) }

class ObservableStyleClass(node: Node, val value: ObservableValue<CssRule>) {
    val listener: ChangeListener<CssRule>

    init {
        fun checkAdd(newValue: CssRule?) {
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

fun Node.bindClass(value: ObservableValue<CssRule>): ObservableStyleClass = ObservableStyleClass(this, value)

// Containers

fun <T> multi(vararg elements: T) = MultiValue(elements)

open class CssBox<T>(val top: T, val right: T, val bottom: T, val left: T) {
    override fun toString() = "${PropertyHolder.toCss(top)} ${PropertyHolder.toCss(right)} ${PropertyHolder.toCss(bottom)} ${PropertyHolder.toCss(left)}"
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

class BorderImageSlice(val widths: CssBox<LinearDimension>, val filled: Boolean = false)

class MultiValue<T>(initialElements: Array<out T>? = null) {
    val elements = mutableListOf<T>()

    init {
        if (initialElements != null) elements.addAll(initialElements)
    }

    operator fun plusAssign(element: T) {
        elements.add(element)
    }

    fun add(element: T) = elements.add(element)
    fun addAll(list: Iterable<T>) = elements.addAll(list)
    fun addAll(vararg element: T) = elements.addAll(element)
}
