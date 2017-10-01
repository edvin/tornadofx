@file:Suppress("unused", "UNCHECKED_CAST")

package tornadofx

import javafx.application.Platform
import javafx.application.Platform.isFxApplicationThread
import javafx.beans.binding.BooleanExpression
import javafx.beans.property.*
import javafx.scene.control.ButtonBase
import javafx.scene.control.ChoiceBox
import javafx.scene.control.MenuItem
import javafx.scene.control.TextField
import tornadofx.Command.Companion.CommandKey
import tornadofx.Command.Companion.CommandParameterKey
import kotlin.concurrent.thread

open class Command<in T>(
        val action: (T?) -> Unit,
        val enabled: BooleanExpression = SimpleBooleanProperty(true),
        val async: Boolean = false,
        val ui: Boolean = false
) {
    val running: ReadOnlyBooleanProperty = SimpleBooleanProperty(false)
    val isRunning: Boolean get() = running.value
    val isEnabled: Boolean get() = enabled.value

    internal val disabledProperty = enabled.not().or(running)

    fun execute() = execute(null as T?)

    fun execute(param: T?) {
        if (isRunning || disabledProperty.value) return
        if (async) thread(true) { doRun(param) } else doRun(param)
    }

    private fun doRun(param: T?) {
        if (ui && !isFxApplicationThread()) {
            if (async) {
                Platform.runLater { setRunningAndRun(param) }
            } else {
                FX.runAndWait { setRunningAndRun(param) }
            }
        } else {
            setRunningAndRun(param)
        }
    }

    private fun setRunningAndRun(param: T?) {
        (running as BooleanProperty).value = true
        try {
            action(param)
        } finally {
            running.value = false
        }
    }

    companion object {
        init {
            FX.log.warning("The Commands feature is experimental and subject to change even in minor releases!")
        }

        internal val CommandKey = "tornadofx.Command"
        internal val CommandParameterKey = "tornadofx.CommandParam"
    }
}


/**
 * Create a command with a non null parameter where the is a function reference.
 */
fun <T> command(action: (T) -> Unit, enabled: BooleanExpression = SimpleBooleanProperty(true), async: Boolean = false, ui: Boolean = false) = Command<T>({ action(it!!) }, enabled, async, ui)

/**
 * Create a command with a nullable parameter where the is either a lambda or a function reference.
 *
 * The noarg parameter is useless, but a trick to help Kotlin differentiate between the no null parameter version of this function.
 */
fun <T> command(action: (T?) -> Unit, enabled: BooleanExpression = SimpleBooleanProperty(true), async: Boolean = false, ui: Boolean = false, @Suppress("UNUSED_PARAMETER") nullable: Boolean = true) = Command<T?>({ action(it) }, enabled, async, ui)

/**
 * Create a command with a nullable parameter where the is a lambda.
 *
 * The noarg parameter is useless, but a trick to help Kotlin differentiate between the no null parameter version of this function.
 */
fun <T> command(enabled: BooleanExpression = SimpleBooleanProperty(true), async: Boolean = false, ui: Boolean = false, @Suppress("UNUSED_PARAMETER") nullable: Boolean = true, action: (T?) -> Unit) = Command<T?>({ action(it) }, enabled, async, ui)

/**
 * Create a parameterless command where the action is a function reference.
 */
fun command(action: () -> Unit, enabled: BooleanExpression = SimpleBooleanProperty(true), async: Boolean = false, ui: Boolean = false) = Command<Any>({ action() }, enabled, async, ui)

/**
 * Create a parameterless command where the action is a lambda.
 * This overload allows the command to be defined as the last parameter
 */
fun command(enabled: BooleanExpression = SimpleBooleanProperty(true), async: Boolean = false, ui: Boolean = false, action: () -> Unit) = Command<Any>({ action() }, enabled, async, ui)

var ButtonBase.commandProperty: ObjectProperty<Command<*>>
    get() = properties.getOrPut(CommandKey) {
        SimpleObjectProperty<Command<*>>().apply {
            onChange {
                if (it == null) disableProperty().unbind()
                else disableProperty().cleanBind(it.disabledProperty)
            }
            this@commandProperty.action { (value as? Command<Any?>)?.execute(commandParameter) }
        }
    } as ObjectProperty<Command<*>>
    set(value) { properties.put(CommandKey, value) }

var ButtonBase.command: Command<*>
    get() = commandProperty.value
    set(value) {

        if (value is CommandWithParameter) {
            commandProperty.value = value.command
            commandParameter = value.parameter
        } else {
            commandProperty.value = value as Command<Any?>
        }
    }

