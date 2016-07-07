package tornadofx

import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction2
import kotlin.reflect.KMutableProperty1

open class ViewModel {
    private val properties = FXCollections.observableHashMap<Property<*>, Property<*>>()
    private val dirtyProperties = FXCollections.observableHashMap<Property<*>, Property<*>>()

    // Wrap Kotlin getter/setter property (var)
    fun <S, T> S.wrap(prop: KMutableProperty1<S, T>): Property<T> =
        observable(this, prop).wrap()

    // Wrap POJO Property
    fun <S : Any, T> S.wrap(getter: KFunction<T>, setter: KFunction2<S, T, Unit>): Property<T>
            = observable(getter, setter).wrap()

    // Wrap JavaFX Property
    fun <S : Property<T>, T> S.wrap(): Property<T> {
        val wrapper = SimpleObjectProperty<T>(this.value)
        properties[wrapper] = this
        return wrapper
    }

    fun commit() {
        properties.forEach { it.value.value = it.key.value }
    }

    fun rollback() {
        properties.forEach { it.key.value = it.value.value }
    }

}