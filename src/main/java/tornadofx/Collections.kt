package tornadofx

import javafx.beans.WeakListener
import javafx.collections.*
import tornadofx.FX.IgnoreParentBuilder.No
import tornadofx.FX.IgnoreParentBuilder.Once
import java.lang.ref.WeakReference
import java.util.*

/** Moves the given [item] to the specified [newIndex]. */
fun <T> MutableList<T>.move(item: T, newIndex: Int) {
    check(newIndex in indices) { "Invalid index $newIndex for MutableList of size $size" }
    val currentIndex = indexOf(item)
    if (currentIndex < 0) return
    removeAt(currentIndex)
    add(newIndex, item)
}

/** Moves the given item at the [oldIndex] to the [newIndex]. */
fun <T> MutableList<T>.moveAt(oldIndex: Int, newIndex: Int) {
    check(oldIndex in indices) { "Invalid index $oldIndex for MutableList of size $size" }
    check(newIndex in indices) { "Invalid index $newIndex for MutableList of size $size" }
    val item = this[oldIndex]
    removeAt(oldIndex)
    add(newIndex, item)
}

/** Moves all items meeting a [predicate] to the given [newIndex]. */
fun <T> MutableList<T>.moveAll(newIndex: Int, predicate: (T) -> Boolean) {
    check(newIndex in indices) { "Invalid index $newIndex for MutableList of size $size" }
    val split = partition(predicate)
    clear()
    addAll(split.second)
    addAll(if (newIndex >= size) size else newIndex, split.first)
}

/**
 * Moves the given element at specified [index] up by one increment
 * unless it is at the top already which will result in no movement.
 */
fun <T> MutableList<T>.moveUpAt(index: Int) {
    if (index == 0) return
    check(index in indices) { "Invalid index $index for MutableList of size $size" }
    swap(index, index - 1)
}

/**
 * Moves the given element at specified [index] down by one increment
 * unless it is at the bottom already which will result in no movement.
 */
fun <T> MutableList<T>.moveDownAt(index: Int) {
    if (index == size - 1) return
    check(index in indices) { "Invalid index $index for MutableList of size $size" }
    swap(index, index + 1)
}

/**
 * Moves the given [item] up by an index increment unless it is at the top already which will result in no movement.
 * @return `true` if move was successful, `false` otherwise.
 */
fun <T> MutableList<T>.moveUp(item: T): Boolean {
    val currentIndex = indexOf(item)
    if (currentIndex == -1) return false
    val newIndex = (currentIndex - 1)
    if (currentIndex <= 0) return false
    remove(item)
    add(newIndex, item)
    return true
}

/**
 * Moves the given [item] down by an index increment unless it is at the bottom already which will result in no movement.
 * @return `true` if move was successful, `false` otherwise.
 */
fun <T> MutableList<T>.moveDown(item: T): Boolean {
    val currentIndex = indexOf(item)
    if (currentIndex == -1) return false
    val newIndex = (currentIndex + 1)
    if (newIndex >= size) return false
    remove(item)
    add(newIndex, item)
    return true
}


/**
 * Moves the first element that satisfies the given [predicate] up an index, unless its already at the top.
 * @return `true` if move was successful, `false` otherwise or `null` if no element matched the [predicate].
 */
inline fun <T> MutableList<T>.moveUp(crossinline predicate: (T) -> Boolean): Boolean? = find(predicate)?.let { moveUp(it) }

/**
 * Moves the first element that satisfies the given [predicate] down an index, unless its already at the bottom.
 * @return `true` if move was successful, `false` otherwise or `null` if no element matched the [predicate].
 */
inline fun <T> MutableList<T>.moveDown(crossinline predicate: (T) -> Boolean): Boolean? = find(predicate)?.let { moveDown(it) }

/** Moves all the elements that satisfies the given [predicate] up by an index, unless they are already at the top. */
inline fun <T> MutableList<T>.moveUpAll(crossinline predicate: (T) -> Boolean): Unit = asSequence().withIndex()
    .filter { predicate(it.value) }
    .forEach { moveUpAt(it.index) }

