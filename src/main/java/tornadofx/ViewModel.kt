package tornadofx

import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections

open class ViewModel {
    private val properties = FXCollections.observableHashMap<Property<*>, Property<*>>()
    private val propertyProviders = FXCollections.observableHashMap<Property<*>, () -> Property<*>>()
    private val dirtyProperties = FXCollections.observableArrayList<ObservableValue<*>>()
    val dirtyProperty = SimpleBooleanProperty(false)

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
    fun <S : Property<T>, T> bind(op: () -> S): Property<T> {
        val prop = op()
        val wrapper = SimpleObjectProperty<T>(prop.value)
        @Suppress("UNCHECKED_CAST")
        wrapper.addListener(dirtyListener as ChangeListener<in T>)
        properties[wrapper] = prop
        propertyProviders[wrapper] = op
        return wrapper
    }

    fun rebind() {
        for ((wrapper, op) in propertyProviders) {
            val prop = op()
            wrapper.value = prop.value
            properties[wrapper] = op()
        }
    }

    private val dirtyListener: ChangeListener<Any> = ChangeListener { property, oldValue, newValue ->
        if (dirtyProperties.contains(property)) {
            val sourceValue = properties[property]!!.value
            if (sourceValue == newValue) dirtyProperties.remove(property)
        } else {
            dirtyProperties.add(property)
        }
        updateDirtyState()
    }

    private fun updateDirtyState() {
        val dirtyState = dirtyProperties.isNotEmpty()
        if (dirtyState != dirtyProperty.value) dirtyProperty.value = dirtyState
    }

    fun isDirty(): Boolean = dirtyProperty.value

    fun commit() {
        properties.forEach {
            it.value.value = it.key.value
        }
        dirtyProperties.clear()
        updateDirtyState()
    }

    fun rollback() {
        properties.forEach { it.key.value = it.value.value }
    }

}