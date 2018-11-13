@file:Suppress("UNCHECKED_CAST")

package tornadofx

import javafx.beans.InvalidationListener
import javafx.beans.property.*
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.WeakListChangeListener
import javafx.css.*
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.SelectionMode.MULTIPLE
import javafx.scene.control.SelectionMode.SINGLE
import javafx.scene.control.skin.CellSkinBase
import javafx.scene.control.skin.VirtualContainerBase
import javafx.scene.input.InputEvent
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import java.util.*
import kotlin.reflect.KClass

fun <T> EventTarget.datagrid(
        items: List<T>? = null,
        scope: Scope = FX.defaultScope,
        op: DataGrid<T>.() -> Unit = {}
) = DataGrid<T>().attachTo(this, op){
    it.scope = scope
    if (items is ObservableList<T>) it.items = items
    else if (items is List<T>) it.items.setAll(items)
}

class DataGridPaginator<T>(private val sourceItems: ObservableList<T>, itemsPerPage: Int = 20): HBox() {
    val itemsPerPageProperty = SimpleIntegerProperty(itemsPerPage)
    var itemsPerPage by  itemsPerPageProperty

    val items = FXCollections.observableArrayList<T>()

    private val listChangeTrigger = SimpleObjectProperty(UUID.randomUUID())

    val pageCountProperty = integerBinding(itemsPerPageProperty, sourceItems) {
        Math.max(1, Math.ceil(sourceItems.size.toDouble() / itemsPerPageProperty.value.toDouble()).toInt())
    }
    val pageCount by pageCountProperty

    val currentPageProperty = SimpleIntegerProperty(1)
    var currentPage by currentPageProperty

    private val listChangeListener = ListChangeListener<T> {
        listChangeTrigger.value = UUID.randomUUID()
        setItemsForPage()

        // Check that the current page is still valid, or regenerate buttons
        while (currentPage > pageCount)
            currentPage -= 1
    }

    private val currentFromIndex: Int get() = itemsPerPage * (currentPage - 1)
    private val currentToIndex: Int get() = Math.min(currentFromIndex + itemsPerPage, sourceItems.size)

    init {
        spacing = 5.0
        alignment = Pos.CENTER
        currentPageProperty.onChange { setItemsForPage() }
        pageCountProperty.onChange { generatePageButtons() }
        sourceItems.addListener(listChangeListener)
        generatePageButtons()
        setItemsForPage()
    }

    private fun setItemsForPage() {
        items.setAll(sourceItems.subList(currentFromIndex, currentToIndex))
    }

    private fun generatePageButtons() {
        children.clear()
        togglegroup {
            // TODO: Support pagination for pages
            IntRange(1, pageCount).forEach { pageNo ->
                // TODO: Allow customization of togglebutton graphic/text
                togglebutton(pageNo.toString()) {
                    whenSelected { currentPage = pageNo }
                }
            }
        }
    }
}

@Suppress("unused")
class DataGrid<T>(items: ObservableList<T>) : Control() {
    constructor() : this(FXCollections.observableArrayList())
    constructor(items: List<T>) : this(FXCollections.observableArrayList(items))

    private val FACTORY = StyleablePropertyFactory<DataGrid<T>>(Control.getClassCssMetaData())

    internal var graphicCache = mutableMapOf<T, Node>()

    val itemsProperty = SimpleListProperty<T>(this, "items", items)
    var items: ObservableList<T> get() = itemsProperty.get(); set(value) = itemsProperty.set(value)

    val cellFactoryProperty = SimpleObjectProperty<(DataGrid<T>) -> DataGridCell<T>>(this, "cellFactory")
    var cellFactory: ((DataGrid<T>) -> DataGridCell<T>)? get() = cellFactoryProperty.get(); set(value) = cellFactoryProperty.set(value)

