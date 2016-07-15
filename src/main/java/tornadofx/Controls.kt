package tornadofx

import javafx.beans.binding.Bindings
import javafx.beans.property.*
import javafx.beans.value.ObservableValue
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.scene.web.HTMLEditor
import javafx.scene.web.WebView
import javafx.util.StringConverter
import javafx.util.converter.NumberStringConverter
import java.time.LocalDate
import java.util.concurrent.Callable

fun Pane.webview(op: (WebView.() -> Unit)? = null) = opcr(this, WebView(), op)

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
fun Pane.text(property: Property<String>, op: (Text.() -> Unit)? = null) = text().apply {
    textProperty().bindBidirectional(property)
    op?.invoke(this)
}

fun Pane.textfield(value: String? = null, op: (TextField.() -> Unit)? = null) = opcr(this, TextField().apply { if (value != null) text = value }, op)
fun Pane.textfield(property: Property<String>, op: (TextField.() -> Unit)? = null) = textfield().apply {
    bind(property)
    op?.invoke(this)
}

fun Pane.passwordfield(value: String? = null, op: (PasswordField.() -> Unit)? = null) = opcr(this, PasswordField().apply { if (value != null) text = value }, op)

/**
 * A textfield with an Int converter. This is useful to provide a validator that can operate on Int instead of String (typedValidator)
 */
class IntegerTextField(property: IntegerProperty) : TextField() {
    val converter = NumberStringConverter()
    init {
        textProperty().bindBidirectional(property, converter)
    }
    val convertedObservable: ObservableValue<Int> by lazy { Bindings.createObjectBinding(Callable { converter.fromString(text).toInt() }, property) }
}

/**
 * A textfield with a Double converter. This is useful to provide a validator that can operate on Double instead of String (typedValidator)
 */
class DoubleTextField(property: DoubleProperty) : TextField() {
    val converter = NumberStringConverter()
    init {
        textProperty().bindBidirectional(property, converter)
    }
    val convertedObservable: ObservableValue<Double> by lazy { Bindings.createObjectBinding(Callable { converter.fromString(text).toDouble() }, property) }
}

/**
 * A textfield with a Float converter. This is useful to provide a validator that can operate on Float instead of String (typedValidator)
 */
class FloatTextField(property: FloatProperty) : TextField() {
    val converter = NumberStringConverter()
    init {
        textProperty().bindBidirectional(property, converter)
    }
    val convertedObservable: ObservableValue<Float> by lazy { Bindings.createObjectBinding(Callable { converter.fromString(text).toFloat() }, property) }
}

/**
 * A textfield with a Long converter. This is useful to provide a validator that can operate on Long instead of String (typedValidator)
 */
class LongTextField(property: LongProperty) : TextField() {
    val converter = NumberStringConverter()
    init {
        textProperty().bindBidirectional(property, converter)
    }
    val convertedObservable: ObservableValue<Long> by lazy { Bindings.createObjectBinding(Callable { converter.fromString(text).toLong() }, property) }
}

fun Pane.textfield(property: IntegerProperty, op: (IntegerTextField.() -> Unit)? = null) : IntegerTextField = opcr(this, IntegerTextField(property), op).apply { op?.invoke(this) }
fun Pane.textfield(property: DoubleProperty, op: (DoubleTextField.() -> Unit)? = null) : DoubleTextField = opcr(this, DoubleTextField(property), op).apply { op?.invoke(this) }
fun Pane.textfield(property: FloatProperty, op: (FloatTextField.() -> Unit)? = null) : FloatTextField = opcr(this, FloatTextField(property), op).apply { op?.invoke(this) }
fun Pane.textfield(property: LongProperty, op: (LongTextField.() -> Unit)? = null) : LongTextField = opcr(this, LongTextField(property), op).apply { op?.invoke(this) }

fun Pane.datepicker(op: (DatePicker.() -> Unit)? = null) = opcr(this, DatePicker(), op)
fun Pane.datepicker(property: Property<LocalDate>, op: (DatePicker.() -> Unit)? = null) = datepicker().apply {
    valueProperty().bindBidirectional(property)
    op?.invoke(this)
}

fun Pane.textarea(value: String? = null, op: (TextArea.() -> Unit)? = null) = opcr(this, TextArea().apply { if (value != null) text = value }, op)
fun Pane.textarea(property: Property<String>, op: (TextArea.() -> Unit)? = null) = textarea().apply {
    textProperty().bindBidirectional(property)
    op?.invoke(this)
}

fun <T> Pane.textarea(property: Property<T>, converter: StringConverter<T>, op: (TextArea.() -> Unit)? = null) = textarea().apply {
    textProperty().bindBidirectional(property, converter)
    op?.invoke(this)
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
fun Pane.progressindicator(property: Property<Double>, op: (ProgressIndicator.() -> Unit)? = null) = progressindicator().apply {
    progressProperty().bind(property)
    op?.invoke(this)
}

fun Pane.progressbar(initialValue: Double? = null, op: (ProgressBar.() -> Unit)? = null) = opcr(this, ProgressBar().apply { if (initialValue != null) progress = initialValue }, op)
fun Pane.progressbar(property: Property<Double>, op: (ProgressBar.() -> Unit)? = null) = progressbar().apply {
    progressProperty().bind(property)
    op?.invoke(this)
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
fun Pane.label(property: Property<String>, op: (Label.() -> Unit)? = null) = label().apply {
    textProperty().bind(property)
    op?.invoke(this)
}

fun Pane.hyperlink(text: String = "", op: (Hyperlink.() -> Unit)? = null) = opcr(this, Hyperlink(text), op)
fun Pane.hyperlink(property: Property<String>, op: (Hyperlink.() -> Unit)? = null) = hyperlink().apply {
    textProperty().bind(property)
    op?.invoke(this)
}

fun Pane.menubar(op: (MenuBar.() -> Unit)? = null) = opcr(this, MenuBar(), op)

fun Pane.imageview(url: String? = null, op: (ImageView.() -> Unit)? = null) = opcr(this, if (url == null) ImageView() else ImageView(url), op)