/*******************************************************************************
 * Copyright 2015 Alexander Casall, Manuel Mauky
 *  - Original Implementation in the mvvmFX-Project
 * Copyright 2016 Johannes Pfrang
 *  - Forked at mvvmFX version 1.5.0
 *  - Refactored to a Kotlin-native implementation and converted JavaDoc to KDoc
 *  - Added BeanSetPropertyField/FxSetPropertyField and removed identifiedFields
 *  - Added `allFields`/`allMemberFields`
 *  - Added `ViewSingleModel`/`ViewMultiModel`/`InsaneViewModel`
 *  - Added `valid-/different-/dirtyProperty()`

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tornadofx

// TODO: Add thread-safety?

import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableSet
import javafx.collections.SetChangeListener
import java.util.ArrayList
import java.util.Collections
import java.util.LinkedHashSet
import kotlin.reflect.*

/**
 * A helper class that can be used to simplify the mapping between the ViewModel and the Model for use cases where a
 * typical CRUD functionality is needed and there is no big difference between the structure of the model class and the
 * view.
 *
 *
 * A typical workflow would be:
 *
 *  * load an existing model instance from the backend and copy all values from the model to the properties of the
 * ViewModel
 *  * the user changes the values of the viewModel properties (via the UI). The state of the underlying model instance
 * may not be changed at this point in time!
 *  * when the user clicks an Apply button (and validation is successful) copy all values from the ViewModel fields
 * into the model instance.
 *
 *
 *
 * Additional requirements:
 *
 *  * click "reset" so that the old values are back in the UI. In this case all UI fields should get the current values
 * of the model
 *  * if we are creating a new model instance and the user clicks "reset" we want all UI fields to be reset to a
 * meaningful default value
 *

 *
 *

 * These requirements are quite common but there is a lot of code needed to copy between the model and the viewModel.
 * Additionally we have a tight coupling because every time the structure of the model changes (for example a field is
 * removed) we have several places in the viewModel that need to be adjusted.
 *
 *
 * This component can be used to simplify use cases like the described one and minimize the coupling between the model
 * and the viewModel. See the following code example. First without and afterwards with the [ModelWrapper].
 *
 *
 * The model class (Note: ORMs like Hibernate often want to extend a models class. In Java this usually possible,
 *                  but Kotlin requires the class to be explicitly defined `open`):
 *
 *

 *    open class Person constructor(
 *        var name: String,
 *        var familyName: String,
 *        var age: Int) {}

 *
 * Without [ModelWrapper]:
 *
 *

 *    class PersonViewModel {
 *
 *        val name = SimpleStringProperty()
 *        val familyName = SimpleStringProperty()
 *        val age = SimpleIntegerProperty()
 *
 *        lateinit private var person: Person
 *
 *        fun init(person: Person) {
 *            this.person = person
 *            reloadFromModel()
 *        }
 *
 *        fun reset() {
 *            name.value = &quot;&quot;
 *            familyName.value = &quot;&quot;
 *            age.value = 0
 *        }
 *
 *        fun reloadFromModel() {
 *            if (person != null) {
 *                name.value = person.name
 *                familyName.value = person.familyName
 *                age.value = person.age
 *            }
 *        }
 *
 *        fun save() {
 *            if (someValidation() &amp;&amp; person != null) {
 *                person.name = name.value
 *                person.familyName = familyName.value
 *                person.age = age.value
 *            }
 *        }
 *    }

 * With [ModelWrapper]:
 *
 *

 *    class PersonViewModel : ViewSingleModel<Person>() {
 *
 *        val name = wrapper.field(Person::name)
 *        val familyName = wrapper.field(Person::familyName)
 *        val age = wrapper.field(Person::age)
 *    }

 * In the first example without the [ModelWrapper] we have several lines of code that are specific for each field
 * of the model. If we would add a new field to the model (for example "email") then we would have to update several
 * pieces of code in the ViewModel.
 *
 *
 * On the other hand in the example with the [ModelWrapper] there is only the definition of the properties.
 * For each field we therefore have only one place in the ViewModel that would need an update when the structure of the
 * model changes. Aside from that, the abstract [ViewSingleModel] base class already implements the [ModelWrapper] as
 * `wrapper`, an accessor for the wrapped model instance as `model` and `reset()`/`reload()`/`isValid()`/`save()`-
 * functions automatically based on the given type information. Persistence functionality can be added by overriding
 * the `persist()` function, which is called by the `save()`-method, after the fields have been validated and copied
 * into the model instance.
 * If multiple wrappers for multiple backing models are required, have a look at [ViewMultiModel].
 * TODO: Is ViewMultiModel really such a good idea??


 * @param
 * *            the type of the model class.
 * @property modelProperty (optional)
 * *            the property of the model element that will be wrapped.
 * @constructor
 * *            Create a new instance of [ModelWrapper] that wraps the instance of the Model class wrapped by the property.
 * *            Updates all data when the model instance changes.
 * *            If the model instance is not given, is a has to be defined afterwards with the [set] method.
 */
