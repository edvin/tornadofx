package tornadofx

import com.sun.javafx.scene.control.skin.TableColumnHeader
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.InputEvent
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.GridPane
import javafx.scene.layout.Pane
import javafx.util.Callback
import kotlin.reflect.KClass

fun Node.hasClass(className: String) = styleClass.contains(className)
fun Node.addClass(className: String) = styleClass.add(className)
fun Node.removeClass(className: String) = styleClass.remove(className)
fun Node.toggleClass(className: String, predicate: Boolean) = if (predicate) removeClass(className) else addClass(className)

fun Scene.reloadStylesheets() {
    val styles = stylesheets.toArrayList()
    stylesheets.clear()
    stylesheets.addAll(styles)
}

fun Pane.reloadStylesheets() {
    val styles = stylesheets.toArrayList()
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

operator fun ToolBar.plusAssign(uiComponent: UIComponent): Unit {
    items.add(uiComponent.root)
}

inline fun <reified T : UIComponent> ToolBar.add(type: KClass<T>): Unit = plusAssign(find(type))

operator fun Pane.plusAssign(node: Node) {
    children.add(node)
}

inline fun <reified T : View> Pane.add(type: KClass<T>) = plusAssign(find(type).root)

operator fun <T : View> Pane.plusAssign(type: KClass<T>) = plusAssign(find(type).root)

operator fun Pane.plusAssign(view: UIComponent): Unit {
    plusAssign(view.root)
}

fun GridPane.row(op: Pane.() -> Unit) {
    userData = if (userData is Int) userData as Int + 1 else 1

    // Allow the caller to add children to a fake pane
    val fake = Pane()
    op(fake)

    // Create a new row in the GridPane and add the children added to the fake pane
    addRow(userData as Int, *fake.children.toTypedArray())
}

val <T> TableView<T>.selectedItem: T
    get() = this.selectionModel.selectedItem

val <T> TreeView<T>.selectedValue: T
    get() = this.selectionModel.selectedItem.value

fun <T> TableView<T>.selectFirst() = selectionModel.selectFirst()

fun <T> TreeView<T>.selectFirst() = selectionModel.selectFirst()

val <T> ListView<T>.selectedItem: T
    get() = selectionModel.selectedItem

fun <S> TableView<S>.onSelectionChange(func: (S?) -> Unit) =
    selectionModel.selectedItemProperty().addListener({ observable, oldValue, newValue -> func(newValue) })

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

fun <T> TreeView<T>.onUserSelect(clickCount: Int = 1, action: (T) -> Unit) {
    val isSelected = { event: InputEvent -> !selectionModel.isEmpty }

    addEventFilter(MouseEvent.MOUSE_CLICKED) { event ->
        if (event.clickCount == clickCount && isSelected(event))
            action(selectedValue)
    }

    addEventFilter(KeyEvent.KEY_PRESSED) { event ->
        if (event.code == KeyCode.ENTER && !event.isMetaDown && isSelected(event))
            action(selectedValue)
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
