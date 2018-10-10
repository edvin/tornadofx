package tornadofx

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.css.Styleable
import javafx.geometry.*
import javafx.scene.Cursor
import javafx.scene.ImageCursor
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.ContentDisplay
import javafx.scene.control.OverrunStyle
import javafx.scene.control.ScrollPane
import javafx.scene.effect.BlendMode
import javafx.scene.effect.DropShadow
import javafx.scene.effect.Effect
import javafx.scene.effect.InnerShadow
import javafx.scene.layout.*
import javafx.scene.paint.*
import javafx.scene.shape.StrokeLineCap
import javafx.scene.shape.StrokeLineJoin
import javafx.scene.shape.StrokeType
import javafx.scene.text.*
import sun.net.www.protocol.css.Handler
import tornadofx.PropertyHolder.CssProperty
import java.lang.invoke.MethodHandles
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
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
    infix fun and(rule: CssRule): CssRuleSet = append(CssSubRule(rule, CssSubRule.Relation.REFINE))
    infix fun child(rule: CssRule): CssRuleSet = append(CssSubRule(rule, CssSubRule.Relation.CHILD))
    infix fun contains(rule: CssRule): CssRuleSet = append(CssSubRule(rule, CssSubRule.Relation.DESCENDANT))
    infix fun next(rule: CssRule): CssRuleSet = append(CssSubRule(rule, CssSubRule.Relation.ADJACENT))
    infix fun sibling(rule: CssRule): CssRuleSet = append(CssSubRule(rule, CssSubRule.Relation.SIBLING))
}

interface Selectable {
    fun toSelection(): CssSelector
}

interface SelectionHolder {
    fun addSelection(selection: CssSelection)
    fun removeSelection(selection: CssSelection)

    operator fun String.invoke(op: CssSelectionBlock.() -> Unit): CssSelection = toSelector()(op)
    operator fun Selectable.invoke(op: CssSelectionBlock.() -> Unit): CssSelection = CssSelection(toSelection(), op).also { addSelection(it) }

    fun select(selector: String, op: CssSelectionBlock.() -> Unit): CssSelection = selector(op)
    fun select(selector: Selectable, vararg selectors: Selectable, op: CssSelectionBlock.() -> Unit): CssSelection = select(selector, *selectors)(op)
    fun select(selector: Selectable, vararg selectors: Selectable): CssSelector =
        CssSelector(*selector.toSelection().rule, *selectors.flatMap { it.toSelection().rule.asIterable() }.toTypedArray())

    fun s(selector: String, op: CssSelectionBlock.() -> Unit): CssSelection = select(selector, op)
    fun s(selector: Selectable, vararg selectors: Selectable, op: CssSelectionBlock.() -> Unit): CssSelection = select(selector, *selectors, op = op)
    fun s(selector: Selectable, vararg selectors: Selectable): CssSelector = select(selector, *selectors)

    infix fun Scoped.and(selection: CssSelection): CssSelection = append(selection, CssSubRule.Relation.REFINE)
    infix fun Scoped.child(selection: CssSelection): CssSelection = append(selection, CssSubRule.Relation.CHILD)
    infix fun Scoped.contains(selection: CssSelection): CssSelection = append(selection, CssSubRule.Relation.DESCENDANT)
    infix fun Scoped.next(selection: CssSelection): CssSelection = append(selection, CssSubRule.Relation.ADJACENT)
    infix fun Scoped.sibling(selection: CssSelection): CssSelection = append(selection, CssSubRule.Relation.SIBLING)

    fun Scoped.append(oldSelection: CssSelection, relation: CssSubRule.Relation): CssSelection {
        removeSelection(oldSelection)
        val ruleSets = oldSelection.selector.rule
        if (ruleSets.size > 1) Stylesheet.log.warning("Selection has ${ruleSets.size} selectors, but only the first will be used")
        return CssSelection(CssSelector(toRuleSet().append(ruleSets[0], relation))) { mix(oldSelection.block) }.also { addSelection(it) }
    }
}

open class Stylesheet(vararg val imports: KClass<out Stylesheet>) : SelectionHolder, Rendered {

