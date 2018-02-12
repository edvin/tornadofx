package tornadofx.osgi.impl

import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceEvent
import org.osgi.framework.ServiceListener
import org.osgi.util.tracker.ServiceTracker
import tornadofx.*
import tornadofx.osgi.ChildInterceptorProvider

class InterceptorListener(val context: BundleContext) : ServiceListener {
    val tracker = ServiceTracker<ChildInterceptorProvider, Any>(context, context.createFilter("(&(objectClass=${ChildInterceptorProvider::class.java.name}))"), null)

    init {
        tracker.open()
        tracker.withEach {
            FX.addChildInterceptor(it.interceptor)
        }
    }

    override fun serviceChanged(event: ServiceEvent) {
        if (event.isChildInterceptorProviderEvent()) {
            val provider = context.getService(event.serviceReference) as ChildInterceptorProvider

            if (event.type == ServiceEvent.REGISTERED) {
                FX.addChildInterceptor(provider.interceptor)
            } else if (event.type == ServiceEvent.UNREGISTERING) {
                FX.removeChildInterceptor(provider.interceptor)
            }
        }
    }

    private fun ServiceEvent.isChildInterceptorProviderEvent() = objectClass == ChildInterceptorProvider::class.qualifiedName

}