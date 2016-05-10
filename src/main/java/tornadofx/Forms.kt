package tornadofx

import javafx.beans.DefaultProperty
import javafx.beans.binding.Bindings.createObjectBinding
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.css.PseudoClass
import javafx.geometry.Orientation
import javafx.geometry.Orientation.HORIZONTAL
import javafx.geometry.Orientation.VERTICAL
import javafx.scene.Node
import javafx.scene.control.Control
import javafx.scene.control.Label
import javafx.scene.control.Skin
import javafx.scene.control.SkinBase
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority.SOMETIMES
import javafx.scene.layout.VBox
import java.util.concurrent.Callable

fun Pane.form(op: (Form.() -> Unit)? = null) = opcr(this, Form(), op)

class Form : VBox() {

    init {
        addClass(Stylesheet.form)
    }

    internal val labelContainerWidth: Double
        get() = fieldsets.flatMap { it.fields }.map { it.labelContainer }.map { f -> f.prefWidth(-1.0) }.max() ?: 0.0

    val fieldsets: List<Fieldset>
        get() = children.filter { it is Fieldset }.map { it as Fieldset }

    override fun getUserAgentStylesheet() =
        Form::class.java.getResource("form.css").toExternalForm()

    fun fieldset(text: String? = null, icon: Node? = null, labelPosition: Orientation? = null, op: (Fieldset.() -> Unit)? = null): Fieldset {
        val fieldset = Fieldset(text ?: "")
        children.add(fieldset)
        if (labelPosition != null) fieldset.labelPosition = labelPosition
        if (icon != null) fieldset.icon = icon
        op?.invoke(fieldset)
        return fieldset
    }

}

@DefaultProperty("children")
class Fieldset(name: String? = null) : VBox() {
    var text by property<String>()
    fun textProperty() = getProperty(Fieldset::text)

    var inputGrow by property(SOMETIMES)
    fun inputGrowProperty() = getProperty(Fieldset::inputGrow)

    var labelPosition by property<Orientation>()
    fun labelPositionProperty() = getProperty(Fieldset::labelPosition)

    var wrapWidth by property<Double>()
    fun wrapWidthProperty() = getProperty(Fieldset::wrapWidth)

    var icon by property<Node>()
    fun iconProperty() = getProperty(Fieldset::icon)

    var legend by property<Label?>()
    fun legendProperty() = getProperty(Fieldset::legend)

    fun field(text: String? = null, vararg inputs: Node, op: (Field.() -> Unit)? = null): Field {
        val field = Field(text ?: "", *inputs)
        children.add(field)
        op?.invoke(field)
        return field
    }

    init {
        addClass(Stylesheet.fieldset)

        syncOrientationState()

        // Add legend label when text is populated
        textProperty().addListener { observable, oldValue, newValue -> if (newValue != null) addLegend() }

        // Add legend when icon is populated
        iconProperty().addListener { observable1, oldValue1, newValue -> if (newValue != null) addLegend() }

        // Make sure input children gets the configured HBox.hgrow property
        children.addListener(ListChangeListener { c ->
            while (c.next()) {
                if (c.wasAdded()) {
                    c.addedSubList.filter { it is Field }.map { it as Field }.forEach { added ->

                        // Configure hgrow for current children
                        added.inputContainer.children.forEach { this.configureHgrow(it) }

                        // Add listener to support inputs added later
                        added.inputContainer.children.addListener(ListChangeListener { while (it.next()) if (it.wasAdded()) it.addedSubList.forEach { this.configureHgrow(it) } })
                    }
                }
            }
        })

        // Change HGrow for unconfigured children when inputGrow changes
        inputGrowProperty().addListener { observable, oldValue, newValue ->
            children.filter { it is Field }.map { it as Field }.forEach {
                it.inputContainer.children.forEach { this.configureHgrow(it) }
            }
        }

        // Default
        labelPosition = HORIZONTAL
        if (name != null) text = name
    }

    private fun syncOrientationState() {
        // Apply pseudo classes when orientation changes
        labelPositionProperty().addListener { observable, oldValue, newValue ->
            if (newValue == HORIZONTAL) {
                pseudoClassStateChanged(VERTICAL_PSEUDOCLASS_STATE, false)
                pseudoClassStateChanged(HORIZONTAL_PSEUDOCLASS_STATE, true)
            } else {
                pseudoClassStateChanged(HORIZONTAL_PSEUDOCLASS_STATE, false)
                pseudoClassStateChanged(VERTICAL_PSEUDOCLASS_STATE, true)
            }
        }

        // Setup listeneres for wrapping
        wrapWidthProperty().addListener { observable, oldValue, newValue ->
            val responsiveOrientation = createObjectBinding<Orientation>(Callable { if (width < newValue) VERTICAL else HORIZONTAL }, widthProperty())

            if (labelPositionProperty().isBound)
                labelPositionProperty().unbind()

            labelPositionProperty().bind(responsiveOrientation)
        }
    }