    @Suppress("unused")
    companion object {
        val log: Logger by lazy { Logger.getLogger("CSS") }

        // IDs
        val buttonsHbox: CssRule by cssid()
        val colorBarIndicator: CssRule by cssid()
        val colorRectIndicator: CssRule by cssid()
        val settingsPane: CssRule by cssid()
        val spacer1: CssRule by cssid()
        val spacer2: CssRule by cssid()
        val spacerBottom: CssRule by cssid()
        val spacerSide: CssRule by cssid()

        // Elements
        val star: CssRule by csselement("*")

        // Style classes used by JavaFX
        val accordion: CssRule by cssclass()
        val alert: CssRule by cssclass()
        val areaLegendSymbol: CssRule by cssclass()
        val arrow: CssRule by cssclass()
        val arrowButton: CssRule by cssclass()
        val axis: CssRule by cssclass()
        val axisLabel: CssRule by cssclass()
        val axisMinorTickMark: CssRule by cssclass()
        val axisTickMark: CssRule by cssclass()
        val bar: CssRule by cssclass()
        val barChart: CssRule by cssclass()
        val barLegendSymbol: CssRule by cssclass()
        val bottomClass: CssRule by cssclass("bottom")
        val box: CssRule by cssclass()
        val bubbleLegendSymbol: CssRule by cssclass()
        val bullet: CssRule by cssclass()
        val bulletButton: CssRule by cssclass()
        val button: CssRule by cssclass()
        val buttonBar: CssRule by cssclass()
        val calendarGrid: CssRule by cssclass()
        val cell: CssRule by cssclass()
        val centerPill: CssRule by cssclass()
        val chart: CssRule by cssclass()
        val chartAlternativeColumnFill: CssRule by cssclass()
        val chartAlternativeRowFill: CssRule by cssclass()
        val chartAreaSymbol: CssRule by cssclass()
        val chartBar: CssRule by cssclass()
        val chartBubble: CssRule by cssclass()
        val chartContent: CssRule by cssclass()
        val chartHorizontalGridLines: CssRule by cssclass()
        val chartHorizontalZeroLine: CssRule by cssclass()
        val chartLegend: CssRule by cssclass()
        val chartLegendItem: CssRule by cssclass()
        val chartLegendSymbol: CssRule by cssclass()
        val chartLineSymbol: CssRule by cssclass()
        val chartPie: CssRule by cssclass()
        val chartPieLabel: CssRule by cssclass()
        val chartPieLabelLine: CssRule by cssclass()
        val chartPlotBackground: CssRule by cssclass()
        val chartSeriesAreaFill: CssRule by cssclass()
        val chartSeriesAreaLine: CssRule by cssclass()
        val chartSeriesLine: CssRule by cssclass()
        val chartSymbol: CssRule by cssclass()
        val chartTitle: CssRule by cssclass()
        val chartVerticalGridLines: CssRule by cssclass()
        val chartVerticalZeroLine: CssRule by cssclass()
        val checkBox: CssRule by cssclass()
        val checkBoxTableCell: CssRule by cssclass()
        val choiceBox: CssRule by cssclass()
        val choiceDialog: CssRule by cssclass()
        val clippedContainer: CssRule by cssclass()
        val colorInputField: CssRule by cssclass()
        val colorPalette: CssRule by cssclass()
        val colorPaletteRegion: CssRule by cssclass()
        val colorPicker: CssRule by cssclass()
        val colorPickerGrid: CssRule by cssclass()
        val colorPickerLabel: CssRule by cssclass()
        val colorRect: CssRule by cssclass()
        val colorRectBorder: CssRule by cssclass()
        val colorRectPane: CssRule by cssclass()
        val colorSquare: CssRule by cssclass()
        val columnDragHeader: CssRule by cssclass()
        val columnHeader: CssRule by cssclass()
        val columnHeaderBackground: CssRule by cssclass()
        val columnOverlay: CssRule by cssclass()
        val columnResizeLine: CssRule by cssclass()
        val comboBox: CssRule by cssclass()
        val comboBoxBase: CssRule by cssclass()
        val comboBoxPopup: CssRule by cssclass()
        val confirmation: CssRule by cssclass()
        val container: CssRule by cssclass()
        val content: CssRule by cssclass()
        val contextMenu: CssRule by cssclass()
        val controlBox: CssRule by cssclass()
        val controlButtonsTab: CssRule by cssclass()
        val controlsPane: CssRule by cssclass()
        val corner: CssRule by cssclass()
        val currentNewColorGrid: CssRule by cssclass()
        val customcolorControlsBackground: CssRule by cssclass()  // color lowercase intentionally (that's how it is in modena)
        val customColorDialog: CssRule by cssclass()
        val dateCell: CssRule by cssclass()
        val datePicker: CssRule by cssclass()
        val datePickerPopup: CssRule by cssclass()
        val dayCell: CssRule by cssclass()
        val dayNameCell: CssRule by cssclass()
        val decrementArrow: CssRule by cssclass()
        val decrementArrowButton: CssRule by cssclass()
        val decrementButton: CssRule by cssclass()
        val defaultColor0: CssRule by cssclass()
        val defaultColor1: CssRule by cssclass()
        val defaultColor2: CssRule by cssclass()
        val defaultColor3: CssRule by cssclass()
        val defaultColor4: CssRule by cssclass()
        val defaultColor5: CssRule by cssclass()
        val defaultColor6: CssRule by cssclass()
        val defaultColor7: CssRule by cssclass()
        val detailsButton: CssRule by cssclass()
        val determinateIndicator: CssRule by cssclass()
        val dialogPane: CssRule by cssclass()
        val dot: CssRule by cssclass()
        val edgeToEdge: CssRule by cssclass()
        val emptyTable: CssRule by cssclass()
        val error: CssRule by cssclass()
        val expandableContent: CssRule by cssclass()
        val filler: CssRule by cssclass()
        val firstTitledPane: CssRule by cssclass()
        val floating: CssRule by cssclass()
        val focusIndicator: CssRule by cssclass()
        val formSelectButton: CssRule by cssclass()
        val graphicContainer: CssRule by cssclass()
        val headerPanel: CssRule by cssclass()
        val headersRegion: CssRule by cssclass()
        val hijrahDayCell: CssRule by cssclass()
        val hoverSquare: CssRule by cssclass()
        val htmlEditor: CssRule by cssclass()
        val htmlEditorAlignCenter: CssRule by cssclass()
        val htmlEditorAlignJustify: CssRule by cssclass()
        val htmlEditorAlignLeft: CssRule by cssclass()
        val htmlEditorAlignRight: CssRule by cssclass()
        val htmlEditorBackground: CssRule by cssclass()
        val htmlEditorBold: CssRule by cssclass()
        val htmlEditorBullets: CssRule by cssclass()
        val htmlEditorCopy: CssRule by cssclass()
        val htmlEditorCut: CssRule by cssclass()
        val htmlEditorForeground: CssRule by cssclass()
        val htmlEditorHr: CssRule by cssclass()
        val htmlEditorIndent: CssRule by cssclass()
        val htmlEditorItalic: CssRule by cssclass()
        val htmlEditorNumbers: CssRule by cssclass()
        val htmlEditorOutdent: CssRule by cssclass()
        val htmlEditorPaste: CssRule by cssclass()
        val htmlEditorStrike: CssRule by cssclass()
        val htmlEditorUnderline: CssRule by cssclass()
        val hyperlink: CssRule by cssclass()
        val imageView: CssRule by cssclass()
        val incrementArrow: CssRule by cssclass()
        val incrementArrowButton: CssRule by cssclass()
        val incrementButton: CssRule by cssclass()
        val indexedCell: CssRule by cssclass()
        val indicator: CssRule by cssclass()
        val information: CssRule by cssclass()
        val label: CssRule by cssclass()
        val leftArrow: CssRule by cssclass()
        val leftArrowButton: CssRule by cssclass()
        val leftContainer: CssRule by cssclass()
        val leftPill: CssRule by cssclass()
        val less: CssRule by cssclass()
        val line: CssRule by cssclass()
        val listCell: CssRule by cssclass()
        val listView: CssRule by cssclass()
        val mark: CssRule by cssclass()
        val mediaView: CssRule by cssclass()
        val menu: CssRule by cssclass()
        val menuBar: CssRule by cssclass()
        val menuButton: CssRule by cssclass()
        val menuDownArrow: CssRule by cssclass()
        val menuItem: CssRule by cssclass()
        val menuUpArrow: CssRule by cssclass()
        val mnemonicUnderline: CssRule by cssclass()
        val monthYearPane: CssRule by cssclass()
        val more: CssRule by cssclass()
        val negative: CssRule by cssclass()
        val nestedColumnHeader: CssRule by cssclass()
        val nextMonth: CssRule by cssclass()
        val numberButton: CssRule by cssclass()
        val openButton: CssRule by cssclass()
        val page: CssRule by cssclass()
        val pageInformation: CssRule by cssclass()
        val pagination: CssRule by cssclass()
        val paginationControl: CssRule by cssclass()
        val passwordField: CssRule by cssclass()
        val percentage: CssRule by cssclass()
        val pickerColor: CssRule by cssclass()
        val pickerColorRect: CssRule by cssclass()
        val pieLegendSymbol: CssRule by cssclass()
        val placeholder: CssRule by cssclass()
        val popup: CssRule by cssclass()
        val previousMonth: CssRule by cssclass()
        val progress: CssRule by cssclass()
        val progressBar: CssRule by cssclass()
        val progressBarTableCell: CssRule by cssclass()
        val progressIndicator: CssRule by cssclass()
        val radio: CssRule by cssclass()
        val radioButton: CssRule by cssclass()
        val rightArrow: CssRule by cssclass()
        val rightClass: CssRule by cssclass("right")
        val rightArrowButton: CssRule by cssclass()
        val rightContainer: CssRule by cssclass()
        val rightPill: CssRule by cssclass()
        val root: CssRule by cssclass()
        val scrollArrow: CssRule by cssclass()
        val scrollBar: CssRule by cssclass()
        val scrollPane: CssRule by cssclass()
        val secondaryLabel: CssRule by cssclass()
        val secondaryText: CssRule by cssclass()
        val segment: CssRule by cssclass()
        val segment0: CssRule by cssclass()
        val segment1: CssRule by cssclass()
        val segment2: CssRule by cssclass()
        val segment3: CssRule by cssclass()
        val segment4: CssRule by cssclass()
        val segment5: CssRule by cssclass()
        val segment6: CssRule by cssclass()
        val segment7: CssRule by cssclass()
        val segment8: CssRule by cssclass()
        val segment9: CssRule by cssclass()
        val segment10: CssRule by cssclass()
        val segment11: CssRule by cssclass()
        val selectedClass: CssRule by cssclass("selected")
        val separator: CssRule by cssclass()
        val settingsLabel: CssRule by cssclass()
        val settingsUnit: CssRule by cssclass()
        val sheet: CssRule by cssclass()
        val showHideColumnImage: CssRule by cssclass()
        val showHideColumnsButton: CssRule by cssclass()
        val slider: CssRule by cssclass()
        val sortOrder: CssRule by cssclass()
        val sortOrderDot: CssRule by cssclass()
        val sortOrderDotsContainer: CssRule by cssclass()
        val spinner: CssRule by cssclass()
        val splitArrowsHorizontal: CssRule by cssclass()
        val splitArrowsOnLeftHorizontal: CssRule by cssclass()
        val splitArrowsOnRightHorizontal: CssRule by cssclass()
        val splitArrowsOnLeftVertical: CssRule by cssclass()
        val splitArrowsOnRightVertical: CssRule by cssclass()
        val splitArrowsVertical: CssRule by cssclass()
        val splitButton: CssRule by cssclass()
        val splitMenuButton: CssRule by cssclass()
        val splitPane: CssRule by cssclass()
        val splitPaneDivider: CssRule by cssclass()
        val stackedBarChart: CssRule by cssclass()
        val tab: CssRule by cssclass()
        val tabCloseButton: CssRule by cssclass()
        val tabContentArea: CssRule by cssclass()
        val tabDownButton: CssRule by cssclass()
        val tabHeaderArea: CssRule by cssclass()
        val tabHeaderBackground: CssRule by cssclass()
        val tabLabel: CssRule by cssclass()
        val tableCell: CssRule by cssclass()
        val tableRowCell: CssRule by cssclass()
        val tableView: CssRule by cssclass()
        val tableColumn: CssRule by cssclass()
        val tabPane: CssRule by cssclass()
        val text: CssRule by cssclass()
        val textArea: CssRule by cssclass()
        val textField: CssRule by cssclass()
        val textInput: CssRule by cssclass()
        val textInputDialog: CssRule by cssclass()
        val thumb: CssRule by cssclass()
        val tick: CssRule by cssclass()
        val title: CssRule by cssclass()
        val titledPane: CssRule by cssclass()
        val today: CssRule by cssclass()
        val toggleButton: CssRule by cssclass()
        val toolBar: CssRule by cssclass()
        val toolBarOverflowButton: CssRule by cssclass()
        val tooltip: CssRule by cssclass()
        val track: CssRule by cssclass()
        val transparentPattern: CssRule by cssclass()
        val treeCell: CssRule by cssclass()
        val treeDisclosureNode: CssRule by cssclass()
        val treeTableCell: CssRule by cssclass()
        val treeTableRowCell: CssRule by cssclass()
        val treeTableView: CssRule by cssclass()
        val treeView: CssRule by cssclass()
        val viewport: CssRule by cssclass()
        val virtualFlow: CssRule by cssclass()
        val warning: CssRule by cssclass()
        val webcolorField: CssRule by cssclass()  // color lowercase intentionally (that's how it is in modena)
        val webField: CssRule by cssclass()
        val webView: CssRule by cssclass()
        val weekNumberCell: CssRule by cssclass()

        // Style classes used by Form Builder
        val form: CssRule by cssclass()
        val fieldset: CssRule by cssclass()
        val legend: CssRule by cssclass()
        val field: CssRule by cssclass()
        val labelContainer: CssRule by cssclass()
        val inputContainer: CssRule by cssclass()

        // DataGrid
        val datagrid: CssRule by cssclass()
        val datagridCell: CssRule by cssclass()
        val datagridRow: CssRule by cssclass()

        // Keyboard
        val keyboard: CssRule by cssclass()
        val keyboardKey: CssRule by cssclass()
        val keyboardSpacerKey: CssRule by cssclass()

        // Pseudo classes used by JavaFX
        val armed: CssRule by csspseudoclass()
        val bottom: CssRule by csspseudoclass()
        val cancel: CssRule by csspseudoclass()
        val cellSelection: CssRule by csspseudoclass()
        val checked: CssRule by csspseudoclass()
        val constrainedResize: CssRule by csspseudoclass()
        val containsFocus: CssRule by csspseudoclass()
        val collapsed: CssRule by csspseudoclass()
        val default: CssRule by csspseudoclass()
        val determinate: CssRule by csspseudoclass()
        val disabled: CssRule by csspseudoclass()
        val editable: CssRule by csspseudoclass()
        val empty: CssRule by csspseudoclass()
        val even: CssRule by csspseudoclass()
        val expanded: CssRule by csspseudoclass()
        val filled: CssRule by csspseudoclass()
        val fitToHeight: CssRule by csspseudoclass(snakeCase = false)
        val fitToWidth: CssRule by csspseudoclass(snakeCase = false)
        val focused: CssRule by csspseudoclass()
        val header: CssRule by csspseudoclass()
        val horizontal: CssRule by csspseudoclass()
        val hover: CssRule by csspseudoclass()
        val indeterminate: CssRule by csspseudoclass()
        val lastVisible: CssRule by csspseudoclass()
        val left: CssRule by csspseudoclass()
        val noHeader: CssRule by csspseudoclass()
        val odd: CssRule by csspseudoclass()
        val openvertically: CssRule by csspseudoclass()  // intentionally single word
        val pannable: CssRule by csspseudoclass()
        val pressed: CssRule by csspseudoclass()
        val readonly: CssRule by csspseudoclass()
        val right: CssRule by csspseudoclass()
        val rowSelection: CssRule by csspseudoclass()
        val selected: CssRule by csspseudoclass()
        val showing: CssRule by csspseudoclass()
        val showMnemonics: CssRule by csspseudoclass()
        val top: CssRule by csspseudoclass()
        val vertical: CssRule by csspseudoclass()
        val visited: CssRule by csspseudoclass()

        init {
            if (!FX.osgiAvailable) detectAndInstallUrlHandler()
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
         *
         * If an OSGi environment is detected, the TornadoFX OSGi Activator will install the OSGi
         * compatible URL handler instead.
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

    val selections: MutableList<CssSelection> = mutableListOf()

    override fun addSelection(selection: CssSelection) {
        selections += selection
    }

    override fun removeSelection(selection: CssSelection) {
        selections -= selection
    }

    override fun render(): String = imports.joinToString(separator = "\n", postfix = "\n") { "@import url(css://${it.java.name})" } +
            selections.joinToString(separator = "") { it.render() }

    val base64URL: URL get() = URL("css://${Base64.getEncoder().encodeToString(render().toByteArray())}:64")
    val externalForm: String get() = base64URL.toExternalForm()
}

open class PropertyHolder {
    companion object {
        val selectionScope: ThreadLocal<CssSelectionBlock> = ThreadLocal()

        fun Double.pos(relative: Boolean): String = if (relative) "${fiveDigits.format(this * 100)}%" else "${fiveDigits.format(this)}px"

        fun <T> toCss(value: T): String = when (value) {
            null -> ""  // This should only happen in a container (such as box(), Array<>(), Pair())
            is MultiValue<*> -> value.elements.joinToString { toCss(it) }
            is CssBox<*> -> "${toCss(value.top)} ${toCss(value.right)} ${toCss(value.bottom)} ${toCss(value.left)}"
            is FontWeight -> "${value.weight}"  // Needs to come before `is Enum<*>`
            is Enum<*> -> value.toString().toLowerCase().replace("_", "-")
            is Font -> "${if (value.style == "Regular") "normal" else value.style} ${value.size}pt ${toCss(value.family)}"
            is Cursor -> {
                if (value is ImageCursor)
                    value.image.javaClass.getDeclaredField("url").apply { isAccessible = true }.let { it.get(value.image).toString() }
                else
                    value.toString()
            }
            is URI -> "url(\"${value.toASCIIString()}\")"
            is BackgroundPosition -> "${value.horizontalSide} ${value.horizontalPosition.pos(value.isHorizontalAsPercentage)} " +
                    "${value.verticalSide} ${value.verticalPosition.pos(value.isVerticalAsPercentage)}"
            is BackgroundSize -> when {
                value.isContain -> "contain"
                value.isCover -> "cover"
                else -> buildString {
                    append(if (value.width == BackgroundSize.AUTO) "auto" else value.width.pos(value.isWidthAsPercentage))
                    append(' ')
                    append(if (value.height == BackgroundSize.AUTO) "auto" else value.height.pos(value.isHeightAsPercentage))
                }
            }
            is BorderStrokeStyle -> when (value) {
                BorderStrokeStyle.NONE -> "none"
                BorderStrokeStyle.DASHED -> "dashed"
                BorderStrokeStyle.DOTTED -> "dotted"
                BorderStrokeStyle.SOLID -> "solid"
                else -> buildString {
                    append("segments(${value.dashArray.joinToString()}) ")
                    append(toCss(value.type))
                    append(" line-join ${toCss(value.lineJoin)} ")
                    if (value.lineJoin == StrokeLineJoin.MITER) {
                        append(value.miterLimit)
                    }
                    append(" line-cap ${toCss(value.lineCap)}")
                }
            }
            is BorderImageSlice -> toCss(value.widths) + if (value.filled) " fill" else ""
            is List<*> -> value.joinToString(" ") { toCss(it) }
            is Pair<*, *> -> "${toCss(value.first)} ${toCss(value.second)}"
            is KClass<*> -> "\"${value.qualifiedName}\""
            is CssProperty<*> -> value.name
            is Raw -> value.name
            is String -> "\"$value\""
            is Effect -> when (value) { // JavaFX currently only supports DropShadow and InnerShadow in CSS
                is DropShadow -> "dropshadow(${toCss(value.blurType)}, ${value.color.css}, ${value.radius}, ${value.spread}, ${value.offsetX}, ${value.offsetY})"
                is InnerShadow -> "innershadow(${toCss(value.blurType)}, ${value.color.css}, ${value.radius}, ${value.choke}, ${value.offsetX}, ${value.offsetY})"
                else -> "none"
            }
            is Color -> value.css
            is Paint -> value.toString().replace(Regex("0x[0-9a-f]{8}")) { Color.web(it.groupValues[0]).css }
            else -> value.toString()
        }
    }

    val properties: MutableMap<String, Pair<Any, ((Any) -> String)?>> = mutableMapOf()
    val unsafeProperties: MutableMap<String, Any> = mutableMapOf()
    val mergedProperties: Map<String, Pair<Any, ((Any) -> String)?>> get() = properties + unsafeProperties.mapValues { it.value to null }

    // Root
    var baseColor: Color by cssprop("-fx-base")
    var accentColor: Color by cssprop("-fx-accent")
    var focusColor: Paint by cssprop("-fx-focus-color")
    var faintFocusColor: Paint by cssprop("-fx-faint-focus-color")
    var selectionBarText: Paint by cssprop("-fx-selection-bar-text")

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
    var borderInsets: MultiValue<CssBox<Dimension<Dimension.LinearUnits>>> by cssprop("-fx-border-insets")
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
    var strokeDashArray: List<Dimension<Dimension.LinearUnits>> by cssprop("-fx-stroke-dash-array")
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
    var maxCellsInRow: Int by cssprop("-fx-max-cells-in-row")

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
    var barFill: Color by cssprop("-fx-bar-fill")

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

    infix fun CssProperty<*>.force(value: Any): Unit = unsafe(this, value)
    infix fun String.force(value: Any): Unit = unsafe(this, value)

    fun unsafe(key: CssProperty<*>, value: Any): Unit = unsafe(key.name, value)
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
        val name: String = name.camelToSnake()
        var value: T
            get() {
                val props = selectionScope.get().properties

                if (!props.containsKey(name) && multiValue)
                    props[name] = MultiValue<T>() to renderer as ((Any) -> String)?

                return selectionScope.get().properties[name]?.first as T
            }
            set(value) {
                selectionScope.get().properties[name] = value as Any to renderer as ((Any) -> String)?
            }
    }

    infix fun <T : Any> CssProperty<T>.set(value: T): Unit = setProperty(this, value)
    fun <T : Any> setProperty(property: CssProperty<T>, value: T) {
        properties[property.name] = value to properties[property.name]?.second
    }

    class Raw(val name: String)

    fun raw(name: String): Raw = Raw(name)
}

class CssSelection(val selector: CssSelector, op: CssSelectionBlock.() -> Unit) : Rendered {
    val block: CssSelectionBlock = CssSelectionBlock(op)

