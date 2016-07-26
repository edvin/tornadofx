package tornadofx

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.layout.Pane
import javafx.stage.Stage
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

open class App(primaryView: KClass<out View>? = null, icon: Image? = null, vararg stylesheet: KClass<out Stylesheet>) : Application() {
    constructor(primaryView: KClass<out View>? = null, vararg stylesheet: KClass<out Stylesheet>) : this(primaryView, null, *stylesheet)

    open val primaryView: KClass<out View> = primaryView ?: DeterminedByParameter::class

    init {
        icon?.let { addStageIcon(it) }
        stylesheet.forEach { importStylesheet(it) }
    }

    override fun start(stage: Stage) {
        FX.registerApplication(this, stage)

        try {
            val view = find(determinePrimaryView())

            stage.apply {
                scene = Scene(view.root)
                view.properties["tornadofx.scene"] = scene
                scene.stylesheets.addAll(FX.stylesheets)
                titleProperty().bind(view.titleProperty)
                hookLayoutDebuggerShortcut()
                show()
            }
            FX.initialized.value = true
        } catch (ex: Exception) {
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), ex)
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