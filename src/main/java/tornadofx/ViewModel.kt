package tornadofx

import javafx.beans.property.*
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ObservableSet
import javafx.scene.control.ListView
import javafx.scene.control.TableView

open class ViewModel {
    val properties = FXCollections.observableHashMap<Property<*>, () -> Property<*>>()
    val dirtyProperties = FXCollections.observableArrayList<ObservableValue<*>>()
    private val dirtyStateProperty = SimpleBooleanProperty(false)
    fun dirtyStateProperty() = dirtyStateProperty

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
            Int::class.javaPrimitiveType -> if (value != null) SimpleIntegerProperty(value as Int) else SimpleIntegerProperty()
            Long::class.javaPrimitiveType -> if (value != null) SimpleLongProperty(value as Long) else SimpleLongProperty()
            Double::class.javaPrimitiveType -> if (value != null) SimpleDoubleProperty(value as Double) else SimpleDoubleProperty()
            Float::class.javaPrimitiveType -> if (value != null) SimpleFloatProperty(value as Float) else SimpleFloatProperty()
            Boolean::class.javaPrimitiveType -> if (value != null) SimpleBooleanProperty(value as Boolean) else SimpleBooleanProperty()
            String::class -> if (value != null) SimpleStringProperty(value as String) else SimpleStringProperty()
            is ObservableList<*> -> if (value != null) SimpleListProperty(value as ObservableList<T>) else SimpleListProperty()
            is ObservableSet<*> -> if (value != null) SimpleSetProperty(value as ObservableSet<T>) else SimpleSetProperty()
            is List<*> -> if (value != null) SimpleListProperty((value as List<T>).observable()) else SimpleListProperty()
            is Set<*> -> if (value != null) SimpleSetProperty((value as Set<T>).observable()) else SimpleSetProperty()
            else -> if (value != null) SimpleObjectProperty(value) else SimpleObjectProperty()
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

    fun commit() {
        properties.forEach {
            it.value.invoke().value = it.key.value
        }
        clearDirtyState()
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

    private fun clearDirtyState() {
        dirtyProperties.clear()
        updateDirtyState()
    }
}

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
