@file:Suppress("unused")

package tornadofx

import javafx.application.Application
import javafx.application.Platform
import javafx.beans.property.ListProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableSet
import javafx.event.EventTarget
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import tornadofx.FX.Companion.childInterceptors
import tornadofx.FX.Companion.inheritParamHolder
import tornadofx.FX.Companion.inheritScopeHolder
import tornadofx.FX.Companion.stylesheets
import tornadofx.osgi.impl.getBundleId
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

open class Scope() {
    internal var workspaceInstance: Workspace? = null

    constructor(workspace: Workspace, vararg setInScope: ScopedInstance) : this() {
        set(*setInScope)
        workspaceInstance = workspace
    }

    constructor(vararg setInScope: ScopedInstance) : this() {
        set(*setInScope)
    }

    fun workspace(workspace: Workspace) = apply { workspaceInstance = workspace }

    val hasActiveWorkspace: Boolean get() = workspaceInstance != null

    var workspace: Workspace
        get() {
            if (workspaceInstance == null) {
                // Use configured default workspace
                workspaceInstance = find(FX.defaultWorkspace, this)
            }
            return workspaceInstance!!
        }
        set(value) {
            workspaceInstance = value
        }

    fun deregister() {
        FX.primaryStages.remove(this)
        FX.applications.remove(this)
        FX.components.remove(this)
    }

    // Fix the component types to this scope
    operator fun invoke(vararg injectable: KClass<out Component>) {
        injectable.forEach { FX.fixedScopes[it] = this }
    }
}

// Fix this component types to the given scope
fun KClass<out Component>.scope(scope: Scope) = scope.invoke(this)

var DefaultScope = Scope()

