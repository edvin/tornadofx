package tornadofx

import javafx.application.Platform
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.SimpleBooleanProperty
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
import javafx.scene.control.*
import javafx.scene.input.Clipboard
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.stage.Window
import javafx.util.Callback
import java.io.InputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.logging.Logger
import java.util.prefs.Preferences
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.*

interface Injectable

abstract class Component {
    open val scope: Scope = FX.inheritScopeHolder.get()
    val params: Map<String, Any> = FX.inheritParamHolder.get() ?: mapOf()
    val subscribedEvents = HashMap<KClass<out FXEvent>, ArrayList<(FXEvent) -> Unit>>()

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
        val node = if (nodename != null) Preferences.userRoot().node(nodename) else Preferences.userNodeForPackage(FX.getApplication(scope)!!.javaClass)
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

    inline fun <reified T> inject(overrideScope: Scope = scope, vararg params: Pair<String, Any>): ReadOnlyProperty<Component, T> where T : Component, T : Injectable = object : ReadOnlyProperty<Component, T> {
        override fun getValue(thisRef: Component, property: KProperty<*>) = find(T::class, overrideScope, *params)
    }

    inline fun <reified T> param(): ReadOnlyProperty<Component, T> = object : ReadOnlyProperty<Component, T> {
        override fun getValue(thisRef: Component, property: KProperty<*>) = params[property.name] as T
    }

    inline fun <reified T : Fragment> fragment(overrideScope: Scope = scope, vararg params: Pair<String, Any>): ReadOnlyProperty<Component, T> = object : ReadOnlyProperty<Component, T> {
        var fragment: T? = null

        override fun getValue(thisRef: Component, property: KProperty<*>): T {
            if (fragment == null) fragment = find(T::class, overrideScope, *params)
            return fragment!!
        }
    }

    inline fun <reified T : Any> di(): ReadOnlyProperty<Component, T> = object : ReadOnlyProperty<Component, T> {
        var injected: T? = null
        override fun getValue(thisRef: Component, property: KProperty<*>): T {
            if (injected == null) injected = FX.dicontainer?.let { it.getInstance(T::class) } ?: throw AssertionError("Injector is not configured, so bean of type ${T::class} can not be resolved")
            return injected!!
        }
    }

    val primaryStage: Stage get() = FX.getPrimaryStage(scope)!!

    @Deprecated("Clashes with Region.background, so runAsync is a better name", ReplaceWith("runAsync"), DeprecationLevel.WARNING)
    fun <T> background(func: () -> T) = task(func)

    /**
     * Perform the given operation on an Injectable of the specified type asynchronousyly.
     *
     * MyController::class.runAsync { functionOnMyController() } ui { processResultOnUiThread(it) }
     */
    inline fun <reified T, R> KClass<T>.runAsync(noinline op: T.() -> R) where T : Component, T : Injectable = task { op(find(T::class, scope)) }