    private fun addLegend() {
        if (legend == null) {
            legend = Label()
            legend!!.textProperty().bind(textProperty())
            legend!!.addClass(Stylesheet.legend)
            children.add(0, legend)
        }

        legend!!.graphic = icon
    }

    private fun configureHgrow(input: Node) {
        HBox.setHgrow(input, inputGrow)
    }

    val form: Form get() = parent as Form

    internal val fields: List<Field>
        get() = children.filter { it is Field }.map { it as Field }

    companion object {
        private val HORIZONTAL_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("horizontal")
        private val VERTICAL_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("vertical")
    }
}

@DefaultProperty("inputs")
class Field() : Control() {
    var text by property<String>()
    fun textProperty() = getProperty(Field::text)

    private val label = Label()
    val labelContainer = LabelContainer(label)
    val inputContainer = InputContainer()
    var inputs: ObservableList<Node>? = null

    constructor(text: String, vararg inputs: Node) : this() {
        this@Field.text = text
        inputContainer.children.addAll(*inputs)
    }

    init {
        isFocusTraversable = false
        addClass(Stylesheet.field)
        label.textProperty().bind(textProperty())
        inputs = inputContainer.children
        children.addAll(labelContainer, inputContainer)
    }

    val fieldset: Fieldset
        get() = parent as Fieldset

    override fun createDefaultSkin() = FieldSkin(this)

    inner class LabelContainer(label: Label) : HBox() {
        init {
            children.add(label)
            addClass(Stylesheet.labelContainer)
        }
    }

    inner class InputContainer : HBox() {
        init {
            addClass(Stylesheet.inputContainer)
        }
    }

}

class FieldSkin(control: Field) : SkinBase<Field>(control) {

    override fun computePrefWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        val field = skinnable
        val fieldset = field.fieldset
        val labelHasContent = field.text.isNotBlank()

        val labelWidth = if (labelHasContent) field.fieldset.form.labelContainerWidth else 0.0
        val inputWidth = field.inputContainer.prefWidth(height)

        if (fieldset.labelPosition == HORIZONTAL)
            return Math.max(labelWidth, inputWidth) + leftInset + rightInset

        return labelWidth + inputWidth + leftInset + rightInset
    }

    override fun computeMinHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return computePrefHeight(width, topInset, rightInset, bottomInset, leftInset)
    }

    override fun computePrefHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        val field = skinnable
        val fieldset = field.fieldset
        val labelHasContent = field.text.isNotBlank()

        val labelHeight = if (labelHasContent) field.labelContainer.prefHeight(-1.0) else 0.0
        val inputHeight = field.inputContainer.prefHeight(-1.0)

        if (fieldset.labelPosition == HORIZONTAL)
            return Math.max(labelHeight, inputHeight) + topInset + bottomInset

        return labelHeight + inputHeight + topInset + bottomInset
    }

    override fun layoutChildren(contentX: Double, contentY: Double, contentWidth: Double, contentHeight: Double) {
        val field = skinnable
        val fieldset = field.fieldset
        val labelHasContent = field.text.isNotBlank()

        val labelWidth = field.fieldset.form.labelContainerWidth
        if (fieldset.labelPosition == HORIZONTAL) {
            if (labelHasContent) {
                field.labelContainer.resizeRelocate(contentX, contentY, Math.min(labelWidth, contentWidth), contentHeight)

                val inputX = contentX + labelWidth
                val inputWidth = contentWidth - labelWidth

                field.inputContainer.resizeRelocate(inputX, contentY, inputWidth, contentHeight)
            } else {
                field.inputContainer.resizeRelocate(contentX, contentY, contentWidth, contentHeight)
            }
        } else {
            if (labelHasContent) {
                val labelPrefHeight = field.labelContainer.prefHeight(-1.0)
                val labelHeight = Math.min(labelPrefHeight, contentHeight)

                field.labelContainer.resizeRelocate(contentX, contentY, Math.min(labelWidth, contentWidth), labelHeight)

                val restHeight = labelHeight - contentHeight

                field.inputContainer.resizeRelocate(contentX, contentY + labelHeight, contentWidth, restHeight)
            } else {
                field.inputContainer.resizeRelocate(contentX, contentY, contentWidth, contentHeight)
            }
        }
    }
}