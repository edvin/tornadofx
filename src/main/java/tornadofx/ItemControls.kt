package tornadofx

import com.sun.javafx.scene.control.skin.TableRowSkin
import javafx.beans.InvalidationListener
import javafx.beans.Observable
import javafx.beans.binding.Bindings
import javafx.beans.binding.BooleanBinding
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.*
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.event.EventTarget
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.cell.*
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Polygon
import javafx.scene.text.Text
import javafx.util.Callback
import javafx.util.StringConverter
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

// ================================================================
// Spinners

/**
 * Creates a Spinner for an arbitrary type. This spinner requires you to configure a value factory, or it will throw an exception.
 */
fun <T> EventTarget.spinner(
    editable: Boolean = false,
    property: Property<T>? = null,
    enableScroll: Boolean = false,
    op: Spinner<T>.() -> Unit = {}
): Spinner<T> = Spinner<T>().attachTo(this, op) { it.setup(property, editable, enableScroll, requireValueFactory = true) }

/**
 * Creates a Spinner instance with the [value factory][Spinner.valueFactory]
 * set to be an instance of [SpinnerValueFactory.ListSpinnerValueFactory].
 *
 * The Spinner [value property][Spinner.valueProperty] will be set to the first
 * element in [items], if an element exists, or `null` otherwise.
 */
fun <T> EventTarget.spinner(
    items: ObservableList<T>,
    editable: Boolean = false,
    property: Property<T>? = null,
    enableScroll: Boolean = false,
    op: Spinner<T>.() -> Unit = {}
): Spinner<T> = Spinner<T>(items).attachTo(this, op) { it.setup(property, editable, enableScroll) }

/**
 * Creates a Spinner instance with the given [valueFactory] set.
 */
fun <T> EventTarget.spinner(
    valueFactory: SpinnerValueFactory<T>,
    editable: Boolean = false,
    property: Property<T>? = null,
    enableScroll: Boolean = false,
    op: Spinner<T>.() -> Unit = {}
): Spinner<T> = Spinner<T>(valueFactory).attachTo(this, op) { it.setup(property, editable, enableScroll) }

/**
 * Creates a Spinner instance with the [value factory][Spinner.valueFactory]
 * set to be an instance of [SpinnerValueFactory.IntegerSpinnerValueFactory] or
 * [SpinnerValueFactory.DoubleSpinnerValueFactory] depending on the generic type.
 *
 * @param min The minimum allowed numeric value for the Spinner.
 * @param max The maximum allowed numeric value for the Spinner.
 * @param initialValue The value of the Spinner when first instantiated, must
 *                     be within the bounds of the min and max arguments, or
 *                     else the min value will be used.
 * @param amountToStepBy The amount to increment or decrement by, per step.
 */
inline fun <reified T : Number> EventTarget.spinner(
    min: T? = null,
    max: T? = null,
    initialValue: T? = null,
    amountToStepBy: T? = null,
    editable: Boolean = false,
    property: Property<T>? = null,
    enableScroll: Boolean = false,
    noinline op: Spinner<T>.() -> Unit = {}
): Spinner<T> {
    val isInt = T::class == Int::class || min is Int || max is Int || initialValue is Int || property is IntegerProperty

    val spinner: Spinner<T> = if (isInt) {
        Spinner(min?.toInt() ?: 0, max?.toInt() ?: 100, initialValue?.toInt() ?: 0, amountToStepBy?.toInt() ?: 1)
    } else {
        Spinner(min?.toDouble() ?: 0.0, max?.toDouble() ?: 100.0, initialValue?.toDouble() ?: 0.0, amountToStepBy?.toDouble() ?: 1.0)
    }

    return spinner.attachTo(this, op) { it.setup(property, editable, enableScroll) }
}

@PublishedApi
internal fun <T> Spinner<T>.setup(
    property: Property<T>?,
    editable: Boolean,
    enableScroll: Boolean,
    requireValueFactory: Boolean = false
) {
    if (property != null) {
        if (requireValueFactory) requireNotNull(valueFactory) {
            "You must configure the value factory or use the Number based spinner builder " +
                    "which configures a default value factory along with min, max and initialValue!"
        }

        valueFactory.valueProperty().also {
            it.bindBidirectional(property)
            ViewModel.register(it, property)
        }
    }

    isEditable = editable
    if (editable) focusedProperty().onChange { if (!it) increment(0) }

    if (enableScroll) setOnScroll { event ->
        if (event.deltaY > 0) increment()
        if (event.deltaY < 0) decrement()
    }
}


// ================================================================
// ComboBox

fun <T> EventTarget.combobox(
    property: Property<T>? = null,
    values: List<T>? = null,
    op: ComboBox<T>.() -> Unit = {}
): ComboBox<T> = ComboBox<T>().attachTo(this, op) {
    if (values != null) it.items = values as? ObservableList<T> ?: values.observable()
    if (property != null) it.bind(property)
}

fun <T> ComboBox<T>.cellFormat(
    scope: Scope,
    formatButtonCell: Boolean = true,
    formatter: ListCell<T>.(T) -> Unit
) {
    cellFactory = Callback {
        //ListView may be defined or not, so properties are set the safe way
        SmartListCell(scope, it, mapOf<Any, Any>("tornadofx.cellFormat" to formatter))
    }
    if (formatButtonCell) buttonCell = cellFactory.call(null)
}


