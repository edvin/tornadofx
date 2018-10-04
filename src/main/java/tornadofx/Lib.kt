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

// ================================================================
// Clipboard

fun Clipboard.setContent(op: ClipboardContent.() -> Unit) {
    val content = ClipboardContent()
    op(content)
    setContent(content)
}

fun Clipboard.put(dataFormat: DataFormat, value: Any): Unit = setContent { put(dataFormat, value) }
fun Clipboard.putString(value: String): Unit = setContent { putString(value) }
fun Clipboard.putFiles(files: MutableList<File>): Unit = setContent { putFiles(files) }


// ================================================================
// Change Listeners

@Suppress("FunctionName")
inline fun <T> ChangeListener(crossinline listener: (observable: ObservableValue<out T>?, oldValue: T, newValue: T) -> Unit): ChangeListener<T> =
    javafx.beans.value.ChangeListener { observable, oldValue, newValue -> listener(observable, oldValue, newValue) }


fun <T> ObservableValue<T>.onChangeOnce(op: (T?) -> Unit): Unit = onChangeTimes(1, op)

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


fun <T> ObservableValue<T>.onChange(op: (T?) -> Unit): ObservableValue<T> = apply { addListener { _, _, newValue -> op(newValue) } }
fun ObservableIntegerValue.onChange(op: (Int) -> Unit): ObservableIntegerValue = apply { addListener { _, _, newValue -> op(newValue?.toInt() ?: 0) } }
fun ObservableLongValue.onChange(op: (Long) -> Unit): ObservableLongValue = apply { addListener { _, _, newValue -> op(newValue?.toLong() ?: 0L) } }
fun ObservableFloatValue.onChange(op: (Float) -> Unit): ObservableFloatValue = apply { addListener { _, _, newValue -> op(newValue?.toFloat() ?: 0f) } }
fun ObservableDoubleValue.onChange(op: (Double) -> Unit): ObservableDoubleValue = apply { addListener { _, _, newValue -> op(newValue?.toDouble() ?: 0.0) } }
fun ObservableBooleanValue.onChange(op: (Boolean) -> Unit): ObservableBooleanValue = apply { addListener { _, _, newValue -> op(newValue ?: false) } }

fun <T> ObservableList<T>.onChange(op: (ListChangeListener.Change<out T>) -> Unit): ObservableList<T> = apply {
    addListener(ListChangeListener { op(it) })
}


// ================================================================
// Proxy Properties

/**
 * Create a proxy property backed by calculated data based on a specific property. The setter
 * must return the new value for the backed property.
 * The scope of the getter and setter will be the receiver property
 */
