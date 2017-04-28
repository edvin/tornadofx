@file:Suppress("unused", "UNCHECKED_CAST")

package tornadofx

import javafx.application.Platform
import javafx.application.Platform.isFxApplicationThread
import javafx.beans.binding.BooleanExpression
import javafx.beans.property.*
import javafx.scene.control.ButtonBase
import javafx.scene.control.MenuItem
import tornadofx.Command.Companion.CommandKey
import tornadofx.Command.Companion.CommandParameterKey
import kotlin.concurrent.thread

class Command<in T>(
        val action: (T) -> Unit,
        val enabled: BooleanExpression = SimpleBooleanProperty(true),
        val async: Boolean = false,
        val ui: Boolean = false,
        val title: String? = null
) {
    val running: ReadOnlyBooleanProperty = SimpleBooleanProperty(false)
    val isRunning: Boolean get() = running.value
    val isEnabled: Boolean get() = enabled.value

    internal val disabledProperty = enabled.not().or(running)

    fun execute() = execute(null as T)

    fun execute(param: T) {
        if (isRunning || disabledProperty.value) return
        if (async) thread(true) { doRun(param) } else doRun(param)
    }

    private fun doRun(param: T) {
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

    private fun setRunningAndRun(param: T) {
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

fun <T> command(action: (T) -> Unit, enabled: BooleanExpression = SimpleBooleanProperty(true), async: Boolean = false, ui: Boolean = false, title: String? = null) = Command<T>({ action(it) }, enabled, async, ui, title)
fun command(action: () -> Unit, enabled: BooleanExpression = SimpleBooleanProperty(true), async: Boolean = false, ui: Boolean = false, title: String? = null) = Command<Any>({ action() }, enabled, async, ui, title)

val ButtonBase.commandProperty: ObjectProperty<Command<*>>
    get() = properties.getOrPut(CommandKey) {
        SimpleObjectProperty<Command<*>>().apply {
            onChange {
                if (it == null) disableProperty().unbind()
                else disableProperty().cleanBind(it.disabledProperty)
                if (text.isNullOrBlank()) text = it?.title
            }
            this@commandProperty.action { (value as? Command<Any?>)?.execute(commandParameter) }
        }
    } as ObjectProperty<Command<*>>

var ButtonBase.command: Command<*>
    get() = commandProperty.value
    set(value) {
        commandProperty.value = value as Command<Any?>
    }

val ButtonBase.commandParameterProperty: ObjectProperty<Any?>
    get() = properties.getOrPut(CommandParameterKey) {
        SimpleObjectProperty<Any?>()
    } as ObjectProperty<Any?>

var ButtonBase.commandParameter: Any?
    get() = commandParameterProperty.value
    set(value) {
        commandParameterProperty.value = value
    }

val MenuItem.commandProperty: ObjectProperty<Command<*>>
    get() = properties.getOrPut(CommandKey) {
        SimpleObjectProperty<Command<*>>().apply {
            onChange {
                if (it == null) disableProperty().unbind()
                else disableProperty().cleanBind(it.disabledProperty)
                if (text.isNullOrBlank()) text = it?.title
            }
            this@commandProperty.action { (value as? Command<Any?>)?.execute(commandParameter) }
        }
    } as ObjectProperty<Command<*>>

var MenuItem.command: Command<*>
    get() = commandProperty.value
    set(value) {
        commandProperty.value = value as Command<Any?>
    }

val MenuItem.commandParameterProperty: ObjectProperty<Any?>
    get() = properties.getOrPut(CommandParameterKey) {
        SimpleObjectProperty<Any?>()
    } as ObjectProperty<Any?>

var MenuItem.commandParameter: Any?
    get() = commandParameterProperty.value
    set(value) {
        commandParameterProperty.value = value
    }