package tornadofx

import javafx.beans.property.ObjectProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableBooleanValue
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableMap
import javafx.event.EventTarget
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.scene.web.HTMLEditor
import javafx.scene.web.WebView
import javafx.util.StringConverter
import java.time.LocalDate


@Deprecated("Properties set on the fake node would be lost. Do not use this function.", ReplaceWith("Manually adding children"))
fun children(addTo: MutableList<Node>, op: Pane.() -> Unit) {
    val fake = Pane().also(op)
    addTo.addAll(fake.children)
}


// ================================================================
// Text

fun EventTarget.text(op: Text.() -> Unit = {}): Text = Text().attachTo(this, op)

fun EventTarget.text(value: String? = null, op: Text.() -> Unit = {}): Text = Text().attachTo(this, op) {
    if (value != null) it.text = value
}

fun EventTarget.text(observable: ObservableValue<String>, op: Text.() -> Unit = {}): Text = text {
    bind(observable)
    op(this)
}

fun EventTarget.textflow(op: TextFlow.() -> Unit = {}): TextFlow = TextFlow().attachTo(this, op)


// ================================================================
// Label

fun EventTarget.label(
    text: String = "",
    graphic: Node? = null,
    op: Label.() -> Unit = {}
): Label = Label(text).attachTo(this, op) {
    if (graphic != null) it.graphic = graphic
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> EventTarget.label(
    observable: ObservableValue<T>,
    graphicProperty: ObjectProperty<Node>? = null,
    converter: StringConverter<in T>? = null,
    noinline op: Label.() -> Unit = {}
): Label = label {
    when {
        converter != null -> textProperty().bind(observable.stringBinding { converter.toString(it) })
        T::class == String::class -> textProperty().bind(observable as ObservableValue<String>)
        else -> textProperty().bind(observable.stringBinding { it?.toString() })
    }
    if (graphicProperty != null) graphicProperty().bind(graphicProperty)
    op(this)
}


// ================================================================
// TextField

fun EventTarget.textfield(value: String? = null, op: TextField.() -> Unit = {}): TextField = TextField().attachTo(this, op) {
    if (value != null) it.text = value
}

fun EventTarget.textfield(property: ObservableValue<String>, op: TextField.() -> Unit = {}): TextField = textfield {
    bind(property)
    op(this)
}

@JvmName("textfieldNumber")
fun EventTarget.textfield(property: ObservableValue<Number>, op: TextField.() -> Unit = {}): TextField = textfield {
    bind(property)
    op(this)
}

fun <T> EventTarget.textfield(property: Property<T>, converter: StringConverter<T>, op: TextField.() -> Unit = {}): TextField = textfield {
    textProperty().bindBidirectional(property, converter)
    ViewModel.register(textProperty(), property)
    op(this)
}


fun EventTarget.passwordfield(value: String? = null, op: PasswordField.() -> Unit = {}): PasswordField = PasswordField().attachTo(this, op) {
    if (value != null) it.text = value
}

fun EventTarget.passwordfield(property: ObservableValue<String>, op: PasswordField.() -> Unit = {}): PasswordField = passwordfield {
    bind(property)
    op(this)
}


// ================================================================
// TextArea

fun EventTarget.textarea(value: String? = null, op: TextArea.() -> Unit = {}): TextArea = TextArea().attachTo(this, op) {
    if (value != null) it.text = value
}

fun EventTarget.textarea(property: ObservableValue<String>, op: TextArea.() -> Unit = {}): TextArea = textarea {
    bind(property)
    op(this)
}

fun <T> EventTarget.textarea(property: Property<T>, converter: StringConverter<T>, op: TextArea.() -> Unit = {}): TextArea = textarea {
    textProperty().bindBidirectional(property, converter)
    ViewModel.register(textProperty(), property)
    op(this)
}


// ================================================================
// DatePicker

fun EventTarget.datepicker(op: DatePicker.() -> Unit = {}): DatePicker = DatePicker().attachTo(this, op)

fun EventTarget.datepicker(property: Property<LocalDate>, op: DatePicker.() -> Unit = {}): DatePicker = datepicker {
    bind(property)
    op(this)
}


// ================================================================
// Progress

fun EventTarget.progressindicator(op: ProgressIndicator.() -> Unit = {}): ProgressIndicator = ProgressIndicator().attachTo(this, op)

fun EventTarget.progressindicator(property: Property<Number>, op: ProgressIndicator.() -> Unit = {}): ProgressIndicator = progressindicator {
    bind(property)
    op(this)
}


fun EventTarget.progressbar(value: Double? = null, op: ProgressBar.() -> Unit = {}): ProgressBar = ProgressBar().attachTo(this, op) {
    if (value != null) it.progress = value
}

fun EventTarget.progressbar(property: ObservableValue<Number>, op: ProgressBar.() -> Unit = {}): ProgressBar = progressbar {
    bind(property)
    op(this)
}


fun EventTarget.slider(
    min: Number? = null,
    max: Number? = null,
    value: Number? = null,
    orientation: Orientation? = null,
    op: Slider.() -> Unit = {}
): Slider = Slider().attachTo(this, op) {
    if (min != null) it.min = min.toDouble()
    if (max != null) it.max = max.toDouble()
    if (value != null) it.value = value.toDouble()
    if (orientation != null) it.orientation = orientation
}

fun <T> EventTarget.slider(
    range: ClosedRange<T>,
    value: Number? = null,
    orientation: Orientation? = null,
    op: Slider.() -> Unit = {}
): Slider
        where T : Comparable<T>,
              T : Number = slider(range.start, range.endInclusive, value, orientation, op)


// ================================================================
// Buttons

fun EventTarget.button(
    text: String = "",
    graphic: Node? = null,
    op: Button.() -> Unit = {}
): Button = Button(text).attachTo(this, op) {
    if (graphic != null) it.graphic = graphic
}

fun EventTarget.button(
    text: ObservableValue<String>,
    graphic: Node? = null,
    op: Button.() -> Unit = {}
): Button = Button().attachTo(this, op) {
    it.textProperty().bind(text)
    if (graphic != null) it.graphic = graphic
}


fun EventTarget.menubutton(
    text: String = "",
    graphic: Node? = null,
    op: MenuButton.() -> Unit = {}
): MenuButton = MenuButton(text).attachTo(this, op) {
    if (graphic != null) it.graphic = graphic
}


fun ToolBar.button(
    text: String = "",
    graphic: Node? = null,
    op: Button.() -> Unit = {}
): Button = Button(text).also {
    if (graphic != null) it.graphic = graphic
    items += it
    op(it)
}

fun ToolBar.button(
    text: ObservableValue<String>,
    graphic: Node? = null,
    op: Button.() -> Unit = {}
): Button = Button().also {
    it.textProperty().bind(text)
    if (graphic != null) it.graphic = graphic
    items += it
    op(it)
}


fun EventTarget.buttonbar(buttonOrder: String? = null, op: ButtonBar.() -> Unit): ButtonBar = ButtonBar().attachTo(this, op) {
    if (buttonOrder != null) it.buttonOrder = buttonOrder
}

fun ButtonBar.button(
    text: String = "",
    type: ButtonBar.ButtonData? = null,
    graphic: Node? = null,
    op: Button.() -> Unit = {}
): Button = Button(text).also {
    if (type != null) ButtonBar.setButtonData(it, type)
    if (graphic != null) it.graphic = graphic
    buttons += it
    op(it)
}

fun ButtonBar.button(
    text: ObservableValue<String>,
    type: ButtonBar.ButtonData? = null,
    graphic: Node? = null,
    op: Button.() -> Unit = {}
): Button = Button().also {
    it.textProperty().bind(text)
    if (type != null) ButtonBar.setButtonData(it, type)
    if (graphic != null) it.graphic = graphic
    buttons += it
    op(it)
}


/**
 * Create a togglebutton inside the current or given toggle group. The optional value parameter will be matched against
 * the extension property `selectedValueProperty()` on Toggle Group. If the #ToggleGroup.selectedValueProperty is used,
 * it's value will be updated to reflect the value for this radio button when it's selected.
 *
 * Likewise, if the `selectedValueProperty` of the ToggleGroup is updated to a value that matches the value for this
 * togglebutton, it will be automatically selected.
 */
fun EventTarget.togglebutton(
    text: String? = null,
    group: ToggleGroup? = getToggleGroup(),
    selectFirst: Boolean = true,
    value: Any? = null,
    op: ToggleButton.() -> Unit = {}
): ToggleButton = ToggleButton().attachTo(this, op) {
    it.text = text ?: value?.toString() ?: ""
    it.properties["tornadofx.toggleGroupValue"] = value ?: text
    if (group != null) it.toggleGroup = group
    if (it.toggleGroup?.selectedToggle == null && selectFirst) it.isSelected = true
}

fun EventTarget.togglebutton(
    text: ObservableValue<String>? = null,
    group: ToggleGroup? = getToggleGroup(),
    selectFirst: Boolean = true,
    value: Any? = null,
    op: ToggleButton.() -> Unit = {}
): ToggleButton = ToggleButton().attachTo(this, op) {
    it.textProperty().bind(text)
    it.properties["tornadofx.toggleGroupValue"] = value ?: text
    if (group != null) it.toggleGroup = group
    if (it.toggleGroup?.selectedToggle == null && selectFirst) it.isSelected = true
}

fun ToggleButton.whenSelected(op: () -> Unit) {
    selectedProperty().onChange { if (it) op() }
}


/**
 * Create a radiobutton inside the current or given toggle group. The optional value parameter will be matched against
 * the extension property `selectedValueProperty()` on Toggle Group. If the #ToggleGroup.selectedValueProperty is used,
 * it's value will be updated to reflect the value for this radio button when it's selected.
 *
 * Likewise, if the `selectedValueProperty` of the ToggleGroup is updated to a value that matches the value for this
 * radiobutton, it will be automatically selected.
 */
fun EventTarget.radiobutton(
    text: String? = null,
    group: ToggleGroup? = getToggleGroup(),
    value: Any? = null,
    op: RadioButton.() -> Unit = {}
): RadioButton = RadioButton().attachTo(this, op) {
    it.text = text ?: value?.toString() ?: ""
    it.properties["tornadofx.toggleGroupValue"] = value ?: text
    if (group != null) it.toggleGroup = group
}


fun EventTarget.checkbox(
    text: String? = null,
    property: Property<Boolean>? = null,
    op: CheckBox.() -> Unit = {}
): CheckBox = CheckBox(text).attachTo(this, op) {
    if (property != null) it.bind(property)
}


fun Node.togglegroup(op: ToggleGroup.() -> Unit = {}): ToggleGroup = ToggleGroup().also {
    properties["tornadofx.togglegroup"] = it
    op(it)
}

/**
 * Bind the selectedValueProperty of this toggle group to the given property. Passing in a writeable value
 * will result in a bidirectional binding, while passing in a read only value will result in a unidirectional binding.
 *
 * If the toggles are configured with the value parameter (@see #togglebutton and #radiogroup), the corresponding
 * button will be selected when the value is changed. Likewise, if the selected toggle is changed,
 * the property value will be updated if it is writeable.
 */
fun <T> ToggleGroup.bind(property: ObservableValue<T>): ObjectProperty<T> = selectedValueProperty<T>().apply {
    (property as? Property<T>)?.also(::bindBidirectional) ?: bind(property)
}

/**
 * Generates a writable property that represents the selected value for this toggele group.
 * If the toggles are configured with a value (@see #togglebutton and #radiogroup) the corresponding
 * toggle will be selected when this value is changed. Likewise, if the toggle is changed by clicking
 * it, the value for the toggle will be written to this property.
 *
 * To bind to this property, use the #ToggleGroup.bind() function.
 */
@Suppress("UNCHECKED_CAST")
fun <T> ToggleGroup.selectedValueProperty(): ObjectProperty<T> = properties.getOrPut("tornadofx.selectedValueProperty") {
    SimpleObjectProperty<T>().apply {
        selectedToggleProperty().onChange { value = it?.properties?.get("tornadofx.toggleGroupValue") as T? }
        onChange { selectedValue -> selectToggle(toggles.find { it.properties["tornadofx.toggleGroupValue"] == selectedValue }) }
    }
} as ObjectProperty<T>


// ================================================================
// HTML

fun EventTarget.webview(op: WebView.() -> Unit = {}): WebView = WebView().attachTo(this, op)


fun EventTarget.htmleditor(
    html: String? = null,
    op: HTMLEditor.() -> Unit = {}
): HTMLEditor = HTMLEditor().attachTo(this, op) {
    if (html != null) it.htmlText = html
}


fun EventTarget.hyperlink(
    text: String = "",
    graphic: Node? = null,
    op: Hyperlink.() -> Unit = {}
): Hyperlink = Hyperlink(text, graphic).attachTo(this, op)

fun EventTarget.hyperlink(
    observable: ObservableValue<String>,
    graphic: Node? = null,
    op: Hyperlink.() -> Unit = {}
): Hyperlink = hyperlink(graphic = graphic) {
    bind(observable)
    op(this)
}


enum class ColorPickerMode { Button, MenuButton, SplitMenuButton }

fun EventTarget.colorpicker(
    color: Color? = null,
    mode: ColorPickerMode = ColorPickerMode.Button,
    op: ColorPicker.() -> Unit = {}
): ColorPicker = ColorPicker().attachTo(this, op) {
    if (mode == ColorPickerMode.MenuButton) it.addClass(ColorPicker.STYLE_CLASS_BUTTON)
    else if (mode == ColorPickerMode.SplitMenuButton) it.addClass(ColorPicker.STYLE_CLASS_SPLIT_BUTTON)
    if (color != null) it.value = color
}


fun EventTarget.menubar(op: MenuBar.() -> Unit = {}): MenuBar = MenuBar().attachTo(this, op)


// ================================================================
// ImageView

fun EventTarget.imageview(
    url: String? = null,
    lazyload: Boolean = true,
    op: ImageView.() -> Unit = {}
): ImageView = opcr(this, if (url == null) ImageView() else ImageView(Image(url, lazyload)), op)

fun EventTarget.imageview(
    url: ObservableValue<String>,
    lazyload: Boolean = true,
    op: ImageView.() -> Unit = {}
): ImageView = ImageView().attachTo(this, op) { imageView ->
    imageView.imageProperty().bind(objectBinding(url) { value?.let { Image(it, lazyload) } })
}

fun EventTarget.imageview(
    image: ObservableValue<Image?>,
    op: ImageView.() -> Unit = {}
): ImageView = ImageView().attachTo(this, op) {
    it.imageProperty().bind(image)
}

fun EventTarget.imageview(
    image: Image,
    op: ImageView.() -> Unit = {}
): ImageView = ImageView(image).attachTo(this, op)


// ================================================================
// Property Utilities

/** Listen to changes and update the value of the property if the given mutator results in a different value. */
fun <T : Any?> Property<T>.mutateOnChange(mutator: (T?) -> T?): ObservableValue<T> = onChange {
    val changed = mutator(value)
    if (changed != value) value = changed
}


/** Remove leading or trailing whitespace from a Text Input Control. */
fun TextInputControl.trimWhitespace(): ObservableBooleanValue = focusedProperty().onChange { focused ->
    if (!focused && text != null) text = text.trim()
}

/** Remove any whitespace from a Text Input Control. */
fun TextInputControl.stripWhitespace(): ObservableValue<String> = textProperty().mutateOnChange { it?.replace(Regex("\\s*"), "") }

/** Remove any non integer values from a Text Input Control. */
fun TextInputControl.stripNonInteger(): ObservableValue<String> = textProperty().mutateOnChange { it?.replace(Regex("[^0-9]"), "") }

/** Remove any non integer values from a Text Input Control. */ // TODO Maybe add deprecated version so it doesn't break existing code?
fun TextInputControl.stripNonNumeric(vararg allowedChars: Char = charArrayOf('.', ',')): ObservableValue<String> =
    textProperty().mutateOnChange { it?.replace(Regex("[^0-9${allowedChars.joinToString("")}]"), "") }


internal val EventTarget.properties: ObservableMap<Any, Any>
    get() = when (this) {
        is Node -> properties
        is Tab -> properties
        is MenuItem -> properties
        else -> throw IllegalArgumentException("Don't know how to extract properties object from $this")
    }

@Suppress("UNCHECKED_CAST")
var EventTarget.tagProperty: Property<Any?>
    get() = properties.getOrPut("tornadofx.value") { SimpleObjectProperty<Any?>() } as SimpleObjectProperty<Any?>
    set(value) {
        properties["tornadofx.value"] = value
    }

var EventTarget.tag: Any?
    get() = tagProperty.value
    set(value) {
        tagProperty.value = value
    }


// ================================================================
// Action

fun MenuItem.action(op: () -> Unit): Unit = setOnAction { op() }
fun TextField.action(op: () -> Unit): Unit = setOnAction { op() }
fun ButtonBase.action(op: () -> Unit): Unit = setOnAction { op() }
fun ChoiceBox<*>.action(op: () -> Unit): Unit = setOnAction { op() }