    override fun render(): String = render(emptyList(), CssSubRule.Relation.DESCENDANT)
    fun render(parents: List<String>, relation: CssSubRule.Relation): String = buildString {
        val ruleStrings = selector.strings(parents, relation)
        block.mergedProperties.let {
            // TODO: Handle custom renderer
            if (it.isNotEmpty()) {
                append("${ruleStrings.joinToString()} {\n")
                it.forEach { (name, value) -> append("    $name: ${value.second?.invoke(value.first) ?: PropertyHolder.toCss(value.first)};\n") }
                append("}\n")
            }
        }
        block.selections.forEach { (sel, rel) -> append(sel.render(ruleStrings, rel)) }
    }
}

class CssSelector(vararg val rule: CssRuleSet) : Selectable {

    override fun toSelection(): CssSelector = this
    fun strings(parents: List<String>, relation: CssSubRule.Relation): List<String> = rule.mapEach { render() }.cartesian(parents, relation)
    fun simpleRender(): String = rule.joinToString { it.render() }

    companion object {
        fun String.merge(other: String, relation: CssSubRule.Relation): String = "$this${relation.render()}$other"
        fun List<String>.cartesian(parents: List<String>, relation: CssSubRule.Relation): List<String> =
            takeIf { parents.isEmpty() } ?: parents.asSequence().flatMap { parent -> asSequence().map { child -> parent.merge(child, relation) } }.toList()
    }
}

class CssSelectionBlock(op: CssSelectionBlock.() -> Unit) : PropertyHolder(), SelectionHolder {
    val log: Logger = Logger.getLogger("ErrorHandler")
    val selections: MutableMap<CssSelection, CssSubRule.Relation> = mutableMapOf()

