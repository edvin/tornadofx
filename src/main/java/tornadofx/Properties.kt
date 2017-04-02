package tornadofx

import javafx.beans.Observable
import javafx.beans.binding.*
import javafx.beans.property.*
import javafx.beans.property.adapter.JavaBeanObjectPropertyBuilder
import javafx.beans.value.*
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.Callable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.*
import kotlin.reflect.jvm.javaMethod

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
        this.observable( propertyName = propName, propertyType = T::class )

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
inline fun <reified S : Any, reified T : Any> S.observable( getter: KFunction<T>? = null, setter: KFunction2<S, T, Unit>? = null, propertyName: String? = null, @Suppress("UNUSED_PARAMETER")propertyType: KClass<T>? = null ): ObjectProperty<T> {
    if( getter == null && propertyName == null ) throw AssertionError("Either getter or propertyName must be provided")
    var propName = propertyName
    if( propName == null && getter != null ){
        propName = getter.name.substring(3).let { it.first().toLowerCase() + it.substring(1) }
    }
    return JavaBeanObjectPropertyBuilder.create().apply{
        bean( this@observable )
        this.name( propName )
        if( getter != null ) this.getter( getter.javaMethod )
        if( setter != null ) this.setter( setter.javaMethod )
    } .build() as ObjectProperty<T>
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
operator fun LongProperty.setValue(thisRef: Any, property: KProperty<*>, value: Long)  = set(value)

operator fun ObservableIntegerValue.getValue(thisRef: Any, property: KProperty<*>) = get()
operator fun IntegerProperty.setValue(thisRef: Any, property: KProperty<*>, value: Int) = set(value)

operator fun ObservableBooleanValue.getValue(thisRef: Any, property: KProperty<*>) = get()
operator fun BooleanProperty.setValue(thisRef: Any, property: KProperty<*>, value: Boolean) = set(value)

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

fun <T> ObservableValue<T>.stringBinding(vararg dependencies: Observable, op: (T?) -> String?): StringBinding
        = Bindings.createStringBinding(Callable { op(value) }, this, *dependencies)

fun <T : Any> stringBinding(receiver: T, vararg dependencies: Observable, op: T.() -> String?): StringBinding =
    Bindings.createStringBinding(Callable { receiver.op() }, *createObservableArray(receiver, *dependencies))

fun <T, R> ObservableValue<T>.objectBinding(vararg dependencies: Observable, op: (T?) -> R?): Binding<R?>
        = Bindings.createObjectBinding(Callable { op(value) }, this, *dependencies)

fun <T : Any, R> objectBinding(receiver: T, vararg dependencies: Observable, op: T.() -> R?): ObjectBinding<R?>
        = Bindings.createObjectBinding(Callable { receiver.op() }, *createObservableArray(receiver, *dependencies))

private fun <T> createObservableArray(receiver: T, vararg dependencies: Observable):Array<out Observable> =
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