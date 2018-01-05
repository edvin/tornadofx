@file:Suppress("UNCHECKED_CAST")

package tornadofx

import com.sun.corba.se.impl.util.RepositoryId.cache
import com.sun.javafx.scene.control.skin.TableRowSkin
import com.sun.org.apache.bcel.internal.Repository.addClass
import javafx.application.Platform
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
import tornadofx.Stylesheet.Companion.editable
import tornadofx.Stylesheet.Companion.root
import tornadofx.Stylesheet.Companion.title
import tornadofx.WizardStyles.Companion.graphic
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

/**
 * Create a spinner for an arbitrary type. This spinner requires you to configure a value factory, or it will throw an exception.
 */
fun <T> EventTarget.spinner(editable: Boolean = false, property: Property<T>? = null, enableScroll: Boolean = false, op: Spinner<T>.() -> Unit = {}): Spinner<T> {
    val spinner = Spinner<T>()
    spinner.isEditable = editable
    opcr(this, spinner, op)
    if (property != null) {
        requireNotNull(spinner.valueFactory) {
            "You must configure the value factory or use the Number based spinner builder " +
                    "which configures a default value factory along with min, max and initialValue!"
        }
        spinner.valueFactory.valueProperty().bindBidirectional(property)
        ViewModel.register(spinner.valueFactory.valueProperty(), property)
    }

    if (enableScroll) {
        spinner.setOnScroll { event ->
            if (event.deltaY > 0) spinner.increment()
            if (event.deltaY < 0) spinner.decrement()
        }
    }

    if (editable) {
        spinner.focusedProperty().addListener { _, _, newValue ->
            if (!newValue) {
                spinner.increment(0)
            }
        }
    }

    return spinner
}

inline fun <reified T : Number> EventTarget.spinner(min: T? = null, max: T? = null, initialValue: T? = null, amountToStepBy: T? = null, editable: Boolean = false, property: Property<T>? = null, enableScroll: Boolean = false, noinline op: Spinner<T>.() -> Unit = {}): Spinner<T> {
    val spinner: Spinner<T>
    val isInt = (property is IntegerProperty && property !is DoubleProperty && property !is FloatProperty) || min is Int || max is Int || initialValue is Int ||
            T::class == Int::class || T::class == Integer::class || T::class.javaPrimitiveType == Integer::class.java
    if (isInt) {
        spinner = Spinner(min?.toInt() ?: 0, max?.toInt() ?: 100, initialValue?.toInt() ?: 0, amountToStepBy?.toInt() ?: 1)
    } else {
        spinner = Spinner(min?.toDouble() ?: 0.0, max?.toDouble() ?: 100.0, initialValue?.toDouble() ?: 0.0, amountToStepBy?.toDouble() ?: 1.0)
    }
    if (property != null) {
        spinner.valueFactory.valueProperty().bindBidirectional(property)
        ViewModel.register(spinner.valueFactory.valueProperty(), property)
    }
    spinner.isEditable = editable

    if (enableScroll) {
        spinner.setOnScroll { event ->
            if (event.deltaY > 0) spinner.increment()
            if (event.deltaY < 0) spinner.decrement()
        }
    }

    if (editable) {
        spinner.focusedProperty().addListener { _, _, newValue ->
            if (!newValue) {
                spinner.increment(0)
            }
        }
    }

    return opcr(this, spinner, op)
}

fun <T> EventTarget.spinner(items: ObservableList<T>, editable: Boolean = false, property: Property<T>? = null, enableScroll: Boolean = false, op: Spinner<T>.() -> Unit = {}): Spinner<T> {
    val spinner = Spinner<T>(items)
    if (property != null) {
        spinner.valueFactory.valueProperty().bindBidirectional(property)
        ViewModel.register(spinner.valueFactory.valueProperty(), property)
    }
    spinner.isEditable = editable

    if (enableScroll) {
        spinner.setOnScroll { event ->
            if (event.deltaY > 0) spinner.increment()
            if (event.deltaY < 0) spinner.decrement()
        }
    }

    if (editable) {
        spinner.focusedProperty().addListener { _, _, newValue ->
            if (!newValue) {
                spinner.increment(0)
            }
        }
    }

    return opcr(this, spinner, op)
}

