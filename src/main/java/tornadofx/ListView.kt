package tornadofx

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.scene.Node
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.util.Callback
import kotlin.reflect.KClass

fun <T> ListView<T>.onEdit(eventListener: ListCell<T>.(EditEventType, T?) -> Unit) {
    isEditable = true
    properties["tornadofx.editSupport"] = eventListener
    // Install a edit capable cellFactory it none is present. The default cellFormat factory will do.
    if (properties["tornadofx.editCapable"] != true) cellFormat { }
}

/**
 * Calculate a unique Node per item and set this Node as the graphic of the ListCell.
 *
 * To support this feature, a custom cellFactory is automatically installed, unless an already
 * compatible cellFactory is found. The cellFactories installed via #cellFormat already knows
 * how to retrieve cached values.
 */
fun <T> ListView<T>.cellCache(cachedGraphicProvider: (T) -> Node) {
    properties["tornadofx.cellCache"] = ListCellCache(cachedGraphicProvider)
    // Install a cache capable cellFactory it none is present. The default cellFormat factory will do.
    if (properties["tornadofx.cellCacheCapable"] != true) {
        cellFormat {  }
    }
}


/**
 * Execute action when the enter key is pressed or the mouse is clicked
 * @param clickCount The number of mouse clicks to trigger the action
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

val <T> ListView<T>.selectedItem: T?
    get() = selectionModel.selectedItem

@Suppress("UNCHECKED_CAST")
fun <T> ListView<T>.cellFormat(formatter: (ListCell<T>.(T) -> Unit)) {
    properties["tornadofx.cellFormat"] = formatter
    if (properties["tornadofx.cellFormatCapable"] != true)
        cellFactory = Callback { SmartListCell(this@cellFormat) }
}

fun <T> ListView<T>.asyncItems(func: () -> Collection<T>) =
        task { func() } success { if (items == null) items = FXCollections.observableArrayList(it) else items.setAll(it) }

fun <T> ListView<T>.onUserDelete(action: (T) -> Unit) {
    addEventFilter(KeyEvent.KEY_PRESSED, { event ->
        if (event.code == KeyCode.BACK_SPACE && selectedItem != null)
            action(selectedItem!!)
    })
}

class ListCellCache<T>(private val cacheProvider: (T) -> Node) {
    private val store = mutableMapOf<T, Node>()
    fun getOrCreateNode(value: T) = store.getOrPut(value, { cacheProvider(value) })
}

abstract class ItemFragment<T> : Fragment() {
    val itemProperty: ObjectProperty<T> = SimpleObjectProperty()
    var item by itemProperty
}

abstract class ListCellFragment<T> : ItemFragment<T>() {
    val cellProperty: ObjectProperty<ListCell<T>> = SimpleObjectProperty()
    var cell by cellProperty
}

fun <T, F: ItemFragment<T>> ListView<T>.cellFragment(fragment: KClass<F>) {
    properties["tornadofx.cellFragment"] = fragment
    if (properties["tornadofx.cellFormatCapable"] != true)
        cellFactory = Callback { SmartListCell(this@cellFragment) }
}

@Suppress("UNCHECKED_CAST")
open class SmartListCell<T>(listView: ListView<T>) : ListCell<T>() {
    val editSupport: (ListCell<T>.(EditEventType, T?) -> Unit)? get() = listView.properties["tornadofx.editSupport"] as (ListCell<T>.(EditEventType, T?) -> Unit)?
    val cellFormat: (ListCell<T>.(T) -> Unit)? get() = listView.properties["tornadofx.cellFormat"] as (ListCell<T>.(T) -> Unit)?
    val cellCache: ListCellCache<T>? get() = listView.properties["tornadofx.cellCache"] as ListCellCache<T>?
    val cellFragment: KClass<ItemFragment<T>>? = listView.properties["tornadofx.cellFragment"] as KClass<ItemFragment<T>>?

    init {
        listView.properties["tornadofx.cellFormatCapable"] = true
        listView.properties["tornadofx.cellCacheCapable"] = true
        listView.properties["tornadofx.editCapable"] = true
    }

    override fun startEdit() {
        super.startEdit()
        editSupport?.invoke(this, EditEventType.StartEdit, null)
    }

    override fun commitEdit(newValue: T) {
        super.commitEdit(newValue)
        editSupport?.invoke(this, EditEventType.CommitEdit, newValue)
    }

    override fun cancelEdit() {
        super.cancelEdit()
        editSupport?.invoke(this, EditEventType.CancelEdit, null)
    }

    override fun updateItem(item: T, empty: Boolean) {
        super.updateItem(item, empty)

        if (item == null || empty) {
            with(textProperty()) {
                if (isBound) unbind()
                value = null
            }
            with(graphicProperty()) {
                if (isBound) unbind()
                value = null
            }
        } else {
            cellCache?.apply { graphic = getOrCreateNode(item) }
            cellFragment?.apply {
                val fragment = find(this)
                fragment.itemProperty.value = item
                if (fragment is ListCellFragment<*>)
                    fragment.cellProperty.value = this@SmartListCell
                graphic = fragment.root
            }
            cellFormat?.invoke(this, item)
        }
    }

}