    val cellFormatProperty by lazy { SimpleObjectProperty<(DataGridCell<T>.(T) -> Unit)>() }
    var cellFormat: ((DataGridCell<T>).(T) -> Unit)? get() = cellFormatProperty.get(); set(value) = cellFormatProperty.set(value)
    fun cellFormat(cellFormat: (DataGridCell<T>).(T) -> Unit) {
        this.cellFormat = cellFormat
    }

    val scopeProperty = SimpleObjectProperty<Scope>()
    var scope: Scope? by scopeProperty

    val cellCacheProperty by lazy { SimpleObjectProperty<((T) -> Node)>() }
    var cellCache: ((T) -> Node)? get() = cellCacheProperty.get(); set(value) = cellCacheProperty.set(value)

    /**
     * Assign a Node to the graphic property of this cell. The graphic is cached and will be reused
     * for whatever cell is currently displaying the current item. Cells will in their life cycle be
     * used to display serveral different items, but using this function will make sure that there is
     * never a mismatch between the cached graphic node and the item it was created for.
     */
    fun cellCache(cachedGraphic: (T) -> Node) {
        this.cellCache = cachedGraphic
    }

    val cellFragmentProperty by lazy { SimpleObjectProperty<KClass<DataGridCellFragment<T>>>() }
    var cellFragment by cellFragmentProperty
    fun cellFragment(fragment: KClass<DataGridCellFragment<T>>) {
        properties["tornadofx.cellFragment"] = fragment
    }
    inline fun <reified C : DataGridCellFragment<T>> cellFragment() {
        properties["tornadofx.cellFragment"] = C::class
    }

    val cellWidthProperty: StyleableObjectProperty<Number> = FACTORY.createStyleableNumberProperty(this, "cellWidth", "-fx-cell-width", { it.cellWidthProperty }, 150.0) as StyleableObjectProperty<Number>
    var cellWidth: Double get() = cellWidthProperty.value as Double; set(value) {
        cellWidthProperty.value = value
    }

    val maxCellsInRowProperty: StyleableObjectProperty<Number> = FACTORY.createStyleableNumberProperty(this, "maxCellsInRow", "-fx-max-cells-in-row", { it.maxCellsInRowProperty }, Int.MAX_VALUE) as StyleableObjectProperty<Number>
    var maxCellsInRow: Int get() = maxCellsInRowProperty.value.toInt(); set(value) {
        maxCellsInRowProperty.value = value
    }

    val maxRowsProperty: StyleableObjectProperty<Number> = FACTORY.createStyleableNumberProperty(this, "maxRows", "-fx-max-rows", { it.maxRowsProperty }, Int.MAX_VALUE) as StyleableObjectProperty<Number>
    var maxRows: Int get() = maxRowsProperty.value.toInt(); set(value) {
        maxRowsProperty.value = value
    }

    val cellHeightProperty: StyleableObjectProperty<Number> = FACTORY.createStyleableNumberProperty(this, "cellHeight", "-fx-cell-height", { it.cellHeightProperty }, 150.0) as StyleableObjectProperty<Number>
    var cellHeight: Double get() = cellHeightProperty.value as Double; set(value) {
        cellHeightProperty.value = value
    }

    val horizontalCellSpacingProperty: StyleableProperty<Number> = FACTORY.createStyleableNumberProperty(this, "horizontalCellSpacing", "-fx-horizontal-cell-spacing", { it.horizontalCellSpacingProperty }, 8.0)
    var horizontalCellSpacing: Double get() = horizontalCellSpacingProperty.value as Double; set(value) {
        horizontalCellSpacingProperty.value = value
    }

    val verticalCellSpacingProperty: StyleableProperty<Number> = FACTORY.createStyleableNumberProperty(this, "verticalCellSpacing", "-fx-vertical-cell-spacing", { it.verticalCellSpacingProperty }, 8.0)
    var verticalCellSpacing: Double get() = verticalCellSpacingProperty.value as Double; set(value) {
        verticalCellSpacingProperty.value = value
    }

    val selectionModel = DataGridSelectionModel(this)
    val focusModel = DataGridFocusModel(this)