@Suppress("UNUSED")
class ModelWrapper<M : Any>
@JvmOverloads constructor(val modelProperty: ObjectProperty<M> = SimpleObjectProperty<M>()) {
    private val dirtyFlag = ReadOnlyBooleanWrapper()
    private val diffFlag = ReadOnlyBooleanWrapper()

    /**
     * Getter and setter for the model instance wrapped by this model wrapper
     * NB: This is the essentially the same as the [get] and [set] methods, just easier to read
     */
    var model: M?
        get() = modelProperty.get()
        set(value) = modelProperty.set(value)

    /**
     * This interface defines the operations that are possible for each field of a wrapped class.
     *
     * @param T
     * *            target type. The base type of the returned property, f.e. [String].
     * *
     * @param M
     * *            model type. The type of the Model class, that is wrapped by this [ModelWrapper] instance.
     * *
     * @param R
     * *            return type. The type of the Property that is returned via [property], e.g. [StringProperty].
     */
    internal interface PropertyField<T, in M, out R : Property<T>> {
        fun commit(wrappedObject: M)

        fun reload(wrappedObject: M)

        fun resetToDefault()

        fun updateDefault(wrappedObject: M)

        val property: R

        /**
         * Determines if the value in the model object and the property field are different or not.

         * This method is used to implement the [differentProperty] flag.

         * @param wrappedObject
         * *            the wrapped model object
         * *
         * @return `false` if both the wrapped model object and the property field have the same value,
         * *         otherwise `true`
         */
        fun isDifferent(wrappedObject: M): Boolean

        // TODO: It's probably better to fully do this on the ViewModel level instead and have an overridable method stub
        //       just like [ViewModel.commit] there. Advantage: Access to multiple field at the same time allows
        //       more complex validation patterns. PropertyField implementations cleaner.
        //       Disadvantage: Field validation is decoupled from field creation
        // Also, realistically, this should be a ReadOnlyBooleanProperty that gets updated on every propertyWasChanged(),
        // or save local BooleanProperty valid states for every field and bind them all in the @ModelWrapper, always
        // re-validating only the property that actually changed (instead of iterating through all fields, like it's
        // currently done in propertyWasChanged(). we could just keep a differentCount or a differentList and update
        // those without iterating through all fields; depends on whether or not we want to expose differentFields/dirtyFields
        // as an ObservableList; otoh it would also be nice to have something like Property<*>.differentProperty, maybe
        // as an extension function (see @bottom))
        fun isValid(): Boolean

        //fun differentProperty(): ReadOnlyBooleanProperty
        //fun dirtyProperty(): ReadOnlyBooleanProperty
    }

    /**
     * So, this could easily replace KPropertyField and FxPropertyField.
     * The same can probably be done with KListPropertyField/FxListPropertyField and the Set-equivalents
     * Could also name this PropertyFieldImpl or sth else
     */
    private inner class GenericPropertyField<T, out R : Property<T>>
    constructor(
            name: String,
            private val getter: (M) -> T,
            private val setter: ((M, T) -> Unit)?,
            private var defaultValue: T? = null,
            propertySupplier: (M?, String?) -> R,
            private val validator: (T) -> Boolean = { t: T -> true }
               ) : PropertyField<T, M, R> {

        constructor(accessor: KProperty1<M, T>,
                    defaultValue: T? = null,
                    propertySupplier: (M?, String?) -> R,
                    validator: (T) -> Boolean = { t: T -> true }
                   ) : this(accessor.name, accessor.getter, if (accessor is KMutableProperty1<M, T>) accessor.setter else null,
                defaultValue, propertySupplier, validator)

        //@JvmName("FxPropertyField")
        constructor(name: String,
                    accessor: (M) -> Property<T>,
                    defaultValue: T? = null,
                    propertySupplier: (M?, String?) -> R,
                    validator: (T) -> Boolean = { t: T -> true }
                   ) : this(name, { accessor(it).value }, { m: M, t: T -> accessor(m).value = t }, defaultValue,
                propertySupplier, validator)

        //@JvmName("FxPropertyField")
        /*
        constructor(accessor: KProperty1<M, Property<T>>,
                    defaultValue: T? = null,
                    propertySupplier: (M?, String?) -> R,
                    validator: (T) -> Boolean = { t: T -> true }) : this(accessor.name, { accessor.get(it).value },
                if (accessor is KMutableProperty1<M, Property<T>>) { m: M, t: T -> accessor(m).value = t } else null as ((M, T) -> Unit)?,
                defaultValue, propertySupplier, validator)
        */

        override val property: R

        init {
            this.property = propertySupplier(null, name)
            this.property.addListener { observable, oldValue, newValue -> this@ModelWrapper.propertyWasChanged() }
        }

        override fun commit(wrappedObject: M) {
            setter?.invoke(wrappedObject, property.value)
            // gracefully fail and log if we have no setter and the property was changed nonetheless
            // DEBUG_LOG("No setter for $property.name found, while trying to assign the value $property.value")
        }

        override fun reload(wrappedObject: M) {
            property.value = getter(wrappedObject)
        }

        override fun resetToDefault() {
            property.value = defaultValue
        }

        override fun updateDefault(wrappedObject: M) {
            defaultValue = getter(wrappedObject)
        }

        override fun isDifferent(wrappedObject: M): Boolean {
            val modelValue = getter(wrappedObject)
            val wrapperValue = property.value

            return modelValue != wrapperValue
        }

        override fun isValid(): Boolean {
            return validator(property.value)
        }

        @Suppress("UNCHECKED_CAST")
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as GenericPropertyField<*, *>
            return property.name == other.property.name
        }

        override fun hashCode(): Int {
            return property.name.hashCode()
        }
    }

    /**
     * An implementation of [PropertyField] that is used when the fields of the model class are JavaFX Properties too.
     * TODO: Should we accept ReadOnly(List|Set)?Property for the Fx(Set|List)?PropertyFields and gracefully ignore commit()s
     *       or throw an exception; and what about KProperty vs KMutableProperty?
     */
    private inner class FxPropertyField<T, R : Property<T>>
    //@JvmOverloads // causes CompilationException!
    constructor(name: String,
                private val accessor: (M) -> Property<T>,
                private var defaultValue: T? = null,
                propertySupplier: (M?, String?) -> R,
                private val validator: (T) -> Boolean = { t: T -> true }
               ) : PropertyField<T, M, R> {

        constructor(accessor: KProperty1<M, Property<T>>,
                    defaultValue: T? = null,
                    propertySupplier: (M?, String?) -> R,
                    validator: (T) -> Boolean = { t: T -> true }) : this(accessor.name, accessor, defaultValue,
                propertySupplier, validator)

        override val property: R // TODO: use property.name for equals()/hashCode() now that it's set?

        init {
            this.property = propertySupplier(null, name)
            this.property.addListener { observable, oldValue, newValue -> this@ModelWrapper.propertyWasChanged() }
        }

        override fun commit(wrappedObject: M) {
            accessor(wrappedObject).value = property.value
        }

        override fun reload(wrappedObject: M) {
            property.value = accessor(wrappedObject).value
        }

        override fun resetToDefault() {
            property.value = defaultValue
        }

        override fun updateDefault(wrappedObject: M) {
            defaultValue = accessor(wrappedObject).value
        }

        override fun isDifferent(wrappedObject: M): Boolean {
            val modelValue = accessor(wrappedObject).value
            val wrapperValue = property.value

            return modelValue != wrapperValue
        }

        override fun isValid(): Boolean {
            return validator(property.value)
        }

        // TODO: do we also want to compare `property.name` or `defaultValue`?
        @Suppress("UNCHECKED_CAST")
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as FxPropertyField<*, *>
            return accessor == other.accessor
        }

        override fun hashCode(): Int {
            return accessor.hashCode()
        }
    }


    /**
     * An implementation of [PropertyField] that is used when the fields of the model class are **not** JavaFX
     * Properties but are normal Kotlin properties (any visible `val` or `var`).
     *
     * @property accessor [KProperty1] or [KMutableProperty1] of the model class field
     */
    private inner class KPropertyField<T, out R : Property<T>>
    //@JvmOverloads // causes CompilationException!
    constructor(private val accessor: KProperty1<M, T>,
            //private val setter: ((M, T) -> Unit)?,
                private var defaultValue: T? = null,
                propertySupplier: (M?, String?) -> R,
                private val validator: (T) -> Boolean = { t: T -> true }
               ) : PropertyField<T, M, R> {

        override val property: R

        init {
            this.property = propertySupplier(null, accessor.name)
            this.property.addListener { observable, oldValue, newValue -> this@ModelWrapper.propertyWasChanged() }
        }

        override fun commit(wrappedObject: M) {
            if (isDifferent(wrappedObject) && accessor is KMutableProperty1<M, T>) {
                accessor.set(wrappedObject, property.value)
            }
            // do nothing if property is immutable (`val`)
        }

        override fun reload(wrappedObject: M) {
            property.value = accessor(wrappedObject)
        }

        override fun resetToDefault() {
            property.value = defaultValue
        }

        override fun updateDefault(wrappedObject: M) {
            defaultValue = accessor(wrappedObject)
        }

        override fun isDifferent(wrappedObject: M): Boolean {
            val modelValue = accessor(wrappedObject)
            val wrapperValue = property.value

            return modelValue != wrapperValue
        }

        override fun isValid(): Boolean {
            return validator(property.value)
        }

        @Suppress("UNCHECKED_CAST")
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as KPropertyField<*, *>

            if (accessor != other.accessor) return false
            if (defaultValue != other.defaultValue) return false

            return true
        }

        override fun hashCode(): Int {
            var result = accessor.hashCode()
            result = 31 * result + (defaultValue?.hashCode() ?: 0)
            return result
        }
    }

    /**
     * An implementation of [PropertyField] that is used when the field of the model class is a [ListProperty] too.
     *
     * @param E
     * *            the type of the list elements.
     */
    private inner class FxListPropertyField<E, T : ObservableList<E>, out R : Property<T>>
    //@JvmOverloads // causes CompilationException!
    constructor(private val accessor: KProperty1<M, ListProperty<E>>,
                private var defaultValue: List<E>? = mutableListOf<E>(),
                private val validator: (T) -> Boolean = { t: T -> true }
               ) : PropertyField<T, M, R> {

        private val targetProperty = SimpleListProperty(FXCollections.observableArrayList<E>())

        init {
            this.targetProperty.addListener(ListChangeListener<E> { change -> this@ModelWrapper.propertyWasChanged() })
        }

        override fun commit(wrappedObject: M) {
            accessor(wrappedObject).setAll(targetProperty.value)
        }

        override fun reload(wrappedObject: M) {
            targetProperty.setAll(accessor(wrappedObject).value)
        }

        override fun resetToDefault() {
            targetProperty.setAll(defaultValue)
        }

        override fun updateDefault(wrappedObject: M) {
            defaultValue = ArrayList(accessor(wrappedObject).value)
        }

        @Suppress("UNCHECKED_CAST")
        override val property: R
            get() = targetProperty as R

        override fun isDifferent(wrappedObject: M): Boolean {
            val modelValue = accessor(wrappedObject).value
            val wrapperValue = targetProperty

            return modelValue != wrapperValue
        }

        override fun isValid(): Boolean {
            return validator(property.value)
        }

        @Suppress("UNCHECKED_CAST")
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as FxListPropertyField<*, *, *>

            if (accessor != other.accessor) return false
            if (defaultValue != other.defaultValue) return false

            return true
        }

        override fun hashCode(): Int {
            var result = accessor.hashCode()
            result = 31 * result + (defaultValue?.hashCode() ?: 0)
            return result
        }
    }

    /**
     * An implementation of [PropertyField] that is used when the field of the model class is a [List] and
     * is **not** a JavaFX [ListProperty].

     * @param E
     * *            the type of the list elements.
     */
    private inner class KListPropertyField<E, T : ObservableList<E>, out R : Property<T>>
    //@JvmOverloads // causes CompilationException!
    constructor(private val accessor: KProperty1<M, List<E>>,
                private var defaultValue: List<E>? = mutableListOf<E>(),
                private val validator: (T) -> Boolean = { t: T -> true }
               ) : PropertyField<T, M, R> {

        private val targetProperty = SimpleListProperty(FXCollections.observableArrayList<E>())

        init {
            this.targetProperty.addListener(ListChangeListener<E> { change -> this@ModelWrapper.propertyWasChanged() })
        }

        // TODO: KMutableProperty vs MutableList
        // Figure out how to do this better than clearing the model's list and re-adding everything, even
        // if there may have been just one element added or removed (which is common in CRUD).
        // A problem we face here is, that if we try to do things in a more intelligent way, the list elements
        // need to have proper `equals()`-implementations. I'm not sure if we can assume this to be the case :/
        // Also, we may not change the order of the list... With a [Set] it's probably easier :/
        override fun commit(wrappedObject: M) {
            val list = accessor(wrappedObject)
            if (list is MutableList<E>) {
                list.clear()
                list.addAll(targetProperty.value)
            }
            // do nothing if list is immutable (even if it is a KMutableProperty)
        }

        override fun reload(wrappedObject: M) {
            targetProperty.setAll(accessor(wrappedObject))
        }

        override fun resetToDefault() {
            targetProperty.setAll(defaultValue)
        }

        override fun updateDefault(wrappedObject: M) {
            defaultValue = ArrayList<E>(accessor(wrappedObject))
        }

        @Suppress("UNCHECKED_CAST")
        override val property: R
            get() = targetProperty as R

        override fun isDifferent(wrappedObject: M): Boolean {
            val modelValue = accessor(wrappedObject)
            val wrapperValue = targetProperty

            return modelValue != wrapperValue
        }

        override fun isValid(): Boolean {
            return validator(property.value)
        }

        @Suppress("UNCHECKED_CAST")
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as KListPropertyField<*, *, *>

            if (accessor != other.accessor) return false
            if (defaultValue != other.defaultValue) return false

            return true
        }

        override fun hashCode(): Int {
            var result = accessor.hashCode()
            result = 31 * result + (defaultValue?.hashCode() ?: 0)
            return result
        }
    }

    /**
     * An implementation of [PropertyField] that is used when the field of the model class is a [SetProperty] too.
     *
     * @param E
     * *            the type of the set elements.
     */
    private inner class FxSetPropertyField<E, T : ObservableSet<E>, out R : Property<T>>
    //@JvmOverloads // causes CompilationException!
    constructor(private val accessor: KProperty1<M, SetProperty<E>>,
                private var defaultValue: Set<E>? = mutableSetOf<E>(),
                private val validator: (T) -> Boolean = { t: T -> true }
               ) : PropertyField<T, M, R> {

        private val targetProperty = SimpleSetProperty(FXCollections.observableSet<E>())

        init {
            this.targetProperty.addListener(SetChangeListener<E> { change -> this@ModelWrapper.propertyWasChanged() })
        }

        override fun commit(wrappedObject: M) {
            val set = accessor(wrappedObject)
            if (set is SetProperty<E>) {
                set.clear()
                set.addAll(targetProperty.value)
            }
        }

        override fun reload(wrappedObject: M) {
            targetProperty.clear()
            targetProperty.addAll(accessor(wrappedObject).value)
        }

        override fun resetToDefault() {
            targetProperty.clear()
            targetProperty.addAll(defaultValue ?: emptySet())
        }

        override fun updateDefault(wrappedObject: M) {
            defaultValue = LinkedHashSet<E>(accessor(wrappedObject).value)
        }

        @Suppress("UNCHECKED_CAST")
        override val property: R
            get() = targetProperty as R

        override fun isDifferent(wrappedObject: M): Boolean {
            val modelValue = accessor(wrappedObject).value
            val wrapperValue = targetProperty

            return modelValue != wrapperValue
        }

        override fun isValid(): Boolean {
            return validator(property.value)
        }

        @Suppress("UNCHECKED_CAST")
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as FxSetPropertyField<*, *, *>

            if (accessor != other.accessor) return false
            if (defaultValue != other.defaultValue) return false

            return true
        }

        override fun hashCode(): Int {
            var result = accessor.hashCode()
            result = 31 * result + (defaultValue?.hashCode() ?: 0)
            return result
        }
    }

    /**
     * An implementation of [PropertyField] that is used when the field of the model class is a [Set] and
     * is **not** a JavaFX [SetProperty].
     *
     * @param E
     * *            the type of the set elements.
     */
    private inner class KSetPropertyField<E, T : ObservableSet<E>, out R : Property<T>>
    //@JvmOverloads // causes CompilationException!
    constructor(private val accessor: KProperty1<M, Set<E>>,
                private var defaultValue: Set<E>? = mutableSetOf<E>(),
                private val validator: (T) -> Boolean = { t: T -> true }
               ) : PropertyField<T, M, R> {

        private val targetProperty = SimpleSetProperty(FXCollections.observableSet<E>())

        init {
            this.targetProperty.addListener(SetChangeListener<E> { change -> this@ModelWrapper.propertyWasChanged() })
        }

        // TODO: KMutableProperty vs MutableSet!
        override fun commit(wrappedObject: M) {
            val set = accessor(wrappedObject)
            if (set is MutableSet<E>) {
                set.clear()
                set.addAll(targetProperty.value)
            }
            // do nothing if set is immutable (even if it is a KMutableProperty)
        }

        override fun reload(wrappedObject: M) {
            targetProperty.clear()
            targetProperty.addAll(accessor(wrappedObject))
        }

        override fun resetToDefault() {
            targetProperty.clear()
            targetProperty.addAll(defaultValue ?: emptySet())
        }

        override fun updateDefault(wrappedObject: M) {
            defaultValue = LinkedHashSet(accessor(wrappedObject))
        }

        @Suppress("UNCHECKED_CAST")
        override val property: R
            get() = targetProperty as R

        override fun isDifferent(wrappedObject: M): Boolean {
            val modelValue = accessor(wrappedObject)
            val wrapperValue = targetProperty

            return modelValue != wrapperValue
        }

        override fun isValid(): Boolean {
            return validator(property.value)
        }

        @Suppress("UNCHECKED_CAST")
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as KSetPropertyField<*, *, *>

            if (accessor != other.accessor) return false
            if (defaultValue != other.defaultValue) return false

            return true
        }

        override fun hashCode(): Int {
            var result = accessor.hashCode()
            result = 31 * result + (defaultValue?.hashCode() ?: 0)
            return result
        }
    }

    // TODO: Add tests to ensure that `equals` holds
    private val fields: MutableSet<PropertyField<*, M, *>> = LinkedHashSet()

    internal operator fun get(propName: String): PropertyField<*, M, *>? {
        return fields.firstOrNull { it.property.name == propName }
    }

    val size: Int
        get() = fields.size

    init {
        reload()
        this.modelProperty.addListener { observable, oldValue, newValue ->
            reload()
            useCurrentValuesAsDefaults()
        }
    }

    /**
     * Create a new instance of [ModelWrapper] that wraps the given instance of the Model class.

     * @param model
     * *            the element of the model that will be wrapped.
     */
    constructor(model: M?) : this(SimpleObjectProperty(model))

    /**
     * Define the model element that will be wrapped by this [ModelWrapper] instance.

     * @param model
     * *            the element of the model that will be wrapped.
     */
    fun set(model: M?) = this.modelProperty.set(model)

    /**
     * @return the wrapped model element if one was defined, otherwise `null`.
     */
    fun get(): M? = this.modelProperty.get()

    /**
     * @return property holding the model instance wrapped by this model wrapper instance.
     */
    fun modelProperty(): ObjectProperty<M> = modelProperty

    /**
     * Resets all defined fields to their default values. If no default value was defined `null` will be used
     * instead.
     *
     *
     * **Note:** This method has no effects on the wrapped model element but will only change the values of the
     * defined property fields.
     */
    fun reset() {
        fields.forEach { it.resetToDefault() }
        calculateDifferenceFlag()
    }

    /**
     * Use all values that are currently present in the wrapped model object as new default values for respective field.
     * This overrides/updates the values that were set during the initialization of the field mappings.
     *
     *
     * Subsequent calls to [reset] will reset the values to this new default values.
     *
     *
     * Usage example:
     *

    ModelWrapper{@code} wrapper =  ModelWrapper{@code<>}()

    val name = wrapper.field(Person::name, "oldDefault")

    val p = Person()
    wrapper.set(p) // or `model = p` if using [ViewSingleModel]


    p.name = "Luise"

    wrapper.useCurrentValuesAsDefaults() // now "Luise" is the default value for the name field.


    name.set("Hugo")
    wrapper.commit()

    name.get() // Hugo
    p.name // Hugo


    wrapper.reset() // reset to the new defaults
    name.get() // Luise

    wrapper.commit() // put values from properties to the wrapped model object
    p.name // Luise

     */
    fun useCurrentValuesAsDefaults() {
        val m = model
        if (m != null) {
            fields.forEach { it.updateDefault(m) }
        }
    }

    /**
     * Take the current value of each property field and write it into the wrapped model element.
     *
     *
     * If no model element is defined then nothing will happen.
     *
     *
     * **Note:** This method has no effects on the values of the defined property fields but will only change the
     * state of the wrapped model element.
     */
    fun commit() {
        val m = model
        if (m != null) {
            fields.forEach { it.commit(m) }
            dirtyFlag.set(false)
            calculateDifferenceFlag()
        }
    }

    /**
     * Take the current values from the wrapped model element and put them in the corresponding property fields.
     *
     *
     * If no model element is defined then nothing will happen.
     *
     *
     * **Note:** This method has no effects on the wrapped model element but will only change the values of the
     * defined property fields.
     */
    fun reload() {
        val m = model
        if (m != null) {
            fields.forEach { it.reload(m) }
            dirtyFlag.set(false)
            calculateDifferenceFlag()
        }
    }

    fun isValid(): Boolean {
        fields.forEach { if (!it.isValid()) return false }
        return true
    }


    private fun propertyWasChanged() {
        dirtyFlag.set(true)
        calculateDifferenceFlag()
    }

    private fun calculateDifferenceFlag() {
        val m = model
        if (m != null && !fields.isEmpty()) {
            fields.forEach {
                if (it.isDifferent(m)) {
                    diffFlag.set(true)
                    return
                }
            }
            diffFlag.set(false)
        }
    }

    operator fun contains(prop: KProperty1<M, *>): Boolean {
        return fields.any { it.property.name == prop.name }
    }

    operator fun contains(propName: String?): Boolean {
        return fields.any { it.property.name == propName }
    }


    /** Field type String  */

    /**
     * Add a new field of type [String] to this instance of the wrapper. This method is used for model elements
     * that are accessible as [KProperty1] or [KMutableProperty1]. This is the recommended method.
     *
     *

     * Example:
     *
     *

    val personWrapper = new ModelWrapper{@code<Person>}()

    val wrappedNameProperty = personWrapper.field(Person::name, "")

     * @param accessor
     * *            the [KProperty1]/[KMutableProperty1] for the field of a given model instance.
     * *
     * @param defaultValue (optional)
     * *            the default value that is used when [reset] is invoked. if not given and the underlying property
     * *            is nullable the defaultValue is `null`, otherwise it's an empty string
     * *
     * @return The wrapped property instance.
     */
    @JvmOverloads
    @JvmName("nullableField")
    fun field(accessor: KProperty1<M, String?>, defaultValue: String? = null): ReadOnlyStringProperty {
        return add(KPropertyField(accessor, defaultValue, ::SimpleStringProperty))
    }

    // TODO: Do we want to separate ReadOnly and ReadWrite fields?
    //      (the model fields are usually `var`s anyway)

    @JvmOverloads
    @JvmName("nullableField")
    fun field(accessor: KMutableProperty1<M, String?>, defaultValue: String? = null): StringProperty {
        return add(KPropertyField(accessor, defaultValue, ::SimpleStringProperty))
    }

    // TODO: Is this a good idea to provide nice default values if the KProperty value is not nullable?
    // Ideally we'd use the defaultValue as it's given the primary constructor of M or the initial value in M,
    // but that isn't really possible in java/kotlin without creating an instance of the class and we can't force users
    // to have a no-argument constructor so we can have a look, so just do something "good enough"
    @JvmOverloads
    fun field(accessor: KProperty1<M, String>, defaultValue: String = ""): ReadOnlyStringProperty {
        return add(KPropertyField(accessor, defaultValue, ::SimpleStringProperty))
    }

    @JvmOverloads
    fun field(accessor: KMutableProperty1<M, String>, defaultValue: String = ""): StringProperty {
        return add(KPropertyField(accessor, defaultValue, ::SimpleStringProperty))
    }

    // TODO: we could support POJOs more or less like in Properties.kt:
    // Note: Properties.kt requires a KClass<T> argument, but here we really only use it for type inference,
    //       but there are other possibilities. Which is best?:
    ///    val name: StringProperty = wrapper.field("name") // infered by return type (just ditch propType from below)
    //       or
    ///    val name = wrapper.field("name", String::class)  // infered by parameter <--- IMHO this (implemented below)
    //      or we take a non-generic generic (`fun <T: String> field(...)`)
    ///    val name = wrapper.field<String>("name") // infered by generic parameter

    @JvmOverloads
    fun field(propName: String, @Suppress(
            "UNUSED_PARAMETER") propType: KClass<String>, defaultValue: String = ""): StringProperty {
        val suffix = propName.first().toUpperCase() + propName.substring(1)

        // works if we do sth like this: inline fun <reified C: M>, but then we'd have to do:
        ///      `val name = wrapper.field<Person>(name, String::class)`
        // advantage: getter and setter are immediately available
        //val getter = C::class.java.getDeclaredMethod("get$suffix")
        //val setter = C::class.java.getDeclaredMethod("set$suffix")

        val m = model
        if (m != null) {
            // fail early if we have a model instance set and the property is invalid
            m.javaClass.getDeclaredMethod("get$suffix") ?: throw IllegalArgumentException("Invalid property $propName!")
        }

        // avoids `bean` parameter dependency or reified generic hell, but fails late and silently instead of loud and early
        // disadvantage: methods have to be re-determined via reflection every time they are invoked
        @Suppress("UNCHECKED_CAST")
        val wrappedGetter = { m: M -> model?.javaClass?.getDeclaredMethod("get$suffix")?.invoke(m) ?: defaultValue }
                as (M) -> String?
        @Suppress("UNCHECKED_CAST")
        val wrappedSetter = { m: M, v: String? -> model?.javaClass?.getDeclaredMethod("set$suffix")?.invoke(m, v) }
                as (M, String?) -> Unit

        return add(GenericPropertyField(propName, wrappedGetter, wrappedSetter, defaultValue, ::SimpleStringProperty))

        //return field(propName, wrappedGetter, wrappedSetter, defaultValue)
    }

    @JvmOverloads
    fun field(propName: String, getter: (M) -> String?, setter: (M, String?) -> Unit, defaultValue: String? = null): StringProperty {
        return add(GenericPropertyField(propName, getter, setter, defaultValue, ::SimpleStringProperty))


        /* @Deprecated in favor of GenericPropertyField
        // or we could just do this

        val kProp = object : KMutableProperty1<M, String?> {
            override fun invoke(p1: M): String? {
                return get(p1)
            }

            override val getter: KProperty1.Getter<M, String?>
                get() = throw UnsupportedOperationException()

            override fun get(receiver: M): String? {
                return getter(receiver)
            }

            override val setter: KMutableProperty1.Setter<M, String?>
                get() = throw UnsupportedOperationException()

            override fun set(receiver: M, value: String?) {
                setter(receiver, value)
            }

            override val name: String
                get() = propName
            override val annotations: List<Annotation>
                get() = emptyList()
            override val parameters: List<KParameter>
                get() = throw UnsupportedOperationException()
            override val returnType: KType
                get() = String::class.defaultType

            override fun call(vararg args: Any?): String? {
                val m = args[0] as? M ?: throw IllegalArgumentException()
                return invoke(m)
            }

            override fun callBy(args: Map<KParameter, Any?>): String? {
                val m = args.values.firstOrNull() as? M ?: throw IllegalArgumentException()
                return get(m)
            }

        }

        return add(KPropertyField(kProp, defaultValue, ::SimpleStringProperty))
        */
        //throw NotImplementedError()
        //return add(PojoPropertyField(propName, getter, setter, defaultValue, ::SimpleStringProperty))
    }

    /**
     * Add a new field of type [String] to this instance of the wrapper. This method is used for model elements
     * that are following the enhanced JavaFX-Beans-standard i.e. the model fields are available as JavaFX Properties.
     *
     *

     * Example:
     *
     *

    val personWrapper = ModelWrapper{@code<Person>}()

    val wrappedNameProperty = personWrapper.field({ it.nameProperty() })

    // or with a method reference
    val wrappedNameProperty = personWrapper.field(Person::nameProperty)

     * @param accessor
     * *            the [KProperty1]/[KMutableProperty1] that returns the JavaFX property for a given model instance.
     * *
     * @param defaultValue (optional)
     * *            the default value that is used when [reset] is invoked.
     * *
     * @return The wrapped property instance.
     */
    @JvmOverloads
    @JvmName("fxfield")
    fun field(accessor: KProperty1<M, StringProperty>, defaultValue: String? = null): StringProperty {
        return add(FxPropertyField(accessor, defaultValue, ::SimpleStringProperty))
    }


    /** Field type Boolean  */

    @JvmOverloads
    @JvmName("nullableField")
    fun field(accessor: KProperty1<M, Boolean?>, defaultValue: Boolean? = null): BooleanProperty {
        return add(KPropertyField(accessor, defaultValue, ::SimpleBooleanProperty))
    }

    @JvmOverloads
    fun field(accessor: KProperty1<M, Boolean>, defaultValue: Boolean = false): BooleanProperty {
        return add(KPropertyField(accessor, defaultValue, ::SimpleBooleanProperty))
    }

    @JvmOverloads
    @JvmName("fxfield")
    fun field(accessor: KProperty1<M, BooleanProperty>, defaultValue: Boolean? = null): BooleanProperty {
        return add(FxPropertyField(accessor, defaultValue, ::SimpleBooleanProperty))
    }


    /** Field type Double  */
    // Note: DoubleProperty does not implement Property<Double> and that probably won't change:
    // https://community.oracle.com/thread/2575601
    // The same goes for FloatProperty, IntegerProperty and LongProperty.

    @JvmOverloads
    fun field(accessor: KProperty1<M, Double?>, defaultValue: Double? = null): DoubleProperty {
        return add(KPropertyField(accessor, defaultValue, ::SimpleDoubleProperty))
    }

    @JvmOverloads
    @JvmName("fxfield")
    fun field(accessor: KProperty1<M, DoubleProperty>, defaultValue: Double? = null): DoubleProperty {
        return add(FxPropertyField(accessor, defaultValue, ::SimpleDoubleProperty))
    }


    /** Field type Float  */

    @JvmOverloads
    fun field(accessor: KProperty1<M, Float?>, defaultValue: Float? = null): FloatProperty {
        return add(KPropertyField(accessor, defaultValue, ::SimpleFloatProperty))
    }

    @JvmOverloads
    @JvmName("fxfield")
    fun field(accessor: KProperty1<M, FloatProperty>, defaultValue: Float? = null): FloatProperty {
        return add(FxPropertyField(accessor, defaultValue, ::SimpleFloatProperty))
    }


    /** Field type Integer  */

    @JvmOverloads
    fun field(accessor: KProperty1<M, Int?>, defaultValue: Int? = null): IntegerProperty {
        return add(KPropertyField(accessor, defaultValue, ::SimpleIntegerProperty))
    }

    @JvmOverloads
    @JvmName("fxfield")
    fun field(accessor: KProperty1<M, IntegerProperty>, defaultValue: Int? = null): IntegerProperty {
        return add(FxPropertyField(accessor, defaultValue, ::SimpleIntegerProperty))
    }


    /** Field type Long  */

    @JvmOverloads
    fun field(accessor: KProperty1<M, Long?>, defaultValue: Long? = null): LongProperty {
        return add(KPropertyField(accessor, defaultValue, ::SimpleLongProperty))
    }

    @JvmOverloads
    @JvmName("fxfield")
    fun field(accessor: KProperty1<M, LongProperty>, defaultValue: Long? = null): LongProperty {
        return add(FxPropertyField(accessor, defaultValue, ::SimpleLongProperty))
    }


    /** Field type generic  */

    //@JvmOverloads
    fun <T> field(accessor: KProperty1<M, T>, defaultValue: T): ObjectProperty<T> {
        return add<T, ObjectProperty<T>>(// ::SimpleObjectProperty cannot infer type parameter
                KPropertyField(accessor, defaultValue, { b: Any?, n: String? -> SimpleObjectProperty<T>(b, n) }))
    }

    //@JvmOverloads
    @JvmName("fxfield")
    fun <T> field(accessor: KProperty1<M, Property<T>>, defaultValue: T): ObjectProperty<T> {
        return add(FxPropertyField(accessor, defaultValue, { b: Any?, n: String? -> SimpleObjectProperty<T>(b, n) }))
    }


    /** Field type List  */
    // todo: nullable lists and sets

    @JvmOverloads
    fun <E> field(accessor: KProperty1<M, List<E>>, defaultValue: List<E>? = mutableListOf<E>()): ListProperty<E> {
        return add(KListPropertyField<E, ObservableList<E>, ListProperty<E>>(accessor, defaultValue))
    }

    @JvmOverloads
    @JvmName("fxfield")
    fun <E> field(accessor: KProperty1<M, ListProperty<E>>, defaultValue: List<E>? = mutableListOf<E>()): ListProperty<E> {
        return add(FxListPropertyField<E, ObservableList<E>, ListProperty<E>>(accessor, defaultValue))
    }

    /** Field type Set  */

    @JvmOverloads
    fun <E> field(accessor: KProperty1<M, Set<E>>, defaultValue: Set<E>? = mutableSetOf<E>()): SetProperty<E> {
        return add(KSetPropertyField<E, ObservableSet<E>, SetProperty<E>>(accessor, defaultValue))
    }

    @JvmOverloads
    @JvmName("fxfield")
    fun <E> field(accessor: KProperty1<M, SetProperty<E>>, defaultValue: Set<E>? = mutableSetOf<E>()): SetProperty<E> {
        return add(FxSetPropertyField<E, ObservableSet<E>, SetProperty<E>>(accessor, defaultValue))
    }


    private fun <T, R : Property<T>> add(field: PropertyField<T, M, R>): R {
        if (!fields.add(field)) return field.property // return without reload() if already added
        val m = model
        if (m != null) {
            field.reload(m)
        }
        return field.property
    }

    /**
     * This boolean flag indicates whether there is a difference of the data between the wrapped model object and the
     * properties provided by this wrapper.
     *
     *
     * Note the difference to [dirtyProperty]: This property will be `true` if the data of the
     * wrapped model is different to the properties of this wrapper. If you change the data back to the initial state so
     * that the data is equal again, this property will change back to `false` while the
     * [dirtyProperty] will still be `true`.

     * Simply speaking: This property indicates whether there is a difference in data between the model and the wrapper.
     * The [dirtyProperty] indicates whether there was a change done.


     * Note: Only those changes are observed that are done through the wrapped property fields of this wrapper. If you
     * change the data of the model instance directly, this property won't turn to `true`.


     * @return a read-only property indicating a difference between model and wrapper.
     */
    fun differentProperty(): ReadOnlyBooleanProperty {
        return diffFlag.readOnlyProperty
    }

    /**
     * See [differentProperty].
     */
    val isDifferent: Boolean
        get() = diffFlag.get()

    /**
     * This boolean flag indicates whether there was a change to at least one wrapped property.
     *
     *
     * Note the difference to [differentProperty]: This property will turn to `true` when the value
     * of one of the wrapped properties is changed. It will only change back to `false` when either the
     * [commit] or [reload] method is called. This property will stay `true` even if
     * afterwards another change is done so that the data is equal again. In this case the [differentProperty]
     * will switch back to `false`.

     * Simply speaking: This property indicates whether there was a change done to the wrapped properties or not. The
     * [differentProperty] indicates whether there is a difference in data at the moment.

     * @return a read only boolean property indicating if there was a change done.
     */
    fun dirtyProperty(): ReadOnlyBooleanProperty {
        return dirtyFlag.readOnlyProperty
    }

    /**
     * See [dirtyProperty].
     */
    val isDirty: Boolean
        get() = dirtyFlag.get()
}


