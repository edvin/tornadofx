package tornadofx.osgi

import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import org.osgi.service.url.URLStreamHandlerService
import tornadofx.osgi.impl.ApplicationListener
import tornadofx.osgi.impl.StylesheetListener
import java.util.*

class Activator : BundleActivator {
    val applicationListener = ApplicationListener()
    val stylesheetListener = StylesheetListener()

    override fun start(context: BundleContext) {
        val cssOptions = Hashtable<String, String>()
        cssOptions["url.handler.protocol"] = "css"
        cssOptions["url.content.mimetype"] = "text/css"
        context.registerService(URLStreamHandlerService::class.java, CSSURLStreamHandlerService(), cssOptions)
        context.addServiceListener(applicationListener)
        context.addServiceListener(stylesheetListener)
    }

    override fun stop(context: BundleContext) {
        context.removeServiceListener(applicationListener)
        context.removeServiceListener(stylesheetListener)
    }
}