    init {
        val currentScope = PropertyHolder.selectionScope.get()
        PropertyHolder.selectionScope.set(this)
        try {
            op()
        } catch (e: Exception) {
            // the CSS rule caused an error, do not let the error propagate to
            // avoid an infinite loop in the error handler
            log.log(Level.WARNING, "CSS rule caused an error", e)
        }
        PropertyHolder.selectionScope.set(currentScope)
    }

    override fun addSelection(selection: CssSelection) {
        selections[selection] = CssSubRule.Relation.DESCENDANT
    }

    override fun removeSelection(selection: CssSelection) {
        selections.remove(selection)
    }

    operator fun CssSelectionBlock.unaryPlus() {
        this@CssSelectionBlock.mix(this)
    }

    @Deprecated("Use and() instead as it's clearer", ReplaceWith("and(selector, op)"))
    fun add(selector: String, op: CssSelectionBlock.() -> Unit): CssSelection = and(selector, op)

    @Deprecated("Use and() instead as it's clearer", ReplaceWith("and(selector, *selectors, op = op)"))
    fun add(selector: Selectable, vararg selectors: Selectable, op: CssSelectionBlock.() -> Unit): CssSelection = and(selector, *selectors, op = op)


    /** [CssSubRule.Relation.REFINE] */
    fun and(selector: String, op: CssSelectionBlock.() -> Unit): CssSelection = and(selector.toSelector(), op = op)

