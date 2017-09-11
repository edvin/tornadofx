package tornadofx.osgi

import org.osgi.framework.BundleContext
import tornadofx.App
import java.util.*
import kotlin.reflect.KClass

interface ApplicationProvider {
    val application: KClass<out App>
}

inline fun <reified T: App> BundleContext.registerApplication() = registerApplication(T::class)
fun BundleContext.registerApplication(application: KClass<out App>) {
    val provider = object : ApplicationProvider {
        override val application = application
    }
    registerService(ApplicationProvider::class.java, provider, Hashtable<String, String>())
}