@file:Suppress("UNCHECKED_CAST")

package tornadofx

import javafx.application.Platform
import tornadofx.EventBus.RunOn.ApplicationThread
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Level
import kotlin.collections.HashSet
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

interface EventRegistration {
    val eventType: KClass<out FXEvent>
    val owner: Component?
    val action: EventContext.(FXEvent) -> Unit
    fun unsubscribe()
}

class FXEventRegistration(override val eventType: KClass<out FXEvent>,
                          override val owner: Component?,
                          private val maxCount: Long? = null,
                          private val anAction: EventContext.(FXEvent) -> Unit) : EventRegistration {

    private val invocationCount = AtomicLong()

    override val action: EventContext.(FXEvent) -> Unit = {
        if (maxCount == null || invocationCount.getAndIncrement() < maxCount) {
            anAction.invoke(this, it)
        } else {
            this@FXEventRegistration.unsubscribe()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FXEventRegistration) return false
        if (eventType != other.eventType) return false
        if (owner != other.owner) return false
        if (maxCount != other.maxCount) return false
        if (action != other.action) return false
        if (invocationCount.get() != other.invocationCount.get()) return false

        return true
    }

    override fun hashCode(): Int {
        var result = eventType.hashCode()
        result = 31 * result + owner.hashCode()
        result = 31 * result + action.hashCode()
        return result
    }

    override fun unsubscribe() {
        FX.eventbus.unsubscribe(this)
    }
}

data class InvalidatableEventRegistration(val registration: EventRegistration, @Volatile var valid: Boolean = true) : EventRegistration by registration

class EventBus {
    enum class RunOn { ApplicationThread, BackgroundThread }

    private val subscriptions = ConcurrentHashMap<KClass<out FXEvent>, Set<InvalidatableEventRegistration>>()
    private val eventScopes = ConcurrentHashMap<EventContext.(FXEvent) -> Unit, Scope>()

    inline fun <reified T : FXEvent> subscribe(scope: Scope, registration: FXEventRegistration) = subscribe(T::class, scope, registration)
    fun <T : FXEvent> subscribe(event: KClass<T>, scope: Scope, registration: FXEventRegistration) {
        subscriptions.compute(event) { _, set ->
            val newSet = if (set != null) HashSet(set.filter { it.valid }) else HashSet()
            newSet.add(InvalidatableEventRegistration(registration))
            newSet
        }
        eventScopes[registration.action] = scope
    }

    inline fun <reified T : FXEvent> subscribe(owner: Component? = null, times: Long? = null, scope: Scope, noinline action: (T) -> Unit) = subscribe(owner, times, T::class, scope, action)

    fun <T : FXEvent> subscribe(owner: Component? = null, times: Long? = null, event: KClass<T>, scope: Scope, action: (T) -> Unit) {
        subscribe(event, scope, FXEventRegistration(event, owner, times, action as EventContext.(FXEvent) -> Unit))
    }

    fun <T : FXEvent> subscribe(owner: Component? = null, times: Long? = null, event: Class<T>, scope: Scope, action: (T) -> Unit) = subscribe(event.kotlin, scope, FXEventRegistration(event.kotlin, owner, times, action as EventContext.(FXEvent) -> Unit))

    inline fun <reified T : FXEvent> unsubscribe(noinline action: EventContext.(T) -> Unit) = unsubscribe(T::class, action)
    fun <T : FXEvent> unsubscribe(event: Class<T>, action: EventContext.(T) -> Unit) = unsubscribe(event.kotlin, action)

    fun <T : FXEvent> unsubscribe(event: KClass<T>, action: EventContext.(T) -> Unit) {
        subscriptions[event]?.forEach {
            if (it.action == action) {
                it.valid = false
            }
        }
        eventScopes.remove(action)
    }

    fun unsubscribe(registration: EventRegistration) {
        unsubscribe(registration.eventType, registration.action)
        registration.owner?.subscribedEvents?.computeIfPresent(registration.eventType) {_, list -> list.filter { it != registration }}
    }

    private fun cleanupInvalidRegistrations(eventKlass: KClass<FXEvent>) {
        subscriptions.computeIfPresent(eventKlass) { _, set -> set.filter { it.valid }.toSet() }
    }

    fun fire(event: FXEvent) {
        fun fireEvents() {
            val eventKlass = event.javaClass.kotlin
            subscriptions[eventKlass]?.forEach {
                if (it.valid && (event.scope == null || event.scope == eventScopes[it.action])) {
                    val context = EventContext()
                    try {
                        it.action.invoke(context, event)
                    } catch (subscriberFailure: Exception) {
                        log.log(Level.WARNING, "Event $event was delivered to subscriber from ${it.owner}, but invocation resulted in exception", subscriberFailure)
                    }
                    if (context.unsubscribe) unsubscribe(it)
                }
            }
            cleanupInvalidRegistrations(eventKlass)
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

    internal fun clear() {
        subscriptions.clear()
        eventScopes.clear()
    }

    internal fun unsubscribeAll(scope: Scope) {
        val scopedContexts: Map<EventContext.(FXEvent) -> Unit, Scope> = eventScopes.filter { it.value == scope }
        val registrations = mutableListOf<EventRegistration>()
        subscriptions.forEach { (_, subscriptionRegistrations) ->
            registrations += subscriptionRegistrations.filter { scopedContexts.containsKey(it.action) }
        }
        registrations.forEach { it.unsubscribe() }
    }

}