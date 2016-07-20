package tornadofx

import javafx.beans.Observable
import javafx.beans.binding.*
import javafx.beans.property.*
import javafx.beans.value.*
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.Callable
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
    val field = javaClass.findFieldByName("${prop.name}\$delegate")
            ?: throw IllegalArgumentException("No delegate field with name '${prop.name}' found")

    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val delegate = field.get(this) as PropertyDelegate<T>
    return delegate.fxProperty as ObjectProperty<T>
}

fun Class<*>.findFieldByName(name: String): Field? {
    val field = (declaredFields + fields).find { it.name == name }
    if (field != null) return field
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    if (superclass == java.lang.Object::class.java) return null
    return superclass.findFieldByName(name)
}

fun Class<*>.findMethodByName(name: String): Method? {
    val method = (declaredMethods + methods).find { it.name == name }
    if (method != null) return method
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    if (superclass == java.lang.Object::class.java) return null
    return superclass.findMethodByName(name)
}

/**
 * Convert an owner instance and a corresponding property reference into an observable
 */
fun <S, T> S.observable(prop: KMutableProperty1<S, T>): ObjectProperty<T> {
    val owner = this
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
        override fun set(newValue: T) {
            setter.invoke(bean, newValue)
        }
    }

}

@JvmName("pojoObservable")
inline fun <reified T : Any> Any.observable(propName: String) =
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

private class SynchronizedSingleAssign<T> : SingleAssign<T> {

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

private class UnsynchronizedSingleAssign<T> : SingleAssign<T> {

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

operator fun <T> ObservableValue<T>.getValue(thisRef: Any, property: KProperty<*>) = value
operator fun <T> Property<T?>.setValue(thisRef: Any, property: KProperty<*>, value: T?) {
    this.value = value
}

operator fun ObservableDoubleValue.getValue(thisRef: Any, property: KProperty<*>): Double = value.toDouble()
operator fun DoubleProperty.setValue(thisRef: Any, property: KProperty<*>, value: Double) {
    this.value = value
}

operator fun ObservableFloatValue.getValue(thisRef: Any, property: KProperty<*>): Float = value.toFloat()
operator fun FloatProperty.setValue(thisRef: Any, property: KProperty<*>, value: Float) {
    this.value = value
}

operator fun ObservableLongValue.getValue(thisRef: Any, property: KProperty<*>): Long = value.toLong()
operator fun LongProperty.setValue(thisRef: Any, property: KProperty<*>, value: Long) {
    this.value = value
}

operator fun ObservableIntegerValue.getValue(thisRef: Any, property: KProperty<*>): Int = value.toInt()
operator fun IntegerProperty.setValue(thisRef: Any, property: KProperty<*>, value: Int) {
    this.value = value
}

operator fun ObservableBooleanValue.getValue(thisRef: Any, property: KProperty<*>): Boolean = value
operator fun BooleanProperty.setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
    this.value = value
}

fun <T> Property<T>.integerBinding(vararg dependencies: Observable, op: (T?) -> Int): IntegerBinding
        = Bindings.createIntegerBinding(Callable { op(value) }, this, *dependencies)

fun <T> Property<T>.longBinding(vararg dependencies: Observable, op: (T?) -> Long): LongBinding
        = Bindings.createLongBinding(Callable { op(value) }, this, *dependencies)

fun <T> Property<T>.doubleBinding(vararg dependencies: Observable, op: (T?) -> Double): DoubleBinding
        = Bindings.createDoubleBinding(Callable { op(value) }, this, *dependencies)

fun <T> Property<T>.floatBinding(vararg dependencies: Observable, op: (T?) -> Float): FloatBinding
        = Bindings.createFloatBinding(Callable { op(value) }, this, *dependencies)

fun <T> Property<T>.booleanBinding(vararg dependencies: Observable, op: (T?) -> Boolean): BooleanBinding
        = Bindings.createBooleanBinding(Callable { op(value) }, this, *dependencies)

fun <T> Property<T>.stringBinding(vararg dependencies: Observable, op: (T?) -> String?): StringBinding
        = Bindings.createStringBinding(Callable { op(value) }, this, *dependencies)

fun <T, R> Property<T>.objectBinding(vararg dependencies: Observable, op: (T?) -> R?): Binding<R?>
        = Bindings.createObjectBinding(Callable { op(value) }, this, *dependencies)
