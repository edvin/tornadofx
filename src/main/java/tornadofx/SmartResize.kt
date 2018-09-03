package tornadofx

import javafx.application.Platform
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TreeTableColumn
import javafx.scene.control.TreeTableView
import javafx.util.Callback
import tornadofx.ResizeType.*
import tornadofx.adapters.*

import kotlin.collections.set
import kotlin.math.abs
import kotlin.reflect.KClass

//private const val SMART_RESIZE_INSTALLED = "tornadofx.smartResizeInstalled"
private const val SMART_RESIZE = "tornadofx.smartResize"
private const val IS_SMART_RESIZING = "tornadofx.isSmartResizing"
const val RESIZE_TYPE_KEY = "tornadofx.smartColumnResizeType"

private typealias GroupedColumns = Map<KClass<out ResizeType>, List<TornadoFXColumn<out Any?>>>

sealed class ResizeType(val isResizable: Boolean) {
    class Pref(val width: Number) : ResizeType(true)
    class Fixed(val width: Number) : ResizeType(false)
    class Weight(val weight: Number, val padding: Number = 0.0, val minContentWidth: Boolean = false, var minRecorded: Boolean = false) : ResizeType(true)
    class Pct(val value: Number) : ResizeType(true)
    class Content(val padding: Number = 0.0, val useAsMin: Boolean = false, val useAsMax: Boolean = false, var minRecorded: Boolean = false, var maxRecorded: Boolean = false) : ResizeType(true)
    class Remaining : ResizeType(true)

    var delta: Double = 0.0
}

typealias TableViewResizeCallback = Callback<TableView.ResizeFeatures<out Any>, Boolean>
typealias TreeTableViewResizeCallback = Callback<TreeTableView.ResizeFeatures<out Any>, Boolean>


class SmartResize private constructor() : TableViewResizeCallback {

    override fun call(param: TableView.ResizeFeatures<out Any>) = resizeCall(param.toTornadoFXFeatures()) { table ->
        if (!isPolicyInstalled(table)) install(table)
    }

    fun requestResize(table: TableView<*>) {
        Platform.runLater {
            call(TableView.ResizeFeatures(table, null, 0.0))
        }
    }

    companion object {
        val POLICY = SmartResize()
        val ResizeTypeKey = "tornadofx.smartColumnResizeType"

        private var TableView<*>.isSmartResizing: Boolean
            get() = properties[IS_SMART_RESIZING] == true
            set(value) {
                properties[IS_SMART_RESIZING] = value
            }

        private val policyChangeListener = ChangeListener<Callback<TableView.ResizeFeatures<*>, Boolean>> { observable, _, newValue ->
            val table = (observable as ObjectProperty<*>).bean as TableView<*>
            if (newValue == POLICY) install(table) else uninstall(table)
        }

        private val itemsChangeListener = ChangeListener<ObservableList<*>> { observable, _, _ ->
            val table = (observable as ObjectProperty<*>).bean as TableView<*>
            POLICY.requestResize(table)
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

            if (table?.isSmartResizing == false) {
                val rt = column.resizeType
                val diff = oldValue.toDouble() - newValue.toDouble()
                rt.delta -= diff
                POLICY.call(TableView.ResizeFeatures<Any>(table as TableView<Any>?, null, 0.0))
            }
        }


        private fun isPolicyInstalled(table: TableView<*>) = table.properties[SMART_RESIZE] == true

        private fun install(table: TableView<*>) {
            table.columnResizePolicyProperty().addListener(policyChangeListener)
            table.columns.addListener(columnsChangeListener)
            table.itemsProperty().addListener(itemsChangeListener)
            table.columns.forEach { it.widthProperty().addListener(columnWidthChangeListener) }
            table.properties[SMART_RESIZE] = true
        }

        private fun uninstall(table: TableView<*>) {
            table.columnResizePolicyProperty().removeListener(policyChangeListener)
            table.columns.removeListener(columnsChangeListener)
            table.itemsProperty().removeListener(itemsChangeListener)
            table.columns.forEach { it.widthProperty().removeListener(columnWidthChangeListener) }
            table.properties.remove(SMART_RESIZE)
        }
    }
}

class TreeTableSmartResize private constructor() : TreeTableViewResizeCallback {

    override fun call(param: TreeTableView.ResizeFeatures<out Any>) = resizeCall(param.toTornadoFXResizeFeatures()) { table ->
        if (!isPolicyInstalled(table)) install(table)
    }

    fun requestResize(table: TreeTableView<*>) {
        Platform.runLater {
            call(TreeTableView.ResizeFeatures(table, null, 0.0))
        }
    }

