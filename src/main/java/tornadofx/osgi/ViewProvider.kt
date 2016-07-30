package tornadofx.osgi

import javafx.event.EventTarget
import org.osgi.framework.BundleContext
import org.osgi.framework.FrameworkUtil
import tornadofx.FX
import tornadofx.UIComponent
import tornadofx.find
import tornadofx.plusAssign
import java.util.*
import kotlin.reflect.KClass

interface ViewProvider {
    val view: UIComponent
    val discriminator: Any?
}

interface ViewReceiver {
    fun viewProvided(provider: ViewProvider)
}

fun BundleContext.registerView(viewType: KClass<out UIComponent>, discriminator: Any? = null) {
    val view = find(viewType)
    val provider = object : ViewProvider {
        override val view = view
        override val discriminator = discriminator
    }
    registerService(ViewProvider::class.java, provider, Hashtable<String, String>())
}

/**
 * Subscribe to ViewProvider events from other OSGi bundles and
 * add the provided view to this UI element if the acceptor returns true.
 */
fun EventTarget.addViewsWhen(acceptor: (ViewProvider) -> Boolean) {
    if (!FX.osgiAvailable) throw IllegalArgumentException("You can only subscribe to ViewProviders when you're in an OSGi context")
    val context = FrameworkUtil.getBundle(acceptor.javaClass).bundleContext
    val receiver = object : ViewReceiver {
        override fun viewProvided(provider: ViewProvider) {
            if (acceptor.invoke(provider))
                plusAssign(provider.view)
        }
    }
    context.registerService(ViewReceiver::class.java, receiver, Hashtable<String, String>())
}
