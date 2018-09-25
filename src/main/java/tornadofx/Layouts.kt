package tornadofx

import javafx.beans.property.ObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.canvas.Canvas
import javafx.scene.control.*
import javafx.scene.layout.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction1
import kotlin.reflect.full.createInstance


private const val BuilderTargetKey = "tornadofx.builderTarget"

private const val GridPaneRowIdKey = "TornadoFX.GridPaneRowId" // FIXME Inconsistent, should be in camelCase
private const val GridPaneParentObjectKey = "TornadoFX.GridPaneParentObject" // FIXME Inconsistent, should be in camelCase


@Suppress("UNCHECKED_CAST")
var Node.builderTarget: KFunction1<*, ObjectProperty<Node>>?
    get() = properties[BuilderTargetKey] as KFunction1<Any, ObjectProperty<Node>>?
    set(value) {
        properties[BuilderTargetKey] = value
    }


// ================================================================
// ToolBar

fun EventTarget.toolbar(vararg nodes: Node, op: ToolBar.() -> Unit = {}): ToolBar {
    val toolbar = ToolBar()
    if (nodes.isNotEmpty()) toolbar.items.addAll(nodes)
    opcr(this, toolbar, op)
    return toolbar
}

@Deprecated(
    "No need to wrap ToolBar children in children {} anymore. Remove the wrapper and all builder items will still be added as before.",
    ReplaceWith("apply op"),
    DeprecationLevel.WARNING
)
fun ToolBar.children(op: ToolBar.() -> Unit): ToolBar = apply(op)


// ================================================================
// Spacer

fun HBox.spacer(prio: Priority = Priority.ALWAYS, op: Pane.() -> Unit = {}): Pane = opcr(this, Pane().apply { HBox.setHgrow(this, prio) }, op)
fun VBox.spacer(prio: Priority = Priority.ALWAYS, op: Pane.() -> Unit = {}): Pane = opcr(this, Pane().apply { VBox.setVgrow(this, prio) }, op)
fun ToolBar.spacer(prio: Priority = Priority.ALWAYS, op: Pane.() -> Unit = {}): Pane {
    val pane = Pane().apply {
        addClass("spacer")
        hgrow = prio
    }
    op(pane)
    add(pane)
    return pane
}


// ================================================================
// Separator

fun EventTarget.separator(orientation: Orientation = Orientation.HORIZONTAL, op: Separator.() -> Unit = {}): Separator = opcr(this, Separator(orientation), op)
fun ToolBar.separator(orientation: Orientation = Orientation.HORIZONTAL, op: Separator.() -> Unit = {}): Separator {
    val separator = Separator(orientation).also(op)
    add(separator)
    return separator
}


// ================================================================
// Panes

fun EventTarget.pane(op: Pane.() -> Unit = {}): Pane = opcr(this, Pane(), op)


fun EventTarget.hbox(spacing: Number? = null, alignment: Pos? = null, op: HBox.() -> Unit = {}): HBox {
    val hbox = HBox()
    if (alignment != null) hbox.alignment = alignment
    if (spacing != null) hbox.spacing = spacing.toDouble()
    return opcr(this, hbox, op)
}

fun EventTarget.vbox(spacing: Number? = null, alignment: Pos? = null, op: VBox.() -> Unit = {}): VBox {
    val vbox = VBox()
    if (alignment != null) vbox.alignment = alignment
    if (spacing != null) vbox.spacing = spacing.toDouble()
    return opcr(this, vbox, op)
}


fun EventTarget.flowpane(op: FlowPane.() -> Unit = {}): FlowPane = opcr(this, FlowPane(), op)
fun EventTarget.tilepane(op: TilePane.() -> Unit = {}): TilePane = opcr(this, TilePane(), op)
fun EventTarget.stackpane(initialChildren: Iterable<Node>? = null, op: StackPane.() -> Unit = {}): StackPane =
    opcr(this, StackPane().apply { if (initialChildren != null) children.addAll(initialChildren) }, op)

fun EventTarget.anchorpane(vararg nodes: Node, op: AnchorPane.() -> Unit = {}): AnchorPane {
    val anchorpane = AnchorPane()
    if (nodes.isNotEmpty()) anchorpane.children.addAll(nodes)
    opcr(this, anchorpane, op)
    return anchorpane
}


