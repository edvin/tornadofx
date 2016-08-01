@file:Suppress("UNCHECKED_CAST")

package tornadofx.osgi.impl

import org.osgi.framework.FrameworkUtil
import org.osgi.framework.ServiceEvent
import org.osgi.util.tracker.ServiceTracker
import tornadofx.FX
import kotlin.reflect.KClass

val ServiceEvent.objectClass: String
    get() = (serviceReference.getProperty("objectClass") as Array<String>)[0]

val fxBundle = FrameworkUtil.getBundle(Activator::class.java)
val fxBundleContext = fxBundle.bundleContext

/**
 * Try to resolve the OSGi Bundle Id of the given class. This is safe
 * to run even when there isn't an OSGi runtime present.
 */
fun getBundleId(classFromBundle: KClass<*>) =
        if (FX.osgiAvailable) FrameworkUtil.getBundle(classFromBundle.java)?.bundleId else null

inline fun <S, T> ServiceTracker<S, T>.withEach(fn: (S) -> Unit) {
    services?.forEach {
        it as S
        fn(it)
    }
}