/**
 * Create a field for every [KProperty1] of the wrapped model, including inherited ones, and return those fields
 * as a map indexed by the properties name.
 *
 * XXX: This may not make much sense because we loose way too much type information
 */
inline fun <reified M : Any> ModelWrapper<M>.allFields(exclude: String, vararg exclusions: String): Map<String, Property<*>> {
    val res = mutableMapOf<String, Property<*>>()

    val props = M::class.memberProperties
    for (prop in props) {
        if (prop.name == exclude || prop.name in exclusions) continue
        val fxProp = this.field(prop, null)
        res.put(prop.name, fxProp)
    }
    return Collections.unmodifiableMap(res)
}

inline fun <reified M : Any> ModelWrapper<M>.allFields(vararg exclusions: KProperty1<M, *>): Map<String, Property<*>> {
    val res = mutableMapOf<String, Property<*>>()

    val props = M::class.memberProperties
    for (prop in props) {
        if (prop in exclusions) continue
        val fxProp = this.field(prop, null)
        res.put(prop.name, fxProp)
    }
    return Collections.unmodifiableMap(res)
}

/**
 * Create a field for every [KProperty1] of the wrapped model, excluding inherited ones, and return those fields
 * as a map indexed by the properties name.
 *
 * XXX: This may not make much sense because we loose way too much type information
 */
