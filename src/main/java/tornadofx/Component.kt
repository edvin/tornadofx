package tornadofx

import com.sun.javafx.fxml.LoadListener
import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.concurrent.Task
import javafx.event.EventDispatchChain
import javafx.event.EventTarget
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.ProgressIndicator
import javafx.scene.input.Clipboard
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
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
import kotlin.reflect.*

interface Injectable

abstract class Component {
    val config: Properties
        get() = _config.value

    val clipboard: Clipboard by lazy { Clipboard.getSystemClipboard() }

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

    val properties by lazy { FXCollections.observableHashMap<Any, Any>() }
    val log by lazy { Logger.getLogger(this@Component.javaClass.name) }

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

    inline fun <reified T> inject(): ReadOnlyProperty<Component, T> where T : Component, T : Injectable = object : ReadOnlyProperty<Component, T> {
        override fun getValue(thisRef: Component, property: KProperty<*>) = find(T::class)
    }

    inline fun <reified T : Fragment> fragment(): ReadOnlyProperty<Component, T> = object : ReadOnlyProperty<Component, T> {
        var fragment: T? = null

        override fun getValue(thisRef: Component, property: KProperty<*>): T {
            if (fragment == null) fragment = find(T::class)
            return fragment!!
        }
    }

    inline fun <reified T : Any> di(): ReadOnlyProperty<Component, T> = object : ReadOnlyProperty<Component, T> {
        override fun getValue(thisRef: Component, property: KProperty<*>) = FX.dicontainer?.let { it.getInstance(T::class) } ?: throw AssertionError("Injector is not configured, so bean of type ${T::class} can not be resolved")
    }

    val primaryStage: Stage get() = FX.primaryStage

    @Deprecated("Clashes with Region.background, so runAsync is a better name", ReplaceWith("runAsync"), DeprecationLevel.WARNING)
    fun <T> background(func: () -> T) = task(func)

    /**
     * Perform the given operation on an Injectable of the specified type asynchronousyly.
     *
     * MyController::class.runAsync { functionOnMyController() } ui { processResultOnUiThread(it) }
     */
    inline fun <reified T, R> KClass<T>.runAsync(noinline op: T.() -> R) where T : Component, T : Injectable = task { op(find(T::class)) }

    /**
     * Perform the given operation on an Injectable class function member asynchronousyly.
     *
     * CustomerController::listContacts.runAsync(customerId) { processResultOnUiThread(it) }
     */
    inline fun <reified InjectableType, reified ReturnType> KFunction1<InjectableType, ReturnType>.runAsync(noinline doOnUi: ((ReturnType) -> Unit)? = null): Task<ReturnType>
            where InjectableType : Component, InjectableType : Injectable {
        val t = task { invoke(find(InjectableType::class)) }
        if (doOnUi != null) t.ui(doOnUi)
        return t
    }

    /**
     * Perform the given operation on an Injectable class function member asynchronousyly.
     *
     * CustomerController::listCustomers.runAsync { processResultOnUiThread(it) }
     */
    inline fun <reified InjectableType, reified P1, reified ReturnType> KFunction2<InjectableType, P1, ReturnType>.runAsync(p1: P1, noinline doOnUi: ((ReturnType) -> Unit)? = null)
            where InjectableType : Component, InjectableType : Injectable
            = task { invoke(find(InjectableType::class), p1) }.apply { if (doOnUi != null) ui(doOnUi) }

    inline fun <reified InjectableType, reified P1, reified P2, reified ReturnType> KFunction3<InjectableType, P1, P2, ReturnType>.runAsync(p1: P1, p2: P2, noinline doOnUi: ((ReturnType) -> Unit)? = null)
            where InjectableType : Component, InjectableType : Injectable
            = task { invoke(find(InjectableType::class), p1, p2) }.apply { if (doOnUi != null) ui(doOnUi) }

    inline fun <reified InjectableType, reified P1, reified P2, reified P3, reified ReturnType> KFunction4<InjectableType, P1, P2, P3, ReturnType>.runAsync(p1: P1, p2: P2, p3: P3, noinline doOnUi: ((ReturnType) -> Unit)? = null)
            where InjectableType : Component, InjectableType : Injectable
            = task { invoke(find(InjectableType::class), p1, p2, p3) }.apply { if (doOnUi != null) ui(doOnUi) }

