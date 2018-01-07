package tornadofx

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.geometry.Side
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.TabPane
import javafx.scene.control.ToolBar
import javafx.scene.input.KeyCombination
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import tornadofx.Workspace.NavigationMode.Stack
import kotlin.reflect.KClass

class HeadingContainer : HBox() {
    init {
        addClass("heading-container")
    }
}

class WorkspaceArea : BorderPane() {
    internal var dynamicComponentMode: Boolean = false
    internal val dynamicComponents = FXCollections.observableArrayList<Node>()
    var header: ToolBar by singleAssign()

    init {
        addClass("workspace")
    }
}

open class Workspace(title: String = "Workspace", navigationMode: NavigationMode = Stack) : View(title) {
    var refreshButton: Button by singleAssign()
    var saveButton: Button by singleAssign()
    var createButton: Button by singleAssign()
    var deleteButton: Button by singleAssign()
    var backButton: Button by singleAssign()
    var forwardButton: Button by singleAssign()

    enum class NavigationMode { Stack, Tabs }

    val navigationModeProperty: ObjectProperty<NavigationMode> = SimpleObjectProperty(navigationMode)
    var navigationMode by navigationModeProperty

    val viewStack = FXCollections.observableArrayList<UIComponent>()

    val maxViewStackDepthProperty = SimpleIntegerProperty(DefaultViewStackDepth)
    var maxViewStackDepth by maxViewStackDepthProperty

    val headingContainer = HeadingContainer()
    val tabContainer = TabPane().addClass("editor-container")
    val stackContainer = StackPane().addClass("editor-container")

    val contentContainerProperty = SimpleObjectProperty<Parent>(stackContainer)
    var contentContainer by contentContainerProperty

    val showHeadingLabelProperty = SimpleBooleanProperty(true)
    var showHeadingLabel by showHeadingLabelProperty

    val dockedComponentProperty: ObjectProperty<UIComponent> = SimpleObjectProperty()
    val dockedComponent: UIComponent? get() = dockedComponentProperty.value

    private val viewPos = integerBinding(viewStack, dockedComponentProperty) { viewStack.indexOf(dockedComponent) }

    val leftDrawer: Drawer
        get() = (root.left as? Drawer) ?: Drawer(Side.LEFT, false, false).also {
            root.left = it
            it.toFront()
        }

    val rightDrawer: Drawer
        get() = (root.right as? Drawer) ?: Drawer(Side.RIGHT, false, false).also {
            root.right = it
            it.toFront()
        }

    val bottomDrawer: Drawer
        get() = (root.bottom as? Drawer) ?: Drawer(Side.BOTTOM, false, false).also {
            root.bottom = it
            it.toFront()
        }

    companion object {
        val activeWorkspaces = FXCollections.observableArrayList<Workspace>()
        val DefaultViewStackDepth = 10

        fun closeAll() {
            activeWorkspaces.forEach(Workspace::close)
        }

        var defaultSavable = true
        var defaultDeletable = true
        var defaultRefreshable = true
        var defaultCloseable = true
        var defaultComplete = true
        var defaultCreatable = true

        init {
            importStylesheet("/tornadofx/workspace.css")
        }

    }

    fun disableNavigation() {
        viewStack.clear()
        maxViewStackDepth = 0
    }

    private fun registerWorkspaceAccelerators() {
        accelerators[KeyCombination.valueOf("Ctrl+S")] = {
            if (!saveButton.isDisable) onSave()
        }
        accelerators[KeyCombination.valueOf("Ctrl+N")] = {
            if (!createButton.isDisable) onSave()
        }
        accelerators[KeyCombination.valueOf("Ctrl+R")] = {
            if (!refreshButton.isDisable) onRefresh()
        }
        accelerators[KeyCombination.valueOf("F5")] = {
            if (!refreshButton.isDisable) onRefresh()
        }
    }

    override fun onDock() {
        activeWorkspaces += this
    }

    override fun onUndock() {
        activeWorkspaces -= this
    }

