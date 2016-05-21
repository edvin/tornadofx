package tornadofx

import com.sun.javafx.scene.control.skin.TableColumnHeader
import javafx.application.Platform
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
import javafx.stage.Stage
import javafx.util.Callback
import javafx.util.StringConverter
import javafx.util.converter.*
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

fun Node.tooltip(text: String? = null, graphic: Node? = null, op: (Tooltip.() -> Unit)? = null) {
    val newToolTip = Tooltip(text)
    graphic?.apply { newToolTip.graphic = this }
    if (op != null) newToolTip.op()
    if (this is Control) tooltip = newToolTip else Tooltip.install(this, newToolTip)
}

fun Scene.reloadStylesheets() {
    val styles = stylesheets.toMutableList()
    stylesheets.clear()
    stylesheets.addAll(styles)
}

fun Scene.reloadViews() {
    findUIComponents().forEach { FX.replaceComponent(it) }
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
        childrenUnmodifiable.filtered { it is Parent }.forEach { (it as Parent).clearViews() }
    } else {
        childrenUnmodifiable.filtered { it is Parent }.forEach { (it as Parent).findUIComponents(list) }
    }
}

private fun Parent.clearViews() {
    val uicmp = uiComponent<UIComponent>()
    if (uicmp is View) {
        FX.components.remove(uicmp.javaClass.kotlin)
    } else {
        childrenUnmodifiable.filtered { it is Parent }.forEach { (it as Parent).clearViews() }
    }
}

