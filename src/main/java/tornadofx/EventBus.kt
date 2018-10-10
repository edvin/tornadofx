package tornadofx

import javafx.application.Platform
import tornadofx.EventBus.RunOn.ApplicationThread
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Level
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
    val count: AtomicLong = AtomicLong()

    fun unsubscribe(): Unit = FX.eventbus.unsubscribe(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FXEventRegistration) return false

        if (eventType != other.eventType) return false
        if (owner != other.owner) return false
        if (action != other.action) return false

        return true
    }

    override fun hashCode(): Int {
        var result = eventType.hashCode()
        result = 31 * result + (owner?.hashCode() ?: 0)
        result = 31 * result + action.hashCode()
        return result
    }
}

@Suppress("UNCHECKED_CAST")
class EventBus {
    enum class RunOn { ApplicationThread, BackgroundThread }

    private val subscriptions = mutableMapOf<KClass<out FXEvent>, MutableSet<FXEventRegistration>>()
    private val eventScopes = mutableMapOf<EventContext.(FXEvent) -> Unit, Scope>()


    fun <T : FXEvent> subscribe(event: KClass<T>, scope: Scope, registration: FXEventRegistration) {
        subscriptions.getOrPut(event) { mutableSetOf() }.add(registration)
        eventScopes[registration.action] = scope
    }

    fun <T : FXEvent> subscribe(owner: Component? = null, times: Long? = null, event: KClass<T>, scope: Scope, action: (T) -> Unit): Unit =
        subscribe(event, scope, FXEventRegistration(event, owner, times, action as EventContext.(FXEvent) -> Unit))

    fun <T : FXEvent> subscribe(owner: Component? = null, times: Long? = null, event: Class<T>, scope: Scope, action: (T) -> Unit): Unit =
        subscribe(event.kotlin, scope, FXEventRegistration(event.kotlin, owner, times, action as EventContext.(FXEvent) -> Unit))

    inline fun <reified T : FXEvent> subscribe(scope: Scope, registration: FXEventRegistration): Unit = subscribe(T::class, scope, registration)

    inline fun <reified T : FXEvent> subscribe(owner: Component? = null, times: Long? = null, scope: Scope, noinline action: (T) -> Unit): Unit =
        subscribe(owner, times, T::class, scope, action)


    fun <T : FXEvent> unsubscribe(event: KClass<T>, action: EventContext.(T) -> Unit) {
        subscriptions[event]?.removeAll { it.action == action }
        eventScopes.remove(action)
    }

    fun <T : FXEvent> unsubscribe(event: Class<T>, action: EventContext.(T) -> Unit): Unit = unsubscribe(event.kotlin, action)

    inline fun <reified T : FXEvent> unsubscribe(noinline action: EventContext.(T) -> Unit): Unit = unsubscribe(T::class, action)

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
                        try {
                            it.action.invoke(context, event)
                        } catch (e: Exception) {
                            log.log(Level.WARNING, "Event $event was delivered to subscriber from ${it.owner}, but invocation resulted in exception", e)
                        }
                        if (context.unsubscribe) unsubscribe(it)
                    } else {
                        unsubscribe(it)
                    }
                }
            }
        }

        val isOnFx = Platform.isFxApplicationThread()
        val runOnFx = event.runOn == ApplicationThread

        when {
            !(isOnFx xor runOnFx) -> fireEvents()
            isOnFx -> thread { fireEvents() }
            runOnFx -> runLater { fireEvents() }
        }
    }
}
