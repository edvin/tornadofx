package tornadofx

import javafx.beans.property.ObjectProperty
import javafx.event.EventTarget
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction1

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

@Suppress("UNCHECKED_CAST")
internal var BorderPane.builderTarget : KFunction1<BorderPane, ObjectProperty<Node>>?
    get() = properties["tornadofx.builderTarget"] as KFunction1<BorderPane, ObjectProperty<Node>>?
    set(value) { properties["tornadofx.builderTarget"] = value }

fun BorderPane.top(op: BorderPane.() -> Unit) = region(BorderPane::topProperty, op)
fun BorderPane.bottom(op: BorderPane.() -> Unit) = region(BorderPane::bottomProperty, op)
fun BorderPane.left(op: BorderPane.() -> Unit) = region(BorderPane::leftProperty, op)
fun BorderPane.right(op: BorderPane.() -> Unit) = region(BorderPane::rightProperty, op)
fun BorderPane.center(op: BorderPane.() -> Unit) = region(BorderPane::centerProperty, op)
internal fun BorderPane.region(region: KFunction1<BorderPane, ObjectProperty<Node>>, op: BorderPane.() -> Unit) {
    builderTarget = region
    op()
    builderTarget = null
}

@Deprecated("Use top = node {} instead")
fun <T : Node> BorderPane.top(topNode: T, op: (T.() -> Unit)? = null): T {
    top = topNode
    return opcr(this, topNode, op)
}

internal fun <C: UIComponent> BorderPane.setRegion(region: KFunction1<BorderPane, ObjectProperty<Node>>, nodeType: KClass<C>) : BorderPane {
    region.invoke(this).value = find(nodeType).root
    return this
}

fun <C: UIComponent> BorderPane.top(nodeType: KClass<C>) = setRegion(BorderPane::topProperty, nodeType)
fun <C: UIComponent> BorderPane.right(nodeType: KClass<C>) = setRegion(BorderPane::rightProperty, nodeType)
fun <C: UIComponent> BorderPane.bottom(nodeType: KClass<C>) = setRegion(BorderPane::bottomProperty, nodeType)
fun <C: UIComponent> BorderPane.left(nodeType: KClass<C>) = setRegion(BorderPane::leftProperty, nodeType)
fun <C: UIComponent> BorderPane.center(nodeType: KClass<C>) = setRegion(BorderPane::centerProperty, nodeType)

@Deprecated("Use bottom = node {} instead")
fun <T : Node> BorderPane.bottom(bottomNode: T, op: (T.() -> Unit)? = null): T {
    bottom = bottomNode
    return opcr(this, bottomNode, op)
}

@Deprecated("Use left = node {} instead")
fun <T : Node> BorderPane.left(leftNode: T, op: (T.() -> Unit)? = null): T {
    left = leftNode
    return opcr(this, leftNode, op)
}

@Deprecated("Use right = node {} instead")
fun <T : Node> BorderPane.right(rightNode: T, op: (T.() -> Unit)? = null): T {
    right = rightNode
    return opcr(this, rightNode, op)
}

@Deprecated("Use center = node {} instead")
fun <T : Node> BorderPane.center(centerNode: T, op: (T.() -> Unit)? = null): T {
    center = centerNode
    return opcr(this, centerNode, op)
}

fun EventTarget.titledpane(title: String, node: Node): TitledPane = opcr(this, TitledPane(title, node))

fun EventTarget.pagination(pageCount: Int? = null, pageIndex: Int? = null, op: (Pagination.() -> Unit)? = null): Pagination {
    val pagination = Pagination()
    if (pageCount != null) pagination.pageCount = pageCount
    if (pageIndex != null) pagination.currentPageIndex = pageIndex
    return opcr(this, pagination, op)
}

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
    if (nodes.isNotEmpty()) anchorpane.children.addAll(nodes)
    opcr(this, anchorpane, op)
    return anchorpane
}

fun EventTarget.accordion(vararg panes: TitledPane, op: (Accordion.() -> Unit)? = null): Accordion {
    val accordion = Accordion()
    if (panes.isNotEmpty()) accordion.panes.addAll(panes)
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

fun EventTarget.region(op: (Region.() -> Unit)? = null) = opcr(this, Region(), op)

fun Region.paddingRight(p: Double) {
    padding = Insets(padding.top, p, padding.bottom, padding.left)
}

fun Region.paddingLeft(p: Double) {
    padding = Insets(padding.top, padding.right, padding.bottom, p)
}

fun Region.paddingTop(p: Double) {
    padding = Insets(p, padding.right, padding.bottom, padding.left)
}

fun Region.paddingBottom(p: Double) {
    padding = Insets(padding.top, padding.right, p, padding.left)
}
