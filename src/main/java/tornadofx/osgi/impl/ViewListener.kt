package tornadofx.osgi.impl

import javafx.application.Platform
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceEvent
import org.osgi.framework.ServiceListener
import org.osgi.util.tracker.ServiceTracker
import tornadofx.osgi.ViewProvider
import tornadofx.osgi.ViewReceiver
import tornadofx.removeFromParent

internal class ViewListener(val context: BundleContext) : ServiceListener {
    val providerTracker = ServiceTracker<ViewProvider, Any>(context, context.createFilter("(&(objectClass=${ViewProvider::class.java.name}))"), null)
    val receiverTracker = ServiceTracker<ViewReceiver, Any>(context, context.createFilter("(&(objectClass=${ViewReceiver::class.java.name}))"), null)

    init {
        providerTracker.open()
        receiverTracker.open()

        providerTracker.withEach { provider ->
            receiverTracker.withEach { receiver ->
                Platform.runLater {
                    receiver.viewProvided(provider)
                }
            }
        }

    }

    override fun serviceChanged(event: ServiceEvent) {
        if (event.isViewProviderEvent()) {
            val provider = context.getService(event.serviceReference) as ViewProvider

            if (event.type == ServiceEvent.REGISTERED) {
                receiverTracker.withEach {
                    Platform.runLater {
                        it.viewProvided(provider)
                    }
                }
            } else if (event.type == ServiceEvent.UNREGISTERING) {
                Platform.runLater {
                    provider.getView().root.removeFromParent()
                }
            }
        } else if (event.isViewReceiverEvent()) {
            val receiver = context.getService(event.serviceReference) as ViewReceiver

            if (event.type == ServiceEvent.REGISTERED) {
                providerTracker.withEach {
                    Platform.runLater {
                        receiver.viewProvided(it)
                    }
                }
            }
        }
    }

    private fun ServiceEvent.isViewProviderEvent() = objectClass == ViewProvider::class.qualifiedName
    private fun ServiceEvent.isViewReceiverEvent() = objectClass == ViewReceiver::class.qualifiedName
}