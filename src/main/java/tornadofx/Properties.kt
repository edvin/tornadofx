package tornadofx

import javafx.beans.Observable
import javafx.beans.binding.*
import javafx.beans.property.*
import javafx.beans.property.adapter.JavaBeanObjectPropertyBuilder
import javafx.beans.value.*
import javafx.collections.ObservableList
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.Callable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.*
import kotlin.reflect.jvm.javaMethod

fun <T> ViewModel.property(value: T? = null) = PropertyDelegate(SimpleObjectProperty<T>(this, "ViewModelProperty", value))
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
fun <S, T> S.observable(prop: KMutableProperty1<S, T>) = observable(this, prop)

/**
 * Convert an owner instance and a corresponding property reference into an observable
 */
@JvmName("observableFromMutableProperty")
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

open class PojoProperty<T>(bean: Any, propName: String) : SimpleObjectProperty<T>(bean, propName) {
    fun refresh() {
        fireValueChangedEvent()
    }
}


@JvmName("pojoObservable")
inline fun <reified T : Any> Any.observable(propName: String) =
        this.observable(propertyName = propName, propertyType = T::class)

/**
 * Convert a pojo bean instance into a writable observable.
 *
 * Example: val observableName = myPojo.observable(MyPojo::getName, MyPojo::setName)
 *            or
 *          val observableName = myPojo.observable(MyPojo::getName)
 *            or
 *          val observableName = myPojo.observable("name")
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified S : Any, reified T : Any> S.observable(getter: KFunction<T>? = null, setter: KFunction2<S, T, Unit>? = null, propertyName: String? = null, @Suppress("UNUSED_PARAMETER") propertyType: KClass<T>? = null): ObjectProperty<T> {
    if (getter == null && propertyName == null) throw AssertionError("Either getter or propertyName must be provided")
    var propName = propertyName
    if (propName == null && getter != null) {
        propName = getter.name.substring(3).let { it.first().toLowerCase() + it.substring(1) }
    }
    return JavaBeanObjectPropertyBuilder.create().apply {
        bean(this@observable)
        this.name(propName)
        if (getter != null) this.getter(getter.javaMethod)
        if (setter != null) this.setter(setter.javaMethod)
    }.build() as ObjectProperty<T>
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

/**
 * Binds this property to an observable, automatically unbinding it before if already bound.
 */
fun <T> Property<T>.cleanBind(observable: ObservableValue<T>) {
    unbind()
    bind(observable)
}

operator fun <T> ObservableValue<T>.getValue(thisRef: Any, property: KProperty<*>) = value
operator fun <T> Property<T?>.setValue(thisRef: Any, property: KProperty<*>, value: T?) = setValue(value)

operator fun ObservableDoubleValue.getValue(thisRef: Any, property: KProperty<*>) = get()
operator fun DoubleProperty.setValue(thisRef: Any, property: KProperty<*>, value: Double) = set(value)

operator fun ObservableFloatValue.getValue(thisRef: Any, property: KProperty<*>) = get()
operator fun FloatProperty.setValue(thisRef: Any, property: KProperty<*>, value: Float) = set(value)

operator fun ObservableLongValue.getValue(thisRef: Any, property: KProperty<*>) = get()
operator fun LongProperty.setValue(thisRef: Any, property: KProperty<*>, value: Long) = set(value)

operator fun ObservableIntegerValue.getValue(thisRef: Any, property: KProperty<*>) = get()
operator fun IntegerProperty.setValue(thisRef: Any, property: KProperty<*>, value: Int) = set(value)

operator fun ObservableBooleanValue.getValue(thisRef: Any, property: KProperty<*>) = get()
operator fun BooleanProperty.setValue(thisRef: Any, property: KProperty<*>, value: Boolean) = set(value)

operator fun ObservableDoubleValue.plus(other: Number): DoubleProperty
        = SimpleDoubleProperty(get() + other.toDouble())

operator fun ObservableDoubleValue.plus(other: ObservableNumberValue): DoubleProperty
        = SimpleDoubleProperty(get() + other.doubleValue())


