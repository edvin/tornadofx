package tornadofx

import javafx.beans.InvalidationListener
import javafx.beans.property.*
import javafx.beans.value.*
import javafx.collections.*
import javafx.collections.transformation.FilteredList
import javafx.collections.transformation.SortedList
import javafx.geometry.Insets
import javafx.scene.control.ListView
import javafx.scene.control.TableView
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.DataFormat
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
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
        val sortedItems: SortedList<T> = SortedList(filteredItems)) : ObservableList<T> {

    init {
        items.onChange { refilter() }
    }

    // Should setAll be forwarded to the underlying list? This might be needed for full editing capabilities,
    // but will affect the ordering of the underlying list
    var setAllPassThrough = false
    override val size: Int get() = sortedItems.size
    override fun contains(element: T) = element in sortedItems
    override fun containsAll(elements: Collection<T>) = sortedItems.containsAll(elements)
    override fun get(index: Int) = sortedItems[index]
    override fun indexOf(element: T) = sortedItems.indexOf(element)
    override fun isEmpty() = sortedItems.isEmpty()
    override fun iterator() = sortedItems.iterator()
    override fun lastIndexOf(element: T) = sortedItems.lastIndexOf(element)
    override fun add(element: T) = items.add(element)
    override fun add(index: Int, element: T) {
        val item = sortedItems[index]
        val backingIndex = items.indexOf(item)
        if (backingIndex > -1) {
            items.add(backingIndex, element)
        }
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        val item = sortedItems[index]
        val backingIndex = items.indexOf(item)
        if (backingIndex > -1) {
            return items.addAll(backingIndex, elements)
        }
        return false
    }

    override fun addAll(elements: Collection<T>) = items.addAll(elements)

    override fun clear() = items.clear()
    override fun listIterator() = sortedItems.listIterator()
    override fun listIterator(index: Int) = sortedItems.listIterator(index)
    override fun remove(element: T) = items.remove(element)
    override fun removeAll(elements: Collection<T>) = items.removeAll(elements)
    override fun removeAt(index: Int): T? {
        val item = sortedItems[index]
        val backingIndex = items.indexOf(item)
        return if (backingIndex > -1) {
            items.removeAt(backingIndex)
        } else {
            null
        }
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        val item = sortedItems[fromIndex]
        val backingFromIndex = items.indexOf(item)
        if (backingFromIndex > -1) {
            return items.subList(backingFromIndex, items.indexOf(sortedItems[toIndex - 1]))
        }
        return mutableListOf()
    }

    override fun removeAll(vararg elements: T) = items.removeAll(elements)

    override fun addAll(vararg elements: T) = items.addAll(elements)

    override fun remove(from: Int, to: Int) {
        val item = sortedItems[from]
        val backingFromIndex = items.indexOf(item)
        if (backingFromIndex > -1) {
            items.remove(backingFromIndex, items.indexOf(sortedItems[to]))
        }
    }

    override fun retainAll(vararg elements: T) = items.retainAll(elements)

    override fun retainAll(elements: Collection<T>) = items.retainAll(elements)

    override fun removeListener(listener: ListChangeListener<in T>?) {
        sortedItems.removeListener(listener)
    }

    override fun removeListener(listener: InvalidationListener?) {
        sortedItems.removeListener(listener)
    }

    override fun addListener(listener: ListChangeListener<in T>?) {
        sortedItems.addListener(listener)
    }

    override fun addListener(listener: InvalidationListener?) {
        sortedItems.addListener(listener)
    }

    override fun setAll(col: MutableCollection<out T>?) = if (setAllPassThrough) items.setAll(col) else false

    override fun setAll(vararg elements: T) = items.setAll(*elements)

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
     * Avoid reassigning the property value as that would impede binding.
     */
    fun refilter() {
        val p = predicate
        if (p != null) {
            filteredItems.predicate = Predicate { p(it) }
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
    fun bindTo(tableView: TableView<T>): SortedFilteredList<T> = apply {
        tableView.items = this
        sortedItems.comparatorProperty().bind(tableView.comparatorProperty())
    }

    /**
     * Bind this data object to the given ListView.
     *
     * The `listView.items` is set to the underlying sortedItems.
     *
     */
    fun bindTo(listView: ListView<T>): SortedFilteredList<T> = apply { listView.items = this }

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
fun <K, V> Map<K, V>.observable(): ObservableMap<K, V> = FXCollections.observableMap(this)

fun Clipboard.setContent(op: ClipboardContent.() -> Unit) {
    val content = ClipboardContent()
    op(content)
    setContent(content)
}

fun Clipboard.putString(value: String) = setContent { putString(value) }
fun Clipboard.putFiles(files: MutableList<File>) = setContent { putFiles(files) }
fun Clipboard.put(dataFormat: DataFormat, value: Any) = setContent { put(dataFormat, value) }

inline fun <T> ChangeListener(crossinline listener: (observable: ObservableValue<out T>?, oldValue: T, newValue: T) -> Unit): ChangeListener<T> =
        javafx.beans.value.ChangeListener<T> { observable, oldValue, newValue -> listener(observable, oldValue, newValue) }

/**
 * Listen for changes to this observable. Optionally only listen x times.
 * The lambda receives the changed value when the change occurs, which may be null,
 */
fun <T> ObservableValue<T>.onChangeTimes(times: Int, op: (T?) -> Unit) {
    var counter = 0
    val listener = object : ChangeListener<T> {
        override fun changed(observable: ObservableValue<out T>?, oldValue: T, newValue: T) {
            if (++counter == times) {
                removeListener(this)
            }
            op(newValue)
        }
    }
    addListener(listener)
}

fun <T> ObservableValue<T>.onChangeOnce(op: (T?) -> Unit) = onChangeTimes(1, op)

fun <T> ObservableValue<T>.onChange(op: (T?) -> Unit) = apply { addListener { o, oldValue, newValue -> op(newValue) } }
fun ObservableBooleanValue.onChange(op: (Boolean) -> Unit) = apply { addListener { o, old, new -> op(new ?: false) } }
fun ObservableIntegerValue.onChange(op: (Int) -> Unit) = apply { addListener { o, old, new -> op((new ?: 0).toInt()) } }
fun ObservableLongValue.onChange(op: (Long) -> Unit) = apply { addListener { o, old, new -> op((new ?: 0L).toLong()) } }
fun ObservableFloatValue.onChange(op: (Float) -> Unit) = apply {
    addListener { o, old, new ->
        op((new ?: 0f).toFloat())
    }
}

fun ObservableDoubleValue.onChange(op: (Double) -> Unit) = apply {
    addListener { o, old, new ->
        op((new ?: 0.0).toDouble())
    }
}

fun <T> ObservableList<T>.onChange(op: (ListChangeListener.Change<out T>) -> Unit) = apply {
    addListener(ListChangeListener { op(it) })
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
fun insets(horizontal: Number? = null, vertical: Number? = null) = Insets(
        vertical?.toDouble() ?: 0.0,
        horizontal?.toDouble() ?: 0.0,
        vertical?.toDouble() ?: 0.0,
        horizontal?.toDouble() ?: 0.0
)

fun insets(
        top: Number? = null,
        right: Number? = null,
        bottom: Number? = null,
        left: Number? = null
) = Insets(
        top?.toDouble() ?: 0.0,
        right?.toDouble() ?: 0.0,
        bottom?.toDouble() ?: 0.0,
        left?.toDouble() ?: 0.0
)

fun Insets.copy(
        top: Number? = null,
        right: Number? = null,
        bottom: Number? = null,
        left: Number? = null
) = Insets(
        top?.toDouble() ?: this.top,
        right?.toDouble() ?: this.right,
        bottom?.toDouble() ?: this.bottom,
        left?.toDouble() ?: this.left
)


fun Insets.copy(
        horizontal: Number? = null,
        vertical: Number? = null
) = Insets(
        vertical?.toDouble() ?: this.top,
        horizontal?.toDouble() ?: this.right,
        vertical?.toDouble() ?: this.bottom,
        horizontal?.toDouble() ?: this.left
)

val Insets.horizontal get() = (left + right) / 2
val Insets.vertical get() = (top + bottom) / 2
val Insets.all get() = (left + right + top + bottom) / 4


fun String.isLong() = toLongOrNull() != null
fun String.isInt() = toIntOrNull() != null
fun String.isDouble() = toDoubleOrNull() != null
fun String.isFloat() = toFloatOrNull() != null

/**
 * [forEach] with Map.Entree as receiver.
 */
inline fun <K, V> Map<K, V>.withEach(action: Map.Entry<K, V>.() -> Unit) = forEach(action)

/**
 * [forEach] with the element as receiver.
 */
inline fun <T> Iterable<T>.withEach(action: T.() -> Unit) = forEach(action)

/**
 * [forEach] with the element as receiver.
 */
inline fun <T> Sequence<T>.withEach(action: T.() -> Unit) = forEach(action)

/**
 * [forEach] with the element as receiver.
 */
inline fun <T> Array<T>.withEach(action: T.() -> Unit) = forEach(action)

/**
 * [map] with Map.Entree as receiver.
 */
inline fun <K, V, R> Map<K, V>.mapEach(action: Map.Entry<K, V>.() -> R) = map(action)

/**
 * [map] with the element as receiver.
 */
inline fun <T, R> Iterable<T>.mapEach(action: T.() -> R) = map(action)

/**
 * [map] with the element as receiver.
 */
fun <T, R> Sequence<T>.mapEach(action: T.() -> R) = map(action)

/**
 * [map] with the element as receiver.
 */
inline fun <T, R> Array<T>.mapEach(action: T.() -> R) = map(action)

/**
 * [mapTo] with Map.Entree as receiver.
 */
inline fun <K, V, R, C : MutableCollection<in R>> Map<K, V>.mapEachTo(destination: C, action: Map.Entry<K, V>.() -> R) = mapTo(destination, action)

/**
 * [mapTo] with the element as receiver.
 */
inline fun <T, R, C : MutableCollection<in R>> Iterable<T>.mapEachTo(destination: C, action: T.() -> R) = mapTo(destination, action)

/**
 * [mapTo] with the element as receiver.
 */
fun <T, R, C : MutableCollection<in R>> Sequence<T>.mapEachTo(destination: C, action: T.() -> R) = mapTo(destination, action)

/**
 * [mapTo] with the element as receiver.
 */
fun <T, R, C : MutableCollection<in R>> Array<T>.mapEachTo(destination: C, action: T.() -> R) = mapTo(destination, action)

fun Media.play() = MediaPlayer(this).play()