package tornadofx

import javafx.beans.property.ObjectProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableMap
import javafx.event.EventTarget
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.scene.web.HTMLEditor
import javafx.scene.web.WebView
import javafx.stage.Window
import javafx.util.StringConverter
import java.time.LocalDate


internal const val VALUE_PROPERTY = "tornadofx.value"
internal const val SELECTED_VALUE_PROPERTY = "tornadofx.selectedValue"
internal const val TOGGLE_GROUP_PROPERTY = "tornadofx.toggleGroup"
internal const val TOGGLE_GROUP_VALUE_PROPERTY = "tornadofx.toggleGroupValue"


internal val EventTarget.properties: ObservableMap<Any, Any>
    get() = when (this) {
        is Scene -> properties
        is Window -> properties
        is Tab -> properties
        is Node -> properties
        is MenuItem -> properties
        is Toggle -> properties
        is ToggleGroup -> properties
        is TableColumnBase<*, *> -> this.properties // Explicit this is required due to weird SmartCast
        else -> throw IllegalArgumentException("Don't know how to extract properties object from $this")
    }

@Suppress("UNCHECKED_CAST")
var EventTarget.tagProperty: Property<Any?>
    get() = properties.getOrPut(VALUE_PROPERTY) { SimpleObjectProperty<Any?>() } as SimpleObjectProperty<Any?>
    set(value) {
        properties[VALUE_PROPERTY] = value
    }

var EventTarget.tag: Any?
    get() = tagProperty.value
    set(value) {
        tagProperty.value = value
    }


@Deprecated("Properties set on the fake node would be lost. Do not use this function. Add children manually instead.")
fun children(addTo: MutableList<Node>, op: Pane.() -> Unit) {
    val fake = Pane().also(op)
    addTo.addAll(fake.children)
}


// ================================================================
// Text

fun EventTarget.text(
    value: String? = null,
    op: Text.() -> Unit = {}
): Text = Text().attachTo(this, op) { if (value != null) it.text = value }

fun EventTarget.text(
    observable: ObservableValue<String>,
    op: Text.() -> Unit = {}
): Text = Text().attachTo(this, op) { it.bind(observable) }

fun EventTarget.textflow(op: TextFlow.() -> Unit = {}): TextFlow = TextFlow().attachTo(this, op)


// ================================================================
// TextField

fun EventTarget.textfield(
    value: String? = null,
    op: TextField.() -> Unit = {}
): TextField = TextField().attachTo(this, op) { if (value != null) it.text = value }

fun EventTarget.textfield(
    observable: ObservableValue<String>,
    op: TextField.() -> Unit = {}
): TextField = TextField().attachTo(this, op) { it.bind(observable) }

@JvmName("textfieldNumber")
fun EventTarget.textfield(
    observable: ObservableValue<Number>,
    op: TextField.() -> Unit = {}
): TextField = TextField().attachTo(this, op) { it.bind(observable) }

fun <T> EventTarget.textfield(
    property: Property<T>,
    converter: StringConverter<T>,
    op: TextField.() -> Unit = {}
): TextField = TextField().attachTo(this, op) {
    it.textProperty().bindBidirectional(property, converter)
    ViewModel.register(it.textProperty(), property)
}


// ================================================================
// PasswordField

fun EventTarget.passwordfield(
    value: String? = null,
    op: PasswordField.() -> Unit = {}
): PasswordField = PasswordField().attachTo(this, op) { if (value != null) it.text = value }

fun EventTarget.passwordfield(
    observable: ObservableValue<String>,
    op: PasswordField.() -> Unit = {}
): PasswordField = PasswordField().attachTo(this, op) { it.bind(observable) }


// ================================================================
// DatePicker

fun EventTarget.datepicker(
    value: LocalDate? = null,
    op: DatePicker.() -> Unit = {}
): DatePicker = DatePicker().attachTo(this, op) { if (value != null) it.value = value }

fun EventTarget.datepicker(
    observable: ObservableValue<LocalDate>,
    op: DatePicker.() -> Unit = {}
): DatePicker = DatePicker().attachTo(this, op) { it.bind(observable) }


// ================================================================
// TextArea

fun EventTarget.textarea(
    value: String? = null,
    op: TextArea.() -> Unit = {}
): TextArea = TextArea().attachTo(this, op) { if (value != null) it.text = value }

fun EventTarget.textarea(
    observable: ObservableValue<String>,
    op: TextArea.() -> Unit = {}
): TextArea = TextArea().attachTo(this, op) { it.bind(observable) }

fun <T> EventTarget.textarea(
    property: Property<T>,
    converter: StringConverter<T>,
    op: TextArea.() -> Unit = {}
): TextArea = TextArea().attachTo(this, op) {
    it.textProperty().bindBidirectional(property, converter)
    ViewModel.register(it.textProperty(), property)
}