/** Moves all the elements that satisfies the given [predicate] down by an index, unless they are already at the bottom. */
inline fun <T> MutableList<T>.moveDownAll(crossinline predicate: (T) -> Boolean): Unit = asSequence().withIndex()
    .filter { predicate(it.value) }
    .forEach { moveDownAt(it.index) }


fun <T> MutableList<T>.moveToTopWhere(predicate: (T) -> Boolean): Unit = asSequence().filter(predicate).forEach {
    remove(it)
    add(0, it)
}

fun <T> MutableList<T>.moveToBottomWhere(predicate: (T) -> Boolean): Unit = asSequence().filter(predicate).forEach {
    remove(it)
    add(lastIndex, it)
}


/** Swaps the position of two items at two respective indices. */
fun <T> MutableList<T>.swap(indexOne: Int, indexTwo: Int): Unit = Collections.swap(this, indexOne, indexTwo)

/** Swaps the index position of two items */
fun <T> MutableList<T>.swap(itemOne: T, itemTwo: T): Unit = swap(indexOf(itemOne), indexOf(itemTwo))


/**
 * Bind this list to the given observable list by converting them into the correct type via the given converter.
 * Changes to the observable list are synced.
 */
fun <SourceType, TargetType> MutableList<TargetType>.bind(
    sourceList: ObservableList<SourceType>,
    converter: (SourceType) -> TargetType
): ListConversionListener<SourceType, TargetType> {
    val ignoringParentConverter: (SourceType) -> TargetType = {
        FX.ignoreParentBuilder = Once
        try {
            converter(it)
        } finally {
            FX.ignoreParentBuilder = No
        }
    }
    val listener = ListConversionListener(this, ignoringParentConverter)
    (this as? ObservableList<TargetType>)?.setAll(sourceList.map(ignoringParentConverter)) ?: run {
        clear()
        addAll(sourceList.map(ignoringParentConverter))
    }
    sourceList.removeListener(listener)
    sourceList.addListener(listener)
    return listener
}

/**
 * Bind this list to the given observable list by converting them into the correct type via the given converter.
 * Changes to the observable list are synced.
 */
fun <SourceType, TargetType> MutableList<TargetType>.bind(
    sourceSet: ObservableSet<SourceType>,
    converter: (SourceType) -> TargetType
): SetConversionListener<SourceType, TargetType> {
    val ignoringParentConverter: (SourceType) -> TargetType = {
        FX.ignoreParentBuilder = Once
        try {
            converter(it)
        } finally {
            FX.ignoreParentBuilder = No
        }
    }
    val listener = SetConversionListener(this, ignoringParentConverter)
    if (this is ObservableList<*>) {
        sourceSet.forEach { source -> listener.sourceToTarget[source] = ignoringParentConverter(source) }
        (this as ObservableList<TargetType>).setAll(listener.sourceToTarget.values)
    } else {
        clear()
        addAll(sourceSet.map(ignoringParentConverter))
    }
    sourceSet.removeListener(listener)
    sourceSet.addListener(listener)
    return listener
}

fun <SourceTypeKey, SourceTypeValue, TargetType> MutableList<TargetType>.bind(
    sourceMap: ObservableMap<SourceTypeKey, SourceTypeValue>,
    converter: (SourceTypeKey, SourceTypeValue) -> TargetType
): MapConversionListener<SourceTypeKey, SourceTypeValue, TargetType> {
    val ignoringParentConverter: (SourceTypeKey, SourceTypeValue) -> TargetType = { key, value ->
        FX.ignoreParentBuilder = FX.IgnoreParentBuilder.Once
        try {
            converter(key, value)
        } finally {
            FX.ignoreParentBuilder = FX.IgnoreParentBuilder.No
        }
    }
    val listener = MapConversionListener(this, ignoringParentConverter)
    if (this is ObservableList<*>) {
        sourceMap.forEach { source -> listener.sourceToTarget[source] = ignoringParentConverter(source.key, source.value) }
        (this as ObservableList<TargetType>).setAll(listener.sourceToTarget.values)
    } else {
        clear()
        addAll(sourceMap.map { ignoringParentConverter(it.key, it.value) })
    }
    sourceMap.removeListener(listener)
    sourceMap.addListener(listener)
    return listener
}


/**
 * Listens to changes on a list of SourceType and keeps the target list in sync by converting
 * each object into the TargetType via the supplied converter.
 */
