@file:Suppress("UNCHECKED_CAST")

package tornadofx.osgi.impl

import org.osgi.framework.FrameworkUtil
import org.osgi.framework.ServiceEvent

val ServiceEvent.objectClass: String
    get() = (serviceReference.getProperty("objectClass") as Array<String>)[0]

val fxBundleContext = FrameworkUtil.getBundle(Activator::class.java).bundleContext