operator fun WritableDoubleValue.plusAssign(other: Number)
        = set(get() + other.toDouble())

operator fun WritableDoubleValue.plusAssign(other: ObservableNumberValue)
        = set(get() + other.doubleValue())


operator fun DoubleProperty.inc(): DoubleProperty {
    set(get() + 1.0)
    return this
}

operator fun ObservableDoubleValue.minus(other: Number): DoubleProperty
        = SimpleDoubleProperty(get() - other.toDouble())

operator fun ObservableDoubleValue.minus(other: ObservableNumberValue): DoubleProperty
        = SimpleDoubleProperty(get() - other.doubleValue())


operator fun WritableDoubleValue.minusAssign(other: Number)
        = set(get() - other.toDouble())

operator fun WritableDoubleValue.minusAssign(other: ObservableNumberValue)
        = set(get() - other.doubleValue())


operator fun ObservableDoubleValue.unaryMinus(): DoubleProperty
        = SimpleDoubleProperty(-get())


operator fun DoubleProperty.dec(): DoubleProperty {
    set(get() - 1.0)
    return this
}

operator fun ObservableDoubleValue.times(other: Number): DoubleProperty
        = SimpleDoubleProperty(get() * other.toDouble())

operator fun ObservableDoubleValue.times(other: ObservableNumberValue): DoubleProperty
        = SimpleDoubleProperty(get() * other.doubleValue())


operator fun WritableDoubleValue.timesAssign(other: Number)
        = set(get() * other.toDouble())

operator fun WritableDoubleValue.timesAssign(other: ObservableNumberValue)
        = set(get() * other.doubleValue())


operator fun ObservableDoubleValue.div(other: Number): DoubleProperty
        = SimpleDoubleProperty(get() / other.toDouble())

operator fun ObservableDoubleValue.div(other: ObservableNumberValue): DoubleProperty
        = SimpleDoubleProperty(get() / other.doubleValue())


operator fun WritableDoubleValue.divAssign(other: Number)
        = set(get() / other.toDouble())

operator fun WritableDoubleValue.divAssign(other: ObservableNumberValue)
        = set(get() / other.doubleValue())


operator fun ObservableDoubleValue.rem(other: Number): DoubleProperty
        = SimpleDoubleProperty(get() % other.toDouble())

operator fun ObservableDoubleValue.rem(other: ObservableNumberValue): DoubleProperty
        = SimpleDoubleProperty(get() % other.doubleValue())


operator fun WritableDoubleValue.remAssign(other: Number)
        = set(get() % other.toDouble())

operator fun WritableDoubleValue.remAssign(other: ObservableNumberValue)
        = set(get() % other.doubleValue())


operator fun ObservableDoubleValue.compareTo(other: Number): Int {
    if (get() > other.toDouble())
        return 1
    else if (get() < other.toDouble())
        return -1
    else
        return 0
}

operator fun ObservableDoubleValue.compareTo(other: ObservableNumberValue): Int {
    if (get() > other.doubleValue())
        return 1
    else if (get() < other.doubleValue())
        return -1
    else
        return 0
}


operator fun ObservableFloatValue.plus(other: Number): FloatProperty
        = SimpleFloatProperty(get() + other.toFloat())

operator fun ObservableFloatValue.plus(other: ObservableNumberValue): FloatProperty
        = SimpleFloatProperty(get() + other.floatValue())

operator fun WritableFloatValue.plusAssign(other: Number)
        = set(get() + other.toFloat())

operator fun WritableFloatValue.plusAssign(other: ObservableNumberValue)
        = set(get() + other.floatValue())

operator fun FloatProperty.inc(): FloatProperty {
    set(get() + 1.0f)
    return this
}

operator fun ObservableFloatValue.minus(other: Number): FloatProperty
        = SimpleFloatProperty(get() - other.toFloat())

operator fun ObservableFloatValue.minus(other: ObservableNumberValue): FloatProperty = SimpleFloatProperty(get() - other.floatValue())