    companion object {
        val POLICY = TreeTableSmartResize()
        const val ResizeTypeKey = "tornadofx.smartColumnResizeType"

        internal var TreeTableView<*>.isSmartResizing: Boolean
            get() = properties[IS_SMART_RESIZING] == true
            set(value) {
                properties[IS_SMART_RESIZING] = value
            }

        private val policyChangeListener = ChangeListener<Callback<TreeTableView.ResizeFeatures<*>, Boolean>> { observable, oldValue, newValue ->
            val table = (observable as ObjectProperty<*>).bean as TreeTableView<*>
            if (newValue == POLICY) install(table) else uninstall(table)
        }

        private val columnsChangeListener = ListChangeListener<TreeTableColumn<*, *>> { s ->
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
            val column = (observable as ReadOnlyProperty<*>).bean as TreeTableColumn<*, *>
            val table: TreeTableView<out Any>? = column.treeTableView

            if (table != null && !table.isSmartResizing) {
                val rt = column.resizeType
                val diff = oldValue.toDouble() - newValue.toDouble()
                rt.delta -= diff
                POLICY.call(TreeTableView.ResizeFeatures<Any>(table as TreeTableView<Any>?, null, 0.0))
            }

        }

        private fun isPolicyInstalled(table: TreeTableView<*>) = table.properties[SMART_RESIZE] == true

        private fun install(table: TreeTableView<*>) {
            table.columnResizePolicyProperty().addListener(policyChangeListener)
            table.columns.addListener(columnsChangeListener)
            table.columns.forEach { it.widthProperty().addListener(columnWidthChangeListener) }
            table.properties[SMART_RESIZE] = true
        }

        private fun uninstall(table: TreeTableView<*>) {
            table.columnResizePolicyProperty().removeListener(policyChangeListener)
            table.columns.removeListener(columnsChangeListener)
            table.columns.forEach { it.widthProperty().removeListener(columnWidthChangeListener) }
            table.properties.remove(SMART_RESIZE)
        }
    }
}

fun TableView<*>.smartResize() {
    columnResizePolicy = SmartResize.POLICY
}

fun TableView<*>.requestResize() {
    SmartResize.POLICY.requestResize(this)
}

fun TreeTableView<*>.smartResize() {
    columnResizePolicy = TreeTableSmartResize.POLICY
}

fun TreeTableView<*>.requestResize() {
    TreeTableSmartResize.POLICY.requestResize(this)
}

/**
 * Get the width of the area available for columns inside the TableView
 */
fun TableView<*>.getContentWidth() = TableView::class.java.getDeclaredField("contentWidth").let {
    it.isAccessible = true
    it.get(this@getContentWidth) as Double
}

/**
 * Get the width of the area available for columns inside the TableView
 */
fun TreeTableView<*>.getContentWidth() = TreeTableView::class.java.getDeclaredField("contentWidth").let {
    it.isAccessible = true
    it.get(this@getContentWidth) as Double
}

val TableView<*>.contentColumns: List<TableColumn<*, *>>
    get() = columns.flatMap {
        if (it.columns.isEmpty()) listOf(it) else it.columns
    }

val TreeTableView<*>.contentColumns: List<TreeTableColumn<*, *>>
    get() = columns.flatMap {
        if (it.columns.isEmpty()) listOf(it) else it.columns
    }

internal var TableColumn<*, *>.resizeType: ResizeType
    get() = resizeTypeProperty().value
    set(value) {
        resizeTypeProperty().value = value
    }

internal var TreeTableColumn<*, *>.resizeType: ResizeType
    get() = resizeTypeProperty().value
    set(value) {
        resizeTypeProperty().value = value
    }

@Suppress("UNCHECKED_CAST")
internal fun TableColumn<*, *>.resizeTypeProperty() =
        properties.getOrPut(SmartResize.ResizeTypeKey) { SimpleObjectProperty(Content()) } as ObjectProperty<ResizeType>

@Suppress("UNCHECKED_CAST")
internal fun TreeTableColumn<*, *>.resizeTypeProperty() =
        properties.getOrPut(TreeTableSmartResize.ResizeTypeKey) { SimpleObjectProperty(Content()) } as ObjectProperty<ResizeType>

fun <S, T> TableColumn<S, T>.fixedWidth(width: Number) = apply {
    minWidth = width.toDouble()
    maxWidth = width.toDouble()
    resizeType = Fixed(width.toDouble())
}

