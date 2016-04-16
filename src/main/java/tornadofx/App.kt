package tornadofx

import javafx.application.Application
import javafx.beans.property.SimpleStringProperty
import javafx.scene.Scene
import javafx.stage.Stage
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class App : Application() {
    abstract val primaryView: KClass<out ViewContainer>

    override fun start(stage: Stage) {
        installErrorHandler()

        FX.primaryStage = stage
        FX.application = this

        try {
            val view = find(primaryView)

            stage.apply {
                scene = Scene(view.root)
                scene.stylesheets.addAll(FX.stylesheets)
                titleProperty().bind(view.titleProperty)
                show()
            }
        } catch (ex: Exception) {
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), ex)
        }
    }

    private fun installErrorHandler() {
        if (Thread.getDefaultUncaughtExceptionHandler() == null)
            Thread.setDefaultUncaughtExceptionHandler(DefaultErrorHandler())
    }

    inline fun <reified T : Injectable> inject(): ReadOnlyProperty<App, T> = object : ReadOnlyProperty<App, T> {
        override fun getValue(thisRef: App, property: KProperty<*>) = find(T::class)
    }

}

abstract class SingleViewApp(title: String? = null) : App(), ViewContainer {
    private var stylesheet: Stylesheet? = null
    override val primaryView = javaClass.kotlin
    override val titleProperty = SimpleStringProperty(title)

    init {
        FX.components[javaClass.kotlin] = this
    }

    override fun start(stage: Stage) {
        stylesheet?.apply { FX.stylesheets.add(base64URL.toExternalForm()) }
        super.start(stage)
    }

    fun css(op: (Stylesheet.() -> Unit)) {
        stylesheet = Stylesheet().apply { op(this) }
    }
}