    override val root = WorkspaceArea().apply {
        top {
            vbox {
                header = toolbar {
                    addClass("header")
                    // Force the container to retain pos center even when it's resized (hack to make ToolBar behave)
                    skinProperty().onChange {
                        (lookup(".container") as? HBox)?.apply {
                            alignment = Pos.CENTER_LEFT
                            alignmentProperty().onChange {
                                if (it != Pos.CENTER_LEFT) alignment = Pos.CENTER_LEFT
                            }
                        }
                    }
                    button {
                        addClass("icon-only")
                        backButton = this
                        graphic = label { addClass("icon", "back") }
                        action {
                            if (dockedComponent?.onNavigateBack() ?: true) {
                                navigateBack()
                            }
                        }
                        disableProperty().bind(booleanBinding(viewPos, viewStack) { value < 1 })
                    }
                    button {
                        addClass("icon-only")
                        forwardButton = this
                        graphic = label { addClass("icon", "forward") }
                        action {
                            if (dockedComponent?.onNavigateForward() ?: true) {
                                navigateForward()
                            }
                        }
                        disableProperty().bind(booleanBinding(viewPos, viewStack) { value == viewStack.size - 1 })
                    }
                    button {
                        addClass("icon-only")
                        refreshButton = this
                        isDisable = true
                        graphic = label {
                            addClass("icon", "refresh")
                        }
                        action {
                            onRefresh()
                        }
                    }
                    button {
                        addClass("icon-only")
                        saveButton = this
                        isDisable = true
                        graphic = label { addClass("icon", "save") }
                        action {
                            onSave()
                        }
                    }
                    button {
                        addClass("icon-only")
                        createButton = this
                        isDisable = true
                        graphic = label { addClass("icon", "create") }
                        action {
                            onCreate()
                        }
                    }
                    button {
                        addClass("icon-only")
                        deleteButton = this
                        isDisable = true
                        graphic = label { addClass("icon", "delete") }
                        action {
                            onDelete()
                        }
                    }
                    add(headingContainer)
                    spacer()
                }
            }
        }
    }

    private fun navigateForward(): Boolean {
        if (!forwardButton.isDisabled) {
            dock(viewStack[viewPos.get() + 1], false)
            return true
        }
        return false
    }

    fun navigateBack(): Boolean {
        if (!backButton.isDisabled) {
            dock(viewStack[viewPos.get() - 1], false)
            return true
        }
        return false
    }

    init {
//        @Suppress("LeakingThis")
//        if (!scope.hasActiveWorkspace) scope.workspaceInstance = this
        navigationModeProperty.addListener { _, ov, nv -> navigationModeChanged(ov, nv) }
        tabContainer.tabs.onChange { change ->
            while (change.next()) {
                if (change.wasRemoved()) {
                    change.removed.forEach {
                        if (it == dockedComponent) {
                            titleProperty.unbind()
                            refreshButton.disableProperty().unbind()
                            saveButton.disableProperty().unbind()
                            createButton.disableProperty().unbind()
                            deleteButton.disableProperty().unbind()
                        }
                    }
                }
                if (change.wasAdded()) {
                    change.addedSubList.forEach {
                        it.content.properties["tornadofx.tab"] = it
                    }
                }
            }
        }
        tabContainer.selectionModel.selectedItemProperty().addListener { observableValue, ov, nv ->
            val newCmp = nv?.content?.uiComponent<UIComponent>()
            val oldCmp = ov?.content?.uiComponent<UIComponent>()
            if (newCmp != null && newCmp != dockedComponent) {
                setAsCurrentlyDocked(newCmp)
            }
            if (oldCmp != newCmp) oldCmp?.callOnUndock()
        }
        dockedComponentProperty.onChange { child ->
            if (child != null) {
                inDynamicComponentMode {
                    if (contentContainer == stackContainer) {
                        tabContainer.tabs.clear()

                        stackContainer.clear()
                        stackContainer += child
                    } else {
                        stackContainer.clear()

                        var tab = tabContainer.tabs.find { it.content == child.root }
                        if (tab == null) {
                            tabContainer += child
                            tab = tabContainer.tabs.last()
                        } else {
                            child.callOnDock()
                        }
                        tabContainer.selectionModel.select(tab)
                    }
                }
            }
        }
        navigationModeChanged(null, navigationMode)
        registerWorkspaceAccelerators()
    }

    private fun navigationModeChanged(oldMode: NavigationMode?, newMode: NavigationMode?) {
        if (oldMode == null || oldMode != newMode) {
            contentContainer = if (navigationMode == Stack) stackContainer else tabContainer
            root.center = contentContainer
            if (contentContainer == stackContainer && tabContainer.tabs.isNotEmpty()) {
                tabContainer.tabs.clear()
            }
            dockedComponent?.also {
                dockedComponentProperty.value = null
                dock(it, true)
            }
        }
        if (newMode == Stack) {
            if (backButton !in root.header.items) {
                root.header.items.add(0, backButton)
                root.header.items.add(1, forwardButton)
            }
        } else {
            root.header.items -= backButton
            root.header.items -= forwardButton
        }
    }

    override fun onSave() {
        dockedComponentProperty.value
                ?.takeIf { it.savable.value }
                ?.onSave()
    }