fun <R, T> proxyprop(receiver: Property<R>, getter: Property<R>.() -> T, setter: Property<R>.(T) -> R): ObjectProperty<T> =
    object : SimpleObjectProperty<T>() {
        init {
            receiver.onChange { fireValueChangedEvent() }
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
fun <R> proxypropDouble(receiver: Property<R>, getter: Property<R>.() -> Double, setter: Property<R>.(Double) -> R): DoubleProperty =
    object : SimpleDoubleProperty() {
        init {
            receiver.onChange { fireValueChangedEvent() }
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


// ================================================================
// Insets

/** Constructs a new Insets instance with same value for all four offsets. */
fun insets(all: Number? = null): Insets = if (all == null) Insets.EMPTY else Insets(all.toDouble())

/** Constructs a new Insets instance with different offsets for each axis. */
fun insets(
    horizontal: Number? = null,
    vertical: Number? = null
): Insets = Insets(
    vertical?.toDouble() ?: 0.0,
    horizontal?.toDouble() ?: 0.0,
    vertical?.toDouble() ?: 0.0,
    horizontal?.toDouble() ?: 0.0
)

/** Constructs a new Insets instance with four different offsets. */
fun insets(
    top: Number? = null,
    right: Number? = null,
    bottom: Number? = null,
    left: Number? = null
): Insets = Insets(
    top?.toDouble() ?: 0.0,
    right?.toDouble() ?: 0.0,
    bottom?.toDouble() ?: 0.0,
    left?.toDouble() ?: 0.0
)


fun Insets.copy(
    horizontal: Number? = null,
    vertical: Number? = null
): Insets = Insets(
    vertical?.toDouble() ?: this.top,
    horizontal?.toDouble() ?: this.right,
    vertical?.toDouble() ?: this.bottom,
    horizontal?.toDouble() ?: this.left
)

fun Insets.copy(
    top: Number? = null,
    right: Number? = null,
    bottom: Number? = null,
    left: Number? = null
): Insets = Insets(
    top?.toDouble() ?: this.top,
    right?.toDouble() ?: this.right,
    bottom?.toDouble() ?: this.bottom,
    left?.toDouble() ?: this.left
)


val Insets.horizontal: Double get() = (left + right) / 2
val Insets.vertical: Double get() = (top + bottom) / 2
val Insets.all: Double get() = (left + right + top + bottom) / 4


// ================================================================
// String Utilities

fun String.isInt(): Boolean = toIntOrNull() != null
fun String.isLong(): Boolean = toLongOrNull() != null
fun String.isFloat(): Boolean = toFloatOrNull() != null
fun String.isDouble(): Boolean = toDoubleOrNull() != null


// ================================================================
// Collection Utilities

fun <T> Set<T>.observable(): ObservableSet<T> = FXCollections.observableSet(this)
fun <T> List<T>.observable(): ObservableList<T> = FXCollections.observableList(this)
fun <K, V> Map<K, V>.observable(): ObservableMap<K, V> = FXCollections.observableMap(this)


/** [forEach] with the element as receiver. */
inline fun <T> Array<out T>.withEach(action: T.() -> Unit): Unit = forEach(action)

/** [forEach] with the element as receiver. */
inline fun <T> Iterable<T>.withEach(action: T.() -> Unit): Unit = forEach(action)

/** [forEach] with the element as receiver. */
inline fun <T> Sequence<T>.withEach(action: T.() -> Unit): Unit = forEach(action)

/** [forEach] with Map.Entree as receiver. */
inline fun <K, V> Map<out K, V>.withEach(action: Map.Entry<K, V>.() -> Unit): Unit = forEach(action)


/** [map] with the element as receiver. */
inline fun <T, R> Array<out T>.mapEach(action: T.() -> R): List<R> = map(action)

/** [map] with the element as receiver. */
inline fun <T, R> Iterable<T>.mapEach(action: T.() -> R): List<R> = map(action)

/** [map] with the element as receiver. */
fun <T, R> Sequence<T>.mapEach(action: T.() -> R): Sequence<R> = map(action)

/** [map] with Map.Entree as receiver. */
inline fun <K, V, R> Map<out K, V>.mapEach(action: Map.Entry<K, V>.() -> R): List<R> = map(action)


/** [mapTo] with the element as receiver. */
inline fun <T, R, C : MutableCollection<in R>> Array<out T>.mapEachTo(destination: C, action: T.() -> R): C = mapTo(destination, action)

/** [mapTo] with the element as receiver. */
inline fun <T, R, C : MutableCollection<in R>> Iterable<T>.mapEachTo(destination: C, action: T.() -> R): C = mapTo(destination, action)

/** [mapTo] with the element as receiver. */
inline fun <T, R, C : MutableCollection<in R>> Sequence<T>.mapEachTo(destination: C, action: T.() -> R): C = mapTo(destination, action)

/** [mapTo] with Map.Entree as receiver. */
inline fun <K, V, R, C : MutableCollection<in R>> Map<out K, V>.mapEachTo(destination: C, action: Map.Entry<K, V>.() -> R): C = mapTo(destination, action)


/** A wrapper for an observable list of items that can be bound to a list control like TableView, ListView etc.
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
    val sortedItems: SortedList<T> = SortedList(filteredItems)
) : ObservableList<T> {

    val predicateProperty: ObjectProperty<(T) -> Boolean> = object : SimpleObjectProperty<(T) -> Boolean>() {
        override fun set(newValue: ((T) -> Boolean)) {
            super.set(newValue)
            filteredItems.predicate = Predicate { newValue(it) }
        }
    }
    var predicate by predicateProperty // FIXME Specify type

    // Should setAll be forwarded to the underlying list? This might be needed for full editing capabilities,
    // but will affect the ordering of the underlying list
    var setAllPassThrough: Boolean = false

    init {
        items.onChange { refilter() }
    }

    override val size: Int get() = sortedItems.size
    override fun isEmpty(): Boolean = sortedItems.isEmpty()

    override fun contains(element: T): Boolean = element in sortedItems
    override fun containsAll(elements: Collection<T>): Boolean = sortedItems.containsAll(elements)

    override fun iterator(): MutableIterator<T> = sortedItems.iterator()
    override fun listIterator(): MutableListIterator<T> = sortedItems.listIterator()
    override fun listIterator(index: Int): MutableListIterator<T> = sortedItems.listIterator(index)

    override fun add(element: T): Boolean = items.add(element)
    override fun add(index: Int, element: T) {
        val item = sortedItems[index]
        val backingIndex = items.indexOf(item)
        if (backingIndex > -1) {
            items.add(backingIndex, element)
        }
    }

    override fun addAll(vararg elements: T): Boolean = items.addAll(elements)
    override fun addAll(elements: Collection<T>): Boolean = items.addAll(elements)
    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        val item = sortedItems[index]
        val backingIndex = items.indexOf(item)
        if (backingIndex > -1) {
            return items.addAll(backingIndex, elements)
        }
        return false
    }

    /** Support editing of the sorted/filtered list. Useful to support editing support in ListView/TableView etc */
    override fun set(index: Int, element: T): T {
        val item = sortedItems[index]
        val backingIndex = items.indexOf(item)
        if (backingIndex > -1) {
            items[backingIndex] = element
        }
        return item
    }

    override fun setAll(vararg elements: T): Boolean = items.setAll(*elements)
    override fun setAll(col: MutableCollection<out T>?): Boolean = if (setAllPassThrough) items.setAll(col) else false

    override fun remove(element: T): Boolean = items.remove(element)
    override fun remove(from: Int, to: Int) {
        val item = sortedItems[from]
        val backingFromIndex = items.indexOf(item)
        if (backingFromIndex > -1) {
            items.remove(backingFromIndex, items.indexOf(sortedItems[to]))
        }
    }

    override fun removeAt(index: Int): T? {
        val item = sortedItems[index]
        val backingIndex = items.indexOf(item)
        return if (backingIndex > -1) {
            items.removeAt(backingIndex)
        } else {
            null
        }
    }

    override fun removeAll(vararg elements: T): Boolean = items.removeAll(elements)
    override fun removeAll(elements: Collection<T>): Boolean = items.removeAll(elements)

    override fun retainAll(vararg elements: T): Boolean = items.retainAll(elements)
    override fun retainAll(elements: Collection<T>): Boolean = items.retainAll(elements)

    override fun get(index: Int): T = sortedItems[index]
    override fun indexOf(element: T): Int = sortedItems.indexOf(element)
    override fun lastIndexOf(element: T): Int = sortedItems.lastIndexOf(element)

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        val item = sortedItems[fromIndex]
        val backingFromIndex = items.indexOf(item)
        if (backingFromIndex > -1) {
            return items.subList(backingFromIndex, items.indexOf(sortedItems[toIndex - 1]))
        }
        return mutableListOf()
    }

    override fun clear(): Unit = items.clear()


    override fun addListener(listener: ListChangeListener<in T>?): Unit = sortedItems.addListener(listener)
    override fun addListener(listener: InvalidationListener?): Unit = sortedItems.addListener(listener)
    override fun removeListener(listener: ListChangeListener<in T>?): Unit = sortedItems.removeListener(listener)
    override fun removeListener(listener: InvalidationListener?): Unit = sortedItems.removeListener(listener)


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
        observable.addListener { _, _, newValue ->
            predicate = { filterExpr(newValue, it) }
        }
    }
}

// ================================================================
// Media Utilities

fun Media.play(): Unit = MediaPlayer(this).play()
