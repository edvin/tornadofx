package tornadofx

import javafx.beans.binding.BooleanExpression
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.Node
import javafx.scene.layout.StackPane

/**
 * This property always contains the topmost Node in the children
 * list of the StackPane
 */
val StackPane.contentProperty: ReadOnlyObjectProperty<Node?>
    get() {
        @Suppress("UNCHECKED_CAST")
        return properties.getOrPut("tornadofx.contentProperty") {
            val p: ReadOnlyObjectWrapper<Node?> = ReadOnlyObjectWrapper(children.lastOrNull())
            children.onChange { p.value = children.lastOrNull() }
            p.readOnlyProperty
        } as ReadOnlyObjectProperty<Node?>
    }

val StackPane.content: Node? get() = contentProperty.value

val StackPane.savable: BooleanExpression
    get() {
        val savable = SimpleBooleanProperty(true)

        fun updateState(): Unit = savable.cleanBind(contentUiComponent<UIComponent>()?.savable ?: SimpleBooleanProperty(Workspace.defaultSavable))

        updateState()
        contentProperty.onChange { updateState() }

        return savable
    }

val StackPane.creatable: BooleanExpression
    get() {
        val creatable = SimpleBooleanProperty(true)

        fun updateState(): Unit = creatable.cleanBind(contentUiComponent<UIComponent>()?.creatable ?: SimpleBooleanProperty(Workspace.defaultCreatable))

        updateState()
        contentProperty.onChange { updateState() }

        return creatable
    }

val StackPane.deletable: BooleanExpression
    get() {
        val deletable = SimpleBooleanProperty(true)

        fun updateState(): Unit = deletable.cleanBind(contentUiComponent<UIComponent>()?.deletable ?: SimpleBooleanProperty(Workspace.defaultDeletable))

        updateState()
        contentProperty.onChange { updateState() }

        return deletable
    }

val StackPane.refreshable: BooleanExpression
    get() {
        val refreshable = SimpleBooleanProperty(true)

        fun updateState(): Unit = refreshable.cleanBind(contentUiComponent<UIComponent>()?.refreshable ?: SimpleBooleanProperty(Workspace.defaultRefreshable))

        updateState()
        contentProperty.onChange { updateState() }

        return refreshable
    }


inline fun <reified T : UIComponent> StackPane.contentUiComponent(): T? = content?.uiComponent()

fun StackPane.onSave(): Unit = contentUiComponent<UIComponent>()?.onSave() ?: Unit
fun StackPane.onCreate(): Unit = contentUiComponent<UIComponent>()?.onCreate() ?: Unit
fun StackPane.onDelete(): Unit = contentUiComponent<UIComponent>()?.onDelete() ?: Unit
fun StackPane.onRefresh(): Unit = contentUiComponent<UIComponent>()?.onRefresh() ?: Unit

fun StackPane.onNavigateBack(): Boolean = contentUiComponent<UIComponent>()?.onNavigateBack() ?: true
fun StackPane.onNavigateForward(): Boolean = contentUiComponent<UIComponent>()?.onNavigateForward() ?: true
