package tornadofx

import com.sun.javafx.scene.control.skin.TableColumnHeader
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.InputEvent
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.*
import javafx.stage.Stage
import javafx.util.Callback
import kotlin.reflect.KClass

fun Node.hasClass(className: String) = styleClass.contains(className)
fun Node.addClass(className: String) = styleClass.add(className)
fun Node.removeClass(className: String) = styleClass.remove(className)
fun Node.toggleClass(className: String, predicate: Boolean) = if (predicate) addClass(className) else removeClass(className)

fun Scene.reloadStylesheets() {
    val styles = stylesheets.toMutableList()
    stylesheets.clear()
    stylesheets.addAll(styles)
}

fun Stage.reloadStylesheetsOnFocus() {
    focusedProperty().addListener { obs, old, new ->
        if (new)
            scene.reloadStylesheets()
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

operator fun ToolBar.plusAssign(uiComponent: UIComponent): Unit {
    items.add(uiComponent.root)
}

operator fun ToolBar.plusAssign(node: Node): Unit {
    items.add(node)
}

fun ToolBar.add(node: Node) = plusAssign(node)

inline fun <reified T : UIComponent> ToolBar.add(type: KClass<T>): Unit = plusAssign(find(type))

operator fun Pane.plusAssign(node: Node) {
    children.add(node)
}

inline fun <reified T : View> Pane.add(type: KClass<T>) = plusAssign(find(type).root)

fun Pane.add(node: Node) = plusAssign(node)

operator fun <T : UIComponent> Pane.plusAssign(type: KClass<T>) = plusAssign(find(type).root)

operator fun Pane.plusAssign(view: UIComponent): Unit {
    plusAssign(view.root)
}

val <T> TableView<T>.selectedItem: T
    get() = this.selectionModel.selectedItem

val <T> TreeView<T>.selectedValue: T
    get() = this.selectionModel.selectedItem.value

fun <T> TableView<T>.selectFirst() = selectionModel.selectFirst()

fun <T> TreeView<T>.selectFirst() = selectionModel.selectFirst()

val <T> ListView<T>.selectedItem: T
    get() = selectionModel.selectedItem

val <T> ComboBox<T>.selectedItem: T
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

inline fun <S, T> TableView<S>.addColumn(title: String, crossinline valueProvider: (TableColumn.CellDataFeatures<S, T>) -> ObservableValue<T>): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { valueProvider(it) }
    columns.add(column)
    return column
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
            action(selectedItem)
    }

    addEventFilter(KeyEvent.KEY_PRESSED) { event ->
        if (event.code == KeyCode.ENTER && !event.isMetaDown && isSelected(event))
            action(selectedItem)
    }
}

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
            action(selectedItem)
    })
}

fun <T> ListView<T>.onUserDelete(action: (T) -> Unit) {
    addEventFilter(KeyEvent.KEY_PRESSED, { event ->
        if (event.code == KeyCode.BACK_SPACE && selectedItem != null)
            action(selectedItem)
    })
}

fun <T> TreeView<T>.onUserDelete(action: (T) -> Unit) {
    addEventFilter(KeyEvent.KEY_PRESSED, { event ->
        if (event.code == KeyCode.BACK_SPACE && selectionModel.selectedItem?.value != null)
            action(selectedValue)
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
            action(selectedItem)
    }

    addEventFilter(KeyEvent.KEY_PRESSED) { event ->
        if (event.code == KeyCode.ENTER && !event.isMetaDown && selectedItem != null)
            action(selectedItem)
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
        var margin: Insets? = null,
        var fillHeight: Boolean? = null,
        var fillWidth: Boolean? = null,
        var hAlignment: HPos? = null,
        var vAlignment: VPos? = null
) {
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
        margin?.let { GridPane.setMargin(node, it) }
        fillHeight?.let { GridPane.setFillHeight(node, it) }
        fillWidth?.let { GridPane.setFillWidth(node, it) }
        hAlignment?.let { GridPane.setHalignment(node, it) }
        vAlignment?.let { GridPane.setValignment(node, it) }
        return node
    }
}

fun <T : Node> T.vboxConstraints(op: (VBoxConstraint.() -> Unit)): T {
    val c = VBoxConstraint()
    c.op()
    return c.applyToNode(this)
}

class VBoxConstraint(
        var margin: Insets? = null,
        var vGrow: Priority? = null
) {
    fun <T : Node> applyToNode(node: T): T {
        margin?.let { VBox.setMargin(node, it) }
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
        var margin: Insets? = null,
        var hGrow: Priority? = null
) {
    fun <T : Node> applyToNode(node: T): T {
        margin?.let { HBox.setMargin(node, it) }
        hGrow?.let { HBox.setHgrow(node, it) }
        return node
    }
}