    override fun onDelete() {
        dockedComponentProperty.value
                ?.takeIf { it.deletable.value }
                ?.onDelete()
    }

    override fun onCreate() {
        dockedComponentProperty.value
                ?.takeIf { it.creatable.value }
                ?.onCreate()
    }

    override fun onRefresh() {
        dockedComponentProperty.value
                ?.takeIf { it.refreshable.value }
                ?.onRefresh()
    }

    inline fun <reified T : UIComponent> dock(scope: Scope = this@Workspace.scope, params: Map<*, Any?>? = null) = dock(find<T>(scope, params))

    fun dock(child: UIComponent, forward: Boolean = true) {
        if (child == dockedComponent) return

        // Remove everything after viewpos if moving forward
        if (forward) while (viewPos.get() < viewStack.size -1) viewStack.removeAt(viewPos.get() + 1)

        val addToStack = contentContainer == stackContainer && maxViewStackDepth > 0 && child !in viewStack

        if (addToStack) viewStack += child

        setAsCurrentlyDocked(child)

        // Ensure max stack size
        while (viewStack.size >= maxViewStackDepth && viewStack.isNotEmpty())
            viewStack.removeAt(0)
    }

    private fun setAsCurrentlyDocked(child: UIComponent) {
        titleProperty.bind(child.titleProperty)
        refreshButton.disableProperty().cleanBind(!child.refreshable)
        saveButton.disableProperty().cleanBind(!child.savable)
        createButton.disableProperty().cleanBind(!child.creatable)
        deleteButton.disableProperty().cleanBind(!child.deletable)

        headingContainer.children.clear()
        headingContainer.label(child.headingProperty) {
            graphicProperty().bind(child.iconProperty)
            removeWhen(!showHeadingLabelProperty)
        }

        clearDynamicComponents()

        dockedComponentProperty.value = child
    }

    private fun clearDynamicComponents() {
        root.dynamicComponents.forEach(Node::removeFromParent)
        root.dynamicComponents.clear()
    }

    fun inDynamicComponentMode(function: () -> Unit) {
        root.dynamicComponentMode = true
        try {
            function()
        } finally {
            root.dynamicComponentMode = false
        }
    }

    /**
     * Create a new scope and associate it with this Workspace and optionally add one
     * or more ScopedInstance instances into the scope. The op block operates on the workspace and is passed the new scope. The following example
     * creates a new scope, injects a Customer Model into it and docks the CustomerEditor
     * into the Workspace:
     *
     * <pre>
     * workspace.withNewScope(CustomerModel(customer)) { newScope ->
     *     dock<CustomerEditor>(newScope)
     * }
     * </pre>
     */
    fun withNewScope(vararg setInScope: ScopedInstance, op: Workspace.(Scope) -> Unit) {
        this.op(Scope().also {
            it.workspaceInstance = this
            it.set(*setInScope)
        })
    }

    /**
     * Create a new scope and associate it with this Workspace and dock the given UIComponent type into
     * the scope, passing the given parameters on to the UIComponent and optionally injecting the given Injectables into the new scope.
     */
    inline fun <reified T : UIComponent> dockInNewScope(params: Map<*, Any?>, vararg setInScope: ScopedInstance) {
        withNewScope(*setInScope) { newScope ->
            dock<T>(newScope, params)
        }
    }

    /**
     * Create a new scope and associate it with this Workspace and dock the given UIComponent type into
     * the scope, optionally injecting the given Injectables into the new scope.
     */
    inline fun <reified T : UIComponent> dockInNewScope(vararg setInScope: ScopedInstance) {
        withNewScope(*setInScope) { newScope ->
            dock<T>(newScope)
        }
    }

    /**
     * Create a new scope and associate it with this Workspace and dock the given UIComponent type into
     * the scope and optionally injecting the given Injectables into the new scope.
     */
    fun <T : UIComponent> dockInNewScope(uiComponent: T, vararg setInScope: ScopedInstance) {
        withNewScope(*setInScope) {
            dock(uiComponent)
        }
    }

    /**
     * Will automatically dock the given [UIComponent] if the [ListMenuItem] is selected.
     */
    inline fun <reified T : UIComponent> ListMenuItem.dockOnSelect() {
        whenSelected { dock<T>() }
    }
}

open class WorkspaceApp(val initiallyDockedView: KClass<out UIComponent>, vararg stylesheet: KClass<out Stylesheet>) : App(Workspace::class, *stylesheet) {
    override fun onBeforeShow(view: UIComponent) {
        workspace.dock(find(initiallyDockedView))
    }
}