fun <S, T> TreeTableColumn<S, T>.fixedWidth(width: Number) = apply {
    minWidth = width.toDouble()
    maxWidth = width.toDouble()
    resizeType = Fixed(width.toDouble())
}

fun <S, T> TableColumn<S, T>.minWidth(width: Number) = apply { minWidth = width.toDouble() }
fun <S, T> TreeTableColumn<S, T>.minWidth(width: Number) = apply { minWidth = width.toDouble() }

fun <S, T> TableColumn<S, T>.maxWidth(width: Number) = apply { maxWidth = width.toDouble() }
fun <S, T> TreeTableColumn<S, T>.maxWidth(width: Number) = apply { maxWidth = width.toDouble() }

fun <S, T> TableColumn<S, T>.prefWidth(width: Number) = apply { prefWidth = width.toDouble() }
fun <S, T> TreeTableColumn<S, T>.prefWidth(width: Number) = apply { prefWidth = width.toDouble() }

fun <S, T> TableColumn<S, T>.remainingWidth() = apply { resizeType = Remaining() }
fun <S, T> TreeTableColumn<S, T>.remainingWidth() = apply { resizeType = Remaining() }

fun <S, T> TableColumn<S, T>.weightedWidth(weight: Number, padding: Double = 0.0, minContentWidth: Boolean = false) = apply {
    resizeType = Weight(weight.toDouble(), padding, minContentWidth)
}

fun <S, T> TreeTableColumn<S, T>.weightedWidth(weight: Number, padding: Double = 0.0, minContentWidth: Boolean = false) = apply {
    resizeType = Weight(weight.toDouble(), padding, minContentWidth)
}

fun <S, T> TableColumn<S, T>.pctWidth(pct: Number) = apply {
    resizeType = Pct(pct.toDouble())
}

fun <S, T> TreeTableColumn<S, T>.pctWidth(pct: Number) = apply {
    resizeType = Pct(pct.toDouble())
}

/**
 * Make the column fit the content plus an optional padding width. Optionally constrain the min or max width to be this width.
 */
fun <S, T> TableColumn<S, T>.contentWidth(padding: Double = 0.0, useAsMin: Boolean = false, useAsMax: Boolean = false) = apply {
    resizeType = Content(padding, useAsMin, useAsMax)
}

/**
 * Make the column fit the content plus an optional padding width. Optionally constrain the min or max width to be this width.
 */
fun <S, T> TreeTableColumn<S, T>.contentWidth(padding: Number = 0.0, useAsMin: Boolean = false, useAsMax: Boolean = false) = apply {
    resizeType = Content(padding, useAsMin, useAsMax)
}

internal var TornadoFXColumn<*>.resizeType: ResizeType
    get() = resizeTypeProperty().value
    set(value) {
        resizeTypeProperty().value = value
    }

@Suppress("UNCHECKED_CAST")
internal fun TornadoFXColumn<*>.resizeTypeProperty() =
        properties.getOrPut(RESIZE_TYPE_KEY) { SimpleObjectProperty(Content()) } as ObjectProperty<ResizeType>

var TornadoFXTable<*, *>.isSmartResizing: Boolean
    get() = properties[IS_SMART_RESIZING] == true
    set(value) {
        properties[IS_SMART_RESIZING] = value
    }

fun <S> TornadoFXColumn<S>.fixedWidth(width: Number) = apply {
    minWidth = width.toDouble()
    maxWidth = width.toDouble()
    resizeType = Fixed(width.toDouble())
}


fun <S> TornadoFXColumn<S>.minWidth(width: Number) = apply {
    minWidth = width.toDouble()
}


fun <S> TornadoFXColumn<S>.maxWidth(width: Number) = apply {
    maxWidth = width.toDouble()
}

fun <S> TornadoFXColumn<S>.prefWidth(width: Number) = apply {
    prefWidth = width.toDouble()
}

fun <S> TornadoFXColumn<S>.remainingWidth() = apply {
    resizeType = Remaining()
}

fun <S> TornadoFXColumn<S>.weightedWidth(weight: Number, padding: Double = 0.0, minContentWidth: Boolean = false) = apply {
    resizeType = Weight(weight.toDouble(), padding, minContentWidth)
}

fun <S> TornadoFXColumn<S>.pctWidth(pct: Number) = apply {
    resizeType = Pct(pct.toDouble())
}

/**
 * Make the column fit the content plus an optional padding width. Optionally constrain the min or max width to be this width.
 */
fun <S> TornadoFXColumn<S>.contentWidth(padding: Double = 0.0, useAsMin: Boolean = false, useAsMax: Boolean = false) = apply {
    resizeType = Content(padding, useAsMin, useAsMax)
}

