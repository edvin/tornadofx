package tornadofx

import javafx.application.Application
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.stage.Stage
import kotlin.reflect.KClass

abstract class App : Application() {
    abstract val primaryView: KClass<out View>

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

}