package tornadofx

import com.sun.javafx.scene.control.skin.TableRowSkin
import javafx.beans.binding.Bindings
import javafx.beans.property.*
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.cell.*
import javafx.scene.layout.StackPane
import javafx.scene.text.Text
import javafx.util.Callback
import javafx.util.StringConverter
import tornadofx.ResizeType.*
import java.util.*
import java.util.concurrent.Callable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

fun <T> EventTarget.spinner(editable: Boolean = false, op: (Spinner<T>.() -> Unit)? = null): Spinner<T> {
    val spinner = Spinner<T>()
    spinner.isEditable = editable
    return opcr(this, spinner, op)
}

fun EventTarget.spinner(min: Int, max: Int, initialValue: Int, amountToStepBy: Int = 1, editable: Boolean = false, op: (Spinner<Int>.() -> Unit)? = null): Spinner<Int> {
    val spinner = Spinner<Int>(min, max, initialValue, amountToStepBy)
    spinner.isEditable = editable
    return opcr(this, spinner, op)
}

fun EventTarget.spinner(min: Double, max: Double, initialValue: Double, amountToStepBy: Double = 1.0, editable: Boolean = false, op: (Spinner<Double>.() -> Unit)? = null): Spinner<Double> {
    val spinner = Spinner<Double>(min, max, initialValue, amountToStepBy)
    spinner.isEditable = editable
    return opcr(this, spinner, op)
}

fun <T> EventTarget.spinner(items: ObservableList<T>, editable: Boolean = false, op: (Spinner<T>.() -> Unit)? = null): Spinner<T> {
    val spinner = Spinner<T>(items)
    spinner.isEditable = editable
    return opcr(this, spinner, op)
}

fun <T> EventTarget.spinner(valueFactory: SpinnerValueFactory<T>, editable: Boolean = false, op: (Spinner<T>.() -> Unit)? = null): Spinner<T> {
    val spinner = Spinner<T>(valueFactory)
    spinner.isEditable = editable
    return opcr(this, spinner, op)
}

fun <T> EventTarget.combobox(property: Property<T>? = null, values: List<T>? = null, op: (ComboBox<T>.() -> Unit)? = null) = opcr(this, ComboBox<T>().apply {
    if (values != null) items = if (values is ObservableList<*>) values as ObservableList<T> else values.observable()
    if (property != null) valueProperty().bindBidirectional(property)
}, op)

fun <T> EventTarget.choicebox(values: ObservableList<T>? = null, changeListener: ((ObservableValue<out T>, T?, T?) -> Unit)? = null, op: (ChoiceBox<T>.() -> Unit)? = null) = opcr(this, ChoiceBox<T>().apply {
    if (values != null) items = values
    if (changeListener != null) selectionModel.selectedItemProperty().addListener(changeListener)
}, op)

fun <T> EventTarget.listview(values: ObservableList<T>? = null, op: (ListView<T>.() -> Unit)? = null) = opcr(this, ListView<T>().apply {
    if (values != null) {
        if (values is SortedFilteredList<T>) values.bindTo(this)
        else items = values
    }
}, op)

fun <T> EventTarget.listview(values: ObservableValue<ObservableList<T>>, op: (ListView<T>.() -> Unit)? = null) = opcr(this, ListView<T>().apply {
    fun rebinder() {
        if (items is SortedFilteredList<*>)
            (items as SortedFilteredList<T>).bindTo(this)
    }
    itemsProperty().bind(values)
    rebinder()
    itemsProperty().onChange {
        rebinder()
    }
}, op)

fun <T> EventTarget.tableview(items: ObservableList<T>? = null, op: (TableView<T>.() -> Unit)? = null): TableView<T> {
    val tableview = TableView<T>()
    if (items != null) {
        if (items is SortedFilteredList<T>) items.bindTo(tableview)
        else tableview.items = items
    }
    return opcr(this, tableview, op)
}

fun <T> EventTarget.tableview(items: ObservableValue<ObservableList<T>>, op: (TableView<T>.() -> Unit)? = null): TableView<T> {
    val tableview = TableView<T>()
    fun rebinder() {
        if (tableview.items is SortedFilteredList<*>)
            (tableview.items as SortedFilteredList<T>).bindTo(tableview)
    }
    tableview.itemsProperty().bind(items)
    rebinder()
    tableview.itemsProperty().onChange {
        rebinder()
    }
    return opcr(this, tableview, op)
}

