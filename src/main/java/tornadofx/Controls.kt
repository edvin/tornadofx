@file:Suppress("UNCHECKED_CAST")

package tornadofx

import javafx.beans.binding.BooleanExpression
import javafx.beans.property.ObjectProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent
import javafx.event.EventTarget
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.scene.web.HTMLEditor
import javafx.scene.web.WebView
import javafx.util.StringConverter
import java.time.LocalDate
import kotlin.reflect.KClass

fun EventTarget.webview(op: (WebView.() -> Unit)? = null) = opcr(this, WebView(), op)

enum class ColorPickerMode { Button, MenuButton, SplitMenuButton }

fun EventTarget.colorpicker(color: Color? = null, mode: ColorPickerMode = ColorPickerMode.Button, op: (ColorPicker.() -> Unit)? = null): ColorPicker {
    val picker = ColorPicker()
    if (mode == ColorPickerMode.MenuButton) picker.addClass(ColorPicker.STYLE_CLASS_BUTTON)
    else if (mode == ColorPickerMode.SplitMenuButton) picker.addClass(ColorPicker.STYLE_CLASS_SPLIT_BUTTON)
    if (color != null) picker.value = color
    return opcr(this, picker, op)
}

fun EventTarget.tabpane(op: (TabPane.() -> Unit)? = null) = opcr(this, TabPane(), op)

fun EventTarget.textflow(op: (TextFlow.() -> Unit)? = null) = opcr(this, TextFlow(), op)

fun EventTarget.text(op: (Text.() -> Unit)? = null) = opcr(this, Text(), op)

fun <T : Node> TabPane.tab(text: String, content: T, op: (T.() -> Unit)? = null): Tab {
    val tab = Tab(text, content)
    tabs.add(tab)
    if (op != null) op(content)
    return tab
}

@Deprecated("Use the tab builder that extracts the closeable state from UIComponent.closeable instead", ReplaceWith("add(uiComponent)"))
fun TabPane.tab(uiComponent: UIComponent, closable: Boolean = true, op: (Tab.() -> Unit)? = null): Tab {
    val tab = Tab()
    tab.isClosable = closable
    tab.textProperty().bind(uiComponent.titleProperty)
    tab.content = uiComponent.root
    tabs.add(tab)
    op?.invoke(tab)
    return tab
}

fun TabPane.tab(uiComponent: KClass<out UIComponent>, op: (Tab.() -> Unit)? = null) = tab(find(uiComponent), op)

fun TabPane.tab(uiComponent: UIComponent, op: (Tab.() -> Unit)? = null): Tab {
    add(uiComponent.root)
    val tab = tabs.last()
    op?.invoke(tab)
    return tab
}

val TabPane.savable: BooleanExpression get() {
    val savable = SimpleBooleanProperty(true)

    fun updateState() {
        savable.cleanBind(contentUiComponent<UIComponent>()?.savable ?: SimpleBooleanProperty(Workspace.defaultSavable))
    }

    val contentChangeListener = ChangeListener<Node?> { observable, oldValue, newValue -> updateState() }

    updateState()

    selectionModel.selectedItem?.contentProperty()?.addListener(contentChangeListener)
    selectionModel.selectedItemProperty().addListener { observable, oldTab, newTab ->
        updateState()
        oldTab?.contentProperty()?.removeListener(contentChangeListener)
        newTab?.contentProperty()?.addListener(contentChangeListener)
    }

    return savable
}

val TabPane.deletable: BooleanExpression get() {
    val deletable = SimpleBooleanProperty(true)

    fun updateState() {
        deletable.cleanBind(contentUiComponent<UIComponent>()?.deletable ?: SimpleBooleanProperty(Workspace.defaultDeletable))
    }

    val contentChangeListener = ChangeListener<Node?> { observable, oldValue, newValue -> updateState() }

    updateState()

    selectionModel.selectedItem?.contentProperty()?.addListener(contentChangeListener)
    selectionModel.selectedItemProperty().addListener { observable, oldTab, newTab ->
        updateState()
        oldTab?.contentProperty()?.removeListener(contentChangeListener)
        newTab?.contentProperty()?.addListener(contentChangeListener)
    }

    return deletable
}