class ListConversionListener<SourceType, TargetType>(
    targetList: MutableList<TargetType>,
    val converter: (SourceType) -> TargetType
) : ListChangeListener<SourceType>, WeakListener {

    internal val targetRef: WeakReference<MutableList<TargetType>> = WeakReference(targetList)

    override fun onChanged(change: ListChangeListener.Change<out SourceType>) {
        val list = targetRef.get()
        if (list == null) {
            change.list.removeListener(this)
        } else {
            while (change.next()) {
                if (change.wasPermutated()) {
                    list.subList(change.from, change.to).clear()
                    list.addAll(change.from, change.list.subList(change.from, change.to).map(converter))
                } else {
                    if (change.wasRemoved()) list.subList(change.from, change.from + change.removedSize).clear()
                    if (change.wasAdded()) list.addAll(change.from, change.addedSubList.map(converter))
                }
            }
        }
    }

    override fun wasGarbageCollected(): Boolean = targetRef.get() == null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ListConversionListener<*, *>) return false

        val thisList = targetRef.get() ?: return false
        return thisList === other.targetRef.get()
    }

    override fun hashCode(): Int = targetRef.get()?.hashCode() ?: 0
}

/**
 * Listens to changes on a set of SourceType and keeps the target list in sync by converting
 * each object into the TargetType via the supplied converter.
 */
class SetConversionListener<SourceType, TargetType>(
    targetList: MutableList<TargetType>,
    val converter: (SourceType) -> TargetType
) : SetChangeListener<SourceType>, WeakListener {
    internal val targetRef: WeakReference<MutableList<TargetType>> = WeakReference(targetList)
    internal val sourceToTarget = HashMap<SourceType, TargetType>()

    override fun onChanged(change: SetChangeListener.Change<out SourceType>) {
        val list = targetRef.get()
        if (list == null) {
            change.set.removeListener(this)
            sourceToTarget.clear()
        } else {
            if (change.wasRemoved()) {
                list.remove(sourceToTarget[change.elementRemoved])
                sourceToTarget.remove(change.elementRemoved)
            }
            if (change.wasAdded()) {
                val converted = converter(change.elementAdded)
                sourceToTarget[change.elementAdded] = converted
                list.add(converted)
            }
        }
    }

    override fun wasGarbageCollected(): Boolean = targetRef.get() == null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SetConversionListener<*, *>) return false

        val thisList = targetRef.get() ?: return false
        return thisList === other.targetRef.get()
    }

    override fun hashCode(): Int = targetRef.get()?.hashCode() ?: 0
}

/**
 * Listens to changes on a Map of SourceTypeKey to SourceTypeValue and keeps the target list in sync by converting
 * each object into the TargetType via the supplied converter.
 */
class MapConversionListener<SourceTypeKey, SourceTypeValue, TargetType>(
    targetList: MutableList<TargetType>,
    val converter: (SourceTypeKey, SourceTypeValue) -> TargetType
) : MapChangeListener<SourceTypeKey, SourceTypeValue>, WeakListener {

    internal val targetRef: WeakReference<MutableList<TargetType>> = WeakReference(targetList)
    internal val sourceToTarget = HashMap<Map.Entry<SourceTypeKey, SourceTypeValue>, TargetType>()

    override fun onChanged(change: MapChangeListener.Change<out SourceTypeKey, out SourceTypeValue>) {
        val list = targetRef.get()
        if (list == null) {
            change.map.removeListener(this)
        } else {
            if (change.wasRemoved()) list.remove(converter(change.key, change.valueRemoved))
            if (change.wasAdded()) list.add(converter(change.key, change.valueAdded))
        }
    }

    override fun wasGarbageCollected(): Boolean = targetRef.get() == null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MapConversionListener<*, *, *>) return false

        val thisList = targetRef.get() ?: return false
        return thisList === other.targetRef.get()
    }

    override fun hashCode(): Int = targetRef.get()?.hashCode() ?: 0
}


fun <T> ObservableList<T>.invalidate() {
    if (isNotEmpty()) this[0] = this[0]
}

fun <T> observableList(vararg entries: T): ObservableList<T> = FXCollections.observableArrayList<T>(entries.toList())