    var singleSelect: Boolean get() = selectionModel.selectionMode == SINGLE; set(value) {
        selectionModel.selectionMode = if (value) SINGLE else MULTIPLE
    }
    var multiSelect: Boolean get() = selectionModel.selectionMode == MULTIPLE; set(value) {
        selectionModel.selectionMode = if (value) MULTIPLE else SINGLE
    }

    override fun createDefaultSkin() = DataGridSkin(this)

    override fun getUserAgentStylesheet(): String = DataGrid::class.java.getResource("datagrid.css").toExternalForm()

    override fun getControlCssMetaData(): MutableList<CssMetaData<out Styleable, *>>? = FACTORY.cssMetaData

    // Called when the items list changes structurally
    private val itemsChangeListener = InvalidationListener {
        selectionModel.clearSelectionAndReapply()
        (skin as? DataGridSkin<T>)?.updateItems()
    }

    // Called when the items list is swapped for a new
    private val itemPropertyChangeListener = ChangeListener<ObservableList<T>> { _, oldList, newList ->
        selectionModel.clearSelectionAndReapply()
        if (oldList != null) {
            oldList.removeListener(itemsChangeListener)
            // Keep cache for elements in present in the new list
            oldList.filterNot { it in newList }.forEach { graphicCache.remove(it) }
        } else {
            graphicCache.clear()
        }
        newList.addListener(itemsChangeListener)
        (skin as? DataGridSkin<T>)?.updateItems()
    }

    val selectedItem: T? get() = this.selectionModel.selectedItem

    fun onUserSelect(clickCount: Int = 2, action: (T) -> Unit) {
        val isSelected = { event: InputEvent ->
            !selectionModel.isEmpty
        }

        addEventFilter(MouseEvent.MOUSE_CLICKED) { event ->
            if (event.clickCount == clickCount && isSelected(event))
                action(selectedItem!!)
        }

        addEventFilter(KeyEvent.KEY_PRESSED) { event ->
            if (event.code == KeyCode.ENTER && !event.isMetaDown && isSelected(event))
                action(selectedItem!!)
        }
    }

    init {
        addClass(Stylesheet.datagrid)
        itemsProperty.addListener(itemPropertyChangeListener)
        items.addListener(itemsChangeListener)
    }
}

open class DataGridCell<T>(val dataGrid: DataGrid<T>) : IndexedCell<T>() {
    var cache: Node? = null
    var updating = false
    private var fresh = true
    private var cellFragment: DataGridCellFragment<T>? = null

    init {
        addClass(Stylesheet.datagridCell)

        // Update cell content when index changes
        indexProperty().onChange {
            if (it == -1) clearCellFragment()
            if (!updating) doUpdateItem()
        }
    }

    internal fun doUpdateItem() {
        val totalCount = dataGrid.items.size
        val item = if (index !in 0 until totalCount) null else dataGrid.items[index]
        val cacheProvider = dataGrid.cellCache
        if (item != null) {
            if (cacheProvider != null)
                cache = dataGrid.graphicCache.getOrPut(item, { cacheProvider(item) })

            updateItem(item, false)
        } else {
            cache = null
            updateItem(null, true)
        }

        // Preemptive update of selected state
        val isActuallySelected = index in dataGrid.selectionModel.selectedIndices
        if (!isSelected && isActuallySelected) updateSelected(true)
        else if (isSelected && !isActuallySelected) updateSelected(false)
    }

    override fun updateItem(item: T?, empty: Boolean) {
        super.updateItem(item, empty)
        if (item == null || empty) {
            graphic = null
            text = null
            clearCellFragment()
        } else {
            val formatter = dataGrid.cellFormat
            if (fresh) {
                val cellFragmentType = dataGrid.properties["tornadofx.cellFragment"] as KClass<DataGridCellFragment<T>>?
                cellFragment = if (cellFragmentType != null) find(cellFragmentType, dataGrid.scope ?: FX.defaultScope) else null
                fresh = false
            }
            cellFragment?.apply {
                editingProperty.cleanBind(editingProperty())
                itemProperty.value = item
                cellProperty.value = this@DataGridCell
                graphic = root
            }
            if (cache != null) {
                graphic = StackPane(cache)
                formatter?.invoke(this, item)
            } else {
                if (formatter != null) formatter.invoke(this, item)
                else if (graphic == null) graphic = StackPane(Label(item.toString()))
            }
        }
    }

