package tornadofx

import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableMap
import javafx.concurrent.Task
import javafx.event.EventTarget
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.logging.Logger
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface Injectable

abstract class Component {
    val config: Properties
        get() = _config.value

    val properties: ObservableMap<Any, Any>
        get() = _properties.value

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

    private val _properties = lazy { FXCollections.observableHashMap<Any, Any>() }
    private val _log = lazy { Logger.getLogger(this@Component.javaClass.name) }
    val log: Logger get() = _log.value

    private val configPath = lazy {
        val conf = Paths.get("conf")
        if (!Files.exists(conf))
            Files.createDirectories(conf)
        conf.resolve(javaClass.name + ".properties")
    }

    private val _messages: SimpleObjectProperty<ResourceBundle> = object : SimpleObjectProperty<ResourceBundle>() {
        override fun get(): ResourceBundle? {
            if (super.get() == null) {
                try {
                    val bundle = ResourceBundle.getBundle(this@Component.javaClass.name, FX.locale, FXResourceBundleControl.INSTANCE)
                    if (bundle is FXPropertyResourceBundle)
                        bundle.inheritFromGlobal()
                    set(bundle)
                } catch (ex: Exception) {
                    FX.log.fine({ "No Messages found for ${javaClass.name} in locale ${FX.locale}, using global bundle" })
                    set(FX.messages)
                }
            }
            return super.get()
        }
    }

    var messages: ResourceBundle
        get() = _messages.get()
        set(value) = _messages.set(value)

    val resources: ResourceLookup by lazy {
        ResourceLookup(this)
    }

    inline fun <reified T : Injectable> inject(): ReadOnlyProperty<Component, T> = object : ReadOnlyProperty<Component, T> {
        override fun getValue(thisRef: Component, property: KProperty<*>) = find(T::class)
    }

    inline fun <reified T : Any> di(): ReadOnlyProperty<Component, T> = object : ReadOnlyProperty<Component, T> {
        override fun getValue(thisRef: Component, property: KProperty<*>) = FX.dicontainer?.let { it.getInstance(T::class) } ?: throw AssertionError("Injector is not configured, so bean of type ${T::class} can not be resolved")
    }

    val primaryStage: Stage get() = FX.primaryStage

    fun <T> background(func: () -> T) = task(func)
    infix fun <T> Task<T>.ui(func: (T) -> Unit) = success(func)
}

abstract class Controller : Component(), Injectable

interface ViewContainer : Injectable {
    val root: Parent
    val titleProperty: Property<String>
}

abstract class UIComponent : Component(), ViewContainer {
    var fxmlLoader: FXMLLoader? = null
    var modalStage: Stage? = null

    fun openModal(stageStyle: StageStyle = StageStyle.DECORATED, modality: Modality = Modality.APPLICATION_MODAL, escapeClosesWindow: Boolean = true) {
        if (modalStage == null) {
            if (root !is Parent) {
                throw IllegalArgumentException("Only Parent Fragments can be opened in a Modal")
            } else {
                modalStage = Stage(stageStyle).apply {
                    titleProperty().bind(titleProperty)
                    initModality(modality)

                    Scene(root).apply {
                        if (escapeClosesWindow) {
                            addEventFilter(KeyEvent.KEY_PRESSED) {
                                if (it.code == KeyCode.ESCAPE)
                                    closeModal()
                            }
                        }

                        stylesheets.addAll(FX.stylesheets)
                        icons += FX.primaryStage.icons
                        scene = this
                    }

                    show()
                    if (FX.reloadStylesheetsOnFocus) reloadStylesheetsOnFocus()
                }
            }
        }
    }

    fun closeModal() = modalStage?.apply {
        close()
        modalStage = null
    }

    override val titleProperty = SimpleStringProperty()
    var title: String
        get() = titleProperty.get()
        set(value) = titleProperty.set(value)

    fun <T : Node> fxml(location: String? = null): ReadOnlyProperty<UIComponent, T> = object : ReadOnlyProperty<UIComponent, T> {
        val value: T

        init {
            val componentType = this@UIComponent.javaClass
            val targetLocation = location ?: componentType.simpleName + ".fxml"
            val fxml = componentType.getResource(targetLocation) ?:
                    throw IllegalArgumentException("FXML not found for $componentType")

            fxmlLoader = FXMLLoader(fxml).apply {
                resources = this@UIComponent.messages
                setController(this@UIComponent)
            }

            value = fxmlLoader!!.load()
        }

        override fun getValue(thisRef: UIComponent, property: KProperty<*>) = value
    }


    inline fun <reified T : EventTarget> fxid() = object : ReadOnlyProperty<UIComponent, T> {
        var value: T? = null

        override fun getValue(thisRef: UIComponent, property: KProperty<*>): T {
            return value ?: thisRef.fxmlLoader!!.namespace[property.name] as T
        }
    }

}

abstract class Fragment : UIComponent()

abstract class View : UIComponent(), Injectable

class ResourceLookup(val component: Component) {
    operator fun get(resource: String): String? = component.javaClass.getResource(resource)?.toExternalForm()
    fun url(resource: String): URL? = component.javaClass.getResource(resource)
}