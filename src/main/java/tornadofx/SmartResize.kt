package tornadofx

import com.sun.javafx.scene.control.skin.TreeTableViewSkin
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
import tornadofx.adapters.*

import kotlin.collections.set

//private const val SMART_RESIZE_INSTALLED = "tornadofx.smartResizeInstalled"
private const val SMART_RESIZE = "tornadofx.smartResize"
private const val IS_SMART_RESIZING = "tornadofx.isSmartResizing"
const val RESIZE_TYPE_KEY = "tornadofx.smartColumnResizeType"


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

        internal var TableView<*>.isSmartResizing: Boolean
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
        properties.getOrPut(SmartResize.ResizeTypeKey) { SimpleObjectProperty(ResizeType.Content()) } as ObjectProperty<ResizeType>

@Suppress("UNCHECKED_CAST")
internal fun TreeTableColumn<*, *>.resizeTypeProperty() =
        properties.getOrPut(TreeTableSmartResize.ResizeTypeKey) { SimpleObjectProperty(ResizeType.Content()) } as ObjectProperty<ResizeType>

fun <S, T> TableColumn<S, T>.fixedWidth(width: Number) = apply {
    minWidth = width.toDouble()
    maxWidth = width.toDouble()
    resizeType = ResizeType.Fixed(width.toDouble())
}

fun <S, T> TreeTableColumn<S, T>.fixedWidth(width: Number) = apply {
    minWidth = width.toDouble()
    maxWidth = width.toDouble()
    resizeType = ResizeType.Fixed(width.toDouble())
}

fun <S, T> TableColumn<S, T>.minWidth(width: Number) = apply { minWidth = width.toDouble() }
fun <S, T> TreeTableColumn<S, T>.minWidth(width: Number) = apply { minWidth = width.toDouble() }

fun <S, T> TableColumn<S, T>.maxWidth(width: Number) = apply { maxWidth = width.toDouble() }
fun <S, T> TreeTableColumn<S, T>.maxWidth(width: Number) = apply { maxWidth = width.toDouble() }

fun <S, T> TableColumn<S, T>.prefWidth(width: Number) = apply { prefWidth = width.toDouble() }
fun <S, T> TreeTableColumn<S, T>.prefWidth(width: Number) = apply { prefWidth = width.toDouble() }

fun <S, T> TableColumn<S, T>.remainingWidth() = apply { resizeType = ResizeType.Remaining() }
fun <S, T> TreeTableColumn<S, T>.remainingWidth() = apply { resizeType = ResizeType.Remaining() }

fun <S, T> TableColumn<S, T>.weightedWidth(weight: Number, padding: Double = 0.0, minContentWidth: Boolean = false) = apply {
    resizeType = ResizeType.Weight(weight.toDouble(), padding, minContentWidth)
}

fun <S, T> TreeTableColumn<S, T>.weightedWidth(weight: Number, padding: Double = 0.0, minContentWidth: Boolean = false) = apply {
    resizeType = ResizeType.Weight(weight.toDouble(), padding, minContentWidth)
}

fun <S, T> TableColumn<S, T>.pctWidth(pct: Number) = apply {
    resizeType = ResizeType.Pct(pct.toDouble())
}

fun <S, T> TreeTableColumn<S, T>.pctWidth(pct: Number) = apply {
    resizeType = ResizeType.Pct(pct.toDouble())
}

/**
 * Make the column fit the content plus an optional padding width. Optionally constrain the min or max width to be this width.
 */
fun <S, T> TableColumn<S, T>.contentWidth(padding: Double = 0.0, useAsMin: Boolean = false, useAsMax: Boolean = false) = apply {
    resizeType = ResizeType.Content(padding, useAsMin, useAsMax)
}

/**
 * Make the column fit the content plus an optional padding width. Optionally constrain the min or max width to be this width.
 */