fun <T> EventTarget.spinner(valueFactory: SpinnerValueFactory<T>, editable: Boolean = false, property: Property<T>? = null, enableScroll: Boolean = false, op: Spinner<T>.() -> Unit = {}): Spinner<T> {
    val spinner = Spinner<T>(valueFactory)
    if (property != null) {
        spinner.valueFactory.valueProperty().bindBidirectional(property)
        ViewModel.register(spinner.valueFactory.valueProperty(), property)
    }
    spinner.isEditable = editable

    if (enableScroll) {
        spinner.setOnScroll { event ->
            if (event.deltaY > 0) spinner.increment()
            if (event.deltaY < 0) spinner.decrement()
        }
    }

    if (editable) {
        spinner.focusedProperty().addListener { _, _, newValue ->
            if (!newValue) {
                spinner.increment(0)
            }
        }
    }

    return opcr(this, spinner, op)
}

fun <T> EventTarget.combobox(property: Property<T>? = null, values: List<T>? = null, op: ComboBox<T>.() -> Unit = {}) = opcr(this, ComboBox<T>().apply {
    if (values != null) items = values as? ObservableList<T> ?: values.observable()
    if (property != null) bind(property)
}, op)

fun <T> ComboBox<T>.cellFormat(scope: Scope, formatButtonCell: Boolean = true, formatter: ListCell<T>.(T) -> Unit) {
    cellFactory = Callback {
        //ListView may be defined or not, so properties are set the safe way
        SmartListCell(scope, it, mapOf<Any, Any>("tornadofx.cellFormat" to formatter))
    }
    if (formatButtonCell) buttonCell = cellFactory.call(null)
}

fun <T> EventTarget.choicebox(property: Property<T>? = null, values: List<T>? = null, op: ChoiceBox<T>.() -> Unit = {}) = opcr(this, ChoiceBox<T>().apply {
    if (values != null) items = (values as? ObservableList<T>) ?: values.observable()
    if (property != null) bind(property)
}, op)

fun <T> EventTarget.listview(values: ObservableList<T>? = null, op: ListView<T>.() -> Unit = {}) = opcr(this, ListView<T>().apply {
    if (values != null) {
        if (values is SortedFilteredList<T>) values.bindTo(this)
        else items = values
    }
}, op)

fun <T> EventTarget.listview(values: ReadOnlyListProperty<T>, op: ListView<T>.() -> Unit = {}) = listview(values as ObservableValue<ObservableList<T>>, op)

fun <T> EventTarget.listview(values: ObservableValue<ObservableList<T>>, op: ListView<T>.() -> Unit = {}) = opcr(this, ListView<T>().apply {
    fun rebinder() {
        (items as? SortedFilteredList<T>)?.bindTo(this)
    }
    itemsProperty().bind(values)
    rebinder()
    itemsProperty().onChange {
        rebinder()
    }
}, op)

fun <T> EventTarget.tableview(items: ObservableList<T>? = null, op: TableView<T>.() -> Unit = {}): TableView<T> {
    val tableview = TableView<T>()
    if (items != null) {
        if (items is SortedFilteredList<T>) items.bindTo(tableview)
        else tableview.items = items
    }
    return opcr(this, tableview, op)
}

fun <T> EventTarget.tableview(items: ReadOnlyListProperty<T>, op: TableView<T>.() -> Unit = {}) = tableview(items as ObservableValue<ObservableList<T>>, op)

fun <T> EventTarget.tableview(items: ObservableValue<ObservableList<T>>, op: TableView<T>.() -> Unit = {}): TableView<T> {
    val tableview = TableView<T>()
    fun rebinder() {
        (tableview.items as? SortedFilteredList<T>)?.bindTo(tableview)
    }
    tableview.itemsProperty().bind(items)
    rebinder()
    tableview.itemsProperty().onChange {
        rebinder()
    }
    items.onChange {
        rebinder()
    }
    return opcr(this, tableview, op)
}

fun <T> EventTarget.treeview(root: TreeItem<T>? = null, op: TreeView<T>.() -> Unit = {}): TreeView<T> {
    val treeview = TreeView<T>()
    if (root != null) treeview.root = root
    return opcr(this, treeview, op)
}

fun <T> EventTarget.treetableview(root: TreeItem<T>? = null, op: TreeTableView<T>.() -> Unit = {}): TreeTableView<T> {
    val treetableview = TreeTableView<T>()
    if (root != null) treetableview.root = root
    return opcr(this, treetableview, op)
}

