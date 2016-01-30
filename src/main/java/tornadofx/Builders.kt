package tornadofx

import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.*

fun Pane.titledPane(title: String? = null, op: (TitledPane.() -> Unit)? = null): TitledPane {
    val pane = TitledPane()
    if (title != null) pane.text = title
    return opcr(this, pane, op)
}

fun Pane.textfield(value: String? = null, op: (TextField.() -> Unit)? = null) = opcr(this, TextField().apply { if (value != null) text = value }, op)

fun Pane.datepicker(op: (DatePicker.() -> Unit)? = null) = opcr(this, DatePicker(), op)

fun Pane.textarea(value: String? = null, op: (TextArea.() -> Unit)? = null) = opcr(this, TextArea().apply { if (value != null) text = value }, op)

fun Pane.checkbox(text: String? = null, op: (CheckBox.() -> Unit)? = null) = opcr(this, CheckBox(text), op)

fun Pane.progressIndicator(op: (ProgressIndicator.() -> Unit)? = null) = opcr(this, ProgressIndicator(), op)

fun Pane.progressBar(initialValue: Double? = null, op: (ProgressBar.() -> Unit)? = null) = opcr(this, ProgressBar().apply { if (initialValue != null) progress = initialValue }, op)

fun Pane.button(text: String = "", op: (Button.() -> Unit)? = null) = opcr(this, Button(text), op)

fun Pane.label(text: String = "", op: (Label.() -> Unit)? = null) = opcr(this, Label(text), op)

fun Pane.imageview(url: String? = null, op: (ImageView.() -> Unit)? = null) = opcr(this, if (url == null) ImageView() else ImageView(url), op)

fun HBox.spacer(prio: Priority = Priority.ALWAYS, op: (Pane.() -> Unit)? = null) = opcr(this, Pane().apply { HBox.setHgrow(this, prio) }, op)
fun VBox.spacer(prio: Priority = Priority.ALWAYS, op: (Pane.() -> Unit)? = null) = opcr(this, Pane().apply { VBox.setVgrow(this, prio) }, op)

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

fun Pane.stackpane(initialChildren: Iterable<Node>? = null, op: (StackPane.() -> Unit)? = null) = opcr(this, StackPane().apply { if (initialChildren != null) children.addAll(initialChildren) }, op)
fun Pane.gridpane(op: (GridPane.() -> Unit)? = null) = opcr(this, GridPane(), op)

/**
 * Add the given node to the pane, invoke the node operation and return the node
 */
private fun <T : Node> opcr(pane: Pane, node: T, op: (T.() -> Unit)? = null): T {
    pane.children.add(node)
    op?.invoke(node)
    return node
}