fun <S, T> TreeTableColumn<S, T>.contentWidth(padding: Number = 0.0, useAsMin: Boolean = false, useAsMax: Boolean = false) = apply {
    resizeType = ResizeType.Content(padding, useAsMin, useAsMax)
}

internal var TornadoFXColumn<*>.resizeType: ResizeType
    get() = resizeTypeProperty().value
    set(value) {
        resizeTypeProperty().value = value
    }

@Suppress("UNCHECKED_CAST")
internal fun TornadoFXColumn<*>.resizeTypeProperty() =
        properties.getOrPut(RESIZE_TYPE_KEY) { SimpleObjectProperty(ResizeType.Content()) } as ObjectProperty<ResizeType>

var TornadoFXTable<*, *>.isSmartResizing: Boolean
    get() = properties[IS_SMART_RESIZING] == true
    set(value) {
        properties[IS_SMART_RESIZING] = value
    }

fun <S> TornadoFXColumn<S>.fixedWidth(width: Number) = apply {
    minWidth = width.toDouble()
    maxWidth = width.toDouble()
    resizeType = ResizeType.Fixed(width.toDouble())
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
    resizeType = ResizeType.Remaining()
}

fun <S> TornadoFXColumn<S>.weightedWidth(weight: Number, padding: Double = 0.0, minContentWidth: Boolean = false) = apply {
    resizeType = ResizeType.Weight(weight.toDouble(), padding, minContentWidth)
}

fun <S> TornadoFXColumn<S>.pctWidth(pct: Number) = apply {
    resizeType = ResizeType.Pct(pct.toDouble())
}

/**
 * Make the column fit the content plus an optional padding width. Optionally constrain the min or max width to be this width.
 */
fun <S> TornadoFXColumn<S>.contentWidth(padding: Double = 0.0, useAsMin: Boolean = false, useAsMax: Boolean = false) = apply {
    resizeType = ResizeType.Content(padding, useAsMin, useAsMax)
}