operator fun WritableFloatValue.minusAssign(other: Number)
        = set(get() - other.toFloat())

operator fun WritableFloatValue.minusAssign(other: ObservableNumberValue)
        = set(get() - other.floatValue())

operator fun ObservableFloatValue.unaryMinus(): FloatProperty
        = SimpleFloatProperty(-get())

operator fun FloatProperty.dec(): FloatProperty {
    set(get() - 1.0f)
    return this
}

operator fun ObservableFloatValue.times(other: Number): FloatProperty
        = SimpleFloatProperty(get() * other.toFloat())

operator fun ObservableFloatValue.times(other: ObservableNumberValue): FloatProperty
        = SimpleFloatProperty(get() * other.floatValue())

operator fun WritableFloatValue.timesAssign(other: Number)
        = set(get() * other.toFloat())

operator fun WritableFloatValue.timesAssign(other: ObservableNumberValue)
        = set(get() * other.floatValue())


operator fun ObservableFloatValue.div(other: Number): FloatProperty
        = SimpleFloatProperty(get() / other.toFloat())

operator fun ObservableFloatValue.div(other: ObservableNumberValue): FloatProperty
        = SimpleFloatProperty(get() / other.floatValue())

operator fun WritableFloatValue.divAssign(other: Number)
        = set(get() / other.toFloat())

operator fun WritableFloatValue.divAssign(other: ObservableNumberValue)
        = set(get() / other.floatValue())


operator fun ObservableFloatValue.rem(other: Number): FloatProperty
        = SimpleFloatProperty(get() % other.toFloat())

operator fun ObservableFloatValue.rem(other: ObservableNumberValue): FloatProperty
        = SimpleFloatProperty(get() % other.floatValue())

operator fun WritableFloatValue.remAssign(other: Number)
        = set(get() % other.toFloat())

operator fun WritableFloatValue.remAssign(other: ObservableNumberValue)
        = set(get() % other.floatValue())

operator fun ObservableFloatValue.compareTo(other: Number): Int {
    if (get() > other.toFloat())
        return 1
    else if (get() < other.toFloat())
        return -1
    else
        return 0
}

operator fun ObservableFloatValue.compareTo(other: ObservableNumberValue): Int {
    if (get() > other.floatValue())
        return 1
    else if (get() < other.floatValue())
        return -1
    else
        return 0
}


operator fun ObservableIntegerValue.plus(other: Int): IntegerProperty
        = SimpleIntegerProperty(get() + other)

operator fun ObservableIntegerValue.plus(other: Long): LongProperty
        = SimpleLongProperty(get() + other)

operator fun ObservableIntegerValue.plus(other: Float): FloatProperty
        = SimpleFloatProperty(get() + other)

operator fun ObservableIntegerValue.plus(other: Double): DoubleProperty
        = SimpleDoubleProperty(get() + other)

operator fun ObservableIntegerValue.plus(other: ObservableIntegerValue): IntegerProperty
        = SimpleIntegerProperty(get() + other.get())

operator fun ObservableIntegerValue.plus(other: ObservableLongValue): LongProperty
        = SimpleLongProperty(get() + other.get())

operator fun ObservableIntegerValue.plus(other: ObservableFloatValue): FloatProperty
        = SimpleFloatProperty(get() + other.get())

operator fun ObservableIntegerValue.plus(other: ObservableDoubleValue): DoubleProperty
        = SimpleDoubleProperty(get() + other.get())


operator fun WritableIntegerValue.plusAssign(other: Number)
        = set(get() + other.toInt())

operator fun WritableIntegerValue.plusAssign(other: ObservableNumberValue)
        = set(get() + other.intValue())


operator fun IntegerProperty.inc(): IntegerProperty {
    set(get() + 1)
    return this
}

operator fun ObservableIntegerValue.minus(other: Int): IntegerProperty
        = SimpleIntegerProperty(get() - other)

operator fun ObservableIntegerValue.minus(other: Long): LongProperty
        = SimpleLongProperty(get() - other)