fun EventTarget.buttonbar(
    buttonOrder: String? = null,
    op: ButtonBar.() -> Unit
): ButtonBar = ButtonBar().attachTo(this, op) { if (buttonOrder != null) it.buttonOrder = buttonOrder }


// ================================================================
// ProgressIndicator

fun EventTarget.progressindicator(
    value: Double? = null,
    op: ProgressIndicator.() -> Unit = {}
): ProgressIndicator = ProgressIndicator().attachTo(this, op) { if (value != null) it.progress = value }

fun EventTarget.progressindicator(
    observable: ObservableValue<Number>,
    op: ProgressIndicator.() -> Unit = {}
): ProgressIndicator = ProgressIndicator().attachTo(this, op) { it.bind(observable) }


// ================================================================
// ProgressBar

fun EventTarget.progressbar(
    value: Double? = null,
    op: ProgressBar.() -> Unit = {}
): ProgressBar = ProgressBar().attachTo(this, op) { if (value != null) it.progress = value }

fun EventTarget.progressbar(
    observable: ObservableValue<Number>,
    op: ProgressBar.() -> Unit = {}
): ProgressBar = ProgressBar().attachTo(this, op) { it.bind(observable) }


// ================================================================
// Slider

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
        where T : Number,
              T : Comparable<T> {
    return slider(range.start, range.endInclusive, value, orientation, op)
}


// ================================================================
// CheckBox

fun EventTarget.checkbox(
    text: String? = null,
    observable: ObservableValue<Boolean>? = null,
    op: CheckBox.() -> Unit = {}
): CheckBox = CheckBox(text).attachTo(this, op) { if (observable != null) it.bind(observable) }


// ================================================================
// Button

fun EventTarget.button(
    text: String = "", // FIXME This parameter is not nullable but in all the other existing functions it is nullable
    graphic: Node? = null,
    op: Button.() -> Unit = {}
): Button = Button(text).attachTo(this, op) {
    if (graphic != null) it.graphic = graphic
}

fun EventTarget.button(
    observable: ObservableValue<String>,
    graphic: Node? = null,
    op: Button.() -> Unit = {}
): Button = Button().attachTo(this, op) {
    it.textProperty().bind(observable)
    if (graphic != null) it.graphic = graphic
}


fun ToolBar.button(
    text: String = "",
    graphic: Node? = null,
    op: Button.() -> Unit = {}
): Button = Button(text).attachTo(this, op) {
    if (graphic != null) it.graphic = graphic
}

fun ToolBar.button(
    observable: ObservableValue<String>,
    graphic: Node? = null,
    op: Button.() -> Unit = {}
): Button = Button().attachTo(this, op) {
    it.textProperty().bind(observable)
    if (graphic != null) it.graphic = graphic
}


fun ButtonBar.button(
    text: String = "",
    type: ButtonBar.ButtonData? = null,
    graphic: Node? = null,
    op: Button.() -> Unit = {}
): Button = Button(text).attachTo(this, op) {
    if (type != null) ButtonBar.setButtonData(it, type)
    if (graphic != null) it.graphic = graphic
}

fun ButtonBar.button(
    observable: ObservableValue<String>,
    type: ButtonBar.ButtonData? = null,
    graphic: Node? = null,
    op: Button.() -> Unit = {}
): Button = Button().attachTo(this, op) {
    it.textProperty().bind(observable)
    if (type != null) ButtonBar.setButtonData(it, type)
    if (graphic != null) it.graphic = graphic
}


fun EventTarget.menubutton(
    text: String = "",
    graphic: Node? = null,
    op: MenuButton.() -> Unit = {}
): MenuButton = MenuButton(text).attachTo(this, op) {
    if (graphic != null) it.graphic = graphic
}

fun EventTarget.menubutton(
    observable: ObservableValue<String>,
    graphic: Node? = null,
    op: MenuButton.() -> Unit = {}
): MenuButton = MenuButton().attachTo(this, op) {
    it.textProperty().bind(observable)
    if (graphic != null) it.graphic = graphic
}


/**
 * Create a [ToggleGroup] inside the current or given toggle group. The optional value parameter will be matched against
 * the extension property `selectedValueProperty()` on Toggle Group. If the [ToggleGroup.selectedValueProperty] is used,
 * it's value will be updated to reflect the value for this radio button when it's selected.
 *
 * Likewise, if the `selectedValueProperty` of the ToggleGroup is updated to a value that matches the value for this
 * ToggleGroup, it will be automatically selected.
 */
