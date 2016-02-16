package tornadofx

import javafx.beans.property.ObjectProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty

fun <T> property(value: T? = null) = PropertyDelegate(SimpleObjectProperty<T>(value))
fun <T> property(block: () -> Property<T>) = PropertyDelegate(block())

class PropertyDelegate<T>(val fxProperty: Property<T>) : ReadWriteProperty<Any?, T?> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        return fxProperty.value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        fxProperty.value = value
    }

}

fun <T> Any.fxProp(prop: KMutableProperty1<*, T>): ObjectProperty<T?> {
    // avoid kotlin-reflect dependency
    val field = this.javaClass.getDeclaredField("${prop.name}\$delegate")
    field.isAccessible = true
    val delegate = field.get(this) as PropertyDelegate<T>
    return delegate.fxProperty as ObjectProperty<T?>
}