inline fun <reified M : Any> ModelWrapper<M>.allDeclaredFields(exclude: String, vararg exclusions: String): Map<String, Property<*>> {
    val res = mutableMapOf<String, Property<*>>()

    val props = M::class.declaredMemberProperties
    for (prop in props) {
        if (prop.name == exclude || prop.name in exclusions) continue
        val fxProp = this.field(prop, null)
        res.put(prop.name, fxProp)
    }
    return Collections.unmodifiableMap(res)
}

inline fun <reified M : Any> ModelWrapper<M>.allDeclaredFields(vararg exclusions: KProperty1<M, *>): Map<String, Property<*>> {
    val res = mutableMapOf<String, Property<*>>()

    val props = M::class.declaredMemberProperties
    for (prop in props) {
        if (prop in exclusions) continue
        val fxProp = this.field(prop, null)
        res.put(prop.name, fxProp)
    }
    return Collections.unmodifiableMap(res)
}


/**
 * An abstract ViewModel implementation with multiple backing models wrapped in multiple [ModelWrapper]s.
 * This class implements [reset]/[reload]/[isValid]/[save] in a way, so that each of them calls the matching
 * underlying function on each [ModelWrapper] instance found in the class(chain) inheriting from this one.
 */
@Suppress("UNUSED", "UNCHECKED_CAST")
abstract class ViewMultiModel : ViewModel {
    private val wrappers: List<KProperty1<ViewMultiModel, ModelWrapper<*>>>