    /**
     * Perform the given operation on an Injectable class function member asynchronousyly.
     *
     * CustomerController::listContacts.runAsync(customerId) { processResultOnUiThread(it) }
     */
    inline fun <reified InjectableType, reified ReturnType> KFunction1<InjectableType, ReturnType>.runAsync(noinline doOnUi: ((ReturnType) -> Unit)? = null): Task<ReturnType>
            where InjectableType : Component, InjectableType : Injectable {
        val t = task { invoke(find(InjectableType::class, scope)) }
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
            = task { invoke(find(InjectableType::class, scope), p1) }.apply { if (doOnUi != null) ui(doOnUi) }

    inline fun <reified InjectableType, reified P1, reified P2, reified ReturnType> KFunction3<InjectableType, P1, P2, ReturnType>.runAsync(p1: P1, p2: P2, noinline doOnUi: ((ReturnType) -> Unit)? = null)
            where InjectableType : Component, InjectableType : Injectable
            = task { invoke(find(InjectableType::class, scope), p1, p2) }.apply { if (doOnUi != null) ui(doOnUi) }

    inline fun <reified InjectableType, reified P1, reified P2, reified P3, reified ReturnType> KFunction4<InjectableType, P1, P2, P3, ReturnType>.runAsync(p1: P1, p2: P2, p3: P3, noinline doOnUi: ((ReturnType) -> Unit)? = null)
            where InjectableType : Component, InjectableType : Injectable
            = task { invoke(find(InjectableType::class, scope), p1, p2, p3) }.apply { if (doOnUi != null) ui(doOnUi) }

    inline fun <reified InjectableType, reified P1, reified P2, reified P3, reified P4, reified ReturnType> KFunction5<InjectableType, P1, P2, P3, P4, ReturnType>.runAsync(p1: P1, p2: P2, p3: P3, p4: P4, noinline doOnUi: ((ReturnType) -> Unit)? = null)
            where InjectableType : Component, InjectableType : Injectable
            = task { invoke(find(InjectableType::class, scope), p1, p2, p3, p4) }.apply { if (doOnUi != null) ui(doOnUi) }

    /**
     * Find the given property inside the given Injectable. Useful for assigning a property from a View or Controller
     * in any Component. Example:
     *
     * val person = find(UserController::currentPerson)
     */
    inline fun <reified InjectableType, T> get(prop: KProperty1<InjectableType, T>): T
            where InjectableType : Component, InjectableType : Injectable {
        val injectable = find(InjectableType::class, scope)
        return prop.get(injectable)
    }

    inline fun <reified InjectableType, T> set(prop: KMutableProperty1<InjectableType, T>, value: T)
            where InjectableType : Component, InjectableType : Injectable {
        val injectable = find(InjectableType::class, scope)
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

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : FXEvent> subscribe(noinline action: (T) -> Unit) {
        subscribedEvents.computeIfAbsent(T::class, { ArrayList() }).add(action as (FXEvent) -> Unit)
        val fireNow = if (this is UIComponent) docked else true
        if (fireNow) FX.eventbus.subscribe(T::class, scope, action)
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : FXEvent> unsubscribe(noinline action: (T) -> Unit) {
        subscribedEvents[T::class]?.remove(action)
        FX.eventbus.unsubscribe(T::class, action)
    }

    fun <T : FXEvent> fire(event: T) {
        FX.eventbus.fire(event)
    }

}

abstract class Controller : Component(), Injectable

const val UI_COMPONENT_PROPERTY = "tornadofx.uicomponent"

abstract class UIComponent(viewTitle: String? = "") : Component(), EventTarget {
    override fun buildEventDispatchChain(tail: EventDispatchChain?): EventDispatchChain {
        throw UnsupportedOperationException("not implemented")
    }

    val dockedProperty: ReadOnlyBooleanProperty = SimpleBooleanProperty()
    val docked by dockedProperty
    var fxmlLoader: FXMLLoader? = null
    var modalStage: Stage? = null
    internal var reloadInit = false
    internal var muteDocking = false
    abstract val root: Parent
    var onDockListeners: MutableList<(UIComponent) -> Unit>? = null
    var onUndockListeners: MutableList<(UIComponent) -> Unit>? = null
    var accelerators = HashMap<KeyCombination, Runnable>()

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
        if (!docked) attachLocalEventBusListeners()
        (dockedProperty as SimpleBooleanProperty).value = true
        onDock()
        onDockListeners?.forEach { it.invoke(this) }
    }

    private fun attachLocalEventBusListeners() {
        subscribedEvents.forEach { event, actions ->
            actions.forEach {
                FX.eventbus.subscribe(event, scope, it)
            }
        }
    }

    private fun detachLocalEventBusListeners() {
        subscribedEvents.forEach { event, actions ->
            actions.forEach {
                FX.eventbus.unsubscribe(event, it)
            }
        }
    }

    internal fun callOnUndock() {
        if (muteDocking) return
        detachLocalEventBusListeners()
        (dockedProperty as SimpleBooleanProperty).value = false
        onUndock()
        onUndockListeners?.forEach { it.invoke(this) }
    }


    fun Button.accelerator(combo: String) = accelerator(KeyCombination.valueOf(combo))

    /**
     * Add the key combination as an accelerator for this Button. The accelerator
     * will be applied and reapplied when the UIComponent is docked and undocked.
     */
    fun Button.accelerator(combo: KeyCombination) {
        var oldCombo: Runnable? = null
        var currentScene: Scene? = null
        whenDocked {
            scene?.accelerators?.apply {
                currentScene = scene
                oldCombo = get(combo)
                put(combo, Runnable { fire() })
            }
        }
        whenUndocked {
            currentScene?.accelerators?.apply {
                if (oldCombo != null) put(combo, oldCombo)
                else remove(combo)
            }
        }
    }

    // TODO: Helpers needing access to Scope must be defined here or they must take scope with default as parameter.
    fun <C : UIComponent> BorderPane.top(nodeType: KClass<C>) = setRegion(scope, BorderPane::topProperty, nodeType)

    fun <C : UIComponent> BorderPane.right(nodeType: KClass<C>) = setRegion(scope, BorderPane::rightProperty, nodeType)
    fun <C : UIComponent> BorderPane.bottom(nodeType: KClass<C>) = setRegion(scope, BorderPane::bottomProperty, nodeType)
    fun <C : UIComponent> BorderPane.left(nodeType: KClass<C>) = setRegion(scope, BorderPane::leftProperty, nodeType)
    fun <C : UIComponent> BorderPane.center(nodeType: KClass<C>) = setRegion(scope, BorderPane::centerProperty, nodeType)

    @Suppress("UNCHECKED_CAST")
    fun <T> ListView<T>.cellFormat(formatter: (ListCell<T>.(T) -> Unit)) {
        properties["tornadofx.cellFormat"] = formatter
        if (properties["tornadofx.cellFormatCapable"] != true)
            cellFactory = Callback { SmartListCell(scope, this@cellFormat) }
    }

    fun <T, F : ListCellFragment<T>> ListView<T>.cellFragment(fragment: KClass<F>) {
        properties["tornadofx.cellFragment"] = fragment
        if (properties["tornadofx.cellFormatCapable"] != true)
            cellFactory = Callback { listView -> SmartListCell(scope, listView) }
    }

    fun <T> ListView<T>.onEdit(eventListener: ListCell<T>.(EditEventType, T?) -> Unit) {
        isEditable = true
        properties["tornadofx.editSupport"] = eventListener
        // Install a edit capable cellFactory it none is present. The default cellFormat factory will do.
        if (properties["tornadofx.editCapable"] != true) cellFormat { }
    }

    fun EventTarget.slideshow(scope: Scope = this@UIComponent.scope, op: Slideshow.() -> Unit) = opcr(this, Slideshow(scope), op)

    /**
     * Calculate a unique Node per item and set this Node as the graphic of the ListCell.
     *
     * To support this feature, a custom cellFactory is automatically installed, unless an already
     * compatible cellFactory is found. The cellFactories installed via #cellFormat already knows
     * how to retrieve cached values.
     */
    fun <T> ListView<T>.cellCache(cachedGraphicProvider: (T) -> Node) {
        properties["tornadofx.cellCache"] = ListCellCache(cachedGraphicProvider)
        // Install a cache capable cellFactory it none is present. The default cellFormat factory will do.
        if (properties["tornadofx.cellCacheCapable"] != true) {
            cellFormat { }
        }
    }

    fun <T> ComboBox<T>.cellFormat(formatButtonCell: Boolean = true, formatter: ListCell<T>.(T) -> Unit) {
        cellFactory = Callback {
            it?.properties?.put("tornadofx.cellFormat", formatter)
            SmartListCell(scope, it)
        }
        if (formatButtonCell) {
            buttonCell = cellFactory.call(null)
        }
    }

    @Deprecated("Just an alias for += SomeType::class", ReplaceWith("this += SomeType::class"), DeprecationLevel.WARNING)
    @JvmName("addView")
    inline fun <reified T : View> EventTarget.add(type: KClass<T>, vararg params: Pair<String, Any>): Unit = plusAssign(find(type, scope, *params).root)

    @JvmName("addFragment")
    inline fun <reified T : Fragment> EventTarget.add(type: KClass<T>, vararg params: Pair<String, Any>): Unit = plusAssign(find(type, scope, *params).root)

    @JvmName("plusView")
    operator fun <T : View> EventTarget.plusAssign(type: KClass<T>): Unit = plusAssign(find(type, scope).root)

    @JvmName("plusFragment")
    operator fun <T : Fragment> EventTarget.plusAssign(type: KClass<T>) = plusAssign(find(type, scope).root)

    protected fun openInternalWindow(view: KClass<out UIComponent>, scope: Scope = this@UIComponent.scope, icon: Node? = null, modal: Boolean = true, owner: Node = root, escapeClosesWindow: Boolean = true, closeButton: Boolean = true, vararg params: Pair<String, Any>) =
            InternalWindow(icon, modal, escapeClosesWindow, closeButton).open(find(view, scope, *params), owner)

    protected fun openInternalWindow(view: UIComponent, icon: Node? = null, modal: Boolean = true, owner: Node = root, escapeClosesWindow: Boolean = true, closeButton: Boolean = true) =
            InternalWindow(icon, modal, escapeClosesWindow, closeButton).open(view, owner)

    fun openWindow(stageStyle: StageStyle = StageStyle.DECORATED, modality: Modality = Modality.NONE, escapeClosesWindow: Boolean = true, owner: Window? = null, block: Boolean = false)
            = openModal(stageStyle, modality, escapeClosesWindow, owner, block)

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
                            val primaryStage = FX.getPrimaryStage(scope)
                            if (primaryStage != null) icons += primaryStage.icons
                            scene = this
                            this@UIComponent.properties["tornadofx.scene"] = this
                        }
                    }

                    hookGlobalShortcuts()

                    showingProperty().onChange {
                        if (it == true) {
                            callOnDock()
                            if (FX.reloadStylesheetsOnFocus || FX.reloadViewsOnFocus) {
                                configureReloading()
                            }
                        } else {
                            modalStage = null
                            callOnUndock()
                        }
                    }

                    if (block) showAndWait() else show()
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

    fun closeModal() {
        modalStage?.apply {
            close()
            modalStage = null
        }
        root.findParentOfType(InternalWindow::class)?.close()
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

    inline fun <reified T : Component> find(vararg params: Pair<String, Any>): T = find(T::class, scope, *params)
    fun <T : Component> find(type: KClass<T>, vararg params: Pair<String, Any>) = find(type, scope, *params)

    inline fun <reified T : Any> fxid(propName: String? = null) = object : ReadOnlyProperty<UIComponent, T> {
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
        return replaceWith(find(component, scope), transition)
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
    fun stream(resource: String): InputStream? = component.javaClass.getResourceAsStream(resource)
}
