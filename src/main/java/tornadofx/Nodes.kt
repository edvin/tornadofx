package tornadofx

import com.sun.javafx.scene.control.skin.TableColumnHeader
import javafx.application.Platform
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.geometry.VPos
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
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.util.Callback
import javafx.util.StringConverter
import javafx.util.converter.*
import tornadofx.osgi.OSGIConsole
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import kotlin.reflect.KClass

fun TableColumnBase<*, *>.hasClass(className: String) = styleClass.contains(className)
fun TableColumnBase<*, *>.addClass(className: String) = styleClass.add(className)
fun TableColumnBase<*, *>.removeClass(className: String) = styleClass.remove(className)
fun TableColumnBase<*, *>.toggleClass(className: String, predicate: Boolean) {
    if (predicate) {
        if (!hasClass(className)) addClass(className)
    } else {
        removeClass(className)
    }
}

fun Node.hasClass(className: String) = styleClass.contains(className)
fun <T : Node> T.addClass(className: String): T {
    styleClass.add(className); return this
}

fun <T : Node> T.removeClass(className: String): T {
    styleClass.remove(className); return this
}

fun <T : Node> T.toggleClass(className: String, predicate: Boolean): T {
    if (predicate) {
        if (!hasClass(className)) addClass(className)
    } else {
        removeClass(className)
    }
    return this
}

fun Node.getToggleGroup(): ToggleGroup? = properties["tornadofx.togglegroup"] as ToggleGroup?