// ================================================================
// ChoiceBox

fun <T> EventTarget.choicebox(
    property: Property<T>? = null,
    values: List<T>? = null,
    op: ChoiceBox<T>.() -> Unit = {}
): ChoiceBox<T> = ChoiceBox<T>().attachTo(this, op) {
    if (values != null) it.items = (values as? ObservableList<T>) ?: values.observable()
    if (property != null) it.bind(property)
}


// ================================================================
// ListView

fun <T> EventTarget.listview(
    values: ObservableList<T>? = null,
    op: ListView<T>.() -> Unit = {}
): ListView<T> = ListView<T>().attachTo(this, op) {
    if (values != null) {
        if (values is SortedFilteredList<T>) values.bindTo(it) else it.items = values
    }
}

fun <T> EventTarget.listview(
    values: ReadOnlyListProperty<T>,
    op: ListView<T>.() -> Unit = {}
): ListView<T> = listview(values as ObservableValue<ObservableList<T>>, op)

fun <T> EventTarget.listview(
    values: ObservableValue<ObservableList<T>>,
    op: ListView<T>.() -> Unit = {}
): ListView<T> = ListView<T>().attachTo(this, op) {
    fun rebinder() {
        (it.items as? SortedFilteredList<T>)?.bindTo(it)
    }

    it.itemsProperty().bind(values)
    rebinder()
    it.itemsProperty().onChange { rebinder() }
    values.onChange { rebinder() }
}


// ================================================================
// TableView

fun <T> EventTarget.tableview(
    items: ObservableList<T>? = null,
    op: TableView<T>.() -> Unit = {}
): TableView<T> = TableView<T>().attachTo(this, op) {
    if (items != null) {
        if (items is SortedFilteredList<T>) items.bindTo(it) else it.items = items
    }
}

fun <T> EventTarget.tableview(
    items: ReadOnlyListProperty<T>,
    op: TableView<T>.() -> Unit = {}
): TableView<T> = tableview(items as ObservableValue<ObservableList<T>>, op)

fun <T> EventTarget.tableview(
    items: ObservableValue<ObservableList<T>>,
    op: TableView<T>.() -> Unit = {}
): TableView<T> = TableView<T>().attachTo(this, op) {
    fun rebinder() {
        (it.items as? SortedFilteredList<T>)?.bindTo(it)
    }

    it.itemsProperty().bind(items)
    rebinder()
    it.itemsProperty().onChange { rebinder() }
    items.onChange { rebinder() }
}


// ================================================================
// TreeView

fun <T> EventTarget.treeview(
    root: TreeItem<T>? = null,
    op: TreeView<T>.() -> Unit = {}
): TreeView<T> = TreeView<T>().attachTo(this, op) {
    if (root != null) it.root = root
}

fun <T : Any> TreeView<T>.lazyPopulate(
    leafCheck: (LazyTreeItem<T>) -> Boolean = { !it.hasChildren() },
    itemProcessor: (LazyTreeItem<T>) -> Unit = {},
    childFactory: (TreeItem<T>) -> List<T>?
) {
    fun createItem(value: T) = LazyTreeItem(value, leafCheck, itemProcessor, childFactory).also(itemProcessor)

    requireNotNull(root) { "You must set a root TreeItem before calling lazyPopulate" }

    task {
        childFactory.invoke(root)
    } success {
        root.children.setAll(it?.map(::createItem) ?: emptyList())
    }
}

class LazyTreeItem<T : Any>(
    value: T,
    val leafCheck: (LazyTreeItem<T>) -> Boolean,
    val itemProcessor: (LazyTreeItem<T>) -> Unit = {},
    val childFactory: (TreeItem<T>) -> List<T>?
) : TreeItem<T>(value) {
    private val leafResult: Boolean by lazy { leafCheck(this) }
    var childFactoryInvoked: Boolean = false
    var childFactoryResult: List<T>? = null

    override fun isLeaf(): Boolean = leafResult

    override fun getChildren(): ObservableList<TreeItem<T>> {
        if (!childFactoryInvoked) {
            task {
                invokeAndSetChildFactorySynchronously()
            } success {
                if (childFactoryResult != null) listenForChanges()
            }
        }
        return super.getChildren()
    }

    private fun listenForChanges() {
        (childFactoryResult as? ObservableList<T>)?.addListener(ListChangeListener { change ->
            while (change.next()) {
                if (change.wasPermutated()) {
                    children.subList(change.from, change.to).clear()
                    val permutated = change.list.subList(change.from, change.to).map(::newLazyTreeItem)
                    children.addAll(change.from, permutated)
                } else {
                    if (change.wasRemoved()) {
                        val removed = change.removed.flatMap { removed -> children.filter { it.value == removed } }
                        children.removeAll(removed)
                    }
                    if (change.wasAdded()) {
                        val added = change.addedSubList.map { newLazyTreeItem(it) }
                        children.addAll(change.from, added)
                    }
                }
            }
        })
    }

    fun hasChildren(): Boolean = invokeAndSetChildFactorySynchronously()?.isNotEmpty() == true

    private fun invokeAndSetChildFactorySynchronously(): List<T>? {
        if (!childFactoryInvoked) {
            childFactoryInvoked = true
            childFactoryResult = childFactory(this)?.also { super.getChildren().setAll(it.map(::newLazyTreeItem)) }
        }
        return childFactoryResult
    }

    private fun newLazyTreeItem(item: T) = LazyTreeItem(item, leafCheck, itemProcessor, childFactory).also { itemProcessor(it) }
}


