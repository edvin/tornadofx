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
import tornadofx.*
import tornadofx.ResizeType.*
import kotlin.collections.set

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
                val fixedColumns = param.table.contentColumns.filter { it.resizeType is Fixed }
                fixedColumns.forEach {
                    val rt = it.resizeType as Fixed
                    it.prefWidth = rt.width.toDouble()
                    remainingWidth -= it.width
                }

                // Preferred sized columns get their size and are adjusted for resize-delta that affected them
                val prefColumns = param.table.contentColumns.filter { it.resizeType is Pref }
                prefColumns.forEach {
                    val rt = it.resizeType as Pref
                    it.prefWidth = rt.width.toDouble() + rt.delta.toDouble()
                    remainingWidth -= it.width
                }

                // Content columns are resized to their content and adjusted for resize-delta that affected them
                val contentColumns = param.table.contentColumns.filter { it.resizeType is Content }
                param.table.resizeColumnsToFitContent(contentColumns)
                contentColumns.forEach {
                    val rt = it.resizeType as Content

                    it.prefWidth = it.width + rt.delta.toDouble() + rt.padding.toDouble()

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
                val pctColumns = param.table.contentColumns.filter { it.resizeType is Pct }
                if (pctColumns.isNotEmpty()) {
                    val widthPerPct = contentWidth / 100.0
                    pctColumns.forEach {
                        val rt = it.resizeType as Pct
                        it.prefWidth = (widthPerPct * rt.value.toDouble()) + rt.delta.toDouble()
                        remainingWidth -= it.width
                    }
                }

                // Weighted columns shouldn't be combined with Pct. Weight is converted to pct of remaining width
                // and distributed to weigthed columns + remaining type columns
                val weightColumns = param.table.contentColumns.filter { it.resizeType is Weight }
                if (weightColumns.isNotEmpty()) {
                    val consideredColumns = weightColumns + param.table.contentColumns.filter { it.resizeType is Remaining }
                    // Combining with "Remaining" typed columns. Remaining columns will get a default weight of 1
                    fun TableColumn<*, *>.weight() = (resizeType as? Weight)?.weight?.toDouble() ?: 1.0

                    val totalWeight = consideredColumns.map { it.weight() }.sum()
                    val perWeight = remainingWidth / totalWeight

                    consideredColumns.forEach {
                        val rt = it.resizeType
                        if (rt is Weight) {
                            if (rt.minContentWidth && !rt.minRecorded) {
                                rt.minRecorded = true
                                it.minWidth = it.width.toDouble() + rt.padding.toDouble()
                            }
                            it.prefWidth = Math.max(it.minWidth, (perWeight * rt.weight.toDouble()) + rt.delta + rt.padding.toDouble())
                        } else {
                            it.prefWidth = Math.max(it.minWidth, perWeight + rt.delta)
                        }
                        remainingWidth -= it.width
                    }

                } else {
                    // If no weighted columns, give the rest of the width to the "Remaining" columns
                    val remainingColumns = param.table.contentColumns.filter { it.resizeType is Remaining }
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
                    val rightMostResizable = param.table.contentColumns.reversed().filter { it.resizeType.isResizable }.firstOrNull()
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
                val colIndex = param.table.contentColumns.indexOf(param.column)

                val rightCol = param.table.contentColumns
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
        Platform.runLater {
            call(TableView.ResizeFeatures(table, null, 0.0))
        }
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

            if (table != null && !table.isSmartResizing) {
                val rt = column.resizeType
                val diff = oldValue.toDouble() - newValue.toDouble()
                rt.delta -= diff
                POLICY.call(TableView.ResizeFeatures<Any>(table as TableView<Any>?, null, 0.0))
            }

        }

        private fun isPolicyInstalled(table: TableView<*>): Boolean {
            return table.properties["tornadofx.smartResize"] is SmartResize
        }

        private fun install(table: TableView<*>) {
            table.columnResizePolicyProperty().addListener(policyChangeListener)
            table.columns.addListener(columnsChangeListener)
            table.itemsProperty().addListener(itemsChangeListener)
            table.columns.forEach { it.widthProperty().addListener(columnWidthChangeListener) }
            table.properties["tornadofx.smartResizeInstalled"] = true
        }

        private fun uninstall(table: TableView<*>) {
            table.columnResizePolicyProperty().removeListener(policyChangeListener)
            table.columns.removeListener(columnsChangeListener)
            table.itemsProperty().removeListener(itemsChangeListener)
            table.columns.forEach { it.widthProperty().removeListener(columnWidthChangeListener) }
            table.properties.remove("tornadofx.smartResizeInstalled")
        }
    }
}

