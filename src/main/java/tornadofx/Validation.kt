@file:Suppress("unused")

package tornadofx

import javafx.beans.binding.BooleanExpression
import javafx.beans.property.BooleanProperty
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.scene.Node
import javafx.scene.control.TextInputControl
import kotlin.concurrent.thread

enum class ValidationSeverity { Error, Warning, Info, Success }

sealed class ValidationTrigger {
    object OnBlur : ValidationTrigger()
    class OnChange(val delay: Long = 0) : ValidationTrigger()
    object None : ValidationTrigger()
}

class ValidationMessage(val message: String?, val severity: ValidationSeverity)

class ValidationContext {
    val validators = FXCollections.observableArrayList<Validator<*>>()

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
            trigger: ValidationTrigger = ValidationTrigger.OnChange(),
            noinline validator: ValidationContext.(T?) -> ValidationMessage?) = addValidator(Validator(node, property, trigger, validator))

    fun <T> addValidator(validator: Validator<T>, decorateErrors: Boolean = true): Validator<T> {
        when (validator.trigger) {
            is ValidationTrigger.OnChange -> {
                var delayActive = false

                validator.property.onChange {
                    if (validator.trigger.delay == 0L) {
                        validator.validate(decorateErrors)
                    } else {
                        if (!delayActive) {
                            delayActive = true
                            thread(true) {
                                Thread.sleep(validator.trigger.delay)
                                FX.runAndWait {
                                    validator.validate(decorateErrors)
                                }
                                delayActive = false
                            }
                        }
                    }
                }
            }
            is ValidationTrigger.OnBlur -> {
                validator.node.focusedProperty().onChange {
                    if (!it) validator.validate(decorateErrors)
                }
            }
        }
        validators.add(validator)
        return validator
    }

    /**
     * A boolean indicating the current validation status.
     */
    val valid: ReadOnlyBooleanProperty = SimpleBooleanProperty(true)
    val isValid by valid

    /**
     * Rerun all validators (or just the ones passed in) and return a boolean indicating if validation passed.
     */
    fun validate(vararg fields: ObservableValue<*>) = validate(true, true, fields = *fields)

    /**
     * Rerun all validators (or just the ones passed in) and return a boolean indicating if validation passed.
     * It is allowed to pass inn fields that has no corresponding validator. They will register as validated.
     */
    fun validate(focusFirstError: Boolean = true, decorateErrors: Boolean = true, vararg fields: ObservableValue<*>): Boolean {
        var firstErrorFocused = false
        var validationSucceeded = true

        val validateThese = if (fields.isEmpty()) validators else validators.filter {
            val facade = it.property.viewModelFacade
            facade != null && facade in fields
        }

        for (validator in validateThese) {
            if (!validator.validate(decorateErrors)) {
                validationSucceeded = false
                if (focusFirstError && !firstErrorFocused) {
                    firstErrorFocused = true
                    validator.node.requestFocus()
                }
            }
        }
        return validationSucceeded
    }

    /**
     * Add validator for a TextInputControl and validate the control's textProperty. Useful when
     * you don't bind against a ViewModel or other backing property.
     */
    fun addValidator(node: TextInputControl, trigger: ValidationTrigger = ValidationTrigger.OnChange(), validator: ValidationContext.(String?) -> ValidationMessage?) =
            addValidator<String>(node, node.textProperty(), trigger, validator)


    fun error(message: String? = null) = ValidationMessage(message, ValidationSeverity.Error)
    fun info(message: String? = null) = ValidationMessage(message, ValidationSeverity.Info)
    fun warning(message: String? = null) = ValidationMessage(message, ValidationSeverity.Warning)
    fun success(message: String? = null) = ValidationMessage(message, ValidationSeverity.Success)

    /**
     * Update the valid property state. If the calling validator was valid we need to see if any of the other properties are invalid.
     * If the calling validator is invalid, we know the state is invalid so no need to check the other validators.
     */
    internal fun updateValidState(callingValidatorState: Boolean) {
        (valid as BooleanProperty).value = if (callingValidatorState) validators.find { !it.isValid } == null else false
    }

    inner class Validator<T>(
            val node: Node,
            val property: ObservableValue<T>,
            val trigger: ValidationTrigger = ValidationTrigger.OnChange(),
            val validator: ValidationContext.(T?) -> ValidationMessage?) {

        var result: ValidationMessage? = null
        var decorator: Decorator? = null
        val valid: BooleanExpression = SimpleBooleanProperty(true)
        val isValid: Boolean get() = valid.value

        fun validate(decorateErrors: Boolean = true): Boolean {
            decorator?.apply { undecorate(node) }
            decorator = null

            result = validator(this@ValidationContext, property.value)
            (valid as BooleanProperty).value = result == null || result!!.severity != ValidationSeverity.Error

            if (decorateErrors) {
                result?.apply {
                    decorator = decorationProvider(this)
                    decorator!!.decorate(node)
                }
            }

            updateValidState(isValid)
            return isValid
        }

    }

}