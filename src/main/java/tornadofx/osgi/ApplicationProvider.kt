package tornadofx.osgi

import org.osgi.framework.BundleContext
import tornadofx.App
import java.util.*
import kotlin.reflect.KClass

interface ApplicationProvider {
    val application: KClass<out App>
}

fun BundleContext.registerApplication(application: KClass<out App>) {
    val provider = object : ApplicationProvider {
        override val application = application
    }
    registerService(ApplicationProvider::class.java, provider, Hashtable<String, String>())
}