    /** [CssSubRule.Relation.REFINE] */
    fun and(selector: Selectable, vararg selectors: Selectable, op: CssSelectionBlock.() -> Unit): CssSelection =
        addRelation(CssSubRule.Relation.REFINE, selector, *selectors, op = op)


    /** [CssSubRule.Relation.CHILD] */
    fun child(selector: String, op: CssSelectionBlock.() -> Unit): CssSelection = child(selector.toSelector(), op = op)

    /** [CssSubRule.Relation.CHILD] */
    fun child(selector: Selectable, vararg selectors: Selectable, op: CssSelectionBlock.() -> Unit): CssSelection =
        addRelation(CssSubRule.Relation.CHILD, selector, *selectors, op = op)


    /** [CssSubRule.Relation.DESCENDANT] */
    fun contains(selector: String, op: CssSelectionBlock.() -> Unit): CssSelection = contains(selector.toSelector(), op = op)

    /** [CssSubRule.Relation.DESCENDANT] */
    fun contains(selector: Selectable, vararg selectors: Selectable, op: CssSelectionBlock.() -> Unit): CssSelection =
        addRelation(CssSubRule.Relation.DESCENDANT, selector, *selectors, op = op)


    /** [CssSubRule.Relation.ADJACENT] */
    fun next(selector: String, op: CssSelectionBlock.() -> Unit): CssSelection = next(selector.toSelector(), op = op)

    /** [CssSubRule.Relation.ADJACENT] */
    fun next(selector: Selectable, vararg selectors: Selectable, op: CssSelectionBlock.() -> Unit): CssSelection =
        addRelation(CssSubRule.Relation.ADJACENT, selector, *selectors, op = op)


