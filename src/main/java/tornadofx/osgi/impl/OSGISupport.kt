@file:Suppress("UNCHECKED_CAST")

package tornadofx.osgi.impl

import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.framework.FrameworkUtil
import org.osgi.framework.ServiceEvent
import org.osgi.util.tracker.ServiceTracker
import tornadofx.FX
import java.util.logging.Level
import kotlin.reflect.KClass

val ServiceEvent.objectClass: String
    get() = (serviceReference.getProperty("objectClass") as Array<String>)[0]

val fxBundle: Bundle get() = FrameworkUtil.getBundle(Activator::class.java)
val fxBundleContext: BundleContext get() = fxBundle.bundleContext

/**
 * Try to resolve the OSGi Bundle Id of the given class. This function can only be called
 * if OSGi is available on the classpath.
 */
fun getBundleId(classFromBundle: KClass<*>): Long? {
    try {
        return FrameworkUtil.getBundle(classFromBundle.java)?.bundleId
    } catch (ex: Exception) {
        FX.log.log(Level.WARNING, "OSGi was on the classpath but no Framework did not respond correctly", ex)
        return null
    }
}
inline fun <reified T:Any?> getBundleId(): Long? = getBundleId(T::class)

inline fun <S, T> ServiceTracker<S, T>.withEach(fn: (S) -> Unit) {
    services?.forEach {
        it as S
        fn(it)
    }
}