    private fun clearCellFragment() {
        cellFragment?.apply {
            cellProperty.value = null
            itemProperty.value = null
            editingProperty.unbind()
            editingProperty.value = false
        }
    }

    override fun createDefaultSkin() = DataGridCellSkin(this)
}

abstract class DataGridCellFragment<T> : ItemFragment<T>() {
    val cellProperty: ObjectProperty<DataGridCell<T>?> = SimpleObjectProperty()
    var cell by cellProperty
    val editingProperty = SimpleBooleanProperty(false)
    val editing by editingProperty

    open fun startEdit() {
        cell?.startEdit()
    }

    open fun commitEdit(newValue: T) {
        cell?.commitEdit(newValue)
    }

    open fun cancelEdit() {
        cell?.cancelEdit()
    }

    open fun onEdit(op: () -> Unit) {
        editingProperty.onChange { if (it) op() }
    }
}

// TODO: JDK10: Do we need this?
/*class DataGridCellBehavior<T>(control: DataGridCell<T>) : CellBehaviorBase<DataGridCell<T>>(control, emptyList()) {
    override fun getFocusModel() = control.dataGrid.focusModel

    override fun getCellContainer() = control

    override fun edit(cell: DataGridCell<T>?) {
        // No editing support for now
    }

    override fun getSelectionModel() = control.dataGrid.selectionModel

    override fun doSelect(x: Double, y: Double, button: MouseButton?, clickCount: Int, shiftDown: Boolean, shortcutDown: Boolean) {
        // I don't understand what the anchor is for yet, so to keep `focusedIndex = getAnchor(cellContainer, fm.getFocusedIndex())`
        // from returning something else than the currently focused index, I make sure "isDefaultAnchor" is set to true.
        // Must understand anchor and revisit this
        control.properties["isDefaultAnchor"] = true
        super.doSelect(x, y, button, clickCount, shiftDown, shortcutDown)
    }
}*/

class DataGridCellSkin<T>(control: DataGridCell<T>) : CellSkinBase<DataGridCell<T>>(control)

class DataGridFocusModel<T>(val dataGrid: DataGrid<T>) : FocusModel<T>() {
    override fun getModelItem(index: Int) = if (index in 0 until itemCount) dataGrid.items[index] else null
    override fun getItemCount() = dataGrid.items.size
}


open class DataGridRow<T>(val dataGrid: DataGrid<T>, val dataGridSkin: DataGridSkin<T>) : IndexedCell<T>() {
    init {
        addClass(Stylesheet.datagridRow)

        // Report row as not empty when it's populated
        indexProperty().addListener(InvalidationListener { updateItem(null, index == -1) })
    }

    override fun createDefaultSkin() = DataGridRowSkin(this)
}

class DataGridRowSkin<T>(control: DataGridRow<T>) : CellSkinBase<DataGridRow<T>>(control) {
    private var lastUpdatedCells = -1..-1
    init {
        // Remove default label from CellSkinBase
        children.clear()

        updateCells()

        registerChangeListener(skinnable.indexProperty()) { updateCells() }
        registerChangeListener(skinnable.widthProperty()) { updateCells() }
        registerChangeListener(skinnable.heightProperty()) { updateCells() }

    }

