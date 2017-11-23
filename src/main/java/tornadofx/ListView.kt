package tornadofx

import javafx.beans.property.ObjectProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableMap
import javafx.scene.Node
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.SelectionMode
import javafx.scene.control.TableView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.util.Callback
import tornadofx.FX.IgnoreParentBuilder.No
import tornadofx.FX.IgnoreParentBuilder.Once
import kotlin.reflect.KClass


/**
 * Execute action when the enter key is pressed or the mouse is clicked
 * @param clickCount The number of mouse clicks to trigger the action
 * @param action The runnable to execute on select
 */
fun <T> ListView<T>.onUserSelect(clickCount: Int = 2, action: (T) -> Unit) {
    addEventFilter(MouseEvent.MOUSE_CLICKED) { event ->
        if (event.clickCount == clickCount && selectedItem != null && event.target.isInsideRow())
            action(selectedItem!!)
    }

    addEventFilter(KeyEvent.KEY_PRESSED) { event ->
        if (event.code == KeyCode.ENTER && !event.isMetaDown && selectedItem != null)
            action(selectedItem!!)
    }
}

val <T> ListView<T>.selectedItem: T?
    get() = selectionModel.selectedItem

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
    val itemProperty: ObjectProperty<T> = SimpleObjectProperty(this, "item")
    val item by itemProperty
}

abstract class RowItemFragment<S, T> : ItemFragment<T>() {
    val rowItemProperty: ObjectProperty<S> = SimpleObjectProperty(this, "rowItem")
    val rowItem by rowItemProperty
}

abstract class ListCellFragment<T> : ItemFragment<T>() {
    val cellProperty: ObjectProperty<ListCell<T>?> = SimpleObjectProperty()
    var cell by cellProperty
    val editingProperty = SimpleBooleanProperty(false)
    val editing by editingProperty

    open fun startEdit() {
        cell?.startEdit()
    }

    open fun commitEdit(newValue: T) {
        cell?.commitEdit(newValue)
    }

    open fun cancelEdit() {
        cell?.cancelEdit()
    }

    open fun onEdit(op: () -> Unit) {
        editingProperty.onChange { if (it) op() }
    }
}

@Suppress("UNCHECKED_CAST")
open class SmartListCell<T>(val scope: Scope = DefaultScope, listView: ListView<T>?, properties: Map<Any,Any>? = null) : ListCell<T>() {
    /**
     * A convenience constructor allowing to omit `listView` completely, if needed.
     */
    constructor(scope: Scope = DefaultScope, properties : Map<Any,Any>? = null) : this(scope, null, properties)

    private val smartProperties: ObservableMap<Any,Any> = listView?.properties ?: HashMap(properties.orEmpty()).observable()
    private val editSupport: (ListCell<T>.(EditEventType, T?) -> Unit)? get() = smartProperties["tornadofx.editSupport"] as (ListCell<T>.(EditEventType, T?) -> Unit)?
    private val cellFormat: (ListCell<T>.(T) -> Unit)? get() = smartProperties["tornadofx.cellFormat"] as (ListCell<T>.(T) -> Unit)?
    private val cellCache: ListCellCache<T>? get() = smartProperties["tornadofx.cellCache"] as ListCellCache<T>?
    private var cellFragment: ListCellFragment<T>? = null
    private var fresh = true

    init {
        if (listView != null) {
            properties?.let { listView.properties?.putAll(it) }
            listView.properties["tornadofx.cellFormatCapable"] = true
            listView.properties["tornadofx.cellCacheCapable"] = true
            listView.properties["tornadofx.editCapable"] = true
        }
        indexProperty().onChange {
            if (it == -1) clearCellFragment()
        }
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
            textProperty().unbind()
            graphicProperty().unbind()
            text = null
            graphic = null
            clearCellFragment()
        } else {
            FX.ignoreParentBuilder = Once
            try {
                cellCache?.apply { graphic = getOrCreateNode(item) }
            } finally {
                FX.ignoreParentBuilder = No
            }
            if (fresh) {
                val cellFragmentType = smartProperties["tornadofx.cellFragment"] as KClass<ListCellFragment<T>>?
                cellFragment = if (cellFragmentType != null) find(cellFragmentType, scope) else null
                fresh = false
            }
            cellFragment?.apply {
                editingProperty.cleanBind(editingProperty())
                itemProperty.value = item
                cellProperty.value = this@SmartListCell
                graphic = root
            }
            cellFormat?.invoke(this, item)
        }
    }

    private fun clearCellFragment() {
        cellFragment?.apply {
            cellProperty.value = null
            itemProperty.value = null
            editingProperty.unbind()
            editingProperty.value = false
        }
    }

}

fun <T> ListView<T>.bindSelected(property: Property<T>) {
    selectionModel.selectedItemProperty().onChange {
        property.value = it
    }
}

fun <T> ListView<T>.bindSelected(model: ItemViewModel<T>) = this.bindSelected(model.itemProperty)

fun <T, F : ListCellFragment<T>> ListView<T>.cellFragment(scope: Scope = DefaultScope, fragment: KClass<F>) {
    properties["tornadofx.cellFragment"] = fragment
    if (properties["tornadofx.cellFormatCapable"] != true)
        cellFactory = Callback { SmartListCell(scope, it) }
}

@Suppress("UNCHECKED_CAST")
fun <T> ListView<T>.cellFormat(scope: Scope = DefaultScope, formatter: (ListCell<T>.(T) -> Unit)) {
    properties["tornadofx.cellFormat"] = formatter
    if (properties["tornadofx.cellFormatCapable"] != true)
        cellFactory = Callback { SmartListCell(scope, it) }
}

fun <T> ListView<T>.onEdit(scope: Scope = DefaultScope, eventListener: ListCell<T>.(EditEventType, T?) -> Unit) {
    isEditable = true
    properties["tornadofx.editSupport"] = eventListener
    // Install a edit capable cellFactory it none is present. The default cellFormat factory will do.
    if (properties["tornadofx.editCapable"] != true) cellFormat(scope) { }
}

/**
 * Calculate a unique Node per item and set this Node as the graphic of the ListCell.
 *
 * To support this feature, a custom cellFactory is automatically installed, unless an already
 * compatible cellFactory is found. The cellFactories installed via #cellFormat already knows
 * how to retrieve cached values.
 */
fun <T> ListView<T>.cellCache(scope: Scope = DefaultScope, cachedGraphicProvider: (T) -> Node) {
    properties["tornadofx.cellCache"] = ListCellCache(cachedGraphicProvider)
    // Install a cache capable cellFactory it none is present. The default cellFormat factory will do.
    if (properties["tornadofx.cellCacheCapable"] != true) {
        cellFormat(scope) { }
    }
}

fun <T> ListView<T>.multiSelect(enable: Boolean = true) {
    selectionModel.selectionMode = if (enable) SelectionMode.MULTIPLE else SelectionMode.SINGLE
}
