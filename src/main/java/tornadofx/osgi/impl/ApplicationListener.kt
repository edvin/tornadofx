package tornadofx.osgi.impl

import com.sun.javafx.application.PlatformImpl
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.control.Label
import javafx.stage.Stage
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceEvent
import org.osgi.framework.ServiceEvent.REGISTERED
import org.osgi.framework.ServiceEvent.UNREGISTERING
import org.osgi.framework.ServiceListener
import tornadofx.App
import tornadofx.FX
import tornadofx.osgi.ApplicationProvider
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.reflect.KClass

class ApplicationListener : Application(), ServiceListener {
    var delegate: App? = null
    val hasActiveApplication: Boolean get() = delegate != null

    fun isRunningApplication(appClass: KClass<out App>) = appClass == delegate?.javaClass?.kotlin

    companion object {
        var realPrimaryStage: Stage? = null

        private val fxRuntimeInitialized: Boolean get() {
            val initializedField = PlatformImpl::class.java.getDeclaredField("initialized")
            initializedField.isAccessible = true
            val initialized = initializedField.get(null) as AtomicBoolean
            return initialized.get()
        }

        private fun ensureFxRuntimeInitialized() {
            if (!fxRuntimeInitialized) {
                thread(true) {
                    val originalClassLoader = Thread.currentThread().contextClassLoader
                    Thread.currentThread().contextClassLoader = ApplicationListener::class.java.classLoader
                    launch(ApplicationListener::class.java)
                    Thread.currentThread().contextClassLoader = originalClassLoader
                }
                do {
                    println("Waiting for JavaFX Runtime Startup...")
                    Thread.sleep(100)
                } while (!fxRuntimeInitialized)
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
            } else if (event.type == UNREGISTERING && isRunningApplication(entrypoint)) {
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
        ensureFxRuntimeInitialized()
        delegate = provider.application.java.newInstance()
        while (realPrimaryStage == null) {
            println("Waiting for Primary Stage to be initialized...")
            Thread.sleep(100)
        }
        delegate!!.init()
        Platform.runLater {
            delegate!!.start(realPrimaryStage!!)
            realPrimaryStage!!.toFront()
        }
    }

    fun stopDelegate() {
        Platform.runLater {
            delegate!!.stop()
            realPrimaryStage!!.close()
            realPrimaryStage!!.scene.root = Label("No TornadoFX OSGi Bundle running")
            delegate = null
        }
    }

    private fun ServiceEvent.isApplicationProviderEvent() = objectClass == ApplicationProvider::class.qualifiedName

    fun lookForApplicationProviders(context: BundleContext) {
        val refs = context.getServiceReferences(ApplicationProvider::class.java, "(Language=*)")
        if (refs != null && refs.size > 0) {
            val provider = context.getService(refs.first())
            if (provider != null) startDelegate(provider)
        }
    }

}