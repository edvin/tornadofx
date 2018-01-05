@file:Suppress("UNCHECKED_CAST")

package tornadofx

import com.sun.javafx.scene.control.skin.TableColumnHeader
import javafx.animation.Animation
import javafx.animation.PauseTransition
import javafx.application.Platform
import javafx.beans.binding.BooleanBinding
import javafx.beans.property.DoubleProperty
import javafx.beans.property.ListProperty
import javafx.beans.property.Property
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.css.PseudoClass
import javafx.event.EventTarget
import javafx.geometry.*
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.CheckBoxTableCell
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.input.InputEvent
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.util.Callback
import javafx.util.Duration
import javafx.util.StringConverter
import javafx.util.converter.*
import tornadofx.osgi.OSGIConsole
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import java.util.function.UnaryOperator
import kotlin.reflect.KClass
import kotlin.reflect.full.safeCast

fun <S, T> TableColumnBase<S, T>.hasClass(className: String) = styleClass.contains(className)
fun <S, T> TableColumnBase<S, T>.hasClass(className: CssRule) = hasClass(className.name)
fun <S, T> TableColumnBase<S, T>.addClass(className: String): TableColumnBase<S, T> = apply { styleClass.add(className) }
fun <S, T> TableColumnBase<S, T>.addClass(vararg cssClass: CssRule): TableColumnBase<S, T> = apply {
    cssClass.forEach { styleClass.add(it.name) }
}

fun <S, T> TableColumnBase<S, T>.removeClass(vararg cssClass: CssRule): TableColumnBase<S, T> = apply {
    cssClass.forEach { styleClass.remove(it.name) }
}

fun <S, T> TableColumnBase<S, T>.removeClass(className: String): TableColumnBase<S, T> = apply { styleClass.remove(className) }
fun <S, T> TableColumnBase<S, T>.toggleClass(cssClass: CssRule, predicate: Boolean): TableColumnBase<S, T> = apply { toggleClass(cssClass.name, predicate) }
fun <S, T> TableColumnBase<S, T>.toggleClass(className: String, predicate: Boolean): TableColumnBase<S, T> = apply {
    if (predicate) {
        if (!hasClass(className)) styleClass.add(className)
    } else {
        styleClass.remove(className)
    }
}

fun Node.hasClass(className: String) = styleClass.contains(className)
fun Node.hasPseudoClass(className: String) = pseudoClassStates.contains(PseudoClass.getPseudoClass(className))

fun <T : Node> T.addClass(vararg className: String) = apply { styleClass.addAll(className) }
fun Iterable<Node>.addClass(vararg cssClass: String) = forEach { node -> cssClass.forEach { node.addClass(it) } }

fun <T : Node> T.addPseudoClass(className: String) = apply {
    val pseudoClass = PseudoClass.getPseudoClass(className)
    pseudoClassStateChanged(pseudoClass, true)
}

fun <T : Node> T.removePseudoClass(className: String) = apply {
    val pseudoClass = PseudoClass.getPseudoClass(className)
    pseudoClassStateChanged(pseudoClass, false)
}

fun <T : Node> T.removeClass(className: String) = apply { styleClass.remove(className) }

fun <T : Node> T.toggleClass(className: String, predicate: Boolean) = apply {
    if (predicate) {
        if (!hasClass(className)) addClass(className)
    } else {
        removeClass(className)
    }
}

fun <T : Node> T.togglePseudoClass(className: String, predicate: Boolean) = apply {
    if (predicate) {
        if (!hasPseudoClass(className)) addPseudoClass(className)
    } else {
        removePseudoClass(className)
    }
}

fun Node.getToggleGroup(): ToggleGroup? = properties["tornadofx.togglegroup"] as ToggleGroup?

fun Node.tooltip(text: String? = null, graphic: Node? = null, op: Tooltip.() -> Unit = {}): Tooltip {
    val newToolTip = Tooltip(text)
    graphic?.apply { newToolTip.graphic = this }
    newToolTip.op()
    if (this is Control) tooltip = newToolTip else Tooltip.install(this, newToolTip)
    return newToolTip
}

fun Scene.reloadStylesheets() {
    val styles = stylesheets.toMutableList()
    stylesheets.clear()
    styles.toTypedArray().forEachIndexed { i, s ->
        if (s.startsWith("css://")) {
            val b = StringBuilder()
            val queryPairs = mutableListOf<String>()

            if (s.contains("?")) {
                val urlAndQuery = s.split(Regex("\\?"), 2)
                b.append(urlAndQuery[0])
                val query = urlAndQuery[1]

                val pairs = query.split("&")
                pairs.filterNot { it.startsWith("squash=") }.forEach { queryPairs.add(it) }
            } else {
                b.append(s)
            }

            queryPairs.add("squash=${System.currentTimeMillis()}")
            b.append("?").append(queryPairs.joinToString("&"))
            styles[i] = b.toString()
        }
    }
    stylesheets.addAll(styles)
}

internal fun Scene.reloadViews() {
    if (properties["tornadofx.layoutdebugger"] == null) {
        findUIComponents().forEach {
            FX.replaceComponent(it)
        }
    }
}

fun Scene.findUIComponents(): List<UIComponent> {
    val list = ArrayList<UIComponent>()
    root.findUIComponents(list)
    return list
}

/**
 * Aggregate UIComponents under the given parent. Nested UIComponents
 * are not aggregated, but they are removed from the FX.components map
 * so that they would be reloaded when the parent is reloaded.
 *
 * This means that nested UIComponents would loose their state, because
 * the pack/unpack functions will not be called for these views. This should
 * be improved in a future version.
 */
private fun Parent.findUIComponents(list: MutableList<UIComponent>) {
    val uicmp = uiComponent<UIComponent>()
    if (uicmp is UIComponent) {
        list += uicmp
        childrenUnmodifiable.asSequence().filterIsInstance<Parent>().forEach { it.clearViews() }
    } else {
        childrenUnmodifiable.asSequence().filterIsInstance<Parent>().forEach { it.findUIComponents(list) }
    }
}