class FX {
    enum class IgnoreParentBuilder { No, Once }
    companion object {
        var defaultWorkspace: KClass<out Workspace> = Workspace::class
        internal val fixedScopes = HashMap<KClass<out Component>, Scope>()
        internal val inheritScopeHolder = object : ThreadLocal<Scope>() {
            override fun initialValue() = DefaultScope
        }
        internal val inheritParamHolder = ThreadLocal<Map<String, Any?>>()
        internal var ignoreParentBuilder: IgnoreParentBuilder = IgnoreParentBuilder.No
            get() {
                if (field == IgnoreParentBuilder.Once) {
                    field = IgnoreParentBuilder.No
                    return IgnoreParentBuilder.Once
                }
                return field
            }

        val icon: Node get() = SVGIcon("M104.8,49.9c-0.3-2.5-0.7-4.8-1.2-7.1c-1-4.4-2.6-8.9-4.6-13C95.1,22,88.9,15.1,81.7,10c-3.5-2.5-7.1-4.4-11-5.9 c-4.3-1.6-8.7-2.8-13.1-3.4C55,0.2,52.4,0,49.9,0c-0.3,0-0.8,0-1.2,0c-1.8,3.5-3.5,6.9-4.6,10.5c-3.1,9.2-3.6,18.9-2.6,28.3 c0.5,4.6,1.3,9,2.5,13.6s2.5,9,3.9,13.5c2,6.6,3.8,13.1,4.1,19.9c0.3,4.9-0.8,9.9-2.6,14.8c-1.8-3.4-4.4-6.6-8-7.7 c3.3,2.1,4.8,5.8,5.4,9.2c-0.3,0-0.7,0-1,0c-2.6-0.3-5.1-0.7-7.7-1.3c-3.6-1-7.2-2.3-10.5-4.1c-6.9-3.8-12.6-9.2-17.1-15.6 C5.6,73.8,3,64.9,2.6,56.2c-0.3-9,2-17.9,6.4-25.8c4.3-7.4,10.5-13.6,17.9-17.9c2.8-1.6,5.8-3,8.9-3.9c0.7-0.2,1-0.3,1-0.3 S36.5,8.4,36,8.5c-7.6,2.1-14.5,6.2-20.2,11.5C9.4,26.1,4.4,34,2,42.5c-1.3,4.3-2,8.7-2,13.1c0,2.3,0,4.8,0.3,7.1 c0.2,2,0.7,3.8,1,5.8c0.5,2.3,1.2,4.4,2,6.6s1.8,4.3,3,6.4c2,3.3,4.3,6.6,6.9,9.5c3,3.3,6.4,6.2,10.2,8.7 c3.6,2.5,7.6,4.3,11.7,5.7c5.1,1.8,10.3,2.5,15.6,2.8c2.6,0.2,5.3,0,7.9-0.3c2.5-0.3,4.9-0.8,7.2-1.5c8.7-2.3,16.9-7.2,23.3-13.5 c6.6-6.6,11.3-14.6,13.8-23.5c0.8-2.8,1.3-5.6,1.6-8.5c0.2-1.8,0.3-3.6,0.3-5.4C105,53.7,105,51.8,104.8,49.9z M95.8,55.7 c0,2,0,3.9-0.3,5.9c-0.5,4.6-1.6,9-3.6,13.3c-3.8,8.5-10.2,15.8-18.2,20.7c-3.5,2.1-7.1,3.6-11,4.8c-3,0.8-5.9,1.3-9,1.6 c-0.2,0-0.3,0-0.3,0c-0.2,0-0.5,0-0.7,0c1.1-1.8,2.5-3.4,3.9-5.1c-1.5,0.8-3,1.8-4.4,2.8c2.1-4.3,3.8-9,3.9-14 c0.8-11.8-2.3-23-3.8-33.8c-2.3-14.1-0.7-28.1,5.3-39.3c0.3,0,0.7,0.2,1,0.2C67,14.1,74.9,18.2,81.3,24 c6.6,6.2,11.3,14.5,13.1,23.3C95.3,50.3,95.6,52.9,95.8,55.7L95.8,55.7L95.8,55.7z")

        val eventbus = EventBus()
        val log: Logger = Logger.getLogger("FX")
        val initialized = SimpleBooleanProperty(false)

        internal val primaryStages = HashMap<Scope, Stage>()
        val primaryStage: Stage get() = primaryStages[DefaultScope]!!
        fun getPrimaryStage(scope: Scope = DefaultScope) = primaryStages[scope] ?: primaryStages[DefaultScope]
        fun setPrimaryStage(scope: Scope = DefaultScope, stage: Stage) {
            primaryStages[scope] = stage
        }

        internal val applications = HashMap<Scope, Application>()
        val application: Application get() = applications[DefaultScope]!!
        fun getApplication(scope: Scope = DefaultScope) = applications[scope] ?: applications[DefaultScope]
        fun setApplication(scope: Scope = DefaultScope, application: Application) {
            applications[scope] = application
        }

        val stylesheets: ObservableList<String> = FXCollections.observableArrayList<String>()

        internal val components = HashMap<Scope, HashMap<KClass<out ScopedInstance>, ScopedInstance>>()
        fun getComponents(scope: Scope = DefaultScope) = components.getOrPut(scope) { HashMap() }

        val lock = Any()

        internal val childInterceptors = mutableSetOf<ChildInterceptor>()
        fun addChildInterceptor(interceptor: ChildInterceptor) {

            childInterceptors.add(interceptor)
        }

        fun removeChildInterceptor(interceptor: ChildInterceptor) {
            childInterceptors.remove(interceptor)
        }

        @JvmStatic
        var dicontainer: DIContainer? = null
        var reloadStylesheetsOnFocus = false
        var reloadViewsOnFocus = false
        var dumpStylesheets = false
        var layoutDebuggerShortcut: KeyCodeCombination? = KeyCodeCombination(KeyCode.J, KeyCodeCombination.META_DOWN, KeyCodeCombination.ALT_DOWN)
        var osgiDebuggerShortcut: KeyCodeCombination? = KeyCodeCombination(KeyCode.O, KeyCodeCombination.META_DOWN, KeyCodeCombination.ALT_DOWN)

        val osgiAvailable: Boolean by lazy {
            try {
                Class.forName("org.osgi.framework.FrameworkUtil")
                true
            } catch (ex: Throwable) {
                false
            }
        }

        private val _locale: SimpleObjectProperty<Locale> = object : SimpleObjectProperty<Locale>() {
            override fun invalidated() = loadMessages()
        }
        var locale: Locale get() = _locale.get(); set(value) = _locale.set(value)
        fun localeProperty() = _locale

        private val _messages: SimpleObjectProperty<ResourceBundle> = SimpleObjectProperty()
        var messages: ResourceBundle get() = _messages.get(); set(value) = _messages.set(value)
        fun messagesProperty() = _messages

        /**
         * Load global resource bundle for the current locale. Triggered when the locale changes.
         */
        private fun loadMessages() {
            try {
                messages = ResourceBundle.getBundle("Messages", locale, FXResourceBundleControl)
            } catch (ex: Exception) {
                log.fine("No global Messages found in locale $locale, using empty bundle")
                messages = EmptyResourceBundle
            }
        }

        fun installErrorHandler() {
            if (Thread.getDefaultUncaughtExceptionHandler() == null)
                Thread.setDefaultUncaughtExceptionHandler(DefaultErrorHandler())
        }

        init {
            locale = Locale.getDefault()
            inheritScopeHolder.set(DefaultScope)
        }

        fun runAndWait(action: () -> Unit) {
            // run synchronously on JavaFX thread
            if (Platform.isFxApplicationThread()) {
                action()
                return
            }

            // queue on JavaFX thread and wait for completion
            val doneLatch = CountDownLatch(1)
            Platform.runLater {
                try {
                    action()
                } finally {
                    doneLatch.countDown()
                }
            }

            try {
                doneLatch.await()
            } catch (e: InterruptedException) {
                // ignore exception
            }
        }

        @JvmStatic
        fun registerApplication(application: Application, primaryStage: Stage) {
            registerApplication(DefaultScope, application, primaryStage)
        }

        @JvmStatic
        fun registerApplication(scope: Scope = DefaultScope, application: Application, primaryStage: Stage) {
            FX.installErrorHandler()
            setPrimaryStage(scope, primaryStage)
            setApplication(scope, application)

            // If custom scope is activated for application itself, change DefaultScope to be the supplised scope
            if (applications[DefaultScope] == null) {
                DefaultScope = scope
            }

            if (application.parameters?.unnamed != null) {
                with(application.parameters.unnamed) {
                    if (contains("--dev-mode")) {
                        reloadStylesheetsOnFocus = true
                        dumpStylesheets = true
                        reloadViewsOnFocus = true
                    }
                    if (contains("--live-stylesheets")) reloadStylesheetsOnFocus = true
                    if (contains("--dump-stylesheets")) dumpStylesheets = true
                    if (contains("--live-views")) reloadViewsOnFocus = true
                }
            }

            if (reloadStylesheetsOnFocus) primaryStage.reloadStylesheetsOnFocus()
            if (reloadViewsOnFocus) primaryStage.reloadViewsOnFocus()
        }

        @JvmStatic
        @JvmOverloads
        fun <T : Component> find(componentType: Class<T>, scope: Scope = DefaultScope): T = find(componentType.kotlin, scope)

        inline fun <reified T : Component> find(scope: Scope = DefaultScope): T = find(T::class, scope)

        fun replaceComponent(obsolete: UIComponent) {
            val replacement: UIComponent

            if (obsolete is View) {
                getComponents(obsolete.scope).remove(obsolete.javaClass.kotlin)
            }
            if (obsolete is UIComponent) {
                replacement = find(obsolete.javaClass.kotlin, obsolete.scope)
            } else {
                val noArgsConstructor = obsolete.javaClass.constructors.any { it.parameterCount == 0 }
                if (noArgsConstructor) {
                    replacement = obsolete.javaClass.newInstance()
                } else {
                    log.warning("Unable to reload $obsolete because it's missing a no args constructor")
                    return
                }
            }

            (obsolete.root.parent as? Pane)?.children?.apply {
                val index = indexOf(obsolete.root)
                remove(obsolete.root)
                add(index, replacement.root)
                log.info("Reloaded [Parent] $obsolete")
            } ?: run {
                if (obsolete.properties.containsKey("tornadofx.scene")) {
                    val scene = obsolete.properties["tornadofx.scene"] as Scene
                    replacement.properties["tornadofx.scene"] = scene
                    scene.root = replacement.root
                    log.info("Reloaded [Scene] $obsolete")
                } else {
                    log.warning("Unable to reload $obsolete because it has no parent and no scene attached")
                }
            }
        }

        fun applyStylesheetsTo(scene: Scene) {
            scene.stylesheets.addAll(stylesheets)
            stylesheets.addListener(MyListChangeListener(scene))
        }
    }
}

