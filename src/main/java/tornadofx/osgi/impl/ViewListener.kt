package tornadofx.osgi.impl

import org.osgi.framework.ServiceEvent
import org.osgi.framework.ServiceListener
import tornadofx.osgi.ViewProvider
import tornadofx.osgi.ViewReceiver
import tornadofx.removeFromParent

internal class ViewListener : ServiceListener {
    override fun serviceChanged(event: ServiceEvent) {
        if (event.isViewProviderEvent()) {
            val provider = fxBundleContext.getService(event.serviceReference) as ViewProvider

            if (event.type == ServiceEvent.REGISTERED) {
                viewReceivers.forEach { it.viewProvided(provider) }
            } else if (event.type == ServiceEvent.UNREGISTERING) {
                provider.view.root.removeFromParent()
            }
        }
    }

    private val viewReceivers: Collection<ViewReceiver> get() =
        fxBundleContext.getServiceReferences(ViewReceiver::class.java, "(Language=*)")
                .map { fxBundleContext.getService(it) }

    private fun ServiceEvent.isViewProviderEvent() = objectClass == ViewProvider::class.qualifiedName
}