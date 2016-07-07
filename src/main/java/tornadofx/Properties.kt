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
    val suffix = propName.capitalize()

    val getter = bean.javaClass.getDeclaredMethod("get$suffix")
    val setter = bean.javaClass.getDeclaredMethod("set$suffix", propType.java)

    return object : PojoProperty<T>(bean, propName) {
        override fun get() = getter.invoke(bean) as T
        override fun set(newValue: T) { setter.invoke(bean, newValue) }
    }

}

@JvmName("pojoObservable")
inline fun <reified T : Any> Any.observable(propName: String) =
    observable(this, propName, T::class)

fun <T> singleAssign(threadSafeMode: Boolean = false): SingleAssign<T> = if (threadSafeMode) SafeSingleAssign() else UnsafeSingleAssign()

interface SingleAssign<T> {
    fun isInitialized(): Boolean
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T)
}

private class SafeSingleAssign<T> : SingleAssign<T> {
    @Volatile
    private var _value: Any? = null

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (!isInitialized()) throw Exception("Value has not been assigned yet!")
        @Suppress("UNCHECKED_CAST")
        return _value as T
    }

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (isInitialized()) throw Exception("Value has already been assigned!")
        _value = value
    }

    override fun isInitialized() = _value != null
}

private class UnsafeSingleAssign<T> : SingleAssign<T> {

    private var _value: Any? = null

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (!isInitialized()) throw Exception("Value has not been assigned yet!")
        @Suppress("UNCHECKED_CAST")
        return _value as T
    }

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (isInitialized()) throw Exception("Value has already been assigned!")

        _value = value
    }

    override fun isInitialized() = _value != null
}