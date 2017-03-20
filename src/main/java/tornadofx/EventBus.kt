@file:Suppress("UNCHECKED_CAST")

package tornadofx

import javafx.application.Platform
import tornadofx.EventBus.RunOn.ApplicationThread
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.reflect.KClass

open class FXEvent(
        open val runOn: EventBus.RunOn = ApplicationThread,
        open val scope: Scope? = null
)

class EventContext {
    internal var unsubscribe = false
    fun unsubscribe() {
        unsubscribe = true
    }
}

class FXEventRegistration(val eventType: KClass<out FXEvent>, val owner: Component?, val maxCount: Long? = null, val action: EventContext.(FXEvent) -> Unit) {
    val count = AtomicLong()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as FXEventRegistration

        if (action != other.action) return false

        return true
    }

    override fun hashCode(): Int {
        return action.hashCode()
    }
}

class EventBus {
    enum class RunOn { ApplicationThread, BackgroundThread }

    private val subscriptions = HashMap<KClass<out FXEvent>, HashSet<FXEventRegistration>>()
    private val eventScopes = HashMap<EventContext.(FXEvent) -> Unit, Scope>()

    fun <T : FXEvent> subscribe(event: KClass<T>, scope: Scope, registration : FXEventRegistration) {
        subscriptions.computeIfAbsent(event, { HashSet() }).add(registration)
        eventScopes[registration.action] = scope
    }

    fun <T : FXEvent> subscribe(owner: Component? = null, times: Long? = null, event: KClass<T>, scope: Scope, action: (T) -> Unit) {
        subscribe(event, scope, FXEventRegistration(event, owner, times, action as EventContext.(FXEvent) -> Unit))
    }

    fun <T : FXEvent> subscribe(owner: Component? = null, times: Long? = null, event: Class<T>, scope: Scope, action: (T) -> Unit) {
        subscribe(event.kotlin, scope, FXEventRegistration(event.kotlin, owner, times, action as EventContext.(FXEvent) -> Unit))
    }

    fun <T : FXEvent> unsubscribe(event: Class<T>, action: EventContext.(T) -> Unit) {
        unsubscribe(event.kotlin, action)
    }

    fun <T : FXEvent> unsubscribe(event: KClass<T>, action: EventContext.(T) -> Unit) {
        subscriptions[event]?.removeAll { it.action == action }
        eventScopes.remove(action)
    }

    fun unsubscribe(registration: FXEventRegistration) {
        unsubscribe(registration.eventType, registration.action)
        registration.owner?.subscribedEvents?.get(registration.eventType)?.remove(registration)
    }

    fun fire(event: FXEvent) {
        fun fireEvents() {
            subscriptions[event.javaClass.kotlin]?.forEach {
                if (event.scope == null || event.scope == eventScopes[it.action]) {
                    val count = it.count.andIncrement
                    if (it.maxCount == null || count < it.maxCount) {
                        val context = EventContext()
                        it.action.invoke(context, event)
                        if (context.unsubscribe) unsubscribe(it)
                    } else {
                        unsubscribe(it)
                    }
                }
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