private fun Parent.clearViews() {
    val uicmp = uiComponent<UIComponent>()
    if (uicmp is View) {
        FX.getComponents(uicmp.scope).remove(uicmp.javaClass.kotlin)
    } else {
        childrenUnmodifiable.asSequence().filterIsInstance<Parent>().forEach(Parent::clearViews)
    }
}

fun Stage.reloadStylesheetsOnFocus() {
    if (properties["tornadofx.reloadStylesheetsListener"] == null) {
        focusedProperty().onChange { focused ->
            if (focused && FX.initialized.value) scene?.reloadStylesheets()
        }
        properties["tornadofx.reloadStylesheetsListener"] = true
    }
}

fun Stage.hookGlobalShortcuts() {
    addEventFilter(KeyEvent.KEY_PRESSED) {
        if (FX.layoutDebuggerShortcut?.match(it) ?: false)
            LayoutDebugger.debug(scene)
        else if (FX.osgiDebuggerShortcut?.match(it) ?: false && FX.osgiAvailable)
            find<OSGIConsole>().openModal(modality = Modality.NONE)
    }
}

fun Stage.reloadViewsOnFocus() {
    if (properties["tornadofx.reloadViewsListener"] == null) {
        focusedProperty().onChange { focused ->
            if (focused && FX.initialized.value) scene?.reloadViews()
        }
        properties["tornadofx.reloadViewsListener"] = true
    }
}

fun Pane.reloadStylesheets() {
    val styles = stylesheets.toMutableList()
    stylesheets.clear()
    stylesheets.addAll(styles)
}

infix fun Node.addTo(pane: EventTarget) = pane.addChildIfPossible(this)

fun Pane.replaceChildren(vararg uiComponents: UIComponent) =
        this.replaceChildren(*(uiComponents.map { it.root }.toTypedArray()))

fun EventTarget.replaceChildren(vararg node: Node) {
    val children = requireNotNull(getChildList()){"This node doesn't have a child list"}
    children.clear()
    children.addAll(node)
}

operator fun EventTarget.plusAssign(node: Node) {
    addChildIfPossible(node)
}

fun Pane.clear() {
    children.clear()
}

fun <T : EventTarget> T.replaceChildren(op: T.() -> Unit) {
    getChildList()?.clear()
    op(this)
}

fun Node.wrapIn(wrapper: Parent) {
    parent?.replaceWith(wrapper)
    wrapper.addChildIfPossible(this)
}

fun EventTarget.add(node: Node) = plusAssign(node)

operator fun EventTarget.plusAssign(view: UIComponent) {
    if (this is UIComponent) {
        root += view
    } else {
        this += view.root
    }
}

var Region.useMaxWidth: Boolean
    get() = maxWidth == Double.MAX_VALUE
    set(value) = if (value) maxWidth = Double.MAX_VALUE else Unit

var Region.useMaxHeight: Boolean
    get() = maxHeight == Double.MAX_VALUE
    set(value) = if (value) maxHeight = Double.MAX_VALUE else Unit

var Region.useMaxSize: Boolean
    get() = maxWidth == Double.MAX_VALUE && maxHeight == Double.MAX_VALUE
    set(value) = if (value) {
        useMaxWidth = true; useMaxHeight = true
    } else Unit

var Region.usePrefWidth: Boolean
    get() = width == prefWidth
    set(value) = if (value) setMinWidth(Region.USE_PREF_SIZE) else Unit

var Region.usePrefHeight: Boolean
    get() = height == prefHeight
    set(value) = if (value) setMinHeight(Region.USE_PREF_SIZE) else Unit

var Region.usePrefSize: Boolean
    get() = maxWidth == Double.MAX_VALUE && maxHeight == Double.MAX_VALUE
    set(value) = if (value) setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE) else Unit

fun point(x: Number, y: Number) = Point2D(x.toDouble(), y.toDouble())
fun point(x: Number, y: Number, z: Number) = Point3D(x.toDouble(), y.toDouble(), z.toDouble())
infix fun Number.xy(y: Number) = Point2D(toDouble(), y.toDouble())

fun TableView<out Any>.resizeColumnsToFitContent(resizeColumns: List<TableColumn<*, *>> = contentColumns, maxRows: Int = 50, afterResize: () -> Unit = {}) {
    val doResize = {
        try {
            val resizer = skin.javaClass.getDeclaredMethod("resizeColumnToFitContent", TableColumn::class.java, Int::class.java)
            resizer.isAccessible = true
            resizeColumns.forEach { resizer.invoke(skin, it, maxRows) }
            afterResize()
        } catch (ex: Exception) {
            // Silent for now, it is usually run multiple times
            //log.warning("Unable to resize columns to content: ${columns.map { it.text }.joinToString(", ")}")
        }
    }
    if (skin == null) Platform.runLater { doResize() } else doResize()
}

fun <T> TreeTableView<T>.resizeColumnsToFitContent(resizeColumns: List<TreeTableColumn<*, *>> = contentColumns, maxRows: Int = 50, afterResize: () -> Unit = {}) {
    val doResize = {
        val resizer = skin.javaClass.getDeclaredMethod("resizeColumnToFitContent", TreeTableColumn::class.java, Int::class.java)
        resizer.isAccessible = true
        resizeColumns.forEach { resizer.invoke(skin, it, maxRows) }
        afterResize?.invoke()
    }
    if (skin == null) {
        skinProperty().onChangeOnce {
            Platform.runLater { doResize() }
        }
    } else {
        doResize()
    }
}

fun <T> TableView<T>.selectWhere(scrollTo: Boolean = true, condition: (T) -> Boolean) {
    items.asSequence().filter(condition)
            .forEach {
                selectionModel.select(it)
                if (scrollTo) scrollTo(it)
            }
}


fun <T> ListView<T>.selectWhere(scrollTo: Boolean = true, condition: (T) -> Boolean) {
    items.asSequence().filter(condition)
            .forEach {
                selectionModel.select(it)
                if (scrollTo) scrollTo(it)
            }
}