    /** [CssSubRule.Relation.SIBLING] */
    fun sibling(selector: String, op: CssSelectionBlock.() -> Unit): CssSelection = sibling(selector.toSelector(), op = op)

    /** [CssSubRule.Relation.SIBLING] */
    fun sibling(selector: Selectable, vararg selectors: Selectable, op: CssSelectionBlock.() -> Unit): CssSelection =
        addRelation(CssSubRule.Relation.SIBLING, selector, *selectors, op = op)


    private fun addRelation(
        relation: CssSubRule.Relation,
        selector: Selectable, vararg selectors: Selectable,
        op: CssSelectionBlock.() -> Unit
    ): CssSelection {
        val s = select(selector, *selectors, op = op)
        selections[s] = relation
        return s
    }


    @Suppress("UNCHECKED_CAST")
    fun mix(mixin: CssSelectionBlock) {
        mixin.properties.forEach { key, value ->
            (properties[key]?.first as? MultiValue<Any>)?.addAll(value.first as MultiValue<Any>) ?: run { properties[key] = value }
        }
        mixin.unsafeProperties.forEach { key, value ->
            (unsafeProperties[key] as? MultiValue<Any>)?.addAll(value as MultiValue<Any>) ?: run { unsafeProperties[key] = value }
        }
        selections.putAll(mixin.selections)
    }
}

fun mixin(op: CssSelectionBlock.() -> Unit): CssSelectionBlock = CssSelectionBlock(op)

class CssRuleSet(val rootRule: CssRule, vararg val subRule: CssSubRule) : Selectable, Scoped, Rendered {

    override fun render(): String = buildString {
        append(rootRule.render())
        subRule.forEach { append(it.render()) }
    }

    override fun toRuleSet(): CssRuleSet = this
    override fun toSelection(): CssSelector = CssSelector(this)
    override fun append(rule: CssSubRule): CssRuleSet = CssRuleSet(rootRule, *subRule, rule)
    fun append(ruleSet: CssRuleSet, relation: CssSubRule.Relation): CssRuleSet =
        CssRuleSet(rootRule, *subRule, CssSubRule(ruleSet.rootRule, relation), *ruleSet.subRule)
}

class CssRule(val prefix: String, name: String, snakeCase: Boolean = true) : Selectable, Scoped, Rendered {

    val name: String = if (snakeCase) name.camelToSnake() else name

    override fun render(): String = "$prefix$name"
    override fun toRuleSet(): CssRuleSet = CssRuleSet(this)
    override fun toSelection(): CssSelector = toRuleSet().toSelection()
    override fun append(rule: CssSubRule): CssRuleSet = CssRuleSet(this, rule)

    companion object {
        fun elem(value: String, snakeCase: Boolean = true): CssRule = CssRule("", value.cssValidate(), snakeCase)
        fun id(value: String, snakeCase: Boolean = true): CssRule = CssRule("#", value.cssValidate(), snakeCase)
        fun c(value: String, snakeCase: Boolean = true): CssRule = CssRule(".", value.cssValidate(), snakeCase)
        fun pc(value: String, snakeCase: Boolean = true): CssRule = CssRule(":", value.cssValidate(), snakeCase)

        private const val name = "\\*|-?[_a-zA-Z][_a-zA-Z0-9-]*"  // According to http://stackoverflow.com/a/449000/2094298
        private const val prefix = "[.#:]?"
        private const val relation = "[ >+~]?"
        private const val subRule = "\\s*?($relation)\\s*($prefix)($name)"

        val nameRegex: Regex = Regex(name)
        val subRuleRegex: Regex = Regex(subRule)
        val ruleSetRegex: Regex = Regex("($subRule)+\\s*")
        val splitter: Regex = Regex("\\s*,\\s*")
        val upperCaseRegex: Regex = Regex("([A-Z])")
    }
}

class CssSubRule(val rule: CssRule, val relation: Relation) : Rendered {

    override fun render(): String = "${relation.render()}${rule.render()}"

    enum class Relation(val symbol: String) : Rendered {
        REFINE(""),
        CHILD(" > "),
        DESCENDANT(" "),
        ADJACENT(" + "),
        SIBLING(" ~ ");

        override fun render(): String = symbol