operator fun ObservableIntegerValue.minus(other: Float): FloatProperty
        = SimpleFloatProperty(get() - other)

operator fun ObservableIntegerValue.minus(other: Double): DoubleProperty
        = SimpleDoubleProperty(get() - other)

operator fun ObservableIntegerValue.minus(other: ObservableIntegerValue): IntegerProperty
        = SimpleIntegerProperty(get() - other.get())

operator fun ObservableIntegerValue.minus(other: ObservableLongValue): LongProperty
        = SimpleLongProperty(get() - other.get())

operator fun ObservableIntegerValue.minus(other: ObservableFloatValue): FloatProperty
        = SimpleFloatProperty(get() - other.get())

operator fun ObservableIntegerValue.minus(other: ObservableDoubleValue): DoubleProperty
        = SimpleDoubleProperty(get() - other.get())


operator fun WritableIntegerValue.minusAssign(other: Number)
        = set(get() - other.toInt())

operator fun WritableIntegerValue.minusAssign(other: ObservableNumberValue)
        = set(get() - other.intValue())


operator fun ObservableIntegerValue.unaryMinus(): IntegerProperty
        = SimpleIntegerProperty(-get())


operator fun IntegerProperty.dec(): IntegerProperty {
    set(get() - 1)
    return this
}

operator fun ObservableIntegerValue.times(other: Int): IntegerProperty
        = SimpleIntegerProperty(get() * other)

operator fun ObservableIntegerValue.times(other: Long): LongProperty
        = SimpleLongProperty(get() * other)

operator fun ObservableIntegerValue.times(other: Float): FloatProperty
        = SimpleFloatProperty(get() * other)

operator fun ObservableIntegerValue.times(other: Double): DoubleProperty
        = SimpleDoubleProperty(get() * other)

operator fun ObservableIntegerValue.times(other: ObservableIntegerValue): IntegerProperty
        = SimpleIntegerProperty(get() * other.get())

operator fun ObservableIntegerValue.times(other: ObservableLongValue): LongProperty
        = SimpleLongProperty(get() * other.get())

operator fun ObservableIntegerValue.times(other: ObservableFloatValue): FloatProperty
        = SimpleFloatProperty(get() * other.get())

operator fun ObservableIntegerValue.times(other: ObservableDoubleValue): DoubleProperty
        = SimpleDoubleProperty(get() * other.get())


operator fun WritableIntegerValue.timesAssign(other: Number)
        = set(get() * other.toInt())

operator fun WritableIntegerValue.timesAssign(other: ObservableNumberValue)
        = set(get() * other.intValue())


operator fun ObservableIntegerValue.div(other: Int): IntegerProperty
        = SimpleIntegerProperty(get() / other)

operator fun ObservableIntegerValue.div(other: Long): LongProperty
        = SimpleLongProperty(get() / other)

operator fun ObservableIntegerValue.div(other: Float): FloatProperty
        = SimpleFloatProperty(get() / other)

operator fun ObservableIntegerValue.div(other: Double): DoubleProperty
        = SimpleDoubleProperty(get() / other)

operator fun ObservableIntegerValue.div(other: ObservableIntegerValue): IntegerProperty
        = SimpleIntegerProperty(get() / other.get())

operator fun ObservableIntegerValue.div(other: ObservableLongValue): LongProperty
        = SimpleLongProperty(get() / other.get())

operator fun ObservableIntegerValue.div(other: ObservableFloatValue): FloatProperty
        = SimpleFloatProperty(get() / other.get())

operator fun ObservableIntegerValue.div(other: ObservableDoubleValue): DoubleProperty
        = SimpleDoubleProperty(get() / other.get())


operator fun WritableIntegerValue.divAssign(other: Number)
        = set(get() / other.toInt())

operator fun WritableIntegerValue.divAssign(other: ObservableNumberValue)
        = set(get() / other.intValue())


operator fun ObservableIntegerValue.rem(other: Int): IntegerProperty
        = SimpleIntegerProperty(get() % other)