fun <T> weak(referent: T, deinit: () -> Unit = {}): WeakDelegate<T> = WeakDelegate(referent, deinit)

class WeakDelegate<T>(referent: T, deinit: () -> Unit = {}) : ReadOnlyProperty<Any, DeregisteringWeakReference<T>> {
    private val weakRef = DeregisteringWeakReference(referent, deinit)
    override fun getValue(thisRef: Any, property: KProperty<*>) = weakRef
}

class DeregisteringWeakReference<T>(referent: T, val deinit: () -> Unit = {}) : WeakReference<T>(referent) {
    fun ifActive(op: T.() -> Unit) {
        val ref = get()
        if (ref != null) op(ref) else deinit()
    }
}

private class MyListChangeListener(scene: Scene) : ListChangeListener<String> {
    val sceneRef by weak(scene) { stylesheets.removeListener(this) }

    override fun onChanged(change: ListChangeListener.Change<out String>) {
        sceneRef.ifActive {
            while (change.next()) {
                if (change.wasAdded()) change.addedSubList.forEach { stylesheets.add(it) }
                if (change.wasRemoved()) change.removed.forEach { stylesheets.remove(it) }
            }
        }
    }
}

fun setStageIcon(icon: Image, scope: Scope = DefaultScope) {
    val adder = { FX.getPrimaryStage(scope)?.icons?.apply { clear(); add(icon) } }
    if (FX.initialized.value) adder() else FX.initialized.onChange { adder() }
}