    /**
     * This routine is copied from the GridView in ControlsFX.
     */
    private fun updateCells() {
        val rowIndex = skinnable.index

        if (rowIndex > -1) {
            val dataGrid = skinnable.dataGrid
            val maxCellsInRow = (dataGrid.skin as DataGridSkin<*>).computeMaxCellsInRow()
            val totalCellsInGrid = dataGrid.items.size
            val startCellIndex = rowIndex * maxCellsInRow
            val endCellIndex = startCellIndex + maxCellsInRow - 1
            var cacheIndex = 0

            lastUpdatedCells = (startCellIndex..endCellIndex).also { if (it == lastUpdatedCells) return }

            var cellIndex = startCellIndex
            while (cellIndex <= endCellIndex) {
                if (cellIndex < totalCellsInGrid) {
                    var cell = getCellAtIndex(cacheIndex)
                    if (cell == null) {
                        cell = createCell()
                        children.add(cell)
                    }
                    cell.updating = true
                    cell.updateIndex(-1)
                    cell.updating = false
                    cell.updateIndex(cellIndex)
                } else {
                    break
                }// we are going out of bounds -> exist the loop
                cellIndex++
                cacheIndex++
            }

            // In case we are re-using a row that previously had more cells than
            // this one, we need to remove the extra cells that remain
            children.remove(cacheIndex, children.size)
        }
    }

    private fun createCell() = skinnable.dataGrid.cellFactory?.invoke(skinnable.dataGrid) ?: DataGridCell<T>(skinnable.dataGrid)


    @Suppress("UNCHECKED_CAST")
    fun getCellAtIndex(index: Int): DataGridCell<T>? {
        if (index < children.size)
            return children[index] as DataGridCell<T>?
        return null
    }

    override fun computeMaxHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        val dataGrid = skinnable.dataGrid
        return dataGrid.cellHeight * (dataGrid.skin as DataGridSkin<*>)._getItemCount()
    }

    override fun computePrefHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        val dataGrid = skinnable.dataGrid
        return dataGrid.cellHeight + (dataGrid.verticalCellSpacing * 2)
    }

    override fun computePrefWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        val dataGrid = skinnable.dataGrid
        return (dataGrid.cellWidth + dataGrid.horizontalCellSpacing * 2) * skinnable.dataGridSkin.computeMaxCellsInRow()
    }

    override fun layoutChildren(x: Double, y: Double, w: Double, h: Double) {
        val dataGrid = skinnable.dataGrid

        val cellWidth = dataGrid.cellWidth
        val cellHeight = dataGrid.cellHeight
        val horizontalCellSpacing = dataGrid.horizontalCellSpacing
        val verticalCellSpacing = dataGrid.verticalCellSpacing

        var xPos = 0.0

        for (child in children) {
            child.resizeRelocate(xPos + horizontalCellSpacing, y + verticalCellSpacing, cellWidth, cellHeight)
            xPos += cellWidth + (horizontalCellSpacing * 2)
        }
    }
}

class DataGridSelectionModel<T>(val dataGrid: DataGrid<T>) : MultipleSelectionModel<T>() {
    private val selectedIndicies: ObservableList<Int> = FXCollections.observableArrayList()
    private val selectedItems: ObservableList<T> = FXCollections.observableArrayList()

    fun getCellAt(index: Int): DataGridCell<T>? {
        val skin = dataGrid.skin as DataGridSkin<T>
        val cellsPerRow = skin.computeMaxCellsInRow()
        val rowIndex = index / cellsPerRow
        val row = skin.getRow(rowIndex) ?: return null
        val indexInRow = index - (rowIndex * cellsPerRow)
        val children = row.childrenUnmodifiable
        return if (children.size > indexInRow) children[indexInRow] as DataGridCell<T>? else null
    }

    init {
        // Instead of attaching listeners to all cells, distribute selected status directly
        val selectedIndicesListener = ListChangeListener<Int> { c ->
            while (c.next()) {
                if (c.wasAdded()) c.addedSubList.forEach { index ->
                    val cell = getCellAt(index)
                    if (cell != null && !cell.isSelected) {
                        cell.updating = true
                        cell.updateSelected(true)
                        cell.doUpdateItem()
                        cell.updating = false
                    }
                }
                if (c.wasRemoved()) c.removed.forEach { index ->
                    val cell = getCellAt(index)
                    if (cell != null && cell.isSelected) {
                        cell.updating = true
                        cell.updateSelected(false)
                        cell.doUpdateItem()
                        cell.updating = false
                    }
                }
            }
        }

        selectedIndices.addListener(selectedIndicesListener)
    }

