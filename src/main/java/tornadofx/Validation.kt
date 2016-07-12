package tornadofx

import javafx.beans.value.ObservableValue
import javafx.scene.Node
import javafx.scene.control.TextInputControl

enum class ValidationSeverity { Warning, Error }

sealed class ValidationTrigger {
    companion object {
        val onchange = OnChange()
        val onblur = OnBlur()
        val none = None()
    }
    class OnBlur : ValidationTrigger()
    class OnChange(val delay: Long = 0) : ValidationTrigger()
    class None : ValidationTrigger()
}

class ValidationMessage(val message: String, val severity: ValidationSeverity)

class ValidationResult(val node: Node, val message: ValidationMessage)

class Validator<T>(
        val node: Node,
        val property: ObservableValue<T>,
        val trigger: ValidationTrigger = ValidationTrigger.onchange,
        val validator: ValidationContext.(T) -> ValidationMessage?) {
}

class ValidationContext {
    private val _validators = mutableListOf<Validator<*>>()

    /**
     * A read only view of the validators in this context
     */
    private val validators : List<Validator<*>> get() = _validators

    /**
     * The decoration provider decides what kind of decoration should be applied to
     * a control when validation fails. The default decorator will paint a small triangle
     * in the top left corner and display a Tooltip with the error message.
     */
    var decorationProvider: (ValidationMessage) -> Decorator? = { SimpleMessageDecorator(it.message, it.severity) }

    /**
     * Add the given validator to the given property. The supplied node will be decorated by
     * the current decorationProvider for this context if validation fails.
     *
     * The validator function is executed in the scope of this ValidationContex to give
     * access to other fields and shortcuts like the error and warning functions.
     *
     * The validation trigger decides when the validation is applied. ValidationTrigger.OnBlur
     * tracks focus on the supplied node while OnChange tracks changes to the property itself.
     */
    inline fun <reified T> addValidator(
            node: Node,
            property: ObservableValue<T>,
            trigger: ValidationTrigger = ValidationTrigger.onchange,
            noinline validator: ValidationContext.(T) -> ValidationMessage?) {

        addValidator(Validator(node, property, trigger, validator))
    }

    fun <T> addValidator(validator: Validator<T>) {
        when (validator.trigger) {
            is ValidationTrigger.OnChange -> {

            }
            is ValidationTrigger.OnBlur -> {

            }
        }
        _validators.add(validator)
    }

//    fun isValid() = validate().isEmpty()

//    fun validate(): List<ValidationResult> {
//
//    }

    /**
     * Add validator for a TextInputControl and validate the control's textProperty. Useful when
     * you don't bind against a ViewModel or other backing property.
     */
    inline fun addValidator(node: TextInputControl, trigger: ValidationTrigger = ValidationTrigger.onchange, noinline validator: ValidationContext.(String) -> ValidationMessage?) =
        addValidator<String>(node, node.textProperty(), trigger, validator)


    fun error(message: String) = ValidationMessage(message, ValidationSeverity.Error)
    fun warning(message: String) = ValidationMessage(message, ValidationSeverity.Warning)
}
