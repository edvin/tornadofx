package tornadofx

import javafx.beans.binding.BooleanBinding
import javafx.beans.binding.BooleanExpression
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import kotlin.reflect.KClass

fun EventTarget.tabpane(op: TabPane.() -> Unit = {}) =  TabPane().attachTo(this, op)

fun <T : Node> TabPane.tab(text: String, content: T, op: T.() -> Unit = {}): Tab {
    val tab = Tab(text, content)
    tabs.add(tab)
    op(content)
    return tab
}

@Deprecated("Use the tab builder that extracts the closeable state from UIComponent.closeable instead", ReplaceWith("add(uiComponent)"))
fun TabPane.tab(uiComponent: UIComponent, closable: Boolean = true, op: Tab.() -> Unit = {}): Tab {
    val tab = Tab()
    tab.isClosable = closable
    tab.textProperty().bind(uiComponent.titleProperty)
    tab.content = uiComponent.root
    tabs.add(tab)
    op(tab)
    return tab
}

inline fun <reified  T: UIComponent> TabPane.tab(noinline op: Tab.() -> Unit = {}) = tab(T::class, op)
fun TabPane.tab(uiComponent: KClass<out UIComponent>, op: Tab.() -> Unit = {}) = tab(find(uiComponent), op)

fun TabPane.tab(uiComponent: UIComponent, op: Tab.() -> Unit = {}): Tab {
    add(uiComponent.root)
    val tab = tabs.last()
    tab.graphic = uiComponent.icon
    return tab.also(op)
}

fun <T : Node> Iterable<T>.contains(cmp: UIComponent) = any { it == cmp.root }

fun TabPane.contains(cmp: UIComponent) = tabs.map { it.content }.contains(cmp)

fun Tab.disableWhen(predicate: ObservableValue<Boolean>) = disableProperty().cleanBind(predicate)
fun Tab.enableWhen(predicate: ObservableValue<Boolean>) {
    val binding = if (predicate is BooleanBinding) predicate.not() else predicate.toBinding().not()
    disableProperty().cleanBind(binding)
}
fun Tab.closeableWhen(predicate: ObservableValue<Boolean>) {
    closableProperty().bind(predicate)
}

fun Tab.visibleWhen(predicate: ObservableValue<Boolean>) {
    fun updateState() {
        if (predicate.value.not()) tabPane.tabs.remove(this)
        else if (this !in tabPane.tabs) tabPane.tabs.add(this)
    }
    updateState()
    predicate.onChange { updateState() }
}

fun Tab.close() = removeFromParent()

val TabPane.savable: BooleanExpression
    get() {
        val savable = SimpleBooleanProperty(true)

        fun updateState() {
            savable.cleanBind(contentUiComponent<UIComponent>()?.savable ?: SimpleBooleanProperty(Workspace.defaultSavable))
        }

        val contentChangeListener = ChangeListener<Node?> { _, _, _ -> updateState() }

        updateState()

        selectionModel.selectedItem?.contentProperty()?.addListener(contentChangeListener)
        selectionModel.selectedItemProperty().addListener { _, oldTab, newTab ->
            updateState()
            oldTab?.contentProperty()?.removeListener(contentChangeListener)
            newTab?.contentProperty()?.addListener(contentChangeListener)
        }

        return savable
    }

val TabPane.creatable: BooleanExpression
    get() {
        val creatable = SimpleBooleanProperty(true)

        fun updateState() {
            creatable.cleanBind(contentUiComponent<UIComponent>()?.creatable ?: SimpleBooleanProperty(Workspace.defaultCreatable))
        }

        val contentChangeListener = ChangeListener<Node?> { _, _, _ -> updateState() }

        updateState()

        selectionModel.selectedItem?.contentProperty()?.addListener(contentChangeListener)
        selectionModel.selectedItemProperty().addListener { _, oldTab, newTab ->
            updateState()
            oldTab?.contentProperty()?.removeListener(contentChangeListener)
            newTab?.contentProperty()?.addListener(contentChangeListener)
        }

        return savable
    }

val TabPane.deletable: BooleanExpression
    get() {
        val deletable = SimpleBooleanProperty(true)

        fun updateState() {
            deletable.cleanBind(contentUiComponent<UIComponent>()?.deletable ?: SimpleBooleanProperty(Workspace.defaultDeletable))
        }

        val contentChangeListener = ChangeListener<Node?> { observable, oldValue, newValue -> updateState() }

        updateState()

        selectionModel.selectedItem?.contentProperty()?.addListener(contentChangeListener)
        selectionModel.selectedItemProperty().addListener { observable, oldTab, newTab ->
            updateState()
            oldTab?.contentProperty()?.removeListener(contentChangeListener)
            newTab?.contentProperty()?.addListener(contentChangeListener)
        }

        return deletable
    }


val TabPane.refreshable: BooleanExpression
    get() {
        val refreshable = SimpleBooleanProperty(true)

        fun updateState() {
            refreshable.cleanBind(contentUiComponent<UIComponent>()?.refreshable ?: SimpleBooleanProperty(Workspace.defaultRefreshable))
        }

        val contentChangeListener = ChangeListener<Node?> { _, _, _ -> updateState() }

        updateState()

        selectionModel.selectedItem?.contentProperty()?.addListener(contentChangeListener)
        selectionModel.selectedItemProperty().addListener { observable, oldTab, newTab ->
            updateState()
            oldTab?.contentProperty()?.removeListener(contentChangeListener)
            newTab?.contentProperty()?.addListener(contentChangeListener)
        }

        return refreshable
    }

inline fun <reified T : UIComponent> TabPane.contentUiComponent(): T? = selectionModel.selectedItem?.content?.uiComponent()
fun TabPane.onDelete() = contentUiComponent<UIComponent>()?.onDelete()
fun TabPane.onSave() = contentUiComponent<UIComponent>()?.onSave()
fun TabPane.onCreate() = contentUiComponent<UIComponent>()?.onCreate()
fun TabPane.onRefresh() = contentUiComponent<UIComponent>()?.onRefresh()
fun TabPane.onNavigateBack() = contentUiComponent<UIComponent>()?.onNavigateBack() ?: true
fun TabPane.onNavigateForward() = contentUiComponent<UIComponent>()?.onNavigateForward() ?: true

fun TabPane.tab(text: String? = null, tag: Any? = null, op: Tab.() -> Unit = {}): Tab {
    val tab = Tab(text ?: tag?.toString())
    tab.tag = tag
    tabs.add(tab)
    return tab.also(op)
}

fun Tab.whenSelected(op: () -> Unit) {
    selectedProperty().onChange { if (it) op() }
}

fun Tab.select() = apply { tabPane.selectionModel.select(this) }

@Deprecated("No need to use the content{} wrapper anymore, just use a builder directly inside the Tab", ReplaceWith("no content{} wrapper"), DeprecationLevel.WARNING)
fun Tab.content(op: Pane.() -> Unit): Node {
    val fake = VBox()
    op(fake)
    content = if (fake.children.size == 1) fake.children.first() else fake
    return content
}