fun <T> EventTarget.treeview(root: TreeItem<T>? = null, op: (TreeView<T>.() -> Unit)? = null): TreeView<T> {
    val treeview = TreeView<T>()
    if (root != null) treeview.root = root
    return opcr(this, treeview, op)
}

fun <T> EventTarget.treetableview(root: TreeItem<T>? = null, op: (TreeTableView<T>.() -> Unit)? = null): TreeTableView<T> {
    val treetableview = TreeTableView<T>()
    if (root != null) treetableview.root = root
    return opcr(this, treetableview, op)
}

fun <T : Any> TreeView<T>.lazyPopulate(
        leafCheck: (LazyTreeItem<T>) -> Boolean = { it.childFactoryReturnedNull() },
        itemProcessor: ((LazyTreeItem<T>) -> Unit)? = null,
        childFactory: (TreeItem<T>) -> List<T>?
) {
    fun createItem(value: T) = LazyTreeItem(value, leafCheck, itemProcessor, childFactory).apply { itemProcessor?.invoke(this) }

    if (root == null) throw IllegalArgumentException("You must set a root TreeItem before calling lazyPopulate")
    val rootChildren = childFactory.invoke(root)
    if (rootChildren != null) root.children.setAll(rootChildren.map(::createItem))
}

class LazyTreeItem<T : Any>(
        value: T,
        val leafCheck: (LazyTreeItem<T>) -> Boolean,
        val itemProcessor: ((LazyTreeItem<T>) -> Unit)? = null,
        val childFactory: (TreeItem<T>) -> List<T>?
) : TreeItem<T>(value) {
    var leafResult: Boolean? = null
    var childFactoryInvoked = false
    var childFactoryResult: List<TreeItem<T>>? = null

    override fun isLeaf(): Boolean {
        if (leafResult == null)
            leafResult = leafCheck(this)
        return leafResult!!
    }

    override fun getChildren(): ObservableList<TreeItem<T>> {
        if (!childFactoryInvoked) {
            task {
                invokeChildFactorySynchronously()
            } success {
                if (childFactoryResult != null)
                    super.getChildren().setAll(childFactoryResult)
            }
        }
        return super.getChildren()
    }

    fun childFactoryReturnedNull() = invokeChildFactorySynchronously() == null

    private fun invokeChildFactorySynchronously(): List<TreeItem<T>>? {
        if (!childFactoryInvoked) {
            childFactoryInvoked = true
            childFactoryResult = childFactory(this)?.map { LazyTreeItem(it, leafCheck, itemProcessor, childFactory).apply { itemProcessor?.invoke(this) } }
            if (childFactoryResult != null)
                super.getChildren().setAll(childFactoryResult)
        }
        return childFactoryResult
    }
}

fun <T> TreeItem<T>.treeitem(value: T? = null, op: TreeItem<T>.() -> Unit = {}): TreeItem<T> {
    val treeItem = value?.let { TreeItem<T>(it) } ?: TreeItem<T>()
    treeItem.op()
    this += treeItem
    return treeItem
}

operator fun <T> TreeItem<T>.plusAssign(treeItem: TreeItem<T>) {
    this.children.add(treeItem)
}

fun <S> TableView<S>.makeIndexColumn(name: String = "#", startNumber: Int = 1): TableColumn<S, Number> {
    return TableColumn<S, Number>(name).apply {
        isSortable = false
        prefWidth = width
        this@makeIndexColumn.columns += this
        setCellValueFactory { ReadOnlyObjectWrapper(items.indexOf(it.value) + startNumber) }
    }
}

fun <S, T> TableColumn<S, T>.enableTextWrap(): TableColumn<S, T> {
    setCellFactory {
        TableCell<S, T>().apply {
            val text = Text()
            graphic = text
            prefHeight = Control.USE_COMPUTED_SIZE
            text.wrappingWidthProperty().bind(this@enableTextWrap.widthProperty().subtract(Bindings.multiply(2.0, graphicTextGapProperty())))
            text.textProperty().bind(Bindings.createStringBinding(Callable {
                itemProperty().get()?.toString() ?: ""
            }, itemProperty()))
        }
    }
    return this
}

@Suppress("UNCHECKED_CAST")
fun <S> TableView<S>.addColumnInternal(column: TableColumn<S, *>, index: Int? = null) {
    val columnTarget = properties["tornadofx.columnTarget"] as? ObservableList<TableColumn<S, *>> ?: columns
    if (index == null) columnTarget.add(column) else columnTarget.add(index, column)
}

