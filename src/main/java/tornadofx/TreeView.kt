package tornadofx

import javafx.beans.property.*
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.util.Callback
import tornadofx.FX.IgnoreParentBuilder.Once
import kotlin.reflect.KClass

/**
 * Base class for all TreeCellFragments.
 */
abstract class TreeCellFragment<T> : ItemFragment<T>() {

    val cellProperty: ObjectProperty<TreeCell<T>?> = SimpleObjectProperty()
    var cell by cellProperty

    val editingProperty = SimpleBooleanProperty(false)
    val editing by editingProperty

    open fun startEdit() { cell?.startEdit() }

    open fun commitEdit(newValue: T) { cell?.commitEdit(newValue) }

    open fun cancelEdit() { cell?.cancelEdit() }

    open fun onEdit(op: () -> Unit) { editingProperty.onChange { if (it) op() } }
}

open class SmartTreeCell<T>(val scope: Scope = DefaultScope, treeView: TreeView<T>?): TreeCell<T>() {

    private val editSupport: (TreeCell<T>.(EditEventType, T?) -> Unit)? get() = treeView.properties["tornadofx.editSupport"] as (TreeCell<T>.(EditEventType, T?) -> Unit)?
    private val cellFormat: (TreeCell<T>.(T) -> Unit)? get() = treeView.properties["tornadofx.cellFormat"] as (TreeCell<T>.(T) -> Unit)?
    private val cellCache: TreeCellCache<T>? get() = treeView.properties["tornadofx.cellCache"] as TreeCellCache<T>?
    private var cellFragment: TreeCellFragment<T>? = null
    private var fresh = true

    init {
        if (treeView != null) {
            treeView.properties["tornadofx.cellFormatCapable"] = true
            treeView.properties["tornadofx.cellCacheCapable"] = true
            treeView.properties["tornadofx.editCapable"] = true
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
            cleanUp()
            clearCellFragment()
        } else {
            FX.ignoreParentBuilder = Once
            try {
                cellCache?.apply { graphic = getOrCreateNode(item) }
            } finally {
                FX.ignoreParentBuilder = FX.IgnoreParentBuilder.No
            }
            if (fresh) {
                val cellFragmentType = treeView.properties["tornadofx.cellFragment"] as KClass<TreeCellFragment<T>>?
                cellFragment = if (cellFragmentType != null) find(cellFragmentType, scope) else null
                fresh = false
            }
            cellFragment?.apply {
                editingProperty.cleanBind(editingProperty())
                itemProperty.value = item
                cellProperty.value = this@SmartTreeCell
                graphic = root
            }
            cellFormat?.invoke(this, item)
        }
    }

    private fun cleanUp() {
        textProperty().unbind()
        graphicProperty().unbind()
        text = null
        graphic = null
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

class TreeCellCache<T>(private val cacheProvider: (T) -> Node) {
    private val store = mutableMapOf<T, Node>()
    fun getOrCreateNode(value: T) = store.getOrPut(value, { cacheProvider(value) })
}

fun <T> TreeView<T>.bindSelected(property: Property<T>) {
    selectionModel.selectedItemProperty().onChange { property.value = it?.value }
}

/**
 * Binds the currently selected object of type [T] in the given [TreeView] to the corresponding [ItemViewModel].
 */
fun <T> TreeView<T>.bindSelected(model: ItemViewModel<T>) = this.bindSelected(model.itemProperty)


fun <T> TreeView<T>.onUserDelete(action: (T) -> Unit) {
    addEventFilter(KeyEvent.KEY_PRESSED, { event ->
        if (event.code == KeyCode.BACK_SPACE && selectionModel.selectedItem?.value != null)
            action(selectedValue!!)
    })
}

fun <T> TreeView<T>.onUserSelect(action: (T) -> Unit) {
    selectionModel.selectedItemProperty().addListener { obs, old, new ->
        if (new != null && new.value != null)
            action(new.value)
    }
}


/**
 * <p>This method will attempt to select the first index in the control.
 * If clearSelection is not called first, this method
 * will have the result of selecting the first index, whilst retaining
 * the selection of any other currently selected indices.</p>
 *
 * <p>If the first index is already selected, calling this method will have
 * no result, and no selection event will take place.</p>
 *
 * This functions is the same as calling.
 * ```
 * selectionModel.selectFirst()
 *
 * ```
 */
fun <T> TreeView<T>.selectFirst() = selectionModel.selectFirst()

fun <T> TreeView<T>.populate(itemFactory: (T) -> TreeItem<T> = { TreeItem(it) }, childFactory: (TreeItem<T>) -> Iterable<T>?) =
        populateTree(root, itemFactory, childFactory)

/**
 * Registers a `Fragment` which should be used to represent a [TreeItem] for the given [TreeView].
 */
fun <T, F : TreeCellFragment<T>> TreeView<T>.cellFragment(scope: Scope = DefaultScope, fragment: KClass<F>) {
    properties["tornadofx.cellFragment"] = fragment
    if (properties["tornadofx.cellFormatCapable"] != true)
        cellFactory = Callback { SmartTreeCell(scope, it) }
}

fun <S> TreeView<S>.cellFormat(scope: Scope = DefaultScope, formatter: (TreeCell<S>.(S) -> Unit)) {
    properties["tornadofx.cellFormat"] = formatter
    if (properties["tornadofx.cellFormatCapable"] != true) {
        cellFactory = Callback { SmartTreeCell(scope, it) }
    }
}

fun <S> TreeView<S>.cellDecorator(decorator: (TreeCell<S>.(S) -> Unit)) {
    val originalFactory = cellFactory

    if (originalFactory == null) cellFormat(formatter = decorator) else {
        cellFactory = Callback { treeView: TreeView<S> ->
            val cell = originalFactory.call(treeView)
            cell.itemProperty().onChange { decorator(cell, cell.item) }
            cell
        }
    }
}

// -- Properties

/**
 * Returns the currently selected value of type [T] (which is currently the
 * selected value represented by the current selection model). If there
 * are multiple values selected, it will return the most recently selected
 * value.
 *
 * <p>Note that the returned value is a snapshot in time.
 */
val <T> TreeView<T>.selectedValue: T?
    get() = this.selectionModel.selectedItem?.value

fun <T> TreeView<T>.multiSelect(enable: Boolean = true) {
    selectionModel.selectionMode = if (enable) SelectionMode.MULTIPLE else SelectionMode.SINGLE
}

fun <T> TreeTableView<T>.multiSelect(enable: Boolean = true) {
    selectionModel.selectionMode = if (enable) SelectionMode.MULTIPLE else SelectionMode.SINGLE
}