    init {
        // add all model wrappers of the chain of the derived class to the list of wrappers to manage
        wrappers = this.javaClass.kotlin.memberProperties.filterIsInstance<KProperty1<ViewMultiModel, ModelWrapper<*>>>()
    }

    /** Reset the ViewModel to its default values.  */
    override fun reset() = wrappers.forEach { it(this).reset() }

    /** Reload the ViewModel from the backing entities.  */
    override fun reload() = wrappers.forEach { it(this).reload() }

    /**
     * Check whether or not all fields are valid
     * // todo: should we somehow return which property/properties was/were at fault?
     */
    override fun isValid(): Boolean {
        wrappers.forEach { if (!it(this).isValid()) return false }
        return true
    }

    // TODO: design API
    // somehow we need to map the property at fault to a validation failure string
    // the validation failure string is to be returned by the respective properties validator,
    // but how do we return the object, so that the failure message can be assigned to the correct
    // input field in the UI?
    fun invalidFields(): Map<Property<*>, String> {
        return emptyMap()
    }

    /** Copy data, if [isValid], from ViewModel to Entity and call the [persist]-function.  */
    override fun save(): Boolean {
        if (!isValid()) return false
        wrappers.forEach { it(this).commit() }
        return persist()
    }

    /**
     * If required, implement your persistence functionality here.
     * This method is called after the [save]-method has validated all
     * fields and copied the ViewModel's state to the wrapped entity.
     */
    open internal fun persist(): Boolean {
        return true
    }
}

