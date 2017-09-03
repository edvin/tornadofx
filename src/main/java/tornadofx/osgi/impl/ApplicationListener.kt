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
import org.osgi.util.tracker.ServiceTracker
import tornadofx.*
import tornadofx.osgi.ApplicationProvider
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.reflect.KClass

internal class ApplicationListener(val context: BundleContext) : ServiceListener {
    val tracker = ServiceTracker<ApplicationProvider, Any>(context, context.createFilter("(&(objectClass=${ApplicationProvider::class.java.name}))"), null)

    val hasActiveApplication: Boolean get() = delegate != null

    fun isRunningApplication(appClass: KClass<out App>) = appClass == delegate?.javaClass?.kotlin

    init {
        tracker.open()
        tracker.withEach {
            startDelegateIfPossible(it)
        }
    }

    companion object {
        var delegate: App? = null
        var realPrimaryStage: Stage? = null

        private val fxRuntimeInitialized: Boolean
            get() {
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
                    launch<ProxyApplication>()
                    Thread.currentThread().contextClassLoader = originalClassLoader
                }
                print("Waiting for JavaFX Runtime Startup")
                do {
                    Thread.sleep(100)
                    print(".")
                } while (!fxRuntimeInitialized)
                println("[Done]")
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

    }

    override fun serviceChanged(event: ServiceEvent) {
        if (event.isApplicationProviderEvent()) {
            val appProvider = context.getService(event.serviceReference) as ApplicationProvider

            if (event.type == REGISTERED) {
                startDelegateIfPossible(appProvider)
            } else if (event.type == UNREGISTERING && isRunningApplication(appProvider.application)) {
                stopDelegate()
            }
        }
    }

    private fun startDelegateIfPossible(appProvider: ApplicationProvider) {
        if (!hasActiveApplication) startDelegate(appProvider)
        else println("An application already running, not starting ${appProvider.application}.")
    }

    class ProxyApplication : Application() {
        override fun start(stage: Stage) {
            realPrimaryStage = stage
            Platform.setImplicitExit(false)
            FX.installErrorHandler()
        }

        override fun stop() {
            stopDelegate()
        }
    }

    fun startDelegate(provider: ApplicationProvider) {
        ensureFxRuntimeInitialized()
        delegate = provider.application.java.newInstance()
        if (realPrimaryStage == null) {
            print("Waiting for Primary Stage to be initialized")
            while (realPrimaryStage == null) {
                Thread.sleep(100)
                print(".")
            }
            println("[Done]")
        }
        delegate!!.init()
        Platform.runLater {
            delegate!!.start(realPrimaryStage!!)
            realPrimaryStage!!.toFront()
        }
    }

    private fun ServiceEvent.isApplicationProviderEvent() = objectClass == ApplicationProvider::class.qualifiedName

}