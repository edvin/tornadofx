package tornadofx

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
import javafx.scene.input.Clipboard
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.stage.Window
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.logging.Logger
import java.util.prefs.Preferences
import kotlin.concurrent.thread
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface Injectable

abstract class Component {
    val config: Properties
        get() = _config.value

    val clipboard: Clipboard by lazy { Clipboard.getSystemClipboard() }

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

    /**
     * Store and retrieve preferences.
     *
     * Preferences are stored automatically in a OS specific way.
     * <ul>
     *     <li>Windows stores it in the registry at HKEY_CURRENT_USER/Software/JavaSoft/....</li>
     *     <li>Mac OS stores it at ~/Library/Preferences/com.apple.java.util.prefs.plist</li>
     *     <li>Linux stores it at ~/.java</li>
     * </ul>
     */
    fun preferences(nodename: String? = null, op: Preferences.() -> Unit) {
        val node = if (nodename != null) Preferences.userRoot().node(nodename) else Preferences.userNodeForPackage(FX.application.javaClass)
        op(node)
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

    inline fun <reified T : Fragment> fragment(): ReadOnlyProperty<Component, T> = object : ReadOnlyProperty<Component, T> {
        var fragment: T? = null

        override fun getValue(thisRef: Component, property: KProperty<*>): T {
            if (fragment == null) fragment = findFragment(T::class)
            return fragment!!
        }
    }

    inline fun <reified T : Any> di(): ReadOnlyProperty<Component, T> = object : ReadOnlyProperty<Component, T> {
        override fun getValue(thisRef: Component, property: KProperty<*>) = FX.dicontainer?.let { it.getInstance(T::class) } ?: throw AssertionError("Injector is not configured, so bean of type ${T::class} can not be resolved")
    }

    val primaryStage: Stage get() = FX.primaryStage

    @Deprecated("Clashes with Region.background, so runAsync is a better name", ReplaceWith("runAsync"), DeprecationLevel.WARNING)
    fun <T> background(func: () -> T) = task(func)

    fun <T> runAsync(func: () -> T) = task(func)
    infix fun <T> Task<T>.ui(func: (T) -> Unit) = success(func)
}

abstract class Controller : Component(), Injectable

abstract class UIComponent : Component() {
    var fxmlLoader: FXMLLoader? = null
    var modalStage: Stage? = null
    abstract val root: Parent

    fun init() {
        root.properties["tornadofx.uicomponent"] = this
    }

    fun openModal(stageStyle: StageStyle = StageStyle.DECORATED, modality: Modality = Modality.APPLICATION_MODAL, escapeClosesWindow: Boolean = true, owner: Window? = null, block: Boolean = false) {
        if (modalStage == null) {
            if (root !is Parent) {
                throw IllegalArgumentException("Only Parent Fragments can be opened in a Modal")
            } else {
                modalStage = Stage(stageStyle).apply {
                    titleProperty().bind(titleProperty)
                    initModality(modality)
                    if (owner != null) initOwner(owner)

                    if (root.scene != null) {
                        scene = root.scene
                        this@UIComponent.properties["tornadofx.scene"] = root.scene
                    } else {

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
                            this@UIComponent.properties["tornadofx.scene"] = this
                        }
                    }

                    if (block) {
                        if (FX.reloadStylesheetsOnFocus || FX.reloadStylesheetsOnFocus) {
                            thread(true) {
                                Thread.sleep(5000)
                                configureReloading()
                            }
                        }
                        showAndWait()
                    } else {
                        show()
                        configureReloading()
                    }
                }
            }
        }
    }

    private fun Stage.configureReloading() {
        if (FX.reloadStylesheetsOnFocus) reloadStylesheetsOnFocus()
        if (FX.reloadViewsOnFocus) reloadViewsOnFocus()
    }

    fun closeModal() = modalStage?.apply {
        close()
        modalStage = null
    }

    val titleProperty = SimpleStringProperty()
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