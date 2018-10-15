@file:Suppress("UNCHECKED_CAST")

package tornadofx

import javafx.application.HostServices
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
import javafx.scene.layout.StackPane
import javafx.scene.media.Media
import javafx.scene.paint.Paint
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.stage.Window
import javafx.util.Duration
import java.io.Closeable
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
    val config: ConfigProperties
    val configPath: Path

    fun loadConfig() = ConfigProperties(this).apply {
        if (Files.exists(configPath))
            Files.newInputStream(configPath).use { load(it) }
    }
}

class ConfigProperties(val configurable: Configurable) : Properties(), Closeable {
    fun set(pair: Pair<String, Any>) {
        val value = pair.second.let {
            (it as? JsonModel)?.toJSON()?.toString() ?: it.toString()
        }
        set(pair.first, value)
    }

    fun string(key: String): String? = getProperty(key)
    fun string(key: String, defaultValue: String): String = getProperty(key, defaultValue)
    fun boolean(key: String): Boolean? = getProperty(key)?.toBoolean()
    fun boolean(key: String, defaultValue: Boolean): Boolean = boolean(key) ?: defaultValue
    fun double(key: String): Double? = getProperty(key)?.toDouble()
    fun double(key: String, defaultValue: Double): Double = double(key) ?: defaultValue
    fun int(key: String) = getProperty(key)?.toInt()
    fun int(key: String, defaultValue: Int): Int = int(key) ?: defaultValue
    fun jsonObject(key: String) = getProperty(key)?.let { Json.createReader(StringReader(it)).readObject() }
    fun jsonArray(key: String) = getProperty(key)?.let { Json.createReader(StringReader(it)).readArray() }
    inline fun <reified M : JsonModel> jsonModel(key: String) = jsonObject(key)?.toModel<M>()
    inline fun <reified M : JsonModel> jsonModels(key: String) = jsonArray(key)?.toModel<M>()

    fun save() {
        val path = configurable.configPath.apply { if (!Files.exists(parent)) Files.createDirectories(parent) }
        Files.newOutputStream(path).use { output -> store(output, "") }
    }

    override fun close() {
        save()
    }
}

abstract class Component : Configurable {
    open val scope: Scope = FX.inheritScopeHolder.get()
    val workspace: Workspace get() = scope.workspace
    val paramsProperty = SimpleObjectProperty<Map<String, Any?>>(FX.inheritParamHolder.get() ?: mapOf())
    val params: Map<String, Any?> get() = paramsProperty.value
    val subscribedEvents = HashMap<KClass<out FXEvent>, ArrayList<FXEventRegistration>>()

    /**
     * Path to component specific configuration settings. Defaults to javaClass.properties inside
     * the configured configBasePath of the application (By default conf in the current directory).
     */
    override val configPath: Path get() = app.configBasePath.resolve("${javaClass.name}.properties")
    override val config: ConfigProperties by lazy { loadConfig() }

    val clipboard: Clipboard by lazy { Clipboard.getSystemClipboard() }
    val hostServices: HostServices by lazy { FX.application.hostServices }

    inline fun <reified T : Component> find(vararg params: Pair<*, Any?>, noinline op: T.() -> Unit = {}): T = find(T::class, scope, params.toMap()).apply(op)
    inline fun <reified T : Component> find(params: Map<*, Any?>? = null, noinline op: T.() -> Unit = {}): T = find(T::class, scope, params).apply(op)

    fun <T : Component> find(type: KClass<T>, params: Map<*, Any?>? = null, op: T.() -> Unit = {}) = find(type, scope, params).apply(op)
    fun <T : Component> find(type: KClass<T>, vararg params: Pair<*, Any?>, op: T.() -> Unit = {}) = find(type, scope, params.toMap()).apply(op)

    @JvmOverloads
    fun <T : Component> find(componentType: Class<T>, params: Map<*, Any?>? = null, scope: Scope = this@Component.scope): T = find(componentType.kotlin, scope, params)

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
                    val bundle = ResourceBundle.getBundle(this@Component.javaClass.name, FX.locale, this@Component.javaClass.classLoader, FXResourceBundleControl)
                    (bundle as? FXPropertyResourceBundle)?.inheritFromGlobal()
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

    inline fun <reified T> inject(
            overrideScope: Scope = scope,
            vararg params: Pair<String, Any?>
    ): ReadOnlyProperty<Component, T>
            where T : Component, T : ScopedInstance = inject(overrideScope, params.toMap())