// ================================================================
// TreeTableView

fun <T> EventTarget.treetableview(
    root: TreeItem<T>? = null,
    op: TreeTableView<T>.() -> Unit = {}
): TreeTableView<T> = TreeTableView<T>().attachTo(this, op) {
    if (root != null) it.root = root
}


// ================================================================
// TreeItem

fun <T> TreeItem<T>.treeitem(
    value: T? = null,
    op: TreeItem<T>.() -> Unit = {}
): TreeItem<T> {
    val treeItem = value?.let { TreeItem<T>(it) } ?: TreeItem()
    treeItem.op()
    this += treeItem
    return treeItem
}

operator fun <T> TreeItem<T>.plusAssign(treeItem: TreeItem<T>) {
    this.children.add(treeItem)
}


// ================================================================
// TableView & TreeTableView Columns

/**
 * Create a column using the propertyName of the attribute you want shown.
 */
fun <S, T> TableView<S>.column(
    title: String,
    propertyName: String,
    op: TableColumn<S, T>.() -> Unit = {}
): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = PropertyValueFactory<S, T>(propertyName)
    addColumnInternal(column)
    return column.also(op)
}

/**
 * Create a column using the propertyName of the attribute you want shown.
 */
fun <S, T> TreeTableView<S>.column(
    title: String,
    propertyName: String,
    op: TreeTableColumn<S, T>.() -> Unit = {}
): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = TreeItemPropertyValueFactory<S, T>(propertyName)
    addColumnInternal(column)
    return column.also(op)
}


/** Create a column using the getter of the attribute you want shown. */
@JvmName("pojoColumn")
fun <S, T> TableView<S>.column(
    title: String,
    getter: KFunction<T>,
    op: TableColumn<S, T>.() -> Unit = {}
): TableColumn<S, T> {
    val startIndex = if (getter.name.startsWith("is") && getter.name[2].isUpperCase()) 2 else 3
    val propName = getter.name.substring(startIndex).decapitalize()
    return this.column(title, propName, op)
}

/** Create a column using the getter of the attribute you want shown. */
@JvmName("pojoColumn")
fun <S, T> TreeTableView<S>.column(
    title: String,
    getter: KFunction<T>,
    op: TreeTableColumn<S, T>.() -> Unit = {}
): TreeTableColumn<S, T> {
    val startIndex = if (getter.name.startsWith("is") && getter.name[2].isUpperCase()) 2 else 3
    val propName = getter.name.substring(startIndex).decapitalize()
    return this.column(title, propName, op)
}


/**
 * Create a column with a value factory that extracts the value from the given mutable
 * property and converts the property to an observable value.
 */
fun <S, T> TableView<S>.column(
    title: String,
    prop: KMutableProperty1<S, T>,
    op: TableColumn<S, T>.() -> Unit = {}
): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { observable(it.value, prop) }
    addColumnInternal(column)
    return column.also(op)
}

/**
 * Create a column with a value factory that extracts the value from the given mutable
 * property and converts the property to an observable value.
 */
fun <S, T> TreeTableView<S>.column(
    title: String,
    prop: KMutableProperty1<S, T>,
    op: TreeTableColumn<S, T>.() -> Unit = {}
): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = Callback { observable(it.value.value, prop) }
    addColumnInternal(column)
    return column.also(op)
}


/**
 * Create a column with a value factory that extracts the value from the given property and
 * converts the property to an observable value.
 *
 * ATTENTION: This function was renamed to `readonlyColumn` to avoid shadowing the version for
 * observable properties.
 */
fun <S, T> TableView<S>.readonlyColumn(
    title: String,
    prop: KProperty1<S, T>,
    op: TableColumn<S, T>.() -> Unit = {}
): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { observable(it.value, prop) }
    addColumnInternal(column)
    return column.also(op)
}

/**
 * Create a column with a value factory that extracts the value from the given property and
 * converts the property to an observable value.
 *
 * ATTENTION: This function was renamed to `readonlyColumn` to avoid shadowing the version for
 * observable properties.
 */
fun <S, T> TreeTableView<S>.readonlyColumn(
    title: String,
    prop: KProperty1<S, T>,
    op: TreeTableColumn<S, T>.() -> Unit = {}
): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = Callback { observable(it.value.value, prop) }
    addColumnInternal(column)
    return column.also(op)
}


/** Create a column with a value factory that extracts the value from the given ObservableValue property. */
fun <S, T> TableView<S>.column(
    title: String,
    prop: KProperty1<S, ObservableValue<T>>,
    op: TableColumn<S, T>.() -> Unit = {}
): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { prop.call(it.value) }
    addColumnInternal(column)
    return column.also(op)
}