operator fun ObservableIntegerValue.rem(other: Long): LongProperty
        = SimpleLongProperty(get() % other)

operator fun ObservableIntegerValue.rem(other: Float): FloatProperty
        = SimpleFloatProperty(get() % other)

operator fun ObservableIntegerValue.rem(other: Double): DoubleProperty
        = SimpleDoubleProperty(get() % other)

operator fun ObservableIntegerValue.rem(other: ObservableIntegerValue): IntegerProperty
        = SimpleIntegerProperty(get() % other.get())

operator fun ObservableIntegerValue.rem(other: ObservableLongValue): LongProperty
        = SimpleLongProperty(get() % other.get())

operator fun ObservableIntegerValue.rem(other: ObservableFloatValue): FloatProperty
        = SimpleFloatProperty(get() % other.get())

operator fun ObservableIntegerValue.rem(other: ObservableDoubleValue): DoubleProperty
        = SimpleDoubleProperty(get() % other.get())


operator fun WritableIntegerValue.remAssign(other: Number)
        = set(get() % other.toInt())

operator fun WritableIntegerValue.remAssign(other: ObservableNumberValue)
        = set(get() % other.intValue())


operator fun ObservableIntegerValue.rangeTo(other: ObservableIntegerValue): Sequence<IntegerProperty> {
    val sequence = mutableListOf<IntegerProperty>()
    for (i in get()..other.get()) {
        sequence += SimpleIntegerProperty(i)
    }
    return sequence.asSequence()
}

operator fun ObservableIntegerValue.rangeTo(other: Int): Sequence<IntegerProperty> {
    val sequence = mutableListOf<IntegerProperty>()
    for (i in get()..other) {
        sequence += SimpleIntegerProperty(i)
    }
    return sequence.asSequence()
}

operator fun ObservableIntegerValue.rangeTo(other: ObservableLongValue): Sequence<LongProperty> {
    val sequence = mutableListOf<LongProperty>()
    for (i in get()..other.get()) {
        sequence += SimpleLongProperty(i)
    }
    return sequence.asSequence()
}

operator fun ObservableIntegerValue.rangeTo(other: Long): Sequence<LongProperty> {
    val sequence = mutableListOf<LongProperty>()
    for (i in get()..other) {
        sequence += SimpleLongProperty(i)
    }
    return sequence.asSequence()
}


operator fun ObservableIntegerValue.compareTo(other: Number): Int {
    if (get() > other.toDouble())
        return 1
    else if (get() < other.toDouble())
        return -1
    else
        return 0
}

operator fun ObservableIntegerValue.compareTo(other: ObservableNumberValue): Int {
    if (get() > other.doubleValue())
        return 1
    else if (get() < other.doubleValue())
        return -1
    else
        return 0
}


operator fun ObservableLongValue.plus(other: Int): LongProperty
        = SimpleLongProperty(get() + other.toLong())

operator fun ObservableLongValue.plus(other: Long): LongProperty
        = SimpleLongProperty(get() + other)

operator fun ObservableLongValue.plus(other: Double): DoubleProperty
        = SimpleDoubleProperty(get() + other)

operator fun ObservableLongValue.plus(other: Float): FloatProperty
        = SimpleFloatProperty(get() + other)

operator fun ObservableLongValue.plus(other: ObservableIntegerValue): LongProperty
        = SimpleLongProperty(get() + other.intValue())

operator fun ObservableLongValue.plus(other: ObservableLongValue): LongProperty
        = SimpleLongProperty(get() + other.longValue())

operator fun ObservableLongValue.plus(other: ObservableDoubleValue): DoubleProperty
        = SimpleDoubleProperty(get() + other.doubleValue())

operator fun ObservableLongValue.plus(other: ObservableFloatValue): FloatProperty
        = SimpleFloatProperty(get() + other.floatValue())


operator fun WritableLongValue.plusAssign(other: Number)
        = set(get() + other.toLong())