fun EventTarget.togglebutton(
    text: String? = null,
    group: ToggleGroup? = getToggleGroup(),
    selectFirst: Boolean = true,
    value: Any? = null,
    op: ToggleButton.() -> Unit = {}
): ToggleButton = ToggleButton().attachTo(this, op) {
    it.properties[TOGGLE_GROUP_VALUE_PROPERTY] = value ?: text
    if (text != null || value != null) it.text = text ?: value.toString()
    if (group != null) it.toggleGroup = group
    if (it.toggleGroup?.selectedToggle == null && selectFirst) it.isSelected = true
}

fun EventTarget.togglebutton(
    observable: ObservableValue<String>,
    group: ToggleGroup? = getToggleGroup(),
    selectFirst: Boolean = true,
    value: Any? = null,
    op: ToggleButton.() -> Unit = {}
): ToggleButton = ToggleButton().attachTo(this, op) {
    it.properties[TOGGLE_GROUP_VALUE_PROPERTY] = value ?: observable
    it.textProperty().bind(observable)
    if (group != null) it.toggleGroup = group
    if (it.toggleGroup?.selectedToggle == null && selectFirst) it.isSelected = true
}


/**
 * Create a [RadioButton] inside the current or given toggle group. The optional value parameter will be matched against
 * the extension property `selectedValueProperty()` on Toggle Group. If the [ToggleGroup.selectedValueProperty] is used,
 * it's value will be updated to reflect the value for this radio button when it's selected.
 *
 * Likewise, if the `selectedValueProperty` of the ToggleGroup is updated to a value that matches the value for this
 * RadioButton, it will be automatically selected.
 */
fun EventTarget.radiobutton(
    text: String? = null,
    group: ToggleGroup? = getToggleGroup(),
    value: Any? = null,
    op: RadioButton.() -> Unit = {}
): RadioButton = RadioButton().attachTo(this, op) {
    it.properties[TOGGLE_GROUP_VALUE_PROPERTY] = value ?: text
    if (text != null || value != null) it.text = text ?: value.toString()
    if (group != null) it.toggleGroup = group
}

fun EventTarget.radiobutton(
    observable: ObservableValue<String>,
    group: ToggleGroup? = getToggleGroup(),
    value: Any? = null,
    op: RadioButton.() -> Unit = {}
): RadioButton = RadioButton().attachTo(this, op) {
    it.properties[TOGGLE_GROUP_VALUE_PROPERTY] = value ?: observable
    it.textProperty().bind(observable)
    if (group != null) it.toggleGroup = group
}


// ================================================================
// ToggleGroup

fun Node.togglegroup(op: ToggleGroup.() -> Unit = {}): ToggleGroup = ToggleGroup().also { properties[TOGGLE_GROUP_PROPERTY] = it }.also(op)

/**
 * Bind the selectedValueProperty of this toggle group to the given property. Passing in a writable value
 * will result in a bidirectional binding, while passing in a read only value will result in a unidirectional binding.
 *
 * If the toggles are configured with the value parameter (@see #togglebutton and #radiogroup), the corresponding
 * button will be selected when the value is changed. Likewise, if the selected toggle is changed,
 * the property value will be updated if it is writable.
 */