class TreeTableSmartResize private constructor() : TreeTableViewResizeCallback {

    override fun call(param: TreeTableView.ResizeFeatures<out Any>): Boolean {
        param.table.isSmartResizing = true

        try {
            if (param.column == null) {
                // Resize all columns
                val contentWidth = param.table.getContentWidth()
                if (contentWidth == 0.0) return false

                if (!isPolicyInstalled(param.table)) install(param.table)

                var remainingWidth = contentWidth

                // Fixed columns always keep their size
                val fixedColumns = param.table.contentColumns.filter { it.resizeType is Fixed }
                fixedColumns.forEach {
                    val rt = it.resizeType as Fixed
                    it.prefWidth = rt.width.toDouble()
                    remainingWidth -= it.width
                }

                // Preferred sized columns get their size and are adjusted for resize-delta that affected them
                val prefColumns = param.table.contentColumns.filter { it.resizeType is Pref }
                prefColumns.forEach {
                    val rt = it.resizeType as Pref
                    it.prefWidth = rt.width.toDouble() + rt.delta
                    remainingWidth -= it.width
                }

                // Content columns are resized to their content and adjusted for resize-delta that affected them
                val contentColumns = param.table.contentColumns.filter { it.resizeType is Content }
                param.table.resizeColumnsToFitContent(contentColumns)
                contentColumns.forEach {
                    val rt = it.resizeType as Content

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

                // Pct columns share what's left of space from here
                val pctColumns = param.table.contentColumns.filter { it.resizeType is Pct }
                if (pctColumns.isNotEmpty()) {
                    val widthPerPct = contentWidth / 100.0
                    pctColumns.forEach {
                        val rt = it.resizeType as Pct
                        it.prefWidth = (widthPerPct * rt.value.toDouble()) + rt.delta
                        remainingWidth -= it.width
                    }
                }

                // Weighted columns shouldn't be combined with Pct. Weight is converted to pct of remaining width
                // and distributed to weigthed columns + remaining type columns
                val weightColumns = param.table.contentColumns.filter { it.resizeType is Weight }
                if (weightColumns.isNotEmpty()) {
                    val consideredColumns = weightColumns + param.table.contentColumns.filter { it.resizeType is Remaining }
                    // Combining with "Remaining" typed columns. Remaining columns will get a default weight of 1
                    fun TreeTableColumn<*, *>.weight() = (resizeType as? Weight)?.weight?.toDouble() ?: 1.0

                    val totalWeight = consideredColumns.map { it.weight() }.sum()
                    val perWeight = remainingWidth / totalWeight

                    consideredColumns.forEach {
                        val rt = it.resizeType
                        if (rt is Weight) {
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

                } else {
                    // If no weighted columns, give the rest of the width to the "Remaining" columns
                    val remainingColumns = param.table.contentColumns.filter { it.resizeType is Remaining }
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
                    val rightMostResizable = param.table.contentColumns.reversed().filter { it.resizeType.isResizable }.firstOrNull()
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
                val colIndex = param.table.contentColumns.indexOf(param.column)

                val rightCol = param.table.contentColumns
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

    var TreeTableView<*>.isSmartResizing: Boolean
        get() = properties["tornadofx.isSmartResizing"] == true
        set(value) {
            properties["tornadofx.isSmartResizing"] = value
        }

    fun requestResize(table: TreeTableView<*>) {
        Platform.runLater {
            call(TreeTableView.ResizeFeatures(table, null, 0.0))
        }
    }

    companion object {
        val POLICY = TreeTableSmartResize()
        val ResizeTypeKey = "tornadofx.smartColumnResizeType"

        internal var TreeTableView<*>.isSmartResizing: Boolean
            get() = properties["tornadofx.isSmartResizing"] == true
            set(value) {
                properties["tornadofx.isSmartResizing"] = value
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

        private fun isPolicyInstalled(table: TreeTableView<*>): Boolean {
            return table.properties["tornadofx.smartResize"] is SmartResize
        }

        private fun install(table: TreeTableView<*>) {
            table.columnResizePolicyProperty().addListener(policyChangeListener)
            table.columns.addListener(columnsChangeListener)
            table.columns.forEach { it.widthProperty().addListener(columnWidthChangeListener) }
            table.properties["tornadofx.smartResizeInstalled"] = true
        }

        private fun uninstall(table: TreeTableView<*>) {
            table.columnResizePolicyProperty().removeListener(policyChangeListener)
            table.columns.removeListener(columnsChangeListener)
            table.columns.forEach { it.widthProperty().removeListener(columnWidthChangeListener) }
            table.properties.remove("tornadofx.smartResizeInstalled")
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

val TableView<*>.contentColumns: List<TableColumn<*, *>> get() = columns.flatMap {
    if (it.columns.isEmpty()) listOf(it) else it.columns
}

val TreeTableView<*>.contentColumns: List<TreeTableColumn<*, *>> get() = columns.flatMap {
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

fun <S, T> TableColumn<S, T>.fixedWidth(width: Number): TableColumn<S, T> {
    minWidth = width.toDouble()
    maxWidth = width.toDouble()
    resizeType = ResizeType.Fixed(width.toDouble())
    return this
}

fun <S, T> TreeTableColumn<S, T>.fixedWidth(width: Number): TreeTableColumn<S, T> {
    minWidth = width.toDouble()
    maxWidth = width.toDouble()
    resizeType = ResizeType.Fixed(width.toDouble())
    return this
}

fun <S, T> TableColumn<S, T>.minWidth(width: Number): TableColumn<S, T> {
    minWidth = width.toDouble()
    return this
}

fun <S, T> TreeTableColumn<S, T>.minWidth(width: Number): TreeTableColumn<S, T> {
    minWidth = width.toDouble()
    return this
}

fun <S, T> TableColumn<S, T>.maxWidth(width: Number): TableColumn<S, T> {
    maxWidth = width.toDouble()
    return this
}

fun <S, T> TreeTableColumn<S, T>.maxWidth(width: Number): TreeTableColumn<S, T> {
    maxWidth = width.toDouble()
    return this
}

fun <S, T> TableColumn<S, T>.prefWidth(width: Number): TableColumn<S, T> {
    prefWidth = width.toDouble()
    return this
}

fun <S, T> TreeTableColumn<S, T>.prefWidth(width: Number): TreeTableColumn<S, T> {
    prefWidth = width.toDouble()
    return this
}

fun <S, T> TableColumn<S, T>.remainingWidth(): TableColumn<S, T> {
    resizeType = ResizeType.Remaining()
    return this
}

fun <S, T> TreeTableColumn<S, T>.remainingWidth(): TreeTableColumn<S, T> {
    resizeType = ResizeType.Remaining()
    return this
}

fun <S, T> TableColumn<S, T>.weigthedWidth(weight: Number, padding: Double = 0.0, minContentWidth: Boolean = false): TableColumn<S, T> {
    resizeType = ResizeType.Weight(weight.toDouble(), padding, minContentWidth)
    return this
}

fun <S, T> TreeTableColumn<S, T>.weigthedWidth(weight: Number, padding: Double = 0.0, minContentWidth: Boolean = false): TreeTableColumn<S, T> {
    resizeType = ResizeType.Weight(weight.toDouble(), padding, minContentWidth)
    return this
}

fun <S, T> TableColumn<S, T>.pctWidth(pct: Number): TableColumn<S, T> {
    resizeType = ResizeType.Pct(pct.toDouble())
    return this
}

fun <S, T> TreeTableColumn<S, T>.pctWidth(pct: Number): TreeTableColumn<S, T> {
    resizeType = ResizeType.Pct(pct.toDouble())
    return this
}

/**
 * Make the column fit the content plus an optional padding width. Optionally constrain the min or max width to be this width.
 */
fun <S, T> TableColumn<S, T>.contentWidth(padding: Double = 0.0, useAsMin: Boolean = false, useAsMax: Boolean = false): TableColumn<S, T> {
    resizeType = ResizeType.Content(padding, useAsMin, useAsMax)
    return this
}

/**
 * Make the column fit the content plus an optional padding width. Optionally constrain the min or max width to be this width.
 */
fun <S, T> TreeTableColumn<S, T>.contentWidth(padding: Number = 0.0, useAsMin: Boolean = false, useAsMax: Boolean = false): TreeTableColumn<S, T> {
    resizeType = ResizeType.Content(padding, useAsMin, useAsMax)
    return this
}