fun <S, T : Any> TornadoFXTable<S, T>.resizeColumnsToFitContent(resizeColumns: List<TornadoFXColumn<S>> = contentColumns, maxRows: Int = 50, afterResize: () -> Unit = {}) {
    when (table) {
        is TableView<*> -> (table as TableView<*>).resizeColumnsToFitContent(resizeColumns.map { it.column as TableColumn<*, *> }, maxRows, afterResize)
        is TreeTableView<*> -> (table as TreeTableView<*>).resizeColumnsToFitContent(resizeColumns.map { it.column as TreeTableColumn<*, *> }, maxRows, afterResize)
        else -> throw IllegalArgumentException("Unable to resize columns for unknown table type $table")
    }
}

fun <TABLE : Any> resizeCall(
        param: TornadoFXResizeFeatures<*, TABLE>,
        installIfNeeded: (TABLE) -> Unit
): Boolean {
    param.table.isSmartResizing = true
    val paramColumn = param.column
    try {
        if (paramColumn == null) {
            val contentWidth = param.table.contentWidth
            if (contentWidth == 0.0) return false
            installIfNeeded(param.table.table)
            resizeAllColumns(param.table, contentWidth)
        } else {
            // Handle specific column size operation
            val rt = paramColumn.resizeType

            if (!rt.isResizable) return false

            val targetWidth = paramColumn.width + param.delta

            if (!paramColumn.isLegalWidth(targetWidth)) return false

            // Prepare to adjust the right column by the same amount we subtract or add to this column
            val rightColDelta = param.delta * -1.0
            val colIndex = param.table.contentColumns.indexOf(paramColumn)

            val rightCol = param.table.contentColumns
                    .filterIndexed { i, c -> i > colIndex && c.resizeType.isResizable }.firstOrNull {
                        val newWidth = it.width + rightColDelta
                        it.isLegalWidth(newWidth)
                    } ?: return false

            // Apply negative delta and set new with for the right column
            with(rightCol) {
                resizeType.delta += rightColDelta
                prefWidth = width + rightColDelta
            }

            // Apply delta and set new width for the resized column
            with(paramColumn) {
                rt.delta += param.delta
                prefWidth = width + param.delta
            }
            return true
        }
        return true
    } finally {
        param.table.isSmartResizing = false
    }
}

private fun <COLUMN, TABLE : Any> resizeAllColumns(table: TornadoFXTable<COLUMN, TABLE>, contentWidth: Double) {
    fun <S, T : Any> List<TornadoFXColumn<S>>.adjustTo(table: TornadoFXTable<S, T>) = table.resizeColumnsToFitContent(this)

    val groupedColumns = table.contentColumns.groupBy { it.resizeType::class }

    var remainingWidth = contentWidth -
            groupedColumns.resizeFixedColumns() -
            groupedColumns.resizePreferredColumns()

    groupedColumns[Content::class]?.adjustTo(table)

    remainingWidth -=
            groupedColumns.resizeContentColumns() +
            groupedColumns.resizePctColumns(contentWidth)


    val totalWeight = groupedColumns.totalWeightOfWeightedColumns() + groupedColumns.countValues(Remaining::class)
    val widthPerWeight = remainingWidth/totalWeight

    remainingWidth -=  groupedColumns.resizeWeightedColumns(widthPerWeight) +
            groupedColumns.resizeRemainingColumns(widthPerWeight)


    if (remainingWidth > 0.0) table.divideRemainingWith(remainingWidth)
    else if (remainingWidth < 0.0) table.takeBackOverflowedWith(remainingWidth)
}

fun <K> Map<K,Collection<*>>.countValues(key: K) = this[key]?.size ?: 0


private fun GroupedColumns.totalWeightOfWeightedColumns() = this[Weight::class]?.run {
    map { it.resizeType as ResizeType.Weight }.sumByDouble { it.weight as Double }
} ?: 0.0


private fun GroupedColumns.resizeWeightedColumns(widthPerWeight: Double): Double {
    var spaceNeeded = 0.0
    this[Weight::class]?.forEach {
        val rt = it.resizeType as ResizeType.Weight
        if (rt.minContentWidth && !rt.minRecorded) {
            it.minWidth = it.width + rt.padding.toDouble()
            rt.minRecorded = true
        }
        it.prefWidth = maxOf(it.minWidth, (widthPerWeight * rt.weight.toDouble()) + rt.delta + rt.padding.toDouble())
        spaceNeeded += it.width
    }
    return spaceNeeded
}