/** Create a column with a value factory that extracts the value from the given ObservableValue property. */
fun <S, T> TreeTableView<S>.column(
    title: String,
    prop: KProperty1<S, ObservableValue<T>>,
    op: TreeTableColumn<S, T>.() -> Unit = {}
): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = Callback { prop.call(it.value.value) }
    addColumnInternal(column)
    return column.also(op)
}


/**
 * Create a column with a value factory that extracts the observable value from the given function reference.
 * This method requires that you have kotlin-reflect on your classpath.
 */
fun <S, T> TableView<S>.column(
    title: String,
    observableFn: KFunction<ObservableValue<T>>,
    op: TableColumn<S, T>.() -> Unit = {}
): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { observableFn.call(it.value) }
    addColumnInternal(column)
    return column.also(op)
}

/**
 * Create a column with a value factory that extracts the observable value from the given function reference.
 * This method requires that you have kotlin-reflect on your classpath.
 */
fun <S, T> TreeTableView<S>.column(
    title: String,
    observableFn: KFunction<ObservableValue<T>>,
    op: TreeTableColumn<S, T>.() -> Unit = {}
): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = Callback { observableFn.call(it.value) }
    addColumnInternal(column)
    return column.also(op)
}


/**
 * Create a column with a value factory that extracts the value from the given callback.
 */
fun <S, T> TableView<S>.column(
    title: String,
    valueProvider: (TableColumn.CellDataFeatures<S, T>) -> ObservableValue<T>
): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { valueProvider(it) }
    addColumnInternal(column)
    return column
}

/**
 * Create a column with a value factory that extracts the value from the given callback.
 */
fun <S, T> TreeTableView<S>.column(
    title: String,
    valueProvider: (TreeTableColumn.CellDataFeatures<S, T>) -> ObservableValue<T>
): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = Callback { valueProvider(it) }
    addColumnInternal(column)
    return column
}


/**
 * Create a column with a title specified cell type and operate on it. Inside the code block you can call
 * `value { it.value.someProperty }` to set up a cellValueFactory that must return T or ObservableValue<T>
 */
@Suppress("UNUSED_PARAMETER")
fun <S, T : Any> TableView<S>.column(
    title: String,
    cellType: KClass<T>,
    op: TableColumn<S, T>.() -> Unit = {}
): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    addColumnInternal(column)
    return column.also(op)
}

/**
 * Create a column with a title specified cell type and operate on it. Inside the code block you can call
 * `value { it.value.someProperty }` to set up a cellValueFactory that must return T or ObservableValue<T>
 */
@Suppress("UNUSED_PARAMETER")
fun <S, T : Any> TreeTableView<S>.column(
    title: String,
    cellType: KClass<T>,
    op: TreeTableColumn<S, T>.() -> Unit = {}
): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    addColumnInternal(column)
    return column.also(op)
}


/**
 * Create a column holding children columns
 */
fun <S> TableView<S>.nestedColumn(
    title: String,
    op: TableView<S>.(TableColumn<S, Any?>) -> Unit = {} // FIXME Inconsistent signature with the rest of `column` functions
): TableColumn<S, Any?> {
    val column = TableColumn<S, Any?>(title)
    addColumnInternal(column)
    val previousColumnTarget = properties["tornadofx.columnTarget"]
    properties["tornadofx.columnTarget"] = column.columns
    op(this, column)
    properties["tornadofx.columnTarget"] = previousColumnTarget
    return column
}

/**
 * Create a column holding children columns
 */
fun <S> TreeTableView<S>.nestedColumn(
    title: String,
    op: TreeTableView<S>.() -> Unit = {} // FIXME Inconsistent signature with function above
): TreeTableColumn<S, Any?> {
    val column = TreeTableColumn<S, Any?>(title)
    addColumnInternal(column)
    val previousColumnTarget = properties["tornadofx.columnTarget"]
    properties["tornadofx.columnTarget"] = column.columns
    op(this)
    properties["tornadofx.columnTarget"] = previousColumnTarget
    return column
}


@Suppress("UNCHECKED_CAST")
fun <S> TableView<S>.addColumnInternal(column: TableColumn<S, *>, index: Int? = null) {
    val columnTarget = properties["tornadofx.columnTarget"] as? ObservableList<TableColumn<S, *>> ?: columns
    if (index == null) columnTarget.add(column) else columnTarget.add(index, column)
}

@Suppress("UNCHECKED_CAST")
fun <S> TreeTableView<S>.addColumnInternal(column: TreeTableColumn<S, *>, index: Int? = null) {
    val columnTarget = properties["tornadofx.columnTarget"] as? ObservableList<TreeTableColumn<S, *>> ?: columns
    if (index == null) columnTarget.add(column) else columnTarget.add(index, column)
}


fun <S> TableView<S>.makeIndexColumn(name: String = "#", startNumber: Int = 1): TableColumn<S, Number> {
    return TableColumn<S, Number>(name).apply {
        isSortable = false
        prefWidth = width
        this@makeIndexColumn.columns += this
        setCellValueFactory { ReadOnlyIntegerWrapper(items.indexOf(it.value) + startNumber) }
    }
}


