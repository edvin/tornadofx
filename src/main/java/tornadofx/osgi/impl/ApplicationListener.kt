package tornadofx.osgi.impl

import javafx.application.Application
import javafx.application.Platform
import javafx.scene.control.Label
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
                if (!hasActiveApplication) startDelegate(appProvider)
                else println("An application already running, not starting $entrypoint.")
            } else if (event.type == UNREGISTERING && isRunningApplication(appProvider.application)) {
                stopDelegate()
            }
        }
    }

    override fun start(stage: Stage) {
        realPrimaryStage = stage
        Platform.setImplicitExit(false)
        FX.installErrorHandler()
    }

    override fun stop() {
        stopDelegate()
    }

    fun startDelegate(provider: ApplicationProvider) {
        delegate = provider.application.java.newInstance()
        delegate!!.init()
        Platform.runLater {
            delegate!!.start(realPrimaryStage)
            realPrimaryStage.toFront()
        }
    }

    fun stopDelegate() {
        Platform.runLater {
            delegate!!.stop()
            realPrimaryStage.close()
            realPrimaryStage.scene.root = Label("No TornadoFX OSGi Bundle running")
            delegate = null
        }
    }

    private fun ServiceEvent.isApplicationProviderEvent() = objectClass == ApplicationProvider::class.qualifiedName

}