fun <T> TableView<T>.moveToTopWhere(backingList: ObservableList<T> = items, select: Boolean = true, predicate: (T) -> Boolean) {
    if (select) selectionModel.clearSelection()
    backingList.asSequence().filter(predicate).toList().asSequence().forEach {
        backingList.remove(it)
        backingList.add(0, it)
        if (select) selectionModel.select(it)
    }
}

fun <T> TableView<T>.moveToBottomWhere(backingList: ObservableList<T> = items, select: Boolean = true, predicate: (T) -> Boolean) {
    val end = backingList.size - 1
    if (select) selectionModel.clearSelection()
    backingList.asSequence().filter(predicate).toList().asSequence().forEach {
        backingList.remove(it)
        backingList.add(end, it)
        if (select) selectionModel.select(it)

    }
}

val <T> TableView<T>.selectedItem: T?
    get() = this.selectionModel.selectedItem

val <T> TreeTableView<T>.selectedItem: T?
    get() = this.selectionModel.selectedItem?.value

fun <T> TableView<T>.selectFirst() = selectionModel.selectFirst()

fun <T> TreeTableView<T>.selectFirst() = selectionModel.selectFirst()

val <T> ComboBox<T>.selectedItem: T?
    get() = selectionModel.selectedItem

fun <S> TableView<S>.onSelectionChange(func: (S?) -> Unit) =
        selectionModel.selectedItemProperty().addListener({ observable, oldValue, newValue -> func(newValue) })


fun <T> TreeTableView<T>.bindSelected(property: Property<T>) {
    selectionModel.selectedItemProperty().onChange {
        property.value = it?.value
    }
}

fun <T> TreeTableView<T>.bindSelected(model: ItemViewModel<T>) = this.bindSelected(model.itemProperty)

class TableColumnCellCache<T>(private val cacheProvider: (T) -> Node) {
    private val store = mutableMapOf<T, Node>()
    fun getOrCreateNode(value: T) = store.getOrPut(value, { cacheProvider(value) })
}

fun <S, T> TableColumn<S, T>.cellDecorator(decorator: TableCell<S, T>.(T) -> Unit) {
    val originalFactory = cellFactory

    cellFactory = Callback { column: TableColumn<S, T> ->
        val cell = originalFactory.call(column)
        cell.itemProperty().addListener { _, _, newValue ->
            if (newValue != null) decorator(cell, newValue)
        }
        cell
    }
}

fun <S, T> TreeTableColumn<S, T>.cellFormat(formatter: (TreeTableCell<S, T>.(T) -> Unit)) {
    cellFactory = Callback { column: TreeTableColumn<S, T> ->
        object : TreeTableCell<S, T>() {
            override fun updateItem(item: T, empty: Boolean) {
                super.updateItem(item, empty)

                if (item == null || empty) {
                    text = null
                    graphic = null
                } else {
                    formatter(this, item)
                }
            }
        }
    }
}

enum class EditEventType(val editing: Boolean) {
    StartEdit(true), CommitEdit(false), CancelEdit(false)
}

/**
 * Execute action when the enter key is pressed or the mouse is clicked

 * @param clickCount The number of mouse clicks to trigger the action
 * *
 * @param action The action to execute on select
 */
fun <T> TableView<T>.onUserSelect(clickCount: Int = 2, action: (T) -> Unit) {
    val isSelected = { event: InputEvent ->
        event.target.isInsideRow() && !selectionModel.isEmpty
    }

    addEventFilter(MouseEvent.MOUSE_CLICKED) { event ->
        if (event.clickCount == clickCount && isSelected(event))
            action(selectedItem!!)
    }

    addEventFilter(KeyEvent.KEY_PRESSED) { event ->
        if (event.code == KeyCode.ENTER && !event.isMetaDown && isSelected(event))
            action(selectedItem!!)
    }
}

fun Node.onDoubleClick(action: () -> Unit) {
    setOnMouseClicked {
        if (it.clickCount == 2)
            action()
    }
}

/**
 * Execute action when the enter key is pressed or the mouse is clicked

 * @param clickCount The number of mouse clicks to trigger the action
 * *
 * @param action The action to execute on select
 */
fun <T> TreeTableView<T>.onUserSelect(clickCount: Int = 2, action: (T) -> Unit) {
    val isSelected = { event: InputEvent ->
        event.target.isInsideRow() && !selectionModel.isEmpty
    }

    addEventFilter(MouseEvent.MOUSE_CLICKED) { event ->
        if (event.clickCount == clickCount && isSelected(event))
            action(selectedItem!!)
    }

    addEventFilter(KeyEvent.KEY_PRESSED) { event ->
        if (event.code == KeyCode.ENTER && !event.isMetaDown && isSelected(event))
            action(selectedItem!!)
    }
}

val <S, T> TableCell<S, T>.rowItem: S get() = tableView.items[index]
val <S, T> TreeTableCell<S, T>.rowItem: S get() = treeTableView.getTreeItem(index).value

fun <T> ListProperty<T>.asyncItems(func: () -> Collection<T>) =
        task { func() } success { value = (it as? ObservableList<T>) ?: observableArrayList(it) }

fun <T> ObservableList<T>.asyncItems(func: () -> Collection<T>) =
        task { func() } success { setAll(it) }

fun <T> SortedFilteredList<T>.asyncItems(func: () -> Collection<T>) =
        task { func() } success { items.setAll(it) }

fun <T> TableView<T>.asyncItems(func: FXTask<*>.() -> Collection<T>) =
        task(func = func).success { if (items == null) items = observableArrayList(it) else items.setAll(it) }

fun <T> ComboBox<T>.asyncItems(func: FXTask<*>.() -> Collection<T>) =
        task(func = func).success { if (items == null) items = observableArrayList(it) else items.setAll(it) }

fun <T> TableView<T>.onUserDelete(action: (T) -> Unit) {
    addEventFilter(KeyEvent.KEY_PRESSED, { event ->
        if (event.code == KeyCode.BACK_SPACE && selectedItem != null)
            action(selectedItem!!)
    })
}

/**
 * Did the event occur inside a TableRow, TreeTableRow or ListCell?
 */