    inline fun <reified T> inject(
            overrideScope: Scope = scope,
            params: Map<String, Any?>? = null
    ): ReadOnlyProperty<Component, T>
            where T : Component,
                  T : ScopedInstance =
            object : ReadOnlyProperty<Component, T> {
                override fun getValue(thisRef: Component, property: KProperty<*>) = find<T>(overrideScope, params)
            }

    inline fun <reified T> param(defaultValue: T? = null): ReadOnlyProperty<Component, T> = object : ReadOnlyProperty<Component, T> {
        override fun getValue(thisRef: Component, property: KProperty<*>): T {
            val param = thisRef.params[property.name] as? T
            if (param == null) {
                if (defaultValue != null) return defaultValue
                @Suppress("ALWAYS_NULL")
                if (property.returnType.isMarkedNullable) return defaultValue as T
                throw IllegalStateException("param for name [$property.name] has not been set")
            } else {
                return param
            }
        }
    }

    fun <T : ScopedInstance> setInScope(value: T, scope: Scope = this.scope) = FX.getComponents(scope).put(value.javaClass.kotlin, value)

    @Deprecated("No need to use the nullableParam anymore, use param instead", ReplaceWith("param(defaultValue)"))
    inline fun <reified T> nullableParam(defaultValue: T? = null) = param(defaultValue)

    inline fun <reified T : Fragment> fragment(overrideScope: Scope = scope, vararg params: Pair<String, Any?>): ReadOnlyProperty<Component, T> = fragment(overrideScope, params.toMap())
    inline fun <reified T : Fragment> fragment(overrideScope: Scope = scope, params: Map<String, Any?>): ReadOnlyProperty<Component, T> = object : ReadOnlyProperty<Component, T> {
        val fragment: T by lazy { find<T>(overrideScope, params) }
        override fun getValue(thisRef: Component, property: KProperty<*>): T = fragment
    }

    inline fun <reified T : Any> di(name: String? = null): ReadOnlyProperty<Component, T> = object : ReadOnlyProperty<Component, T> {
        var injected: T? = null
        override fun getValue(thisRef: Component, property: KProperty<*>): T {
            val dicontainer = FX.dicontainer ?: throw AssertionError(
                    "Injector is not configured, so bean of type ${T::class} cannot be resolved")
            return dicontainer.let {
                if (name != null) {
                    it.getInstance<T>(name)
                } else {
                    it.getInstance()
                }
            }.also { injected = it }
        }
    }

    val primaryStage: Stage get() = FX.getPrimaryStage(scope)!!

    // This is here for backwards compatibility. Removing it would require an import for the tornadofx.ui version
    infix fun <T> Task<T>.ui(func: (T) -> Unit) = success(func)

    @Deprecated("Clashes with Region.background, so runAsync is a better name", ReplaceWith("runAsync"), DeprecationLevel.WARNING)
    fun <T> background(func: FXTask<*>.() -> T) = task(func = func)

    /**
     * Perform the given operation on an ScopedInstance of the specified type asynchronousyly.
     *
     * MyController::class.runAsync { functionOnMyController() } ui { processResultOnUiThread(it) }
     */
    inline fun <reified T, R> KClass<T>.runAsync(noinline op: T.() -> R) where T : Component, T : ScopedInstance = task { op(find(scope)) }

    /**
     * Perform the given operation on an ScopedInstance class function member asynchronousyly.
     *
     * CustomerController::listContacts.runAsync(customerId) { processResultOnUiThread(it) }
     */
    inline fun <reified InjectableType, reified ReturnType> KFunction1<InjectableType, ReturnType>.runAsync(noinline doOnUi: (ReturnType) -> Unit = {}): Task<ReturnType>
            where InjectableType : Component, InjectableType : ScopedInstance = task { invoke(find(scope)) }.apply { ui(doOnUi) }

    /**
     * Perform the given operation on an ScopedInstance class function member asynchronousyly.
     *
     * CustomerController::listCustomers.runAsync { processResultOnUiThread(it) }
     */
    inline fun <reified InjectableType, reified P1, reified ReturnType> KFunction2<InjectableType, P1, ReturnType>.runAsync(p1: P1, noinline doOnUi: (ReturnType) -> Unit = {})
            where InjectableType : Component, InjectableType : ScopedInstance = task { invoke(find(scope), p1) }.apply { ui(doOnUi) }

