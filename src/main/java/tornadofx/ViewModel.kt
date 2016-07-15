@file:Suppress("unused")

package tornadofx

import com.sun.javafx.binding.BidirectionalBinding
import com.sun.javafx.binding.ExpressionHelper
import javafx.beans.property.*
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.collections.ObservableSet
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.paint.Paint
import java.time.LocalDate

open class ViewModel {
    val properties: ObservableMap<Property<*>, () -> Property<*>> = FXCollections.observableHashMap<Property<*>, () -> Property<*>>()
    val dirtyProperties: ObservableList<ObservableValue<*>> = FXCollections.observableArrayList<ObservableValue<*>>()
    private val dirtyStateProperty = SimpleBooleanProperty(false)
    fun dirtyStateProperty() = dirtyStateProperty
    val validationContext = ValidationContext()

    /**
     * Wrap a JavaFX property and return the ViewModel facade for this property
     *
     * The value is returned in a lambda so that you can swap source objects
     * and call rebind to change the underlying source object in the mappings.
     *
     * You can bind a facade towards any kind of property as long as it can
     * be converted to a JavaFX property. TornadoFX provides a way to support
     * most property types via a concise syntax, see below for examples.
     * ```
     * class PersonViewModel(var person: Person) : ViewModel() {
     *     // Bind JavaFX property
     *     val name = bind { person.nameProperty() }
     *
     *     // Bind Kotlin var based property
     *     val name = bind { person.observable(Person::name)
     *
     *     // Bind Java POJO getter/setter
     *     val name = bind { person.observable(Person::getName, Person::setName)
     *
     *     // Bind Java POJO by property name (not type safe)
     *     val name = bind { person.observable("name") }
     * }
     * ```
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <PropertyType : Property<T>, reified T : Any, ResultType : PropertyType> bind(noinline op: () -> PropertyType): ResultType {
        val prop = op()
        val value = prop.value

        // Faster check where possible
        var facade: Any? = null

        when (prop) {
            is IntegerProperty -> facade = if (value != null) SimpleIntegerProperty(this, prop.name, value as Int) else SimpleIntegerProperty(this, prop.name)
            is DoubleProperty -> facade = if (value != null) SimpleDoubleProperty(this, prop.name, value as Double) else SimpleDoubleProperty(this, prop.name)
            is FloatProperty -> facade = if (value != null) SimpleFloatProperty(this, prop.name, value as Float) else SimpleFloatProperty(this, prop.name)
            is BooleanProperty -> facade = if (value != null) SimpleBooleanProperty(this, prop.name, value as Boolean) else SimpleBooleanProperty(this, prop.name)
        }

        if (facade == null) {
            facade = when (T::class.javaPrimitiveType ?: T::class) {
                Int::class.javaPrimitiveType -> if (value != null) SimpleIntegerProperty(this, prop.name, value as Int) else SimpleIntegerProperty(this, prop.name)
                Long::class.javaPrimitiveType -> if (value != null) SimpleLongProperty(this, prop.name, value as Long) else SimpleLongProperty(this, prop.name)
                Double::class.javaPrimitiveType -> if (value != null) SimpleDoubleProperty(this, prop.name, value as Double) else SimpleDoubleProperty(this, prop.name)
                Float::class.javaPrimitiveType -> if (value != null) SimpleFloatProperty(this, prop.name, value as Float) else SimpleFloatProperty(this, prop.name)
                Boolean::class.javaPrimitiveType -> if (value != null) SimpleBooleanProperty(this, prop.name, value as Boolean) else SimpleBooleanProperty(this, prop.name)
                String::class -> if (value != null) SimpleStringProperty(this, prop.name, value as String) else SimpleStringProperty(this, prop.name)
                is ObservableList<*> -> if (value != null) SimpleListProperty(this, prop.name, value as ObservableList<T>) else SimpleListProperty(this, prop.name)
                is ObservableSet<*> -> if (value != null) SimpleSetProperty(this, prop.name, value as ObservableSet<T>) else SimpleSetProperty(this, prop.name)
                is List<*> -> if (value != null) SimpleListProperty(this, prop.name, (value as List<T>).observable()) else SimpleListProperty(this, prop.name)
                is Set<*> -> if (value != null) SimpleSetProperty(this, prop.name, (value as Set<T>).observable()) else SimpleSetProperty(this, prop.name)
                else -> if (value != null) SimpleObjectProperty(this, prop.name, value) else SimpleObjectProperty(this, prop.name)
            }
        }

        (facade as Property<*>).addListener(dirtyListener)
        properties[facade] = op

        return facade as ResultType
    }

    inline fun <reified T : Any> property(noinline op: () -> Property<T>) = PropertyDelegate(bind(op))

    val dirtyListener: ChangeListener<Any> = ChangeListener { property, oldValue, newValue ->
        if (dirtyProperties.contains(property)) {
            val sourceValue = properties[property]!!.invoke().value
            if (sourceValue == newValue) dirtyProperties.remove(property)
        } else {
            dirtyProperties.add(property)
        }
        updateDirtyState()
    }

    private fun updateDirtyState() {
        val dirtyState = dirtyProperties.isNotEmpty()
        if (dirtyState != dirtyStateProperty.value) dirtyStateProperty.value = dirtyState
    }

    val isDirty: Boolean get() = dirtyStateProperty.value
    val isNotDirty: Boolean get() = !isDirty

    fun validate(focusFirstError: Boolean = true): Boolean = validationContext.validate(focusFirstError)

    /**
     * Perform validation and flush the values into the source object if validation passes.
     * @param force Force flush even if validation fails
     */
    fun commit(force: Boolean = false, focusFirstError: Boolean = true): Boolean {
        if (!validate(focusFirstError) && !force) return false

        for ((facade, propExtractor) in properties)
            propExtractor().value = facade.value

        clearDirtyState()
        return true
    }