fun EventTarget.isInsideRow(): Boolean {
    if (this !is Node)
        return false

    if (this is TableColumnHeader)
        return false

    if (this is TableRow<*> || this is TableView<*> || this is TreeTableRow<*> || this is TreeTableView<*> || this is ListCell<*>)
        return true

    if (this.parent != null)
        return this.parent.isInsideRow()

    return false
}

/**
 * Access BorderPane constraints to manipulate and apply on this control
 */
inline fun <T : Node> T.borderpaneConstraints(op: (BorderPaneConstraint.() -> Unit)): T {
    val bpc = BorderPaneConstraint(this)
    bpc.op()
    return bpc.applyToNode(this)
}

class BorderPaneConstraint(node: Node,
                           override var margin: Insets? = BorderPane.getMargin(node),
                           var alignment: Pos? = null
) : MarginableConstraints() {
    fun <T : Node> applyToNode(node: T): T {
        margin.let { BorderPane.setMargin(node, it) }
        alignment?.let { BorderPane.setAlignment(node, it) }
        return node
    }
}

/**
 * Access GridPane constraints to manipulate and apply on this control
 */
inline fun <T : Node> T.gridpaneConstraints(op: (GridPaneConstraint.() -> Unit)): T {
    val gpc = GridPaneConstraint(this)
    gpc.op()
    return gpc.applyToNode(this)
}

class GridPaneConstraint(node: Node,
                         var columnIndex: Int? = null,
                         var rowIndex: Int? = null,
                         var hGrow: Priority? = null,
                         var vGrow: Priority? = null,
                         override var margin: Insets? = GridPane.getMargin(node),
                         var fillHeight: Boolean? = null,
                         var fillWidth: Boolean? = null,
                         var hAlignment: HPos? = null,
                         var vAlignment: VPos? = null,
                         var columnSpan: Int? = null,
                         var rowSpan: Int? = null

) : MarginableConstraints() {
    var vhGrow: Priority? = null
        set(value) {
            vGrow = value
            hGrow = value
            field = value
        }

    var fillHeightWidth: Boolean? = null
        set(value) {
            fillHeight = value
            fillWidth = value
            field = value
        }

    fun columnRowIndex(columnIndex: Int, rowIndex: Int) {
        this.columnIndex = columnIndex
        this.rowIndex = rowIndex
    }

    fun fillHeightWidth(fill: Boolean) {
        fillHeight = fill
        fillWidth = fill
    }

    fun <T : Node> applyToNode(node: T): T {
        columnIndex?.let { GridPane.setColumnIndex(node, it) }
        rowIndex?.let { GridPane.setRowIndex(node, it) }
        hGrow?.let { GridPane.setHgrow(node, it) }
        vGrow?.let { GridPane.setVgrow(node, it) }
        margin.let { GridPane.setMargin(node, it) }
        fillHeight?.let { GridPane.setFillHeight(node, it) }
        fillWidth?.let { GridPane.setFillWidth(node, it) }
        hAlignment?.let { GridPane.setHalignment(node, it) }
        vAlignment?.let { GridPane.setValignment(node, it) }
        columnSpan?.let { GridPane.setColumnSpan(node, it) }
        rowSpan?.let { GridPane.setRowSpan(node, it) }
        return node
    }
}

inline fun <T : Node> T.vboxConstraints(op: (VBoxConstraint.() -> Unit)): T {
    val c = VBoxConstraint(this)
    c.op()
    return c.applyToNode(this)
}

inline fun <T : Node> T.stackpaneConstraints(op: (StackpaneConstraint.() -> Unit)): T {
    val c = StackpaneConstraint(this)
    c.op()
    return c.applyToNode(this)
}

class VBoxConstraint(node: Node,
                     override var margin: Insets? = VBox.getMargin(node),
                     var vGrow: Priority? = null

) : MarginableConstraints() {
    fun <T : Node> applyToNode(node: T): T {
        margin?.let { VBox.setMargin(node, it) }
        vGrow?.let { VBox.setVgrow(node, it) }
        return node
    }
}

class StackpaneConstraint(node: Node,
                          override var margin: Insets? = StackPane.getMargin(node),
                          var alignment: Pos? = null

) : MarginableConstraints() {
    fun <T : Node> applyToNode(node: T): T {
        margin?.let { StackPane.setMargin(node, it) }
        alignment?.let { StackPane.setAlignment(node, it) }
        return node
    }
}

inline fun <T : Node> T.hboxConstraints(op: (HBoxConstraint.() -> Unit)): T {
    val c = HBoxConstraint(this)
    c.op()
    return c.applyToNode(this)
}

class HBoxConstraint(node: Node,
                     override var margin: Insets? = HBox.getMargin(node),
                     var hGrow: Priority? = null
) : MarginableConstraints() {

    fun <T : Node> applyToNode(node: T): T {
        margin?.let { HBox.setMargin(node, it) }
        hGrow?.let { HBox.setHgrow(node, it) }
        return node
    }
}

var Node.hgrow: Priority? get() = HBox.getHgrow(this); set(value) {
    HBox.setHgrow(this, value)
}
var Node.vgrow: Priority? get() = VBox.getVgrow(this); set(value) {
    VBox.setVgrow(this, value)
    // Input Container vgrow must propagate to Field and Fieldset
    if (parent?.parent is Field) {
        VBox.setVgrow(parent.parent, value)
        if (parent.parent?.parent is Fieldset)
            VBox.setVgrow(parent.parent.parent, value)
    }
}

inline fun <T : Node> T.anchorpaneConstraints(op: AnchorPaneConstraint.() -> Unit): T {
    val c = AnchorPaneConstraint()
    c.op()
    return c.applyToNode(this)
}

class AnchorPaneConstraint(
        var topAnchor: Number? = null,
        var rightAnchor: Number? = null,
        var bottomAnchor: Number? = null,
        var leftAnchor: Number? = null
) {
    fun <T : Node> applyToNode(node: T): T {
        topAnchor?.let { AnchorPane.setTopAnchor(node, it.toDouble()) }
        rightAnchor?.let { AnchorPane.setRightAnchor(node, it.toDouble()) }
        bottomAnchor?.let { AnchorPane.setBottomAnchor(node, it.toDouble()) }
        leftAnchor?.let { AnchorPane.setLeftAnchor(node, it.toDouble()) }
        return node
    }
}

