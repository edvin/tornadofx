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
        FX.registerApplication(this, stage)

        try {
            val view = find(primaryView)

            stage.apply {
                scene = Scene(view.root)
                scene.stylesheets.addAll(FX.stylesheets)
                titleProperty().bind(view.titleProperty)
                show()
            }

            FX.initialized.value = true
        } catch (ex: Exception) {
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), ex)
        }
    }

    inline fun <reified T : Injectable> inject(): ReadOnlyProperty<App, T> = object : ReadOnlyProperty<App, T> {
        override fun getValue(thisRef: App, property: KProperty<*>) = find(T::class)
    }

}

abstract class SingleViewApp(title: String? = null) : App(), ViewContainer {
    override val primaryView = javaClass.kotlin
    override val titleProperty = SimpleStringProperty(title)

    init {
        FX.components[javaClass.kotlin] = this
    }

}