    fun rollback() {
        for ((facade, propExtractor) in properties)
            facade.value = propExtractor().value
        clearDirtyState()
    }

    inline fun <reified T> addValidator(
            node: Node,
            property: ObservableValue<T>,
            trigger: ValidationTrigger = ValidationTrigger.OnChange(),
            noinline validator: ValidationContext.(T?) -> ValidationMessage?) {

        validationContext.addValidator(node, property, trigger, validator)
    }

    fun setDecorationProvider(decorationProvider: (ValidationMessage) -> Decorator?) {
        validationContext.decorationProvider = decorationProvider
    }

    val isValid: Boolean get() = validationContext.isValid

    /**
     * Extract the value of the corresponding source property
     */
    fun <T> backingValue(property: Property<T>) = properties[property]?.invoke()?.value

    fun <T> isDirty(property: Property<T>) = backingValue(property) != property.value
    fun <T> isNotDirty(property: Property<T>) = !isDirty(property)

    private fun clearDirtyState() {
        dirtyProperties.clear()
        updateDirtyState()
    }
}

/**
 * Check if a given property from the ViewModel is dirty. This is a shorthand form of:
 *
 * `model.isDirty(model.property)`
 *
 * With this you can write:
 *
 * `model.property.isDirty`
 *
 */
val <T> Property<T>.isDirty: Boolean get() = (bean as? ViewModel)?.isDirty(this) ?: false
val <T> Property<T>.isNotDirty: Boolean get() = !isDirty

/**
 * Listen to changes in the given observable and call the op with the new value on change.
 * After each change the viewmodel is rolled back to reflect the values in the new source object or objects.
 */
fun <V : ViewModel, T> V.rebindOnChange(observable: ObservableValue<T>, op: V.(T?) -> Unit) {
    observable.addListener { observableValue, oldValue, newValue ->
        op(newValue)
        rollback()
    }
}

fun <V : ViewModel, T> V.rebindOnTreeItemChange(observable: ObservableValue<TreeItem<T>>, op: V.(T?) -> Unit) {
    observable.addListener { observableValue, oldValue, newValue ->
        op(newValue?.value)
        rollback()
    }
}

fun <V : ViewModel, T> V.rebindOnChange(tableview: TableView<T>, op: V.(T?) -> Unit)
        = rebindOnChange(tableview.selectionModel.selectedItemProperty(), op)

fun <V : ViewModel, T> V.rebindOnChange(listview: ListView<T>, op: V.(T?) -> Unit)
        = rebindOnChange(listview.selectionModel.selectedItemProperty(), op)

fun <V : ViewModel, T> V.rebindOnChange(treeview: TreeView<T>, op: V.(T?) -> Unit)
        = rebindOnTreeItemChange(treeview.selectionModel.selectedItemProperty(), op)

fun <V : ViewModel, T> V.rebindOnChange(treetableview: TreeTableView<T>, op: V.(T?) -> Unit)
        = rebindOnTreeItemChange(treetableview.selectionModel.selectedItemProperty(), op)

fun <T : ViewModel> T.rebind(op: (T.() -> Unit)) {
    op()
    rollback()
}

/**
 * Add the given validator to a property that resides inside a ViewModel. The supplied node will be
 * decorated by the current decorationProvider for this context inside the ViewModel of the property
 * if validation fails.
 *
 * The validator function is executed in the scope of this ValidationContext to give
 * access to other fields and shortcuts like the error and warning functions.
 *
 * The validation trigger decides when the validation is applied. ValidationTrigger.OnBlur
 * tracks focus on the supplied node while OnChange tracks changes to the property itself.
 */