abstract class MarginableConstraints {
    abstract var margin: Insets?
    var marginTop: Double
        get() = margin?.top ?: 0.0
        set(value) {
            margin = Insets(value, margin?.right ?: 0.0, margin?.bottom ?: 0.0, margin?.left ?: 0.0)
        }

    var marginRight: Double
        get() = margin?.right ?: 0.0
        set(value) {
            margin = Insets(margin?.top ?: 0.0, value, margin?.bottom ?: 0.0, margin?.left ?: 0.0)
        }

    var marginBottom: Double
        get() = margin?.bottom ?: 0.0
        set(value) {
            margin = Insets(margin?.top ?: 0.0, margin?.right ?: 0.0, value, margin?.left ?: 0.0)
        }

    var marginLeft: Double
        get() = margin?.left ?: 0.0
        set(value) {
            margin = Insets(margin?.top ?: 0.0, margin?.right ?: 0.0, margin?.bottom ?: 0.0, value)
        }

    fun marginTopBottom(value: Double) {
        marginTop = value
        marginBottom = value
    }

    fun marginLeftRight(value: Double) {
        marginLeft = value
        marginRight = value
    }
}

@Suppress("CAST_NEVER_SUCCEEDS", "UNCHECKED_CAST")
inline fun <T, reified S : Any> TableColumn<T, S>.makeEditable() = apply {
    tableView?.isEditable = true
    isEditable = true
    when (S::class.javaPrimitiveType ?: S::class) {
        Int::class -> cellFactory = TextFieldTableCell.forTableColumn<T, S>(IntegerStringConverter() as StringConverter<S>)
        Integer::class -> cellFactory = TextFieldTableCell.forTableColumn<T, S>(IntegerStringConverter() as StringConverter<S>)
        Integer::class.javaPrimitiveType -> cellFactory = TextFieldTableCell.forTableColumn<T, S>(IntegerStringConverter() as StringConverter<S>)
        Double::class -> cellFactory = TextFieldTableCell.forTableColumn<T, S>(DoubleStringConverter() as StringConverter<S>)
        Double::class.javaPrimitiveType -> cellFactory = TextFieldTableCell.forTableColumn<T, S>(DoubleStringConverter() as StringConverter<S>)
        Float::class -> cellFactory = TextFieldTableCell.forTableColumn<T, S>(FloatStringConverter() as StringConverter<S>)
        Float::class.javaPrimitiveType -> cellFactory = TextFieldTableCell.forTableColumn<T, S>(FloatStringConverter() as StringConverter<S>)
        Long::class -> cellFactory = TextFieldTableCell.forTableColumn<T, S>(LongStringConverter() as StringConverter<S>)
        Long::class.javaPrimitiveType -> cellFactory = TextFieldTableCell.forTableColumn<T, S>(LongStringConverter() as StringConverter<S>)
        Number::class -> cellFactory = TextFieldTableCell.forTableColumn<T, S>(NumberStringConverter() as StringConverter<S>)
        BigDecimal::class -> cellFactory = TextFieldTableCell.forTableColumn<T, S>(BigDecimalStringConverter() as StringConverter<S>)
        BigInteger::class -> cellFactory = TextFieldTableCell.forTableColumn<T, S>(BigIntegerStringConverter() as StringConverter<S>)
        String::class -> cellFactory = TextFieldTableCell.forTableColumn<T, S>(DefaultStringConverter() as StringConverter<S>)
        LocalDate::class -> cellFactory = TextFieldTableCell.forTableColumn<T, S>(LocalDateStringConverter() as StringConverter<S>)
        LocalTime::class -> cellFactory = TextFieldTableCell.forTableColumn<T, S>(LocalTimeStringConverter() as StringConverter<S>)
        LocalDateTime::class -> cellFactory = TextFieldTableCell.forTableColumn<T, S>(LocalDateTimeStringConverter() as StringConverter<S>)
        Boolean::class.javaPrimitiveType -> {
            this as TableColumn<T, Boolean>
            setCellFactory(CheckBoxTableCell.forTableColumn(this))
        }
        else -> throw RuntimeException("makeEditable() is not implemented for specified class type:" + S::class.qualifiedName)
    }
}

fun <T> TableView<T>.regainFocusAfterEdit() = apply {
    editingCellProperty().onChange {
        if (it == null)
            requestFocus()
    }
}

fun <T, S : Any> TableColumn<T, S>.makeEditable(converter: StringConverter<S>): TableColumn<T, S> = apply {
    tableView?.isEditable = true
    cellFactory = TextFieldTableCell.forTableColumn<T, S>(converter)
}

fun <T> TreeTableView<T>.populate(itemFactory: (T) -> TreeItem<T> = { TreeItem(it) }, childFactory: (TreeItem<T>) -> Iterable<T>?) =
        populateTree(root, itemFactory, childFactory)

/**
 * Add children to the given item by invoking the supplied childFactory function, which converts
 * a TreeItem&lt;T> to a List&lt;T>?.
 *
 * If the childFactory returns a non-empty list, each entry in the list is converted to a TreeItem&lt;T>
 * via the supplied itemProcessor function. The default itemProcessor from TreeTableView.populate and TreeTable.populate
 * simply wraps the given T in a TreeItem, but you can override it to add icons etc. Lastly, the populateTree
 * function is called for each of the generated child items.
 */
fun <T> populateTree(item: TreeItem<T>, itemFactory: (T) -> TreeItem<T>, childFactory: (TreeItem<T>) -> Iterable<T>?) {
    val children = childFactory.invoke(item)

    children?.map { itemFactory.invoke(it) }?.apply {
        item.children.setAll(this)
        forEach { populateTree(it, itemFactory, childFactory) }
    }

    (children as? ObservableList<T>)?.addListener(ListChangeListener { change ->
        while (change.next()) {
            if (change.wasPermutated()) {
                item.children.subList(change.from, change.to).clear()
                val permutated = change.list.subList(change.from, change.to).map { itemFactory.invoke(it) }
                item.children.addAll(change.from, permutated)
                permutated.forEach { populateTree(it, itemFactory, childFactory) }
            } else {
                if (change.wasRemoved()) {
                    val removed = change.removed.flatMap { removed -> item.children.filter { it.value == removed } }
                    item.children.removeAll(removed)
                }
                if (change.wasAdded()) {
                    val added = change.addedSubList.map { itemFactory.invoke(it) }
                    item.children.addAll(change.from, added)
                    added.forEach { populateTree(it, itemFactory, childFactory) }
                }
            }
        }
    })
}

