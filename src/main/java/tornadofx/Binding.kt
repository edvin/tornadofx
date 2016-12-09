@file:Suppress("UNCHECKED_CAST", "CAST_NEVER_SUCCEEDS")

package tornadofx

import javafx.beans.binding.Bindings
import javafx.beans.property.Property
import javafx.beans.property.StringProperty
import javafx.beans.value.ObservableValue
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

inline fun <reified T : Any> Labeled.bind(property: ObservableValue<T>, readonly: Boolean = false, converter: StringConverter<T>? = null, format: Format? = null) =
        bindStringProperty(textProperty(), converter, format, property, readonly)

inline fun <reified T : Any> TitledPane.bind(property: Property<T>, readonly: Boolean = false, converter: StringConverter<T>? = null, format: Format? = null) =
        bindStringProperty(textProperty(), converter, format, property, readonly)

inline fun <reified T : Any> Text.bind(property: Property<T>, readonly: Boolean = false, converter: StringConverter<T>? = null, format: Format? = null) =
        bindStringProperty(textProperty(), converter, format, property, readonly)

inline fun <reified T : Any> TextInputControl.bind(property: Property<T>, readonly: Boolean = false, converter: StringConverter<T>? = null, format: Format? = null) =
    bindStringProperty(textProperty(), converter, format, property, readonly)

inline fun <reified T : Any> bindStringProperty(stringProperty: StringProperty, converter: StringConverter<T>?, format: Format?, property: ObservableValue<T>, readonly: Boolean) {
    if (stringProperty.isBound) stringProperty.unbind()
    val effectiveReadonly = if (readonly) readonly else property !is Property<T>

    if (T::class == String::class) {
        if (effectiveReadonly)
            stringProperty.bind(property as ObservableValue<String>)
        else
            stringProperty.bindBidirectional(property as Property<String>)
    } else {
        val effectiveConverter = if (format != null) null else converter ?: getDefaultConverter<T>()
        if (effectiveReadonly) {
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
                stringProperty.bindBidirectional(property as Property<T>, effectiveConverter)
            } else if (format != null) {
                stringProperty.bindBidirectional(property as Property<T>, format)
            } else {
                throw IllegalArgumentException("Cannot convert from ${T::class} to String without an explicit converter or format")
            }
        }
    }
}

inline fun <reified T : Any> getDefaultConverter(): StringConverter<T>? {
    val converter: StringConverter<out Any>? = when (T::class.javaPrimitiveType ?: T::class) {
        Int::class.javaPrimitiveType -> IntegerStringConverter()
        Long::class.javaPrimitiveType -> LongStringConverter()
        Double::class.javaPrimitiveType -> DoubleStringConverter()
        Float::class.javaPrimitiveType -> FloatStringConverter()
        Date::class -> DateStringConverter()
        BigDecimal::class -> BigDecimalStringConverter()
        BigInteger::class -> BigIntegerStringConverter()
        Number::class -> NumberStringConverter()
        LocalDate::class -> LocalDateStringConverter()
        LocalTime::class -> LocalTimeStringConverter()
        LocalDateTime::class -> LocalDateTimeStringConverter()
        Boolean::class.javaPrimitiveType -> BooleanStringConverter() as StringConverter<T>
        else -> null
    }
    return if (converter != null) converter as StringConverter<T> else null
}