package tornadofx

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.Node
import javafx.scene.control.SelectionMode
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.util.Callback
import javafx.util.StringConverter
import kotlin.reflect.KClass

abstract class TableCellFragment<S, T> : RowItemFragment<S, T>() {
    val cellProperty: ObjectProperty<TableCell<S, T>?> = SimpleObjectProperty()
    var cell by cellProperty

    val editingProperty = SimpleBooleanProperty(false)
    val editing by editingProperty

    open fun startEdit() {
    }

    open fun commitEdit(newValue: T) {
    }

    open fun cancelEdit() {
    }

    open fun onEdit(op: () -> Unit) {
        editingProperty.onChange { if (it) op() }
    }
}

@Suppress("UNCHECKED_CAST")
open class SmartTableCell<S, T>(val scope: Scope = DefaultScope, val owningColumn: TableColumn<S, T>) : TableCell<S, T>() {
    private val editSupport: (TableCell<S, T>.(EditEventType, T?) -> Unit)? get() = owningColumn.properties["tornadofx.editSupport"] as (TableCell<S, T>.(EditEventType, T?) -> Unit)?
    private val cellFormat: (TableCell<S, T>.(T) -> Unit)? get() = owningColumn.properties["tornadofx.cellFormat"] as (TableCell<S, T>.(T) -> Unit)?
    private val cellCache: TableColumnCellCache<T>? get() = owningColumn.properties["tornadofx.cellCache"] as TableColumnCellCache<T>?
    private var cellFragment: TableCellFragment<S, T>? = null
    private var fresh = true

    init {
        owningColumn.properties["tornadofx.cellFormatCapable"] = true
        owningColumn.properties["tornadofx.cellCacheCapable"] = true
        owningColumn.properties["tornadofx.editCapable"] = true
        indexProperty().onChange {
            if (it == -1) clearCellFragment()
        }
    }

    override fun startEdit() {
        super.startEdit()
        editSupport?.invoke(this, EditEventType.StartEdit, null)
        cellFragment?.startEdit()
    }

    override fun commitEdit(newValue: T) {
        super.commitEdit(newValue)
        editSupport?.invoke(this, EditEventType.CommitEdit, newValue)
        cellFragment?.commitEdit(newValue)
    }

    override fun cancelEdit() {
        super.cancelEdit()
        editSupport?.invoke(this, EditEventType.CancelEdit, null)
        cellFragment?.cancelEdit()
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
            FX.ignoreParentBuilder = FX.IgnoreParentBuilder.Once
            try {
                cellCache?.apply { graphic = getOrCreateNode(item) }
            } finally {
                FX.ignoreParentBuilder = FX.IgnoreParentBuilder.No
            }
            if (fresh) {
                val cellFragmentType = owningColumn.properties["tornadofx.cellFragment"] as KClass<TableCellFragment<S, T>>?
                cellFragment = if (cellFragmentType != null) find(cellFragmentType, scope) else null
                fresh = false
            }
            cellFragment?.apply {
                editingProperty.cleanBind(editingProperty())
                itemProperty.value = item
                rowItemProperty.value = tableView.items[index]
                cellProperty.value = this@SmartTableCell
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


fun <S, T> TableColumn<S, T>.cellFormat(scope: Scope = DefaultScope, formatter: TableCell<S, T>.(T) -> Unit) {
    properties["tornadofx.cellFormat"] = formatter
    if (properties["tornadofx.cellFormatCapable"] != true)
        cellFactory = Callback { SmartTableCell<S, T>(scope, it) }
}

fun <S, T, F: TableCellFragment<S, T>> TableColumn<S, T>.cellFragment(scope: Scope = DefaultScope, fragment: KClass<F>) {
    properties["tornadofx.cellFragment"] = fragment
    if (properties["tornadofx.cellFormatCapable"] != true)
        cellFactory = Callback { SmartTableCell<S, T>(scope, it) }
}

/**
 * Calculate a unique Node per item and set this Node as the graphic of the TableCell.
 *
 * To support this feature, a custom cellFactory is automatically installed, unless an already
 * compatible cellFactory is found. The cellFactories installed via #cellFormat already knows
 * how to retrieve cached values.
 */
fun <S, T> TableColumn<S, T>.cellCache(scope: Scope  = DefaultScope, cachedGraphicProvider: (T) -> Node) {
    properties["tornadofx.cellCache"] = TableColumnCellCache(cachedGraphicProvider)
    // Install a cache capable cellFactory it none is present. The default cellFormat factory will do.
    if (properties["tornadofx.cellCacheCapable"] != true)
        cellFactory = Callback { SmartTableCell<S, T>(scope, it) }
}


fun <T, S> TableColumn<T, S?>.converter(converter: StringConverter<in S>): TableColumn<T, S?> = apply {
    cellFormat(DefaultScope) { text = converter.toString(it) }
}

fun <T> TableView<T>.multiSelect(enable: Boolean = true) {
    selectionModel.selectionMode = if (enable) SelectionMode.MULTIPLE else SelectionMode.SINGLE
}