    override fun selectPrevious() {
        select(selectedIndex - 1)
    }

    override fun selectFirst() {
        select(0)
    }

    override fun selectLast() {
        select(dataGrid.items.lastIndex)
    }

    override fun getSelectedIndices() = selectedIndicies

    override fun clearAndSelect(index: Int) {
        selectedIndicies.clear()
        selectedItems.clear()
        select(index)
    }

    override fun getSelectedItems() = selectedItems

    override fun selectNext() {
        select(selectedIndex + 1)
    }

    override fun selectAll() {
        selectedIndicies.clear()
        selectedItems.clear()
        dataGrid.items.forEachIndexed { index, item ->
            selectedIndicies.add(index)
            selectedItems.add(item)
        }
        select(dataGrid.items.lastIndex)
    }

    override fun clearSelection(index: Int) {
        if (index in selectedIndicies) {
            selectedIndicies.remove(index)
            selectedItems.remove(dataGrid.items[index])
        }
        if (selectedIndex == index) {
            selectedIndex = -1
            selectedItem = null
        }
    }

    override fun clearSelection() {
        selectedIndicies.clear()
        selectedItems.clear()
        selectedItem = null
        selectedIndex = -1
    }

    override fun isEmpty() = selectedIndicies.isEmpty()

    override fun selectIndices(index: Int, vararg indices: Int) {
        select(index)
        indices.forEach { select(it) }
    }

    override fun isSelected(index: Int) = index in selectedIndicies

    override fun select(obj: T) {
        val index = dataGrid.items.indexOf(obj)
        select(index)
    }

    override fun select(index: Int) {
        if (index !in dataGrid.items.indices) return
        selectedIndex = index
        selectedItem = dataGrid.items[index]

        if (selectionMode == SINGLE) {
            selectedIndicies.removeAll { it != index }
            selectedItems.removeAll { it != selectedItem }
        }

        if (index !in selectedIndicies) {
            selectedIndicies.add(index)
            selectedItems.add(selectedItem)
            dataGrid.focusModel.focus(index)
        }
    }

    /**
     * Clear selection and reapply for the items that are still in the list
     */
    fun clearSelectionAndReapply() {
        val currentItems = selectedItems.toList()
        val currentIndexes = selectedIndicies.toList()
        val selectedItemsToIndex = (currentItems zip currentIndexes).toMap()

        clearSelection()

        for (item in currentItems) {
            val index = dataGrid.items.indexOf(item)
            if (index > -1) {
                select(index)
            } else {
                // If item is gone, select the item at the same index position
                select(selectedItemsToIndex[item]!!)
            }
        }
    }

}

@Suppress("UNCHECKED_CAST")
class DataGridSkin<T>(control: DataGrid<T>) : VirtualContainerBase<DataGrid<T>, DataGridRow<T>>(control) {
    private val gridViewItemsListener = ListChangeListener<T> {
        updateItemCount()
        skinnable.requestLayout()
    }

    private val weakGridViewItemsListener = WeakListChangeListener(gridViewItemsListener)

    init {
        updateItems()

        virtualFlow.id = "virtual-flow"
        virtualFlow.isPannable = false
        virtualFlow.isFocusTraversable = false
        virtualFlow.setCellFactory { createCell() }
        children.add(virtualFlow)

        updateItemCount()

        registerChangeListener(control.itemsProperty) { updateItems() }
        registerChangeListener(control.cellFactoryProperty) { recreateCells() }
        registerChangeListener(control.parentProperty()) { _ ->
                run {
                        if (skinnable.parent != null && skinnable.isVisible)
                                skinnable.requestLayout()
                    }
            }
        registerChangeListener(control.cellHeightProperty as ObservableValue<Number>) { recreateCells() }
        registerChangeListener(control.cellWidthProperty as ObservableValue<Number>) { recreateCells() }
        registerChangeListener(control.horizontalCellSpacingProperty as ObservableValue<Number>) { _ ->
                updateItemCount()
                recreateCells()
            }
        registerChangeListener(control.verticalCellSpacingProperty as ObservableValue<Number>) { recreateCells() }
        registerChangeListener(control.widthProperty()) { updateItemCount() }
        registerChangeListener(control.heightProperty()) { updateItemCount() }


        focusOnClick()
    }