fun <S, T> TableColumn<S, T>.enableTextWrap(): TableColumn<S, T> = apply column@{
    setCellFactory {
        TableCell<S, T>().apply {
            val text = Text()
            graphic = text
            prefHeight = Control.USE_COMPUTED_SIZE
            text.wrappingWidthProperty().bind(this@column.widthProperty().subtract(Bindings.multiply(2.0, graphicTextGapProperty())))
            text.textProperty().bind(stringBinding(itemProperty()) { get()?.toString() ?: "" })
        }
    }
}


// ================================================================
// TableColumn Cells

inline fun <S, reified T> TableColumn<S, T?>.useTextField(
    converter: StringConverter<T>? = null,
    noinline afterCommit: (TableColumn.CellEditEvent<S, T?>) -> Unit = {}
): TableColumn<S, T?> = apply {
    when (T::class) {
        String::class -> {
            @Suppress("UNCHECKED_CAST")
            this as TableColumn<S, String?>
            this.cellFactory = TextFieldTableCell.forTableColumn()
        }
        else -> {
            requireNotNull(converter) { "You must supply a converter for non String columns" }
            cellFactory = TextFieldTableCell.forTableColumn(converter)
        }
    }

    setOnEditCommit {
        val property = it.tableColumn.getCellObservableValue(it.rowValue) as Property<T?>
        property.value = it.newValue
        afterCommit(it)
    }
}

fun <S, T> TableColumn<S, T?>.useComboBox(
    items: ObservableList<T>,
    afterCommit: (TableColumn.CellEditEvent<S, T?>) -> Unit = {}
): TableColumn<S, T?> = apply {
    cellFactory = ComboBoxTableCell.forTableColumn(items)
    setOnEditCommit {
        val property = it.tableColumn.getCellObservableValue(it.rowValue) as Property<T?>
        property.value = it.newValue
        afterCommit(it)
    }
}

fun <S, T> TableColumn<S, T?>.useChoiceBox(
    items: ObservableList<T>,
    afterCommit: (TableColumn.CellEditEvent<S, T?>) -> Unit = {}
): TableColumn<S, T?> = apply {
    cellFactory = ChoiceBoxTableCell.forTableColumn(items)
    setOnEditCommit {
        val property = it.tableColumn.getCellObservableValue(it.rowValue) as Property<T?>
        property.value = it.newValue
        afterCommit(it)
    }
}

@Suppress("UNCHECKED_CAST")
fun <S> TableColumn<S, out Number?>.useProgressBar(
    scope: Scope,
    afterCommit: (TableColumn.CellEditEvent<S, Number?>) -> Unit = {}
): TableColumn<S, out Number?> = apply {
    cellFormat(scope) {
        addClass(Stylesheet.progressBarTableCell)
        graphic = cache {
            progressbar(itemProperty().doubleBinding { it?.toDouble() ?: 0.0 }) { useMaxWidth = true }
        }
    }
    (this as TableColumn<S, Number?>).setOnEditCommit {
        val property = it.tableColumn.getCellObservableValue(it.rowValue) as Property<Number?>
        property.value = it.newValue?.toDouble()
        afterCommit(it as TableColumn.CellEditEvent<S, Number?>)
    }
}

fun <S> TableColumn<S, Boolean?>.useCheckbox(editable: Boolean = true): TableColumn<S, Boolean?> = apply {
    cellFormat {
        graphic = cache {
            alignment = Pos.CENTER
            checkbox {
                if (editable) {
                    selectedProperty().bindBidirectional(itemProperty())

                    setOnAction {
                        tableView.edit(index, tableColumn)
                        commitEdit(!isSelected)
                    }
                } else {
                    selectedProperty().bind(itemProperty())
                }
            }
        }
    }
    if (editable) runLater { tableView?.isEditable = true }
}

// This was used earlier, but was changed to using cellFormat with cache, see above
//class CheckBoxCell<S>(val makeEditable: Boolean) : TableCell<S, Boolean?>() {
//    val checkbox: CheckBox by lazy {
//        CheckBox().apply {
//            if (makeEditable) {
//                selectedProperty().bindBidirectional(itemProperty())
//                setOnAction {
//                    tableView.edit(index, tableColumn)
//                    commitEdit(!isSelected)
//                }
//            } else {
//                isDisable = true
//                selectedProperty().bind(itemProperty())
//            }
//        }
//    }
//
//    init {
//        if (makeEditable) {
//            isEditable = true
//            tableView?.isEditable = true
//        }
//    }
//
//    override fun updateItem(item: Boolean?, empty: Boolean) {
//        super.updateItem(item, empty)
//        style { alignment = Pos.CENTER }
//        graphic = if (empty || item == null) null else checkbox
//    }
//}


// ================================================================
// ListView Cells

fun <S> ListView<S>.useCheckbox(converter: StringConverter<S>? = null, getter: (S) -> ObservableValue<Boolean>) {
    setCellFactory { CheckBoxListCell(getter, converter) }
}


// ================================================================
// Bindings

fun <T> TableView<T>.bindSelected(property: Property<T>) {
    selectionModel.selectedItemProperty().onChange { property.value = it }
}

fun <T> TableView<T>.bindSelected(model: ItemViewModel<T>) {
    selectionModel.selectedItemProperty().onChange { model.item = it }
}


// ================================================================
// Selections

@Suppress("UNCHECKED_CAST")
val <T> TableView<T>.selectedCell: TablePosition<T, *>?
    get() = selectionModel.selectedCells.firstOrNull() as TablePosition<T, *>? // (╯°□°）╯︵ ┻━┻

