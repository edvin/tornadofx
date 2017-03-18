package tornadofx

import javafx.application.Platform
import javafx.beans.property.*
import javafx.beans.value.*
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableSet
import javafx.collections.transformation.FilteredList
import javafx.collections.transformation.SortedList
import javafx.concurrent.Task
import javafx.geometry.Insets
import javafx.scene.control.ListView
import javafx.scene.control.TableView
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.DataFormat
import java.io.File
import java.util.function.Predicate

/**
 * A wrapper for an observable list of items that can be bound to a list control like TableView, ListView etc.
 *
 * The wrapper makes the data sortable and filterable. Configure a filter by setting the
 * predicate property or by calling filterWhen to automatically update the predicate when
 * an observable value changes.
 *
 ** Usage:
 *
 * ```kotlin
 * val table = TableView<Person>()
 * val data = SortedFilteredList(persons).bindTo(table)
 * ```
 *
 * Items can be updated by calling `data.items.setAll` or `data.items.addAll` at a later time.
 */
@Suppress("UNCHECKED_CAST")
class SortedFilteredList<T>(
        val items: ObservableList<T> = FXCollections.observableArrayList(),
        initialPredicate: (T) -> Boolean = { true },
        val filteredItems: FilteredList<T> = FilteredList(items, initialPredicate),
        val sortedItems: SortedList<T> = SortedList(filteredItems)) : ObservableList<T> by sortedItems {

    /**
     * Support editing of the sorted/filtered list. Useful to support editing support in ListView/TableView etc
     */
    override fun set(index: Int, element: T): T {
        val item = sortedItems[index]
        val backingIndex = items.indexOf(item)
        if (backingIndex > -1) {
            items[backingIndex] = element
        }
        return item
    }


    /**
     * Force the filtered list to refilter it's items based on the current predicate without having to configure a new predicate.
     */
    fun refilter() {
        filteredItems.javaClass.getDeclaredMethod("refilter").apply {
            isAccessible = true
            invoke(filteredItems)
        }
    }

    val predicateProperty: ObjectProperty<(T) -> Boolean> = object : SimpleObjectProperty<(T) -> Boolean>() {
        override fun set(newValue: ((T) -> Boolean)) {
            super.set(newValue)
            filteredItems.predicate = Predicate { newValue(it) }
        }
    }
    var predicate by predicateProperty

    /**
     * Bind this data object to the given TableView.
     *
     * The `tableView.items` is set to the underlying sortedItems.
     *
     * The underlying sortedItems.comparatorProperty` is automatically bound to `tableView.comparatorProperty`.
     */
    fun bindTo(tableView: TableView<T>): SortedFilteredList<T> {
        tableView.items = sortedItems
        sortedItems.comparatorProperty().bind(tableView.comparatorProperty())
        return this
    }

    /**
     * Bind this data object to the given ListView.
     *
     * The `listView.items` is set to the underlying sortedItems.
     *
     */
    fun bindTo(listView: ListView<T>): SortedFilteredList<T> {
        listView.items = sortedItems
        return this
    }

    /**
     * Update the filter predicate whenever the given observable changes. The filter expression
     * receives both the observable value and the current list item to evaluate.
     *
     * Convenient for filtering based on a TextField:
     *
     * <pre>
     * textfield {
     *     promptText = "Filtrering"
     *     data.filterWhen(textProperty(), { query, item -> item.matches(query) } )
     * }
     * </pre>
     */
    fun <Q> filterWhen(observable: ObservableValue<Q>, filterExpr: (Q, T) -> Boolean) {
        observable.addListener { observableValue, oldValue, newValue ->
            predicate = { filterExpr(newValue, it) }
        }
    }
}

fun <T> List<T>.observable(): ObservableList<T> = FXCollections.observableList(this)
fun <T> Set<T>.observable(): ObservableSet<T> = FXCollections.observableSet(this)

class FXTask<T>(val status: TaskStatus? = null, val func: FXTask<*>.() -> T) : Task<T>() {
    override fun call() = func(this)

    init {
        status?.item = this
    }

    override fun done() {
        if (status?.item == this)
            status?.item = null
    }

    override public fun updateProgress(workDone: Long, max: Long) {
        super.updateProgress(workDone, max)
    }

    override public fun updateProgress(workDone: Double, max: Double) {
        super.updateProgress(workDone, max)
    }

    @Suppress("UNCHECKED_CAST")
    fun value(v: Any) {
        super.updateValue(v as T)
    }

    override public fun updateTitle(t: String?) {
        super.updateTitle(t)
    }

    override public fun updateMessage(m: String?) {
        super.updateMessage(m)
    }

}

