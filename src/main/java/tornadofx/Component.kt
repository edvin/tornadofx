@file:Suppress("UNCHECKED_CAST")

package tornadofx

import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory
import com.sun.javafx.application.HostServicesDelegate
import javafx.application.Platform
import javafx.beans.binding.BooleanExpression
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.concurrent.Task
import javafx.event.EventDispatchChain
import javafx.event.EventHandler
import javafx.event.EventTarget
import javafx.fxml.FXMLLoader
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.Clipboard
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.scene.paint.Paint
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.stage.Window
import javafx.util.Callback
import java.io.InputStream
import java.io.StringReader
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.logging.Logger
import java.util.prefs.Preferences
import javax.json.Json
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.*

@Deprecated("Injectable was a misnomer", ReplaceWith("ScopedInstance"))
interface Injectable : ScopedInstance

interface ScopedInstance

interface Configurable {
    val config: Properties
    val configPath: Path

    fun Properties.set(pair: Pair<String, Any?>) {
        val value = pair.second?.let {
            (it as? JsonModel)?.toJSON()?.toString() ?: it.toString()
        }
        set(pair.first, value)
    }

    fun Properties.string(key: String, defaultValue: String? = null) = config.getProperty(key, defaultValue)
    fun Properties.boolean(key: String) = getProperty(key)?.toBoolean() ?: false
    fun Properties.double(key: String) = getProperty(key)?.toDouble()
    fun Properties.jsonObject(key: String) = getProperty(key)?.let { Json.createReader(StringReader(it)).readObject() }
    fun Properties.jsonArray(key: String) = getProperty(key)?.let { Json.createReader(StringReader(it)).readArray() }

    fun Properties.save() {
        val path = configPath.apply { if (!Files.exists(parent)) Files.createDirectories(parent) }
        Files.newOutputStream(path).use { output -> store(output, "") }
    }

    fun loadConfig() = Properties().apply {
        if (Files.exists(configPath))
            Files.newInputStream(configPath).use { load(it) }
    }
}

abstract class Component : Configurable {
    open val scope: Scope = FX.inheritScopeHolder.get()
    val workspace: Workspace get() = scope.workspace
    val params: Map<String, Any?> = FX.inheritParamHolder.get() ?: mapOf()
    val subscribedEvents = HashMap<KClass<out FXEvent>, ArrayList<FXEventRegistration>>()

    /**
     * Path to component specific configuration settings. Defaults to javaClass.properties inside
     * the configured configBasePath of the application (By default conf in the current directory).
     */
    override val configPath: Path get() = app.configBasePath.resolve("${javaClass.name}.properties")
    override val config: Properties by lazy { loadConfig() }
    inline fun <reified M : JsonModel> Properties.jsonModel(key: String) = jsonObject(key)?.toModel<M>()

    val clipboard: Clipboard by lazy { Clipboard.getSystemClipboard() }
    val hostServices: HostServicesDelegate get() = HostServicesFactory.getInstance(FX.application)

    inline fun <reified T : Component> find(params: Map<*, Any?>? = null, noinline op: (T.() -> Unit)? = null): T = find(T::class, scope, params).apply { op?.invoke(this) }
    fun <T : Component> find(type: KClass<T>, params: Map<*, Any?>? = null, op: (T.() -> Unit)? = null) = find(type, scope, params).apply { op?.invoke(this) }
    @JvmOverloads fun <T : Component> find(componentType: Class<T>, params: Map<*, Any?>? = null, scope: Scope = this@Component.scope): T = find(componentType.kotlin, scope, params)

    fun <T : Any> k(javaClass: Class<T>): KClass<T> = javaClass.kotlin

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

    val app: App get() = FX.application as App

