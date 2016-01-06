package tornadofx

import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox

fun Pane.titledPane(title: String? = null, op: (TitledPane.() -> Unit)? = null): TitledPane {
    val pane = TitledPane()
    if (title != null) pane.text = title
    return opcr(this, pane, op)
}

fun Pane.textfield(op: (TextField.() -> Unit)? = null) = opcr(this, TextField(), op)

fun Pane.textarea(op: (TextArea.() -> Unit)? = null) = opcr(this, TextArea(), op)

fun Pane.progressIndicator(op: (ProgressIndicator.() -> Unit)? = null) = opcr(this, ProgressIndicator(), op)

fun Pane.button(text: String = "", op: (Button.() -> Unit)? = null) = opcr(this, Button(text), op)

fun Pane.label(text: String = "", op: (Label.() -> Unit)? = null) = opcr(this, Label(text), op)

fun Pane.hbox(spacing: Double? = null, op: (HBox.() -> Unit)? = null): HBox {
    val hbox = HBox()
    if (spacing != null) hbox.spacing = spacing
    return opcr(this, hbox, op)
}

fun Pane.vbox(spacing: Double? = null, op: (VBox.() -> Unit)? = null): VBox {
    val vbox = VBox()
    if (spacing != null) vbox.spacing = spacing
    return opcr(this, vbox, op)
}

fun Pane.gridpane(op: (GridPane.() -> Unit)? = null) = opcr(this, GridPane(), op)

/**
 * Add the given node to the pane, invoke the node operation and return the node
 */
private fun <T : Node> opcr(pane: Pane, node: T, op: (T.() -> Unit)? = null): T {
    pane.children.add(node)
    op?.invoke(node)
    return node
}