val TabPane.refreshable: BooleanExpression get() {
    val refreshable = SimpleBooleanProperty(true)

    fun updateState() {
        refreshable.cleanBind(contentUiComponent<UIComponent>()?.refreshable ?: SimpleBooleanProperty(Workspace.defaultRefreshable))
    }

    val contentChangeListener = ChangeListener<Node?> { observable, oldValue, newValue -> updateState() }

    updateState()

    selectionModel.selectedItem?.contentProperty()?.addListener(contentChangeListener)
    selectionModel.selectedItemProperty().addListener { observable, oldTab, newTab ->
        updateState()
        oldTab?.contentProperty()?.removeListener(contentChangeListener)
        newTab?.contentProperty()?.addListener(contentChangeListener)
    }

    return refreshable
}

inline fun <reified T : UIComponent> TabPane.contentUiComponent(): T? = selectionModel.selectedItem?.content?.uiComponent<T>()
fun TabPane.onDelete() = contentUiComponent<UIComponent>()?.onDelete()
fun TabPane.onSave() = contentUiComponent<UIComponent>()?.onSave()
fun TabPane.onRefresh() = contentUiComponent<UIComponent>()?.onRefresh()
fun TabPane.onNavigateBack() = contentUiComponent<UIComponent>()?.onNavigateBack() ?: true
fun TabPane.onNavigateForward() = contentUiComponent<UIComponent>()?.onNavigateForward() ?: true

fun TabPane.tab(text: String, op: (Tab.() -> Unit)? = null): Tab {
    val tab = Tab(text)
    tabs.add(tab)
    op?.invoke(tab)
    return tab
}

@Deprecated("No need to use the content{} wrapper anymore, just use a builder directly inside the Tab", ReplaceWith("no content{} wrapper"), DeprecationLevel.WARNING)
fun Tab.content(op: Pane.() -> Unit): Node {
    val fake = VBox()
    op(fake)
    content = if (fake.children.size == 1) fake.children.first() else fake
    return content
}

@Deprecated("Properties set on the fake node would be lost. Do not use this function.", ReplaceWith("Manually adding children"), DeprecationLevel.WARNING)
fun children(addTo: MutableList<Node>, op: Pane.() -> Unit) {
    val fake = Pane()
    op(fake)
    addTo.addAll(fake.children)
}


fun EventTarget.text(initialValue: String? = null, op: (Text.() -> Unit)? = null) = opcr(this, Text().apply { if (initialValue != null) text = initialValue }, op)
fun EventTarget.text(property: Property<String>, op: (Text.() -> Unit)? = null) = text().apply {
    bind(property)
    op?.invoke(this)
}

fun EventTarget.text(observable: ObservableValue<String>, op: (Text.() -> Unit)? = null) = text().apply {
    bind(observable)
    op?.invoke(this)
}

fun EventTarget.textfield(value: String? = null, op: (TextField.() -> Unit)? = null) = opcr(this, TextField().apply { if (value != null) text = value }, op)

fun EventTarget.textfield(property: ObservableValue<String>, op: (TextField.() -> Unit)? = null) = textfield().apply {
    bind(property)
    op?.invoke(this)
}

@JvmName("textfieldNumber")
fun EventTarget.textfield(property: ObservableValue<Number>, op: (TextField.() -> Unit)? = null) = textfield().apply {
    bind(property)
    op?.invoke(this)
}

fun EventTarget.passwordfield(value: String? = null, op: (PasswordField.() -> Unit)? = null) = opcr(this, PasswordField().apply { if (value != null) text = value }, op)
fun EventTarget.passwordfield(property: ObservableValue<String>, op: (PasswordField.() -> Unit)? = null) = passwordfield().apply {
    bind(property)
    op?.invoke(this)
}

fun <T> EventTarget.textfield(property: Property<T>, converter: StringConverter<T>, op: (TextField.() -> Unit)? = null) = textfield().apply {
    textProperty().bindBidirectional(property, converter)
    ViewModel.register(textProperty(), property)
    op?.invoke(this)
}

