@file:Suppress("UNCHECKED_CAST")

package tornadofx

import javafx.beans.binding.BooleanBinding
import javafx.beans.binding.BooleanExpression
import javafx.beans.property.ObjectProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableMap
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

internal val EventTarget.properties: ObservableMap<Any, Any>
    get() = when (this) {
        is Node -> properties
        is Tab -> properties
        is MenuItem -> properties
        else -> throw IllegalArgumentException("Don't know how to extract properties object from $this")
    }

var EventTarget.tagProperty: Property<Any?>
    get() = properties.getOrPut("tornadofx.value") {
        SimpleObjectProperty<Any?>()
    } as SimpleObjectProperty<Any?>
    set(value) {
        properties["tornadofx.value"] = value
    }

var EventTarget.tag: Any?
    get() = tagProperty.value
    set(value) {
        tagProperty.value = value
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

inline fun <reified  T: UIComponent> TabPane.tab(noinline op: (Tab.() -> Unit)? = null) = tab(T::class, op)
fun TabPane.tab(uiComponent: KClass<out UIComponent>, op: (Tab.() -> Unit)? = null) = tab(find(uiComponent), op)

fun TabPane.tab(uiComponent: UIComponent, op: (Tab.() -> Unit)? = null): Tab {
    add(uiComponent.root)
    val tab = tabs.last()
    op?.invoke(tab)
    return tab
}

fun <T : Node> Iterable<T>.contains(cmp: UIComponent) = any { it == cmp.root }

fun TabPane.contains(cmp: UIComponent) = tabs.map { it.content }.contains(cmp)

fun Tab.disableWhen(predicate: ObservableValue<Boolean>) = disableProperty().cleanBind(predicate)
fun Tab.enableWhen(predicate: ObservableValue<Boolean>) {
    val binding = if (predicate is BooleanBinding) predicate.not() else predicate.toBinding().not()
    disableProperty().cleanBind(binding)
}

fun Tab.visibleWhen(predicate: ObservableValue<Boolean>) {
    fun updateState() {
        if (predicate.value.not()) tabPane.tabs.remove(this)
        else if (this !in tabPane.tabs) tabPane.tabs.add(this)
    }
    updateState()
    predicate.onChange { updateState() }
}

val TabPane.savable: BooleanExpression
    get() {
        val savable = SimpleBooleanProperty(true)

        fun updateState() {
            savable.cleanBind(contentUiComponent<UIComponent>()?.savable ?: SimpleBooleanProperty(Workspace.defaultSavable))
        }

        val contentChangeListener = ChangeListener<Node?> { _, _, _ -> updateState() }

        updateState()

        selectionModel.selectedItem?.contentProperty()?.addListener(contentChangeListener)
        selectionModel.selectedItemProperty().addListener { observable, oldTab, newTab ->
            updateState()
            oldTab?.contentProperty()?.removeListener(contentChangeListener)
            newTab?.contentProperty()?.addListener(contentChangeListener)
        }

        return savable
    }

val TabPane.deletable: BooleanExpression
    get() {
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

val TabPane.refreshable: BooleanExpression
    get() {
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
fun TabPane.onCreate() = contentUiComponent<UIComponent>()?.onCreate()
fun TabPane.onRefresh() = contentUiComponent<UIComponent>()?.onRefresh()
fun TabPane.onNavigateBack() = contentUiComponent<UIComponent>()?.onNavigateBack() ?: true
fun TabPane.onNavigateForward() = contentUiComponent<UIComponent>()?.onNavigateForward() ?: true

fun TabPane.tab(text: String? = null, tag: Any? = null, op: (Tab.() -> Unit)? = null): Tab {
    val tab = Tab(text ?: tag?.toString())
    tab.tag = tag
    tabs.add(tab)
    op?.invoke(tab)
    return tab
}

fun Tab.whenSelected(op: () -> Unit) {
    selectedProperty().onChange { if (it) op() }
}

fun Tab.select(): Tab {
    tabPane.selectionModel.select(this)
    return this
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

fun EventTarget.button(text: ObservableValue<String>, graphic: Node? = null, op: (Button.() -> Unit)? = null): Button {
    val button = Button()
    button.textProperty().bind(text)
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

fun ToolBar.button(text: ObservableValue<String>, graphic: Node? = null, op: (Button.() -> Unit)? = null): Button {
    val button = Button()
    button.textProperty().bind(text)
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

fun ButtonBar.button(text: ObservableValue<String>, type: ButtonBar.ButtonData? = null, graphic: Node? = null, op: (Button.() -> Unit)? = null): Button {
    val button = Button()
    button.textProperty().bind(text)
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

/**
 * Bind the selectedValueProperty of this toggle group to the given property. Passing in a writeable value
 * will result in a bidirectional binding, while passing in a read only value will result in a unidirectional binding.
 *
 * If the toggles are configured with the value parameter (@see #togglebutton and #radiogroup), the corresponding
 * button will be selected when the value is changed. Likewise, if the selected toggle is changed,
 * the property value will be updated if it is writeable.
 */
fun <T> ToggleGroup.bind(property: ObservableValue<T>) = with(selectedValueProperty<T>()) {
    if (property is Property<*>) bindBidirectional(property as Property<T>)
    else bind(property)
}

/**
 * Generates a writable property that represents the selected value for this toggele group.
 * If the toggles are configured with a value (@see #togglebutton and #radiogroup) the corresponding
 * toggle will be selected when this value is changed. Likewise, if the toggle is changed by clicking
 * it, the value for the toggle will be written to this property.
 *
 * To bind to this property, use the #ToggleGroup.bind() function.
 */
fun <T> ToggleGroup.selectedValueProperty(): ObjectProperty<T> =
        properties.getOrPut("tornadofx.selectedValueProperty") {
            SimpleObjectProperty<T>().apply {
                selectedToggleProperty().onChange {
                    value = it?.properties?.get("tornadofx.toggleGroupValue") as T?
                }
                onChange { selectedValue ->
                    selectToggle(toggles.find { it.properties["tornadofx.toggleGroupValue"] == selectedValue })
                }
            }
        } as ObjectProperty<T>

/**
 * Create a togglebutton inside the current or given toggle group. The optional value parameter will be matched against
 * the extension property `selectedValueProperty()` on Toggle Group. If the #ToggleGroup.selectedValueProperty is used,
 * it's value will be updated to reflect the value for this radio button when it's selected.
 *
 * Likewise, if the `selectedValueProperty` of the ToggleGroup is updated to a value that matches the value for this
 * togglebutton, it will be automatically selected.
 */
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
 * Create a radiobutton inside the current or given toggle group. The optional value parameter will be matched against
 * the extension property `selectedValueProperty()` on Toggle Group. If the #ToggleGroup.selectedValueProperty is used,
 * it's value will be updated to reflect the value for this radio button when it's selected.
 *
 * Likewise, if the `selectedValueProperty` of the ToggleGroup is updated to a value that matches the value for this
 * radiobutton, it will be automatically selected.
 */
fun Node.radiobutton(text: String? = null, group: ToggleGroup? = getToggleGroup(), value: Any? = null, op: (RadioButton.() -> Unit)? = null)
        = opcr(this, RadioButton().apply {
    this.text = if (value != null && text == null) value.toString() else text ?: ""
    properties["tornadofx.toggleGroupValue"] = value ?: text
    if (group != null) toggleGroup = group
}, op)

fun EventTarget.label(text: String = "", graphic: Node? = null, op: (Label.() -> Unit)? = null): Label {
    val label = Label(text)
    if (graphic != null) label.graphic = graphic
    return opcr(this, label, op)
}

inline fun <reified T> EventTarget.label(observable: ObservableValue<T>, graphicProperty: ObjectProperty<Node>? = null, converter: StringConverter<in T>? = null, noinline op: (Label.() -> Unit)? = null) = label().apply {
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
    op?.invoke(this)
}

fun EventTarget.hyperlink(text: String = "", graphic: Node? = null, op: (Hyperlink.() -> Unit)? = null) = opcr(this, Hyperlink(text, graphic), op)
fun EventTarget.hyperlink(observable: ObservableValue<String>, graphic: Node? = null, op: (Hyperlink.() -> Unit)? = null) = hyperlink(graphic = graphic).apply {
    bind(observable)
    op?.invoke(this)
}

fun EventTarget.menubar(op: (MenuBar.() -> Unit)? = null) = opcr(this, MenuBar(), op)

fun EventTarget.imageview(url: String? = null, lazyload: Boolean = true, op: (ImageView.() -> Unit)? = null)
        = opcr(this, if (url == null) ImageView() else ImageView(Image(url, lazyload)), op)

fun EventTarget.imageview(url: ObservableValue<String>, lazyload: Boolean = true, op: (ImageView.() -> Unit)? = null)
        = opcr(this, ImageView().apply { imageProperty().bind(objectBinding(url) { value?.let { Image(it, lazyload) } }) }, op)

fun EventTarget.imageview(image: ObservableValue<Image?>, op: (ImageView.() -> Unit)? = null)
        = opcr(this, ImageView().apply { imageProperty().bind(image) }, op)

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

fun ChoiceBox<*>.action(op: () -> Unit) = setOnAction { op() }
fun ButtonBase.action(op: () -> Unit) = setOnAction { op() }
fun TextField.action(op: () -> Unit) = setOnAction { op() }
fun MenuItem.action(op: () -> Unit) = setOnAction { op() }