fun Node.tooltip(text: String? = null, graphic: Node? = null, op: (Tooltip.() -> Unit)? = null): Tooltip {
    val newToolTip = Tooltip(text)
    graphic?.apply { newToolTip.graphic = this }
    if (op != null) newToolTip.op()
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

fun Scene.reloadViews() {
    if (properties["javafx.layoutdebugger"] == null) {
        findUIComponents().forEach {
            if (it.reloadInit) FX.replaceComponent(it)
            it.reloadInit = true
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
        FX.components.remove(uicmp.javaClass.kotlin)
    } else {
        childrenUnmodifiable.asSequence().filterIsInstance<Parent>().forEach { it.clearViews() }
    }
}

fun Stage.reloadStylesheetsOnFocus() {
    focusedProperty().addListener { obs, old, focused ->
        if (focused && FX.initialized.value) scene?.reloadStylesheets()
    }
}

fun Stage.hookGlobalShortcuts() {
    addEventFilter(KeyEvent.KEY_PRESSED) {
        if (FX.layoutDebuggerShortcut?.match(it) ?: false)
            LayoutDebugger.debug(scene)
        else if (FX.osgiDebuggerShortcut?.match(it) ?: false && FX.osgiAvailable)
            find(OSGIConsole::class).openModal(modality = Modality.NONE)
    }
}

fun Stage.reloadViewsOnFocus() {
    focusedProperty().addListener { obs, old, focused ->
        if (focused && FX.initialized.value) scene?.reloadViews()
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
    val children = getChildList() ?: throw IllegalArgumentException("This node doesn't have a child list")
    children.clear()
    children.addAll(node)
}

operator fun EventTarget.plusAssign(node: Node) {
    addChildIfPossible(node)
}

fun <T : EventTarget> T.replaceChildren(op: T.() -> Unit) {
    getChildList()?.clear()
    op(this)
}

@Deprecated("Just an alias for += SomeType::class", ReplaceWith("this += SomeType::class"), DeprecationLevel.WARNING)
@JvmName("addView")
inline fun <reified T : View> EventTarget.add(type: KClass<T>): Unit = plusAssign(find(type).root)

@JvmName("addFragment")
inline fun <reified T : Fragment> EventTarget.add(type: KClass<T>): Unit = plusAssign(find(type).root)

fun EventTarget.add(node: Node) = plusAssign(node)

@JvmName("plusView")
operator fun <T : View> EventTarget.plusAssign(type: KClass<T>): Unit = plusAssign(find(type).root)

@JvmName("plusFragment")
operator fun <T : Fragment> EventTarget.plusAssign(type: KClass<T>) = plusAssign(find(type).root)

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
    set(value) = if (value) setMinWidth(Button.USE_PREF_SIZE) else Unit

var Region.usePrefHeight: Boolean
    get() = height == prefHeight
    set(value) = if (value) setMinHeight(Button.USE_PREF_SIZE) else Unit

var Region.usePrefSize: Boolean
    get() = maxWidth == Double.MAX_VALUE && maxHeight == Double.MAX_VALUE
    set(value) = if (value) setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE) else Unit


fun TableView<out Any>.resizeColumnsToFitContent(resizeColumns: List<TableColumn<*, *>> = columns, maxRows: Int = 50, afterResize: (() -> Unit)? = null) {
    val doResize = {
        try {
            val resizer = skin.javaClass.getDeclaredMethod("resizeColumnToFitContent", TableColumn::class.java, Int::class.java)
            resizer.isAccessible = true
            resizeColumns.forEach { resizer.invoke(skin, it, maxRows) }
            afterResize?.invoke()
        } catch (ex: Exception) {
            // Silent for now, it is usually run multiple times
            //log.warning("Unable to resize columns to content: ${columns.map { it.text }.joinToString(", ")}")
        }
    }
    if (skin == null) Platform.runLater { doResize() } else doResize()
}

fun <T> TreeTableView<T>.resizeColumnsToFitContent(resizeColumns: List<TreeTableColumn<T, *>> = columns, maxRows: Int = 50, afterResize: (() -> Unit)? = null) {
    val doResize = {
        val resizer = skin.javaClass.getDeclaredMethod("resizeColumnToFitContent", TreeTableColumn::class.java, Int::class.java)
        resizer.isAccessible = true
        resizeColumns.forEach { resizer.invoke(skin, it, maxRows) }
        afterResize?.invoke()
    }
    if (skin == null) Platform.runLater { doResize() } else doResize()
}

fun <T> TableView<T>.selectWhere(scrollTo: Boolean = true, condition: (T) -> Boolean) {
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

val <T> TreeView<T>.selectedValue: T?
    get() = this.selectionModel.selectedItem?.value

fun <T> TableView<T>.selectFirst() = selectionModel.selectFirst()

fun <T> TreeView<T>.selectFirst() = selectionModel.selectFirst()

fun <T> TreeTableView<T>.selectFirst() = selectionModel.selectFirst()

val <T> ComboBox<T>.selectedItem: T?
    get() = selectionModel.selectedItem

fun <S> TableView<S>.onSelectionChange(func: (S?) -> Unit) =
        selectionModel.selectedItemProperty().addListener({ observable, oldValue, newValue -> func(newValue) })

class TableColumnCellCache<T>(private val cacheProvider: (T) -> Node) {
    private val store = mutableMapOf<T, Node>()
    fun getOrCreateNode(value: T) = store.getOrPut(value, { cacheProvider(value) })
}

/**
 * Calculate a unique Node per item and set this Node as the graphic of the TableCell.
 *
 * To support this feature, a custom cellFactory is automatically installed, unless an already
 * compatible cellFactory is found. The cellFactories installed via #cellFormat already knows
 * how to retrieve cached values.
 */
fun <S, T> TableColumn<S, T>.cellCache(cachedGraphicProvider: (T) -> Node) {
    properties["tornadofx.cellCache"] = TableColumnCellCache(cachedGraphicProvider)
    // Install a cache capable cellFactory it none is present. The default cellFormat factory will do.
    if (properties["tornadofx.cellCacheCapable"] != true) {
        cellFormat { }
    }
}

@Suppress("UNCHECKED_CAST")
fun <S, T> TableColumn<S, T>.cellFormat(formatter: TableCell<S, T>.(T) -> Unit) {
    properties["tornadofx.cellCacheCapable"] = true
    cellFactory = Callback { column: TableColumn<S, T> ->
        object : TableCell<S, T>() {
            override fun updateItem(item: T, empty: Boolean) {
                super.updateItem(item, empty)

                if (item == null || empty) {
                    text = null
                    graphic = null
                } else {
                    // Consult the cell cache before calling the formatter function
                    val cellCache = this@cellFormat.properties["tornadofx.cellCache"]
                    if (cellCache is TableColumnCellCache<*>) {
                        graphic = (cellCache as TableColumnCellCache<T>).getOrCreateNode(item)
                    }
                    formatter(this, item)
                }
            }
        }
    }
}

fun <T> ComboBox<T>.cellFormat(formatter: ListCell<T>.(T) -> Unit) {
    cellFactory = Callback {
        it.properties["tornadofx.cellFormat"] = formatter
        SmartListCell(it)
    }
}

fun <S, T> TableColumn<S, T>.cellDecorator(decorator: TableCell<S, T>.(T) -> Unit) {
    val originalFactory = cellFactory

    cellFactory = Callback { column: TableColumn<S, T> ->
        val cell = originalFactory.call(column)
        cell.itemProperty().addListener { obs, oldValue, newValue -> decorator(cell, newValue) }
        cell
    }
}

fun <S> TreeView<S>.cellFormat(formatter: (TreeCell<S>.(S) -> Unit)) {
    cellFactory = Callback {
        object : TreeCell<S>() {
            override fun updateItem(item: S?, empty: Boolean) {
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

fun <S> TreeView<S>.cellDecorator(decorator: (TreeCell<S>.(S?) -> Unit)) {
    val originalFactory = cellFactory

    if (originalFactory == null) cellFormat(decorator) else {
        cellFactory = Callback { treeView: TreeView<S> ->
            val cell = originalFactory.call(treeView)
            cell.itemProperty().onChange { decorator(cell, cell.item) }
            cell
        }
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
        event.target.isInsideTableRow() && !selectionModel.isEmpty
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

/**
 * Execute action when the enter key is pressed or the mouse is clicked

 * @param clickCount The number of mouse clicks to trigger the action
 * *
 * @param action The action to execute on select
 */
fun <T> TreeTableView<T>.onUserSelect(clickCount: Int = 2, action: (T) -> Unit) {
    val isSelected = { event: InputEvent ->
        event.target.isInsideTableRow() && !selectionModel.isEmpty
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

fun <T> SortedFilteredList<T>.asyncItems(func: () -> Collection<T>) =
        task { func() } success { items.setAll(it) }

fun <T> TableView<T>.asyncItems(func: () -> Collection<T>) =
        task { func() } success { if (items == null) items = observableArrayList(it) else items.setAll(it) }

fun <T> ComboBox<T>.asyncItems(func: () -> Collection<T>) =
        task { func() } success { if (items == null) items = observableArrayList(it) else items.setAll(it) }

fun <T> TreeView<T>.onUserSelect(action: (T) -> Unit) {
    selectionModel.selectedItemProperty().addListener { obs, old, new ->
        if (new != null && new.value != null)
            action(new.value)
    }
}

fun <T> TableView<T>.onUserDelete(action: (T) -> Unit) {
    addEventFilter(KeyEvent.KEY_PRESSED, { event ->
        if (event.code == KeyCode.BACK_SPACE && selectedItem != null)
            action(selectedItem!!)
    })
}

fun <T> TreeView<T>.onUserDelete(action: (T) -> Unit) {
    addEventFilter(KeyEvent.KEY_PRESSED, { event ->
        if (event.code == KeyCode.BACK_SPACE && selectionModel.selectedItem?.value != null)
            action(selectedValue!!)
    })
}

/**
 * Did the event occur inside a TableRow or TreeTableRow?
 */
fun EventTarget.isInsideTableRow(): Boolean {
    if (this !is Node)
        return false

    if (this is TableColumnHeader)
        return false

    if (this is TableRow<*> || this is TableView<*> || this is TreeTableRow<*> || this is TreeTableView<*>)
        return true

    if (this.parent != null)
        return this.parent.isInsideTableRow()

    return false
}

/**
 * Access BorderPane constraints to manipulate and apply on this control
 */
fun <T : Node> T.borderpaneConstraints(op: (BorderPaneConstraint.() -> Unit)): T {
    val bpc = BorderPaneConstraint()
    bpc.op()
    return bpc.applyToNode(this)
}

class BorderPaneConstraint(
        override var margin: Insets = Insets(0.0, 0.0, 0.0, 0.0),
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
fun <T : Node> T.gridpaneConstraints(op: (GridPaneConstraint.() -> Unit)): T {
    val gpc = GridPaneConstraint()
    gpc.op()
    return gpc.applyToNode(this)
}

class GridPaneConstraint(
        var columnIndex: Int? = null,
        var rowIndex: Int? = null,
        var hGrow: Priority? = null,
        var vGrow: Priority? = null,
        override var margin: Insets = Insets(0.0, 0.0, 0.0, 0.0),
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

fun <T : Node> T.vboxConstraints(op: (VBoxConstraint.() -> Unit)): T {
    val c = VBoxConstraint()
    c.op()
    return c.applyToNode(this)
}

class VBoxConstraint(
        override var margin: Insets = Insets(0.0, 0.0, 0.0, 0.0),
        var vGrow: Priority? = null

) : MarginableConstraints() {
    fun <T : Node> applyToNode(node: T): T {
        margin.let { VBox.setMargin(node, it) }
        vGrow?.let { VBox.setVgrow(node, it) }
        return node
    }
}

fun <T : Node> T.hboxConstraints(op: (HBoxConstraint.() -> Unit)): T {
    val c = HBoxConstraint()
    c.op()
    return c.applyToNode(this)
}

class HBoxConstraint(
        override var margin: Insets = Insets(0.0, 0.0, 0.0, 0.0),
        var hGrow: Priority? = null
) : MarginableConstraints() {

    fun <T : Node> applyToNode(node: T): T {
        margin.let { HBox.setMargin(node, it) }
        hGrow?.let { HBox.setHgrow(node, it) }
        return node
    }
}

fun <T : Node> T.anchorpaneConstraints(op: AnchorPaneConstraint.() -> Unit): T {
    val c = AnchorPaneConstraint()
    c.op()
    return c.applyToNode(this)
}

class AnchorPaneConstraint(
        var topAnchor: Double? = null,
        var rightAnchor: Double? = null,
        var bottomAnchor: Double? = null,
        var leftAnchor: Double? = null
) {
    fun <T : Node> applyToNode(node: T): T {
        topAnchor?.let { AnchorPane.setTopAnchor(node, it) }
        rightAnchor?.let { AnchorPane.setRightAnchor(node, it) }
        bottomAnchor?.let { AnchorPane.setBottomAnchor(node, it) }
        leftAnchor?.let { AnchorPane.setLeftAnchor(node, it) }
        return node
    }
}

abstract class MarginableConstraints {
    abstract var margin: Insets
    var marginTop: Double
        get() = margin.top
        set(value) {
            margin = margin.let { Insets(value, it.right, it.bottom, it.left) }
        }

    var marginRight: Double
        get() = margin.right
        set(value) {
            margin = margin.let { Insets(it.top, value, it.bottom, it.left) }
        }

    var marginBottom: Double
        get() = margin.bottom
        set(value) {
            margin = margin.let { Insets(it.top, it.right, value, it.left) }
        }

    var marginLeft: Double
        get() = margin.left
        set(value) {
            margin = margin.let { Insets(it.top, it.right, it.bottom, value) }
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
inline fun <T, reified S : Any> TableColumn<T, S>.makeEditable() {
    isEditable = true
    when (S::class.javaPrimitiveType ?: S::class) {
        Number::class -> setCellFactory(TextFieldTableCell.forTableColumn<T, S>(NumberStringConverter() as StringConverter<S>))
        String::class -> setCellFactory(TextFieldTableCell.forTableColumn<T, S>(DefaultStringConverter() as StringConverter<S>))
        LocalDate::class -> setCellFactory(TextFieldTableCell.forTableColumn<T, S>(LocalDateStringConverter() as StringConverter<S>))
        LocalTime::class -> setCellFactory(TextFieldTableCell.forTableColumn<T, S>(LocalTimeStringConverter() as StringConverter<S>))
        LocalDateTime::class -> setCellFactory(TextFieldTableCell.forTableColumn<T, S>(LocalDateTimeStringConverter() as StringConverter<S>))
        Boolean::class.javaPrimitiveType -> {
            this as TableColumn<T, Boolean>
            setCellFactory(CheckBoxTableCell.forTableColumn(this))
        }
        else -> throw RuntimeException("makeEditable() is not implemented for specified class type:" + S::class.qualifiedName)
    }
}

fun <T> TreeTableView<T>.populate(itemFactory: (T) -> TreeItem<T> = { TreeItem(it) }, childFactory: (TreeItem<T>) -> Iterable<T>?) =
        populateTree(root, itemFactory, childFactory)

fun <T> TreeView<T>.populate(itemFactory: (T) -> TreeItem<T> = { TreeItem(it) }, childFactory: (TreeItem<T>) -> Iterable<T>?) =
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
    childFactory.invoke(item)?.map { itemFactory.invoke(it) }?.apply {
        item.children.setAll(this)
        forEach { populateTree(it, itemFactory, childFactory) }
    }
}

/**
 * Return the UIComponent (View or Fragment) that owns this Parent
 */
inline fun <reified T : UIComponent> Node.uiComponent(): T? = properties[UI_COMPONENT_PROPERTY] as? T

/**
 * Find all UIComponents of the specified type that owns any of this node's children
 */
inline fun <reified T : UIComponent> Parent.findAll(): List<T> = childrenUnmodifiable
        .filterIsInstance<Parent>()
        .filter { it.uiComponent<UIComponent>() is T }
        .map { it.uiComponent<T>()!! }

/**
 * Find all UIComponents of the specified type that owns any of this UIComponent's root node's children
 */
inline fun <reified T : UIComponent> UIComponent.findAll(): List<T> = root.findAll()

/**
 * Find the first UIComponent of the specified type that owns any of this node's children
 */
inline fun <reified T : UIComponent> Parent.lookup(noinline op: (T.() -> Unit)? = null): T? {
    val result = findAll<T>().getOrNull(0)
    if (result != null) op?.invoke(result)
    return result
}

/**
 * Find the first UIComponent of the specified type that owns any of this UIComponent's root node's children
 */
inline fun <reified T : UIComponent> UIComponent.lookup(noinline op: (T.() -> Unit)? = null): T? {
    val result = findAll<T>().getOrNull(0)
    if (result != null) op?.invoke(result)
    return result
}

fun EventTarget.removeFromParent() {
    if (this is UIComponent) {
        root.removeFromParent()
    } else if (this is Tab) {
        tabPane?.tabs?.remove(this)
    } else if (this is Node) {
        parent?.getChildList()?.remove(this)
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
fun Node.replaceWith(replacement: Node, transition: ViewTransition? = null, onTransit: (() -> Unit)? = null): Boolean {
    if (isTransitioning || replacement.isTransitioning) {
        return false
    }
    onTransit?.invoke()
    if (this == scene?.root) {
        val scene = scene!!
        if (replacement !is Parent) {
            throw IllegalArgumentException("Replacement scene root must be a Parent")
        }

        if (transition != null) {
            transition.call(this, replacement) {
                scene.root = it as Parent
            }
        } else {
            removeFromParent()
            replacement.removeFromParent()
            scene.root = replacement
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


fun Node.hide() {
    isVisible = false
    isManaged = false
}

fun Node.show() {
    isVisible = true
    isManaged = true
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> Node.findParentOfType(parentType: KClass<T>): T? {
    if (parent == null) return null
    if (parent.javaClass.kotlin == parentType) return parent as T
    return parent!!.findParentOfType(parentType)
}