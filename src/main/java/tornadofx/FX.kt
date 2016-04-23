package tornadofx

import javafx.application.Application
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.scene.image.Image
import javafx.stage.Stage
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.logging.Logger
import kotlin.reflect.KClass

class FX {
    companion object {
        val log = Logger.getLogger("FX")
        val initialized = SimpleBooleanProperty(false)
        lateinit var primaryStage: Stage
        lateinit var application: Application
        val stylesheets = FXCollections.observableArrayList<String>()
        val components = HashMap<KClass<out Injectable>, Injectable>()
        val lock = Any()
        @JvmStatic
        var dicontainer: DIContainer? = null
        @JvmStatic
        var reloadStylesheetsOnFocus = false

        private val _locale: SimpleObjectProperty<Locale> = object : SimpleObjectProperty<Locale>() {
            override fun invalidated() = loadMessages()
        }
        var locale: Locale get() = _locale.get(); set(value) = _locale.set(value)
        fun localeProperty() = _locale

        private val _messages: SimpleObjectProperty<ResourceBundle> = SimpleObjectProperty()
        var messages: ResourceBundle get() = _messages.get(); set(value) = _messages.set(value)
        fun messagesProperty() = _messages

        /**
         * Load global resource bundle for the current locale. Triggered when the locale changes.
         */
        private fun loadMessages() {
            try {
                messages = ResourceBundle.getBundle("Messages", locale, FXResourceBundleControl.INSTANCE)
            } catch (ex: Exception) {
                log.fine({ "No global Messages found in locale $locale, using empty bundle" })
                messages = EmptyResourceBundle.INSTANCE
            }
        }

        fun installErrorHandler() {
            if (Thread.getDefaultUncaughtExceptionHandler() == null)
                Thread.setDefaultUncaughtExceptionHandler(DefaultErrorHandler())
        }

        init {
            locale = Locale.getDefault()
        }

        fun runAndWait(action: () -> Unit) {
            // run synchronously on JavaFX thread
            if (Platform.isFxApplicationThread()) {
                action()
                return
            }

            // queue on JavaFX thread and wait for completion
            val doneLatch = CountDownLatch(1)
            Platform.runLater {
                try {
                    action()
                } finally {
                    doneLatch.countDown()
                }
            }

            try {
                doneLatch.await()
            } catch (e: InterruptedException) {
                // ignore exception
            }
        }

        @JvmStatic
        fun registerApplication(application: Application, primaryStage: Stage) {
            FX.installErrorHandler()
            FX.primaryStage = primaryStage
            FX.application = application
            if (reloadStylesheetsOnFocus) primaryStage.reloadStylesheetsOnFocus()
        }

        @JvmStatic
        fun <T: Injectable> find(componentType: Class<T>) =
            find(componentType.kotlin)
    }
}

fun addStageIcon(icon: Image) {
    val adder = { FX.primaryStage.icons += icon }
    if (FX.initialized.value) adder() else FX.initialized.addListener { obs, o, n -> adder() }
}

fun reloadStylesheetsOnFocus() {
    FX.reloadStylesheetsOnFocus = true
}

fun importStylesheet(stylesheet: String) {
    val css = FX::class.java.getResource(stylesheet)
    FX.stylesheets.add(css.toExternalForm())
}

fun <T : Stylesheet> importStylesheet(stylesheetType: KClass<T>) =
        FX.stylesheets.add("css://${stylesheetType.java.name}")

inline fun <reified T : Injectable> find(): T = find(T::class)

@Suppress("UNCHECKED_CAST")
fun <T : Injectable> find(type: KClass<T>): T {
    if (!FX.components.containsKey(type)) {
        synchronized(FX.lock) {
            if (!FX.components.containsKey(type))
                FX.components[type] = type.java.newInstance()
        }
    }
    return FX.components[type] as T
}

interface DIContainer {
    fun <T : Any> getInstance(type: KClass<T>): T
}