package tornadofx

import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox

fun Pane.textfield(op: TextField.() -> Unit = {}): TextField {
    val textfield = TextField()
    op(textfield)
    children.add(textfield)
    return textfield
}

fun Pane.textarea(op: TextArea.() -> Unit = {}): TextArea {
    val textarea = TextArea()
    op(textarea)
    children.add(textarea)
    return textarea
}

fun Pane.progressIndicator(op: ProgressIndicator.() -> Unit = {}): ProgressIndicator {
    val indicator = ProgressIndicator()
    op(indicator)
    children.add(indicator)
    return indicator
}

fun Pane.button(text: String = "", op: Button.() -> Unit = {}): Button {
    val button = Button(text)
    op(button)
    children.add(button)
    return button
}

fun Pane.label(text: String = "", op: Label.() -> Unit = {}): Label {
    val label = Label(text)
    op(label)
    children.add(label)
    return label
}

fun Pane.hbox(spacing: Double = 0.0, op: HBox.() -> Unit = {}): HBox {
    val hbox = HBox(spacing)
    op(hbox)
    children.add(hbox)
    return hbox
}

fun Pane.vbox(spacing: Double = 0.0, op: VBox.() -> Unit = {}): VBox {
    val vbox = VBox(spacing)
    op(vbox)
    children.add(vbox)
    return vbox
}

fun Pane.gridpane(op: GridPane.() -> Unit = {}): GridPane {
    val gridpane = GridPane()
    op(gridpane)
    children.add(gridpane)
    return gridpane
}

