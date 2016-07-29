package tornadofx.osgi

import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import tornadofx.osgi.impl.ApplicationListener
import tornadofx.osgi.impl.StylesheetListener

class Activator : BundleActivator {
    val applicationListener = ApplicationListener()
    val stylesheetListener = StylesheetListener()

    override fun start(context: BundleContext) {
        context.addServiceListener(applicationListener)
        context.addServiceListener(stylesheetListener)
    }

    override fun stop(context: BundleContext) {
        context.removeServiceListener(applicationListener)
        context.removeServiceListener(stylesheetListener)
    }
}