fun <T> ToggleGroup.bind(observable: ObservableValue<T>) {
    selectedValueProperty<T>().apply {
        if (observable is Property<T>) bindBidirectional(observable) else bind(observable)
    }
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
fun <T> ToggleGroup.selectedValueProperty(): ObjectProperty<T> = properties.getOrPut(SELECTED_VALUE_PROPERTY) {
    SimpleObjectProperty<T>().apply {
        selectedToggleProperty().onChange {
            value = it?.properties?.get(TOGGLE_GROUP_VALUE_PROPERTY) as T?
        }
        onChange { selectedValue ->
            selectToggle(toggles.find { it.properties[TOGGLE_GROUP_VALUE_PROPERTY] == selectedValue })
        }
    }
} as ObjectProperty<T>

fun ToggleButton.whenSelected(op: () -> Unit) {
    selectedProperty().onChange { if (it) op() }
}


// ================================================================
// Label

fun EventTarget.label(
    text: String? = "",
    graphic: Node? = null,
    op: Label.() -> Unit = {}
): Label = Label(text).attachTo(this, op) { if (graphic != null) it.graphic = graphic }

inline fun <reified T> EventTarget.label(
    observable: ObservableValue<T>,
    graphicProperty: ObservableValue<Node>? = null, // FIXME Inconsistent with all the other functions. There is no other function with ObservableValue<Node>
    converter: StringConverter<in T>? = null,
    noinline op: Label.() -> Unit = {}
): Label = label().apply {
    if (converter == null) {
        if (T::class == String::class) {
            @Suppress("UNCHECKED_CAST")
            textProperty().bind(observable as ObservableValue<String>)
        } else {
            textProperty().bind(observable.stringBinding { it?.toString() })
        }
    } else {
        textProperty().bind(observable.stringBinding { converter.toString(it) })
    }
    if (graphic != null) graphicProperty().bind(graphicProperty)
    op(this)
}


// ================================================================
// WebView

fun EventTarget.webview(op: WebView.() -> Unit = {}): WebView = WebView().attachTo(this, op)


// ================================================================
// Hyperlink

fun EventTarget.hyperlink(
    text: String = "",
    graphic: Node? = null,
    op: Hyperlink.() -> Unit = {}
): Hyperlink = Hyperlink(text).attachTo(this, op) {
    if (graphic != null) it.graphic = graphic
}

fun EventTarget.hyperlink(
    observable: ObservableValue<String>,
    graphic: Node? = null,
    op: Hyperlink.() -> Unit = {}
): Hyperlink = Hyperlink().attachTo(this, op) {
    it.bind(observable)
    if (graphic != null) it.graphic = graphic
}


// ================================================================
// HTMLEditor

fun EventTarget.htmleditor(
    html: String? = null,
    op: HTMLEditor.() -> Unit = {}
): HTMLEditor = HTMLEditor().attachTo(this, op) { if (html != null) it.htmlText = html }


// ================================================================
// ColorPicker

fun EventTarget.colorpicker(
    color: Color? = null,
    mode: ColorPickerMode = ColorPickerMode.Button,
    op: ColorPicker.() -> Unit = {}
): ColorPicker = ColorPicker().attachTo(this, op) {
    if (mode == ColorPickerMode.MenuButton) it.addClass(ColorPicker.STYLE_CLASS_BUTTON)
    else if (mode == ColorPickerMode.SplitMenuButton) it.addClass(ColorPicker.STYLE_CLASS_SPLIT_BUTTON)
    if (color != null) it.value = color
}

enum class ColorPickerMode { Button, MenuButton, SplitMenuButton }


// ================================================================
// MenuBar

fun EventTarget.menubar(op: MenuBar.() -> Unit = {}): MenuBar = MenuBar().attachTo(this, op)


// ================================================================
// ImageView

fun EventTarget.imageview(
    url: String? = null,
    lazyload: Boolean = true,
    op: ImageView.() -> Unit = {}
): ImageView = ImageView().attachTo(this, op) { if (url != null) it.image = Image(url, lazyload) }

fun EventTarget.imageview(
    observableUrl: ObservableValue<String>,
    lazyload: Boolean = true,
    op: ImageView.() -> Unit = {}
): ImageView = ImageView().attachTo(this, op) {
    it.imageProperty().bind(objectBinding(observableUrl) { value?.let { url -> Image(url, lazyload) } })
}

fun EventTarget.imageview(
    image: Image,
    op: ImageView.() -> Unit = {}
): ImageView = ImageView(image).attachTo(this, op)

fun EventTarget.imageview(
    observableImage: ObservableValue<Image?>,
    op: ImageView.() -> Unit = {}
): ImageView = ImageView().attachTo(this, op) { it.imageProperty().bind(observableImage) }


// ================================================================
// Utility Functions

/** Listen to changes and update the value of the property if the given mutator results in a different value. */
fun <T : Any?> Property<T>.mutateOnChange(mutator: (T?) -> T?) {
    onChange {
        val changed = mutator(value)
        if (changed != value) value = changed
    }
}

/** Remove leading or trailing whitespace from a Text Input Control. */
fun TextInputControl.trimWhitespace() {
    focusedProperty().onChange { focused ->
        if (!focused && text != null) text = text.trim()
    }
}

/** Remove any whitespace from a Text Input Control. */
fun TextInputControl.stripWhitespace(): Unit =
    textProperty().mutateOnChange { it?.replace(Regex("\\s*"), "") }

/** Remove any non integer values from a Text Input Control. */
fun TextInputControl.stripNonInteger(): Unit =
    textProperty().mutateOnChange { it?.replace(Regex("[^0-9]"), "") }

@Deprecated("Use the variant with chars instead")
fun TextInputControl.stripNonNumeric(vararg allowedChars: String = arrayOf(".", ",")): Unit = stripNonNumeric(*allowedChars.joinToString("").toCharArray())

/**
 * Remove any non numeric values from a Text Input Control.
 *
 * Additional [allowed chars][allowedChars] can be specified.
 */
fun TextInputControl.stripNonNumeric(vararg allowedChars: Char = charArrayOf('.', ',')) {
    textProperty().mutateOnChange { text ->
        val chars = allowedChars.asSequence().filter { it !in '0'..'9' }.distinct().sorted().joinToString("")
        text?.replace(Regex("[^0-9$chars]"), "")
    }
}

fun MenuItem.action(op: () -> Unit): Unit = setOnAction { op() }
fun TextField.action(op: () -> Unit): Unit = setOnAction { op() }
fun ButtonBase.action(op: () -> Unit): Unit = setOnAction { op() }
fun ChoiceBox<*>.action(op: () -> Unit): Unit = setOnAction { op() }
