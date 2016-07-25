package tornadofx

import javafx.event.EventTarget
import javafx.geometry.Orientation
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.*

@Deprecated("Properties set on the fake pane would be lost. Use form/fieldset builder instead", ReplaceWith("form"), DeprecationLevel.WARNING)
fun GridPane.row(title: String? = null, op: (Pane.() -> Unit)? = null) {
    val GridPaneRowIdKey = "TornadoFX.GridPaneRowId";

    properties[GridPaneRowIdKey] = if (properties.containsKey(GridPaneRowIdKey)) properties[GridPaneRowIdKey] as Int + 1 else 1

    // Allow the caller to add children to a fake pane
    val fake = Pane()
    if (title != null)
        fake.children.add(Label(title))

    op?.invoke(fake)

    // Create a new row in the GridPane and add the children added to the fake pane
    addRow(properties[GridPaneRowIdKey] as Int, *fake.children.toTypedArray())
}

fun ToolBar.spacer(prio: Priority = Priority.ALWAYS, op: (Pane.() -> Unit)? = null): Pane {
    val pane = Pane().apply { HBox.setHgrow(this, prio) }
    op?.invoke(pane)
    add(pane)
    return pane
}

fun HBox.spacer(prio: Priority = Priority.ALWAYS, op: (Pane.() -> Unit)? = null) = opcr(this, Pane().apply { HBox.setHgrow(this, prio) }, op)
fun VBox.spacer(prio: Priority = Priority.ALWAYS, op: (Pane.() -> Unit)? = null) = opcr(this, Pane().apply { VBox.setVgrow(this, prio) }, op)

fun EventTarget.toolbar(vararg nodes: Node, op: (ToolBar.() -> Unit)? = null): ToolBar {
    val toolbar = ToolBar()
    if (nodes.isNotEmpty())
        toolbar.items.addAll(nodes)
    opcr(this, toolbar, op)
    return toolbar
}


@Deprecated("No need to wrap ToolBar children in children{} anymore. Remove the wrapper and all builder items will still be added as before.", ReplaceWith("no children{} wrapper"), DeprecationLevel.WARNING)
fun ToolBar.children(op: ToolBar.() -> Unit): ToolBar {
    op(this)
    return this
}

fun EventTarget.hbox(spacing: Double? = null, children: Iterable<Node>? = null, op: (HBox.() -> Unit)? = null): HBox {
    val hbox = HBox()
    if (children != null)
        hbox.children.addAll(children)
    if (spacing != null) hbox.spacing = spacing
    return opcr(this, hbox, op)
}

fun EventTarget.vbox(spacing: Double? = null, children: Iterable<Node>? = null, op: (VBox.() -> Unit)? = null): VBox {
    val vbox = VBox()
    if (children != null)
        vbox.children.addAll(children)
    if (spacing != null) vbox.spacing = spacing
    return opcr(this, vbox, op)
}

fun ToolBar.separator(orientation: Orientation = Orientation.HORIZONTAL, op: (Separator.() -> Unit)? = null): Separator {
    val separator = Separator(orientation)
    op?.invoke(separator)
    add(separator)
    return separator
}

fun EventTarget.separator(orientation: Orientation = Orientation.HORIZONTAL, op: (Separator.() -> Unit)? = null) = opcr(this, Separator(orientation), op)

fun EventTarget.group(initialChildren: Iterable<Node>? = null, op: (Group.() -> Unit)? = null) = opcr(this, Group().apply { if (initialChildren != null) children.addAll(initialChildren) }, op)
fun EventTarget.stackpane(initialChildren: Iterable<Node>? = null, op: (StackPane.() -> Unit)? = null) = opcr(this, StackPane().apply { if (initialChildren != null) children.addAll(initialChildren) }, op)
fun EventTarget.gridpane(op: (GridPane.() -> Unit)? = null) = opcr(this, GridPane(), op)
fun EventTarget.flowpane(op: (FlowPane.() -> Unit)? = null) = opcr(this, FlowPane(), op)
fun EventTarget.tilepane(op: (TilePane.() -> Unit)? = null) = opcr(this, TilePane(), op)
fun EventTarget.borderpane(op: (BorderPane.() -> Unit)? = null) = opcr(this, BorderPane(), op)

@Deprecated("Properties set on the container will be lost if you add only a single child Node", ReplaceWith("BorderPane.top = yourNode { }"), DeprecationLevel.WARNING)
fun BorderPane.top(op: (Pane.() -> Unit)? = null) {
    val vbox = VBox()
    op?.invoke(vbox)
    top = if (vbox.children.size == 1) vbox.children[0] else vbox
}

fun <T : Node> BorderPane.top(topNode: T, op: (T.() -> Unit)? = null): T {
    top = topNode
    return opcr(this, topNode, op)
}

fun <T : Node> BorderPane.bottom(bottomNode: T, op: (T.() -> Unit)? = null): T {
    bottom = bottomNode
    return opcr(this, bottomNode, op)
}