fun EventTarget.group(initialChildren: Iterable<Node>? = null, op: Group.() -> Unit = {}): Group =
    opcr(this, Group().apply { if (initialChildren != null) children.addAll(initialChildren) }, op)

// ================================================================
// GridPane

fun EventTarget.gridpane(op: GridPane.() -> Unit = {}): GridPane = opcr(this, GridPane(), op)

fun GridPane.row(title: String? = null, op: Pane.() -> Unit = {}) {
    properties[GridPaneRowIdKey] = if (properties.containsKey(GridPaneRowIdKey)) properties[GridPaneRowIdKey] as Int + 1 else 0

    // Allow the caller to add children to a fake pane
    val fake = Pane()
    fake.properties[GridPaneParentObjectKey] = this
    if (title != null) fake.children.add(Label(title))

    op(fake)

    // Create a new row in the GridPane and add the children added to the fake pane
    addRow(properties[GridPaneRowIdKey] as Int, *fake.children.toTypedArray())
}

/**
 * Removes the corresponding row to which this [node] belongs to.
 *
 * It does the opposite of the [GridPane.row] cleaning all internal state properly.
 *
 * @return the row index of the removed row.
 */
fun GridPane.removeRow(node: Node): Int {
    val rowId = properties[GridPaneRowIdKey] as Int?
    if (rowId != null) {
        when (rowId) {
            0 -> properties.remove(GridPaneRowIdKey)
            else -> properties[GridPaneRowIdKey] = rowId - 1
        }
    }
    val rowIndex = GridPane.getRowIndex(node) ?: 0
    val nodesToDelete = mutableListOf<Node>()
    children.forEach { child ->
        val childRowIndex = GridPane.getRowIndex(child) ?: 0
        if (childRowIndex == rowIndex) {
            nodesToDelete.add(child)
            // Remove row index property from the node
            GridPane.setRowIndex(child, null)
            GridPane.setColumnIndex(child, null)
        } else if (childRowIndex > rowIndex) {
            GridPane.setRowIndex(child, childRowIndex - 1)
        }
    }
    children.removeAll(nodesToDelete)
    return rowIndex
}

fun GridPane.removeAllRows() {
    children.forEach {
        GridPane.setRowIndex(it, null)
        GridPane.setColumnIndex(it, null)
    }
    children.clear()
    properties.remove(GridPaneRowIdKey)
}

fun GridPane.constraintsForColumn(columnIndex: Int): ColumnConstraints = constraintsFor(columnConstraints, columnIndex)
fun GridPane.constraintsForRow(rowIndex: Int): RowConstraints = constraintsFor(rowConstraints, rowIndex)

//constraints for row and column can be handled the same way
internal inline fun <reified T : ConstraintsBase> constraintsFor(constraints: ObservableList<T>, index: Int): T {
    while (constraints.size <= index) constraints.add(T::class.createInstance())
    return constraints[index]
}

val Parent.gridpaneColumnConstraints: ColumnConstraints?
    get() {
        var cursor = this
        var next = parent
        while (next != null) {
            val gridReference = when {
                next is GridPane -> next to GridPane.getColumnIndex(cursor)?.let { it }
                // perhaps we're still in the row builder
                next.parent == null -> (next.properties[GridPaneParentObjectKey] as? GridPane)?.let {
                    it to next.getChildList()?.indexOf(cursor)
                }
                else -> null
            }

            if (gridReference != null) {
                val (grid, columnIndex) = gridReference
                if (columnIndex != null && columnIndex >= 0) return grid.constraintsForColumn(columnIndex)
            }
            cursor = next
            next = next.parent
        }
        return null
    }

fun Parent.gridpaneColumnConstraints(op: ColumnConstraints.() -> Unit): ColumnConstraints? = gridpaneColumnConstraints?.apply(op)


// ================================================================
// BorderPane

fun EventTarget.borderpane(op: BorderPane.() -> Unit = {}): BorderPane = opcr(this, BorderPane(), op)

fun BorderPane.top(op: BorderPane.() -> Unit): Unit = region(BorderPane::topProperty, op)
fun BorderPane.bottom(op: BorderPane.() -> Unit): Unit = region(BorderPane::bottomProperty, op)
fun BorderPane.left(op: BorderPane.() -> Unit): Unit = region(BorderPane::leftProperty, op)
fun BorderPane.right(op: BorderPane.() -> Unit): Unit = region(BorderPane::rightProperty, op)
fun BorderPane.center(op: BorderPane.() -> Unit): Unit = region(BorderPane::centerProperty, op)