fun addStageIcon(icon: Image, scope: Scope = DefaultScope) {
    val adder = { FX.getPrimaryStage(scope)?.icons?.add(icon) }
    if (FX.initialized.value) adder() else FX.initialized.onChange { adder() }
}

fun reloadStylesheetsOnFocus() {
    FX.reloadStylesheetsOnFocus = true
}

fun dumpStylesheets() {
    FX.dumpStylesheets = true
}

fun reloadViewsOnFocus() {
    FX.reloadViewsOnFocus = true
}

fun importStylesheet(stylesheet: String) {
    val css = FX::class.java.getResource(stylesheet)
    if (css != null)
        stylesheets.add(css.toExternalForm())
    else
        FX.log.log(Level.WARNING, "Unable to find stylesheet at $stylesheet - check that the path is correct")
}

inline fun <reified T : Stylesheet> importStylesheet() = importStylesheet(T::class)
fun <T : Stylesheet> importStylesheet(stylesheetType: KClass<T>) {
    val url = StringBuilder("css://${stylesheetType.java.name}")
    if (FX.osgiAvailable) {
        val bundleId = getBundleId(stylesheetType)
        if (bundleId != null) url.append("?$bundleId")
    }
    val urlString = url.toString()
    if (urlString !in FX.stylesheets) FX.stylesheets.add(url.toString())
}

inline fun <reified T : Stylesheet> removeStylesheet() = removeStylesheet(T::class)
fun <T : Stylesheet> removeStylesheet(stylesheetType: KClass<T>) {
    val url = StringBuilder("css://${stylesheetType.java.name}")
    if (FX.osgiAvailable) {
        val bundleId = getBundleId(stylesheetType)
        if (bundleId != null) url.append("?$bundleId")
    }
    FX.stylesheets.remove(url.toString())
}


fun <T : ScopedInstance> setInScope(value: T, scope: Scope = DefaultScope) = FX.getComponents(scope).put(value.javaClass.kotlin, value)
@Suppress("UNCHECKED_CAST")
fun <T : ScopedInstance> Scope.set(vararg value: T) = apply {
    FX.getComponents(this).apply {
        for (v in value) put(v.javaClass.kotlin, v)
    }
}

fun varargParamsToMap(params: Array<out Pair<String, Any?>>): Map<*, Any?>? {
    val m = HashMap<String, Any?>()
    params.forEach { m.put(it.first, it.second) }
    return m
}

inline fun <reified T : Component> find(scope: Scope = DefaultScope, params: Map<*, Any?>? = null): T = find(T::class, scope, params)

