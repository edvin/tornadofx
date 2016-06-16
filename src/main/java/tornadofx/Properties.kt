package tornadofx

import javafx.beans.property.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.*

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
    @Suppress("UNCHECKED_CAST")
    val delegate = field.get(this) as PropertyDelegate<T>
    return delegate.fxProperty as ObjectProperty<T>
}

/**
 * Convert an owner instance and a corresponding property reference into an observable
 */
fun <S, T> observable(owner: S, prop: KMutableProperty1<S, T>): ObjectProperty<T> {
    return object : SimpleObjectProperty<T>(owner, prop.name) {
        override fun get() = prop.get(owner)
        override fun set(v: T) = prop.set(owner, v)
    }
}

/**
 * Convert an owner instance and a corresponding property reference into a readonly observable
 */
fun <S, T> observable(owner: S, prop: KProperty1<S, T>): ReadOnlyObjectProperty<T> {
    return object : ReadOnlyObjectWrapper<T>(owner, prop.name) {
        override fun get() = prop.get(owner)
    }
}

/**
 * Convert an bean instance and a corresponding getter/setter reference into a writable observable.
 *
 * Example: val observableName = observable(myPojo, MyPojo::getName, MyPojo::setName)
 */
fun <S : Any, T> observable(bean: S, getter: KFunction<T>, setter: KFunction2<S, T, Unit>): PojoProperty<T> {
    val propName = getter.name.substring(3).let { it.first().toLowerCase() + it.substring(1) }

    return object : PojoProperty<T>(bean, propName) {
        override fun get() = getter.call(bean)
        override fun set(newValue: T) {
            setter.invoke(bean, newValue)
        }
    }
}

@JvmName("pojoObservable")
fun <S : Any, T> S.observable(getter: KFunction<T>, setter: KFunction2<S, T, Unit>): PojoProperty<T> =
        observable(this, getter, setter)

open class PojoProperty<T>(bean: Any, propName: String) : SimpleObjectProperty<T>(bean, propName) {
    fun refresh() {
        fireValueChangedEvent()
    }
}

@Suppress("UNCHECKED_CAST")
fun <S : Any, T : Any> observable(bean: S, propName: String, propType: KClass<T>): PojoProperty<T> {
    val suffix = propName.first().toUpperCase() + propName.substring(1)

    val getter = bean.javaClass.getDeclaredMethod("get$suffix")
    val setter = bean.javaClass.getDeclaredMethod("set$suffix", propType.java)

    return object : PojoProperty<T>(bean, propName) {
        override fun get() = getter.invoke(bean) as T
        override fun set(newValue: T) { setter.invoke(bean, newValue) }
    }

}

@JvmName("pojoObservable")
inline fun <reified T : Any> T.observable(propName: String) =
    observable(this, propName, T::class)

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
        @Suppress("UNCHECKED_CAST")
        return _value as T
    }

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        synchronized(this) {
            if (initialized) {
                throw Exception("Value has already been assigned!")
            }
            _value = value
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
        @Suppress("UNCHECKED_CAST")
        return _value as T
    }

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (initialized) {
            throw Exception("Value has already been assigned!")
        }
        _value = value
        initialized = true
    }
    override fun isInitialized() = initialized
}