internal fun BorderPane.region(region: KFunction1<BorderPane, ObjectProperty<Node>>?, op: BorderPane.() -> Unit) {
    builderTarget = region
    op()
    builderTarget = null
}

inline fun <reified C : UIComponent> BorderPane.setRegion(scope: Scope, region: KFunction1<BorderPane, ObjectProperty<Node>>): BorderPane = apply {
    region.invoke(this).value = find<C>(scope).root
}

fun <C : UIComponent> BorderPane.setRegion(scope: Scope, region: KFunction1<BorderPane, ObjectProperty<Node>>, nodeType: KClass<C>): BorderPane = apply {
    region.invoke(this).value = find(nodeType, scope).root
}

@Deprecated("Use top = node {} instead")
fun <T : Node> BorderPane.top(topNode: T, op: T.() -> Unit = {}): T {
    top = topNode
    return opcr(this, topNode, op)
}

@Deprecated("Use bottom = node {} instead")
fun <T : Node> BorderPane.bottom(bottomNode: T, op: T.() -> Unit = {}): T {
    bottom = bottomNode
    return opcr(this, bottomNode, op)
}

@Deprecated("Use left = node {} instead")
fun <T : Node> BorderPane.left(leftNode: T, op: T.() -> Unit = {}): T {
    left = leftNode
    return opcr(this, leftNode, op)
}

@Deprecated("Use right = node {} instead")
fun <T : Node> BorderPane.right(rightNode: T, op: T.() -> Unit = {}): T {
    right = rightNode
    return opcr(this, rightNode, op)
}

@Deprecated("Use center = node {} instead")
fun <T : Node> BorderPane.center(centerNode: T, op: T.() -> Unit = {}): T {
    center = centerNode
    return opcr(this, centerNode, op)
}


// ================================================================
// TitledPane

fun EventTarget.titledpane(title: String? = null, node: Node? = null, collapsible: Boolean = true, op: TitledPane.() -> Unit = {}): TitledPane {
    val titledPane = TitledPane(title, node)
    titledPane.isCollapsible = collapsible
    opcr(this, titledPane, op)
    return titledPane
}

fun EventTarget.titledpane(title: ObservableValue<String>, node: Node? = null, collapsible: Boolean = true, op: TitledPane.() -> Unit = {}): TitledPane {
    val titledPane = TitledPane("", node)
    titledPane.textProperty().bind(title)
    titledPane.isCollapsible = collapsible
    opcr(this, titledPane, op)
    return titledPane
}


// ================================================================
// Accordion

fun EventTarget.accordion(vararg panes: TitledPane, op: Accordion.() -> Unit = {}): Accordion {
    val accordion = Accordion()
    if (panes.isNotEmpty()) accordion.panes.addAll(panes)
    opcr(this, accordion, op)
    return accordion
}

fun <T : Node> Accordion.fold(title: String? = null, node: T, expanded: Boolean = false, op: T.() -> Unit = {}): TitledPane {
    val fold = TitledPane(title, node)
    fold.isExpanded = expanded
    panes += fold
    op(node)
    return fold
}

@Deprecated(
    "Properties added to the container will be lost if you add only a single child Node",
    ReplaceWith("Accordion.fold(title, node, op)"),
    DeprecationLevel.WARNING
)
fun Accordion.fold(title: String? = null, op: Pane.() -> Unit = {}): TitledPane {
    val vbox = VBox().also(op)
    val fold = TitledPane(title, if (vbox.children.size == 1) vbox.children[0] else vbox)
    panes += fold
    return fold
}


// ================================================================
// Pagination

fun EventTarget.pagination(pageCount: Int? = null, pageIndex: Int? = null, op: Pagination.() -> Unit = {}): Pagination {
    val pagination = Pagination()
    if (pageCount != null) pagination.pageCount = pageCount
    if (pageIndex != null) pagination.currentPageIndex = pageIndex
    return opcr(this, pagination, op)
}


// ================================================================
// ScrollPane

fun EventTarget.scrollpane(fitToWidth: Boolean = false, fitToHeight: Boolean = false, op: ScrollPane.() -> Unit = {}): ScrollPane {
    val pane = ScrollPane()
    pane.isFitToWidth = fitToWidth
    pane.isFitToHeight = fitToHeight
    opcr(this, pane, op)
    return pane
}