/**
 * Create a column holding children columns
 */
@Suppress("UNCHECKED_CAST")
fun <S> TableView<S>.nestedColumn(title: String, op: (TableView<S>.() -> Unit)? = null): TableColumn<S, Any?> {
    val column = TableColumn<S, Any?>(title)
    addColumnInternal(column)
    val previousColumnTarget = properties["tornadofx.columnTarget"] as? ObservableList<TableColumn<S, *>>
    properties["tornadofx.columnTarget"] = column.columns
    op?.invoke(this)
    properties["tornadofx.columnTarget"] = previousColumnTarget
    return column
}

/**
 * Create a column using the propertyName of the attribute you want shown.
 */
fun <S, T> TableView<S>.column(title: String, propertyName: String, op: (TableColumn<S, T>.() -> Unit)? = null): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = PropertyValueFactory<S, T>(propertyName)
    addColumnInternal(column)
    op?.invoke(column)
    return column
}

fun <S, T> TableColumn<S, T?>.useComboBox(items: ObservableList<T>, afterCommit: ((TableColumn.CellEditEvent<S, T?>) -> Unit)? = null): TableColumn<S, T?> {
    cellFactory = ComboBoxTableCell.forTableColumn(items)
    setOnEditCommit {
        val property = it.tableColumn.getCellObservableValue(it.rowValue) as Property<T?>
        property.value = it.newValue
        afterCommit?.invoke(it)
    }
    return this
}

inline fun <S, reified T> TableColumn<S, T?>.useTextField(converter: StringConverter<T>? = null, noinline afterCommit: ((TableColumn.CellEditEvent<S, T?>) -> Unit)? = null): TableColumn<S, T?> {
    when (T::class) {
        String::class -> {
            @Suppress("UNCHECKED_CAST")
            val stringColumn = this as TableColumn<S, String?>
            stringColumn.cellFactory = TextFieldTableCell.forTableColumn()
        }
        else -> {
            if (converter == null)
                throw IllegalArgumentException("You must supply a converter for non String columns")
            cellFactory = TextFieldTableCell.forTableColumn(converter)
        }
    }

    setOnEditCommit {
        val property = it.tableColumn.getCellObservableValue(it.rowValue) as ObjectProperty<T?>
        property.value = it.newValue
        afterCommit?.invoke(it)
    }
    return this
}

fun <S, T> TableColumn<S, T?>.useChoiceBox(items: ObservableList<T>, afterCommit: ((TableColumn.CellEditEvent<S, T?>) -> Unit)? = null): TableColumn<S, T?> {
    cellFactory = ChoiceBoxTableCell.forTableColumn(items)
    setOnEditCommit {
        val property = it.tableColumn.getCellObservableValue(it.rowValue) as ObjectProperty<T?>
        property.value = it.newValue
        afterCommit?.invoke(it)
    }
    return this
}

fun <S> TableColumn<S, Double?>.useProgressBar(afterCommit: ((TableColumn.CellEditEvent<S, Double?>) -> Unit)? = null): TableColumn<S, Double?> {
    cellFactory = ProgressBarTableCell.forTableColumn()
    setOnEditCommit {
        val property = it.tableColumn.getCellObservableValue(it.rowValue) as ObjectProperty<Double?>
        property.value = it.newValue
        afterCommit?.invoke(it)
    }
    return this
}

fun <S> TableColumn<S, Boolean?>.useCheckbox(editable: Boolean = true): TableColumn<S, Boolean?> {
    setCellFactory { CheckBoxCell(editable) }
    return this
}

class CheckBoxCell<S>(val editable: Boolean) : TableCell<S, Boolean?>() {
    override fun updateItem(item: Boolean?, empty: Boolean) {
        super.updateItem(item, empty)
        style { alignment = Pos.CENTER }

        if (empty || item == null) {
            graphic = null
        } else {
            graphic = CheckBox().apply {
                val model = tableView.items[index]
                val prop = tableColumn.cellValueFactory.call(TableColumn.CellDataFeatures(tableView, tableColumn, model)) as ObjectProperty
                isEditable = editable

                if (editable) {
                    selectedProperty().bindBidirectional(prop)
                } else {
                    disableProperty().set(true)
                    selectedProperty().bind(prop)
                }
            }
        }
    }
}