fun EventTarget.datepicker(op: (DatePicker.() -> Unit)? = null) = opcr(this, DatePicker(), op)
fun EventTarget.datepicker(property: Property<LocalDate>, op: (DatePicker.() -> Unit)? = null) = datepicker().apply {
    bind(property)
    op?.invoke(this)
}

fun EventTarget.textarea(value: String? = null, op: (TextArea.() -> Unit)? = null) = opcr(this, TextArea().apply { if (value != null) text = value }, op)
fun EventTarget.textarea(property: ObservableValue<String>, op: (TextArea.() -> Unit)? = null) = textarea().apply {
    bind(property)
    op?.invoke(this)
}

fun <T> EventTarget.textarea(property: Property<T>, converter: StringConverter<T>, op: (TextArea.() -> Unit)? = null) = textarea().apply {
    textProperty().bindBidirectional(property, converter)
    ViewModel.register(textProperty(), property)
    op?.invoke(this)
}

fun EventTarget.buttonbar(buttonOrder: String? = null, op: (ButtonBar.() -> Unit)): ButtonBar {
    val bar = ButtonBar()
    if (buttonOrder != null) bar.buttonOrder = buttonOrder
    return opcr(this, bar, op)
}

fun EventTarget.htmleditor(html: String? = null, op: (HTMLEditor.() -> Unit)? = null) = opcr(this, HTMLEditor().apply { if (html != null) htmlText = html }, op)

fun EventTarget.checkbox(text: String? = null, property: Property<Boolean>? = null, op: (CheckBox.() -> Unit)? = null) = opcr(this, CheckBox(text).apply {
    if (property != null) bind(property)
}, op)

fun EventTarget.progressindicator(op: (ProgressIndicator.() -> Unit)? = null) = opcr(this, ProgressIndicator(), op)
fun EventTarget.progressindicator(property: Property<Number>, op: (ProgressIndicator.() -> Unit)? = null) = progressindicator().apply {
    bind(property)
    op?.invoke(this)
}

fun EventTarget.progressbar(initialValue: Double? = null, op: (ProgressBar.() -> Unit)? = null) = opcr(this, ProgressBar().apply { if (initialValue != null) progress = initialValue }, op)
fun EventTarget.progressbar(property: ObservableValue<Number>, op: (ProgressBar.() -> Unit)? = null) = progressbar().apply {
    bind(property)
    op?.invoke(this)
}

fun EventTarget.slider(min: Double? = null, max: Double? = null, value: Double? = null, orientation: Orientation? = null, op: (Slider.() -> Unit)? = null) = opcr(this, Slider().apply {
    if (min != null) this.min = min
    if (max != null) this.max = max
    if (value != null) this.value = value
    if (orientation != null) this.orientation = orientation
}, op)