operator fun WritableLongValue.plusAssign(other: ObservableNumberValue)
        = set(get() + other.longValue())


operator fun LongProperty.inc(): LongProperty {
    set(get() + 1)
    return this
}

operator fun ObservableLongValue.minus(other: Int): LongProperty
        = SimpleLongProperty(get() - other.toLong())

operator fun ObservableLongValue.minus(other: Long): LongProperty
        = SimpleLongProperty(get() - other)

operator fun ObservableLongValue.minus(other: Double): DoubleProperty
        = SimpleDoubleProperty(get() - other)

operator fun ObservableLongValue.minus(other: Float): FloatProperty
        = SimpleFloatProperty(get() - other)

operator fun ObservableLongValue.minus(other: ObservableIntegerValue): LongProperty
        = SimpleLongProperty(get() - other.intValue())

operator fun ObservableLongValue.minus(other: ObservableLongValue): LongProperty
        = SimpleLongProperty(get() - other.longValue())

operator fun ObservableLongValue.minus(other: ObservableDoubleValue): DoubleProperty
        = SimpleDoubleProperty(get() - other.doubleValue())

operator fun ObservableLongValue.minus(other: ObservableFloatValue): FloatProperty
        = SimpleFloatProperty(get() - other.floatValue())


operator fun WritableLongValue.minusAssign(other: Number)
        = set(get() - other.toLong())

operator fun WritableLongValue.minusAssign(other: ObservableNumberValue)
        = set(get() - other.longValue())


operator fun ObservableLongValue.unaryMinus(): LongProperty
        = SimpleLongProperty(-get())


operator fun LongProperty.dec(): LongProperty {
    set(get() - 1)
    return this
}

operator fun ObservableLongValue.times(other: Int): LongProperty
        = SimpleLongProperty(get() * other.toLong())

operator fun ObservableLongValue.times(other: Long): LongProperty
        = SimpleLongProperty(get() * other)

operator fun ObservableLongValue.times(other: Double): DoubleProperty
        = SimpleDoubleProperty(get() * other)

operator fun ObservableLongValue.times(other: Float): FloatProperty
        = SimpleFloatProperty(get() * other)

operator fun ObservableLongValue.times(other: ObservableIntegerValue): LongProperty
        = SimpleLongProperty(get() * other.intValue())

operator fun ObservableLongValue.times(other: ObservableLongValue): LongProperty
        = SimpleLongProperty(get() * other.longValue())

operator fun ObservableLongValue.times(other: ObservableDoubleValue): DoubleProperty
        = SimpleDoubleProperty(get() * other.doubleValue())

operator fun ObservableLongValue.times(other: ObservableFloatValue): FloatProperty
        = SimpleFloatProperty(get() * other.floatValue())


operator fun WritableLongValue.timesAssign(other: Number)
        = set(get() * other.toLong())

operator fun WritableLongValue.timesAssign(other: ObservableNumberValue)
        = set(get() * other.longValue())


operator fun ObservableLongValue.div(other: Int): LongProperty
        = SimpleLongProperty(get() / other.toLong())

operator fun ObservableLongValue.div(other: Long): LongProperty
        = SimpleLongProperty(get() / other)

operator fun ObservableLongValue.div(other: Double): DoubleProperty
        = SimpleDoubleProperty(get() / other)

operator fun ObservableLongValue.div(other: Float): FloatProperty
        = SimpleFloatProperty(get() / other)

operator fun ObservableLongValue.div(other: ObservableIntegerValue): LongProperty
        = SimpleLongProperty(get() / other.intValue())

operator fun ObservableLongValue.div(other: ObservableLongValue): LongProperty
        = SimpleLongProperty(get() / other.longValue())

operator fun ObservableLongValue.div(other: ObservableDoubleValue): DoubleProperty
        = SimpleDoubleProperty(get() / other.doubleValue())

operator fun ObservableLongValue.div(other: ObservableFloatValue): FloatProperty
        = SimpleFloatProperty(get() / other.floatValue())


operator fun WritableLongValue.divAssign(other: Number)
        = set(get() / other.toLong())