    inline fun <reified InjectableType, reified P1, reified P2, reified ReturnType> KFunction3<InjectableType, P1, P2, ReturnType>.runAsync(p1: P1, p2: P2, noinline doOnUi: (ReturnType) -> Unit = {})
            where InjectableType : Component, InjectableType : ScopedInstance = task { invoke(find(scope), p1, p2) }.apply { ui(doOnUi) }

    inline fun <reified InjectableType, reified P1, reified P2, reified P3, reified ReturnType> KFunction4<InjectableType, P1, P2, P3, ReturnType>.runAsync(p1: P1, p2: P2, p3: P3, noinline doOnUi: (ReturnType) -> Unit = {})
            where InjectableType : Component, InjectableType : ScopedInstance = task { invoke(find(scope), p1, p2, p3) }.apply { ui(doOnUi) }

    inline fun <reified InjectableType, reified P1, reified P2, reified P3, reified P4, reified ReturnType> KFunction5<InjectableType, P1, P2, P3, P4, ReturnType>.runAsync(p1: P1, p2: P2, p3: P3, p4: P4, noinline doOnUi: (ReturnType) -> Unit = {})
            where InjectableType : Component, InjectableType : ScopedInstance = task { invoke(find(scope), p1, p2, p3, p4) }.apply { ui(doOnUi) }

    /**
     * Find the given property inside the given ScopedInstance. Useful for assigning a property from a View or Controller
     * in any Component. Example:
     *
     * val person = find(UserController::currentPerson)
     */
    inline fun <reified InjectableType, T> get(prop: KProperty1<InjectableType, T>): T
            where InjectableType : Component, InjectableType : ScopedInstance {
        val injectable = find<InjectableType>(scope)
        return prop.get(injectable)
    }

    inline fun <reified InjectableType, T> set(prop: KMutableProperty1<InjectableType, T>, value: T)
            where InjectableType : Component, InjectableType : ScopedInstance {
        val injectable = find<InjectableType>(scope)
        return prop.set(injectable, value)
    }

    /**
     * Runs task in background. If not set directly, looks for `TaskStatus` instance in current scope.
     */
    fun <T> runAsync(status: TaskStatus? = find(scope), func: FXTask<*>.() -> T) = task(status, func)