// Buttons
fun EventTarget.button(text: String = "", graphic: Node? = null, op: (Button.() -> Unit)? = null): Button {
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

fun ButtonBar.button(text: String = "", type: ButtonBar.ButtonData? = null, graphic: Node? = null, op: (Button.() -> Unit)? = null): Button {
    val button = Button(text)
    if (type != null)
        ButtonBar.setButtonData(button, type)
    if (graphic != null)
        button.graphic = graphic
    buttons.add(button)
    op?.invoke(button)
    return button
}

fun Node.togglegroup(op: (ToggleGroup.() -> Unit)? = null): ToggleGroup {
    val group = ToggleGroup()
    properties["tornadofx.togglegroup"] = group
    op?.invoke(group)
    return group
}

fun <T> ToggleGroup.bind(property: ObservableValue<T>) = with(selectedValueProperty<T>()) {
    if (property is Property<*>) bindBidirectional(property as Property<T>)
    else bind(property)
}

fun <T> ToggleGroup.selectedValueProperty(): ObjectProperty<T> =
        properties.getOrPut("tornadofx.selectedValueProperty") {
            val selectedValueProperty = SimpleObjectProperty<T>()
            selectedToggleProperty().onChange {
                selectedValueProperty.value = it?.properties?.get("tornadofx.toggleGroupValue") as T?
            }
            selectedValueProperty.onChange { selectedValue ->
                selectToggle(toggles.find { it.properties["tornadofx.toggleGroupValue"] == selectedValue })
            }
            selectedValueProperty
        } as ObjectProperty<T>

fun Node.togglebutton(text: String? = null, group: ToggleGroup? = getToggleGroup(), selectFirst: Boolean = true, value: Any? = null, op: (ToggleButton.() -> Unit)? = null) =
        opcr(this, ToggleButton().apply {
            this.text = if (value != null && text == null) value.toString() else text ?: ""
            properties["tornadofx.toggleGroupValue"] = value ?: text
            if (group != null) toggleGroup = group
            if (toggleGroup?.selectedToggle == null && selectFirst) isSelected = true
        }, op)

fun ToggleButton.whenSelected(op: () -> Unit) {
    selectedProperty().onChange { if (it) op() }
}

/**
 * Create a radiobutton inside the current or given toggle group. The optiona value parameter will be matched against
 * the extension property `selectedValueProperty()` on Toggle Group.
 */
fun Node.radiobutton(text: String? = null, group: ToggleGroup? = getToggleGroup(), value: Any? = null, op: (RadioButton.() -> Unit)? = null)
        = opcr(this, RadioButton().apply {
    this.text = if (value != null && text == null) value.toString() else text ?: ""
    properties["tornadofx.toggleGroupValue"] = value ?: text
    if (group != null) toggleGroup = group
}, op)

fun EventTarget.label(text: String = "", op: (Label.() -> Unit)? = null) = opcr(this, Label(text), op)
inline fun <reified T> EventTarget.label(observable: ObservableValue<T>, noinline op: (Label.() -> Unit)? = null) = label().apply {
    if (T::class == String::class) {
        @Suppress("UNCHECKED_CAST")
        textProperty().bind(observable as ObservableValue<String>)
    } else {
        textProperty().bind(observable.stringBinding { it?.toString() })
    }
    op?.invoke(this)
}

fun EventTarget.hyperlink(text: String = "", op: (Hyperlink.() -> Unit)? = null) = opcr(this, Hyperlink(text), op)
fun EventTarget.hyperlink(observable: ObservableValue<String>, op: (Hyperlink.() -> Unit)? = null) = hyperlink().apply {
    bind(observable)
    op?.invoke(this)
}

fun EventTarget.menubar(op: (MenuBar.() -> Unit)? = null) = opcr(this, MenuBar(), op)

fun EventTarget.imageview(url: String? = null, lazyload: Boolean = true, op: (ImageView.() -> Unit)? = null)
        = opcr(this, if (url == null) ImageView() else ImageView(Image(url, lazyload)), op)

fun EventTarget.imageview(image: Image, op: (ImageView.() -> Unit)? = null)
        = opcr(this, ImageView(image), op)

/**
 * Listen to changes and update the value of the property if the given mutator results in a different value
 */
fun <T : Any?> Property<T>.mutateOnChange(mutator: (T?) -> T?) = onChange {
    val changed = mutator(value)
    if (changed != value) value = changed
}

/**
 * Remove leading or trailing whitespace from a Text Input Control.
 */
fun TextInputControl.trimWhitespace() = focusedProperty().onChange { focused ->
    if (!focused && text != null) text = text.trim()
}

/**
 * Remove any whitespace from a Text Input Control.
 */
fun TextInputControl.stripWhitespace() = textProperty().mutateOnChange { it?.replace(Regex("\\s*"), "") }

/**
 * Remove any non integer values from a Text Input Control.
 */
fun TextInputControl.stripNonInteger() = textProperty().mutateOnChange { it?.replace(Regex("[^0-9]"), "") }

/**
 * Remove any non integer values from a Text Input Control.
 */
fun TextInputControl.stripNonNumeric(vararg allowedChars: String = arrayOf(".", ",")) = textProperty().mutateOnChange { it?.replace(Regex("[^0-9${allowedChars.joinToString("")}]"), "") }

fun ButtonBase.action(op: ActionEvent.() -> Unit) = setOnAction(op)
fun TextField.action(op: ActionEvent.() -> Unit) = setOnAction(op)
fun MenuItem.action(op: ActionEvent.() -> Unit) = setOnAction(op)