@Suppress("UNCHECKED_CAST")
fun <T : Component> find(type: KClass<T>, scope: Scope = DefaultScope, params: Map<*, Any?>? = null): T {
    val useScope = FX.fixedScopes[type] ?: scope
    inheritScopeHolder.set(useScope)
    val stringKeyedMap = HashMap<String, Any?>()
    params?.forEach {
        val stringKey = (it.key as? KProperty<*>)?.name ?: it.key.toString()
        stringKeyedMap[stringKey] = params[it.key]
    }
    inheritParamHolder.set(stringKeyedMap)

    if (ScopedInstance::class.java.isAssignableFrom(type.java)) {
        var components = FX.getComponents(useScope)
        if (!components.containsKey(type as KClass<out ScopedInstance>)) {
            synchronized(FX.lock) {
                if (!components.containsKey(type)) {
                    val cmp = type.java.newInstance()
                    (cmp as? UIComponent)?.init()
                    // if cmp.scope overrode the scope, inject into that instead
                    if (cmp is Component && cmp.scope != useScope) {
                        components = FX.getComponents(cmp.scope)
                    }
                    components[type] = cmp
                }
            }
        }
        val cmp = components[type] as T
        cmp.paramsProperty.value = stringKeyedMap
        return cmp
    }

    val cmp = type.java.newInstance()
    cmp.paramsProperty.value = stringKeyedMap
    (cmp as? Fragment)?.init()

    // Become default workspace for scope if not set
    if (cmp is Workspace && cmp.scope.workspaceInstance == null)
        cmp.scope.workspaceInstance = cmp

    return cmp
}

interface DIContainer {
    fun <T : Any> getInstance(type: KClass<T>): T
    fun <T : Any> getInstance(type: KClass<T>, name: String): T {
        throw AssertionError("Injector is not configured, so bean of type $type with name $name can not be resolved")
    }
}

inline fun <reified T : Any> DIContainer.getInstance() = getInstance(T::class)
inline fun <reified T : Any> DIContainer.getInstance(name: String) = getInstance(T::class, name)

/**
 * Add the given node to the pane, invoke the node operation and return the node
 */
fun <T : Node> opcr(parent: EventTarget, node: T, op: T.() -> Unit = {}): T {
    parent.addChildIfPossible(node)
    op(node)
    return node
}

@Suppress("UNNECESSARY_SAFE_CALL")
fun EventTarget.addChildIfPossible(node: Node, index: Int? = null) {
    if (FX.childInterceptors.dropWhile { !it(this, node, index) }.isNotEmpty()) return

    if (FX.ignoreParentBuilder != FX.IgnoreParentBuilder.No) return
    if (this is Node) {
        val target = builderTarget
        if (target != null) {
            // Trick to get around the disallowed use of invoke on out projected types
            @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
            target!!(this).value = node
            return
        }
    }
    when (this) {
        is WorkspaceArea -> {
            // Decide if the component should be tracked for removal on undock
            if (dynamicComponentMode) dynamicComponents.add(node)

            if (node is MenuBar) {
                // MenuBar is added above the toolbar and is not considered dynamic
                (top as VBox).children.add(0, node)
            } else {
                val targetIndex: Int
                if (node is ButtonBase) {
                    // Add buttons after last button
                    targetIndex = header.items.indexOfLast { it is Button } + 1
                } else {
                    targetIndex = header.items.indexOfFirst { it.hasClass("spacer") } + 1
                }
                header.items.add(targetIndex, node)
            }
        }
        is Workspace -> {
            root.addChildIfPossible(node, index)
        }
        is Wizard -> {
            val uicmp = node.uiComponent<UIComponent>()
            if (uicmp != null) {
                val muteState = uicmp.muteDocking
                uicmp.muteDocking = true
                pages.add(uicmp)
                uicmp.muteDocking = muteState
                if (pages.size == 1)
                    currentPage = uicmp
            }
        }
        is Drawer -> {
            val uicmp = node.uiComponent<UIComponent>()
            if (uicmp != null) {
                item(uicmp, false)
            } else {
                val title = if (node is Labeled) node.textProperty() else SimpleStringProperty(node.toString())
                val icon = if (node is Labeled) node.graphicProperty() else SimpleObjectProperty()
                item(title, icon) {
                    add(node)
                }
            }
        }
        is UIComponent -> root?.addChildIfPossible(node)
        is ScrollPane -> content = node
        is Tab -> {
            content = node
            // Map the tab to the UIComponent for later retrieval. Used to close tab with UIComponent.close()
            node.uiComponent<UIComponent>()?.properties?.set("tornadofx.tab", this)
        }
        is ButtonBase -> {
            graphic = node
        }
        is BorderPane -> {
        } // Either pos = builder { or caught by builderTarget above
        is TabPane -> {
            val uicmp = node.uiComponent<UIComponent>()
            val tab = if (uicmp != null) {
                Tab().apply {
                    content = node
                    textProperty().bind(uicmp.titleProperty)
                    closableProperty().bind(uicmp.closeable)
                }
            } else {
                Tab(node.toString(), node)
            }
            tabs.add(tab)
        }
        is TitledPane -> {
            if (content is Pane) {
                content.addChildIfPossible(node, index)
            } else if (content is Node) {
                val container = VBox()
                container.children.addAll(content, node)
                content = container
            } else {
                content = node
            }
        }
        is SqueezeBox -> {
            if (node is TitledPane)
                addChild(node)
        }
        is DataGrid<*> -> {
        }
        is Field -> {
            inputContainer.add(node)
        }
        is CustomMenuItem -> {
            content = node
        }
        is MenuItem -> {
            graphic = node
        }
        else -> getChildList()?.apply {
            if (!contains(node)) {
                if (index != null && index < size)
                    add(index, node)
                else
                    add(node)
            }
        }
    }
}