open class TaskStatus : ItemViewModel<Task<*>>() {
    val running: ReadOnlyBooleanProperty = bind { SimpleBooleanProperty().apply { if (item != null) bind(item.runningProperty()) } }
    val message: ReadOnlyStringProperty = bind { SimpleStringProperty().apply { if (item != null) bind(item.messageProperty()) } }
    val title: ReadOnlyStringProperty = bind { SimpleStringProperty().apply { if (item != null) bind(item.titleProperty()) } }
    val progress: ReadOnlyDoubleProperty = bind { SimpleDoubleProperty().apply { if (item != null) bind(item.progressProperty()) } }
}

fun <T> task(taskStatus: TaskStatus? = null, func: FXTask<*>.() -> T): Task<T> = FXTask(taskStatus, func = func).apply {
    setOnFailed({ Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), exception) })
    Thread(this).start()
}

infix fun <T> Task<T>.success(func: (T) -> Unit): Task<T> {
    Platform.runLater {
        setOnSucceeded { func(value) }
    }
    return this
}

infix fun <T> Task<T>.fail(func: (Throwable) -> Unit): Task<T> {
    Platform.runLater {
        setOnFailed { func(exception) }
    }
    return this
}

fun Clipboard.setContent(op: ClipboardContent.() -> Unit) {
    val content = ClipboardContent()
    op(content)
    setContent(content)
}

fun Clipboard.putString(value: String) = setContent { putString(value) }
fun Clipboard.putFiles(files: MutableList<File>) = setContent { putFiles(files) }
fun Clipboard.put(dataFormat: DataFormat, value: Any) = setContent { put(dataFormat, value) }

fun <T> ObservableValue<T>.onChange(op: (T?) -> Unit) = apply { addListener { o, oldValue, newValue -> op(newValue) } }
fun ObservableBooleanValue.onChange(op: (Boolean) -> Unit) = apply { addListener { o, old, new -> op(new ?: false) } }
fun ObservableIntegerValue.onChange(op: (Int) -> Unit) = apply { addListener { o, old, new -> op((new ?: 0).toInt()) } }
fun ObservableLongValue.onChange(op: (Long) -> Unit) = apply { addListener { o, old, new -> op((new ?: 0L).toLong()) } }
fun ObservableFloatValue.onChange(op: (Float) -> Unit) = apply { addListener { o, old, new -> op((new ?: 0f).toFloat()) } }
fun ObservableDoubleValue.onChange(op: (Double) -> Unit) = apply { addListener { o, old, new -> op((new ?: 0.0).toDouble()) } }
fun <T> ObservableList<T>.onChange(op: (ListChangeListener.Change<out T>) -> Unit) = apply {
    addListener(ListChangeListener{ op(it) })
}

/**
 * Create a proxy property backed by calculated data based on a specific property. The setter
 * must return the new value for the backed property.
 * The scope of the getter and setter will be the receiver property
 */
fun <R, T> proxyprop(receiver: Property<R>, getter: Property<R>.() -> T, setter: Property<R>.(T) -> R): ObjectProperty<T> = object : SimpleObjectProperty<T>() {
    init {
        receiver.onChange {
            fireValueChangedEvent()
        }
    }

    override fun invalidated() {
        receiver.value = setter(receiver, super.get())
    }

    override fun get() = getter.invoke(receiver)
    override fun set(v: T) {
        receiver.value = setter(receiver, v)
        super.set(v)
    }
}

/**
 * Create a proxy double property backed by calculated data based on a specific property. The setter
 * must return the new value for the backed property.
 * The scope of the getter and setter will be the receiver property
 */
fun <R> proxypropDouble(receiver: Property<R>, getter: Property<R>.() -> Double, setter: Property<R>.(Double) -> R): DoubleProperty = object : SimpleDoubleProperty() {
    init {
        receiver.onChange {
            fireValueChangedEvent()
        }
    }

    override fun invalidated() {
        receiver.value = setter(receiver, super.get())
    }

    override fun get() = getter.invoke(receiver)
    override fun set(v: Double) {
        receiver.value = setter(receiver, v)
        super.set(v)
    }
}

fun insets(all: Number) = Insets(all.toDouble(), all.toDouble(), all.toDouble(), all.toDouble())
fun insets(horizontal: Number? = null, vertical: Number? = null) = Insets(vertical?.toDouble() ?: 0.0, horizontal?.toDouble() ?: 0.0, vertical?.toDouble() ?: 0.0, horizontal?.toDouble() ?: 0.0)
fun insets(top: Number? = null, right: Number? = null, bottom: Number? = null, left: Number? = null) = Insets(top?.toDouble() ?: 0.0, right?.toDouble() ?: 0.0, bottom?.toDouble() ?: 0.0, left?.toDouble() ?: 0.0)
