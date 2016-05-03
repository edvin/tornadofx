package tornadofx

import javafx.beans.property.Property
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.scene.web.HTMLEditor
import javafx.util.StringConverter
import java.time.LocalDate

enum class ColorPickerMode { Button, MenuButton, SplitMenuButton }
fun Pane.colorpicker(color: Color? = null, mode: ColorPickerMode = ColorPickerMode.Button, op: (ColorPicker.() -> Unit)? = null): ColorPicker {
    val picker = ColorPicker()
    if (mode == ColorPickerMode.MenuButton) picker.addClass(ColorPicker.STYLE_CLASS_BUTTON)
    else if (mode == ColorPickerMode.SplitMenuButton) picker.addClass(ColorPicker.STYLE_CLASS_SPLIT_BUTTON)
    if (color != null) picker.value = color
    return opcr(this, picker, op)
}

fun Pane.tabpane(op: (TabPane.() -> Unit)? = null) = opcr(this, TabPane(), op)

fun Pane.textflow(op: (TextFlow.() -> Unit)? = null) = opcr(this, TextFlow(), op)

fun Pane.text(op: (Text.() -> Unit)? = null) = opcr(this, Text(), op)

fun <T : Node> TabPane.tab(text: String, content: T, op: (T.() -> Unit)? = null): Tab {
    val tab = Tab(text, content)
    tabs.add(tab)
    if (op != null) op(content)
    return tab
}

fun Pane.text(initialValue: String? = null, op: (Text.() -> Unit)? = null) = opcr(this, Text().apply { if (initialValue != null) text = initialValue }, op)
fun Pane.text(property: Property<String>, op: (Text.() -> Unit)? = null) = text(op = op).apply {
    textProperty().bindBidirectional(property)
}

fun Pane.textfield(value: String? = null, op: (TextField.() -> Unit)? = null) = opcr(this, TextField().apply { if (value != null) text = value }, op)
fun Pane.textfield(property: Property<String>, op: (TextField.() -> Unit)? = null) = textfield(op = op).apply {
    bind(property)
}

fun Pane.passwordfield(value: String? = null, op: (PasswordField.() -> Unit)? = null) = opcr(this, PasswordField().apply { if (value != null) text = value }, op)

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

fun Pane.buttonbar(buttonOrder: String? = null, op: (ButtonBar.() -> Unit)): ButtonBar {
    val bar = ButtonBar()
    if (buttonOrder != null) bar.buttonOrder = buttonOrder
    return opcr(this, bar, op)
}

fun Pane.htmleditor(html: String? = null, op: (HTMLEditor.() -> Unit)? = null) = opcr(this, HTMLEditor().apply { if (html != null) htmlText = html }, op)

fun Pane.checkbox(text: String? = null, property: Property<Boolean>? = null, op: (CheckBox.() -> Unit)? = null) = opcr(this, CheckBox(text).apply {
    if (property != null) selectedProperty().bindBidirectional(property)
}, op)

fun Pane.progressindicator(op: (ProgressIndicator.() -> Unit)? = null) = opcr(this, ProgressIndicator(), op)
fun Pane.progressindicator(property: Property<Double>, op: (ProgressIndicator.() -> Unit)? = null) = progressindicator(op = op).apply {
    progressProperty().bind(property)
}

fun Pane.progressbar(initialValue: Double? = null, op: (ProgressBar.() -> Unit)? = null) = opcr(this, ProgressBar().apply { if (initialValue != null) progress = initialValue }, op)
fun Pane.progressbar(property: Property<Double>, op: (ProgressBar.() -> Unit)? = null) = progressbar(op = op).apply {
    progressProperty().bind(property)
}

fun Pane.slider(min: Double? = null, max: Double? = null, value: Double? = null, orientation: Orientation? = null, op: (Slider.() -> Unit)? = null) = opcr(this, Slider().apply {
    if (min != null) this.min = min
    if (max != null) this.max = max
    if (value != null) this.value = value
    if (orientation != null) this.orientation = orientation
}, op)

// Buttons
fun Pane.button(text: String = "", graphic: Node? = null, op: (Button.() -> Unit)? = null): Button {
    val button = Button(text)
    if (graphic != null) button.graphic = graphic
    return opcr(this, button, op)
}

fun ToolBar.button(text: String = "", graphic: Node? = null, op: (Button.() -> Unit)? = null): Button {
    val button = Button(text)
    if (graphic != null)
        button.graphic = graphic
    items.add(button)
    op?.invoke(button)
    return button
}

fun ButtonBar.button(text: String = "", graphic: Node? = null, op: (Button.() -> Unit)? = null): Button {
    val button = Button(text)
    if (graphic != null)
        button.graphic = graphic
    buttons.add(button)
    op?.invoke(button)
    return button
}

fun Pane.togglegroup(op: (ToggleGroup.() -> Unit)? = null): ToggleGroup {
    val group = ToggleGroup()
    properties["tornadofx.togglegroup"] = group
    op?.invoke(group)
    return group
}

fun Pane.togglebutton(text: String = "", group: ToggleGroup? = getToggleGroup(), op: (ToggleButton.() -> Unit)? = null) =
        opcr(this, ToggleButton(text).apply { if (group != null) toggleGroup = group }, op)

fun Pane.radiobutton(text: String = "", group: ToggleGroup? = getToggleGroup(), op: (RadioButton.() -> Unit)? = null)
        = opcr(this, RadioButton(text).apply { if (group != null) toggleGroup = group }, op)

fun Pane.label(text: String = "", op: (Label.() -> Unit)? = null) = opcr(this, Label(text), op)
fun Pane.label(property: Property<String>, op: (Label.() -> Unit)? = null) = label(op = op).apply {
    textProperty().bind(property)
}

fun Pane.hyperlink(text: String = "", op: (Hyperlink.() -> Unit)? = null) = opcr(this, Hyperlink(text), op)
fun Pane.hyperlink(property: Property<String>, op: (Hyperlink.() -> Unit)? = null) = hyperlink(op = op).apply {
    textProperty().bind(property)
}

fun Pane.menubar(op: (MenuBar.() -> Unit)? = null) = opcr(this, MenuBar(), op)

fun Pane.imageview(url: String? = null, op: (ImageView.() -> Unit)? = null) = opcr(this, if (url == null) ImageView() else ImageView(url), op)