        companion object {
            fun of(symbol: String): Relation = when (symbol) {
                "" -> REFINE
                ">" -> CHILD
                " " -> DESCENDANT
                "+" -> ADJACENT
                "~" -> SIBLING
                else -> throw IllegalArgumentException("Invalid Css Relation: $symbol")
            }
        }
    }
}


// ================================================================
// Inline CSS

class InlineCss : PropertyHolder(), Rendered {
    // TODO: Handle custom renderer
    override fun render(): String = mergedProperties.entries.joinToString(separator = "") { (key, value) ->
        " $key: ${value.second?.invoke(value.first) ?: toCss(value.first)};"
    }
}

fun Iterable<Node>.style(append: Boolean = false, op: InlineCss.() -> Unit): Unit = withEach { style(append, op) }

fun Styleable.style(append: Boolean = false, op: InlineCss.() -> Unit) {

    val setStyleMethod = this.javaClass.methods.firstOrNull { method ->
        method.name == "setStyle" && method.returnType == Void.TYPE && method.parameterCount == 1 && method.parameters[0].type == String::class.java
    }

    setStyleMethod ?: throw IllegalArgumentException("Don't know how to set style for Styleable subclass $javaClass")

    val block = InlineCss().apply(op)

    val newStyle = if (append && style.isNotBlank()) style + block.render() else block.render().trim()

    try {
        // in Java 9 setStyleMethod.canAccess(this) can be used for checking instead of wrapping this invocation in a try-catch clause
        setStyleMethod.invoke(this, newStyle)
    } catch (exception: Exception) {
        when (exception) {
            is IllegalAccessException, is IllegalArgumentException -> FX.log.warning("Cannot access $javaClass.setStyle(...) through reflection due to insufficient priviledge.")
            else -> FX.log.warning("Invocation of $javaClass.setStyle(...) through reflection failed.")
        }
        throw exception
    }
}


// ================================================================
// Delegates

fun csselement(value: String? = null, snakeCase: Boolean = value == null): CssElementDelegate = CssElementDelegate(value, snakeCase)
fun cssid(value: String? = null, snakeCase: Boolean = value == null): CssIdDelegate = CssIdDelegate(value, snakeCase)
fun cssclass(value: String? = null, snakeCase: Boolean = value == null): CssClassDelegate = CssClassDelegate(value, snakeCase)
fun csspseudoclass(value: String? = null, snakeCase: Boolean = value == null): CssPseudoClassDelegate = CssPseudoClassDelegate(value, snakeCase)
inline fun <reified T : Any> cssproperty(value: String? = null, noinline renderer: ((T) -> String)? = null): CssPropertyDelegate<T> =
    CssPropertyDelegate(value, MultiValue::class.java.isAssignableFrom(T::class.java), renderer)

class CssElementDelegate(val name: String?, val snakeCase: Boolean = name == null) : ReadOnlyProperty<Any, CssRule> {
    override fun getValue(thisRef: Any, property: KProperty<*>): CssRule = CssRule.elem(name ?: property.name, snakeCase)
}

class CssIdDelegate(val name: String?, val snakeCase: Boolean = name == null) : ReadOnlyProperty<Any, CssRule> {
    override fun getValue(thisRef: Any, property: KProperty<*>): CssRule = CssRule.id(name ?: property.name, snakeCase)
}

class CssClassDelegate(val name: String?, val snakeCase: Boolean = name == null) : ReadOnlyProperty<Any, CssRule> {
    override fun getValue(thisRef: Any, property: KProperty<*>): CssRule = CssRule.c(name ?: property.name, snakeCase)
}

class CssPseudoClassDelegate(val name: String?, val snakeCase: Boolean = name == null) : ReadOnlyProperty<Any, CssRule> {
    override fun getValue(thisRef: Any, property: KProperty<*>): CssRule = CssRule.pc(name ?: property.name, snakeCase)
}

class CssPropertyDelegate<T : Any>(val name: String?, val multiValue: Boolean, val renderer: ((T) -> String)? = null) : ReadOnlyProperty<Any, CssProperty<T>> {
    override fun getValue(thisRef: Any, property: KProperty<*>): CssProperty<T> = CssProperty(name ?: property.name, multiValue, renderer)
}


// ================================================================
// Dimensions

open class Dimension<T : Enum<T>>(val value: Double, val units: T) {
    operator fun unaryPlus(): Dimension<T> = this
    operator fun unaryMinus(): Dimension<T> = Dimension(-value, units)
    operator fun plus(value: Number): Dimension<T> = Dimension(this.value + value.toDouble(), units)
    operator fun plus(value: Dimension<T>): Dimension<T> = safeMath(value, Double::plus)
    operator fun minus(value: Number): Dimension<T> = Dimension(this.value - value.toDouble(), units)
    operator fun minus(value: Dimension<T>): Dimension<T> = safeMath(value, Double::minus)
    operator fun times(value: Number): Dimension<T> = Dimension(this.value * value.toDouble(), units)
    operator fun div(value: Number): Dimension<T> = Dimension(this.value / value.toDouble(), units)
    operator fun rem(value: Number): Dimension<T> = Dimension(this.value % value.toDouble(), units)

    private inline fun safeMath(value: Dimension<T>, crossinline op: (Double, Double) -> Double): Dimension<T> {
        require(units == value.units) { "Cannot combine $this and $value: The units do not match" }
        return Dimension(op(this.value, value.value), units)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Dimension<*>) return false

        if (value != other.value) return false
        if (units != other.units) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + units.hashCode()
        return result
    }

    override fun toString(): String = when (value) {
        Double.POSITIVE_INFINITY, Double.MAX_VALUE -> "infinity"
        Double.NEGATIVE_INFINITY, Double.MIN_VALUE -> "-infinity"
        Double.NaN -> "0$units"
        else -> "${fiveDigits.format(value)}$units"
    }

    @Suppress("EnumEntryName")
    enum class AngularUnits { deg, rad, grad, turn }

    @Suppress("EnumEntryName")
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

        override fun toString(): String = value
    }
}

operator fun <T : Enum<T>> Number.plus(value: Dimension<T>): Dimension<T> = value + this
operator fun <T : Enum<T>> Number.minus(value: Dimension<T>): Dimension<T> = -value + this
operator fun <T : Enum<T>> Number.times(value: Dimension<T>): Dimension<T> = value * this

val infinity: Dimension<Dimension.LinearUnits> = Dimension(Double.POSITIVE_INFINITY, Dimension.LinearUnits.px)

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


// ================================================================
// Enums

enum class FXVisibility { VISIBLE, HIDDEN, COLLAPSE, INHERIT; }
enum class FXTabAnimation { GROW, NONE; }


// ================================================================
// Misc

val fiveDigits: DecimalFormat = DecimalFormat("#.#####", DecimalFormatSymbols.getInstance(Locale.ENGLISH))

val Color.css: String get() = "rgba(${(red * 255).toInt()}, ${(green * 255).toInt()}, ${(blue * 255).toInt()}, ${fiveDigits.format(opacity)})"

internal fun String.camelToSnake() = (get(0).toLowerCase() + substring(1)).replace(CssRule.upperCaseRegex, "-$1").toLowerCase()
internal fun String.cssValidate() = if (matches(CssRule.nameRegex)) this else throw IllegalArgumentException("Invalid CSS Name: $this")
internal fun String.toSelector() = CssSelector(*split(CssRule.splitter).map(String::toRuleSet).toTypedArray())
internal fun String.toRuleSet() = if (matches(CssRule.ruleSetRegex)) {
    val rules = CssRule.subRuleRegex.findAll(this)
        .mapEach { CssSubRule(CssRule(groupValues[2], groupValues[3], false), CssSubRule.Relation.of(groupValues[1])) }
        .toList()
    CssRuleSet(rules[0].rule, *rules.drop(1).toTypedArray())
} else throw IllegalArgumentException("Invalid CSS Rule Set: $this")

fun loadFont(path: String, size: Number): Font? {
    return MethodHandles.lookup().lookupClass().getResourceAsStream(path)?.use { Font.loadFont(it, size.toDouble()) }
}


// ================================================================
// Style Class

/**
 * Check if this Styleable has the given type safe css class.
 * Pseudo classes are also supported.
 */
fun Styleable.hasClass(cssClass: CssRule): Boolean = if (cssClass.prefix == ":") hasPseudoClass(cssClass.name) else hasClass(cssClass.name)


/**
 * Add one or more type safe css classes to this Styleable.
 * Pseudo classes are also supported.
 */
fun <T : Styleable> T.addClass(vararg cssClass: CssRule): T = apply { cssClass.withEach { if (prefix == ":") addPseudoClass(name) else addClass(name) } }


