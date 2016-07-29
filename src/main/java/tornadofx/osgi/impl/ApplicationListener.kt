package tornadofx.osgi.impl

import javafx.application.Application
import javafx.stage.Stage
import org.osgi.framework.ServiceEvent
import org.osgi.framework.ServiceEvent.REGISTERED
import org.osgi.framework.ServiceEvent.UNREGISTERING
import org.osgi.framework.ServiceListener
import tornadofx.App
import tornadofx.FX
import tornadofx.osgi.ApplicationProvider
import kotlin.reflect.KClass

class ApplicationListener : ServiceListener {
    val applicationProxy = ApplicationProxy()

    override fun serviceChanged(event: ServiceEvent) {
        if (event.isApplicationProviderEvent()) {
            val refs = bundleContext.getServiceReferences(ApplicationProvider::class.java, "(Language=*)")
            val appProvider = bundleContext.getService(refs.first())
            val entrypoint = appProvider.application

            if (event.type == REGISTERED) {
                if (!applicationProxy.hasActiveApplication) startApplication(appProvider)
                else println("An application already running, not starting $entrypoint.")
            } else if (event.type == UNREGISTERING && applicationProxy.isRunningApplication(appProvider.application)) {
                applicationProxy.stopApplication()
            }
        }
    }

    private fun startApplication(appProvider: ApplicationProvider) {
        if (!applicationProxy.running) Application.launch(ApplicationProxy::class.java)
        applicationProxy.startApplication(appProvider)
    }

    private fun ServiceEvent.isApplicationProviderEvent() = objectClass == ApplicationProvider::class.qualifiedName

    inner class ApplicationProxy : Application() {
        var running = false
        var delegate: App? = null
        lateinit var realPrimaryStage: Stage
        val hasActiveApplication: Boolean get() = delegate != null

        fun isRunningApplication(appClass: KClass<out App>) = appClass == delegate?.javaClass?.kotlin

        override fun start(stage: Stage) {
            realPrimaryStage = stage
            running = true
            FX.installErrorHandler()
        }

        override fun stop() {
            stopApplication()
            running = false
        }

        fun startApplication(provider: ApplicationProvider) {
            delegate = provider.application.java.newInstance()
            delegate!!.init()
            delegate!!.start(realPrimaryStage)
        }

        fun stopApplication() {
            realPrimaryStage.hide()
        }

    }
}