fun <S, T : Any> TornadoFXTable<S, T>.resizeColumnsToFitContent(resizeColumns: List<TornadoFXColumn<*>> = contentColumns, maxRows: Int = 50, afterResize: () -> Unit = {}) {
    val doResize = {
        val columnType = if (skin is TreeTableViewSkin<*>) TreeTableColumn::class.java else TableColumn::class.java
        val resizer = skin!!.javaClass.getDeclaredMethod("resizeColumnToFitContent", columnType, Int::class.java)
        resizer.isAccessible = true
        resizeColumns.forEach { resizer.invoke(skin, it.column, maxRows) }
        afterResize()
    }
    if (skin == null) {
        skinProperty.onChangeOnce {
            Platform.runLater { doResize() }
        }
    } else {
        doResize()
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
            // Resize all columns

            val contentWidth = param.table.contentWidth
            if (contentWidth == 0.0) return false

            installIfNeeded(param.table.table)

            var remainingWidth = contentWidth

            val groupedColumns = param.table.contentColumns.groupBy { it.resizeType::class }
            // Fixed columns always keep their size
            groupedColumns[ResizeType.Fixed::class]?.forEach {
                val rt = it.resizeType as ResizeType.Fixed
                it.prefWidth = rt.width.toDouble()
                remainingWidth -= it.width
            }

            // Preferred sized columns get their size and are adjusted for resize-delta that affected them
            groupedColumns[ResizeType.Pref::class]?.forEach {
                val rt = it.resizeType as ResizeType.Pref
                it.prefWidth = rt.width.toDouble() + rt.delta.toDouble()
                remainingWidth -= it.width
            }

            // Content columns are resized to their content and adjusted for resize-delta that affected them
            groupedColumns[ResizeType.Content::class]?.also { contentColumns ->
                param.table.resizeColumnsToFitContent(contentColumns)
                contentColumns.forEach {
                    val rt = it.resizeType as ResizeType.Content

                    it.prefWidth = it.width + rt.delta + rt.padding.toDouble()

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
            }

            // Pct columns share what's left of space from here
            groupedColumns[ResizeType.Pct::class]?.also { pctColumn ->
                val widthPerPct = contentWidth / 100.0
                pctColumn.forEach {
                    val rt = it.resizeType as ResizeType.Pct
                    it.prefWidth = (widthPerPct * rt.value.toDouble()) + rt.delta.toDouble()
                    remainingWidth -= it.width
                }
            }

            // Weighted columns shouldn't be combined with Pct. Weight is converted to pct of remaining width
            // and distributed to weigthed columns + remaining type columns
            groupedColumns[ResizeType.Weight::class]?.also { weightColumns ->
                val consideredColumns = weightColumns + param.table.contentColumns.filter { it.resizeType is ResizeType.Remaining }
                // Combining with "Remaining" typed columns. Remaining columns will get a default weight of 1
                fun TornadoFXColumn<*>.weight() = (resizeType as? ResizeType.Weight)?.weight?.toDouble() ?: 1.0

                val totalWeight = consideredColumns.sumByDouble { it.weight() }
                val perWeight = remainingWidth / totalWeight

                consideredColumns.forEach {
                    val rt = it.resizeType
                    if (rt is ResizeType.Weight) {
                        if (rt.minContentWidth && !rt.minRecorded) {
                            rt.minRecorded = true
                            it.minWidth = it.width + rt.padding.toDouble()
                        }
                        it.prefWidth = Math.max(it.minWidth, (perWeight * rt.weight.toDouble()) + rt.delta + rt.padding.toDouble())
                    } else {
                        it.prefWidth = Math.max(it.minWidth, perWeight + rt.delta)
                    }
                    remainingWidth -= it.width
                }

            } ?: run {
                // If no weighted columns, give the rest of the width to the "Remaining" columns
                groupedColumns[ResizeType.Remaining::class]?.also { remainingColumns ->
                    if (remainingWidth > 0) {
                        val perColumn = remainingWidth / remainingColumns.size.toDouble()
                        remainingColumns.forEach {
                            it.prefWidth = perColumn + it.resizeType.delta
                            remainingWidth -= it.width
                        }
                    }
                }
            }

            // Adjustment if we didn't assign all width
            if (remainingWidth > 0.0) {
                // Give remaining width to the right-most resizable column
                val rightMostResizable = param.table.contentColumns.lastOrNull { it.resizeType.isResizable }
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
                    val reduceableCandidate = param.table.contentColumns
                            .filter { it.resizeType.isResizable && it.minWidth < it.width }
                            .sortedBy { (it.width - it.minWidth) * -1 }
                            .firstOrNull()

                    canReduceMore = reduceableCandidate != null
                    if (reduceableCandidate != null && remainingWidth < 0.0) {
                        val reduceBy = Math.min(1.0, Math.abs(remainingWidth))
                        val toWidth = reduceableCandidate.width - reduceBy
                        reduceableCandidate.prefWidth = toWidth
                        remainingWidth += reduceBy
                    }
                }
            }
        } else {
            // Handle specific column size operation
            val rt = paramColumn.resizeType

            if (!rt.isResizable) return false

            val targetWidth = paramColumn.width + param.delta

            // Would resize result in illegal width?
            if (targetWidth !in paramColumn.minWidthProperty.value..paramColumn.maxWidthProperty.value) return false

            // Prepare to adjust the right column by the same amount we subtract or add to this column
            val rightColDelta = param.delta * -1.0
            val colIndex = param.table.contentColumns.indexOf(paramColumn)

            val rightCol = param.table.contentColumns
                    .filterIndexed { i, c -> i > colIndex && c.resizeType.isResizable }.firstOrNull {
                val newWidth = it.width + rightColDelta
                newWidth in it.minWidthProperty.value..it.maxWidthProperty.value
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