fun <T : Any> TreeView<T>.lazyPopulate(
        leafCheck: (LazyTreeItem<T>) -> Boolean = { it.hasChildren() },
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
    var leafResult: Boolean? = null
    var childFactoryInvoked = false
    var childFactoryResult: List<T>? = null

    override fun isLeaf(): Boolean {
        if (leafResult == null)
            leafResult = leafCheck(this)
        return leafResult!!
    }

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
                    val permutated = change.list.subList(change.from, change.to).map { newLazyTreeItem(it) }
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

    fun hasChildren(): Boolean {
        val result = invokeAndSetChildFactorySynchronously()
        return result == null || result.isEmpty()
    }

    private fun invokeAndSetChildFactorySynchronously(): List<T>? {
        if (!childFactoryInvoked) {
            childFactoryInvoked = true
            childFactoryResult = childFactory(this)
            if (childFactoryResult != null) super.getChildren().setAll(childFactoryResult!!.map { newLazyTreeItem(it) })
        }
        return childFactoryResult
    }

    private fun newLazyTreeItem(item: T) = LazyTreeItem(item, leafCheck, itemProcessor, childFactory).apply { itemProcessor(this) }
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

fun <S, T> TableColumn<S, T>.enableTextWrap() = apply {
    setCellFactory {
        TableCell<S, T>().apply {
            val text = Text()
            graphic = text
            prefHeight = Control.USE_COMPUTED_SIZE
            text.wrappingWidthProperty().bind(this@enableTextWrap.widthProperty().subtract(Bindings.multiply(2.0, graphicTextGapProperty())))
            text.textProperty().bind(stringBinding(itemProperty()) { get()?.toString() ?: "" })
        }
    }
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

/**
 * Create a column holding children columns
 */
@Suppress("UNCHECKED_CAST")
fun <S> TableView<S>.nestedColumn(title: String, op: TableView<S>.(TableColumn<S, Any?>) -> Unit = {}): TableColumn<S, Any?> {
    val column = TableColumn<S, Any?>(title)
    addColumnInternal(column)
    val previousColumnTarget = properties["tornadofx.columnTarget"] as? ObservableList<TableColumn<S, *>>
    properties["tornadofx.columnTarget"] = column.columns
    op(this, column)
    properties["tornadofx.columnTarget"] = previousColumnTarget
    return column
}

/**
 * Create a column holding children columns
 */
@Suppress("UNCHECKED_CAST")
fun <S> TreeTableView<S>.nestedColumn(title: String, op: TreeTableView<S>.() -> Unit = {}): TreeTableColumn<S, Any?> {
    val column = TreeTableColumn<S, Any?>(title)
    addColumnInternal(column)
    val previousColumnTarget = properties["tornadofx.columnTarget"] as? ObservableList<TableColumn<S, *>>
    properties["tornadofx.columnTarget"] = column.columns
    op(this)
    properties["tornadofx.columnTarget"] = previousColumnTarget
    return column
}

/**
 * Create a column using the propertyName of the attribute you want shown.
 */
fun <S, T> TableView<S>.column(title: String, propertyName: String, op: TableColumn<S, T>.() -> Unit = {}): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = PropertyValueFactory<S, T>(propertyName)
    addColumnInternal(column)
    return column.also(op)
}

/**
 * Create a column using the getter of the attribute you want shown.
 */
@JvmName("pojoColumn")
fun <S, T> TableView<S>.column(title: String, getter: KFunction<T>): TableColumn<S, T> {
    val startIndex = if (getter.name.startsWith("is") && getter.name[2].isUpperCase()) 2 else 3
    val propName = getter.name.substring(startIndex).decapitalize()
    return this.column(title, propName)
}

/**
 * Create a column using the propertyName of the attribute you want shown.
 */
fun <S, T> TreeTableView<S>.column(title: String, propertyName: String, op: TreeTableColumn<S, T>.() -> Unit = {}): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = TreeItemPropertyValueFactory<S, T>(propertyName)
    addColumnInternal(column)
    return column.also(op)
}

/**
 * Create a column using the getter of the attribute you want shown.
 */
