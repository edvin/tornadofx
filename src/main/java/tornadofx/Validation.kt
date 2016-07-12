package tornadofx

import javafx.beans.value.ObservableValue
import javafx.scene.Node
import javafx.scene.control.TextInputControl
import kotlin.concurrent.thread

enum class ValidationSeverity { Error, Warning, Info }

sealed class ValidationTrigger {
    object OnBlur : ValidationTrigger()
    class OnChange(val delay: Long = 0) : ValidationTrigger()
    object None : ValidationTrigger()
    companion object {
        val OnChangeImmediate = OnChange()
    }
}

class ValidationMessage(val message: String, val severity: ValidationSeverity)

class ValidationContext {
    val validators = mutableListOf<Validator<*>>()

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
            trigger: ValidationTrigger = ValidationTrigger.OnChangeImmediate,
            noinline validator: ValidationContext.(T?) -> ValidationMessage?) {

        addValidator(Validator(node, property, trigger, validator))
    }

    fun <T> addValidator(validator: Validator<T>) {
        when (validator.trigger) {
            is ValidationTrigger.OnChange -> {
                var delayActive = false

                validator.property.onChange {
                    if (validator.trigger.delay == 0L) {
                        validator.validate()
                    } else {
                        if (!delayActive) {
                            delayActive = true
                            thread(true) {
                                Thread.sleep(validator.trigger.delay)
                                FX.runAndWait {
                                    validator.validate()
                                }
                                delayActive = false
                            }
                        }
                    }
                }
            }
            is ValidationTrigger.OnBlur -> {
                validator.node.focusedProperty().onChange {
                    if (it == false) validator.validate()
                }
            }
        }
        validators.add(validator)
    }

    /**
     * A boolean indicating the current validation status.
     */
    val isValid: Boolean get() = validators.find { !it.isValid } == null

    /**
     * Rerun all validators and return a boolean indicating if validation passes.
     */
    fun validate(): Boolean {
        for (validator in validators)
            validator.validate()

        return isValid
    }

    /**
     * Add validator for a TextInputControl and validate the control's textProperty. Useful when
     * you don't bind against a ViewModel or other backing property.
     */
    fun addValidator(node: TextInputControl, trigger: ValidationTrigger = ValidationTrigger.OnChangeImmediate, validator: ValidationContext.(String?) -> ValidationMessage?) =
            addValidator<String>(node, node.textProperty(), trigger, validator)


    fun error(message: String) = ValidationMessage(message, ValidationSeverity.Error)
    fun info(message: String) = ValidationMessage(message, ValidationSeverity.Info)
    fun warning(message: String) = ValidationMessage(message, ValidationSeverity.Warning)

    inner class Validator<T>(
            val node: Node,
            val property: ObservableValue<T>,
            val trigger: ValidationTrigger = ValidationTrigger.OnChangeImmediate,
            val validator: ValidationContext.(T?) -> ValidationMessage?) {

        var result: ValidationMessage? = null
        var decorator: Decorator? = null

        fun validate(): Boolean {
            decorator?.apply { undecorate(node) }
            decorator = null

            result = validator(this@ValidationContext, property.value)

            result?.apply {
                decorator = decorationProvider(this)
                decorator!!.decorate(node)
            }

            return isValid
        }

        val isValid: Boolean get() = result == null || result!!.severity != ValidationSeverity.Error
    }

}