/**
 * An abstract ViewModel implementation with a single backing model.
 * This class automatically creates the [ModelWrapper]-instance ([wrapper]) based on the generic type information,
 * implements [reset]/[reload]/[isValid]/[save] accordingly and provides an accessor ([model]) for the
 * wrapped model instance, that can also be set in the constructor.
 */
@Suppress("UNUSED")
abstract class ViewSingleModel<M : Any> : ViewModel {
    internal val wrapper: ModelWrapper<M> = ModelWrapper()

    var model: M?
        get() = wrapper.get()
        set(newModel) {
            wrapper.set(newModel)
            wrapper.reload()
        }

    constructor() {
    }

    /** Create a new instance of this ViewModel, wrapping the given entity.  */
    constructor(model: M) {
        this.model = model
    }

    /** Reset the ViewModel to its default values.  */
    override fun reset() = wrapper.reset()

    /** Reload the ViewModel from the backing entity.  */
    override fun reload() = wrapper.reload()

    /** Check whether or not all fields are valid  */
    override fun isValid(): Boolean = wrapper.isValid()

    fun dirtyProperty(): ReadOnlyBooleanProperty = wrapper.dirtyProperty()
    fun differentProperty(): ReadOnlyBooleanProperty = wrapper.differentProperty()

    // TODO: design API (see [ViewMultiModel])
    fun invalidFields(): Map<Property<*>, String> {
        return emptyMap()
    }

