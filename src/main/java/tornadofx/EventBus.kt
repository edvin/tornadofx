@file:Suppress("UNCHECKED_CAST")

package tornadofx

import javafx.application.Platform
import tornadofx.EventBus.RunOn.ApplicationThread
import java.util.*
import kotlin.concurrent.thread
import kotlin.reflect.KClass

open class FXEvent(
        open val runOn: EventBus.RunOn = ApplicationThread,
        open val scope: Scope? = null
)

class EventBus {
    enum class RunOn { ApplicationThread, BackgroundThread }

    private val subscriptions = HashMap<KClass<out FXEvent>, HashSet<(FXEvent) -> Unit>>()
    private val eventScopes = HashMap<(FXEvent) -> Unit, Scope>()

    fun <T : FXEvent> subscribe(event: KClass<T>, scope: Scope, action: (T) -> Unit) {
        subscriptions.computeIfAbsent(event, { HashSet() }).add(action as (FXEvent) -> Unit)
        eventScopes[action] = scope
    }

    fun <T : FXEvent> subscribe(event: Class<T>, scope: Scope, action: (T) -> Unit) {
        subscriptions.computeIfAbsent(event.kotlin, { HashSet() }).add(action as (FXEvent) -> Unit)
        eventScopes[action] = scope
    }

    fun <T : FXEvent> unsubscribe(event: Class<T>, action: (T) -> Unit) {
        subscriptions[event.kotlin]?.remove(action)
        eventScopes.remove(action)
    }

    fun <T : FXEvent> unsubscribe(event: KClass<T>, action: (T) -> Unit) {
        subscriptions[event]?.remove(action)
        eventScopes.remove(action)
    }

    fun fire(event: FXEvent) {
        fun fireEvents() {
            subscriptions[event.javaClass.kotlin]?.forEach {
                if (event.scope == null || event.scope == eventScopes[it])
                    it.invoke(event)
            }
        }

        if (Platform.isFxApplicationThread()) {
            if (event.runOn == ApplicationThread) {
                fireEvents()
            } else {
                thread(true) {
                    fireEvents()
                }
            }
        } else {
            if (event.runOn == ApplicationThread) {
                Platform.runLater {
                    fireEvents()
                }
            } else {
                fireEvents()
            }
        }
    }

}