var ButtonBase.commandParameterProperty: Property<Any?>
    get() = properties.getOrPut(CommandParameterKey) {
        SimpleObjectProperty<Any?>()
    } as Property<Any?>
    set(value) { properties.put(CommandParameterKey, value) }

var ButtonBase.commandParameter: Any?
    get() = commandParameterProperty.value
    set(value) {
        if (value is Property<*>) {
            commandParameterProperty = value as Property<Any?>
        } else {
            commandParameterProperty.value = value
        }
    }

var MenuItem.commandProperty: Property<Command<*>>
    get() = properties.getOrPut(CommandKey) {
        SimpleObjectProperty<Command<*>>().apply {
            onChange {
                if (it == null) disableProperty().unbind()
                else disableProperty().cleanBind(it.disabledProperty)
            }
            this@commandProperty.action { (value as? Command<Any?>)?.execute(commandParameter) }
        }
    } as Property<Command<*>>
    set(value) { properties.put(CommandKey, value) }

var MenuItem.command: Command<*>
    get() = commandProperty.value
    set(value) {
        if (value is CommandWithParameter) {
            commandProperty.value = value.command
            commandParameter = value.parameter
        } else {
            commandProperty.value = value as Command<Any?>
        }
    }

var MenuItem.commandParameterProperty: Property<Any?>
    get() = properties.getOrPut(CommandParameterKey) {
        SimpleObjectProperty<Any?>()
    } as Property<Any?>
    set(value) { properties.put(CommandParameterKey, value) }

var MenuItem.commandParameter: Any?
    get() = commandParameterProperty.value
    set(value) {
        if (value is Property<*>) {
            commandParameterProperty = value as Property<Any?>
        } else {
            commandParameterProperty.value = value
        }
    }

var TextField.commandProperty: Property<Command<*>>
    get() = properties.getOrPut(CommandKey) {
        SimpleObjectProperty<Command<*>>().apply {
            onChange {
                if (it == null) disableProperty().unbind()
                else disableProperty().cleanBind(it.disabledProperty)
            }
            this@commandProperty.action { (value as? Command<Any?>)?.execute(commandParameter) }
        }
    } as Property<Command<*>>
    set(value) { properties.put(CommandKey, value) }

var TextField.command: Command<*>
    get() = commandProperty.value
    set(value) {
        if (value is CommandWithParameter) {
            commandProperty.value = value.command
            commandParameter = value.parameter
        } else {
            commandProperty.value = value as Command<Any?>
        }
    }

var TextField.commandParameterProperty: Property<Any?>
    get() = properties.getOrPut(CommandParameterKey) {
        SimpleObjectProperty<Any?>()
    } as Property<Any?>
    set(value) { properties.put(CommandParameterKey, value) }

var TextField.commandParameter: Any?
    get() = commandParameterProperty.value
    set(value) {
        if (value is Property<*>) {
            commandParameterProperty = value as Property<Any?>
        } else {
            commandParameterProperty.value = value
        }
    }


var ChoiceBox<*>.commandProperty: Property<Command<*>>
    get() = properties.getOrPut(CommandKey) {
        SimpleObjectProperty<Command<*>>().apply {
            onChange {
                if (it == null) disableProperty().unbind()
                else disableProperty().cleanBind(it.disabledProperty)
            }
            this@commandProperty.action { (value as? Command<Any?>)?.execute(commandParameter) }
        }
    } as Property<Command<*>>
    set(value) { properties.put(CommandKey, value) }

var ChoiceBox<*>.command: Command<*>
    get() = commandProperty.value
    set(value) {
        if (value is CommandWithParameter) {
            commandProperty.value = value.command
            commandParameter = value.parameter
        } else {
            commandProperty.value = value as Command<Any?>
        }
    }

var ChoiceBox<*>.commandParameterProperty: Property<Any?>
    get() = properties.getOrPut(CommandParameterKey) {
        SimpleObjectProperty<Any?>()
    } as Property<Any?>
    set(value) { properties.put(CommandParameterKey, value) }

var ChoiceBox<*>.commandParameter: Any?
    get() = commandParameterProperty.value
    set(value) {
        if (value is Property<*>) {
            commandParameterProperty = value as Property<Any?>
        } else {
            commandParameterProperty.value = value
        }
    }

class CommandWithParameter(val command: Command<*>, val parameter: Any?) : Command<Any?>({})

operator fun Command<*>.invoke(parameter: Any?) = CommandWithParameter(this, parameter)

infix fun Command<*>.with(parameter: Any?) = invoke(parameter)
