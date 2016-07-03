package tornadofx

import javafx.beans.binding.Bindings
import javafx.beans.property.ObjectProperty
import javafx.beans.property.Property
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.scene.control.*
import javafx.scene.control.cell.*
import javafx.scene.layout.Pane
import javafx.scene.text.Text
import javafx.util.Callback
import javafx.util.StringConverter
import tornadofx.control.DatePickerTableCell
import java.time.LocalDate
import java.util.concurrent.Callable
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

fun <T> Pane.spinner(editable: Boolean = false, op: (Spinner<T>.() -> Unit)? = null): Spinner<T> {
    val spinner = Spinner<T>()
    spinner.isEditable = editable
    return opcr(this, spinner, op)
}

fun Pane.spinner(min: Int, max: Int, initialValue: Int, amountToStepBy: Int = 1, editable: Boolean = false, op: (Spinner<Int>.() -> Unit)? = null): Spinner<Int> {
    val spinner = Spinner<Int>(min, max, initialValue, amountToStepBy)
    spinner.isEditable = editable
    return opcr(this, spinner, op)
}

fun Pane.spinner(min: Double, max: Double, initialValue: Double, amountToStepBy: Double = 1.0, editable: Boolean = false, op: (Spinner<Double>.() -> Unit)? = null): Spinner<Double> {
    val spinner = Spinner<Double>(min, max, initialValue, amountToStepBy)
    spinner.isEditable = editable
    return opcr(this, spinner, op)
}

fun <T> Pane.spinner(items: ObservableList<T>, editable: Boolean = false, op: (Spinner<T>.() -> Unit)? = null): Spinner<T> {
    val spinner = Spinner<T>(items)
    spinner.isEditable = editable
    return opcr(this, spinner, op)
}

fun <T> Pane.spinner(valueFactory: SpinnerValueFactory<T>, editable: Boolean = false, op: (Spinner<T>.() -> Unit)? = null): Spinner<T> {
    val spinner = Spinner<T>(valueFactory)
    spinner.isEditable = editable
    return opcr(this, spinner, op)
}

fun <T> Pane.combobox(property: Property<T>? = null, values: ObservableList<T>? = null, op: (ComboBox<T>.() -> Unit)? = null) = opcr(this, ComboBox<T>().apply {
    if (values != null) items = values
    if (property != null) valueProperty().bindBidirectional(property)
}, op)

fun <T> Pane.choicebox(values: ObservableList<T>? = null, changeListener: ((ObservableValue<out T>, T?, T?) -> Unit)? = null, op: (ChoiceBox<T>.() -> Unit)? = null) = opcr(this, ChoiceBox<T>().apply {
    if (values != null) items = values
    if (changeListener != null) selectionModel.selectedItemProperty().addListener(changeListener)
}, op)

fun <T> Pane.listview(values: ObservableList<T>? = null, op: (ListView<T>.() -> Unit)? = null) = opcr(this, ListView<T>().apply {
    values?.let { items = it }
}, op)

fun <S> Pane.tableview(items: ObservableList<S>? = null, op: (TableView<S>.() -> Unit)? = null): TableView<S> {
    val tableview = TableView<S>()
    if (items != null) tableview.items = items
    op?.invoke(tableview)
    children.add(tableview)
    return tableview
}

fun <S> Pane.treeview(root: TreeItem<S>? = null, op: (TreeView<S>.() -> Unit)? = null): TreeView<S> {
    val treeview = TreeView<S>()
    if (root != null) treeview.root = root
    op?.invoke(treeview)
    children.add(treeview)
    return treeview
}

fun <S> Pane.treetableview(root: TreeItem<S>? = null, op: (TreeTableView<S>.() -> Unit)? = null): TreeTableView<S> {
    val treetableview = TreeTableView<S>()
    if (root != null) treetableview.root = root
    op?.invoke(treetableview)
    children.add(treetableview)
    return treetableview
}

fun <T> TreeView<T>.lazyItems(
        rootValue: T,
        leafCheck: (LazyTreeItem<T>) -> Boolean = { it.childFactoryReturnedNull() },
        newItemProcessor: ((LazyTreeItem<T>) -> Unit)? = null,
        childFactory: (LazyTreeItem<T>) -> List<T>?
) {
    fun createItem(value: T) : LazyTreeItem<T> {
        val newItem = LazyTreeItem(value, leafCheck, { childFactory(it)?.map { createItem(it) }})
        newItemProcessor?.invoke(newItem)
        return newItem
    }

    root = createItem(rootValue)
}

class LazyTreeItem<T>(
        value: T,
        val leafCheck: (LazyTreeItem<T>) -> Boolean,
        val childFactory: (LazyTreeItem<T>) -> List<TreeItem<T>>?
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
            childFactoryInvoked = true
            task {
                childFactoryResult = childFactory(this)
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
            childFactoryResult = childFactory(this)
            if (childFactoryResult != null)
                super.getChildren().setAll(childFactoryResult)
        }
        return childFactoryResult
    }
}