var ScrollPane.edgeToEdge: Boolean
    get() = hasClass("edge-to-edge")
    set(value) {
        if (value) addClass("edge-to-edge") else removeClass("edge-to-edge")
    }


// ================================================================
// SplitPane

fun EventTarget.splitpane(orientation: Orientation = Orientation.HORIZONTAL, vararg nodes: Node, op: SplitPane.() -> Unit = {}): SplitPane {
    val splitpane = SplitPane()
    splitpane.orientation = orientation
    if (nodes.isNotEmpty())
        splitpane.items.addAll(nodes)
    opcr(this, splitpane, op)
    return splitpane
}

@Deprecated(
    "No need to wrap splitpane items in items{} anymore. Remove the wrapper and all builder items will still be added as before.",
    ReplaceWith("apply op"),
    DeprecationLevel.WARNING
)
fun SplitPane.items(op: (SplitPane.() -> Unit)): Unit = op(this)


// ================================================================
// Canvas

fun EventTarget.canvas(width: Double = 0.0, height: Double = 0.0, op: Canvas.() -> Unit = {}): Canvas = opcr(this, Canvas(width, height), op)


// ================================================================
// Region

fun EventTarget.region(op: Region.() -> Unit = {}): Region = opcr(this, Region(), op)

@Deprecated("Use the paddingTop property instead", ReplaceWith("paddingTop = p"))
fun Region.paddingTop(p: Double) {
    paddingTop = p
}

@Deprecated("Use the paddingBottom property instead", ReplaceWith("paddingBottom = p"))
fun Region.paddingBottom(p: Double) {
    paddingBottom = p
}

@Deprecated("Use the paddingLeft property instead", ReplaceWith("paddingLeft = p"))
fun Region.paddingLeft(p: Double) {
    paddingLeft = p
}

@Deprecated("Use the paddingRight property instead", ReplaceWith("paddingRight = p"))
fun Region.paddingRight(p: Double) {
    paddingRight = p
}

@Deprecated("Use the paddingVertical property instead", ReplaceWith("paddingVertical = p"))
fun Region.paddingVertical(p: Double) {
    paddingVertical = p
}

@Deprecated("Use the paddingHorizontal property instead", ReplaceWith("paddingHorizontal = p"))
fun Region.paddingHorizontal(p: Double) {
    paddingHorizontal = p
}

@Deprecated("Use the paddingAll property instead", ReplaceWith("paddingAll = p"))
fun Region.paddingAll(p: Double) {
    paddingAll = p
}


var Region.paddingTop: Number
    get() = padding.top
    set(value) {
        padding = padding.copy(top = value)
    }

var Region.paddingBottom: Number
    get() = padding.bottom
    set(value) {
        padding = padding.copy(bottom = value)
    }

var Region.paddingLeft: Number
    get() = padding.left
    set(value) {
        padding = padding.copy(left = value)
    }

var Region.paddingRight: Number
    get() = padding.right
    set(value) {
        padding = padding.copy(right = value.toDouble())
    }

var Region.paddingVertical: Number
    get() = padding.vertical * 2
    set(value) {
        val half = value.toDouble() / 2.0
        padding = padding.copy(vertical = half)
    }

var Region.paddingHorizontal: Number
    get() = padding.horizontal * 2
    set(value) {
        val half = value.toDouble() / 2.0
        padding = padding.copy(horizontal = half)
    }

var Region.paddingAll: Number
    get() = padding.all
    set(value) {
        padding = insets(value)
    }


fun Region.fitToParentHeight() {
    val parent = this.parent
    if (parent != null && parent is Region) fitToHeight(parent)
}

fun Region.fitToParentWidth() {
    val parent = this.parent
    if (parent != null && parent is Region) fitToWidth(parent)
}

fun Region.fitToParentSize() {
    fitToParentHeight()
    fitToParentWidth()
}

fun Region.fitToHeight(region: Region) {
    minHeightProperty().bind(region.heightProperty())
}

fun Region.fitToWidth(region: Region) {
    minWidthProperty().bind(region.widthProperty())
}

fun Region.fitToSize(region: Region) {
    fitToHeight(region)
    fitToWidth(region)
}
