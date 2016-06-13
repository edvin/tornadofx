package tornadofx

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
import java.nio.charset.StandardCharsets.UTF_8
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

open class CssBlock {
    val selections = mutableListOf<Selection>()

    operator fun Selection.unaryPlus() {
        modifier = true
    }

    fun select(selector: String, block: Selection.() -> Unit): Selection {
        val selection = Selection(selector)
        selection.block()
        selections += selection
        return selection
    }

    fun select(vararg selector: CSSSelector, block: Selection.() -> Unit) = select(selector.joinToString(), block)

    fun s(selector: String, block: Selection.() -> Unit) = select(selector, block)

    fun s(vararg selector: CSSSelector, block: Selection.() -> Unit) = select(*selector, block = block)
}

open class SelectionBlock : CssBlock() {
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

    fun mix(other: SelectionBlock) {
        properties.putAll(other.properties)
        selections.addAll(other.selections)
    }

    operator fun Mixin.unaryPlus() = this@SelectionBlock.mix(this)

    private inline fun <reified V : Any> cssprop(key: String): ReadWriteProperty<SelectionBlock, V> {
        return object : ReadWriteProperty<SelectionBlock, V> {
            override fun getValue(thisRef: SelectionBlock, property: KProperty<*>): V {
                if (!properties.containsKey(key) && MultiValue::class.java.isAssignableFrom(V::class.java))
                    properties[key] = MultiValue<V>()
                return properties[key] as V
            }

            override fun setValue(thisRef: SelectionBlock, property: KProperty<*>, value: V) {
                properties[key] = value as Any
            }
        }
    }
}

class Mixin : SelectionBlock()

class Selection(selector: String) : SelectionBlock() {
    var selector: String
    var modifier = false

    init {
        this.selector = selector.trim().replace(selectorSeparator, ", ")
    }

    fun render(current: String = ""): String {
        return buildString {
            val currentSelector = expand(current, selector)
            if (properties.isNotEmpty()) {
                append("$currentSelector {\n")
                for ((name, value) in properties) {
                    val renderedValue = toCss(value)
                    if (renderedValue.isNotBlank())
                        append("    $name: $renderedValue;\n")
                }
                append("}\n")
            }
            for (selection in selections) {
                append(selection.render(if (selection.modifier) currentSelector else "$currentSelector "))
            }
        }
    }

    private fun expand(current: String, selector: String) =
            selector.split(selectorSeparator).map { internalExpand(current, it) }.joinToString().replace("  ", " ")

    private fun internalExpand(current: String, selector: String) =
            current.split(selectorSeparator).map { it + selector }.joinToString().replace("  ", " ")

    override fun toString() = render()

    companion object {
        private val selectorSeparator = Regex("\\s*,\\s*")
    }
}

fun Double.pos(relative: Boolean) = if (relative) "${fiveDigits.format(this * 100)}%" else "${fiveDigits.format(this)}px"
fun <T> toCss(value: T): String {
    when (value) {
        null -> return ""  // This should only happen in a container (such as box(), Array<>(), Pair())
        is MultiValue<*> -> return value.elements.map { toCss(it) }.joinToString(", ")
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

open class Stylesheet : CssBlock() {
    companion object {
        // Pseudo classes used by JavaFX
        val armed by csspseudoclass()
        val disabled by csspseudoclass()
        val focused by csspseudoclass()
        val hover by csspseudoclass()
        val pressed by csspseudoclass()
        val showMnemonics by csspseudoclass()

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
    }

    open fun render() = buildString { selections.forEach { append(it) } }

    val base64URL: URL get() {
        val content = Base64.getEncoder().encodeToString(render().toByteArray(UTF_8))
        return URL("css://$content:64")
    }

    fun mixin(init: Mixin.() -> Unit): Mixin {
        val mixin = Mixin()
        mixin.init()
        return mixin
    }

    override fun toString() = render()
}

// Type safe selectors
abstract class CSSSelector(val prefix: String, _name: String? = null) {
    private val entries by lazy { mutableListOf<Pair<Type, CSSSelector>>() }

    enum class Type(val prefix: String) {
        PLUS(""), CONTAINS(" "), DIRECT(" > ");

        override fun toString() = prefix
    }

    val cssName: String
    val name: String

    companion object {
        val uc = Regex("([A-Z])")
    }

    init {
        val base = _name ?: javaClass.simpleName
        name = (base[0].toLowerCase() + base.substring(1)).replace(uc, "-$1").toLowerCase()
        cssName = prefix + name
    }

    // .box.label
    operator fun plus(other: CSSSelector): CSSSelector {
        entries.add(Pair(Type.PLUS, other))
        return this
    }

    // .box .label
    infix fun contains(other: CSSSelector): CSSSelector {
        entries.add(Pair(Type.CONTAINS, other))
        return this
    }

    // .box > .label
    infix fun direct(other: CSSSelector): CSSSelector {
        entries.add(Pair(Type.DIRECT, other))
        return this
    }

    override fun toString() = buildString {
        append(cssName)
        for ((prefix, selector) in entries) append("$prefix$selector")
    }
}

open class CSSClass(name: String? = null) : CSSSelector(".", name)
open class CSSPseudoClass(name: String? = null) : CSSSelector(":", name)
open class CSSId(name: String? = null) : CSSSelector("#", name)

class CSSClassDelegate(val name: String?) : ReadOnlyProperty<Any, CSSClass> {
    override fun getValue(thisRef: Any, property: KProperty<*>) = CSSClass(name ?: property.name)
}

class CSSPseudoClassDelegate(val name: String?) : ReadOnlyProperty<Any, CSSPseudoClass> {
    var value: CSSPseudoClass? = null
    override fun getValue(thisRef: Any, property: KProperty<*>) = CSSPseudoClass(name ?: property.name)
}

class CSSIdDelegate(val name: String?) : ReadOnlyProperty<Any, CSSId> {
    override fun getValue(thisRef: Any, property: KProperty<*>) = CSSId(name ?: property.name)
}

fun csspseudoclass(value: String? = null) = CSSPseudoClassDelegate(value)
fun cssclass(value: String? = null) = CSSClassDelegate(value)
fun cssid(value: String? = null) = CSSIdDelegate(value)

/**
 * Add styles to the node using type safe CSS
 */
fun Node.style(append: Boolean = false, op: SelectionBlock.() -> Unit) {
    val block = SelectionBlock()
    op(block)
    val output = StringBuilder()
    for ((name, value) in block.properties)
        output.append(" $name: ${toCss(value)};")

    if (append && style.isNotBlank())
        style += output.toString()
    else
        style = output.toString().trim()
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