fun <T> TableView<T>.bindSelected(property: Property<T>) {
    property.bind(selectionModel.selectedItemProperty())
}

fun <T> TableView<T>.bindSelected(model: ItemViewModel<T>) {
    model.itemProperty.bind(selectionModel.selectedItemProperty())
}


/**
 * Create a column with a value factory that extracts the value from the given mutable
 * property and converts the property to an observable value.
 */
inline fun <reified S, T> TableView<S>.column(title: String, prop: KMutableProperty1<S, T>, noinline op: (TableColumn<S, T>.() -> Unit)? = null): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { observable(it.value, prop) }
    addColumnInternal(column)
    op?.invoke(column)
    return column
}

inline fun <reified S, T> TreeTableView<S>.column(title: String, prop: KMutableProperty1<S, T>, noinline op: (TreeTableColumn<S, T>.() -> Unit)? = null): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = Callback { observable(it.value.value, prop) }
    columns.add(column)
    op?.invoke(column)
    return column
}

/**
 * Create a column with a value factory that extracts the value from the given property and
 * converts the property to an observable value.
 */
inline fun <reified S, T> TableView<S>.column(title: String, prop: KProperty1<S, T>, noinline op: (TableColumn<S, T>.() -> Unit)? = null): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { observable(it.value, prop) }
    addColumnInternal(column)
    op?.invoke(column)
    return column
}

inline fun <reified S, T> TreeTableView<S>.column(title: String, prop: KProperty1<S, T>, noinline op: (TreeTableColumn<S, T>.() -> Unit)? = null): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = Callback { observable(it.value.value, prop) }
    columns.add(column)
    op?.invoke(column)
    return column
}

/**
 * Create a column with a value factory that extracts the value from the given ObservableValue property.
 */
@JvmName(name = "columnForObservableProperty")
inline fun <reified S, T> TableView<S>.column(title: String, prop: KProperty1<S, ObservableValue<T>>, noinline op: (TableColumn<S, T>.() -> Unit)? = null): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { prop.call(it.value) }
    addColumnInternal(column)
    op?.invoke(column)
    return column
}

/**
 * Create a column with a title specified cell type and operate on it. Inside the code block you can call
 * `value { it.value.someProperty }` to set up a cellValueFactory that must return T or ObservableValue<T>
 */
@Suppress("UNUSED_PARAMETER")
fun <S, T : Any> TableView<S>.column(title: String, cellType: KClass<T>, op: (TableColumn<S, T>.() -> Unit)? = null): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    addColumnInternal(column)
    op?.invoke(column)
    return column
}

/**
 * Create a column with a value factory that extracts the value from the given callback.
 */
fun <S, T> TableView<S>.column(title: String, valueProvider: (TableColumn.CellDataFeatures<S, T>) -> ObservableValue<T>): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { valueProvider(it) }
    columns.add(column)
    return column
}

/**
 * Configure a cellValueFactory for the column. If the returned value is not observable, it is automatically
 * wrapped in a SimpleObjectProperty for convenience.
 */
@Suppress("UNCHECKED_CAST")
infix fun <S> TableColumn<S, *>.value(cellValueFactory: (TableColumn.CellDataFeatures<S, Any>) -> Any?): TableColumn<S, *> {
    this.cellValueFactory = Callback {
        val createdValue = cellValueFactory(it as TableColumn.CellDataFeatures<S, Any>)
        if (createdValue is ObservableValue<*>) createdValue as ObservableValue<Any> else SimpleObjectProperty(createdValue)
    }
    return this
}

@JvmName(name = "columnForObservableProperty")
inline fun <reified S, T> TreeTableView<S>.column(title: String, prop: KProperty1<S, ObservableValue<T>>): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = Callback { prop.call(it.value.value) }
    columns.add(column)
    return column
}

/**
 * Create a column with a value factory that extracts the observable value from the given function reference.
 * This method requires that you have kotlin-reflect on your classpath.
 */
inline fun <S, reified T> TableView<S>.column(title: String, observableFn: KFunction<ObservableValue<T>>): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { observableFn.call(it.value) }
    addColumnInternal(column)
    return column
}

inline fun <S, reified T> TreeTableView<S>.column(title: String, observableFn: KFunction<ObservableValue<T>>): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = Callback { observableFn.call(it.value) }
    columns.add(column)
    return column
}