fun Stage.reloadStylesheetsOnFocus() {
    focusedProperty().addListener { obs, old, focused ->
        if (focused && FX.initialized.value) scene?.reloadStylesheets()
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

infix fun Node.addTo(pane: Pane) = pane.children.add(this)

fun Pane.replaceChildren(vararg uiComponents: UIComponent) =
        this.replaceChildren(*(uiComponents.map { it.root }.toTypedArray()))

fun Pane.replaceChildren(vararg node: Node) {
    children.clear()
    children.addAll(node)
}

fun ToolBar.children(op: Pane.() -> Unit): ToolBar {
    val fake = Pane()
    op(fake)
    items.addAll(fake.children)
    return this
}

fun children(addTo: MutableList<Node>, op: Pane.() -> Unit) {
    val fake = Pane()
    op(fake)
    addTo.addAll(fake.children)
}

operator fun ToolBar.plusAssign(uiComponent: UIComponent): Unit {
    items.add(uiComponent.root)
}

operator fun ToolBar.plusAssign(node: Node): Unit {
    items.add(node)
}

fun ToolBar.add(node: Node) = plusAssign(node)

@JvmName("addView")
inline fun <reified T : View> ToolBar.add(type: KClass<T>): Unit = plusAssign(find(type))

@JvmName("addFragment")
inline fun <reified T : Fragment> ToolBar.add(type: KClass<T>): Unit = plusAssign(findFragment(type))

operator fun Pane.plusAssign(node: Node) {
    children.add(node)
}

@JvmName("addView")
inline fun <reified T : View> Pane.add(type: KClass<T>): Unit = plusAssign(find(type).root)

@JvmName("addFragment")
inline fun <reified T : Fragment> Pane.add(type: KClass<T>): Unit = plusAssign(findFragment(type).root)

fun Pane.add(node: Node) = plusAssign(node)

@JvmName("plusView")
operator fun <T : View> Pane.plusAssign(type: KClass<T>): Unit = plusAssign(find(type).root)

@JvmName("plusFragment")
operator fun <T : Fragment> Pane.plusAssign(type: KClass<T>) = plusAssign(findFragment(type).root)

operator fun Pane.plusAssign(view: UIComponent) {
    plusAssign(view.root)
}

operator fun UIComponent.plusAssign(view: UIComponent) {
    val r = root
    if (r is Pane)
        r += view
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


fun <T> TableView<T>.resizeColumnsToFitContent(resizeColumns: List<TableColumn<T, *>> = columns, maxRows: Int = 50) {
    val doResize = {
        val resizer = skin.javaClass.getDeclaredMethod("resizeColumnToFitContent", TableColumn::class.java, Int::class.java)
        resizer.isAccessible = true
        resizeColumns.forEach { resizer.invoke(skin, it, maxRows) }
    }
    if (skin == null) Platform.runLater { doResize() } else doResize()
}

fun <T> TreeTableView<T>.resizeColumnsToFitContent(resizeColumns: List<TreeTableColumn<T, *>> = columns, maxRows: Int = 50) {
    val doResize = {
        val resizer = skin.javaClass.getDeclaredMethod("resizeColumnToFitContent", TreeTableColumn::class.java, Int::class.java)
        resizer.isAccessible = true
        resizeColumns.forEach { resizer.invoke(skin, it, maxRows) }
    }
    if (skin == null) Platform.runLater { doResize() } else doResize()
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

val <T> ListView<T>.selectedItem: T?
    get() = selectionModel.selectedItem

val <T> ComboBox<T>.selectedItem: T?
    get() = selectionModel.selectedItem

fun <S> TableView<S>.onSelectionChange(func: (S?) -> Unit) =
        selectionModel.selectedItemProperty().addListener({ observable, oldValue, newValue -> func(newValue) })

fun <S, T> TableColumn<S, T>.fixedWidth(width: Double): TableColumn<S, T> {
    minWidth = width
    maxWidth = width
    return this
}

inline fun <S, T> TableColumn<S, T>.cellFormat(crossinline formatter: (TableCell<S, T>.(T) -> Unit)) {
    cellFactory = Callback { column: TableColumn<S, T> ->
        object : TableCell<S, T>() {
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

inline fun <S> TreeView<S>.cellFormat(crossinline formatter: (TreeCell<S>.(S) -> Unit)) {
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

inline fun <S, T> TreeTableColumn<S, T>.cellFormat(crossinline formatter: (TreeTableCell<S, T>.(T) -> Unit)) {
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

inline fun <T> ListView<T>.cellFormat(crossinline formatter: (ListCell<T>.(T) -> Unit)) {
    cellFactory = Callback {
        object : ListCell<T>() {
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

val <S, T> TableCell<S, T>.rowItem: S get() = tableView.items[index]

fun <T> TableView<T>.asyncItems(func: () -> ObservableList<T>) =
        task { func() } success { if (items == null) items = it else items.setAll(it) }

fun <T> ListView<T>.asyncItems(func: () -> ObservableList<T>) =
        task { func() } success { if (items == null) items = it else items.setAll(it) }

fun <T> ComboBox<T>.asyncItems(func: () -> ObservableList<T>) =
        task { func() } success { if (items == null) items = it else items.setAll(it) }

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

fun <T> ListView<T>.onUserDelete(action: (T) -> Unit) {
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
 * Execute action when the enter key is pressed or the mouse is clicked

 * @param clickCount The number of mouse clicks to trigger the action
 * *
 * @param action The runnable to execute on select
 */
fun <T> ListView<T>.onUserSelect(clickCount: Int = 2, action: (T) -> Unit) {
    addEventFilter(MouseEvent.MOUSE_CLICKED) { event ->
        if (event.clickCount == clickCount && selectedItem != null)
            action(selectedItem!!)
    }

    addEventFilter(KeyEvent.KEY_PRESSED) { event ->
        if (event.code == KeyCode.ENTER && !event.isMetaDown && selectedItem != null)
            action(selectedItem!!)
    }
}

/**
 * Did the event occur inside a TableRow?
 */
fun EventTarget.isInsideTableRow(): Boolean {
    if (this !is Node)
        return false

    if (this is TableColumnHeader)
        return false

    if (this is TableRow<*> || this is TableView<*>)
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

@Suppress("CAST_NEVER_SUCCEEDS")
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

fun <T> TreeTableView<T>.populate(itemFactory: (T) -> TreeItem<T> = { TreeItem(it) }, childFactory: (TreeItem<T>) -> List<T>?) =
        populateTree(root, itemFactory, childFactory)

fun <T> TreeView<T>.populate(itemFactory: (T) -> TreeItem<T> = { TreeItem(it) }, childFactory: (TreeItem<T>) -> List<T>?) =
        populateTree(root, itemFactory, childFactory)

/**
 * Add children to the given item by invoking the supplied childFactory function, which converts
 * a TreeItem&lt;T> to a List&lt;T>?.
 *
 * If the childFactory returns a non-empty list, each entry in the list is converted to a TreeItem&lt;T>
 * via the supplied itemFactory function. The default itemFactory from TreeTableView.populate and TreeTable.populate
 * simply wraps the given T in a TreeItem, but you can override it to add icons etc. Lastly, the populateTree
 * function is called for each of the generated child items.
 */
fun <T> populateTree(item: TreeItem<T>, itemFactory: (T) -> TreeItem<T>, childFactory: (TreeItem<T>) -> List<T>?) {
    childFactory.invoke(item)?.map { itemFactory.invoke(it) }?.apply {
        item.children.setAll(this)
        forEach { populateTree(it, itemFactory, childFactory) }
    }
}

/**
 * Return the UIComponent (View or Fragment) that owns this Parent
 */
inline fun <reified T : UIComponent> Parent.uiComponent(): T? = properties["tornadofx.uicomponent"]?.let {
    if (it is T) it else null
}

/**
 * Find all UIComponents of the specified type that owns any of this node's children
 */
inline fun <reified T : UIComponent> Parent.findAll(): List<T> = childrenUnmodifiable
        .filter { it is Parent }
        .map { it as Parent }
        .filter { it.uiComponent<UIComponent>() is T }
        .map { it.uiComponent<T>()!! }

/**
 * Find all UIComponents of the specified type that owns any of this UIComponent's root node's children
 */
inline fun <reified T : UIComponent> UIComponent.findAll(): List<T> = root.findAll()

/**
 * Find the first UIComponent of the specified type that owns any of this node's children
 */
inline fun <reified T : UIComponent> Parent.find(): T? = findAll<T>().getOrNull(0)

/**
 * Find the first UIComponent of the specified type that owns any of this UIComponent's root node's children
 */
inline fun <reified T : UIComponent> UIComponent.find(): T? = findAll<T>().getOrNull(0)