/**
 * Bind the children of this Layout node to the given observable list of items by converting
 * them into nodes via the given converter function. Changes to the source list will be reflected
 * in the children list of this layout node.
 */
fun <T> EventTarget.bindChildren(sourceList: ObservableList<T>, converter: (T) -> Node): ListConversionListener<T, Node>
        = requireNotNull(getChildList()?.bind(sourceList, converter)) { "Unable to extract child nodes from $this" }

/**
 * Bind the children of this Layout node to the items of the given ListPropery by converting
 * them into nodes via the given converter function. Changes to the source list and changing the list inside the ListProperty
 * will be reflected in the children list of this layout node.
 */
fun <T> EventTarget.bindChildren(sourceList: ListProperty<T>, converter: (T) -> Node): ListConversionListener<T, Node>
        = requireNotNull(getChildList()?.bind(sourceList, converter)) { "Unable to extract child nodes from $this" }

/**
 * Bind the children of this Layout node to the given observable set of items by converting
 * them into nodes via the given converter function. Changes to the source set will be reflected
 * in the children list of this layout node.
 */
inline fun <reified T> EventTarget.bindChildren(sourceSet: ObservableSet<T>, noinline converter: (T) -> Node): SetConversionListener<T, Node>
        = requireNotNull(getChildList()?.bind(sourceSet, converter)) { "Unable to extract child nodes from $this" }

/**
 * Bind the children of this Layout node to the given observable list of items by converting
 * them into UIComponents via the given converter function. Changes to the source list will be reflected
 * in the children list of this layout node.
 */
inline fun <reified T> EventTarget.bindComponents(sourceList: ObservableList<T>, noinline converter: (T) -> UIComponent): ListConversionListener<T, Node>
        = requireNotNull(getChildList()?.bind(sourceList) { converter(it).root }) { "Unable to extract child nodes from $this" }


/**
 * Find the list of children from a Parent node. Gleaned code from ControlsFX for this.
 */
fun EventTarget.getChildList(): MutableList<Node>? = when (this) {
    is SplitPane -> items
    is ToolBar -> items
    is Pane -> children
    is Group -> children
    is HBox -> children
    is VBox -> children
    is Control -> (skin as? SkinBase<*>)?.children ?: getChildrenReflectively()
    is Parent -> getChildrenReflectively()
    else -> null
}

@Suppress("UNCHECKED_CAST", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
private fun Parent.getChildrenReflectively(): MutableList<Node>? {
    val getter = this.javaClass.findMethodByName("getChildren")
    if (getter != null && java.util.List::class.java.isAssignableFrom(getter.returnType)) {
        getter.isAccessible = true
        return getter.invoke(this) as MutableList<Node>
    }
    return null
}
