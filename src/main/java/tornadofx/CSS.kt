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
import sun.net.www.protocol.css.Handler
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

interface Rendered {
    fun render(): String
}

interface Scoped {
    fun toRuleSet(): CssRuleSet
    fun append(rule: CssSubRule): CssRuleSet
    infix fun and(rule: CssRule) = append(CssSubRule(rule, CssSubRule.Relation.REFINE))
    infix fun child(rule: CssRule) = append(CssSubRule(rule, CssSubRule.Relation.CHILD))
    infix fun contains(rule: CssRule) = append(CssSubRule(rule, CssSubRule.Relation.DESCENDANT))
    infix fun next(rule: CssRule) = append(CssSubRule(rule, CssSubRule.Relation.ADJACENT))
    infix fun sibling(rule: CssRule) = append(CssSubRule(rule, CssSubRule.Relation.SIBLING))
}

interface Selectable {
    fun toSelection(): CssSelector
}

interface SelectionHolder {
    fun addSelection(selection: CssSelection)
    fun removeSelection(selection: CssSelection)
    operator fun String.invoke(op: CssSelectionBlock.() -> Unit) = toSelector()(op)
    operator fun Selectable.invoke(op: CssSelectionBlock.() -> Unit): CssSelection {
        val selection = CssSelection(toSelection(), op)
        addSelection(selection)
        return selection
    }

    fun select(selector: String, op: CssSelectionBlock.() -> Unit) = selector(op)
    fun select(selector: Selectable, vararg selectors: Selectable, op: CssSelectionBlock.() -> Unit) = select(selector, *selectors)(op)
    fun select(selector: Selectable, vararg selectors: Selectable) = CssSelector(
            *selector.toSelection().rule,
            *selectors.flatMap { it.toSelection().rule.asIterable() }.toTypedArray()
    )

    fun s(selector: String, op: CssSelectionBlock.() -> Unit) = select(selector, op)
    fun s(selector: Selectable, vararg selectors: Selectable, op: CssSelectionBlock.() -> Unit) = select(selector, *selectors, op = op)
    fun s(selector: Selectable, vararg selectors: Selectable) = select(selector, *selectors)

    infix fun Scoped.and(selection: CssSelection) = append(selection, CssSubRule.Relation.REFINE)
    infix fun Scoped.child(selection: CssSelection) = append(selection, CssSubRule.Relation.CHILD)
    infix fun Scoped.contains(selection: CssSelection) = append(selection, CssSubRule.Relation.DESCENDANT)
    infix fun Scoped.next(selection: CssSelection) = append(selection, CssSubRule.Relation.ADJACENT)
    infix fun Scoped.sibling(selection: CssSelection) = append(selection, CssSubRule.Relation.SIBLING)
    fun Scoped.append(oldSelection: CssSelection, relation: CssSubRule.Relation): CssSelection {
        removeSelection(oldSelection)
        val ruleSets = oldSelection.selector.rule
        if (ruleSets.size > 1) {
            Stylesheet.log.warning { "Selection has ${ruleSets.size} selectors, but only the first will be used" }
        }
        val selection = CssSelection(CssSelector(toRuleSet().append(ruleSets[0], relation))) { mix(oldSelection.block) }
        addSelection(selection)
        return selection
    }
}

open class Stylesheet(vararg val imports: KClass<out Stylesheet>) : SelectionHolder, Rendered {
    companion object {
        val log: Logger by lazy { Logger.getLogger("CSS") }

        // Elements
        val star by csselement("*")
        // TODO: Are other elements available by default in JavaFX?

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
        val datePickerPopUp by cssclass()
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
        val track by cssclass()
        val thumb by cssclass()
        val scrollPane by cssclass()
        val separator by cssclass()
        val slider by cssclass()
        val spinner by cssclass()
        val splitMenuButton by cssclass()
        val splitPane by cssclass()
        val tableView by cssclass()
        val tabPane by cssclass()
        val tab by cssclass()
        val tabLabel by cssclass()
        val tabCloseButton by cssclass()
        val tabContentArea by cssclass()
        val tabHeaderArea by cssclass()
        val headersRegion by cssclass()
        val tabHeaderBackground by cssclass()
        val controlButtonsTab by cssclass()
        val tabDownButton by cssclass()
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
        val text by cssclass()

        // Style classes used by Form Builder
        val form by cssclass()
        val fieldset by cssclass()
        val legend by cssclass()
        val field by cssclass()
        val labelContainer by cssclass()
        val inputContainer by cssclass()

        // DataGrid
        val datagrid by cssclass()
        val datagridCell by cssclass()
        val datagridRow by cssclass()

        // Pseudo classes used by JavaFX
        val armed by csspseudoclass()
        val disabled by csspseudoclass()
        val focused by csspseudoclass()
        val hover by csspseudoclass()
        val pressed by csspseudoclass()
        val showMnemonics by csspseudoclass()
        val selected by csspseudoclass()
        val visited by csspseudoclass()
        val even by csspseudoclass()
        val odd by csspseudoclass()
        val filled by csspseudoclass()
        val empty by csspseudoclass()

        init {
            detectAndInstallUrlHandler()
        }

        fun importServiceLoadedStylesheets() {
            val loader = ServiceLoader.load(Stylesheet::class.java)
            for (style in loader) importStylesheet(style.javaClass.kotlin)
        }

        /**
         * Try to retrieve a type safe stylesheet, and force installation of url handler
         * if it's missing. This typically happens in environments with atypical class loaders.
         *
         * Under normal circumstances, the CSS handler that comes with TornadoFX should be picked
         * up by the JVM automatically.
         */
        private fun detectAndInstallUrlHandler() {
            try {
                URL("css://content:64")
            } catch (ex: MalformedURLException) {
                log.info("Installing CSS url handler, since it was not picked up automatically")
                try {
                    URL.setURLStreamHandlerFactory(Handler.HandlerFactory())
                } catch (installFailed: Throwable) {
                    log.log(Level.WARNING, "Unable to install CSS url handler, type safe stylesheets might not work", ex)
                }
            }
        }
    }