@JvmName("pojoColumn")
fun <S, T> TreeTableView<S>.column(title: String, getter: KFunction<T>): TreeTableColumn<S, T> {
    val startIndex = if (getter.name.startsWith("is") && getter.name[2].isUpperCase()) 2 else 3
    val propName = getter.name.substring(startIndex).decapitalize()
    return this.column(title, propName)
}

fun <S, T> TableColumn<S, T?>.useComboBox(items: ObservableList<T>, afterCommit: (TableColumn.CellEditEvent<S, T?>) -> Unit = {}) = apply {
    cellFactory = ComboBoxTableCell.forTableColumn(items)
    setOnEditCommit {
        val property = it.tableColumn.getCellObservableValue(it.rowValue) as Property<T?>
        property.value = it.newValue
        afterCommit(it)
    }
}

inline fun <S, reified T> TableColumn<S, T?>.useTextField(
        converter: StringConverter<T>? = null,
        noinline afterCommit: (TableColumn.CellEditEvent<S, T?>) -> Unit = {}
) = apply {
    when (T::class) {
        String::class -> {
            @Suppress("UNCHECKED_CAST")
            val stringColumn = this as TableColumn<S, String?>
            stringColumn.cellFactory = TextFieldTableCell.forTableColumn()
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

fun <S, T> TableColumn<S, T?>.useChoiceBox(items: ObservableList<T>, afterCommit: (TableColumn.CellEditEvent<S, T?>) -> Unit = {}) = apply {
    cellFactory = ChoiceBoxTableCell.forTableColumn(items)
    setOnEditCommit {
        val property = it.tableColumn.getCellObservableValue(it.rowValue) as Property<T?>
        property.value = it.newValue
        afterCommit(it)
    }
}

fun <S> TableColumn<S, out Number?>.useProgressBar(scope: Scope, afterCommit: (TableColumn.CellEditEvent<S, Number?>) -> Unit = {}) = apply {
    cellFormat(scope) {
        addClass(Stylesheet.progressBarTableCell)
        graphic = cache {
            progressbar(itemProperty().doubleBinding { it?.toDouble() ?: 0.0 }) {
                useMaxWidth = true
            }
        }
    }
    (this as TableColumn<S, Number?>).setOnEditCommit {
        val property = it.tableColumn.getCellObservableValue(it.rowValue) as Property<Number?>
        property.value = it.newValue?.toDouble()
        afterCommit(it as TableColumn.CellEditEvent<S, Number?>)
    }
}

fun <S> TableColumn<S, Boolean?>.useCheckbox(editable: Boolean = true) = apply {
    setCellFactory { CheckBoxCell(editable) }
    if (editable) {
        Platform.runLater {
            tableView?.isEditable = true
        }
    }
}

fun <S> ListView<S>.useCheckbox(converter: StringConverter<S>? = null, getter: (S) -> ObservableValue<Boolean>) {
    setCellFactory { CheckBoxListCell(getter, converter) }
}

class CheckBoxCell<S>(val makeEditable: Boolean) : TableCell<S, Boolean?>() {
    val checkbox: CheckBox by lazy {
        CheckBox().apply {
            if (makeEditable) {
                selectedProperty().bindBidirectional(itemProperty())
                setOnAction {
                    tableView.edit(index, tableColumn)
                    commitEdit(!isSelected)
                }
            } else {
                isDisable = true
                selectedProperty().bind(itemProperty())
            }
        }
    }

    init {
        if (makeEditable) {
            isEditable = true
            tableView?.isEditable = true
        }
    }

    override fun updateItem(item: Boolean?, empty: Boolean) {
        super.updateItem(item, empty)
        style { alignment = Pos.CENTER }
        graphic = if (empty || item == null) null else checkbox
    }
}

fun <T> TableView<T>.bindSelected(property: Property<T>) {
    selectionModel.selectedItemProperty().onChange {
        property.value = it
    }
}

fun <T> TableView<T>.bindSelected(model: ItemViewModel<T>) {
    selectionModel.selectedItemProperty().onChange {
        model.item = it
    }
}

val <T> TableView<T>.selectedCell: TablePosition<T, *>?
    get() = selectionModel.selectedCells.firstOrNull() as TablePosition<T, *>?

val <T> TableView<T>.selectedColumn: TableColumn<T, *>?
    get() = selectedCell?.tableColumn

val <T> TableView<T>.selectedValue: Any?
    get() = selectedColumn?.getCellObservableValue(selectedItem)?.value

val <T> TreeTableView<T>.selectedCell: TreeTablePosition<T, *>?
    get() = selectionModel.selectedCells.firstOrNull()

val <T> TreeTableView<T>.selectedColumn: TreeTableColumn<T, *>?
    get() = selectedCell?.tableColumn

val <T> TreeTableView<T>.selectedValue: Any?
    get() = selectedColumn?.getCellObservableValue(selectionModel.selectedItem)?.value

/**
 * Create a column with a value factory that extracts the value from the given mutable
 * property and converts the property to an observable value.
 */
inline fun <reified S, T> TableView<S>.column(title: String, prop: KMutableProperty1<S, T>, noinline op: TableColumn<S, T>.() -> Unit = {}): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { observable(it.value, prop) }
    addColumnInternal(column)
    return column.also(op)
}

inline fun <reified S, T> TreeTableView<S>.column(title: String, prop: KMutableProperty1<S, T>, noinline op: TreeTableColumn<S, T>.() -> Unit = {}): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = Callback { observable(it.value.value, prop) }
    addColumnInternal(column)
    return column.also(op)
}

/**
 * Create a column with a value factory that extracts the value from the given property and
 * converts the property to an observable value.
 */
inline fun <reified S, T> TableView<S>.column(title: String, prop: KProperty1<S, T>, noinline op: TableColumn<S, T>.() -> Unit = {}): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { observable(it.value, prop) }
    addColumnInternal(column)
    return column.also(op)
}