/**
 * Return the UIComponent (View or Fragment) that owns this Parent
 */
inline fun <reified T : UIComponent> Node.uiComponent(): T? = properties[UI_COMPONENT_PROPERTY] as? T

/**
 * Return the UIComponent (View or Fragment) that represents the root of the current Scene within this Stage
 */
inline fun <reified T : UIComponent> Stage.uiComponent(): T? = scene.root.uiComponent()

/**
 * Find all UIComponents of the specified type that owns any of this node's children
 */
inline fun <reified T : UIComponent> Parent.findAll(): List<T> = childrenUnmodifiable
        .filterIsInstance<Parent>()
        .map { it.uiComponent<UIComponent>() }
        .filterIsInstance<T>()


/**
 * Find all UIComponents of the specified type that owns any of this UIComponent's root node's children
 */
inline fun <reified T : UIComponent> UIComponent.findAll(): List<T> = root.findAll()

/**
 * Find the first UIComponent of the specified type that owns any of this node's children
 */
inline fun <reified T : UIComponent> Parent.lookup(noinline op: T.() -> Unit = {}): T? = findAll<T>().getOrNull(0)?.also(op)

/**
 * Find the first UIComponent of the specified type that owns any of this UIComponent's root node's children
 */
inline fun <reified T : UIComponent> UIComponent.lookup(noinline op: T.() -> Unit = {}): T? = findAll<T>().getOrNull(0)?.also(op)

fun EventTarget.removeFromParent() {
    if (this is UIComponent) {
        root.removeFromParent()
    } else if (this is DrawerItem) {
        drawer.items.remove(this)
    } else if (this is Tab) {
        tabPane?.tabs?.remove(this)
    } else if (this is Node) {
        (parent?.parent as? ToolBar)?.items?.remove(this)
                ?: parent?.getChildList()?.remove(this)
    }
}

/**
 * Listen for changes to an observable value and replace all content in this Node with the
 * new content created by the onChangeBuilder. The builder operates on the node and receives
 * the new value of the observable as it's only parameter.
 *
 * The onChangeBuilder is run immediately with the current value of the property.
 */
fun <S : EventTarget, T> S.dynamicContent(property: ObservableValue<T>, onChangeBuilder: S.(T?) -> Unit) {
    val onChange: (T?) -> Unit = {
        getChildList()?.clear()
        onChangeBuilder(this@dynamicContent, it)
    }
    property.onChange(onChange)
    onChange(property.value)
}

const val TRANSITIONING_PROPERTY = "tornadofx.transitioning"
/**
 * Whether this node is currently being used in a [ViewTransition]. Used to determine whether it can be used in a
 * transition. (Nodes can only exist once in the scenegraph, so it cannot be in two transitions at once.)
 */
internal var Node.isTransitioning: Boolean
    get() {
        val x = properties[TRANSITIONING_PROPERTY]
        return x != null && (x !is Boolean || x != false)
    }
    set(value) {
        properties[TRANSITIONING_PROPERTY] = value
    }

/**
 * Replace this [Node] with another, optionally using a transition animation.
 *
 * @param replacement The node that will replace this one
 * @param transition The [ViewTransition] used to animate the transition
 * @return Whether or not the transition will run
 */
fun Node.replaceWith(replacement: Node, transition: ViewTransition? = null, sizeToScene: Boolean = false, centerOnScreen: Boolean = false, onTransit: () -> Unit = {}): Boolean {
    if (isTransitioning || replacement.isTransitioning) {
        return false
    }
    onTransit()
    if (this == scene?.root) {
        val scene = scene!!

        if (replacement !is Parent) {
            throw IllegalArgumentException("Replacement scene root must be a Parent")
        }

        // Update scene property to support Live Views
        replacement.uiComponent<UIComponent>()?.properties?.put("tornadofx.scene", scene)

        if (transition != null) {
            transition.call(this, replacement) {
                scene.root = it as Parent
                if (sizeToScene) scene.window.sizeToScene()
                if (centerOnScreen) scene.window.centerOnScreen()
            }
        } else {
            removeFromParent()
            replacement.removeFromParent()
            scene.root = replacement
            if (sizeToScene) scene.window.sizeToScene()
            if (centerOnScreen) scene.window.centerOnScreen()
        }
        return true
    } else if (parent is Pane) {
        val parent = parent as Pane
        val attach = if (parent is BorderPane) {
            when (this) {
                parent.top -> {
                    { it: Node -> parent.top = it }
                }
                parent.right -> {
                    { parent.right = it }
                }
                parent.bottom -> {
                    { parent.bottom = it }
                }
                parent.left -> {
                    { parent.left = it }
                }
                parent.center -> {
                    { parent.center = it }
                }
                else -> {
                    { throw IllegalStateException("Child of BorderPane not found in BorderPane") }
                }
            }
        } else {
            val children = parent.children
            val index = children.indexOf(this);
            { children.add(index, it) }
        }

        if (transition != null) {
            transition.call(this, replacement, attach)
        } else {
            removeFromParent()
            replacement.removeFromParent()
            attach(replacement)
        }
        return true
    } else {
        return false
    }
}

@Deprecated("This will go away in the future. Use the version with centerOnScreen parameter", ReplaceWith("replaceWith(replacement, transition, sizeToScene, false)"))
fun Node.replaceWith(replacement: Node, transition: ViewTransition? = null, sizeToScene: Boolean, onTransit: () -> Unit = {}) =
        replaceWith(replacement, transition, sizeToScene, false)

fun Node.hide() {
    isVisible = false
    isManaged = false
}

fun Node.show() {
    isVisible = true
    isManaged = true
}

