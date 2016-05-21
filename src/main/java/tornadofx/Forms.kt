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
import javafx.scene.control.Label
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

    fun fieldset(text: String? = null, icon: Node? = null, labelPosition: Orientation? = null, wrapWidth: Double? = null, op: (Fieldset.() -> Unit)? = null): Fieldset {
        val fieldset = Fieldset(text ?: "")
        if (wrapWidth != null) fieldset.wrapWidth = wrapWidth
        children.add(fieldset)
        if (labelPosition != null) fieldset.labelPosition = labelPosition
        if (icon != null) fieldset.icon = icon
        op?.invoke(fieldset)
        return fieldset
    }

}

@DefaultProperty("children")
class Fieldset(text: String? = null, labelPosition: Orientation = HORIZONTAL) : VBox() {
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

    fun field(text: String? = null, op: (Pane.() -> Unit)? = null): Field {
        val field = Field(text ?: "")
        children.add(field)
        op?.invoke(field.inputContainer)
        return field
    }

    init {
        addClass(Stylesheet.fieldset)

        // Apply pseudo classes when orientation changes
        syncOrientationState()

        // Add legend label when text is populated
        textProperty().addListener { observable, oldValue, newValue -> if (newValue != null) addLegend() }

        // Add legend when icon is populated
        iconProperty().addListener { observable1, oldValue1, newValue -> if (newValue != null) addLegend() }

        // Make sure input children gets the configured HBox.hgrow property
        syncHgrow()

        // Initial values
        this@Fieldset.labelPosition = labelPosition
        if (text != null) this@Fieldset.text = text
    }

    private fun syncHgrow() {
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
    }

    private fun syncOrientationState() {
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
            val responsiveOrientation = createObjectBinding<Orientation>(Callable {
                if (width < newValue) VERTICAL else HORIZONTAL
            }, widthProperty())

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
class Field(text: String? = null) : Pane() {
    var text by property(text)
    fun textProperty() = getProperty(Field::text)

    val label = Label()
    val labelContainer = HBox(label).apply { addClass(Stylesheet.labelContainer) }
    val inputContainer = HBox().apply { addClass(Stylesheet.inputContainer) }
    var inputs: ObservableList<Node>? = null

    init {
        isFocusTraversable = false
        addClass(Stylesheet.field)
        label.textProperty().bind(textProperty())
        inputs = inputContainer.children
        children.addAll(labelContainer, inputContainer)
    }

    val fieldset: Fieldset get() = parent as Fieldset

    override fun computePrefHeight(width: Double): Double {
        val labelHasContent = text.isNotBlank()

        val labelHeight = if (labelHasContent) labelContainer.prefHeight(-1.0) else 0.0
        val inputHeight = inputContainer.prefHeight(-1.0)

        val insets = insets

        if (fieldset.labelPosition == HORIZONTAL)
            return Math.max(labelHeight, inputHeight) + insets.top + insets.bottom

        return labelHeight + inputHeight + insets.top + insets.bottom
    }

    override fun computePrefWidth(height: Double): Double {
        val fieldset = fieldset
        val labelHasContent = text.isNotBlank()

        val labelWidth = if (labelHasContent) fieldset.form.labelContainerWidth else 0.0
        val inputWidth = inputContainer.prefWidth(height)

        val insets = insets

        if (fieldset.labelPosition == HORIZONTAL)
            return Math.max(labelWidth, inputWidth) + insets.left + insets.right

        return labelWidth + inputWidth + insets.left + insets.right
    }

    override fun computeMinHeight(width: Double) = computePrefHeight(width)

    override fun layoutChildren() {
        val fieldset = fieldset
        val labelHasContent = text.isNotBlank()

        val insets = insets
        var contentX = insets.left
        val contentY = insets.top
        val contentWidth = width - insets.left - insets.right
        val contentHeight = height - insets.top - insets.bottom

        val labelWidth = Math.min(contentWidth, fieldset.form.labelContainerWidth)

        if (fieldset.labelPosition == HORIZONTAL) {
            if (labelHasContent) {
                labelContainer.resizeRelocate(contentX, contentY, labelWidth, contentHeight)

                val inputX = contentX + labelWidth
                val inputWidth = contentWidth - labelWidth

                inputContainer.resizeRelocate(inputX, contentY, inputWidth, contentHeight)
            } else {
                inputContainer.resizeRelocate(contentX, contentY, contentWidth, contentHeight)
            }
        } else {
            if (labelHasContent) {
                val labelPrefHeight = labelContainer.prefHeight(-1.0)
                val labelHeight = Math.min(labelPrefHeight, contentHeight)

                labelContainer.resizeRelocate(contentX, contentY, Math.min(labelWidth, contentWidth), labelHeight)

                val restHeight = labelHeight - contentHeight

                inputContainer.resizeRelocate(contentX, contentY + labelHeight, contentWidth, restHeight)
            } else {
                inputContainer.resizeRelocate(contentX, contentY, contentWidth, contentHeight)
            }
        }
    }

}
