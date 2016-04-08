package tornadofx

import javafx.application.Application
import javafx.beans.property.SimpleStringProperty
import javafx.scene.Scene
import javafx.scene.layout.Pane
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

abstract class SingleViewApp(title: String? = null, override val root: Pane) : App(), ViewContainer {
    override val primaryView = javaClass.kotlin
    override val titleProperty = SimpleStringProperty(title)
}