operator fun WritableLongValue.divAssign(other: ObservableNumberValue)
        = set(get() / other.longValue())


operator fun ObservableLongValue.rem(other: Int): LongProperty
        = SimpleLongProperty(get() % other.toLong())

operator fun ObservableLongValue.rem(other: Long): LongProperty
        = SimpleLongProperty(get() % other)

operator fun ObservableLongValue.rem(other: Double): DoubleProperty
        = SimpleDoubleProperty(get() % other)

operator fun ObservableLongValue.rem(other: Float): FloatProperty
        = SimpleFloatProperty(get() % other)

operator fun ObservableLongValue.rem(other: ObservableIntegerValue): LongProperty
        = SimpleLongProperty(get() % other.intValue())

operator fun ObservableLongValue.rem(other: ObservableLongValue): LongProperty
        = SimpleLongProperty(get() % other.longValue())

operator fun ObservableLongValue.rem(other: ObservableDoubleValue): DoubleProperty
        = SimpleDoubleProperty(get() % other.doubleValue())

operator fun ObservableLongValue.rem(other: ObservableFloatValue): FloatProperty
        = SimpleFloatProperty(get() % other.floatValue())


operator fun WritableLongValue.remAssign(other: Number)
        = set(get() % other.toLong())

operator fun WritableLongValue.remAssign(other: ObservableNumberValue)
        = set(get() % other.longValue())


operator fun ObservableLongValue.rangeTo(other: ObservableLongValue): Sequence<LongProperty> {
    val sequence = mutableListOf<LongProperty>()
    for (i in get()..other.get()) {
        sequence += SimpleLongProperty(i)
    }
    return sequence.asSequence()
}

operator fun ObservableLongValue.rangeTo(other: Long): Sequence<LongProperty> {
    val sequence = mutableListOf<LongProperty>()
    for (i in get()..other) {
        sequence += SimpleLongProperty(i)
    }
    return sequence.asSequence()
}

operator fun ObservableLongValue.rangeTo(other: ObservableIntegerValue): Sequence<LongProperty> {
    val sequence = mutableListOf<LongProperty>()
    for (i in get()..other.get()) {
        sequence += SimpleLongProperty(i)
    }
    return sequence.asSequence()
}

operator fun ObservableLongValue.rangeTo(other: Int): Sequence<LongProperty> {
    val sequence = mutableListOf<LongProperty>()
    for (i in get()..other) {
        sequence += SimpleLongProperty(i)
    }
    return sequence.asSequence()
}

operator fun ObservableLongValue.compareTo(other: Number): Int {
    if (get() > other.toDouble())
        return 1
    else if (get() < other.toDouble())
        return -1
    else
        return 0
}

operator fun ObservableLongValue.compareTo(other: ObservableNumberValue): Int {
    if (get() > other.doubleValue())
        return 1
    else if (get() < other.doubleValue())
        return -1
    else
        return 0
}


fun <T> ObservableValue<T>.integerBinding(vararg dependencies: Observable, op: (T?) -> Int): IntegerBinding
        = Bindings.createIntegerBinding(Callable { op(value) }, this, *dependencies)

fun <T : Any> integerBinding(receiver: T, vararg dependencies: Observable, op: T.() -> Int): IntegerBinding
        = Bindings.createIntegerBinding(Callable { receiver.op() }, *createObservableArray(receiver, *dependencies))

fun <T> ObservableValue<T>.longBinding(vararg dependencies: Observable, op: (T?) -> Long): LongBinding
        = Bindings.createLongBinding(Callable { op(value) }, this, *dependencies)

fun <T : Any> longBinding(receiver: T, vararg dependencies: Observable, op: T.() -> Long): LongBinding
        = Bindings.createLongBinding(Callable { receiver.op() }, *createObservableArray(receiver, *dependencies))

fun <T> ObservableValue<T>.doubleBinding(vararg dependencies: Observable, op: (T?) -> Double): DoubleBinding
        = Bindings.createDoubleBinding(Callable { op(value) }, this, *dependencies)