fun <T : Node> BorderPane.left(leftNode: T, op: (T.() -> Unit)? = null): T {
    left = leftNode
    return opcr(this, leftNode, op)
}

fun <T : Node> BorderPane.right(rightNode: T, op: (T.() -> Unit)? = null): T {
    right = rightNode
    return opcr(this, rightNode, op)
}

fun <T : Node> BorderPane.center(centerNode: T, op: (T.() -> Unit)? = null): T {
    center = centerNode
    return opcr(this, centerNode, op)
}

@Deprecated("Properties set on the container will be lost if you add only a single child Node", ReplaceWith("BorderPane.bottom = yourNode { }"), DeprecationLevel.WARNING)
fun BorderPane.bottom(op: (Pane.() -> Unit)? = null) {
    val vbox = VBox()
    op?.invoke(vbox)
    bottom = if (vbox.children.size == 1) vbox.children[0] else vbox
}

@Deprecated("Properties set on the container will be lost if you add only a single child Node", ReplaceWith("BorderPane.left = yourNode { }"), DeprecationLevel.WARNING)
fun BorderPane.left(op: (Pane.() -> Unit)? = null) {
    val vbox = VBox()
    op?.invoke(vbox)
    left = if (vbox.children.size == 1) vbox.children[0] else vbox
}

@Deprecated("Properties set on the container will be lost if you add only a single child Node", ReplaceWith("BorderPane.right = yourNode { }"), DeprecationLevel.WARNING)
fun BorderPane.right(op: (Pane.() -> Unit)? = null) {
    val vbox = VBox()
    op?.invoke(vbox)
    right = if (vbox.children.size == 1) vbox.children[0] else vbox
}

@Deprecated("Properties set on the container will be lost if you add only a single child Node", ReplaceWith("BorderPane.center = yourNode { }"), DeprecationLevel.WARNING)
fun BorderPane.center(op: (Pane.() -> Unit)? = null) {
    val vbox = VBox()
    op?.invoke(vbox)
    center = if (vbox.children.size == 1) vbox.children[0] else vbox
}

fun EventTarget.titledpane(title: String, node: Node): TitledPane = opcr(this, TitledPane(title, node))

fun EventTarget.pagination(pageCount: Int? = null, pageIndex: Int? = null, op: (Pagination.() -> Unit)? = null): Pagination {
    val pagination = Pagination()
    if (pageCount != null) pagination.pageCount = pageCount
    if (pageIndex != null) pagination.currentPageIndex = pageIndex
    return opcr(this, pagination, op)
}

@Suppress("UNCHECKED_CAST")
fun <T : Node> EventTarget.scrollpane(content: T, op: (T.() -> Unit)? = null): ScrollPane =
        opcr(this, ScrollPane(content), op as (Node.() -> Unit)?)

fun EventTarget.scrollpane(op: (ScrollPane.() -> Unit)? = null) = opcr(this, ScrollPane(), op)

fun EventTarget.splitpane(vararg nodes: Node, op: (SplitPane.() -> Unit)? = null): SplitPane {
    val splitpane = SplitPane()
    if (nodes.isNotEmpty())
        splitpane.items.addAll(nodes)
    opcr(this, splitpane, op)
    return splitpane
}

@Deprecated("No need to wrap splitpane items in items{} anymore. Remove the wrapper and all builder items will still be added as before.", ReplaceWith("no items{} wrapper"), DeprecationLevel.WARNING)
fun SplitPane.items(op: (SplitPane.() -> Unit)) = op(this)

fun EventTarget.anchorpane(vararg nodes: Node, op: (AnchorPane.() -> Unit)? = null): AnchorPane {
    val anchorpane = AnchorPane()
    if (nodes.isNotEmpty()) {
        anchorpane.children.addAll(nodes)
    }
    opcr(this, anchorpane, op)
    return anchorpane
}

fun EventTarget.accordion(vararg panes: TitledPane, op: (Accordion.() -> Unit)? = null): Accordion {
    val accordion = Accordion()
    if (panes.isNotEmpty())
        accordion.panes.addAll(panes)
    opcr(this, accordion, op)
    return accordion
}

fun <T : Node> Accordion.fold(title: String? = null, node: T, op: (T.() -> Unit)? = null): TitledPane {
    val fold = TitledPane(title, node)
    panes += fold
    op?.invoke(node)
    return fold
}

@Deprecated("Properties added to the container will be lost if you add only a single child Node", ReplaceWith("Accordion.fold(title, node, op)"), DeprecationLevel.WARNING)
fun Accordion.fold(title: String? = null, op: (Pane.() -> Unit)? = null): TitledPane {
    val vbox = VBox()
    op?.invoke(vbox)
    val fold = TitledPane(title, if (vbox.children.size == 1) vbox.children[0] else vbox)
    panes += fold
    return fold
}
