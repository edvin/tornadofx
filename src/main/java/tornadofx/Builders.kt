package tornadofx

import javafx.beans.property.ObjectProperty
import javafx.beans.property.Property
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.text.Text
import javafx.util.Callback
import javafx.util.StringConverter
import java.time.LocalDate
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1

fun Pane.titledPane(title: String, node: Node): TitledPane {
    val pane = TitledPane(title, node)
    return opcr(this, pane)
}

fun Pane.tabpane(op: (TabPane.() -> Unit)? = null) = opcr(this, TabPane(), op)

fun <T : Node> TabPane.tab(text: String, content: T, op: (T.() -> Unit)? = null): Tab {
    val tab = Tab(text, content)
    tabs.add(tab)
    if (op != null) op(content)
    return tab
}

fun GridPane.row(title: String? = null, op: Pane.() -> Unit) {
    userData = if (userData is Int) userData as Int + 1 else 1

    // Allow the caller to add children to a fake pane
    val fake = Pane()
    if (title != null)
        fake.children.add(Label(title))

    op(fake)

    // Create a new row in the GridPane and add the children added to the fake pane
    addRow(userData as Int, *fake.children.toTypedArray())
}

fun Pane.text(initialValue: String? = null, op: (Text.() -> Unit)? = null) = opcr(this, Text().apply { if (initialValue != null) text = initialValue }, op)
fun Pane.text(property: Property<String>, op: (Text.() -> Unit)? = null) = text(op = op).apply {
    textProperty().bindBidirectional(property)
}

fun <T> Pane.combobox(property: Property<T>? = null, values: ObservableList<T>? = null, op: (ComboBox<T>.() -> Unit)? = null) = opcr(this, ComboBox<T>().apply {
    if (values != null) items = values
    if (property != null) valueProperty().bindBidirectional(property)
}, op)

fun Pane.textfield(value: String? = null, op: (TextField.() -> Unit)? = null) = opcr(this, TextField().apply { if (value != null) text = value }, op)
fun Pane.textfield(property: Property<String>, op: (TextField.() -> Unit)? = null) = textfield(op = op).apply {
    textProperty().bindBidirectional(property)
}

fun <T> Pane.textfield(property: Property<T>, converter: StringConverter<T>, op: (TextField.() -> Unit)? = null) = textfield(op = op).apply {
    textProperty().bindBidirectional(property, converter)
}

fun Pane.datepicker(op: (DatePicker.() -> Unit)? = null) = opcr(this, DatePicker(), op)
fun Pane.datepicker(property: Property<LocalDate>, op: (DatePicker.() -> Unit)? = null) = datepicker(op = op).apply {
    valueProperty().bindBidirectional(property)
}

fun Pane.textarea(value: String? = null, op: (TextArea.() -> Unit)? = null) = opcr(this, TextArea().apply { if (value != null) text = value }, op)
fun Pane.textarea(property: Property<String>, op: (TextArea.() -> Unit)? = null) = textarea(op = op).apply {
    textProperty().bindBidirectional(property)
}

fun <T> Pane.textarea(property: Property<T>, converter: StringConverter<T>, op: (TextArea.() -> Unit)? = null) = textarea(op = op).apply {
    textProperty().bindBidirectional(property, converter)
}

fun Pane.checkbox(text: String? = null, property: Property<Boolean>? = null, op: (CheckBox.() -> Unit)? = null) = opcr(this, CheckBox(text).apply {
    if (property != null) selectedProperty().bindBidirectional(property)
}, op)

fun Pane.progressIndicator(op: (ProgressIndicator.() -> Unit)? = null) = opcr(this, ProgressIndicator(), op)

fun Pane.progressBar(initialValue: Double? = null, op: (ProgressBar.() -> Unit)? = null) = opcr(this, ProgressBar().apply { if (initialValue != null) progress = initialValue }, op)
fun Pane.progressBar(property: Property<Double>, op: (ProgressBar.() -> Unit)? = null) = progressBar(op = op).apply {
    progressProperty().bind(property)
}

fun Pane.button(text: String = "", op: (Button.() -> Unit)? = null) = opcr(this, Button(text), op)
fun ToolBar.button(text: String = "", op: (Button.() -> Unit)? = null) {
    val button = Button(text)
    items.add(button)
    op?.invoke(button)
}

fun Pane.label(text: String = "", op: (Label.() -> Unit)? = null) = opcr(this, Label(text), op)
fun Pane.label(property: Property<String>, op: (Label.() -> Unit)? = null) = label(op = op).apply {
    textProperty().bind(property)
}

fun Pane.imageview(url: String? = null, op: (ImageView.() -> Unit)? = null) = opcr(this, if (url == null) ImageView() else ImageView(url), op)

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

inline fun <S> Pane.tableview(op: (FXTableView<S>.() -> Unit)): FXTableView<S> {
    val tableview = FXTableView<S>()
    op(tableview)
    children.add(tableview)
    return tableview
}

class FXTableView<S> : TableView<S>() {
    /**
     * Create a column with a value factory that extracts the value from the given callback.
     */
    fun <T> column(title: String, valueProvider: (TableColumn.CellDataFeatures<S, T>) -> ObservableValue<T>): TableColumn<S, T> {
        val column = TableColumn<S, T>(title)
        column.cellValueFactory = Callback { valueProvider(it) }
        columns.add(column)
        return column
    }

    /**
     * Create a column with a value factory that extracts the value from the given property and
     * converts the property to an observable value.
     */
    fun <T> column(title: String, prop: KMutableProperty1<S, T>): TableColumn<S, T> {
        val column = TableColumn<S, T>(title)
        column.cellValueFactory = Callback { observable(it.value, prop) }
        columns.add(column)
        return column
    }

    /**
     * Create a column with a value factory that extracts the observable value from the given function reference.
     * This method requires that you have kotlin-reflect on your classpath.
     */
    inline fun <reified T> column(title: String, observableFn: KFunction<ObjectProperty<T>>): TableColumn<S, T> {
        val column = TableColumn<S, T>(title)
        column.cellValueFactory = ReflectionHelper.CellValueFunctionRefCallback(observableFn)
        columns.add(column)
        return column
    }
}