/**
 * Create a column with a value factory that extracts the value from the given callback.
 */
inline fun <reified S, T> TreeTableView<S>.column(title: String, noinline valueProvider: (TreeTableColumn.CellDataFeatures<S, T>) -> ObservableValue<T>): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = Callback { valueProvider(it) }
    columns.add(column)
    return column
}


fun <S> TableView<S>.rowExpander(expandOnDoubleClick: Boolean = false, expandedNodeCallback: RowExpanderPane.(S) -> Unit): ExpanderColumn<S> {
    val expander = ExpanderColumn(expandedNodeCallback)
    addColumnInternal(expander, 0)
    setRowFactory {
        object : TableRow<S>() {
            override fun createDefaultSkin(): Skin<*> {
                return ExpandableTableRowSkin(this, expander)
            }
        }
    }
    if (expandOnDoubleClick) onUserSelect(2) {
        expander.toggleExpanded(selectionModel.selectedIndex)
    }
    return expander
}

class RowExpanderPane(val tableRow: TableRow<*>, val expanderColumn: ExpanderColumn<*>) : StackPane() {
    init {
        addClass("expander-pane")
    }

    fun toggleExpanded() {
        expanderColumn.toggleExpanded(tableRow.index)
    }

    fun expandedProperty() = expanderColumn.getCellObservableValue(tableRow.index) as SimpleBooleanProperty
    var expanded: Boolean get() = expandedProperty().value; set(value) {
        expandedProperty().value = value
    }
}

class ExpanderColumn<S>(private val expandedNodeCallback: RowExpanderPane.(S) -> Unit) : TableColumn<S, Boolean>() {
    private val expandedNodeCache = HashMap<S, Node>()
    private val expansionState = mutableMapOf<S, BooleanProperty>()

    init {
        addClass("expander-column")

        cellValueFactory = Callback {
            expansionState.getOrPut(it.value, { getExpandedProperty(it.value) })
        }

        cellFactory = Callback { ToggleCell() }
    }

    fun toggleExpanded(index: Int) {
        val expanded = getCellObservableValue(index) as SimpleBooleanProperty
        expanded.value = !expanded.value
        tableView.refresh()
    }

    fun getOrCreateExpandedNode(tableRow: TableRow<S>): Node? {
        val index = tableRow.index
        if (index > -1 && index < tableView.items.size) {
            val item = tableView.items[index]!!
            var node: Node? = expandedNodeCache[item]
            if (node == null) {
                node = RowExpanderPane(tableRow, this)
                expandedNodeCallback(node, item)
                expandedNodeCache.put(item, node)
            }
            return node
        }
        return null
    }

    fun getExpandedNode(item: S): Node? = expandedNodeCache[item]

    fun getExpandedProperty(item: S): BooleanProperty {
        var value: BooleanProperty? = expansionState[item]
        if (value == null) {
            value = object : SimpleBooleanProperty(item, "expanded", false) {
                /**
                 * When the expanded state change we refresh the tableview.
                 * If the expanded state changes to false we remove the cached expanded node.
                 */
                override fun invalidated() {
                    tableView.refresh()
                    if (!getValue()) expandedNodeCache.remove(bean)
                }
            }
            expansionState.put(item, value)
        }
        return value
    }

    private inner class ToggleCell : TableCell<S, Boolean>() {
        private val button = Button()

        init {
            button.isFocusTraversable = false
            button.styleClass.add("expander-button")
            button.setPrefSize(16.0, 16.0)
            button.padding = Insets(0.0)
            button.setOnAction { toggleExpanded(index) }
        }

        override fun updateItem(expanded: Boolean?, empty: Boolean) {
            super.updateItem(expanded, empty)
            if (item == null || empty) {
                graphic = null
            } else {
                button.text = if (expanded == true) "-" else "+"
                graphic = button
            }
        }
    }
}

class ExpandableTableRowSkin<S>(val tableRow: TableRow<S>, val expander: ExpanderColumn<S>) : TableRowSkin<S>(tableRow) {
    var tableRowPrefHeight = -1.0

    init {
        tableRow.itemProperty().addListener { observable, oldValue, newValue ->
            if (oldValue != null) {
                val expandedNode = this.expander.getExpandedNode(oldValue)
                if (expandedNode != null) children.remove(expandedNode)
            }
        }
    }

    val expanded: Boolean get() {
        val item = skinnable.item
        return (item != null && expander.getCellData(skinnable.index))
    }

