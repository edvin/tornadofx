package tornadofx

import javafx.beans.property.ObjectProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty

fun <T> property(value: T? = null) = PropertyDelegate(SimpleObjectProperty<T>(value))
fun <T> property(block: () -> Property<T>) = PropertyDelegate(block())

class PropertyDelegate<T>(val fxProperty: Property<T>) : ReadWriteProperty<Any, T> {

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return fxProperty.value
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        fxProperty.value = value
    }

}

fun <T> Any.getProperty(prop: KMutableProperty1<*, T>): ObjectProperty<T> {
    // avoid kotlin-reflect dependency
    val field = this.javaClass.getDeclaredField("${prop.name}\$delegate")
    field.isAccessible = true
    val delegate = field.get(this) as PropertyDelegate<T>
    return delegate.fxProperty as ObjectProperty<T>
}

enum class SingleAssignThreadSafetyMode {
    SYNCHRONIZED,
    NONE
}

fun <T> singleAssign(threadSafeyMode: SingleAssignThreadSafetyMode = SingleAssignThreadSafetyMode.SYNCHRONIZED): SingleAssign<T> =
    if (threadSafeyMode.equals(SingleAssignThreadSafetyMode.SYNCHRONIZED)) SynchronizedSingleAssign<T>() else UnsynchronizedSingleAssign<T>()

private object UNINITIALIZED_VALUE

interface SingleAssign<T> {
    fun isInitialized(): Boolean
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T)
}

private class SynchronizedSingleAssign<T>: SingleAssign<T> {

    @Volatile
    private var initialized = false

    @Volatile
    private var _value: Any? = UNINITIALIZED_VALUE

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (!initialized)
            throw Exception("Value has not been assigned yet!")
        return _value as T
    }

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        synchronized(this) {
            if (initialized) {
                throw Exception("Value has already been assigned!")
            }
            _value = value as T
            initialized = true
        }
    }
    override fun isInitialized() = initialized
}

private class UnsynchronizedSingleAssign<T>: SingleAssign<T> {

    private var initialized = false
    private var _value: Any? = UNINITIALIZED_VALUE

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (!initialized)
            throw Exception("Value has not been assigned yet!")
        return _value as T
    }

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (initialized) {
            throw Exception("Value has already been assigned!")
        }
        _value = value as T
        initialized = true
    }
    override fun isInitialized() = initialized
}