    fun <T> runAsync(daemon: Boolean = false, status: TaskStatus? = find(scope), func: FXTask<*>.() -> T) = task(daemon, status, func)

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : FXEvent> subscribe(times: Number? = null, noinline action: EventContext.(T) -> Unit): FXEventRegistration {
        val registration = FXEventRegistration(T::class, this, times?.toLong(), action as EventContext.(FXEvent) -> Unit)
        subscribedEvents.getOrPut(T::class) { ArrayList() }.add(registration)
        val fireNow = (this as? UIComponent)?.isDocked ?: true
        if (fireNow) FX.eventbus.subscribe<T>(scope, registration)
        return registration
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : FXEvent> unsubscribe(noinline action: EventContext.(T) -> Unit) {
        subscribedEvents[T::class]?.removeAll { it.action == action }
        FX.eventbus.unsubscribe(action)
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

    lateinit var fxmlLoader: FXMLLoader
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
    open val creatable: BooleanExpression get() = properties.getOrPut("tornadofx.creatable") { SimpleBooleanProperty(Workspace.defaultCreatable) } as BooleanExpression
    open val complete: BooleanExpression get() = properties.getOrPut("tornadofx.complete") { SimpleBooleanProperty(Workspace.defaultComplete) } as BooleanExpression

    var isComplete: Boolean
        get() = complete.value
        set(value) {
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

    fun creatableWhen(creatable: () -> BooleanExpression) {
        properties["tornadofx.creatable"] = creatable()
    }

    fun closeableWhen(closeable: () -> BooleanExpression) {
        properties["tornadofx.closeable"] = closeable()
    }

    fun refreshableWhen(refreshable: () -> BooleanExpression) {
        properties["tornadofx.refreshable"] = refreshable()
    }

    fun whenSaved(onSave: () -> Unit) {
        properties["tornadofx.onSave"] = onSave
    }

    fun whenCreated(onCreate: () -> Unit) {
        properties["tornadofx.onCreate"] = onCreate
    }

    fun whenDeleted(onDelete: () -> Unit) {
        properties["tornadofx.onDelete"] = onDelete
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

        creatableWhen { creatable }
        whenCreated { onCreate() }

        deletableWhen { deletable }
        whenDeleted { onDelete() }

        refreshableWhen { refreshable }
        whenRefreshed { onRefresh() }
    }

    /**
     * Forward the Workspace button states and actions to the TabPane, which
     * in turn will forward these states and actions to whatever View is represented
     * by the currently active Tab.
     */
    fun StackPane.connectWorkspaceActions() {
        savableWhen { savable }
        whenSaved { onSave() }

        creatableWhen { creatable }
        whenCreated { onCreate() }

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

    fun disableCreate() {
        properties["tornadofx.creatable"] = SimpleBooleanProperty(false)
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
            if (newParent != null && newParent != oldParent && !isDocked) {
                callOnDock()
                // Call `onTabSelected` if/when we are connected to a Tab and it's selected
                // Note that this only works for builder constructed tabpanes
                owningTab?.let {
                    it.selectedProperty()?.onChange { if (it) onTabSelected() }
                    if (it.isSelected) onTabSelected()
                }
            }
        })
        root.sceneProperty().addListener({ _, oldParent, newParent ->
            if (modalStage != null || root.parent != null) return@addListener
            if (newParent == null && oldParent != null && isDocked) callOnUndock()
            if (newParent != null && newParent != oldParent && !isDocked) {
                // Call undock when window closes
                newParent.windowProperty().onChangeOnce {
                    it?.showingProperty()?.onChange {
                        if (!it && isDocked) callOnUndock()
                    }
                }
                callOnDock()
            }
        })
        isInitialized = true
    }

    val currentStage: Stage?
        get() {
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

    /**
     * Called when a Component is detached from the Scene
     */
    open fun onUndock() {
    }

    /**
     * Called when a Component becomes the Scene root or
     * when its root node is attached to another Component.
     * @see UIComponent.add
     */
    open fun onDock() {
    }

    /**
     * Called right before the stage for this view is shown. You can access
     * the `currentWindow` property at this stage. This callback is only available
     * to top level UIComponents
     */
    open fun onBeforeShow() {

    }

    /**
     * Called when this Component is hosted by a Tab and the corresponding tab is selected
     */
    open fun onTabSelected() {

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

    /**
     * Create callback which is triggered when the Creaste button in the Workspace
     * is clicked.
     */
    open fun onCreate() {
        (properties["tornadofx.onCreate"] as? () -> Unit)?.invoke()
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

    inline fun <reified T : UIComponent> goto(params: Map<String, Any?>? = null) = find<T>(params).onGoto(this)
    inline fun <reified T : UIComponent> goto(vararg params: Pair<String, Any?>) {
        goto<T>(params.toMap())
    }

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

    inline fun <reified T : UIComponent> TabPane.tab(scope: Scope = this@UIComponent.scope, noinline op: Tab.() -> Unit = {}) = tab(find<T>(scope), op)

    inline fun <reified C : UIComponent> BorderPane.top() = top(C::class)
    fun <C : UIComponent> BorderPane.top(nodeType: KClass<C>) = setRegion(scope, BorderPane::topProperty, nodeType)
    inline fun <reified C : UIComponent> BorderPane.right() = right(C::class)
    fun <C : UIComponent> BorderPane.right(nodeType: KClass<C>) = setRegion(scope, BorderPane::rightProperty, nodeType)
    inline fun <reified C : UIComponent> BorderPane.bottom() = bottom(C::class)
    fun <C : UIComponent> BorderPane.bottom(nodeType: KClass<C>) = setRegion(scope, BorderPane::bottomProperty, nodeType)
    inline fun <reified C : UIComponent> BorderPane.left() = left(C::class)
    fun <C : UIComponent> BorderPane.left(nodeType: KClass<C>) = setRegion(scope, BorderPane::leftProperty, nodeType)
    inline fun <reified C : UIComponent> BorderPane.center() = center(C::class)
    fun <C : UIComponent> BorderPane.center(nodeType: KClass<C>) = setRegion(scope, BorderPane::centerProperty, nodeType)

    fun <S, T> TableColumn<S, T>.cellFormat(formatter: TableCell<S, T>.(T) -> Unit) = cellFormat(scope, formatter)

    fun <S, T, F : TableCellFragment<S, T>> TableColumn<S, T>.cellFragment(fragment: KClass<F>) = cellFragment(scope, fragment)

    fun <T, F : TreeCellFragment<T>> TreeView<T>.cellFragment(fragment: KClass<F>) = cellFragment(scope, fragment)
    /**
     * Calculate a unique Node per item and set this Node as the graphic of the TableCell.
     *
     * To support this feature, a custom cellFactory is automatically installed, unless an already
     * compatible cellFactory is found. The cellFactories installed via #cellFormat already knows
     * how to retrieve cached values.
     */
    fun <S, T> TableColumn<S, T>.cellCache(cachedGraphicProvider: (T) -> Node) = cellCache(scope, cachedGraphicProvider)


    fun EventTarget.slideshow(defaultTimeout: Duration? = null, scope: Scope = this@UIComponent.scope, op: Slideshow.() -> Unit) = opcr(this, Slideshow(scope, defaultTimeout), op)

    fun <T, F : ListCellFragment<T>> ListView<T>.cellFragment(fragment: KClass<F>) = cellFragment(scope, fragment)

    fun <T> ListView<T>.cellFormat(formatter: (ListCell<T>.(T) -> Unit)) = cellFormat(scope, formatter)

    fun <T> ListView<T>.onEdit(eventListener: ListCell<T>.(EditEventType, T?) -> Unit) = onEdit(scope, eventListener)

    fun <T> ListView<T>.cellCache(cachedGraphicProvider: (T) -> Node) = cellCache(scope, cachedGraphicProvider)

    fun <S> TableColumn<S, out Number?>.useProgressBar(afterCommit: (TableColumn.CellEditEvent<S, Number?>) -> Unit = {}) = useProgressBar(scope, afterCommit)

    fun <T> ComboBox<T>.cellFormat(formatButtonCell: Boolean = true, formatter: ListCell<T>.(T) -> Unit) = cellFormat(scope, formatButtonCell, formatter)

    inline fun <reified T : UIComponent> Drawer.item(
            scope: Scope = this@UIComponent.scope,
            vararg params: Pair<*, Any?>,
            expanded: Boolean = false,
            showHeader: Boolean = false,
            noinline op: DrawerItem.() -> Unit = {}
    ) = item(T::class, scope, params.toMap(), expanded, showHeader, op)

    inline fun <reified T : UIComponent> Drawer.item(
            scope: Scope = this@UIComponent.scope,
            params: Map<*, Any?>? = null,
            expanded: Boolean = false,
            showHeader: Boolean = false,
            noinline op: DrawerItem.() -> Unit = {}
    ) = item(T::class, scope, params, expanded, showHeader, op)

    inline fun <reified T : UIComponent> TableView<*>.placeholder(
            scope: Scope = this@UIComponent.scope,
            params: Map<*, Any?>? = null,
            noinline op: T.() -> Unit = {}
    ) {
        placeholder = find(T::class, scope, params).apply(op).root
    }

    inline fun <reified T : UIComponent> TableView<*>.placeholder(
            scope: Scope = this@UIComponent.scope,
            vararg params: Pair<*, Any?>,
            noinline op: T.() -> Unit = {}
    ) {
        placeholder(scope, params.toMap(), op)
    }


    inline fun <reified T : UIComponent> ListView<*>.placeholder(
            scope: Scope = this@UIComponent.scope,
            params: Map<*, Any?>? = null,
            noinline op: T.() -> Unit = {}
    ) {
        placeholder = find(T::class, scope, params).apply(op).root
    }

    inline fun <reified T : UIComponent> ListView<*>.placeholder(
            scope: Scope = this@UIComponent.scope,
            vararg params: Pair<*, Any?>,
            noinline op: T.() -> Unit = {}
    ) {
        placeholder(scope, params.toMap(), op)
    }

    inline fun <reified T : UIComponent> TreeTableView<*>.placeholder(
            scope: Scope = this@UIComponent.scope,
            params: Map<*, Any?>? = null,
            noinline op: T.() -> Unit = {}
    ) {
        placeholder = find(T::class, scope, params).apply(op).root
    }

    inline fun <reified T : UIComponent> TreeTableView<*>.placeholder(
            scope: Scope = this@UIComponent.scope,
            vararg params: Pair<*, Any?>,
            noinline op: T.() -> Unit = {}
    ) {
        placeholder(scope, params.toMap(), op)
    }

    fun Drawer.item(
            uiComponent: KClass<out UIComponent>,
            scope: Scope = this@UIComponent.scope,
            params: Map<*, Any?>? = null,
            expanded: Boolean = false,
            showHeader: Boolean = false,
            op: DrawerItem.() -> Unit = {}
    ) = item(find(uiComponent, scope, params), expanded, showHeader, op)

    fun Drawer.item(
            uiComponent: KClass<out UIComponent>,
            scope: Scope = this@UIComponent.scope,
            vararg params: Pair<*, Any?>,
            expanded: Boolean = false,
            showHeader: Boolean = false,
            op: DrawerItem.() -> Unit = {}
    ) {
        item(uiComponent, scope, params.toMap(), expanded, showHeader, op)
    }

    fun <T : UIComponent> EventTarget.add(type: KClass<T>, params: Map<*, Any?>? = null, op: T.() -> Unit = {}) {
        val view = find(type, scope, params)
        plusAssign(view.root)
        op(view)
    }

    inline fun <reified T : UIComponent> EventTarget.add(vararg params: Pair<*, Any?>, noinline op: T.() -> Unit = {}) = add(T::class, params.toMap(), op)
    fun <T : UIComponent> EventTarget.add(uiComponent: Class<T>) = add(find(uiComponent))

    fun EventTarget.add(uiComponent: UIComponent) = plusAssign(uiComponent.root)
    fun EventTarget.add(child: Node) = plusAssign(child)

    operator fun <T : UIComponent> EventTarget.plusAssign(type: KClass<T>) = plusAssign(find(type, scope).root)

    protected inline fun <reified T : UIComponent> openInternalWindow(
            scope: Scope = this@UIComponent.scope,
            icon: Node? = null,
            modal: Boolean = true,
            owner: Node = root,
            escapeClosesWindow: Boolean = true,
            closeButton: Boolean = true,
            overlayPaint: Paint = c("#000", 0.4),
            params: Map<*, Any?>? = null
    ) = openInternalWindow(T::class, scope, icon, modal, owner, escapeClosesWindow, closeButton, overlayPaint, params)

    protected inline fun <reified T : UIComponent> openInternalWindow(
            scope: Scope = this@UIComponent.scope,
            icon: Node? = null,
            modal: Boolean = true,
            owner: Node = root,
            escapeClosesWindow: Boolean = true,
            closeButton: Boolean = true,
            overlayPaint: Paint = c("#000", 0.4),
            vararg params: Pair<*, Any?>
    ) {
        openInternalWindow<T>(scope, icon, modal, owner, escapeClosesWindow, closeButton, overlayPaint, params.toMap())
    }

    protected fun openInternalWindow(
            view: KClass<out UIComponent>,
            scope: Scope = this@UIComponent.scope,
            icon: Node? = null,
            modal: Boolean = true,
            owner: Node = root,
            escapeClosesWindow: Boolean = true,
            closeButton: Boolean = true,
            overlayPaint: Paint = c("#000", 0.4),
            params: Map<*, Any?>? = null
    ) = InternalWindow(icon, modal, escapeClosesWindow, closeButton, overlayPaint).open(find(view, scope, params), owner)

    protected fun openInternalWindow(
            view: KClass<out UIComponent>,
            scope: Scope = this@UIComponent.scope,
            icon: Node? = null,
            modal: Boolean = true,
            owner: Node = root,
            escapeClosesWindow: Boolean = true,
            closeButton: Boolean = true,
            overlayPaint: Paint = c("#000", 0.4),
            vararg params: Pair<*, Any?>
    ) {
        openInternalWindow(view, scope, icon, modal, owner, escapeClosesWindow, closeButton, overlayPaint, params.toMap())
    }

    protected fun openInternalWindow(
            view: UIComponent,
            icon: Node? = null,
            modal: Boolean = true,
            owner: Node = root,
            escapeClosesWindow: Boolean = true,
            closeButton: Boolean = true,
            overlayPaint: Paint = c("#000", 0.4)
    ) = InternalWindow(icon, modal, escapeClosesWindow, closeButton, overlayPaint).open(view, owner)

    protected fun openInternalBuilderWindow(
            title: String,
            scope: Scope = this@UIComponent.scope,
            icon: Node? = null,
            modal: Boolean = true,
            owner: Node = root,
            escapeClosesWindow: Boolean = true,
            closeButton: Boolean = true,
            overlayPaint: Paint = c("#000", 0.4),
            rootBuilder: UIComponent.() -> Parent
    ) = InternalWindow(icon, modal, escapeClosesWindow, closeButton, overlayPaint).open(BuilderFragment(scope, title, rootBuilder), owner)

    @JvmOverloads
    fun openWindow(
            stageStyle: StageStyle = StageStyle.DECORATED,
            modality: Modality = Modality.NONE,
            escapeClosesWindow: Boolean = true,
            owner: Window? = currentWindow,
            block: Boolean = false,
            resizable: Boolean? = null) = openModal(stageStyle, modality, escapeClosesWindow, owner, block, resizable)

    @JvmOverloads
    fun openModal(stageStyle: StageStyle = StageStyle.DECORATED, modality: Modality = Modality.APPLICATION_MODAL, escapeClosesWindow: Boolean = true, owner: Window? = currentWindow, block: Boolean = false, resizable: Boolean? = null): Stage? {
        if (modalStage == null) {
            modalStage = Stage(stageStyle)
            // modalStage needs to be set before this code to make close() work in blocking mode
            with(modalStage!!) {
                aboutToBeShown = true
                if (resizable != null) isResizable = resizable
                titleProperty().bind(titleProperty)
                initModality(modality)
                if (owner != null) initOwner(owner)

                if (escapeClosesWindow) {
                    addEventFilter(KeyEvent.KEY_PRESSED) {
                        if (it.code == KeyCode.ESCAPE)
                            close()
                    }
                }

                if (getRootWrapper().scene != null) {
                    scene = getRootWrapper().scene
                    this@UIComponent.properties["tornadofx.scene"] = getRootWrapper().scene
                } else {
                    Scene(getRootWrapper()).apply {
                        FX.applyStylesheetsTo(this)
                        scene = this
                        this@UIComponent.properties["tornadofx.scene"] = this
                    }
                }

                val primaryStage = FX.getPrimaryStage(scope)
                if (primaryStage != null) icons += primaryStage.icons

                hookGlobalShortcuts()

                onBeforeShow()

                showingProperty().onChange {
                    if (it) {
                        if (owner != null) {
                            x = owner.x + (owner.width / 2) - (scene.width / 2)
                            y = owner.y + (owner.height / 2) - (scene.height / 2)
                        }
                        callOnDock()
                        if (FX.reloadStylesheetsOnFocus || FX.reloadViewsOnFocus) {
                            configureReloading()
                        }
                        aboutToBeShown = false
                    } else {
                        modalStage = null
                        callOnUndock()
                    }
                }

                if (block) showAndWait() else show()
            }
        } else {
            if (!modalStage!!.isShowing)
                modalStage!!.show()
        }

        return modalStage
    }

    private fun Stage.configureReloading() {
        if (FX.reloadStylesheetsOnFocus) reloadStylesheetsOnFocus()
        if (FX.reloadViewsOnFocus) reloadViewsOnFocus()
    }

    @Deprecated("Use close() instead", replaceWith = ReplaceWith("close()"))
    fun closeModal() = close()

    fun close() {
        val internalWindow = root.findParent<InternalWindow>()
        if (internalWindow != null) {
            internalWindow.close()
            return
        }

        (modalStage ?: currentStage)?.apply {
            close()
            modalStage = null
        }
        owningTab?.apply {
            tabPane?.tabs?.remove(this)
        }
    }

    val owningTab: Tab? get() = properties["tornadofx.tab"] as? Tab

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
        val targetLocation = location ?: componentType.simpleName+".fxml"
        val fxml = requireNotNull(componentType.getResource(targetLocation)) { "FXML not found for $componentType in $targetLocation" }

        fxmlLoader = FXMLLoader(fxml).apply {
            resources = this@UIComponent.messages
            if (root != null) setRoot(root)
            if (hasControllerAttribute) {
                setControllerFactory { this@UIComponent }
            } else {
                setController(this@UIComponent)
            }
        }

        return fxmlLoader.load()
    }

    fun <T : Any> fxid(propName: String? = null) = object : ReadOnlyProperty<UIComponent, T> {
        override fun getValue(thisRef: UIComponent, property: KProperty<*>): T {
            val key = propName ?: property.name
            val value = thisRef.fxmlLoader.namespace[key]
            if (value == null) {
                log.warning("Property $key of $thisRef was not resolved because there is no matching fx:id in ${thisRef.fxmlLoader.location}")
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
    fun builderFragment(
            title: String = "",
            scope: Scope = this@UIComponent.scope,
            rootBuilder: UIComponent.() -> Parent
    ) = BuilderFragment(scope, title, rootBuilder)

    fun builderWindow(
            title: String = "",
            modality: Modality = Modality.APPLICATION_MODAL,
            stageStyle: StageStyle = StageStyle.DECORATED,
            scope: Scope = this@UIComponent.scope,
            owner: Window? = currentWindow,
            rootBuilder: UIComponent.() -> Parent
    ) = builderFragment(title, scope, rootBuilder).apply {
        openWindow(modality = modality, stageStyle = stageStyle, owner = owner)
    }

    fun dialog(
            title: String = "",
            modality: Modality = Modality.APPLICATION_MODAL,
            stageStyle: StageStyle = StageStyle.DECORATED,
            scope: Scope = this@UIComponent.scope,
            owner: Window? = currentWindow,
            labelPosition: Orientation = Orientation.HORIZONTAL,
            builder: StageAwareFieldset.() -> Unit
    ): Stage? {
        val fragment = builderFragment(title, scope, { form() })
        val fieldset = StageAwareFieldset(title, labelPosition)
        fragment.root.add(fieldset)
        fieldset.stage = fragment.openWindow(modality = modality, stageStyle = stageStyle, owner = owner)!!
        builder(fieldset)
        fieldset.stage.sizeToScene()
        return fieldset.stage
    }

    inline fun <reified T : UIComponent> replaceWith(
            transition: ViewTransition? = null,
            sizeToScene: Boolean = false,
            centerOnScreen: Boolean = false
    ) = replaceWith(T::class, transition, sizeToScene, centerOnScreen)

    fun <T : UIComponent> replaceWith(
            component: KClass<T>,
            transition: ViewTransition? = null,
            sizeToScene: Boolean = false,
            centerOnScreen: Boolean = false
    ) = replaceWith(find(component, scope), transition, sizeToScene, centerOnScreen)

    /**
     * Replace this component with another, optionally using a transition animation.
     *
     * @param replacement The component that will replace this one
     * @param transition The [ViewTransition] used to animate the transition
     * @return Whether or not the transition will run
     */
    fun replaceWith(
            replacement: UIComponent,
            transition: ViewTransition? = null,
            sizeToScene: Boolean = false,
            centerOnScreen: Boolean = false
    ) = root.replaceWith(replacement.root, transition, sizeToScene, centerOnScreen) {
        if (root == root.scene?.root) (root.scene.window as? Stage)?.titleProperty()?.cleanBind(replacement.titleProperty)
    }

    private fun undockFromParent(replacement: UIComponent) {
        (replacement.root.parent as? Pane)?.children?.remove(replacement.root)
    }

}

@Suppress("UNCHECKED_CAST")
fun <U : UIComponent> U.whenDocked(listener: (U) -> Unit) {
    if (onDockListeners == null) onDockListeners = mutableListOf()
    onDockListeners!!.add(listener as (UIComponent) -> Unit)
}

@Suppress("UNCHECKED_CAST")
fun <U : UIComponent> U.whenDockedOnce(listener: (U) -> Unit) {
    if (onDockListeners == null) onDockListeners = mutableListOf()
    onDockListeners!!.add {
        onDockListeners!!.remove(listener)
        listener(this)
    }
}

@Suppress("UNCHECKED_CAST")
fun <U : UIComponent> U.whenUndocked(listener: (U) -> Unit) {
    if (onUndockListeners == null) onUndockListeners = mutableListOf()
    onUndockListeners!!.add(listener as (UIComponent) -> Unit)
}

@Suppress("UNCHECKED_CAST")
fun <U : UIComponent> U.whenUndockedOnce(listener: (U) -> Unit) {
    if (onUndockListeners == null) onUndockListeners = mutableListOf()
    onUndockListeners!!.add {
        onUndockListeners!!.remove(listener)
        listener(this)
    }
}

abstract class Fragment @JvmOverloads constructor(title: String? = null, icon: Node? = null) : UIComponent(title, icon)

abstract class View @JvmOverloads constructor(title: String? = null, icon: Node? = null) : UIComponent(title, icon), ScopedInstance

class ResourceLookup(val component: Any) {
    operator fun get(resource: String): String = component.javaClass.getResource(resource).toExternalForm()
    fun url(resource: String): URL = component.javaClass.getResource(resource)
    fun media(resource: String): Media = Media(url(resource).toExternalForm())
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