    private val _messages: SimpleObjectProperty<ResourceBundle> = object : SimpleObjectProperty<ResourceBundle>() {
        override fun get(): ResourceBundle? {
            if (super.get() == null) {
                try {
                    val bundle = ResourceBundle.getBundle(this@Component.javaClass.name, FX.locale, FXResourceBundleControl.INSTANCE)
                    if (bundle is FXPropertyResourceBundle)
                        bundle.inheritFromGlobal()
                    set(bundle)
                } catch (ex: Exception) {
                    FX.log.fine("No Messages found for ${javaClass.name} in locale ${FX.locale}, using global bundle")
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

    inline fun <reified T> inject(overrideScope: Scope = scope, params: Map<String, Any?>? = null): ReadOnlyProperty<Component, T> where T : Component, T : ScopedInstance = object : ReadOnlyProperty<Component, T> {
        override fun getValue(thisRef: Component, property: KProperty<*>) = find(T::class, overrideScope, params)
    }

    inline fun <reified T> param(defaultValue: T? = null): ReadOnlyProperty<Component, T> = object : ReadOnlyProperty<Component, T> {
        override fun getValue(thisRef: Component, property: KProperty<*>): T {
            val param = thisRef.params[property.name] as? T
            if (param == null) {
                if (defaultValue == null) {
                    throw IllegalStateException("param for name [$property.name] has not been set")
                }
                return defaultValue
            } else {
                return param
            }
        }
    }

    inline fun <reified T> nullableParam(defaultValue: T? = null): ReadOnlyProperty<Component, T?> = object : ReadOnlyProperty<Component, T?> {
        override fun getValue(thisRef: Component, property: KProperty<*>): T? {
            return thisRef.params[property.name] as? T ?: defaultValue
        }
    }

    inline fun <reified T : Fragment> fragment(overrideScope: Scope = scope, params: Map<String, Any?>): ReadOnlyProperty<Component, T> = object : ReadOnlyProperty<Component, T> {
        var fragment: T? = null

        override fun getValue(thisRef: Component, property: KProperty<*>): T {
            if (fragment == null) fragment = find(T::class, overrideScope, params)
            return fragment!!
        }
    }

    inline fun <reified T : Any> di(name: String? = null): ReadOnlyProperty<Component, T> = object : ReadOnlyProperty<Component, T> {
        var injected: T? = null
        override fun getValue(thisRef: Component, property: KProperty<*>): T {
            if (FX.dicontainer == null) {
                throw AssertionError("Injector is not configured, so bean of type ${T::class} cannot be resolved")
            } else {
                if (injected == null) injected = FX.dicontainer?.let {
                    if (name != null) {
                        it.getInstance(T::class, name)
                    } else {
                        it.getInstance(T::class)
                    }
                }
            }
            return injected!!
        }
    }

    val primaryStage: Stage get() = FX.getPrimaryStage(scope)!!

    @Deprecated("Clashes with Region.background, so runAsync is a better name", ReplaceWith("runAsync"), DeprecationLevel.WARNING)
    fun <T> background(func: FXTask<*>.() -> T) = task(func = func)

    /**
     * Perform the given operation on an ScopedInstance of the specified type asynchronousyly.
     *
     * MyController::class.runAsync { functionOnMyController() } ui { processResultOnUiThread(it) }
     */
    inline fun <reified T, R> KClass<T>.runAsync(noinline op: T.() -> R) where T : Component, T : ScopedInstance = task { op(find(T::class, scope)) }

    /**
     * Perform the given operation on an ScopedInstance class function member asynchronousyly.
     *
     * CustomerController::listContacts.runAsync(customerId) { processResultOnUiThread(it) }
     */
    inline fun <reified InjectableType, reified ReturnType> KFunction1<InjectableType, ReturnType>.runAsync(noinline doOnUi: ((ReturnType) -> Unit)? = null): Task<ReturnType>
            where InjectableType : Component, InjectableType : ScopedInstance {
        val t = task { invoke(find(InjectableType::class, scope)) }
        if (doOnUi != null) t.ui(doOnUi)
        return t
    }

    /**
     * Perform the given operation on an ScopedInstance class function member asynchronousyly.
     *
     * CustomerController::listCustomers.runAsync { processResultOnUiThread(it) }
     */
    inline fun <reified InjectableType, reified P1, reified ReturnType> KFunction2<InjectableType, P1, ReturnType>.runAsync(p1: P1, noinline doOnUi: ((ReturnType) -> Unit)? = null)
            where InjectableType : Component, InjectableType : ScopedInstance
            = task { invoke(find(InjectableType::class, scope), p1) }.apply { if (doOnUi != null) ui(doOnUi) }

    inline fun <reified InjectableType, reified P1, reified P2, reified ReturnType> KFunction3<InjectableType, P1, P2, ReturnType>.runAsync(p1: P1, p2: P2, noinline doOnUi: ((ReturnType) -> Unit)? = null)
            where InjectableType : Component, InjectableType : ScopedInstance
            = task { invoke(find(InjectableType::class, scope), p1, p2) }.apply { if (doOnUi != null) ui(doOnUi) }

    inline fun <reified InjectableType, reified P1, reified P2, reified P3, reified ReturnType> KFunction4<InjectableType, P1, P2, P3, ReturnType>.runAsync(p1: P1, p2: P2, p3: P3, noinline doOnUi: ((ReturnType) -> Unit)? = null)
            where InjectableType : Component, InjectableType : ScopedInstance
            = task { invoke(find(InjectableType::class, scope), p1, p2, p3) }.apply { if (doOnUi != null) ui(doOnUi) }

    inline fun <reified InjectableType, reified P1, reified P2, reified P3, reified P4, reified ReturnType> KFunction5<InjectableType, P1, P2, P3, P4, ReturnType>.runAsync(p1: P1, p2: P2, p3: P3, p4: P4, noinline doOnUi: ((ReturnType) -> Unit)? = null)
            where InjectableType : Component, InjectableType : ScopedInstance
            = task { invoke(find(InjectableType::class, scope), p1, p2, p3, p4) }.apply { if (doOnUi != null) ui(doOnUi) }

    /**
     * Find the given property inside the given ScopedInstance. Useful for assigning a property from a View or Controller
     * in any Component. Example:
     *
     * val person = find(UserController::currentPerson)
     */
    inline fun <reified InjectableType, T> get(prop: KProperty1<InjectableType, T>): T
            where InjectableType : Component, InjectableType : ScopedInstance {
        val injectable = find(InjectableType::class, scope)
        return prop.get(injectable)
    }

    inline fun <reified InjectableType, T> set(prop: KMutableProperty1<InjectableType, T>, value: T)
            where InjectableType : Component, InjectableType : ScopedInstance {
        val injectable = find(InjectableType::class, scope)
        return prop.set(injectable, value)
    }

    fun <T> runAsync(status: TaskStatus? = find<TaskStatus>(scope), func: FXTask<*>.() -> T) = task(status, func)

    /**
     * Replace this node with a progress node while a long running task
     * is running and swap it back when complete.
     *
     * If this node is Labeled, the graphic property will contain the progress bar instead while the task is running.
     *
     * The default progress node is a ProgressIndicator that fills the same
     * client area as the parent. You can swap the progress node for any Node you like.
     */
    fun <T : Any> Node.runAsyncWithProgress(progress: Node = ProgressIndicator(), op: () -> T): Task<T> {
        if (this is Labeled) {
            val oldGraphic = graphic
            graphic = progress
            return task {
                val result = op()
                runLater {
                    this@runAsyncWithProgress.graphic = oldGraphic
                }
                result
            }
        } else {
            if (progress is Region)
                progress.setPrefSize(boundsInParent.width, boundsInParent.height)
            val children = parent.getChildList() ?: throw IllegalArgumentException("This node has no child list, and cannot contain the progress node")
            val index = children.indexOf(this)
            children.add(index, progress)
            removeFromParent()
            return task {
                val result = op()
                runLater {
                    children.add(index, this@runAsyncWithProgress)
                    progress.removeFromParent()
                }
                result
            }
        }
    }

    infix fun <T> Task<T>.ui(func: (T) -> Unit) = success(func)

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : FXEvent> subscribe(times: Number? = null, noinline action: EventContext.(T) -> Unit): FXEventRegistration {
        val registration = FXEventRegistration(T::class, this, times?.toLong(), action as EventContext.(FXEvent) -> Unit)
        subscribedEvents.computeIfAbsent(T::class, { ArrayList() }).add(registration)
        val fireNow = if (this is UIComponent) isDocked else true
        if (fireNow) FX.eventbus.subscribe(T::class, scope, registration)
        return registration
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : FXEvent> unsubscribe(noinline action: EventContext.(T) -> Unit) {
        subscribedEvents[T::class]?.removeAll { it.action == action }
        FX.eventbus.unsubscribe(T::class, action)
    }

    fun <T : FXEvent> fire(event: T) {
        FX.eventbus.fire(event)
    }

}

abstract class Controller : Component(), ScopedInstance

const val UI_COMPONENT_PROPERTY = "tornadofx.uicomponent"

abstract class UIComponent(viewTitle: String? = "", icon: Node? = null) : Component(), EventTarget {
    override fun buildEventDispatchChain(tail: EventDispatchChain?): EventDispatchChain {
        throw UnsupportedOperationException("not implemented")
    }

    val iconProperty: ObjectProperty<Node> = SimpleObjectProperty(icon)
    var icon by iconProperty

    val isDockedProperty: ReadOnlyBooleanProperty = SimpleBooleanProperty()
    val isDocked by isDockedProperty

    var fxmlLoader: FXMLLoader? = null
    var modalStage: Stage? = null
    internal var muteDocking = false
    abstract val root: Parent
    internal val wrapperProperty = SimpleObjectProperty<Parent>()
    internal fun getRootWrapper(): Parent = wrapperProperty.value ?: root

    private var isInitialized = false
    val currentWindow: Window? get() = modalStage ?: root.scene?.window ?: FX.primaryStage

    open val refreshable: BooleanExpression get() = properties.getOrPut("tornadofx.refreshable") { SimpleBooleanProperty(Workspace.defaultRefreshable) } as BooleanExpression
    open val savable: BooleanExpression get() = properties.getOrPut("tornadofx.savable") { SimpleBooleanProperty(Workspace.defaultSavable) } as BooleanExpression
    open val closeable: BooleanExpression get() = properties.getOrPut("tornadofx.closeable") { SimpleBooleanProperty(Workspace.defaultCloseable) } as BooleanExpression
    open val deletable: BooleanExpression get() = properties.getOrPut("tornadofx.deletable") { SimpleBooleanProperty(Workspace.defaultDeletable) } as BooleanExpression
    open val complete: BooleanExpression get() = properties.getOrPut("tornadofx.complete") { SimpleBooleanProperty(Workspace.defaultComplete) } as BooleanExpression
    var isComplete: Boolean get() = complete.value; set(value) {
        (complete as? BooleanProperty)?.value = value
    }

    fun wrapper(op: () -> Parent) {
        FX.ignoreParentBuilder = FX.IgnoreParentBuilder.Once
        wrapperProperty.value = op()
    }

    fun savableWhen(savable: () -> BooleanExpression) {
        properties["tornadofx.savable"] = savable()
    }

    fun completeWhen(complete: () -> BooleanExpression) {
        properties["tornadofx.complete"] = complete()
    }

    fun deletableWhen(deletable: () -> BooleanExpression) {
        properties["tornadofx.deletable"] = deletable()
    }

    fun closeableWhen(closeable: () -> BooleanExpression) {
        properties["tornadofx.closeable"] = closeable()
    }

    fun whenSaved(onSave: () -> Unit) {
        properties["tornadofx.onSave"] = onSave
    }

    fun whenDeleted(onDelete: () -> Unit) {
        properties["tornadofx.onDelete"] = onDelete
    }

    fun refreshableWhen(refreshable: () -> BooleanExpression) {
        properties["tornadofx.refreshable"] = refreshable()
    }

    fun whenRefreshed(onRefresh: () -> Unit) {
        properties["tornadofx.onRefresh"] = onRefresh
    }

    /**
     * Forward the Workspace button states and actions to the TabPane, which
     * in turn will forward these states and actions to whatever View is represented
     * by the currently active Tab.
     */
    fun TabPane.connectWorkspaceActions() {
        savableWhen { savable }
        whenSaved { onSave() }

        deletableWhen { deletable }
        whenDeleted { onDelete() }

        refreshableWhen { refreshable }
        whenRefreshed { onRefresh() }
    }

    /**
     * Forward the Workspace button states and actions to the given UIComponent.
     * This will override the currently active forwarding to the docked UIComponent.
     *
     * When another UIComponent is docked, that UIComponent will be the new receiver for the
     * Workspace states and actions, hence voiding this call.
     */
    fun forwardWorkspaceActions(uiComponent: UIComponent) {
        savableWhen { uiComponent.savable }
        whenSaved { uiComponent.onSave() }

        deletableWhen { uiComponent.deletable }
        whenDeleted { uiComponent.onDelete() }

        refreshableWhen { uiComponent.refreshable }
        whenRefreshed { uiComponent.onRefresh() }
    }

    /**
     * Callback that runs before the Workspace navigates back in the View stack. Return false to veto the navigation.
     */
    open fun onNavigateBack() = true

    /**
     * Callback that runs before the Workspace navigates forward in the View stack. Return false to veto the navigation.
     */
    open fun onNavigateForward() = true

    var onDockListeners: MutableList<(UIComponent) -> Unit>? = null
    var onUndockListeners: MutableList<(UIComponent) -> Unit>? = null
    val accelerators = HashMap<KeyCombination, () -> Unit>()

    fun disableSave() {
        properties["tornadofx.savable"] = SimpleBooleanProperty(false)
    }

    fun disableRefresh() {
        properties["tornadofx.refreshable"] = SimpleBooleanProperty(false)
    }

    fun disableDelete() {
        properties["tornadofx.deletable"] = SimpleBooleanProperty(false)
    }

    fun disableClose() {
        properties["tornadofx.closeable"] = SimpleBooleanProperty(false)
    }

    fun init() {
        if (isInitialized) return
        root.properties[UI_COMPONENT_PROPERTY] = this
        root.parentProperty().addListener({ _, oldParent, newParent ->
            if (modalStage != null) return@addListener
            if (newParent == null && oldParent != null && isDocked) callOnUndock()
            if (newParent != null && newParent != oldParent && !isDocked) callOnDock()
        })
        root.sceneProperty().addListener({ _, oldParent, newParent ->
            if (modalStage != null || root.parent != null) return@addListener
            if (newParent == null && oldParent != null && isDocked) callOnUndock()
            if (newParent != null && newParent != oldParent && !isDocked) callOnDock()
        })
        isInitialized = true
    }

    val currentStage: Stage? get() {
        val stage = (currentWindow as? Stage)
        if (stage == null) FX.log.warning { "CurrentStage not available for $this" }
        return stage
    }

    fun setWindowMinSize(width: Number, height: Number) = currentStage?.apply {
        minWidth = width.toDouble()
        minHeight = height.toDouble()
    }

    fun setWindowMaxSize(width: Number, height: Number) = currentStage?.apply {
        maxWidth = width.toDouble()
        maxHeight = height.toDouble()
    }

    private val acceleratorListener: EventHandler<KeyEvent> by lazy {
        EventHandler<KeyEvent> { event ->
            accelerators.keys.asSequence().find { it.match(event) }?.apply {
                accelerators[this]?.invoke()
                event.consume()
            }
        }
    }

    /**
     * Add a key listener to the current scene and look for matches against the
     * `accelerators` map in this UIComponent.
     */
    private fun enableAccelerators() {
        root.scene?.addEventFilter(KEY_PRESSED, acceleratorListener)
        root.sceneProperty().addListener { obs, old, new ->
            old?.removeEventFilter(KEY_PRESSED, acceleratorListener)
            new?.addEventFilter(KEY_PRESSED, acceleratorListener)
        }
    }

    private fun disableAccelerators() {
        root.scene?.removeEventFilter(KEY_PRESSED, acceleratorListener)
    }

    open fun onUndock() {
    }

    open fun onDock() {
    }

    open fun onRefresh() {
        (properties["tornadofx.onRefresh"] as? () -> Unit)?.invoke()
    }

    /**
     * Save callback which is triggered when the Save button in the Workspace
     * is clicked, or when the Next button in a Wizard is clicked.
     *
     * For Wizard pages, you should set the complete state of the Page after save
     * to signal whether the Wizard can move to the next page or finish.
     *
     * For Wizards, you should set the complete state of the Wizard
     * after save to signal whether the Wizard can be closed.
     */
    open fun onSave() {
        (properties["tornadofx.onSave"] as? () -> Unit)?.invoke()
    }

    open fun onDelete() {
        (properties["tornadofx.onDelete"] as? () -> Unit)?.invoke()
    }

    open fun onGoto(source: UIComponent) {
        source.replaceWith(this)
    }

    fun goto(target: UIComponent) {
        target.onGoto(this)
    }

    inline fun <reified T : UIComponent> goto(params: Map<String, Any?>) = find<T>(params).onGoto(this)

    internal fun callOnDock() {
        if (!isInitialized) init()
        if (muteDocking) return
        if (!isDocked) attachLocalEventBusListeners()
        (isDockedProperty as SimpleBooleanProperty).value = true
        enableAccelerators()
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
                FX.eventbus.unsubscribe(event, it.action)
            }
        }
    }

    internal fun callOnUndock() {
        if (muteDocking) return
        detachLocalEventBusListeners()
        (isDockedProperty as SimpleBooleanProperty).value = false
        disableAccelerators()
        onUndock()
        onUndockListeners?.forEach { it.invoke(this) }
    }


    fun Button.shortcut(combo: String) = shortcut(KeyCombination.valueOf(combo))

    @Deprecated("Use shortcut instead", ReplaceWith("shortcut(combo)"))
    fun Button.accelerator(combo: KeyCombination) = shortcut(combo)

    /**
     * Add the key combination as a shortcut for this Button's action.
     */
    fun Button.shortcut(combo: KeyCombination) {
        accelerators[combo] = { fire() }
    }

    /**
     * Configure an action for a key combination.
     */
    fun shortcut(combo: KeyCombination, action: () -> Unit) {
        accelerators[combo] = action
    }

    fun <T> shortcut(combo: KeyCombination, command: Command<T>, param: T? = null) {
        accelerators[combo] = { command.execute(param) }
    }

    /**
     * Configure an action for a key combination.
     */
    fun shortcut(combo: String, action: () -> Unit) = shortcut(KeyCombination.valueOf(combo), action)

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
            Platform.runLater {
                buttonCell = cellFactory.call(null)
            }
        }
    }

    fun Drawer.item(uiComponent: KClass<out UIComponent>, scope: Scope = this@UIComponent.scope, params: Map<*, Any?>? = null, expanded: Boolean = false, showHeader: Boolean = false, op: (DrawerItem.() -> Unit)? = null) =
            item(find(uiComponent, scope, params), expanded, showHeader, op)

    @JvmName("addView")
    inline fun <reified T : View> EventTarget.add(type: KClass<T>, params: Map<*, Any?>? = null): Unit = plusAssign(find(type, scope, params).root)

    @JvmName("addFragmentByClass")
    inline fun <reified T : Fragment> EventTarget.add(type: KClass<T>, params: Map<*, Any?>? = null, noinline op: (T.() -> Unit)? = null): Unit {
        val fragment: T = find(type, scope, params)
        plusAssign(fragment.root)
        op?.invoke(fragment)
    }

    fun <T : UIComponent> EventTarget.add(uiComponent: Class<T>) = add(find(uiComponent))
    fun EventTarget.add(uiComponent: UIComponent) = plusAssign(uiComponent.root)
    fun EventTarget.add(child: Node) = plusAssign(child)

    @JvmName("plusView")
    operator fun <T : View> EventTarget.plusAssign(type: KClass<T>): Unit = plusAssign(find(type, scope).root)

    @JvmName("plusFragment")
    operator fun <T : Fragment> EventTarget.plusAssign(type: KClass<T>) = plusAssign(find(type, scope).root)

    protected fun openInternalWindow(view: KClass<out UIComponent>, scope: Scope = this@UIComponent.scope, icon: Node? = null, modal: Boolean = true, owner: Node = root, escapeClosesWindow: Boolean = true, closeButton: Boolean = true, overlayPaint: Paint = c("#000", 0.4), params: Map<*, Any?>? = null) =
            InternalWindow(icon, modal, escapeClosesWindow, closeButton, overlayPaint).open(find(view, scope, params), owner)

    protected fun openInternalWindow(view: UIComponent, icon: Node? = null, modal: Boolean = true, owner: Node = root, escapeClosesWindow: Boolean = true, closeButton: Boolean = true, overlayPaint: Paint = c("#000", 0.4)) =
            InternalWindow(icon, modal, escapeClosesWindow, closeButton, overlayPaint).open(view, owner)

    protected fun openInternalBuilderWindow(title: String, scope: Scope = this@UIComponent.scope, icon: Node? = null, modal: Boolean = true, owner: Node = root, escapeClosesWindow: Boolean = true, closeButton: Boolean = true, overlayPaint: Paint = c("#000", 0.4), rootBuilder: UIComponent.() -> Parent) =
            InternalWindow(icon, modal, escapeClosesWindow, closeButton, overlayPaint).open(BuilderFragment(scope, title, rootBuilder), owner)

    @JvmOverloads fun openWindow(stageStyle: StageStyle = StageStyle.DECORATED, modality: Modality = Modality.NONE, escapeClosesWindow: Boolean = true, owner: Window? = currentWindow, block: Boolean = false, resizable: Boolean? = null)
            = openModal(stageStyle, modality, escapeClosesWindow, owner, block, resizable)

    @JvmOverloads fun openModal(stageStyle: StageStyle = StageStyle.DECORATED, modality: Modality = Modality.APPLICATION_MODAL, escapeClosesWindow: Boolean = true, owner: Window? = currentWindow, block: Boolean = false, resizable: Boolean? = null) {
        if (modalStage == null) {
            if (getRootWrapper() !is Parent) {
                throw IllegalArgumentException("Only Parent Fragments can be opened in a Modal")
            } else {
                modalStage = Stage(stageStyle)
                // modalStage needs to be set before this code to make close() work in blocking mode
                with(modalStage!!) {
                    if (resizable != null) isResizable = resizable
                    titleProperty().bind(titleProperty)
                    initModality(modality)
                    if (owner != null) initOwner(owner)

                    if (getRootWrapper().scene != null) {
                        scene = getRootWrapper().scene
                        this@UIComponent.properties["tornadofx.scene"] = getRootWrapper().scene
                    } else {
                        Scene(getRootWrapper()).apply {
                            if (escapeClosesWindow) {
                                addEventFilter(KeyEvent.KEY_PRESSED) {
                                    if (it.code == KeyCode.ESCAPE)
                                        close()
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
                        if (it) {
                            callOnDock()
                            if (owner != null) {
                                x = owner.x + (owner.width / 2) - (scene.width / 2)
                                y = owner.y + (owner.height / 2) - (scene.height / 2)
                            }
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

    @Deprecated("Use close() instead", replaceWith = ReplaceWith("close()"))
    fun closeModal() = close()

    fun close() {
        modalStage?.apply {
            close()
            modalStage = null
        }
        root.findParentOfType(InternalWindow::class)?.close()
        (root.properties["tornadofx.tab"] as? Tab)?.apply {
            tabPane?.tabs?.remove(this)
        }
    }

    open val titleProperty: StringProperty = SimpleStringProperty(viewTitle)
    var title: String
        get() = titleProperty.get() ?: ""
        set(value) = titleProperty.set(value)

    open val headingProperty: StringProperty = SimpleStringProperty().apply {
        bind(titleProperty)
    }

    var heading: String
        get() = headingProperty.get() ?: ""
        set(value) {
            if (headingProperty.isBound) headingProperty.unbind()
            headingProperty.set(value)
        }

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
    fun <T : Node> fxml(location: String? = null, hasControllerAttribute: Boolean = false, root: Any? = null): ReadOnlyProperty<UIComponent, T> = object : ReadOnlyProperty<UIComponent, T> {
        val value: T = loadFXML(location, hasControllerAttribute, root)
        override fun getValue(thisRef: UIComponent, property: KProperty<*>) = value
    }

    @JvmOverloads
    fun <T : Node> loadFXML(location: String? = null, hasControllerAttribute: Boolean = false, root: Any? = null): T {
        val componentType = this@UIComponent.javaClass
        val targetLocation = location ?: componentType.simpleName + ".fxml"
        val fxml = componentType.getResource(targetLocation) ?:
                throw IllegalArgumentException("FXML not found for $componentType")

        fxmlLoader = FXMLLoader(fxml).apply {
            resources = this@UIComponent.messages
            if (root != null) setRoot(root)
            if (hasControllerAttribute) {
                setControllerFactory { this@UIComponent }
            } else {
                setController(this@UIComponent)
            }
        }

        return fxmlLoader!!.load()
    }

    fun <T : Any> fxid(propName: String? = null) = object : ReadOnlyProperty<UIComponent, T> {
        override fun getValue(thisRef: UIComponent, property: KProperty<*>): T {
            val key = propName ?: property.name
            val value = thisRef.fxmlLoader!!.namespace[key]
            if (value == null) {
                log.warning("Property $key of $thisRef was not resolved because there is no matching fx:id in ${thisRef.fxmlLoader!!.location}")
            } else {
                return value as T
            }

            throw IllegalArgumentException("Property $key does not match fx:id declaration")
        }
    }

    inline fun <reified T : Parent> EventTarget.include(scope: Scope = this@UIComponent.scope, hasControllerAttribute: Boolean = false, location: String): T {
        val loader = object : Fragment() {
            override val scope = scope
            override val root: T by fxml(location, hasControllerAttribute)
        }
        addChildIfPossible(loader.root)
        return loader.root
    }

    /**
     * Create an fragment by supplying an inline builder expression and optionally open it if the openModality is specified. A fragment can also be assigned
     * to an existing node hierarchy using `add()` or `this += inlineFragment {}`, or you can specify the behavior inside it using `Platform.runLater {}` before
     * you return the root node for the builder fragment.
     */
    fun builderFragment(title: String = "", scope: Scope = this@UIComponent.scope, rootBuilder: UIComponent.() -> Parent) = BuilderFragment(scope, title, rootBuilder)

    fun builderWindow(title: String = "", modality: Modality = Modality.APPLICATION_MODAL, stageStyle: StageStyle = StageStyle.DECORATED, scope: Scope = this@UIComponent.scope, owner: Window? = currentWindow, rootBuilder: UIComponent.() -> Parent) = builderFragment(title, scope, rootBuilder).apply {
        openWindow(modality = modality, stageStyle = stageStyle, owner = owner)
    }

    fun dialog(title: String = "", modality: Modality = Modality.APPLICATION_MODAL, stageStyle: StageStyle = StageStyle.DECORATED, scope: Scope = this@UIComponent.scope, owner: Window? = currentWindow, labelPosition: Orientation = Orientation.HORIZONTAL, builder: Fieldset.() -> Unit) = builderFragment(title, scope, { form { fieldset(title, labelPosition = labelPosition) } }).apply {
        builder((root as Form).fieldsets.first())
        openWindow(modality = modality, stageStyle = stageStyle, owner = owner)
    }

    fun <T : UIComponent> replaceWith(component: KClass<T>, transition: ViewTransition? = null, sizeToScene: Boolean = false, centerOnScreen: Boolean = false): Boolean {
        return replaceWith(find(component, scope), transition, sizeToScene, centerOnScreen)
    }

    /**
     * Replace this component with another, optionally using a transition animation.
     *
     * @param replacement The component that will replace this one
     * @param transition The [ViewTransition] used to animate the transition
     * @return Whether or not the transition will run
     */
    fun replaceWith(replacement: UIComponent, transition: ViewTransition? = null, sizeToScene: Boolean = false, centerOnScreen: Boolean = false): Boolean {
        return root.replaceWith(replacement.root, transition, sizeToScene, centerOnScreen) {
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

abstract class Fragment @JvmOverloads constructor(title: String? = null, icon: Node? = null) : UIComponent(title, icon)

abstract class View @JvmOverloads constructor(title: String? = null, icon: Node? = null) : UIComponent(title, icon), ScopedInstance

class ResourceLookup(val component: Any) {
    operator fun get(resource: String): String = component.javaClass.getResource(resource).toExternalForm()
    fun url(resource: String): URL = component.javaClass.getResource(resource)
    fun stream(resource: String): InputStream = component.javaClass.getResourceAsStream(resource)
    fun image(resource: String): Image = Image(stream(resource))
    fun imageview(resource: String, lazyload: Boolean = false): ImageView = ImageView(Image(url(resource).toExternalForm(), lazyload))
    fun json(resource: String) = stream(resource).toJSON()
    fun jsonArray(resource: String) = stream(resource).toJSONArray()
    fun text(resource: String): String = stream(resource).use { it.bufferedReader().readText() }
}

class BuilderFragment(overrideScope: Scope, title: String, rootBuilder: Fragment.() -> Parent) : Fragment(title) {
    override val scope = overrideScope
    override val root = rootBuilder(this)
}