    inline fun <reified InjectableType, reified P1, reified P2, reified P3, reified P4, reified ReturnType> KFunction5<InjectableType, P1, P2, P3, P4, ReturnType>.runAsync(p1: P1, p2: P2, p3: P3, p4: P4, noinline doOnUi: ((ReturnType) -> Unit)? = null)
            where InjectableType : Component, InjectableType : Injectable
            = task { invoke(find(InjectableType::class), p1, p2, p3, p4) }.apply { if (doOnUi != null) ui(doOnUi) }

    /**
     * Find the given property inside the given Injectable. Useful for assigning a property from a View or Controller
     * in any Component. Example:
     *
     * val person = find(UserController::currentPerson)
     */
    inline fun <reified InjectableType, T> get(prop: KProperty1<InjectableType, T>): T
            where InjectableType : Component, InjectableType : Injectable {
        val injectable = find(InjectableType::class)
        return prop.get(injectable)
    }

    inline fun <reified InjectableType, T> set(prop: KMutableProperty1<InjectableType, T>, value: T)
            where InjectableType : Component, InjectableType : Injectable {
        val injectable = find(InjectableType::class)
        return prop.set(injectable, value)
    }

    fun <T> runAsync(func: () -> T) = task(func)
    /**
     * Replace this node with a progress node while a long running task
     * is running and swap it back when complete.
     *
     * The default progress node is a ProgressIndicator that fills the same
     * client area as the parent. You can swap the progress node for any Node you like.
     */
    fun <T> Node.runAsyncWithProgress(progress: Node = ProgressIndicator(), op: () -> T): Task<T> {
        if (progress is Region)
            progress.setPrefSize(boundsInParent.width, boundsInParent.height)
        val children = parent.getChildList() ?: throw IllegalArgumentException("This node has no child list, and cannot contain the progress node")
        val index = children.indexOf(this)
        children.add(index, progress)
        removeFromParent()
        return task {
            val result = op()
            Platform.runLater {
                children.add(index, this)
                progress.removeFromParent()
            }
            result
        }
    }

    infix fun <T> Task<T>.ui(func: (T) -> Unit) = success(func)
}

abstract class Controller : Component(), Injectable

const val UI_COMPONENT_PROPERTY = "tornadofx.uicomponent"

abstract class UIComponent(viewTitle: String? = "") : Component(), EventTarget {
    override fun buildEventDispatchChain(tail: EventDispatchChain?): EventDispatchChain {
        throw UnsupportedOperationException("not implemented")
    }

    var fxmlLoader: FXMLLoader? = null
    var modalStage: Stage? = null
    internal var reloadInit = false
    internal var muteDocking = false
    abstract val root: Parent
    var onDockListeners: MutableList<(UIComponent) -> Unit>? = null
    var onUndockListeners: MutableList<(UIComponent) -> Unit>? = null

    fun init() {
        root.properties[UI_COMPONENT_PROPERTY] = this
        root.parentProperty().addListener({ observable, oldParent, newParent ->
            if (modalStage != null) return@addListener
            if (newParent == null && oldParent != null) callOnUndock()
            if (newParent != null && newParent != oldParent) callOnDock()
        })
        root.sceneProperty().addListener({ observable, oldParent, newParent ->
            if (modalStage != null || root.parent != null) return@addListener
            if (newParent == null && oldParent != null) callOnUndock()
            if (newParent != null && newParent != oldParent) callOnDock()
        })
    }

    open fun onUndock() {
    }

    open fun onDock() {
    }

    internal fun callOnDock() {
        if (muteDocking) return
        onDock()
        onDockListeners?.forEach { it.invoke(this) }
    }

    internal fun callOnUndock() {
        if (muteDocking) return
        onUndock()
        onUndockListeners?.forEach { it.invoke(this) }
    }

