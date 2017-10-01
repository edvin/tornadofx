@file:Suppress("UNCHECKED_CAST", "CAST_NEVER_SUCCEEDS")

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

fun <T> ComboBoxBase<T>.bind(property: ObservableValue<T>, readonly: Boolean = false) {
    ViewModel.register(valueProperty(), property)
    if (readonly || (property !is Property<*>)) valueProperty().bind(property) else valueProperty().bindBidirectional(property as Property<T>)
}

fun DatePicker.bind(property: ObservableValue<LocalDate>, readonly: Boolean = false) {
    ViewModel.register(valueProperty(), property)
    if (readonly || (property !is Property<*>)) valueProperty().bind(property) else valueProperty().bindBidirectional(property as Property<LocalDate>)
}

fun ProgressIndicator.bind(property: ObservableValue<Number>, readonly: Boolean = false) {
    ViewModel.register(progressProperty(), property)
    if (readonly || (property !is Property<*>)) progressProperty().bind(property) else progressProperty().bindBidirectional(property as Property<Number>)
}

fun <T> ChoiceBox<T>.bind(property: ObservableValue<T>, readonly: Boolean = false) {
    ViewModel.register(valueProperty(), property)
    if (readonly || (property !is Property<*>)) valueProperty().bind(property) else valueProperty().bindBidirectional(property as Property<T>)
}

fun CheckBox.bind(property: ObservableValue<Boolean>, readonly: Boolean = false) {
    ViewModel.register(selectedProperty(), property)
    if (readonly || (property !is Property<*>)) selectedProperty().bind(property) else selectedProperty().bindBidirectional(property as Property<Boolean>)
}

fun CheckMenuItem.bind(property: ObservableValue<Boolean>, readonly: Boolean = false) {
    ViewModel.register(selectedProperty(), property)
    if (readonly || (property !is Property<*>)) selectedProperty().bind(property) else selectedProperty().bindBidirectional(property as Property<Boolean>)
}

fun Slider.bind(property: ObservableValue<Number>, readonly: Boolean = false) {
    ViewModel.register(valueProperty(), property)
    if (readonly || (property !is Property<*>)) valueProperty().bind(property) else valueProperty().bindBidirectional(property as Property<Number>)
}

inline fun <reified S : T, reified T : Any> Labeled.bind(property: ObservableValue<S>, readonly: Boolean = false, converter: StringConverter<T>? = null, format: Format? = null) {
    bindStringProperty(textProperty(), converter, format, property, readonly)
}

inline fun <reified S : T, reified T : Any> TitledPane.bind(property: ObservableValue<S>, readonly: Boolean = false, converter: StringConverter<T>? = null, format: Format? = null) =
        bindStringProperty(textProperty(), converter, format, property, readonly)

inline fun <reified S : T, reified T : Any> Text.bind(property: ObservableValue<S>, readonly: Boolean = false, converter: StringConverter<T>? = null, format: Format? = null) =
        bindStringProperty(textProperty(), converter, format, property, readonly)

inline fun <reified S : T, reified T : Any> TextInputControl.bind(property: ObservableValue<S>, readonly: Boolean = false, converter: StringConverter<T>? = null, format: Format? = null) =
        bindStringProperty(textProperty(), converter, format, property, readonly)

inline fun <reified S : T, reified T : Any> bindStringProperty(stringProperty: StringProperty, converter: StringConverter<T>?, format: Format?, property: ObservableValue<S>, readonly: Boolean) {
    if (stringProperty.isBound) stringProperty.unbind()
    val effectiveReadonly = if (readonly) readonly else property !is Property<S> || S::class != T::class

    ViewModel.register(stringProperty, property)

    if (S::class == String::class) {
        if (effectiveReadonly)
            stringProperty.bind(property as ObservableValue<String>)
        else
            stringProperty.bindBidirectional(property as Property<String>)
    } else {
        val effectiveConverter = if (format != null) null else converter ?: getDefaultConverter<S>()
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
                stringProperty.bindBidirectional(property as Property<S>, effectiveConverter as StringConverter<S>)
            } else if (format != null) {
                stringProperty.bindBidirectional(property as Property<S>, format)
            } else {
                throw IllegalArgumentException("Cannot convert from ${S::class} to String without an explicit converter or format")
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

fun ObservableValue<Boolean>.toBinding() = object : BooleanBinding() {
    init {
        super.bind(this@toBinding)
    }

    override fun dispose() {
        super.unbind(this@toBinding)
    }

    override fun computeValue() = this@toBinding.value

    override fun getDependencies(): ObservableList<*> {
        return FXCollections.singletonObservableList(this@toBinding)
    }
}

fun <T, N> ObservableValue<T>.select(nested: (T) -> ObservableValue<N>): Property<N> {
    fun extractNested(): ObservableValue<N>? = if (value != null) nested(value) else null

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
    fun extractNested() = nested(value)

    val dis = this
    var currentNested = extractNested()

    return object : SimpleBooleanProperty() {
        val changeListener = ChangeListener<Boolean> { observableValue, oldValue, newValue ->
            currentNested = extractNested()
            fireValueChangedEvent()
        }

        init {
            dis.onChange {
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
