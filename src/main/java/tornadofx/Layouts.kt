package tornadofx

import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.*

val GridPaneRowIdKey = "TornadoFX.GridPaneRowId";

fun GridPane.row(title: String? = null, op: (Pane.() -> Unit)? = null) {
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

fun Pane.toolbar(vararg nodes: Node, op: (ToolBar.() -> Unit)? = null): ToolBar {
    var toolbar = ToolBar()
    if (nodes.isNotEmpty())
        toolbar.items.addAll(nodes)
    opcr(this, toolbar, op)
    return toolbar
}

fun Pane.hbox(spacing: Double? = null, children: Iterable<Node>? = null, op: (HBox.() -> Unit)? = null): HBox {
    val hbox = HBox()
    if (children != null)
        hbox.children.addAll(children)
    if (spacing != null) hbox.spacing = spacing
    return opcr(this, hbox, op)
}

fun Pane.vbox(spacing: Double? = null, children: Iterable<Node>? = null, op: (VBox.() -> Unit)? = null): VBox {
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

fun Pane.separator(orientation: Orientation = Orientation.HORIZONTAL, op: (Separator.() -> Unit)? = null) = opcr(this, Separator(orientation), op)

fun Pane.stackpane(initialChildren: Iterable<Node>? = null, op: (StackPane.() -> Unit)? = null) = opcr(this, StackPane().apply { if (initialChildren != null) children.addAll(initialChildren) }, op)
fun Pane.gridpane(op: (GridPane.() -> Unit)? = null) = opcr(this, GridPane(), op)
fun Pane.flowpane(op: (FlowPane.() -> Unit)? = null) = opcr(this, FlowPane(), op)
fun Pane.tilepane(op: (TilePane.() -> Unit)? = null) = opcr(this, TilePane(), op)
fun Pane.borderpane(op: (BorderPane.() -> Unit)? = null) = opcr(this, BorderPane(), op)

fun BorderPane.top(op: (Pane.() -> Unit)? = null) {
    val vbox = VBox()
    op?.invoke(vbox)
    top = if (vbox.children.size == 1) vbox.children[0] else vbox
}

fun BorderPane.bottom(op: (Pane.() -> Unit)? = null) {
    val vbox = VBox()
    op?.invoke(vbox)
    bottom = if (vbox.children.size == 1) vbox.children[0] else vbox
}

fun BorderPane.left(op: (Pane.() -> Unit)? = null) {
    val vbox = VBox()
    op?.invoke(vbox)
    left = if (vbox.children.size == 1) vbox.children[0] else vbox
}

fun BorderPane.right(op: (Pane.() -> Unit)? = null) {
    val vbox = VBox()
    op?.invoke(vbox)
    right = if (vbox.children.size == 1) vbox.children[0] else vbox
}

fun BorderPane.center(op: (Pane.() -> Unit)? = null) {
    val vbox = VBox()
    op?.invoke(vbox)
    center = if (vbox.children.size == 1) vbox.children[0] else vbox
}

fun Pane.titledpane(title: String, node: Node): TitledPane {
    val pane = TitledPane(title, node)
    return opcr(this, pane)
}

fun Pane.pagination(pageCount: Int? = null, pageIndex: Int? = null, op: (Pagination.() -> Unit)? = null): Pagination {
    val pagination = Pagination()
    if (pageCount != null) pagination.pageCount = pageCount
    if (pageIndex != null) pagination.currentPageIndex = pageIndex
    return opcr(this, pagination, op)
}

fun Pane.scrollpane(op: (Pane.() -> Unit)? = null) {
    val vbox = VBox()
    op?.invoke(vbox)
    val scrollPane = ScrollPane()
    scrollPane.content = if (vbox.children.size == 1) vbox.children[0] else vbox
    this += scrollPane
}

fun Pane.splitpane(vararg nodes: Node, op: (SplitPane.() -> Unit)? = null): SplitPane {
    var splitpane = SplitPane()
    if (nodes.isNotEmpty())
        splitpane.items.addAll(nodes)
    opcr(this, splitpane, op)
    return splitpane
}

fun SplitPane.items(op: (Pane.() -> Unit)) {
    val fake = Pane()
    op(fake)
    items.addAll(fake.children)
}

fun Pane.anchorpane(vararg nodes: Node, op: (AnchorPane.() -> Unit)? = null): AnchorPane {
    val anchorpane = AnchorPane()
    if (nodes.isNotEmpty()) {
        anchorpane.children.addAll(nodes)
    }
    opcr(this, anchorpane, op)
    return anchorpane
}

fun Pane.accordion(vararg panes: TitledPane, op: (Accordion.() -> Unit)? = null): Accordion {
    var accordion = Accordion()
    if (panes.isNotEmpty())
        accordion.panes.addAll(panes)
    opcr(this, accordion, op)
    return accordion
}

fun Accordion.fold(title: String? = null, op: (Pane.() -> Unit)? = null): TitledPane {
    val vbox = VBox()
    op?.invoke(vbox)
    val fold = TitledPane(title, if (vbox.children.size == 1) vbox.children[0] else vbox)
    panes += fold
    return fold
}