    private fun focusOnClick() {
        skinnable.addEventFilter(MouseEvent.MOUSE_PRESSED) {
            if (!skinnable.isFocused && skinnable.isFocusTraversable) skinnable.requestFocus()
        }
    }

    fun _getItemCount() = itemCount
    override fun getItemCount() = Math.ceil(skinnable.items.size.toDouble() / computeMaxCellsInRow()).toInt()

    /**
     * Compute the maximum number of cells per row. If the calculated number of cells would result in
     * more than the configured maxRow rows, the maxRow setting takes presedence and overrides the maxCellsInRow
     */
    fun computeMaxCellsInRow(): Int {
        val maxCellsInRow = Math.min(Math.max(Math.floor(computeRowWidth() / computeCellWidth()).toInt(), 1), skinnable.maxCellsInRow)
        val neededRows = Math.ceil(skinnable.items.size.toDouble() / maxCellsInRow)
        return if (neededRows > skinnable.maxRows) (skinnable.items.size.toDouble() / skinnable.maxRows).toInt() else maxCellsInRow
    }

    fun computeRowWidth() = skinnable.width - 14 // Account for scrollbar

    private fun computeCellWidth() = skinnable.cellWidth + skinnable.horizontalCellSpacing * 2

    override fun computePrefHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double) = 500.0

    override fun computePrefWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double) = 500.0

    override fun updateItemCount() {
        if (virtualFlow == null) return

        val oldCount = virtualFlow.cellCount
        val newCount = itemCount

        if (newCount != oldCount) {
            virtualFlow.cellCount = newCount
            rebuildCells()
        } else {
            reconfigureCells()
        }
        updateRows(newCount)
    }

    fun createCell() = DataGridRow(skinnable, this)

    fun updateItems() {
        skinnable.items.removeListener(weakGridViewItemsListener)
        skinnable.items.addListener(weakGridViewItemsListener)
        updateItemCount()
        recreateCells()
        skinnable.requestLayout()
    }

    private fun updateRows(rowCount: Int) {
        for (i in 0..rowCount - 1)
            getRow(i)?.updateIndex(i)
    }

    fun getRow(index: Int) = virtualFlow.getVisibleCell(index)

    override fun layoutChildren(x: Double, y: Double, w: Double, h: Double) {
        val x1 = skinnable.insets.left
        val y1 = skinnable.insets.top
        val w1 = skinnable.width - (skinnable.insets.left + skinnable.insets.right)
        val h1 = skinnable.height - (skinnable.insets.top + skinnable.insets.bottom)

        virtualFlow.resizeRelocate(x1, y1, w1, h1)
    }

    // TODO: Remove once https://github.com/javafxports/openjdk-jfx/pull/163 is merged
    private fun recreateCells() {
        ReflectionUtils.callMethod(virtualFlow, "recreateCells")
    }

    private fun rebuildCells() {
        ReflectionUtils.callMethod(virtualFlow, "rebuildCells")
    }

    private fun reconfigureCells() {
        ReflectionUtils.callMethod(virtualFlow, "reconfigureCells")
    }


}

fun <T> DataGrid<T>.bindSelected(property: Property<T>) {
    selectionModel.selectedItemProperty().onChange {
        property.value = it
    }
}

fun <T> DataGrid<T>.bindSelected(model: ItemViewModel<T>) = this.bindSelected(model.itemProperty)

fun <T> DataGrid<T>.asyncItems(func: () -> Collection<T>) =
        task { func() } success { items.setAll(it) }