fun Node.whenVisible(runLater: Boolean = true, op: () -> Unit) {
    visibleProperty().onChange {
        if (it) {
            if (runLater) Platform.runLater(op) else op()
        }
    }
}

inline fun <reified T:Any> Node.findParent(): T? = findParentOfType(T::class)

@Suppress("UNCHECKED_CAST")
fun <T : Any> Node.findParentOfType(parentType: KClass<T>): T? {
    if (parent == null) return null
    parentType.safeCast(parent)?.also { return it }
    val uicmp = parent.uiComponent<UIComponent>()
    parentType.safeCast(uicmp)?.also { return it }
    return parent?.findParentOfType(parentType)
}

val Region.paddingTopProperty: DoubleProperty get() {
    return properties.getOrPut("paddingTopProperty") {
        proxypropDouble(paddingProperty(), { value.top }) {
            Insets(it, value.right, value.bottom, value.left)
        }
    } as DoubleProperty
}

val Region.paddingBottomProperty: DoubleProperty get() {
    return properties.getOrPut("paddingBottomProperty") {
        proxypropDouble(paddingProperty(), { value.bottom }) {
            Insets(value.top, value.right, it, value.left)
        }
    } as DoubleProperty
}

val Region.paddingLeftProperty: DoubleProperty get() {
    return properties.getOrPut("paddingLeftProperty") {
        proxypropDouble(paddingProperty(), { value.left }) {
            Insets(value.top, value.right, value.bottom, it)
        }
    } as DoubleProperty
}

val Region.paddingRightProperty: DoubleProperty get() {
    return properties.getOrPut("paddingRightProperty") {
        proxypropDouble(paddingProperty(), { value.right }) {
            Insets(value.top, it, value.bottom, value.left)
        }
    } as DoubleProperty
}

val Region.paddingVerticalProperty: DoubleProperty get() {
    return properties.getOrPut("paddingVerticalProperty") {
        proxypropDouble(paddingProperty(), { paddingVertical.toDouble() }) {
            val half = it / 2.0
            Insets(half, value.right, half, value.left)
        }
    } as DoubleProperty
}

val Region.paddingHorizontalProperty: DoubleProperty get() {
    return properties.getOrPut("paddingHorizontalProperty") {
        proxypropDouble(paddingProperty(), { paddingHorizontal.toDouble() }) {
            val half = it / 2.0
            Insets(value.top, half, value.bottom, half)
        }
    } as DoubleProperty
}

val Region.paddingAllProperty: DoubleProperty get() {
    return properties.getOrPut("paddingAllProperty") {
        proxypropDouble(paddingProperty(), { paddingAll.toDouble() }) {
            Insets(it, it, it, it)
        }
    } as DoubleProperty
}

// -- Node helpers
/**
 * This extension function will automatically bind to the managedProperty of the given node
 * and will make sure that it is managed, if the given [expr] returning an observable boolean value equals true.
 *
 * @see https://docs.oracle.com/javase/8/javafx/api/javafx/scene/Node.html#managedProperty
 */
fun <T: Node> T.managedWhen(expr: () -> ObservableValue<Boolean>): T = managedWhen(expr())

/**
 * This extension function will automatically bind to the managedProperty of the given node
 * and will make sure that it is managed, if the given [predicate] an observable boolean value equals true.
 *
 * @see https://docs.oracle.com/javase/8/javafx/api/javafx/scene/Node.html#managedProperty)
 */
fun <T: Node> T.managedWhen(predicate: ObservableValue<Boolean>) = apply {
    managedProperty().cleanBind(predicate)
}

/**
 * This extension function will automatically bind to the visibleProperty of the given node
 * and will make sure that it is visible, if the given [predicate] an observable boolean value equals true.
 *
 * @see https://docs.oracle.com/javase/8/javafx/api/javafx/scene/Node.html#visibleProperty
 */
fun <T: Node> T.visibleWhen(predicate: ObservableValue<Boolean>) = apply {
  visibleProperty().cleanBind(predicate)
}

/**
 * This extension function will automatically bind to the visibleProperty of the given node
 * and will make sure that it is visible, if the given [expr] returning an observable boolean value equals true.
 *
 * @see https://docs.oracle.com/javase/8/javafx/api/javafx/scene/Node.html#visibleProperty
 */
fun <T: Node> T.visibleWhen(expr: () -> ObservableValue<Boolean>): T = visibleWhen(expr())

/**
 * This extension function will make sure to hide the given node,
 * if the given [expr] returning an observable boolean value equals true.
 */
fun <T: Node> T.hiddenWhen(expr: () -> ObservableValue<Boolean>): T = hiddenWhen(expr())

/**
 * This extension function will make sure to hide the given node,
 * if the given [predicate] an observable boolean value equals true.
 */
fun <T: Node> T.hiddenWhen(predicate: ObservableValue<Boolean>) = apply {
    val binding = if (predicate is BooleanBinding) predicate.not() else predicate.toBinding().not()
    visibleProperty().cleanBind(binding)
}

/**
 * This extension function will automatically bind to the disableProperty of the given node
 * and will disable it, if the given [expr] returning an observable boolean value equals true.
 *
 * @see https://docs.oracle.com/javase/8/javafx/api/javafx/scene/Node.html#disable
 */
fun <T: Node> T.disableWhen(expr: () -> ObservableValue<Boolean>): T = disableWhen(expr())

/**
 * This extension function will automatically bind to the disableProperty of the given node
 * and will disable it, if the given [predicate] observable boolean value equals true.
 *
 * @see https://docs.oracle.com/javase/8/javafx/api/javafx/scene/Node.html#disableProperty
 */
fun <T: Node> T.disableWhen(predicate: ObservableValue<Boolean>) = apply {
    disableProperty().cleanBind(predicate)
}

/**
 * This extension function will make sure that the given node is enabled when ever,
 * the given [expr] returning an observable boolean value equals true.
 */
fun <T: Node> T.enableWhen(expr: () -> ObservableValue<Boolean>): T = enableWhen(expr())

/**
 * This extension function will make sure that the given node is enabled when ever,
 * the given [predicate] observable boolean value equals true.
 */