    fun openModal(stageStyle: StageStyle = StageStyle.DECORATED, modality: Modality = Modality.APPLICATION_MODAL, escapeClosesWindow: Boolean = true, owner: Window? = null, block: Boolean = false) {
        if (modalStage == null) {
            if (root !is Parent) {
                throw IllegalArgumentException("Only Parent Fragments can be opened in a Modal")
            } else {
                modalStage = Stage(stageStyle)
                // modalStage needs to be set before this code to make closeModal() work in blocking mode
                with(modalStage!!) {
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

                            FX.applyStylesheetsTo(this)
                            icons += FX.primaryStage.icons
                            scene = this
                            this@UIComponent.properties["tornadofx.scene"] = this
                        }
                    }

                    hookGlobalShortcuts()

                    showingProperty().onChange {
                        if (it == true) {
                            callOnDock()
                        } else {
                            modalStage = null
                            callOnUndock()
                        }
                    }

                    if (block) {
                        if (FX.reloadStylesheetsOnFocus || FX.reloadViewsOnFocus) {
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
        } else {
            if (!modalStage!!.isShowing)
                modalStage!!.show()
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

    val titleProperty = SimpleStringProperty(viewTitle)
    var title: String
        get() = titleProperty.get() ?: ""
        set(value) = titleProperty.set(value)

    /**
     * Load an FXML file from the specified location, or from a file with the same package and name as this UIComponent
     * if not specified. If the FXML file specifies a controller (handy for content completion in FXML editors)
     * set the `hasControllerAttribute` parameter to true. This ensures that the `fx:controller` attribute is ignored
     * by the loader so that this UIComponent can still be the controller for the FXML file.
     *
     * Important: If you specify `hasControllerAttribute = true` when infact no `fx:controller` attribute is present,
     * no controller will be set at all. Make sure to only specify this parameter if you actually have the `fx:controller`
     * attribute in your FXML.
     */
    fun <T : Node> fxml(location: String? = null, hasControllerAttribute: Boolean = false): ReadOnlyProperty<UIComponent, T> = object : ReadOnlyProperty<UIComponent, T> {
        val value: T

        init {
            val componentType = this@UIComponent.javaClass
            val targetLocation = location ?: componentType.simpleName + ".fxml"
            val fxml = componentType.getResource(targetLocation) ?:
                    throw IllegalArgumentException("FXML not found for $componentType")

            fxmlLoader = FXMLLoader(fxml).apply {
                resources = this@UIComponent.messages
                if (hasControllerAttribute) {
                    setControllerFactory { this@UIComponent }
                } else {
                    setController(this@UIComponent)
                }
            }

            value = fxmlLoader!!.load()
        }

        override fun getValue(thisRef: UIComponent, property: KProperty<*>) = value
    }

    inline fun <reified T : EventTarget> fxid(propName: String? = null) = object : ReadOnlyProperty<UIComponent, T> {
        override fun getValue(thisRef: UIComponent, property: KProperty<*>): T {
            val key = propName ?: property.name
            val value = thisRef.fxmlLoader!!.namespace[key]
            if (value is T) return value
            if (value == null)
                log.warning("Property $key of $thisRef was not resolved because there is no matching fx:id in ${thisRef.fxmlLoader!!.location}")
            else
                log.warning("Property $key of $thisRef did not resolve to the correct type. Check declaration in ${thisRef.fxmlLoader!!.location}")

            throw IllegalArgumentException("Property $key does not match fx:id declaration")
        }
    }

    fun <T : UIComponent> replaceWith(component: KClass<T>, transition: ViewTransition? = null): Boolean {
        return replaceWith(find(component), transition)
    }

    /**
     * Replace this component with another, optionally using a transition animation.
     *
     * @param replacement The component that will replace this one
     * @param transition The [ViewTransition] used to animate the transition
     * @return Whether or not the transition will run
     */
    fun replaceWith(replacement: UIComponent, transition: ViewTransition? = null): Boolean {
        return root.replaceWith(replacement.root, transition) {
            if (root == root.scene?.root) (root.scene.window as? Stage)?.titleProperty()?.cleanBind(replacement.titleProperty)
        }
    }

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("View Transitions are now created with the `ViewTransition` class")
    inline fun <reified T : View> replaceWith(view: KClass<T>, noinline transition: ((UIComponent, UIComponent, transitionCompleteCallback: () -> Unit) -> Unit)? = null): Boolean {
        @Suppress("DEPRECATION")
        return replaceWith(find(view), transition)
    }

    @JvmName("replaceWithFragment")
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("View Transitions are now created with the `ViewTransition` class")
    inline fun <reified T : Fragment> replaceWith(fragment: KClass<T>, noinline transition: ((UIComponent, UIComponent, transitionCompleteCallback: () -> Unit) -> Unit)? = null): Boolean {
        @Suppress("DEPRECATION")
        return replaceWith(find(fragment), transition)
    }

    @Deprecated("View Transitions are now created with the ViewTransition class")
    fun replaceWith(replacement: UIComponent, transition: ((UIComponent, UIComponent, transitionCompleteCallback: () -> Unit) -> Unit)? = null): Boolean {
        if (root == root.scene?.root) {
            val scene = root.scene

            if (scene.window is Stage) {
                val stage = scene.window as Stage
                stage.titleProperty().unbind()
                stage.titleProperty().bind(replacement.titleProperty)
            }

            if (transition != null) {
                muteDocking = true
                replacement.muteDocking = true

                val temp = StackPane(root, replacement.root)
                scene.root = temp

                transition.invoke(this@UIComponent, replacement, {
                    temp.children.remove(replacement.root)
                    undockFromParent(replacement)
                    replacement.muteDocking = false
                    muteDocking = false
                    scene.root = replacement.root
                })

                return true
            } else {
                undockFromParent(replacement)
                root.scene.root = replacement.root
            }

            return true
        } else if (root.parent is Pane) {
            var unwrapper: ((Node) -> Unit)? = null

            val replaceDelegate = if (root.parent is BorderPane) {
                val borderpane = root.parent as BorderPane
                val tempParent = StackPane()
                if (borderpane.top == root) {
                    borderpane.top = tempParent
                    unwrapper = { borderpane.top = it }
                }
                if (borderpane.bottom == root) {
                    borderpane.bottom = tempParent
                    unwrapper = { borderpane.bottom = it }
                }
                if (borderpane.center == root) {
                    borderpane.center = tempParent
                    unwrapper = { borderpane.center = it }
                }
                if (borderpane.left == root) {
                    borderpane.left = tempParent
                    unwrapper = { borderpane.left = it }
                }
                if (borderpane.right == root) {
                    borderpane.right = tempParent
                    unwrapper = { borderpane.right = it }
                }
                root.removeFromParent()
                tempParent.add(root)
                tempParent
            } else {
                root.parent as Pane
            }
            replaceDelegate.apply {
                val childList = getChildList()!!

                val index = childList.indexOf(root)
                root.removeFromParent()

                if (transition != null) {
                    val temp = StackPane(root, replacement.root)
                    childList.add(index, temp)

                    transition.invoke(this@UIComponent, replacement, {
                        root.removeFromParent()
                        replacement.root.removeFromParent()
                        temp.removeFromParent()
                        childList.add(index, replacement.root)
                        unwrapper?.invoke(replacement.root)
                    })
                } else {
                    replacement.root.removeFromParent()
                    childList.add(index, replacement.root)
                    unwrapper?.invoke(replacement.root)
                }
                return true
            }
        }

        return false
    }

    private fun undockFromParent(replacement: UIComponent) {
        if (replacement.root.parent is Pane) (replacement.root.parent as Pane).children.remove(replacement.root)
    }

}

@Suppress("UNCHECKED_CAST")
fun <U : UIComponent> U.whenDocked(listener: (U) -> Unit) {
    if (onDockListeners == null) onDockListeners = mutableListOf()
    onDockListeners!!.add(listener as (UIComponent) -> Unit)
}

@Suppress("UNCHECKED_CAST")
fun <U : UIComponent> U.whenUndocked(listener: (U) -> Unit) {
    if (onUndockListeners == null) onUndockListeners = mutableListOf()
    onUndockListeners!!.add(listener as (UIComponent) -> Unit)
}

abstract class Fragment(title: String? = null) : UIComponent(title)

abstract class View(title: String? = null) : UIComponent(title), Injectable

class ResourceLookup(val component: Component) {
    operator fun get(resource: String): String? = component.javaClass.getResource(resource)?.toExternalForm()
    fun url(resource: String): URL? = component.javaClass.getResource(resource)
}