    private fun getContent(): Node? {
        val node = expander.getOrCreateExpandedNode(tableRow)
        if (!children.contains(node)) children.add(node)
        return node
    }

    override fun computePrefHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        tableRowPrefHeight = super.computePrefHeight(width, topInset, rightInset, bottomInset, leftInset)
        return if (expanded) tableRowPrefHeight + (getContent()?.prefHeight(width) ?: 0.0) else tableRowPrefHeight
    }

    override fun layoutChildren(x: Double, y: Double, w: Double, h: Double) {
        super.layoutChildren(x, y, w, h)
        if (expanded) getContent()?.resizeRelocate(0.0, tableRowPrefHeight, w, h - tableRowPrefHeight)
    }

}

sealed class ResizeType(val isResizable: Boolean) {
    class Pref(val width: Double) : ResizeType(true)
    class Fixed(val width: Double) : ResizeType(false)
    class Weight(val weight: Double, val padding: Double = 0.0, val minContentWidth: Boolean = false, var minRecorded: Boolean = false) : ResizeType(true)
    class Pct(val value: Double) : ResizeType(true)
    class Content(val padding: Double = 0.0, val useAsMin: Boolean = false, val useAsMax: Boolean = false, var minRecorded: Boolean = false, var maxRecorded: Boolean = false) : ResizeType(true)
    class Remaining : ResizeType(true)

    var delta: Double = 0.0
}

class SmartResize private constructor() : Callback<TableView.ResizeFeatures<out Any>, Boolean> {