inline fun <reified S, T> TreeTableView<S>.column(title: String, prop: KProperty1<S, T>, noinline op: TreeTableColumn<S, T>.() -> Unit = {}): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = Callback { observable(it.value.value, prop) }
    addColumnInternal(column)
    return column.also(op)
}

/**
 * Create a column with a value factory that extracts the value from the given ObservableValue property.
 */
@JvmName(name = "columnForObservableProperty")
inline fun <reified S, T> TableView<S>.column(title: String, prop: KProperty1<S, ObservableValue<T>>, noinline op: TableColumn<S, T>.() -> Unit = {}): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { prop.call(it.value) }
    addColumnInternal(column)
    return column.also(op)
}

/**
 * Add a global edit commit handler to the TableView. You avoid assuming the responsibility
 * for writing back the data into your domain object and can consentrate on the actual
 * response you want to happen when a column commits and edit.
 */
fun <S> TableView<S>.onEditCommit(onCommit: TableColumn.CellEditEvent<S, Any>.(S) -> Unit) {
    fun addEventHandlerForColumn(column: TableColumn<S, *>) {
        column.addEventHandler(TableColumn.editCommitEvent<S, Any>()) { event ->
            // Make sure the domain object gets the new value before we notify our handler
            Platform.runLater {
                onCommit(event, event.rowValue)
            }
        }
    }

    columns.forEach(::addEventHandlerForColumn)

    columns.addListener({ change: ListChangeListener.Change<out TableColumn<S, *>> ->
        while (change.next()) {
            if (change.wasAdded())
                change.addedSubList.forEach(::addEventHandlerForColumn)
        }
    })
}

/**
 * Create a column with a title specified cell type and operate on it. Inside the code block you can call
 * `value { it.value.someProperty }` to set up a cellValueFactory that must return T or ObservableValue<T>
 */
@Suppress("UNUSED_PARAMETER")
fun <S, T : Any> TableView<S>.column(title: String, cellType: KClass<T>, op: TableColumn<S, T>.() -> Unit = {}): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    addColumnInternal(column)
    return column.also(op)
}

/**
 * Create a column with a value factory that extracts the value from the given callback.
 */
fun <S, T> TableView<S>.column(title: String, valueProvider: (TableColumn.CellDataFeatures<S, T>) -> ObservableValue<T>): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { valueProvider(it) }
    addColumnInternal(column)
    return column
}

/**
 * Configure a cellValueFactory for the column. If the returned value is not observable, it is automatically
 * wrapped in a SimpleObjectProperty for convenience.
 */
@Suppress("UNCHECKED_CAST")
infix fun <S> TableColumn<S, *>.value(cellValueFactory: (TableColumn.CellDataFeatures<S, Any>) -> Any?) = apply {
    this.cellValueFactory = Callback {
        val createdValue = cellValueFactory(it as TableColumn.CellDataFeatures<S, Any>)
        (createdValue as? ObservableValue<Any>) ?: SimpleObjectProperty(createdValue)
    }
}