val <T> TreeTableView<T>.selectedCell: TreeTablePosition<T, *>?
    get() = selectionModel.selectedCells.firstOrNull()


val <T> TableView<T>.selectedColumn: TableColumn<T, *>?
    get() = selectedCell?.tableColumn

val <T> TreeTableView<T>.selectedColumn: TreeTableColumn<T, *>?
    get() = selectedCell?.tableColumn


val <T> TableView<T>.selectedValue: Any?
    get() = selectedColumn?.getCellObservableValue(selectedItem)?.value

val <T> TreeTableView<T>.selectedValue: Any?
    get() = selectedColumn?.getCellObservableValue(selectionModel.selectedItem)?.value // TODO Review


// ================================================================
// Editing

fun <T> TableView<T>.enableCellEditing() {
    selectionModel.isCellSelectionEnabled = true
    isEditable = true
}

@Suppress("UNCHECKED_CAST")
fun <T> TableView<T>.selectOnDrag() {
    var startRow = 0
    var startColumn = columns.first()

    // Record start position and clear selection unless Control is down
    addEventFilter(MouseEvent.MOUSE_PRESSED) {
        startRow = 0

        (it.pickResult.intersectedNode as? TableCell<*, *>)?.apply {
            startRow = index
            startColumn = tableColumn as TableColumn<T, *>?

            if (selectionModel.isCellSelectionEnabled)
                selectionModel.clearAndSelect(startRow, startColumn)
            else
                selectionModel.clearAndSelect(startRow)
        }
    }

    // Select items while dragging
    addEventFilter(MouseEvent.MOUSE_DRAGGED) {
        (it.pickResult.intersectedNode as? TableCell<*, *>)?.apply {
            if (items.size > index) {
                if (selectionModel.isCellSelectionEnabled)
                    selectionModel.selectRange(startRow, startColumn, index, tableColumn as TableColumn<T, *>?)
                else
                    selectionModel.selectRange(startRow, index)
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
val <S> TableView<S>.editModel: TableViewEditModel<S>
    get() = properties.getOrPut("tornadofx.editModel") { TableViewEditModel(this) } as TableViewEditModel<S>

fun <S> TableView<S>.enableDirtyTracking(): Unit = editModel.enableDirtyTracking()


/**
 * Add a global edit start handler to the TableView. You can use this callback
 * to cancel the edit request by calling cancel()
 */
fun <S> TableView<S>.onEditStart(onEditStart: TableColumn.CellEditEvent<S, Any?>.(S) -> Unit) {
    fun addEventHandlerForColumn(column: TableColumn<S, *>) {
        column.addEventHandler(TableColumn.editStartEvent<S, Any?>()) { event ->
            onEditStart(event, event.rowValue)
        }
        column.columns.forEach(::addEventHandlerForColumn)
    }

    columns.forEach(::addEventHandlerForColumn)

    columns.addListener { change: ListChangeListener.Change<out TableColumn<S, *>> ->
        while (change.next()) {
            if (change.wasAdded())
                change.addedSubList.forEach(::addEventHandlerForColumn)
        }
    }
}

/**
 * Add a global edit commit handler to the TableView. You avoid assuming the responsibility
 * for writing back the data into your domain object and can concentrate on the actual
 * response you want to happen when a column commits and edit.
 */
fun <S> TableView<S>.onEditCommit(onCommit: TableColumn.CellEditEvent<S, Any>.(S) -> Unit) {
    fun addEventHandlerForColumn(column: TableColumn<S, *>) {
        column.addEventHandler(TableColumn.editCommitEvent<S, Any>()) { event ->
            // Make sure the domain object gets the new value before we notify our handler
            runLater { onCommit(event, event.rowValue) }
        }
        column.columns.forEach(::addEventHandlerForColumn)
    }

    columns.forEach(::addEventHandlerForColumn)

    columns.addListener { change: ListChangeListener.Change<out TableColumn<S, *>> ->
        while (change.next()) {
            if (change.wasAdded())
                change.addedSubList.forEach(::addEventHandlerForColumn)
        }
    }
}


/**
 * Used to cancel an edit event, typically from `onEditStart`
 */
fun <S, T> TableColumn.CellEditEvent<S, T>.cancel(): Unit = tableView.edit(-1, tableColumn)


/**
 * Configure a cellValueFactory for the column. If the returned value is not observable, it is automatically
 * wrapped in a SimpleObjectProperty for convenience.
 */
@Suppress("UNCHECKED_CAST")
infix fun <S> TableColumn<S, *>.value(cellValueFactory: (TableColumn.CellDataFeatures<S, Any>) -> Any?): TableColumn<S, *> = apply {
    this.cellValueFactory = Callback {
        val createdValue = cellValueFactory(it as TableColumn.CellDataFeatures<S, Any>)
        (createdValue as? ObservableValue<Any>) ?: SimpleObjectProperty(createdValue)
    }
}


/**
 * Write a value into the property representing this TableColumn, provided
 * the property is writable.
 */
@Suppress("UNCHECKED_CAST")
fun <S, T> TableColumn<S, T>.setValue(item: S, value: T?) {
    val property = getTableColumnProperty(item)
    (property as? WritableValue<T>)?.value = value
}

/**
 * Get the value from the property representing this TableColumn.
 */
fun <S, T> TableColumn<S, T>.getValue(item: S): T? = getTableColumnProperty(item).value


/**
 * Get the property representing this TableColumn for the given item.
 */
fun <S, T> TableColumn<S, T>.getTableColumnProperty(item: S): ObservableValue<T?> {
    val param = TableColumn.CellDataFeatures<S, T>(tableView, this, item)
    return cellValueFactory.call(param)
}


class TableViewEditModel<S>(val tableView: TableView<S>) {
    val items: ObservableMap<S, TableColumnDirtyState<S>> = FXCollections.observableHashMap()

    val selectedItemDirtyState: ObjectBinding<TableColumnDirtyState<S>?> by lazy {
        objectBinding(tableView.selectionModel.selectedItemProperty()) { getDirtyState(value) }
    }

    val selectedItemDirty: BooleanBinding by lazy {
        booleanBinding(selectedItemDirtyState) { value?.dirty?.value ?: false }
    }

    fun getDirtyState(item: S): TableColumnDirtyState<S> = items.getOrPut(item) { TableColumnDirtyState(this, item) }

    fun enableDirtyTracking(dirtyDecorator: Boolean = true) {
        if (dirtyDecorator) {
            tableView.setRowFactory {
                object : TableRow<S>() {
                    override fun createDefaultSkin() = DirtyDecoratingTableRowSkin(this, this@TableViewEditModel)
                }
            }
        }

        fun addEventHandlerForColumn(column: TableColumn<S, *>) {
            column.addEventHandler(TableColumn.editCommitEvent<S, Any>()) { event ->
                // This fires before the column value is changed (else we would use onEditCommit)
                val item = event.rowValue
                val itemTracker = items.getOrPut(item) { TableColumnDirtyState(this, item) }
                val initialValue = itemTracker.dirtyColumns.getOrPut(event.tableColumn) {
                    event.tableColumn.getValue(item)
                }
                if (initialValue == event.newValue)
                    itemTracker.dirtyColumns.remove(event.tableColumn)
                else
                    itemTracker.dirtyColumns[event.tableColumn] = initialValue

                selectedItemDirty.invalidate()
            }
        }

        // Add columns and track changes to columns
        tableView.columns.forEach(::addEventHandlerForColumn)
        tableView.columns.addListener { change: ListChangeListener.Change<out TableColumn<S, *>> ->
            while (change.next()) if (change.wasAdded()) change.addedSubList.forEach(::addEventHandlerForColumn)
        }

        // Remove dirty state for items removed from the TableView
        val listenForRemovals = ListChangeListener<S> {
            while (it.next()) if (it.wasRemoved()) it.removed.forEach { items.remove(it) }
        }

        // Track removals on current items list
        tableView.items?.addListener(listenForRemovals)

        // Clear items if item list changes and track removals in new list
        tableView.itemsProperty().addListener { _, oldValue, newValue ->
            items.clear()
            oldValue?.removeListener(listenForRemovals)
            newValue?.addListener(listenForRemovals)
        }
    }

    /**
     * Commit the current item, or just the given column for this item if a column is supplied
     */
    fun commit(item: S, column: TableColumn<*, *>? = null) {
        val dirtyState = getDirtyState(item)
        if (column == null) dirtyState.commit() else dirtyState.commit(column)
    }

    fun commit(): Unit = items.values.forEach { it.commit() }

    fun rollback(): Unit = items.values.forEach { it.rollback() }

    /**
     * Rollback the current item, or just the given column for this item if a column is supplied
     */
    fun rollback(item: S, column: TableColumn<*, *>? = null) {
        val dirtyState = getDirtyState(item)
        if (column == null) dirtyState.rollback() else dirtyState.rollback(column)
    }

    fun commitSelected() {
        selectedItemDirtyState.value?.item?.also { commit(it) }
    }

    fun rollbackSelected() {
        selectedItemDirtyState.value?.item?.also { rollback(it) }
    }

    fun isDirty(item: S): Boolean = getDirtyState(item).dirty.value
}

class TableColumnDirtyState<S>(val editModel: TableViewEditModel<S>, val item: S) : Observable {
    val invalidationListeners: MutableList<InvalidationListener> = mutableListOf()

    // Dirty columns and initial value
    private var _dirtyColumns: ObservableMap<TableColumn<S, Any?>, Any?>? = null
    val dirtyColumns: ObservableMap<TableColumn<S, Any?>, Any?>
        get() {
            if (_dirtyColumns == null) _dirtyColumns = FXCollections.observableHashMap()
            return _dirtyColumns!!
        }

    val dirty: BooleanBinding by lazy { booleanBinding(dirtyColumns) { isNotEmpty() } }
    val isDirty: Boolean by dirty

    init {
        dirtyColumns.addListener(InvalidationListener { invalidationListeners.forEach { it.invalidated(this) } })
    }

    @Suppress("UNCHECKED_CAST")
    fun getDirtyColumnProperty(column: TableColumn<*, *>): BooleanBinding = booleanBinding(dirtyColumns) { containsKey(column as TableColumn<S, Any?>) }

    @Suppress("UNCHECKED_CAST")
    fun isDirtyColumn(column: TableColumn<*, *>): Boolean = dirtyColumns.containsKey(column as TableColumn<S, Any?>)

    override fun removeListener(listener: InvalidationListener) {
        invalidationListeners.remove(listener)
    }

    override fun addListener(listener: InvalidationListener) {
        invalidationListeners.add(listener)
    }

    @Suppress("UNCHECKED_CAST")
    fun rollback(column: TableColumn<*, *>) {
        val initialValue = dirtyColumns[column as TableColumn<S, Any?>]
        if (initialValue != null) {
            column.setValue(item, initialValue)
            dirtyColumns.remove(column)
        }
        editModel.tableView.refresh()
    }

    @Suppress("UNCHECKED_CAST")
    fun commit(column: TableColumn<*, *>) {
        val initialValue = dirtyColumns[column as TableColumn<S, Any?>]
        if (initialValue != null) {
            dirtyColumns.remove(column)
        }
        editModel.tableView.refresh()
    }

    fun rollback() {
        dirtyColumns.forEach { it.key.setValue(item, it.value) }
        dirtyColumns.clear()
        editModel.selectedItemDirtyState.invalidate()
        editModel.tableView.refresh()
    }

    fun commit() {
        dirtyColumns.clear()
        editModel.selectedItemDirtyState.invalidate()
        editModel.tableView.refresh()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TableColumnDirtyState<*>) return false

        if (item != other.item) return false

        return true
    }

    override fun hashCode(): Int = item?.hashCode() ?: throw IllegalStateException("Item must be present")
}

@Suppress("UNCHECKED_CAST")
class DirtyDecoratingTableRowSkin<S>(tableRow: TableRow<S>, val editModel: TableViewEditModel<S>) : TableRowSkin<S>(tableRow) {
    private fun getPolygon(cell: TableCell<S, *>) =
        cell.properties.getOrPut("tornadofx.dirtyStatePolygon") { Polygon(0.0, 0.0, 0.0, 10.0, 10.0, 0.0).apply { fill = Color.BLUE } } as Polygon

    override fun layoutChildren(x: Double, y: Double, w: Double, h: Double) {
        super.layoutChildren(x, y, w, h)

        cells.forEach { cell ->
            val item = if (cell.index > -1 && cell.tableView.items.size > cell.index) cell.tableView.items[cell.index] else null
            val polygon = getPolygon(cell)
            val isDirty = item != null && editModel.getDirtyState(item).isDirtyColumn(cell.tableColumn)
            if (isDirty) {
                if (polygon !in children) children.add(polygon)
                polygon.relocate(cell.layoutX, y)
            } else {
                children.remove(polygon)
            }
        }
    }
}


// ================================================================
// Utility

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
    if (expandOnDoubleClick) onUserSelect(2) { expander.toggleExpanded(selectionModel.selectedIndex) }

    return expander
}

class RowExpanderPane(val tableRow: TableRow<*>, val expanderColumn: ExpanderColumn<*>) : StackPane() {
    init {
        addClass("expander-pane")
    }

    fun expandedProperty(): BooleanProperty = expanderColumn.getCellObservableValue(tableRow.index) as SimpleBooleanProperty
    var expanded: Boolean
        get() = expandedProperty().value
        set(value) {
            expandedProperty().value = value
        }

    fun toggleExpanded() {
        expanderColumn.toggleExpanded(tableRow.index)
    }

    override fun getUserAgentStylesheet(): String = RowExpanderPane::class.java.getResource("rowexpanderpane.css").toExternalForm()
}

class ExpanderColumn<S>(private val expandedNodeCallback: RowExpanderPane.(S) -> Unit) : TableColumn<S, Boolean>() {
    private val expandedNodeCache = mutableMapOf<S, Node>()
    private val expansionState = mutableMapOf<S, BooleanProperty>()

    init {
        addClass("expander-readonlyColumn")

        cellValueFactory = Callback { expansionState.getOrPut(it.value) { getExpandedProperty(it.value) } }

        cellFactory = Callback { ToggleCell() }
    }

    fun toggleExpanded(index: Int) {
        val expanded = getCellObservableValue(index) as SimpleBooleanProperty
        expanded.value = !expanded.value
        tableView.refresh()
    }

    fun getOrCreateExpandedNode(tableRow: TableRow<S>): Node? {
        val index = tableRow.index
        if (index in tableView.items.indices) {
            val item = tableView.items[index]!!
            var node: Node? = expandedNodeCache[item]
            if (node == null) {
                node = RowExpanderPane(tableRow, this)
                expandedNodeCallback(node, item)
                expandedNodeCache[item] = node
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
            expansionState[item] = value
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
    var tableRowPrefHeight: Double = -1.0

    init {
        tableRow.itemProperty().addListener { _, oldValue, _ ->
            if (oldValue != null) {
                val expandedNode = this.expander.getExpandedNode(oldValue)
                if (expandedNode != null) children.remove(expandedNode)
            }
        }
    }

    val expanded: Boolean
        get() {
            val item = skinnable.item
            return (item != null && expander.getCellData(skinnable.index))
        }

    private fun getContent(): Node? {
        val node = expander.getOrCreateExpandedNode(tableRow)
        if (node !in children) children.add(node)
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
