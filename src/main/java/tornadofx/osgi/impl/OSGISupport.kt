@file:Suppress("UNCHECKED_CAST")

package tornadofx.osgi.impl

import org.osgi.framework.FrameworkUtil
import org.osgi.framework.ServiceEvent
import org.osgi.util.tracker.ServiceTracker

val ServiceEvent.objectClass: String
    get() = (serviceReference.getProperty("objectClass") as Array<String>)[0]

val fxBundle = FrameworkUtil.getBundle(Activator::class.java)
val fxBundleContext = fxBundle.bundleContext

inline fun <S, T> ServiceTracker<S, T>.withEach(fn: (S) -> Unit) {
    services?.forEach {
        it as S
        fn(it)
    }
}