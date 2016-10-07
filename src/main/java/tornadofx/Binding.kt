@file:Suppress("UNCHECKED_CAST", "CAST_NEVER_SUCCEEDS")

package tornadofx

import javafx.beans.binding.Bindings
import javafx.beans.property.Property
import javafx.beans.property.StringProperty
import javafx.scene.control.*
import javafx.scene.text.Text
import javafx.util.StringConverter
import javafx.util.converter.*
import java.math.BigDecimal
import java.math.BigInteger
import java.text.Format
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import java.util.concurrent.Callable
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

inline fun <reified T> ComboBoxBase<T>.bind(property: Property<T>, readonly: Boolean = false) =
    if (readonly) valueProperty().bind(property) else valueProperty().bindBidirectional(property)

fun DatePicker.bind(property: Property<LocalDate>, readonly: Boolean = false) =
    if (readonly) valueProperty().bind(property) else valueProperty().bindBidirectional(property)

fun ProgressIndicator.bind(property: Property<Number>, readonly: Boolean = false) =
    if (readonly) progressProperty().bind(property) else progressProperty().bindBidirectional(property)

inline fun <reified T> ChoiceBox<T>.bind(property: Property<T>, readonly: Boolean = false) =
    if (readonly) valueProperty().bind(property) else valueProperty().bindBidirectional(property)

fun CheckBox.bind(property: Property<Boolean>, readonly: Boolean = false) =
    if (readonly) selectedProperty().bind(property) else selectedProperty().bindBidirectional(property)

fun Slider.bind(property: Property<Number>, readonly: Boolean = false) =
    if (readonly) valueProperty().bind(property) else valueProperty().bindBidirectional(property)

inline fun <reified T> Labeled.bind(property: Property<T>, readonly: Boolean = false, converter: StringConverter<T>? = null, format: Format? = null) =
        bindStringProperty(textProperty(), converter, format, property, readonly)

inline fun <reified T> TitledPane.bind(property: Property<T>, readonly: Boolean = false, converter: StringConverter<T>? = null, format: Format? = null) =
        bindStringProperty(textProperty(), converter, format, property, readonly)

inline fun <reified T> Text.bind(property: Property<T>, readonly: Boolean = false, converter: StringConverter<T>? = null, format: Format? = null) =
        bindStringProperty(textProperty(), converter, format, property, readonly)

inline fun <reified T> TextInputControl.bind(property: Property<T>, readonly: Boolean = false, converter: StringConverter<T>? = null, format: Format? = null) =
    bindStringProperty(textProperty(), converter, format, property, readonly)

inline fun <reified T> bindStringProperty(stringProperty: StringProperty, converter: StringConverter<T>?, format: Format?, property: Property<T>, readonly: Boolean) {
    if (stringProperty.isBound) stringProperty.unbind()

    if (T::class == String::class) {
        if (readonly)
            stringProperty.bind(property as Property<String>)
        else
            stringProperty.bindBidirectional(property as Property<String>)
    } else {
        val effectiveConverter = converter ?: getDefaultConverter<T>()
        if (readonly) {
            val toStringConverter = Callable {
                if (converter != null)
                    converter.toString(property.value)
                else if (format != null)
                    format.format(property.value)
                else property.value?.toString()
            }
            val stringBinding = Bindings.createStringBinding(toStringConverter, property)
            stringProperty.bind(stringBinding)
        } else {
            if (effectiveConverter != null) {
                stringProperty.bindBidirectional(property, effectiveConverter)
            } else if (format != null) {
                stringProperty.bindBidirectional(property, format)
            } else {
                throw IllegalArgumentException("Cannot convert from ${T::class} to String without an explicit converter or format")
            }
        }
    }
}

inline fun <reified T> getDefaultConverter(): StringConverter<T>? {
    val converter: StringConverter<out Any>? = when (Class.forName(T::class.jvmName)) {

        Int::class.javaObjectType -> IntegerStringConverter()
        Long::class.javaObjectType -> LongStringConverter()
        Double::class.javaObjectType -> DoubleStringConverter()
        Float::class.javaObjectType -> FloatStringConverter()
        Date::class.javaObjectType -> DateStringConverter()
        Boolean::class.javaObjectType -> BooleanStringConverter()

        BigDecimal::class -> BigDecimalStringConverter()
        BigInteger::class -> BigIntegerStringConverter()
        Number::class -> NumberStringConverter()
        LocalDate::class -> LocalDateStringConverter()
        LocalTime::class -> LocalTimeStringConverter()
        LocalDateTime::class -> LocalDateTimeStringConverter()

        else -> null
    }
    return if (converter != null) converter as StringConverter<T> else null
}