fun <T: Node> T.enableWhen(predicate: ObservableValue<Boolean>) = apply {
    val binding = if (predicate is BooleanBinding) predicate.not() else predicate.toBinding().not()
    disableProperty().cleanBind(binding)
}

/**
 * This extension function will make sure that the given node will only be visible in the scene graph,
 * if the given [expr] returning an observable boolean value equals true.
 */
fun <T: Node> T.removeWhen(expr: () -> ObservableValue<Boolean>): T = removeWhen(expr())

/**
 * This extension function will make sure that the given node will only be visible in the scene graph,
 * if the given [predicate] observable boolean value equals true.
 */
fun <T: Node> T.removeWhen(predicate: ObservableValue<Boolean>) = apply {
    val remove = booleanBinding(predicate) { predicate.value.not() }
    visibleProperty().cleanBind(remove)
    managedProperty().cleanBind(remove)
}

fun TextInputControl.editableWhen(predicate: ObservableValue<Boolean>) = apply {
    editableProperty().bind(predicate)
}

/**
 * This extension function will make sure that the given [onHover] function will always be calles
 * when ever the hoverProperty of the given node changes.
 */
fun <T: Node> T.onHover(onHover: (Boolean) -> Unit) = apply {
    hoverProperty().onChange { onHover(isHover) }
}

// -- MenuItem helpers
fun MenuItem.visibleWhen(expr: () -> ObservableValue<Boolean>) = visibleWhen(expr())
fun MenuItem.visibleWhen(predicate: ObservableValue<Boolean>) = visibleProperty().cleanBind(predicate)
fun MenuItem.disableWhen(expr: () -> ObservableValue<Boolean>) = disableWhen(expr())
fun MenuItem.disableWhen(predicate: ObservableValue<Boolean>) = disableProperty().cleanBind(predicate)
fun MenuItem.enableWhen(expr: () -> ObservableValue<Boolean>) = enableWhen(expr())
fun MenuItem.enableWhen(obs: ObservableValue<Boolean>) {
    val binding = if (obs is BooleanBinding) obs.not() else obs.toBinding().not()
    disableProperty().cleanBind(binding)
}

fun EventTarget.svgicon(shape: String, size: Number = 16, color: Paint = Color.BLACK, op: SVGIcon.() -> Unit = {}) = opcr(this, SVGIcon(shape, size, color), op)

class SVGIcon(svgShape: String, size: Number = 16, color: Paint = Color.BLACK) : Pane() {
    init {
        addClass("icon", "svg-icon")
        style {
            shape = svgShape
            backgroundColor += color
            minWidth = size.px
            minHeight = size.px
            maxWidth = size.px
            maxHeight = size.px
        }
    }
}

internal class ShortLongPressHandler(node: Node) {
    var holdTimer = PauseTransition(700.millis)
    var consume: Boolean = false
    var originatingEvent: MouseEvent? = null

    var shortAction: ((MouseEvent) -> Unit)? = null
    var longAction: ((MouseEvent) -> Unit)? = null

    init {
        holdTimer.setOnFinished { longAction?.invoke(originatingEvent!!) }

        node.addEventHandler(MouseEvent.MOUSE_PRESSED) {
            originatingEvent = it
            holdTimer.playFromStart()
            if (consume) it.consume()
        }

        node.addEventHandler(MouseEvent.MOUSE_RELEASED) {
            if (holdTimer.status == Animation.Status.RUNNING) {
                holdTimer.stop()
                shortAction?.invoke(originatingEvent!!)
                if (consume) it.consume()
            }
        }
    }
}

internal val Node.shortLongPressHandler: ShortLongPressHandler get() = properties.getOrPut("tornadofx.shortLongPressHandler") {
    ShortLongPressHandler(this)
} as ShortLongPressHandler

fun <T: Node> T.shortpress(consume: Boolean = false, action: (InputEvent) -> Unit) = apply {
    shortLongPressHandler.apply {
        this.consume = consume
        this.shortAction = action
    }
}

fun <T: Node> T.longpress(threshold: Duration = 700.millis, consume: Boolean = false, action: (MouseEvent) -> Unit) = apply {
    shortLongPressHandler.apply {
        this.consume = consume
        this.holdTimer.duration = threshold
        this.longAction = action
    }
}

/**
 * Create, cache and return a Node and store it within the owning node. Typical usage:
 *
 *
 * ```
 * listview(people) {
 *     cellFormat {
 *         graphic = cache {
 *             hbox {
 *                 label("Some large Node graph here")
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * Used within a Cell, the cache statement makes sure that the node is only created once per cell during the cell's life time.
 * This greatly reduces memory and performance overhead and should be used in every situation where
 * a node graph is created and assigned to the graphic property of a cell.
 *
 * Note that if you call this function without a a unique key parameter, you will only ever create a single
 * cached node for this parent. The use case for this function is mostly to cache the graphic node of a cell,
 * so for these use cases you don't need to supply a cache key.
 *
 * Remember that you can still update whatever you assign to graphic below it on each `cellFormat` update item callback.
 */
fun <T : Node> Node.cache(key: Any = "tornadofx.cachedNode", op: EventTarget.() -> T) = properties.getOrPut(key) {
    op(this)
} as T


/**
 * Filter the input of the text field by passing each change to the discriminator
 * function and only applying the change if the discriminator returns true
 *
 * To only allow digits for example, do:
 *
 * filterInput { it.controlNewText.isInt() }
 *
 * You can also access just the changed text in `it.text` to validate just the new input.
 *
 */
fun TextInputControl.filterInput(discriminator: (TextFormatter.Change) -> Boolean) {
    textFormatter = TextFormatter<Any>(CustomTextFilter(discriminator))
}

/**
 * Custom text filter used to supress input values, for example to
 * only allow numbers in a textfield. Used via the filterInput {} builder
 */
class CustomTextFilter(private val discriminator: (TextFormatter.Change) -> Boolean) : UnaryOperator<TextFormatter.Change> {
    override fun apply(c: TextFormatter.Change): TextFormatter.Change =
            if (discriminator(c)) c else c.clone().apply { text = "" }
}