package tornadofx

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.stage.Stage
import sun.net.www.protocol.css.Handler
import java.net.MalformedURLException
import java.net.URL
import java.util.logging.Level
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

open class App : Application() {
    open val primaryView: KClass<out View> = DeterminedByParameter::class

    override fun start(stage: Stage) {
        FX.registerApplication(this, stage)

        try {
            val view = find(determinePrimaryView())

            stage.apply {
                scene = Scene(view.root)
                view.properties["tornadofx.scene"] = scene
                scene.stylesheets.addAll(FX.stylesheets)
                titleProperty().bind(view.titleProperty)
                show()
            }

            installCSSUrlHandler()
            FX.initialized.value = true
        } catch (ex: Exception) {
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), ex)
        }
    }

    /**
     * Try to retrieve a type safe stylesheet, and force installation of url handler
     * if it's missing. This typically happens in environments with atypical class loaders.
     *
     * Under normal circumstances, the CSS handler that comes with TornadoFX should be picked
     * up by the JVM automatically.
     */
    private fun installCSSUrlHandler() {
        try {
            URL("css://content:64")
        } catch (ex: MalformedURLException) {
            log.info("Installing CSS url handler, since it was not picked up automatically")
            try {
                URL.setURLStreamHandlerFactory(Handler.HandlerFactory())
            } catch (installFailed: Exception) {
                log.log(Level.WARNING, "Unable to install CSS url handler, type safe stylesheets might not work", ex)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun determinePrimaryView(): KClass<out View> {
        if (primaryView == DeterminedByParameter::class) {
            val viewClassName = parameters.named?.get("view-class") ?: throw IllegalArgumentException("No provided --view-class parameter and primaryView was not overridden. Choose one strategy to specify the primary View")
            val viewClass = Class.forName(viewClassName)
            if (View::class.java.isAssignableFrom(viewClass)) return viewClass.kotlin as KClass<out View>
            throw IllegalArgumentException("Class specified by --class-name is not a subclass of tornadofx.View")
        } else {
            return primaryView
        }
    }

    inline fun <reified T : Injectable> inject(): ReadOnlyProperty<App, T> = object : ReadOnlyProperty<App, T> {
        override fun getValue(thisRef: App, property: KProperty<*>) = find(T::class)
    }

    class DeterminedByParameter : View() {
        override val root = Pane()
    }

}