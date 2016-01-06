package tornadofx

import com.sun.javafx.scene.control.skin.TableColumnHeader
import javafx.event.EventTarget
import javafx.scene.Node
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

fun Pane.replaceChildren(vararg uiComponents: UIComponent) =
    this.replaceChildren(*(uiComponents.map { it.root }.toTypedArray()))

fun Pane.replaceChildren(vararg node: Node) {
    children.clear()
    children.addAll(node)
}

fun ToolBar.add(uiComponent: UIComponent) = items.add(uiComponent.root)
inline fun <reified T : UIComponent> ToolBar.add(type: KClass<T>) = add(find(type))

fun Pane.add(node: Node) = children.add(node)
inline fun <reified T : View> Pane.add(type: KClass<T>) = add(find(type).root)

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

fun <T> TableView<T>.selectFirst() = selectionModel.selectFirst()

val <T> ListView<T>.selectedItem: T
    get() = selectionModel.selectedItem

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

 * @param node The node to attach the event to
 * *
 * @param clickCount The number of mouse clicks to trigger the action
 * *
 * @param action The runnable to execute on select
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

/**
 * Execute action when the enter key is pressed or the mouse is clicked

 * @param node The node to attach the event to
 * *
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