private fun GroupedColumns.resizeRemainingColumns(widthPerWeight: Double): Double {
    var spaceNeeded = 0.0
    this[Remaining::class]?.withEach{
        prefWidth = maxOf(minWidth, widthPerWeight + resizeType.delta)
        spaceNeeded += width
    }
    return spaceNeeded
}

private fun <TABLE : Any> TornadoFXTable<out Any?, TABLE>.takeBackOverflowedWith(remainingWidth: Double) {
    var stillToTake = remainingWidth
    contentColumns.filter { it.resizeType.isResizable }.reduceSorted(
            // sort the column by the largest reduction potential: the gap between size and minSize
            sorter = { minWidth - width },
            filter = { minWidth < width }
    ) {
        val reduceBy = minOf(1.0, abs(stillToTake))
        val toWidth = it.width - reduceBy
        it.prefWidth = toWidth
        stillToTake += reduceBy

        stillToTake < 0.0
    }
}

private fun <TABLE : Any> TornadoFXTable<out Any?, TABLE>.divideRemainingWith(remainingWidth: Double) {
    // Give remaining width to the right-most resizable column
    val rightMostResizable = contentColumns.lastOrNull { it.resizeType.isResizable }
    rightMostResizable?.apply {
        prefWidth = width + remainingWidth
    }
}

private fun GroupedColumns.resizePctColumns(contentWidth: Double): Double {
    var spaceNeeded = 0.0
    this[Pct::class]?.also { pctColumn ->
        val widthPerPct = contentWidth / 100.0
        pctColumn.forEach {
            val rt = it.resizeType as ResizeType.Pct
            it.prefWidth = (widthPerPct * rt.value.toDouble()) + rt.delta.toDouble()
            spaceNeeded += it.width
        }
    }
    return spaceNeeded
}

private fun GroupedColumns.resizeContentColumns(): Double {
    // Content columns are resized to their content and adjusted for resize-delta that affected them
    var spaceNeeded = 0.0
    this[Content::class]?.also { contentColumns ->
        contentColumns.forEach {
            val rt = it.resizeType as ResizeType.Content

            it.prefWidth = it.width + rt.delta + rt.padding.toDouble()

            if (rt.hasUnrecordedMin()) it.recordMinFrom(rt)
            if (rt.hasUnrecordedMax()) it.recordMaxFrom(rt)

            spaceNeeded += it.width
        }
    }
    return spaceNeeded
}

private const val DEFAULT_COLUMN_WIDTH = 80.0
private fun ResizeType.Content.hasUnrecordedMin() = !minRecorded && useAsMin
private fun TornadoFXColumn<*>.recordMinFrom(content: ResizeType.Content) {
    if (width != DEFAULT_COLUMN_WIDTH) {
        minWidth = width
        content.minRecorded = true
    }
}

private fun ResizeType.Content.hasUnrecordedMax() = !maxRecorded && useAsMax
private fun TornadoFXColumn<*>.recordMaxFrom(content: ResizeType.Content) {
    if (width != DEFAULT_COLUMN_WIDTH) {
        maxWidth = width
        content.maxRecorded = true
    }
}

private fun GroupedColumns.resizePreferredColumns(): Double {
    var spaceNeeded = 0.0
    this[Pref::class]?.forEach {
        val rt = it.resizeType as ResizeType.Pref
        it.prefWidth = rt.width.toDouble() + rt.delta.toDouble()
        spaceNeeded += it.width
    }
    return spaceNeeded
}



private fun GroupedColumns.resizeFixedColumns(): Double {
    var spaceNeeded = 0.0
    this[Fixed::class]?.forEach {
        val rt = it.resizeType as Fixed
        it.prefWidth = rt.width.toDouble()
        spaceNeeded += it.width
    }
    return spaceNeeded
}

/**
 * Removes elements from the list in a sorted way with a cycle:
 *
 * 1. removes elements that fail the [filter]
 * 2. find the first sorted element with the [sorter]
 * 3. change the state of the element with the [iteration]
 * 4. if [iteration] returns true, start again.
 *
 * @return the reduced list
 */
private inline fun <T, R : Comparable<R>> List<T>.reduceSorted(
        crossinline sorter: T.() -> R,
        noinline filter: T.() -> Boolean,
        iteration: (T) -> Boolean
): List<T> {
    val removingList = asSequence().filter(filter).sortedBy(sorter).toMutableList()
    while (removingList.any()) {
        val element = removingList.first()
        if (!iteration(element)) break
        if (!element.filter()) removingList.remove(element)
        removingList.sortBy(sorter)
    }
    return removingList
}