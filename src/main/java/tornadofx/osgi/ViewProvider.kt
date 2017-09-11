package tornadofx.osgi

import javafx.application.Platform
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
    fun getView(): UIComponent
    val discriminator: Any?
}

interface ViewReceiver {
    fun viewProvided(provider: ViewProvider)
}


/**
 * Provide this View to other OSGi Bundles. To receive this View, call `Node.addViewWhen` on the containing Node
 */
fun BundleContext.registerView(viewType: KClass<out UIComponent>, discriminator: Any? = null) {
    val provider = object : ViewProvider {
        override fun getView() = find(viewType)
        override val discriminator = discriminator
    }
    registerService(ViewProvider::class.java, provider, Hashtable<String, String>())
}
inline fun <reified T:UIComponent> BundleContext.registerView(discriminator: Any? = null) = registerView(T::class, discriminator)

/**
 * Subscribe to ViewProvider events from other OSGi bundles and
 * add the provided view to this UI element if the acceptor returns true.
 */
fun EventTarget.addViewsWhen(acceptor: (ViewProvider) -> Boolean) {
    if (!FX.osgiAvailable) throw IllegalArgumentException("You can only subscribe to ViewProviders when you're in an OSGi context")
    val context = FrameworkUtil.getBundle(acceptor.javaClass).bundleContext
    val receiver = object : ViewReceiver {
        override fun viewProvided(provider: ViewProvider) {
            Platform.runLater {
                if (acceptor.invoke(provider))
                    plusAssign(provider.getView())
            }
        }
    }
    context.registerService(ViewReceiver::class.java, receiver, Hashtable<String, String>())
}
