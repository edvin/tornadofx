package tornadofx

import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.concurrent.Task
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class Component {
    val config: Properties
        get() = _config.value

    fun Properties.set(pair: Pair<String, Any?>) = set(pair.first, pair.second?.toString())
    fun Properties.string(key: String, defaultValue: String? = null) = config.getProperty(key, defaultValue)
    fun Properties.boolean(key: String) = config.getProperty(key)?.toBoolean() ?: false
    fun Properties.double(key: String) = config.getProperty(key)?.toDouble()
    fun Properties.save() = Files.newOutputStream(configPath.value).use { output -> store(output, "") }

    private val _config = lazy {
        Properties().apply {
            if (Files.exists(configPath.value))
                Files.newInputStream(configPath.value).use { load(it) }
        }
    }

    private val configPath = lazy {
        val conf = Paths.get("conf")
        if (!Files.exists(conf))
            Files.createDirectories(conf)
        conf.resolve(javaClass.name.concat(".properties"))
    }

    inline fun <reified T : Component> inject(): ReadOnlyProperty<Component, T> = object : ReadOnlyProperty<Component, T> {
        override fun getValue(thisRef: Component, property: KProperty<*>) = find(T::class)
    }

    val primaryStage: Stage get() = FX.primaryStage

    fun <T> background(func: () -> T) = object : Task<T>() {
        override fun call(): T {
            return func()
        }
    }.apply {
        setOnFailed({ Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), exception) })
        Thread(this).start()
    }

    infix fun <T> Task<T>.ui(func: (T) -> Unit): Task<T> {
        Platform.runLater {
            setOnSucceeded { func(value) }
        }
        return this
    }
}

abstract class Controller : Component()

abstract class UIComponent : Component() {
    abstract val root: Parent
    private val lock = Any()

    val titleProperty = SimpleStringProperty()
    var title: String
        get() = titleProperty.get()
        set(value) = titleProperty.set(value)

    inline fun <reified T : Node> fxml(): FXMLWrapper<T> {
        val wrapper = FXMLWrapper<T>()
        wrapper.load(this)
        return wrapper
    }

    class FXMLWrapper<T : Node> : ReadOnlyProperty<UIComponent, T> {
        var value: T? = null

        fun load(uiComponent: UIComponent): T {
            synchronized(uiComponent.lock) {
                val componentType = uiComponent.javaClass

                val fxml = componentType.getResource(componentType.simpleName.concat(".fxml")) ?:
                        throw IllegalArgumentException("FXML not found for $componentType")

                val loader = FXMLLoader(fxml)
                loader.setController(uiComponent)
                value = loader.load()
                return value as T
            }
        }

        override fun getValue(thisRef: UIComponent, property: KProperty<*>) =
                value ?: load(thisRef)
    }

}

abstract class Fragment : UIComponent() {
    var modalStage: Stage? = null

    fun openModal(stageStyle: StageStyle = StageStyle.DECORATED, modality: Modality = Modality.APPLICATION_MODAL, escapeClosesWindow: Boolean = true) {
        if (modalStage == null) {
            if (root !is Parent) {
                throw IllegalArgumentException("Only Parent Fragments can be opened in a Modal")
            } else {
                modalStage = Stage(stageStyle).apply {
                    titleProperty().bind(titleProperty)
                    initModality(modality)

                    Scene(root as Parent).apply {
                        if (escapeClosesWindow) {
                            addEventFilter(KeyEvent.KEY_PRESSED) {
                                if (it.code == KeyCode.ESCAPE)
                                    closeModal()
                            }
                        }

                        stylesheets.addAll(FX.stylesheets)
                        scene = this
                    }

                    show()
                }

                Platform.runLater { root.requestFocus() }
            }
        }
    }

    fun closeModal() = modalStage?.apply {
        close()
        modalStage = null
    }

}

abstract class View : UIComponent()