@JvmName(name = "columnForObservableProperty")
inline fun <reified S, T> TreeTableView<S>.column(title: String, prop: KProperty1<S, ObservableValue<T>>): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = Callback { prop.call(it.value.value) }
    addColumnInternal(column)
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
    addColumnInternal(column)
    return column
}

/**
 * Create a column with a value factory that extracts the value from the given callback.
 */
inline fun <reified S, T> TreeTableView<S>.column(title: String, noinline valueProvider: (TreeTableColumn.CellDataFeatures<S, T>) -> ObservableValue<T>): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = Callback { valueProvider(it) }
    addColumnInternal(column)
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
    var expanded: Boolean
        get() = expandedProperty().value
        set(value) {
            expandedProperty().value = value
        }

    override fun getUserAgentStylesheet() = RowExpanderPane::class.java.getResource("rowexpanderpane.css").toExternalForm()
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
        if (index in tableView.items.indices) {
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

fun <T> TableView<T>.enableCellEditing() {
    selectionModel.isCellSelectionEnabled = true
    isEditable = true
}

fun <T> TableView<T>.selectOnDrag() {
    var startRow = 0
    var startColumn = columns.first()

    // Record start position and clear selection unless Control is down
    addEventFilter(MouseEvent.MOUSE_PRESSED) {
        startRow = 0

        (it.pickResult.intersectedNode as? TableCell<*, *>)?.apply {
            startRow = index
            startColumn = tableColumn as TableColumn<T, *>?

            if (selectionModel.isCellSelectionEnabled) {
                selectionModel.clearAndSelect(startRow, startColumn)
            } else {
                selectionModel.clearAndSelect(startRow)
            }
        }
    }

    // Select items while dragging
    addEventFilter(MouseEvent.MOUSE_DRAGGED) {
        (it.pickResult.intersectedNode as? TableCell<*, *>)?.apply {
            if (items.size > index) {
                if (selectionModel.isCellSelectionEnabled) {
                    selectionModel.selectRange(startRow, startColumn, index, tableColumn as TableColumn<T, *>?)
                } else {
                    selectionModel.selectRange(startRow, index)
                }
            }
        }
    }
}

fun <S> TableView<S>.enableDirtyTracking() = editModel.enableDirtyTracking()

@Suppress("UNCHECKED_CAST")
val <S> TableView<S>.editModel: TableViewEditModel<S>
    get() = properties.getOrPut("tornadofx.editModel") { TableViewEditModel(this) } as TableViewEditModel<S>

class TableViewEditModel<S>(val tableView: TableView<S>) {
    val items = FXCollections.observableHashMap<S, TableColumnDirtyState<S>>()

    private var _selectedItemDirtyState: ObjectBinding<TableColumnDirtyState<S>?>? = null
    val selectedItemDirtyState: ObjectBinding<TableColumnDirtyState<S>?>
        get() {
            if (_selectedItemDirtyState == null)
                _selectedItemDirtyState = objectBinding(tableView.selectionModel.selectedItemProperty()) { getDirtyState(value) }
            return _selectedItemDirtyState!!
        }

    private var _selectedItemDirty: BooleanBinding? = null
    val selectedItemDirty: BooleanBinding
        get() {
            if (_selectedItemDirty == null)
                _selectedItemDirty = booleanBinding(selectedItemDirtyState) { value?.dirty?.value ?: false }
            return _selectedItemDirty!!
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
                if (initialValue == event.newValue) {
                    itemTracker.dirtyColumns.remove(event.tableColumn)
                } else {
                    itemTracker.dirtyColumns[event.tableColumn] = initialValue
                }
                selectedItemDirty.invalidate()
            }
        }

        // Add columns and track changes to columns
        tableView.columns.forEach(::addEventHandlerForColumn)
        tableView.columns.addListener({ change: ListChangeListener.Change<out TableColumn<S, *>> ->
            while (change.next()) {
                if (change.wasAdded())
                    change.addedSubList.forEach(::addEventHandlerForColumn)
            }
        })

        // Remove dirty state for items removed from the TableView
        val listenForRemovals = ListChangeListener<S> {
            while (it.next()) {
                if (it.wasRemoved()) {
                    it.removed.forEach {
                        items.remove(it)
                    }
                }
            }
        }

        // Track removals on current items list
        tableView.items?.addListener(listenForRemovals)

        // Clear items if item list changes and track removals in new list
        tableView.itemsProperty().addListener { observableValue, oldValue, newValue ->
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

    fun commit() {
        items.values.forEach { it.commit() }
    }

    fun rollback() {
        items.values.forEach { it.rollback() }
    }

    /**
     * Rollback the current item, or just the given column for this item if a column is supplied
     */
    fun rollback(item: S, column: TableColumn<*, *>? = null) {
        val dirtyState = getDirtyState(item)
        if (column == null) dirtyState.rollback() else dirtyState.rollback(column)
    }

    fun commitSelected() {
        val selected = selectedItemDirtyState.value?.item
        if (selected != null) commit(selected)
    }

    fun rollbackSelected() {
        val selected = selectedItemDirtyState.value?.item
        if (selected != null) rollback(selected)
    }

    fun isDirty(item: S): Boolean = getDirtyState(item).dirty.value
}

class TableColumnDirtyState<S>(val editModel: TableViewEditModel<S>, val item: S) : Observable {
    val invalidationListeners = ArrayList<InvalidationListener>()

    // Dirty columns and initial value
    private var _dirtyColumns: ObservableMap<TableColumn<S, Any?>, Any?>? = null
    val dirtyColumns: ObservableMap<TableColumn<S, Any?>, Any?>
        get() {
            if (_dirtyColumns == null)
                _dirtyColumns = FXCollections.observableHashMap<TableColumn<S, Any?>, Any?>()
            return _dirtyColumns!!
        }

    private var _dirty: BooleanBinding? = null
    val dirty: BooleanBinding
        get() {
            if (_dirty == null)
                _dirty = booleanBinding(dirtyColumns) { isNotEmpty() }
            return _dirty!!
        }
    val isDirty: Boolean get() = dirty.value

    fun getDirtyColumnProperty(column: TableColumn<*, *>) = booleanBinding(dirtyColumns) { containsKey(column as TableColumn<S, Any?>) }

    fun isDirtyColumn(column: TableColumn<*, *>) = dirtyColumns.containsKey(column as TableColumn<S, Any?>)

    init {
        dirtyColumns.addListener(InvalidationListener {
            invalidationListeners.forEach { it.invalidated(this) }
        })
    }

    override fun removeListener(listener: InvalidationListener) {
        invalidationListeners.remove(listener)
    }

    override fun addListener(listener: InvalidationListener) {
        invalidationListeners.add(listener)
    }

    override fun equals(other: Any?) = other is TableColumnDirtyState<*> && other.item == item
    override fun hashCode() = item?.hashCode() ?: throw IllegalStateException("Item must be present")

    fun rollback(column: TableColumn<*, *>) {
        val initialValue = dirtyColumns[column as TableColumn<S, Any?>]
        if (initialValue != null) {
            column.setValue(item, initialValue)
            dirtyColumns.remove(column)
        }
        editModel.tableView.refresh()
    }

    fun commit(column: TableColumn<*, *>) {
        val initialValue = dirtyColumns[column as TableColumn<S, Any?>]
        if (initialValue != null) {
            dirtyColumns.remove(column)
        }
        editModel.tableView.refresh()
    }

    fun rollback() {
        dirtyColumns.forEach {
            it.key.setValue(item, it.value)
        }
        dirtyColumns.clear()
        editModel.selectedItemDirtyState.invalidate()
        editModel.tableView.refresh()
    }

    fun commit() {
        dirtyColumns.clear()
        editModel.selectedItemDirtyState.invalidate()
        editModel.tableView.refresh()
    }

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
                if (polygon !in children)
                    children.add(polygon)

                polygon.relocate(cell.layoutX, y)
            } else {
                children.remove(polygon)
            }
        }

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
 * Get the value from the property rperesenting this TableColumn.
 */
fun <S, T> TableColumn<S, T>.getValue(item: S) = getTableColumnProperty(item).value

/**
 * Get the property representing this TableColumn for the given item.
 */
fun <S, T> TableColumn<S, T>.getTableColumnProperty(item: S): ObservableValue<T?> {
    val param = TableColumn.CellDataFeatures<S, T>(tableView, this, item)
    val property = cellValueFactory.call(param)
    return property
}