    /** Copy data, if [isValid], from ViewModel to Entity and call the [persist]-function.  */
    override fun save(): Boolean {
        if (!isValid()) return false
        wrapper.commit()
        return persist()
    }

    /**
     * If required, implement your persistence functionality here.
     * This method is called after the [save]-method has validated all
     * fields and copied the ViewModel's state to the wrapped entity.
     */
    open internal fun persist(): Boolean {
        return true
    }
    /*
        fun Property<*>.isDifferent(): ReadOnlyBooleanProperty {
            return this@ViewSingleModel.wrapper[this.name]!!.differentProperty()
        }

        fun Property<*>.isDirty(): ReadOnlyBooleanProperty {
            return this@ViewSingleModel.wrapper[this.name]!!.dirtyProperty()
        }
    */
}

interface ViewModel {
    fun reset()
    fun reload()
    fun isValid(): Boolean
    //fun invalidFields()
    fun save(): Boolean
}


/**
 * InsaneViewModel: Now we don't even have to call `wrapper.field` any more! (and I've officially gone insane...)
 * I think we could get rid of the properties by doing bytecode manipulation...
 * Example (at least that's how this should work):
 *
 *     class Person constructor(var name, var age, var familyName){}
 *
 *     class PersonViewModel: StandaloneViewModel<Person> {
 *         lateinit var name: StringProperty
 *         lateinit var age: IntegerProperty
 *         lateinit var familyName: StringProperty
 *     }
 *
 */