    @Suppress("DEPRECATION")
    override fun call(param: TableView.ResizeFeatures<out Any>): Boolean {
        param.table.isSmartResizing = true

        try {
            if (param.column == null) {
                // Resize all columns

                val contentWidth = param.table.getContentWidth()
                if (contentWidth == 0.0) return false

                if (!isPolicyInstalled(param.table)) install(param.table)

                var remainingWidth = contentWidth

                // Fixed columns always keep their size
                val fixedColumns = param.table.columns.filter { it.resizeType is Fixed }
                fixedColumns.forEach {
                    val rt = it.resizeType as Fixed
                    it.prefWidth = rt.width
                    remainingWidth -= it.width
                }

                // Preferred sized columns get their size and are adjusted for resize-delta that affected them
                val prefColumns = param.table.columns.filter { it.resizeType is Pref }
                prefColumns.forEach {
                    val rt = it.resizeType as Pref
                    it.prefWidth = rt.width + rt.delta
                    remainingWidth -= it.width
                }

                // Content columns are resized to their content and adjusted for resize-delta that affected them
                val contentColumns = param.table.columns.filter { it.resizeType is Content }
                param.table.resizeColumnsToFitContent(contentColumns)
                contentColumns.forEach {
                    val rt = it.resizeType as Content

                    it.prefWidth = it.width + rt.delta + rt.padding

                    // Save minWidth if different from default
                    if (rt.useAsMin && !rt.minRecorded && it.width != 80.0) {
                        it.minWidth = it.width
                        rt.minRecorded = true
                    }

                    // Save maxWidth if different from default
                    if (rt.useAsMax && !rt.maxRecorded && it.width != 80.0) {
                        it.maxWidth = it.width
                        rt.maxRecorded = true
                    }

                    remainingWidth -= it.width
                }

                // Pct columns share what's left of space from here
                val pctColumns = param.table.columns.filter { it.resizeType is Pct }
                if (pctColumns.isNotEmpty()) {
                    val widthPerPct = contentWidth.toDouble() / 100.0
                    pctColumns.forEach {
                        val rt = it.resizeType as Pct
                        it.prefWidth = (widthPerPct * rt.value) + rt.delta
                        remainingWidth -= it.width
                    }
                }

                // Weighted columns shouldn't be combined with Pct. Weight is converted to pct of remaining width
                // and distributed to weigthed columns + remaining type columns
                val weightColumns = param.table.columns.filter { it.resizeType is Weight }
                if (weightColumns.isNotEmpty()) {
                    val consideredColumns = weightColumns + param.table.columns.filter { it.resizeType is Remaining }
                    // Combining with "Remaining" typed columns. Remaining columns will get a default weight of 1
                    fun TableColumn<*, *>.weight() = (resizeType as? Weight)?.weight ?: 1.0

                    val totalWeight = consideredColumns.map { it.weight() }.sum()
                    val perWeight = remainingWidth / totalWeight

                    consideredColumns.forEach {
                        val rt = it.resizeType
                        if (rt is Weight) {
                            if (rt.minContentWidth && !rt.minRecorded) {
                                rt.minRecorded = true
                                it.minWidth = it.width + rt.padding
                            }
                            it.prefWidth = Math.max(it.minWidth, (perWeight * rt.weight) + rt.delta + rt.padding)
                        } else {
                            it.prefWidth = Math.max(it.minWidth, perWeight + rt.delta)
                        }
                        remainingWidth -= it.width
                    }

                } else {
                    // If no weighted columns, give the rest of the width to the "Remaining" columns
                    val remainingColumns = param.table.columns.filter { it.resizeType is Remaining }
                    if (remainingColumns.isNotEmpty() && remainingWidth > 0) {
                        val perColumn = remainingWidth / remainingColumns.size.toDouble()
                        remainingColumns.forEach {
                            it.prefWidth = perColumn + it.resizeType.delta
                            remainingWidth -= it.width
                        }
                    }
                }

                // Adjustment if we didn't assign all width
                if (remainingWidth > 0.0) {
                    // Give remaining width to the right-most resizable column
                    val rightMostResizable = param.table.columns.reversed().filter { it.resizeType.isResizable }.firstOrNull()
                    rightMostResizable?.apply {
                        prefWidth = width + remainingWidth
                        remainingWidth -= width
                    }
                    // Adjustment for where we assigned more width that we have
                } else if (remainingWidth < 0.0) {
                    // Reduce from resizable columns, for now we reduce the column with largest reduction potential
                    // We should consider reducing based on the resize type of the column as well
                    var canReduceMore = true
                    while (canReduceMore && remainingWidth < 0.0) {
                        // Choose the column with largest reduction potential (largest gap betweeen size and minSize)
                        val reduceableCandidate = param.table.columns
                                .filter { it.resizeType.isResizable && it.minWidth < it.width }
                                .sortedBy { (it.width - it.minWidth) * -1 }
                                .firstOrNull()

                        canReduceMore = reduceableCandidate != null
                        if (reduceableCandidate != null && remainingWidth < 0.0) {
                            val reduceBy = Math.min(1.0, Math.abs(remainingWidth))
                            val toWidth = reduceableCandidate.width - reduceBy
                            reduceableCandidate.resizeType.delta -= reduceBy
                            reduceableCandidate.prefWidth = toWidth
                            remainingWidth += reduceBy
                        }
                    }
                }
            } else {
                // Handle specific column size operation
                val rt = param.column.resizeType

                if (!rt.isResizable) return false

                val targetWidth = param.column.width + param.delta

                // Would resize result in illegal width?
                if (targetWidth < param.column.minWidthProperty().value || targetWidth > param.column.maxWidthProperty().value) return false

                // Prepare to adjust the right column by the same amount we subtract or add to this column
                val rightColDelta = param.delta * -1.0
                val colIndex = param.table.columns.indexOf(param.column)

                val rightCol = param.table.columns
                        .filterIndexed { i, c -> i > colIndex && c.resizeType.isResizable }
                        .filter {
                            val newWidth = it.width + rightColDelta
                            newWidth <= it.maxWidthProperty().value && newWidth >= it.minWidthProperty().value
                        }
                        .firstOrNull() ?: return false

                // Apply negative delta and set new with for the right column
                with(rightCol) {
                    resizeType.delta += rightColDelta
                    prefWidth = width + rightColDelta
                }

                // Apply delta and set new width for the resized column
                with(param.column) {
                    rt.delta += param.delta
                    prefWidth = width + param.delta
                }

            }
            return true
        } finally {
            param.table.isSmartResizing = false
        }
    }

    var TableView<*>.isSmartResizing: Boolean
        get() = properties["tornadofx.isSmartResizing"] == true
        set(value) {
            properties["tornadofx.isSmartResizing"] = value
        }

    fun requestResize(table: TableView<*>) {
        call(TableView.ResizeFeatures(table, null, 0.0))
    }