/**
 * Remove the given given type safe css class(es) from this Styleable.
 * Pseudo classes are also supported.
 */
fun <T : Styleable> T.removeClass(vararg cssClass: CssRule): T =
    apply { cssClass.withEach { if (prefix == ":") removePseudoClass(name) else removeClass(name) } }


/**
 * Toggle the given type safe css class based on the given predicate on this Styleable.
 * Pseudo classes are also supported.
 */
fun <T : Styleable> T.toggleClass(cssClass: CssRule, predicate: Boolean): T =
    if (cssClass.prefix == ":") togglePseudoClass(cssClass.name, predicate) else toggleClass(cssClass.name, predicate)


/**
 * Toggle the given type safe css class based on the given predicate observable value.
 * Whenever the observable value changes, the class is added or removed.
 * Pseudo classes are also supported.
 */
fun <T : Styleable> T.toggleClass(cssClass: CssRule, observablePredicate: ObservableValue<Boolean>) {
    toggleClass(cssClass, observablePredicate.value ?: false)
    observablePredicate.onChange { toggleClass(cssClass, it ?: false) }
}


/**
 * Add the given type safe css classes to every Styleable in this Iterable.
 * Pseudo classes are also supported.
 */
fun Iterable<Styleable>.addClass(vararg cssClass: CssRule): Unit = forEach { node -> cssClass.forEach { node.addClass(it) } }


/**
 * Remove the given type safe css classes from every Styleable in this Iterable.
 * Pseudo classes are also supported.
 */
fun Iterable<Styleable>.removeClass(vararg cssClass: CssRule): Unit = forEach { node -> cssClass.forEach { node.removeClass(it) } }


/**
 * Toggle the given type safe css class on every Styleable in this Iterable based on the given predicate.
 * Pseudo classes are also supported.
 */
fun Iterable<Styleable>.toggleClass(cssClass: CssRule, predicate: Boolean): Unit = withEach { toggleClass(cssClass, predicate) }


/**
 * Toggle the given type safe css class on every Styleable in this Iterable based on the given predicate observable value.
 * Whenever the observable value changes, the class is added or removed.
 * Pseudo classes are also supported.
 */
fun Iterable<Styleable>.toggleClass(cssClass: CssRule, observablePredicate: ObservableValue<Boolean>): Unit =
    withEach { toggleClass(cssClass, observablePredicate) }


/**
 * Bind this observable type safe css rule to this Node.
 * Pseudo classes are also supported.
 */
fun Node.bindClass(value: ObservableValue<CssRule>): ObservableStyleClass = ObservableStyleClass(this, value)

class ObservableStyleClass(node: Node, val value: ObservableValue<CssRule>) {
    val listener: ChangeListener<CssRule>

    init {
        fun checkAdd(newValue: CssRule?) {
            if (newValue != null && !node.hasClass(newValue)) node.addClass(newValue)
        }
        listener = ChangeListener { _, oldValue, newValue ->
            if (oldValue != null) node.removeClass(oldValue)
            checkAdd(newValue)
        }
        checkAdd(value.value)
        value.addListener(listener)
    }

    fun dispose(): Unit = value.removeListener(listener)
}


@Suppress("UNCHECKED_CAST")
fun <T : Node> Node.select(selector: Selectable): T = lookup(selector.toSelection().simpleRender()) as T

@Suppress("UNCHECKED_CAST")
fun <T : Node> Node.selectAll(selector: Selectable): List<T> = (lookupAll(selector.toSelection().simpleRender()) as Set<T>).toList()

fun <T : Node> T.setId(cssId: CssRule): T = apply { id = cssId.name }


// ================================================================
// Containers

fun <T> box(all: T): CssBox<T> = CssBox(all, all, all, all)
fun <T> box(vertical: T, horizontal: T): CssBox<T> = CssBox(vertical, horizontal, vertical, horizontal)
fun <T> box(top: T, right: T, bottom: T, left: T): CssBox<T> = CssBox(top, right, bottom, left)
data class CssBox<out T>(val top: T, val right: T, val bottom: T, val left: T)

fun <T> multi(vararg elements: T): MultiValue<T> = MultiValue(elements)
class MultiValue<T>(initialElements: Array<out T>? = null) {
    val elements: MutableList<T> = mutableListOf()

    init {
        if (initialElements != null) elements.addAll(initialElements)
    }

    operator fun plusAssign(element: T) {
        elements.add(element)
    }

    fun add(element: T): Boolean = elements.add(element)
    fun add(multivalue: MultiValue<T>): Boolean = addAll(multivalue.elements)
    fun addAll(list: Iterable<T>): Boolean = elements.addAll(list)
    fun addAll(vararg element: T): Boolean = elements.addAll(element)
}


// ================================================================
// Colors

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

fun Color.derive(ratio: Double): Color =
    if (ratio < 0) interpolate(Color(0.0, 0.0, 0.0, opacity), -ratio)
    else interpolate(Color(1.0, 1.0, 1.0, opacity), ratio)

fun Color.ladder(vararg stops: Stop): Color {
    val offset = brightness
    val nStops = LinearGradient(0.0, 1.0, 0.0, 1.0, false, CycleMethod.NO_CYCLE, *stops).stops
    return if (offset <= 0.0) {
        nStops.first().color
    } else if (offset >= 1.0) {
        nStops.last().color
    } else {
        val left = nStops.last { it.offset <= offset }
        val right = nStops.first { it.offset >= offset }
        if (right.offset == left.offset) left.color else left.color.interpolate(right.color, (offset - left.offset) / (right.offset - left.offset))
    }
}

/** Converts the given Paint to a Background. */
fun Paint.asBackground(radii: CornerRadii = CornerRadii.EMPTY, insets: Insets = Insets.EMPTY): Background =
    Background(BackgroundFill(this, radii, insets))


class BorderImageSlice(val widths: CssBox<Dimension<Dimension.LinearUnits>>, val filled: Boolean = false)

fun Parent.stylesheet(op: Stylesheet.() -> Unit) {
    val stylesheet = Stylesheet().apply(op)
    stylesheets += stylesheet.base64URL.toExternalForm()
}

/** Adds the [stylesheet] to the given parent. */
@Suppress("NOTHING_TO_INLINE")
inline fun <T : Stylesheet> Parent.addStylesheet(stylesheet: KClass<T>): Boolean = this.stylesheets.add("css://${stylesheet.java.name}")