abstract class InsaneViewModel<M : Any> constructor(var model: M) : ViewModel {
    val wrapper = ModelWrapper(model)

    init {
        model.javaClass.kotlin.declaredMemberProperties.forEach { field ->
            // prop(erty) with same name as field?
            val prop = this.javaClass.kotlin.declaredMemberProperties.firstOrNull { it.name == field.name }
            if (prop is KMutableProperty1<*, *>) {
                // prop is mutable
                // try to wrap field in prop
                val nullable = field.returnType.isMarkedNullable
                when (field) {
                    is KMutableProperty1<M, *> -> {
                        when (field.returnType) { // <---- nice workaround for erased type
                            String::class.defaultType -> {
                                prop as? KMutableProperty1<InsaneViewModel<M>, StringProperty> ?: throw RuntimeException()
                                if (nullable) prop.set(this, wrapper.field(field as KMutableProperty1<M, String?>))
                                else prop.set(this, wrapper.field(field as KMutableProperty1<M, String>))
                            }
                        // and so on
                        }
                    }
                    is KProperty1<M, *> -> {
                        when (field.returnType) {
                            String::class.defaultType -> {
                                prop as? KMutableProperty1<InsaneViewModel<M>, ReadOnlyStringProperty> ?: throw RuntimeException()
                                if (nullable) prop.set(this, wrapper.field(field as KProperty1<M, String?>))
                                else prop.set(this, wrapper.field(field as KProperty1<M, String>))
                            }
                        // and so on
                        }
                    }
                }
            }
        }
    }

    /** Reset the ViewModel to its default values.  */
    override fun reset() = wrapper.reset()

    /** Reload the ViewModel from the backing entity.  */
    override fun reload() = wrapper.reload()

    /** Check whether or not all fields are valid  */
    override fun isValid(): Boolean = wrapper.isValid()

    fun dirtyProperty(): ReadOnlyBooleanProperty = wrapper.dirtyProperty()
    fun differentProperty(): ReadOnlyBooleanProperty = wrapper.differentProperty()

    /** Copy data, if [isValid], from ViewModel to Entity and call the [persist]-function.  */
    override fun save(): Boolean {
        if (!isValid()) return false
        wrapper.commit()
        return persist()
    }

    /**
     * If required, implement your persistence functionality here.
     * This method is called after the [save]-method has validated all
     * fields and copied the ViewModel's state to the wrapped entity.
     */
    open internal fun persist(): Boolean {
        return true
    }
}

/*
fun Property<*>.isDifferent(wrapper: ModelWrapper<*>): ReadOnlyBooleanProperty {
    return wrapper[this.name]!!.differentProperty()
}

fun Property<*>.isDirty(wrapper: ModelWrapper<*>): ReadOnlyBooleanProperty {
    return wrapper[this.name]!!.dirtyProperty()
}


fun ViewSingleModel<*>.isDifferent(prop: Property<*>): ReadOnlyBooleanProperty {
    return this.wrapper[prop.name]!!.differentProperty()
}

fun ViewSingleModel<*>.isDirty(prop: Property<*>): ReadOnlyBooleanProperty {
    return this.wrapper[prop.name]!!.dirtyProperty()
}
*/