fun <S> TableView<S>.makeIndexColumn(name: String = "#", startNumber: Int = 1): TableColumn<S, Number> {
    return TableColumn<S, Number>(name).apply {
        isSortable = false
        prefWidth = width
        this@makeIndexColumn.columns += this
        setCellValueFactory { ReadOnlyObjectWrapper(items.indexOf(it.value) + startNumber) };
    }
}

fun <S, T> TableColumn<S, T>.enableTextWrap(): TableColumn<S, T> {
    setCellFactory {
        TableCell<S, T>().apply {
            val text = Text();
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
 * Create a column using the propertyName of the attribute you want shown.
 */
fun <S, T> TableView<S>.column(title: String, propertyName: String): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = PropertyValueFactory<S, T>(propertyName)
    columns.add(column)
    return column
}

fun <S, T> TableColumn<S, T?>.useComboBox(items: ObservableList<T>, afterCommit: ((TableColumn.CellEditEvent<S, T?>) -> Unit)? = null): TableColumn<S, T?> {
    cellFactory = ComboBoxTableCell.forTableColumn(items)
    setOnEditCommit {
        val property = it.tableColumn.getCellObservableValue(it.rowValue) as ObjectProperty<T?>
        property.value = it.newValue
        afterCommit?.invoke(it)
    }
    return this
}

/**
 * Create an editable DatePicker TableCell. This control requires tornadofx-controls on the classpath.
 */
fun <S> TableColumn<S, LocalDate?>.useDatePicker(converter: StringConverter<LocalDate>? = DatePickerTableCell.DefaultLocalDateConverter(), afterCommit: ((TableColumn.CellEditEvent<S, LocalDate?>) -> Unit)? = null): TableColumn<S, LocalDate?> {
    cellFactory = DatePickerTableCell.forTableColumn(converter)
    setOnEditCommit {
        val property = it.tableColumn.getCellObservableValue(it.rowValue) as ObjectProperty<LocalDate?>
        property.value = it.newValue
        afterCommit?.invoke(it)
    }
    return this
}

inline fun <S, reified T> TableColumn<S, T?>.useTextField(converter: StringConverter<T>? = null, noinline afterCommit: ((TableColumn.CellEditEvent<S, T?>) -> Unit)? = null): TableColumn<S, T?> {
    when (T::class) {
        String::class -> {
            @Suppress("CAST_NEVER_SUCCEEDS")
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

fun <S> TableColumn<S, Boolean?>.useCheckbox(): TableColumn<S, Boolean?> {
    cellFactory = CheckBoxTableCell.forTableColumn(this)
    return this
}

/**
 * Create a column with a value factory that extracts the value from the given mutable
 * property and converts the property to an observable value.
 */
fun <S, T> TableView<S>.column(title: String, prop: KMutableProperty1<S, T>): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { observable(it.value, prop) }
    columns.add(column)
    return column
}

fun <S, T> TreeTableView<S>.column(title: String, prop: KMutableProperty1<S, T>): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = Callback { observable(it.value.value, prop) }
    columns.add(column)
    return column
}

/**
 * Create a column with a value factory that extracts the value from the given property and
 * converts the property to an observable value.
 */
fun <S, T> TableView<S>.column(title: String, prop: KProperty1<S, T>): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { observable(it.value, prop) }
    columns.add(column)
    return column
}

fun <S, T> TreeTableView<S>.column(title: String, prop: KProperty1<S, T>): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = Callback { observable(it.value.value, prop) }
    columns.add(column)
    return column
}

/**
 * Create a column with a value factory that extracts the value from the given ObservableValue property
 */
@JvmName(name = "columnForObservableProperty")
fun <S, T> TableView<S>.column(title: String, prop: KProperty1<S, ObservableValue<T>>): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { prop.call(it.value) }
    columns.add(column)
    return column
}

@JvmName(name = "columnForObservableProperty")
fun <S, T> TreeTableView<S>.column(title: String, prop: KProperty1<S, ObservableValue<T>>): TreeTableColumn<S, T> {
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
    column.cellValueFactory = ReflectionHelper.TableCellValueFunctionRefCallback(observableFn)
    columns.add(column)
    return column
}

inline fun <S, reified T> TreeTableView<S>.column(title: String, observableFn: KFunction<ObservableValue<T>>): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = ReflectionHelper.TreeTableCellValueFunctionRefCallback(observableFn)
    columns.add(column)
    return column
}

/**
 * Create a column with a value factory that extracts the value from the given callback.
 */
fun <S, T> TreeTableView<S>.column(title: String, valueProvider: (TreeTableColumn.CellDataFeatures<S, T>) -> ObservableValue<T>): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = Callback { valueProvider(it) }
    columns.add(column)
    return column
}