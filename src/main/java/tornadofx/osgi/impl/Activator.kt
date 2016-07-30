package tornadofx.osgi.impl

import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import org.osgi.service.url.URLStreamHandlerService
import java.util.*

internal class Activator : BundleActivator {
    val applicationListener = ApplicationListener()
    val stylesheetListener = StylesheetListener()
    val viewListener = ViewListener()

    override fun start(context: BundleContext) {
        val cssOptions = Hashtable<String, String>()
        cssOptions["url.handler.protocol"] = "css"
        cssOptions["url.content.mimetype"] = "text/css"
        context.registerService(URLStreamHandlerService::class.java, CSSURLStreamHandlerService(), cssOptions)
        context.addServiceListener(applicationListener)
        context.addServiceListener(stylesheetListener)
        context.addServiceListener(viewListener)

        applicationListener.lookForApplicationProviders(context)
    }

    override fun stop(context: BundleContext) {
    }
}