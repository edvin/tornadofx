@file:Suppress("UNCHECKED_CAST")

package tornadofx

import javafx.application.Platform
import java.util.*
import kotlin.concurrent.thread

open class FXEvent(open val runOnFxApplicationThread: Boolean = true)

class EventBus {
    private val subscriptions = HashMap<FXEvent, HashSet<(FXEvent) -> Unit>>()

    fun <T : FXEvent> subscribe(event: T, action: (T) -> Unit) =
        subscriptions.computeIfAbsent(event, { HashSet() }).add(action as (FXEvent) -> Unit)

    fun <T : FXEvent> unsubscribe(event: T, action: (T) -> Unit) =
        subscriptions[event]?.remove(action)

    fun fire(event: FXEvent) {
        fun fireEvents() {
            subscriptions[event]?.forEach { it.invoke(event) }
        }

        if (Platform.isFxApplicationThread()) {
            if (event.runOnFxApplicationThread) {
                fireEvents()
            } else {
                thread(true) {
                    fireEvents()
                }
            }
        } else {
            if (event.runOnFxApplicationThread) {
                Platform.runLater {
                    fireEvents()
                }
            } else {
                fireEvents()
            }
        }
    }

}