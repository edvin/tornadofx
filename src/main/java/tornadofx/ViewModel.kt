package tornadofx

import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction2
import kotlin.reflect.KMutableProperty1

open class ViewModel {
    private val properties = FXCollections.observableHashMap<Property<*>, Property<*>>()
    private val dirtyProperties = FXCollections.observableArrayList<ObservableValue<*>>()
    val dirtyProperty = SimpleBooleanProperty(false)

    // Wrap Kotlin getter/setter property (var)
    fun <S, T> S.wrap(prop: KMutableProperty1<S, T>): Property<T> =
            observable(this, prop).wrap()

    // Wrap POJO Property
    fun <S : Any, T> S.wrap(getter: KFunction<T>, setter: KFunction2<S, T, Unit>): Property<T>
            = observable(getter, setter).wrap()

    // Wrap JavaFX Property - all the others end up here
    fun <S : Property<T>, T> S.wrap(): Property<T> {
        val wrapper = SimpleObjectProperty<T>(this.value)
        @Suppress("UNCHECKED_CAST")
        wrapper.addListener(dirtyListener as ChangeListener<in T>)
        properties[wrapper] = this
        return wrapper
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