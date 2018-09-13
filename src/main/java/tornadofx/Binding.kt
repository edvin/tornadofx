package tornadofx

import javafx.beans.binding.Bindings
import javafx.beans.binding.BooleanBinding
import javafx.beans.binding.BooleanExpression
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.StringProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
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

// ================================================================
// Value Bindings

fun <T> ComboBoxBase<T>.bind(property: ObservableValue<T>, readonly: Boolean = false): Unit =
    valueProperty().internalBind(property, readonly)

fun DatePicker.bind(property: ObservableValue<LocalDate>, readonly: Boolean = false): Unit =
    valueProperty().internalBind(property, readonly)

fun ProgressIndicator.bind(property: ObservableValue<Number>, readonly: Boolean = false): Unit =
    progressProperty().internalBind(property, readonly)

fun <T> ChoiceBox<T>.bind(property: ObservableValue<T>, readonly: Boolean = false): Unit =
    valueProperty().internalBind(property, readonly)

fun CheckBox.bind(property: ObservableValue<Boolean>, readonly: Boolean = false): Unit =
    selectedProperty().internalBind(property, readonly)

fun CheckMenuItem.bind(property: ObservableValue<Boolean>, readonly: Boolean = false): Unit =
    selectedProperty().internalBind(property, readonly)

fun Slider.bind(property: ObservableValue<Number>, readonly: Boolean = false): Unit =
    valueProperty().internalBind(property, readonly)

fun <T> Spinner<T>.bind(property: ObservableValue<T>, readonly: Boolean = false): Unit =
    valueFactory.valueProperty().internalBind(property, readonly)


private fun <T> Property<T>.internalBind(property: ObservableValue<T>, readonly: Boolean) {
    ViewModel.register(this, property)
    if (readonly || (property !is Property<*>)) bind(property) else bindBidirectional(property as Property<T>)
}


// ================================================================
// String Bindings

inline fun <reified S : T, reified T : Any> Labeled.bind(
    property: ObservableValue<S>,
    readonly: Boolean = false,
    converter: StringConverter<T>? = null,
    format: Format? = null
): Unit = bindStringProperty(textProperty(), converter, format, property, readonly)

inline fun <reified S : T, reified T : Any> TitledPane.bind(
    property: ObservableValue<S>,
    readonly: Boolean = false,
    converter: StringConverter<T>? = null,
    format: Format? = null
): Unit = bindStringProperty(textProperty(), converter, format, property, readonly)

inline fun <reified S : T, reified T : Any> Text.bind(
    property: ObservableValue<S>,
    readonly: Boolean = false,
    converter: StringConverter<T>? = null,
    format: Format? = null
): Unit = bindStringProperty(textProperty(), converter, format, property, readonly)

inline fun <reified S : T, reified T : Any> TextInputControl.bind(
    property: ObservableValue<S>,
    readonly: Boolean = false,
    converter: StringConverter<T>? = null,
    format: Format? = null
): Unit = bindStringProperty(textProperty(), converter, format, property, readonly)

@Suppress("UNCHECKED_CAST")
inline fun <reified S : T, reified T : Any> bindStringProperty(
    stringProperty: StringProperty,
    converter: StringConverter<T>?,
    format: Format?,
    property: ObservableValue<S>,
    readonly: Boolean
) {
    if (stringProperty.isBound) stringProperty.unbind()
    val effectiveReadonly = readonly || property !is Property<S> || S::class != T::class

    ViewModel.register(stringProperty, property)

    if (S::class == String::class) when {
        effectiveReadonly -> stringProperty.bind(property as ObservableValue<String>)
        else -> stringProperty.bindBidirectional(property as Property<String>)
    } else {
        val effectiveConverter = if (format != null) null else converter ?: getDefaultConverter<S>()
        if (effectiveReadonly) {
            val toStringConverter = Callable {
                when {
                    converter != null -> converter.toString(property.value)
                    format != null -> format.format(property.value)
                    else -> property.value?.toString()
                }
            }
            val stringBinding = Bindings.createStringBinding(toStringConverter, property)
            stringProperty.bind(stringBinding)
        } else when {
            effectiveConverter != null -> stringProperty.bindBidirectional(property as Property<S>, effectiveConverter as StringConverter<S>)
            format != null -> stringProperty.bindBidirectional(property as Property<S>, format)
            else -> throw IllegalArgumentException("Cannot convert from ${S::class} to String without an explicit converter or format")
        }
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any> getDefaultConverter(): StringConverter<T>? = when (T::class.javaPrimitiveType ?: T::class) {
    Int::class.javaPrimitiveType -> IntegerStringConverter()
    Long::class.javaPrimitiveType -> LongStringConverter()
    Float::class.javaPrimitiveType -> FloatStringConverter()
    Double::class.javaPrimitiveType -> DoubleStringConverter()
    Boolean::class.javaPrimitiveType -> BooleanStringConverter()
    Date::class -> DateStringConverter()
    BigInteger::class -> BigIntegerStringConverter()
    BigDecimal::class -> BigDecimalStringConverter()
    Number::class -> NumberStringConverter()
    LocalDate::class -> LocalDateStringConverter()
    LocalTime::class -> LocalTimeStringConverter()
    LocalDateTime::class -> LocalDateTimeStringConverter()
    else -> null
} as StringConverter<T>?


// ================================================================
// ObservableValue Utilities

fun ObservableValue<Boolean>.toBinding(): BooleanBinding = object : BooleanBinding() {
    init {
        super.bind(this@toBinding)
    }

    override fun dispose(): Unit = super.unbind(this@toBinding)
    override fun computeValue(): Boolean = this@toBinding.value
    override fun getDependencies(): ObservableList<*> = FXCollections.singletonObservableList(this@toBinding)
}

@Suppress("UNCHECKED_CAST")
fun <T, N> ObservableValue<T>.select(nested: (T) -> ObservableValue<N>): Property<N> {
    fun extractNested(): ObservableValue<N>? = value?.let(nested)

    var currentNested: ObservableValue<N>? = extractNested()

    return object : SimpleObjectProperty<N>() {
        val changeListener = ChangeListener<Any?> { _, _, _ ->
            invalidated()
            fireValueChangedEvent()
        }

        init {
            currentNested?.addListener(changeListener)
            this@select.addListener(changeListener)
        }

        override fun invalidated() {
            currentNested?.removeListener(changeListener)
            currentNested = extractNested()
            currentNested?.addListener(changeListener)
        }

        override fun get() = currentNested?.value

        override fun set(v: N?) {
            (currentNested as? WritableValue<N>)?.value = v
            super.set(v)
        }
    }
}

fun <T> ObservableValue<T>.selectBoolean(nested: (T) -> BooleanExpression): BooleanExpression {
    fun extractNested(): BooleanExpression = nested(value)

    var currentNested = extractNested()

    return object : SimpleBooleanProperty() {
        val changeListener = ChangeListener<Boolean> { _, _, _ ->
            currentNested = extractNested()
            fireValueChangedEvent()
        }

        init {
            this@selectBoolean.onChange {
                fireValueChangedEvent()
                invalidated()
            }
        }

        override fun invalidated() {
            currentNested.removeListener(changeListener)
            currentNested = extractNested()
            currentNested.addListener(changeListener)
        }

        override fun getValue() = currentNested.value

        override fun setValue(v: Boolean?) {
            (currentNested as? WritableValue<*>)?.value = v
            super.setValue(v)
        }
    }
}