    val selections = mutableListOf<CssSelection>()

    override fun addSelection(selection: CssSelection) {
        selections += selection
    }

    override fun removeSelection(selection: CssSelection) {
        selections -= selection
    }

    override fun render() = imports.map { "@import url(css://${it.java.name})" }.joinToString(separator = "\n", postfix = "\n") +
            selections.joinToString(separator = "") { it.render() }

    val base64URL: URL get() {
        val content = Base64.getEncoder().encodeToString(render().toByteArray(StandardCharsets.UTF_8))
        return URL("css://$content:64")
    }

    val externalForm: String get() = base64URL.toExternalForm()
}

open class PropertyHolder {
    companion object {
        val selectionScope = ThreadLocal<CssSelectionBlock>()
        fun Double.pos(relative: Boolean) = if (relative) "${fiveDigits.format(this * 100)}%" else "${fiveDigits.format(this)}px"
        fun <T> toCss(value: T): String = when (value) {
            null -> ""  // This should only happen in a container (such as box(), Array<>(), Pair())
            is MultiValue<*> -> value.elements.joinToString { toCss(it) }
            is CssBox<*> -> "${toCss(value.top)} ${toCss(value.right)} ${toCss(value.bottom)} ${toCss(value.left)}"
            is FontWeight -> "${value.weight}"  // Needs to come before `is Enum<*>`
            is Enum<*> -> value.toString().toLowerCase().replace("_", "-")
            is Font -> "${if (value.style == "Regular") "normal" else value.style} ${value.size}pt ${toCss(value.family)}"
            is Cursor -> if (value is ImageCursor) {
                value.image.javaClass.getDeclaredField("url").let {
                    it.isAccessible = true
                    it.get(value.image).toString()
                }
            } else {
                value.toString()
            }
            is URI -> "url(\"${value.toASCIIString()}\")"
            is BackgroundPosition -> "${value.horizontalSide} ${value.horizontalPosition.pos(value.isHorizontalAsPercentage)} " +
                    "${value.verticalSide} ${value.verticalPosition.pos(value.isVerticalAsPercentage)}"
            is BackgroundSize -> if (value.isContain) "contain" else if (value.isCover) "cover" else buildString {
                append(if (value.width == BackgroundSize.AUTO) "auto" else value.width.pos(value.isWidthAsPercentage))
                append(" ")
                append(if (value.height == BackgroundSize.AUTO) "auto" else value.height.pos(value.isHeightAsPercentage))
            }
            is BorderStrokeStyle -> when (value) {
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
            is BorderImageSlice -> toCss(value.widths) + if (value.filled) " fill" else ""
            is Array<*> -> value.joinToString { toCss(it) }
            is Pair<*, *> -> "${toCss(value.first)} ${toCss(value.second)}"
            is KClass<*> -> "\"${value.qualifiedName}\""
            is CssProperty<*> -> value.name
            is Raw -> value.name
            is String -> "\"$value\""
            is Effect -> when (value) { // JavaFX currently only supports DropShadow and InnerShadow in CSS
                is DropShadow -> "dropshadow(${toCss(value.blurType)}, ${value.color.css}, " +
                        "${value.radius}, ${value.spread}, ${value.offsetX}, ${value.offsetY})"
                is InnerShadow -> "dropshadow(${toCss(value.blurType)}, ${value.color.css}, " +
                        "${value.radius}, ${value.choke}, ${value.offsetX}, ${value.offsetY})"
                else -> "none"
            }
            is Color -> value.css
            is Paint -> value.toString().replace(Regex("0x[0-9a-f]{8}")) { Color.web(it.groupValues[0]).css }
            else -> value.toString()
        }
    }

    val properties = linkedMapOf<String, Pair<Any, ((Any) -> String)?>>()
    val unsafeProperties = linkedMapOf<String, Any>()
    val mergedProperties: Map<String, Pair<Any, ((Any) -> String)?>> get() = LinkedHashMap(properties).apply { putAll(unsafeProperties.mapValues { it.value to null }) }

    // Root
    var focusColor: Paint by cssprop("-fx-focus-color")
    var faintFocusColor: Paint by cssprop("-fx-faint-focus-color")

    // Font
    var font: Font by cssprop("-fx-font")
    var fontFamily: String by cssprop("-fx-font-family")
    var fontSize: Dimension<Dimension.LinearUnits> by cssprop("-fx-font-size")
    var fontStyle: FontPosture by cssprop("-fx-font-style")
    var fontWeight: FontWeight by cssprop("-fx-font-weight")

    // Node
    var blendMode: BlendMode by cssprop("-fx-blend-mode")
    var cursor: Cursor by cssprop("-fx-cursor")
    var effect: Effect by cssprop("-fx-effect")
    var focusTraversable: Boolean by cssprop("-fx-focus-traversable")
    var opacity: Double by cssprop("-fx-opacity")
    var rotate: Dimension<Dimension.AngularUnits> by cssprop("-fx-rotate")
    var scaleX: Number by cssprop("-fx-scale-x")
    var scaleY: Number by cssprop("-fx-scale-y")
    var scaleZ: Number by cssprop("-fx-scale-z")
    var translateX: Dimension<Dimension.LinearUnits> by cssprop("-fx-translate-x")
    var translateY: Dimension<Dimension.LinearUnits> by cssprop("-fx-translate-y")
    var translateZ: Dimension<Dimension.LinearUnits> by cssprop("-fx-translate-z")
    var visibility: FXVisibility by cssprop("visibility")  // Intentionally not -fx-visibility

    // ImageView
    var image: URI by cssprop("-fx-image")

    // DialogPane
    var graphic: URI by cssprop("-fx-graphic")

    // FlowPane
    var hgap: Dimension<Dimension.LinearUnits> by cssprop("-fx-hgap")
    var vgap: Dimension<Dimension.LinearUnits> by cssprop("-fx-vgap")
    var alignment: Pos by cssprop("-fx-alignment")
    var columnHAlignment: HPos by cssprop("-fx-column-halignment")
    var rowVAlignment: VPos by cssprop("-fx-row-valignment")
    var orientation: Orientation by cssprop("-fx-orientation")

    // GridPane
    var gridLinesVisible: Boolean by cssprop("-fx-grid-lines-visible")

    // HBox
    var spacing: Dimension<Dimension.LinearUnits> by cssprop("-fx-spacing")
    var fillHeight: Boolean by cssprop("-fx-fill-height")

    // Region
    var backgroundColor: MultiValue<Paint> by cssprop("-fx-background-color")
    var backgroundInsets: MultiValue<CssBox<Dimension<Dimension.LinearUnits>>> by cssprop("-fx-background-insets")
    var backgroundRadius: MultiValue<CssBox<Dimension<Dimension.LinearUnits>>> by cssprop("-fx-background-radius")
    var backgroundImage: MultiValue<URI> by cssprop("-fx-background-image")
    var backgroundPosition: MultiValue<BackgroundPosition> by cssprop("-fx-background-position")
    var backgroundRepeat: MultiValue<Pair<BackgroundRepeat, BackgroundRepeat>> by cssprop("-fx-background-repeat")
    var backgroundSize: MultiValue<BackgroundSize> by cssprop("-fx-background-size")
    var borderColor: MultiValue<CssBox<Paint?>> by cssprop("-fx-border-color")
    var borderInsets: MultiValue<CssBox<Dimension<Dimension.LinearUnits>>> by cssprop("-fx-border-radius")
    var borderRadius: MultiValue<CssBox<Dimension<Dimension.LinearUnits>>> by cssprop("-fx-border-radius")
    var borderStyle: MultiValue<BorderStrokeStyle> by cssprop("-fx-border-style")
    var borderWidth: MultiValue<CssBox<Dimension<Dimension.LinearUnits>>> by cssprop("-fx-border-width")
    var borderImageSource: MultiValue<URI> by cssprop("-fx-border-image-source")
    var borderImageInsets: MultiValue<CssBox<Dimension<Dimension.LinearUnits>>> by cssprop("-fx-border-image-insets")
    var borderImageRepeat: MultiValue<Pair<BorderRepeat, BorderRepeat>> by cssprop("-fx-border-image-repeat")
    var borderImageSlice: MultiValue<BorderImageSlice> by cssprop("-fx-border-image-slice")
    var borderImageWidth: CssBox<Dimension<Dimension.LinearUnits>> by cssprop("-fx-border-image-width")
    var padding: CssBox<Dimension<Dimension.LinearUnits>> by cssprop("-fx-padding")
    var positionShape: Boolean by cssprop("-fx-position-shape")
    var scaleShape: Boolean by cssprop("-fx-scale-shape")
    var shape: String by cssprop("-fx-shape")
    var snapToPixel: Boolean by cssprop("-fx-snap-to-pixel")
    var minHeight: Dimension<Dimension.LinearUnits> by cssprop("-fx-min-height")
    var prefHeight: Dimension<Dimension.LinearUnits> by cssprop("-fx-pref-height")
    var maxHeight: Dimension<Dimension.LinearUnits> by cssprop("-fx-max-height")
    var minWidth: Dimension<Dimension.LinearUnits> by cssprop("-fx-min-width")
    var prefWidth: Dimension<Dimension.LinearUnits> by cssprop("-fx-pref-width")
    var maxWidth: Dimension<Dimension.LinearUnits> by cssprop("-fx-max-width")

    // TilePane
    var prefRows: Int by cssprop("-fx-pref-rows")
    var prefColumns: Int by cssprop("-fx-pref-columns")
    var prefTileWidth: Dimension<Dimension.LinearUnits> by cssprop("-fx-pref-tile-width")
    var prefTileHeight: Dimension<Dimension.LinearUnits> by cssprop("-fx-pref-tile-height")
    var tileAlignment: Pos by cssprop("-fx-tile-alignment")

    // VBox
    var fillWidth: Boolean by cssprop("-fx-fill-width")

    // Shape
    var fill: Paint by cssprop("-fx-fill")
    var smooth: Boolean by cssprop("-fx-smooth")
    var stroke: Paint by cssprop("-fx-stroke")
    var strokeType: StrokeType by cssprop("-fx-stroke-type")
    var strokeDashArray: Array<Dimension<Dimension.LinearUnits>> by cssprop("-fx-stroke-dash-array")
    var strokeDashOffset: Dimension<Dimension.LinearUnits> by cssprop("-fx-stroke-dash-offset")
    var strokeLineCap: StrokeLineCap by cssprop("-fx-stroke-line-cap")
    var strokeLineJoin: StrokeLineJoin by cssprop("-fx-stroke-line-join")
    var strokeMiterLimit: Double by cssprop("-fx-stroke-miter-limit")
    var strokeWidth: Dimension<Dimension.LinearUnits> by cssprop("-fx-stroke-width")

    // Rectangle
    var arcHeight: Dimension<Dimension.LinearUnits> by cssprop("-fx-arc-height")
    var arcWidth: Dimension<Dimension.LinearUnits> by cssprop("-fx-arc-width")

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
    var cellSize: Dimension<Dimension.LinearUnits> by cssprop("-fx-cell-size")

    // DataGrid Celll
    var cellWidth: Dimension<Dimension.LinearUnits> by cssprop("-fx-cell-width")
    var cellHeight: Dimension<Dimension.LinearUnits> by cssprop("-fx-cell-height")
    var horizontalCellSpacing: Dimension<Dimension.LinearUnits> by cssprop("-fx-horizontal-cell-spacing")
    var verticalCellSpacing: Dimension<Dimension.LinearUnits> by cssprop("-fx-vertical-cell-spacing")

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
    var graphicTextGap: Dimension<Dimension.LinearUnits> by cssprop("-fx-graphic-text-gap")
    var labelPadding: CssBox<Dimension<Dimension.LinearUnits>> by cssprop("-fx-label-padding")
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
    var indeterminateBarLength: Dimension<Dimension.LinearUnits> by cssprop("-fx-indeterminate-bar-length")
    var indeterminateBarEscape: Boolean by cssprop("-fx-indeterminate-bar-escape")
    var indeterminateBarFlip: Boolean by cssprop("-fx-indeterminate-bar-flip")
    var indeterminateBarAnimationTime: Number by cssprop("-fx-indeterminate-bar-animation-time")

    // ProgressIndicator
    var indeterminateSegmentCount: Int by cssprop("-fx-indeterminate-SegmentCount")
    var progressColor: Paint by cssprop("-fx-progress-color")
    var spinEnabled: Boolean by cssprop("-fx-spin-enabled")

    // ScrollBar
    var blockIncrement: Dimension<Dimension.LinearUnits> by cssprop("-fx-block-increment")
    var unitIncrement: Dimension<Dimension.LinearUnits> by cssprop("-fx-unit-increment")

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
    var tabMaxHeight: Dimension<Dimension.LinearUnits> by cssprop("-fx-tab-max-height")
    var tabMinHeight: Dimension<Dimension.LinearUnits> by cssprop("-fx-tab-min-height")
    var tabMaxWidth: Dimension<Dimension.LinearUnits> by cssprop("-fx-tab-max-width")
    var tabMinWidth: Dimension<Dimension.LinearUnits> by cssprop("-fx-tab-min-width")
    var openTabAnimation: FXTabAnimation by cssprop("-fx-open-tab-animation")
    var closeTabAnimation: FXTabAnimation by cssprop("-fx-close-tab-animation")

    // TableColumnHeader
    var size: Dimension<Dimension.LinearUnits> by cssprop("-fx-size")

    // TableView
    var fixedCellSize: Dimension<Dimension.LinearUnits> by cssprop("-fx-fixed-cell-size")

    // TextArea
    var prefColumnCount: Int by cssprop("-fx-pref-column-count")
    var prefRowCount: Int by cssprop("-fx-pref-row-count")
    var textBoxBorder: Paint by cssprop("-fx-text-box-border")

    // TextInputControl
    var promptTextFill: Paint by cssprop("-fx-prompt-text-fill")
    var highlightFill: Paint by cssprop("-fx-highlight-fill")
    var highlightTextFill: Paint by cssprop("-fx-highlight-text-fill")
    var displayCaret: Boolean by cssprop("-fx-display-caret")

    // TitlePane
    var animated: Boolean by cssprop("-fx-animated")
    var collapsible: Boolean by cssprop("-fx-collapsible")

    // TreeCell
    var indent: Dimension<Dimension.LinearUnits> by cssprop("-fx-indent")

    // Axis
    var side: Side by cssprop("-fx-side")
    var tickLength: Dimension<Dimension.LinearUnits> by cssprop("-fx-tick-length")
    var tickLabelFont: Font by cssprop("-fx-tick-label-font")
    var tickLabelFill: Paint by cssprop("-fx-tick-label-fill")
    var tickLabelGap: Dimension<Dimension.LinearUnits> by cssprop("-fx-tick-label-gap")
    var tickMarkVisible: Boolean by cssprop("-fx-tick-mark-visible")
    var tickLabelsVisible: Boolean by cssprop("-fx-tick-labels-visible")

    // BarChar
    var barGap: Dimension<Dimension.LinearUnits> by cssprop("-fx-bar-gap")
    var categoryGap: Dimension<Dimension.LinearUnits> by cssprop("-fx-category-group")

    // CategoryAxis
    var startMargin: Dimension<Dimension.LinearUnits> by cssprop("-fx-start-margin")
    var endMargin: Dimension<Dimension.LinearUnits> by cssprop("-fx-end-margin")
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
    var labelLineLength: Dimension<Dimension.LinearUnits> by cssprop("-fx-label-line-length")
    var startAngle: Dimension<Dimension.AngularUnits> by cssprop("-fx-start-angle")

    // ValueAxis
    var minorTickLength: Dimension<Dimension.LinearUnits> by cssprop("-fx-minor-tick-length")
    var minorTickVisible: Boolean by cssprop("-fx-minor-tick-visible")

    // XYChart
    var alternativeColumnFillVisible: Boolean by cssprop("-fx-alternative-column-fill-visible")
    var alternativeRowFillVisible: Boolean by cssprop("-fx-alternative-row-fill-visible")
    var horizontalGridLinesVisible: Boolean by cssprop("-fx-horizontal-grid-lines-visible")
    var horizontalZeroLineVisible: Boolean by cssprop("-fx-horizontal-zero-line-visible")
    var verticalGridLinesVisible: Boolean by cssprop("-fx-vertical-grid-lines-visible")
    var verticalZeroLineVisible: Boolean by cssprop("-fx-vertical-zero-line-visible")

    infix fun CssProperty<*>.force(value: Any) = unsafe(this, value)
    infix fun String.force(value: Any) = unsafe(this, value)
    fun unsafe(key: CssProperty<*>, value: Any) = unsafe(key.name, value)
    fun unsafe(key: String, value: Any) {
        unsafeProperties[key] = value
    }

    private inline fun <reified V : Any> cssprop(key: String): ReadWriteProperty<PropertyHolder, V> {
        return object : ReadWriteProperty<PropertyHolder, V> {
            override fun getValue(thisRef: PropertyHolder, property: KProperty<*>): V {
                if (!properties.containsKey(key) && MultiValue::class.java.isAssignableFrom(V::class.java))
                    properties[key] = MultiValue<V>() to null
                return properties[key]?.first as V
            }

            override fun setValue(thisRef: PropertyHolder, property: KProperty<*>, value: V) {
                properties[key] = value as Any to properties[key]?.second
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class CssProperty<T>(name: String, val multiValue: Boolean, val renderer: ((T) -> String)? = null) {
        val name = name.camelToSnake()
        var value: T
            get() {
                val props = selectionScope.get().properties

                if (!props.containsKey(name) && multiValue)
                    props[name] = MultiValue<T>() to renderer as ((Any) -> String)?

                return selectionScope.get().properties[name]?.first as T
            }
            set(value) {
                selectionScope.get().properties.put(name, value as Any to renderer as ((Any) -> String)?)
            }
    }

    infix fun <T : Any> CssProperty<T>.set(value: T) = setProperty(this, value)
    fun <T : Any> setProperty(property: CssProperty<T>, value: T) {
        properties[property.name] = value to properties[property.name]?.second
    }

    class Raw(val name: String)

    fun raw(name: String) = Raw(name)
}

@Suppress("NAME_SHADOWING")
class CssSelection(val selector: CssSelector, op: CssSelectionBlock.() -> Unit) : Rendered {
    val block = CssSelectionBlock(op)

    override fun render() = render(emptyList(), false)

    fun render(parents: List<String>, refine: Boolean): String = buildString {
        val ruleStrings = selector.strings(parents, refine)
        block.mergedProperties.let {
            // TODO: Handle custom renderer
            if (it.isNotEmpty()) {
                append("${ruleStrings.joinToString()} {\n")
                for ((name, value) in it) {
                    append("    $name: ${value.second?.invoke(value.first) ?: PropertyHolder.toCss(value.first)};\n")
                }
                append("}\n")
            }
        }
        for ((selection, refine) in block.selections) {
            append(selection.render(ruleStrings, refine))
        }
    }
}

class CssSelector(vararg val rule: CssRuleSet) : Selectable {
    companion object {
        fun String.merge(other: String, refine: Boolean) = if (refine) "$this$other" else "$this $other"
        fun List<String>.cartesian(parents: List<String>, refine: Boolean) = if (parents.isEmpty()) this else
            parents.asSequence().flatMap { parent -> asSequence().map { child -> parent.merge(child, refine) } }.toList()
    }

    override fun toSelection() = this
    fun strings(parents: List<String>, refine: Boolean) = rule.map { it.render() }.cartesian(parents, refine)

    fun simpleRender() = rule.map { it.render() }.joinToString()
}

class CssSelectionBlock(op: CssSelectionBlock.() -> Unit) : PropertyHolder(), SelectionHolder {
    val selections = mutableMapOf<CssSelection, Boolean>()  // If the boolean is true, this is a refine selection

    init {
        val currentScope = PropertyHolder.selectionScope.get()
        PropertyHolder.selectionScope.set(this)
        op(this)
        PropertyHolder.selectionScope.set(currentScope)
    }

    override fun addSelection(selection: CssSelection) {
        selections[selection] = false
    }

    override fun removeSelection(selection: CssSelection) {
        selections.remove(selection)
    }

    operator fun CssSelectionBlock.unaryPlus() {
        this@CssSelectionBlock.mix(this)
    }

    @Deprecated("Use and() instead as it's clearer", ReplaceWith("and(selector, op)"))
    fun add(selector: String, op: CssSelectionBlock.() -> Unit) = and(selector, op)

    @Deprecated("Use and() instead as it's clearer", ReplaceWith("and(selector, *selectors, op = op)"))
    fun add(selector: Selectable, vararg selectors: Selectable, op: CssSelectionBlock.() -> Unit) = and(selector, *selectors, op = op)

    fun and(selector: String, op: CssSelectionBlock.() -> Unit) = and(selector.toSelector(), op = op)
    fun and(selector: Selectable, vararg selectors: Selectable, op: CssSelectionBlock.() -> Unit): CssSelection {
        val s = select(selector, *selectors)(op)
        selections[s] = true
        return s
    }

    @Suppress("UNCHECKED_CAST")
    fun mix(mixin: CssSelectionBlock) {
        mixin.properties.forEach { k, v ->
            if (properties[k]?.first is MultiValue<*>)
                (properties[k]?.first as MultiValue<Any>).addAll(v.first as MultiValue<Any>)
            else
                properties[k] = v
        }
        mixin.unsafeProperties.forEach { k, v ->
            if (unsafeProperties[k] is MultiValue<*>)
                (unsafeProperties[k] as MultiValue<Any>).addAll(v as MultiValue<Any>)
            else
                unsafeProperties[k] = v
        }
        selections.putAll(mixin.selections)
    }
}

fun mixin(op: CssSelectionBlock.() -> Unit) = CssSelectionBlock(op)

class CssRuleSet(val rootRule: CssRule, vararg val subRule: CssSubRule) : Selectable, Scoped, Rendered {
    override fun render() = buildString {
        append(rootRule.render())
        subRule.forEach { append(it.render()) }
    }

    override fun toRuleSet() = this
    override fun toSelection() = CssSelector(this)
    override fun append(rule: CssSubRule) = CssRuleSet(rootRule, *subRule, rule)
    fun append(ruleSet: CssRuleSet, relation: CssSubRule.Relation)
            = CssRuleSet(rootRule, *subRule, CssSubRule(ruleSet.rootRule, relation), *ruleSet.subRule)
}

class CssRule(val prefix: String, name: String, snakeCase: Boolean = true) : Selectable, Scoped, Rendered {
    companion object {
        fun elem(value: String, snakeCase: Boolean = true) = CssRule("", value.cssValidate(), snakeCase)
        fun id(value: String, snakeCase: Boolean = true) = CssRule("#", value.cssValidate(), snakeCase)
        fun c(value: String, snakeCase: Boolean = true) = CssRule(".", value.cssValidate(), snakeCase)
        fun pc(value: String, snakeCase: Boolean = true) = CssRule(":", value.cssValidate(), snakeCase)

        private val name = "\\*|-?[_a-zA-Z][_a-zA-Z0-9-]*"  // According to http://stackoverflow.com/a/449000/2094298
        private val prefix = "[.#:]?"
        private val relation = "[ >+~]?"
        private val subRule = "\\s*?($relation)\\s*($prefix)($name)"
        val nameRegex = Regex(name)
        val subRuleRegex = Regex(subRule)
        val ruleSetRegex = Regex("($subRule)+\\s*")
        val splitter = Regex("\\s*,\\s*")
        val upperCaseRegex = Regex("([A-Z])")
    }

    val name = if (snakeCase) name.camelToSnake() else name

    override fun render() = "$prefix$name"
    override fun toRuleSet() = CssRuleSet(this)
    override fun toSelection() = toRuleSet().toSelection()
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

        companion object {
            fun of(symbol: String) = when (symbol) {
                "" -> REFINE
                ">" -> CHILD
                " " -> DESCENDANT
                "+" -> ADJACENT
                "~" -> SIBLING
                else -> throw IllegalArgumentException("Invalid Css Relation: $symbol")
            }
        }

        override fun render() = symbol
    }
}

// Inline CSS

class InlineCss : PropertyHolder(), Rendered {
    // TODO: Handle custom renderer
    override fun render() = mergedProperties.entries.joinToString(separator = "") {
        " ${it.key}: ${it.value.second?.invoke(it.value.first) ?: toCss(it.value.first)};"
    }
}

fun Iterable<Node>.style(append: Boolean = false, op: InlineCss.() -> Unit) = forEach { it.style(append, op) }
fun Node.style(append: Boolean = false, op: InlineCss.() -> Unit) {
    val block = InlineCss().apply(op)

    if (append && style.isNotBlank())
        style += block.render()
    else
        style = block.render().trim()
}

// Delegates

fun csselement(value: String? = null, snakeCase: Boolean = value == null) = CssElementDelegate(value, snakeCase)
fun cssid(value: String? = null, snakeCase: Boolean = value == null) = CssIdDelegate(value, snakeCase)
fun cssclass(value: String? = null, snakeCase: Boolean = value == null) = CssClassDelegate(value, snakeCase)
fun csspseudoclass(value: String? = null, snakeCase: Boolean = value == null) = CssPseudoClassDelegate(value, snakeCase)
inline fun <reified T : Any> cssproperty(value: String? = null, noinline renderer: ((T) -> String)? = null) = CssPropertyDelegate<T>(value, MultiValue::class.java.isAssignableFrom(T::class.java), renderer)

class CssElementDelegate(val name: String?, val snakeCase: Boolean = name == null) : ReadOnlyProperty<Any, CssRule> {
    override fun getValue(thisRef: Any, property: KProperty<*>) = CssRule.elem(name ?: property.name, snakeCase)
}

class CssIdDelegate(val name: String?, val snakeCase: Boolean = name == null) : ReadOnlyProperty<Any, CssRule> {
    override fun getValue(thisRef: Any, property: KProperty<*>) = CssRule.id(name ?: property.name, snakeCase)
}

class CssClassDelegate(val name: String?, val snakeCase: Boolean = name == null) : ReadOnlyProperty<Any, CssRule> {
    override fun getValue(thisRef: Any, property: KProperty<*>) = CssRule.c(name ?: property.name, snakeCase)
}

class CssPseudoClassDelegate(val name: String?, val snakeCase: Boolean = name == null) : ReadOnlyProperty<Any, CssRule> {
    override fun getValue(thisRef: Any, property: KProperty<*>) = CssRule.pc(name ?: property.name, snakeCase)
}

class CssPropertyDelegate<T : Any>(val name: String?, val multiValue: Boolean, val renderer: ((T) -> String)? = null) : ReadOnlyProperty<Any, PropertyHolder.CssProperty<T>> {
    override fun getValue(thisRef: Any, property: KProperty<*>) = PropertyHolder.CssProperty<T>(name ?: property.name, multiValue, renderer)
}

// Dimensions

open class Dimension<T : Enum<T>>(val value: Double, val units: T) {
    operator fun unaryPlus() = this
    operator fun unaryMinus() = Dimension(-value, units)
    operator fun plus(value: Number) = Dimension(this.value + value.toDouble(), units)
    operator fun plus(value: Dimension<T>) = safeMath(value, Double::plus)
    operator fun minus(value: Number) = Dimension(this.value - value.toDouble(), units)
    operator fun minus(value: Dimension<T>) = safeMath(value, Double::minus)
    operator fun times(value: Number) = Dimension(this.value * value.toDouble(), units)
    operator fun div(value: Number) = Dimension(this.value / value.toDouble(), units)
    operator fun mod(value: Number) = Dimension(this.value % value.toDouble(), units)

    private fun safeMath(value: Dimension<T>, op: (Double, Double) -> Double) = if (units == value.units)
        Dimension(op(this.value, value.value), units)
    else
        throw IllegalArgumentException("Cannot combine $this and $value: Units do not match")

    override fun equals(other: Any?) = other != null && other is Dimension<*> && value == other.value && units == other.units
    override fun hashCode() = value.hashCode() * 31 + units.hashCode()

    override fun toString() = when (value) {
        Double.POSITIVE_INFINITY, Double.MAX_VALUE -> "infinity"
        Double.NEGATIVE_INFINITY, Double.MIN_VALUE -> "-infinity"
        Double.NaN -> "0$units"
        else -> "${fiveDigits.format(value)}$units"
    }

    enum class AngularUnits { deg, rad, grad, turn }
    enum class LinearUnits(val value: String) {
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

operator fun <T : Enum<T>> Number.plus(value: Dimension<T>) = Dimension(this.toDouble() + value.value, value.units)
operator fun <T : Enum<T>> Number.minus(value: Dimension<T>) = Dimension(this.toDouble() - value.value, value.units)
operator fun <T : Enum<T>> Number.times(value: Dimension<T>) = Dimension(this.toDouble() * value.value, value.units)

val infinity = Dimension(Double.POSITIVE_INFINITY, Dimension.LinearUnits.px)

val Number.px: Dimension<Dimension.LinearUnits> get() = Dimension(this.toDouble(), Dimension.LinearUnits.px)
val Number.mm: Dimension<Dimension.LinearUnits> get() = Dimension(this.toDouble(), Dimension.LinearUnits.mm)
val Number.cm: Dimension<Dimension.LinearUnits> get() = Dimension(this.toDouble(), Dimension.LinearUnits.cm)
val Number.inches: Dimension<Dimension.LinearUnits> get() = Dimension(this.toDouble(), Dimension.LinearUnits.inches)
val Number.pt: Dimension<Dimension.LinearUnits> get() = Dimension(this.toDouble(), Dimension.LinearUnits.pt)
val Number.pc: Dimension<Dimension.LinearUnits> get() = Dimension(this.toDouble(), Dimension.LinearUnits.pc)
val Number.em: Dimension<Dimension.LinearUnits> get() = Dimension(this.toDouble(), Dimension.LinearUnits.em)
val Number.ex: Dimension<Dimension.LinearUnits> get() = Dimension(this.toDouble(), Dimension.LinearUnits.ex)
val Number.percent: Dimension<Dimension.LinearUnits> get() = Dimension(this.toDouble(), Dimension.LinearUnits.percent)

val Number.deg: Dimension<Dimension.AngularUnits> get() = Dimension(this.toDouble(), Dimension.AngularUnits.deg)
val Number.rad: Dimension<Dimension.AngularUnits> get() = Dimension(this.toDouble(), Dimension.AngularUnits.rad)
val Number.grad: Dimension<Dimension.AngularUnits> get() = Dimension(this.toDouble(), Dimension.AngularUnits.grad)
val Number.turn: Dimension<Dimension.AngularUnits> get() = Dimension(this.toDouble(), Dimension.AngularUnits.turn)

// Enums

enum class FXVisibility { VISIBLE, HIDDEN, COLLAPSE, INHERIT; }
enum class FXTabAnimation { GROW, NONE; }

// Misc

val fiveDigits = DecimalFormat("#.#####", DecimalFormatSymbols.getInstance(Locale.ENGLISH))

val Color.css: String get() = "rgba(${(red * 255).toInt()}, ${(green * 255).toInt()}, ${(blue * 255).toInt()}, ${fiveDigits.format(opacity)})"

internal fun String.camelToSnake() = (get(0).toLowerCase() + substring(1)).replace(CssRule.upperCaseRegex, "-$1").toLowerCase()
internal fun String.cssValidate() = if (matches(CssRule.nameRegex)) this else throw IllegalArgumentException("Invalid CSS Name: $this")
internal fun String.toSelector() = CssSelector(*split(CssRule.splitter).map(String::toRuleSet).toTypedArray())
internal fun String.toRuleSet() = if (matches(CssRule.ruleSetRegex)) {
    val rules = CssRule.subRuleRegex.findAll(this)
            .map { CssSubRule(CssRule(it.groupValues[2], it.groupValues[3], false), CssSubRule.Relation.of(it.groupValues[1])) }
            .toList()
    CssRuleSet(rules[0].rule, *rules.drop(1).toTypedArray())
} else throw IllegalArgumentException("Invalid CSS Rule Set: $this")


// Style Class

fun Node.hasClass(cssClass: CssRule) = if (cssClass.prefix == ":") hasPseudoClass(cssClass.name) else hasClass(cssClass.name)

fun <T : Node> T.addClass(vararg cssClass: CssRule): T {
    cssClass.forEach {
        if (it.prefix == ":") addPseudoClass(it.name) else addClass(it.name)
    }
    return this
}

fun <T : Node> T.removeClass(vararg cssClass: CssRule): T {
    cssClass.forEach {
        if (it.prefix == ":") removePseudoClass(it.name) else removeClass(it.name)
    }
    return this
}

fun <T : Node> T.toggleClass(cssClass: CssRule, predicate: Boolean) = if (cssClass.prefix == ":") togglePseudoClass(cssClass.name, predicate) else toggleClass(cssClass.name, predicate)
fun <T : Node> T.toggleClass(cssClass: CssRule, observablePredicate: ObservableValue<Boolean>) {
    toggleClass(cssClass, observablePredicate.value ?: false)
    observablePredicate.onChange {
        toggleClass(cssClass, it ?: false)
    }
}

fun Iterable<Node>.addClass(vararg cssClass: CssRule) = forEach { node -> cssClass.forEach { node.addClass(it) } }
fun Iterable<Node>.removeClass(vararg cssClass: CssRule) = forEach { node -> cssClass.forEach { node.removeClass(it) } }
fun Iterable<Node>.toggleClass(cssClass: CssRule, predicate: Boolean) = forEach { it.toggleClass(cssClass, predicate) }

fun Node.bindClass(value: ObservableValue<CssRule>): ObservableStyleClass = ObservableStyleClass(this, value)
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

@Suppress("UNCHECKED_CAST")
fun <T : Node> Node.select(selector: Selectable) = lookup(selector.toSelection().simpleRender()) as T

@Suppress("UNCHECKED_CAST")
fun <T : Node> Node.selectAll(selector: Selectable) = (lookupAll(selector.toSelection().simpleRender()) as Set<T>).toList()

fun <T : Node> T.setId(cssId: CssRule): T {
    id = cssId.name
    return this
}

// Containers

fun <T> box(all: T) = CssBox(all, all, all, all)
fun <T> box(vertical: T, horizontal: T) = CssBox(vertical, horizontal, vertical, horizontal)
fun <T> box(top: T, right: T, bottom: T, left: T) = CssBox(top, right, bottom, left)
data class CssBox<out T>(val top: T, val right: T, val bottom: T, val left: T)

fun c(colorString: String, opacity: Double = 1.0): Color = try {
    Color.web(colorString, opacity)
} catch (e: Exception) {
    Stylesheet.log.warning("Error parsing color c('$colorString', opacity=$opacity)")
    Color.MAGENTA
}

fun c(red: Double, green: Double, blue: Double, opacity: Double = 1.0): Color = try {
    Color.color(red, green, blue, opacity)
} catch (e: Exception) {
    Stylesheet.log.warning("Error parsing color c(red=$red, green=$green, blue=$blue, opacity=$opacity)")
    Color.MAGENTA
}

fun c(red: Int, green: Int, blue: Int, opacity: Double = 1.0): Color = try {
    Color.rgb(red, green, blue, opacity)
} catch (e: Exception) {
    Stylesheet.log.warning("Error parsing color c(red=$red, green=$green, blue=$blue, opacity=$opacity)")
    Color.MAGENTA
}

fun <T> multi(vararg elements: T) = MultiValue(elements)
class MultiValue<T>(initialElements: Array<out T>? = null) {
    val elements = mutableListOf<T>()

    init {
        if (initialElements != null) elements.addAll(initialElements)
    }

    operator fun plusAssign(element: T) {
        elements.add(element)
    }

    fun add(element: T) = elements.add(element)
    fun add(multivalue: MultiValue<T>) = addAll(multivalue.elements)
    fun addAll(list: Iterable<T>) = elements.addAll(list)
    fun addAll(vararg element: T) = elements.addAll(element)
}

class BorderImageSlice(val widths: CssBox<Dimension<Dimension.LinearUnits>>, val filled: Boolean = false)
