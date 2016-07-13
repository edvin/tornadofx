package tornadofx

import com.sun.javafx.binding.BidirectionalBinding
import com.sun.javafx.binding.ExpressionHelper
import javafx.beans.property.*
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ObservableSet
import javafx.scene.Node
import javafx.scene.control.ListView
import javafx.scene.control.TableView
import javafx.scene.control.TextInputControl

open class ViewModel {
    val properties = FXCollections.observableHashMap<Property<*>, () -> Property<*>>()
    val dirtyProperties = FXCollections.observableArrayList<ObservableValue<*>>()
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
     * most property types via a consice syntax, see below for examples.
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

        val facade = when (T::class.javaPrimitiveType ?: T::class) {
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

        facade.addListener(dirtyListener)
        properties[facade] = op

        return facade as ResultType
    }

    inline fun <S : Property<T>, reified T : Any> property(noinline op: () -> Property<T>) = PropertyDelegate(bind(op))

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

    fun isDirty(): Boolean = dirtyStateProperty.value

    fun validate(): Boolean = validationContext.validate()

    fun commit(): Boolean {
        if (!validate()) return false

        properties.forEach {
            it.value.invoke().value = it.key.value
        }
        clearDirtyState()
        return true
    }

    fun rollback() {
        properties.forEach { it.key.value = it.value.invoke().value }
        clearDirtyState()
    }

    fun rebind() {
        for ((facade, propExtractor) in properties)
            facade.value = propExtractor().value
        clearDirtyState()
    }

    inline fun <reified T> addValidator(
            node: Node,
            property: ObservableValue<T>,
            trigger: ValidationTrigger = ValidationTrigger.OnChangeImmediate,
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
 * `model.isDirty { property }
 *
 */
fun <V : ViewModel, T> V.isDirty(op: V.() -> Property<T>) = isDirty(op())

fun <V : ViewModel, T> V.isNotDirty(op: V.() -> Property<T>) = isNotDirty(op())

/**
 * Listen to changes in the given observable and call the op with the new value on change.
 * After each change the rebind() function is called.
 */
fun <V : ViewModel, T> V.rebindOnChange(observable: ObservableValue<T>, op: V.(T?) -> Unit) {
    observable.addListener { observableValue, oldValue, newValue ->
        op(newValue)
        rebind()
    }
}

fun <V : ViewModel, T> V.rebindOnChange(tableview: TableView<T>, op: V.(T?) -> Unit)
        = rebindOnChange(tableview.selectionModel.selectedItemProperty(), op)

fun <V : ViewModel, T> V.rebindOnChange(listview: ListView<T>, op: V.(T?) -> Unit)
        = rebindOnChange(listview.selectionModel.selectedItemProperty(), op)

fun <T : ViewModel> T.rebind(op: (T.() -> Unit)) {
    op.invoke(this)
    rebind()
}

/**
 * Add the given validator to a property that recides inside a ViewModel. The supplied node will be
 * decorated by the current decorationProvider for this context inside the ViewModel of the property
 * if validation fails.
 *
 * The validator function is executed in the scope of this ValidationContex to give
 * access to other fields and shortcuts like the error and warning functions.
 *
 * The validation trigger decides when the validation is applied. ValidationTrigger.OnBlur
 * tracks focus on the supplied node while OnChange tracks changes to the property itself.
 */
inline fun <reified T> Property<T>.addValidator(
        node: Node,
        trigger: ValidationTrigger = ValidationTrigger.OnChangeImmediate,
        noinline validator: ValidationContext.(T?) -> ValidationMessage?) {

    if (bean is ViewModel) {
        val model = bean as ViewModel
        model.addValidator(node, this, trigger, validator)
    } else {
        throw IllegalArgumentException("The addValidator extension on Property can only be used on properties inside a ViewModel. Use validator.addValidator() instead.")
    }
}

/**
 * Add a validator to a TextInputControl that is already bound to a model property.
 * Trying to bind to a Control that is not bound to a model property will result in an exception.
 */
@Suppress("UNCHECKED_CAST")
fun TextInputControl.addValidator(
        trigger: ValidationTrigger = ValidationTrigger.OnChangeImmediate,
        validator: ValidationContext.(String?) -> ValidationMessage?) {

    val stringProperty = textProperty()
    val field = stringProperty.javaClass.getDeclaredField("helper")
    field.isAccessible = true
    val helper = field.get(stringProperty) as ExpressionHelper<String>

    val field2 = helper.javaClass.getDeclaredField("changeListeners")
    field2.isAccessible = true
    val bindings = field2.get(helper) as Array<*>
    val binding = bindings[0] as BidirectionalBinding<String>

    val field3 = binding.javaClass.getDeclaredMethod("getProperty2")
    field3.isAccessible = true
    val prop = field3.invoke(binding) as Property<String>

    if (prop is Property<*> && prop.bean is ViewModel) {
        val model = prop.bean as ViewModel
        model.addValidator(this, textProperty(), trigger, validator)
    } else {
        throw IllegalArgumentException("The addValidator extension on TextInputControl can only be used on inputs that are already bound bidirectionally to a property in a Viewmodel. Use validator.addValidator() instead.")
    }

}