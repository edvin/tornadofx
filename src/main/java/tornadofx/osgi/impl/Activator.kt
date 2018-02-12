package tornadofx.osgi.impl

import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import org.osgi.service.url.URLStreamHandlerService
import java.util.*

internal class Activator : BundleActivator {
    lateinit var applicationListener: ApplicationListener
    lateinit var stylesheetListener: StylesheetListener
    lateinit var viewListener: ViewListener
    lateinit var interceptorListener: InterceptorListener
    override fun start(context: BundleContext) {
        applicationListener = ApplicationListener(context)
        stylesheetListener = StylesheetListener(context)
        viewListener = ViewListener(context)
        interceptorListener = InterceptorListener(context)
        context.addServiceListener(applicationListener)
        context.addServiceListener(stylesheetListener)
        context.addServiceListener(viewListener)
        context.addServiceListener(interceptorListener)
        val cssOptions = Hashtable<String, String>()
        cssOptions["url.handler.protocol"] = "css"
        cssOptions["url.content.mimetype"] = "text/css"
        context.registerService(URLStreamHandlerService::class.java, CSSURLStreamHandlerService(), cssOptions)
    }

    override fun stop(context: BundleContext) {
    }
}