inline fun <reified T> Property<T>.addValidator(node: Node, trigger: ValidationTrigger = ValidationTrigger.OnChange(), noinline validator: ValidationContext.(T?) -> ValidationMessage?)
        = (bean as? ViewModel)?.addValidator(node, this, trigger, validator)
        ?: throw IllegalArgumentException("The addValidator extension on Property can only be used on properties inside a ViewModel. Use validator.addValidator() instead.")

fun TextInputControl.required(trigger: ValidationTrigger = ValidationTrigger.OnChange(), message: String? = "This field is required")
        = validator(trigger) { if (it.isNullOrBlank()) error(message) else null }

/**
 * Add a validator to a ComboBox that is already bound to a model property.
 */
inline fun <reified T> ComboBox<T>.validator(trigger: ValidationTrigger = ValidationTrigger.OnChange(), noinline validator: ValidationContext.(T?) -> ValidationMessage?)
        = validator(this, valueProperty(), trigger, validator)

/**
 * Add a validator to a ChoiceBox that is already bound to a model property.
 */
inline fun <reified T> ChoiceBox<T>.validator(trigger: ValidationTrigger = ValidationTrigger.OnChange(), noinline validator: ValidationContext.(T?) -> ValidationMessage?)
        = validator(this, valueProperty(), trigger, validator)

/**
 * Add a validator to a Spinner that is already bound to a model property.
 */
inline fun <reified T> Spinner<T>.validator(trigger: ValidationTrigger = ValidationTrigger.OnChange(), noinline validator: ValidationContext.(T?) -> ValidationMessage?)
        = validator(this, valueFactory.valueProperty(), trigger, validator)

/**
 * Add a validator to a TextInputControl that is already bound to a model property.
 */
fun TextInputControl.validator(trigger: ValidationTrigger = ValidationTrigger.OnChange(), validator: ValidationContext.(String?) -> ValidationMessage?)
        = validator(this, textProperty(), trigger, validator)

/**
 * Add a validator to a Labeled Control that is already bound to a model property.
 */
fun Labeled.validator(trigger: ValidationTrigger = ValidationTrigger.OnChange(), validator: ValidationContext.(String?) -> ValidationMessage?)
        = validator(this, textProperty(), trigger, validator)

/**
 * Add a validator to a ColorPicker that is already bound to a model property.
 */
fun ColorPicker.validator(trigger: ValidationTrigger = ValidationTrigger.OnChange(), validator: ValidationContext.(Paint?) -> ValidationMessage?)
        = validator(this, valueProperty(), trigger, validator)

/**
 * Add a validator to a DatePicker that is already bound to a model property.
 */
fun DatePicker.validator(trigger: ValidationTrigger = ValidationTrigger.OnChange(), validator: ValidationContext.(LocalDate?) -> ValidationMessage?)
        = validator(this, valueProperty(), trigger, validator)

/**
 * Add a validator to a CheckBox that is already bound to a model property.
 */
fun CheckBox.validator(trigger: ValidationTrigger = ValidationTrigger.OnChange(), validator: ValidationContext.(Boolean?) -> ValidationMessage?)
        = validator(this, selectedProperty(), trigger, validator)

/**
 * Add a validator to a RadioButton that is already bound to a model property.
 */
fun RadioButton.validator(trigger: ValidationTrigger = ValidationTrigger.OnChange(), validator: ValidationContext.(Boolean?) -> ValidationMessage?)
        = validator(this, selectedProperty(), trigger, validator)

/**
 * Add a validator to the given Control for the given model property.
 */
inline fun <reified T> validator(control: Control, property: Property<T>, trigger: ValidationTrigger, noinline validator: ValidationContext.(T?) -> ValidationMessage?)
        = property.viewModel?.addValidator(control, property, trigger, validator)
        ?: throw IllegalArgumentException("The addValidator extension on TextInputControl can only be used on inputs that are already bound bidirectionally to a property in a Viewmodel. Use validator.addValidator() instead or update the binding.")

/**
 * Extract the ViewModel from a bound ViewModel property
 */
@Suppress("UNCHECKED_CAST")
val Property<*>.viewModel: ViewModel? get() {
    val helperField = javaClass.findFieldByName("helper")
    if (helperField != null) {
        helperField.isAccessible = true
        val helper = helperField.get(this) as? ExpressionHelper<String>
        if (helper != null) {
            val clField = helper.javaClass.findFieldByName("changeListeners")
            if (clField != null) {
                clField.isAccessible = true
                val bindings = clField.get(helper)
                if (bindings is Array<*>) {
                    val binding = bindings.find { it is BidirectionalBinding<*> }

                    if (binding != null) {
                        val propField = binding.javaClass.findMethodByName("getProperty2")
                        if (propField != null) {
                            propField.isAccessible = true
                            val modelProp = propField.invoke(binding) as Property<String>

                            if (modelProp.bean is ViewModel)
                                return modelProp.bean as ViewModel
                        }
                    }
                }
            }
        }
    }

    return null
}