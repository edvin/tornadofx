package tornadofx

import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T> property(value: T? = null) = PropertyDelegate(SimpleObjectProperty<T>(value))
fun <T> property(block: () -> Property<T>) = PropertyDelegate(block())

class PropertyDelegate<T>(private val fxProperty: Property<T>) : ReadWriteProperty<Any?, T?> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        return fxProperty.value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        fxProperty.value = value
    }

}
