package tornadofx.osgi.impl

import javafx.application.Application
import javafx.application.Platform
import javafx.stage.Stage
import org.osgi.framework.ServiceEvent
import org.osgi.framework.ServiceEvent.REGISTERED
import org.osgi.framework.ServiceEvent.UNREGISTERING
import org.osgi.framework.ServiceListener
import tornadofx.App
import tornadofx.FX
import tornadofx.osgi.ApplicationProvider
import kotlin.concurrent.thread
import kotlin.reflect.KClass

class ApplicationListener : Application(), ServiceListener {
    var delegate: App? = null
    val hasActiveApplication: Boolean get() = delegate != null

    fun isRunningApplication(appClass: KClass<out App>) = appClass == delegate?.javaClass?.kotlin

    companion object {
        lateinit var realPrimaryStage: Stage
        init {
            thread(true) {
                val originalClassLoader = Thread.currentThread().contextClassLoader
                Thread.currentThread().contextClassLoader = ApplicationListener::class.java.classLoader
                Application.launch(ApplicationListener::class.java)
                Thread.currentThread().contextClassLoader = originalClassLoader
            }
        }
    }

    override fun serviceChanged(event: ServiceEvent) {
        if (event.isApplicationProviderEvent()) {
            val appProvider = bundleContext.getService(event.serviceReference) as ApplicationProvider
            val entrypoint = appProvider.application

            if (event.type == REGISTERED) {
                if (!hasActiveApplication) startApplication(appProvider)
                else println("An application already running, not starting $entrypoint.")
            } else if (event.type == UNREGISTERING && isRunningApplication(appProvider.application)) {
                stopApplication()
            }
        }
    }

    override fun start(stage: Stage) {
        realPrimaryStage = stage
        FX.installErrorHandler()
    }

    override fun stop() {
        stopApplication()
    }

    fun startApplication(provider: ApplicationProvider) {
        delegate = provider.application.java.newInstance()
        delegate!!.init()
        Platform.runLater {
            println("Starting with realPrimaryStage")
            delegate!!.start(realPrimaryStage)
            realPrimaryStage.toFront()
        }
    }

    fun stopApplication() {
        delegate = null
        Platform.runLater {
            realPrimaryStage.hide()
        }
    }

    private fun ServiceEvent.isApplicationProviderEvent() = objectClass == ApplicationProvider::class.qualifiedName

}