fun <T : Any> doubleBinding(receiver: T, vararg dependencies: Observable, op: T.() -> Double): DoubleBinding
        = Bindings.createDoubleBinding(Callable { receiver.op() }, *createObservableArray(receiver, *dependencies))

fun <T> ObservableValue<T>.floatBinding(vararg dependencies: Observable, op: (T?) -> Float): FloatBinding
        = Bindings.createFloatBinding(Callable { op(value) }, this, *dependencies)

fun <T : Any> floatBinding(receiver: T, vararg dependencies: Observable, op: T.() -> Float): FloatBinding
        = Bindings.createFloatBinding(Callable { receiver.op() }, *createObservableArray(receiver, *dependencies))

fun <T> ObservableValue<T>.booleanBinding(vararg dependencies: Observable, op: (T?) -> Boolean): BooleanBinding =
        Bindings.createBooleanBinding(Callable { op(value) }, this, *dependencies)

fun <T : Any> booleanBinding(receiver: T, vararg dependencies: Observable, op: T.() -> Boolean): BooleanBinding
        = Bindings.createBooleanBinding(Callable { receiver.op() }, *createObservableArray(receiver, *dependencies))

/**
 * A Boolean binding that tracks all items in an observable list and create an observable boolean
 * value by anding together an observable boolean representing each element in the observable list.
 * Whenever the list changes, the binding is updated as well
 */
fun <T : Any> booleanListBinding(list: ObservableList<T>, itemToBooleanExpr: T.() -> BooleanExpression): BooleanExpression {
    val facade = SimpleBooleanProperty()
    fun rebind() {
        if (list.isEmpty()) {
            facade.unbind()
            facade.value = false
        } else {
            facade.cleanBind(list.map(itemToBooleanExpr).reduce { a, b -> a.and(b) })
        }
    }
    list.onChange { rebind() }
    rebind()
    return facade
}

fun <T> ObservableValue<T>.stringBinding(vararg dependencies: Observable, op: (T?) -> String?): StringBinding
        = Bindings.createStringBinding(Callable { op(value) }, this, *dependencies)

fun <T : Any> stringBinding(receiver: T, vararg dependencies: Observable, op: T.() -> String?): StringBinding =
        Bindings.createStringBinding(Callable { receiver.op() }, *createObservableArray(receiver, *dependencies))

fun <T, R> ObservableValue<T>.objectBinding(vararg dependencies: Observable, op: (T?) -> R?): Binding<R?>
        = Bindings.createObjectBinding(Callable { op(value) }, this, *dependencies)

fun <T : Any, R> objectBinding(receiver: T, vararg dependencies: Observable, op: T.() -> R?): ObjectBinding<R?>
        = Bindings.createObjectBinding(Callable { receiver.op() }, *createObservableArray(receiver, *dependencies))

fun <T : Any, R> nonNullObjectBinding(receiver: T, vararg dependencies: Observable, op: T.() -> R): ObjectBinding<R>
        = Bindings.createObjectBinding(Callable { receiver.op() }, *createObservableArray(receiver, *dependencies))

private fun <T> createObservableArray(receiver: T, vararg dependencies: Observable): Array<out Observable> =
        if (receiver is Observable) arrayOf(receiver, *dependencies) else dependencies

/**
 * Assign the value from the creator to this WritableValue if and only if it is currently null
 */
fun <T> WritableValue<T>.assignIfNull(creator: () -> T) {
    if (value == null) value = creator()
}

fun String?.toProperty() = SimpleStringProperty(this ?: "")
fun Double?.toProperty() = SimpleDoubleProperty(this ?: 0.0)
fun Float?.toProperty() = SimpleFloatProperty(this ?: 0.0F)
fun Long?.toProperty() = SimpleLongProperty(this ?: 0L)
fun Boolean?.toProperty() = SimpleBooleanProperty(this ?: false)
fun <T : Any> T?.toProperty() = SimpleObjectProperty<T>(this)