    companion object {
        val POLICY = SmartResize()
        val ResizeTypeKey = "tornadofx.smartColumnResizeType"

        internal var TableView<*>.isSmartResizing: Boolean
            get() = properties["tornadofx.isSmartResizing"] == true
            set(value) {
                properties["tornadofx.isSmartResizing"] = value
            }

        private val policyChangeListener = ChangeListener<Callback<TableView.ResizeFeatures<*>, Boolean>> { observable, oldValue, newValue ->
            val table = (observable as ObjectProperty<*>).bean as TableView<*>
            if (newValue == POLICY) install(table) else uninstall(table)
        }

        private val columnsChangeListener = ListChangeListener<TableColumn<*, *>> { s ->
            while (s.next()) {
                if (s.wasAdded()) s.addedSubList.forEach {
                    it.widthProperty().addListener(columnWidthChangeListener)
                }
                if (s.wasRemoved()) s.removed.forEach {
                    it.widthProperty().removeListener(columnWidthChangeListener)
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        private val columnWidthChangeListener = ChangeListener<Number> { observable, oldValue, newValue ->
            val column = (observable as ReadOnlyProperty<*>).bean as TableColumn<*, *>
            val table: TableView<out Any>? = column.tableView

            if (table != null && !table.isSmartResizing) {
                val rt = column.resizeType
                val diff = oldValue.toDouble() - newValue.toDouble()
                rt.delta -= diff
                POLICY.call(TableView.ResizeFeatures<Any>(table as TableView<Any>?, null, 0.0))
            }

        }

        private fun isPolicyInstalled(table: TableView<*>): Boolean {
            return table.properties["tornadofx.smartResizeInstalled"] == true
        }

        private fun install(table: TableView<*>) {
            table.columnResizePolicyProperty().addListener(policyChangeListener)
            table.columns.addListener(columnsChangeListener)
            table.columns.forEach { it.widthProperty().addListener(columnWidthChangeListener) }
            table.properties["tornadofx.smartResizeInstalled"] = true
        }

        private fun uninstall(table: TableView<*>) {
            table.columnResizePolicyProperty().removeListener(policyChangeListener)
            table.columns.removeListener(columnsChangeListener)
            table.columns.forEach { it.widthProperty().removeListener(columnWidthChangeListener) }
            table.properties.remove("tornadofx.smartResizeInstalled")
        }
    }
}

/**
 * Get the width of the area available for columns inside the TableView
 */
fun TableView<*>.getContentWidth() = TableView::class.java.getDeclaredField("contentWidth").let {
    it.isAccessible = true
    it.get(this@getContentWidth) as Double
}

internal var TableColumn<*, *>.resizeType: ResizeType
    get() = resizeTypeProperty().value
    set(value) {
        resizeTypeProperty().value = value
    }

@Suppress("UNCHECKED_CAST")
internal fun TableColumn<*, *>.resizeTypeProperty() =
        properties.getOrPut(SmartResize.ResizeTypeKey) { SimpleObjectProperty(Content()) } as ObjectProperty<ResizeType>

fun <S, T> TableColumn<S, T>.fixedWidth(width: Double): TableColumn<S, T> {
    minWidth = width
    maxWidth = width
    resizeType = ResizeType.Fixed(width)
    return this
}

fun <S, T> TableColumn<S, T>.minWidth(width: Double): TableColumn<S, T> {
    minWidth = width
    return this
}

fun <S, T> TableColumn<S, T>.maxWidth(width: Double): TableColumn<S, T> {
    maxWidth = width
    return this
}

fun <S, T> TableColumn<S, T>.prefWidth(width: Double): TableColumn<S, T> {
    prefWidth = width
    return this
}

fun <S, T> TableColumn<S, T>.remainingWidth(): TableColumn<S, T> {
    resizeType = ResizeType.Remaining()
    return this
}

fun <S, T> TableColumn<S, T>.weigthedWidth(weight: Double, padding: Double = 0.0, minContentWidth: Boolean = false): TableColumn<S, T> {
    resizeType = ResizeType.Weight(weight, padding, minContentWidth)
    return this
}

fun <S, T> TableColumn<S, T>.pctWidth(pct: Double): TableColumn<S, T> {
    resizeType = ResizeType.Pct(pct)
    return this
}

/**
 * Make the column fit the content plus an optional padding width. Optionally constrain the min or max width to be this width.
 */
fun <S, T> TableColumn<S, T>.contentWidth(padding: Double = 0.0, useAsMin: Boolean = false, useAsMax: Boolean = false): TableColumn<S, T> {
    resizeType = ResizeType.Content(padding, useAsMin, useAsMax)
    return this
}
