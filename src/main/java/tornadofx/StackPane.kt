package tornadofx

import javafx.beans.binding.BooleanExpression
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.scene.Node
import javafx.scene.layout.StackPane

/**
 * This property always contains the topmost Node in the children
 * list of the StackPane
 */
val StackPane.contentProperty: ReadOnlyObjectProperty<Node>
    get() {
        @Suppress("UNCHECKED_CAST")
        return properties.getOrPut("tornadofx.contentProperty") {
            val p = SimpleObjectProperty(children.lastOrNull())
            children.onChange {
                p.value = children.lastOrNull()
            }
            p
        } as ReadOnlyObjectProperty<Node>
    }

val StackPane.content: Node? get() = contentProperty.value

val StackPane.savable: BooleanExpression
    get() {
        val savable = SimpleBooleanProperty(true)

        fun updateState() {
            savable.cleanBind(contentUiComponent<UIComponent>()?.savable
                    ?: SimpleBooleanProperty(Workspace.defaultSavable))
        }

        val contentChangeListener = ChangeListener<Node?> { _, _, _ -> updateState() }

        updateState()

        contentProperty.addListener(contentChangeListener)

        return savable
    }

val StackPane.creatable: BooleanExpression
    get() {
        val creatable = SimpleBooleanProperty(true)

        fun updateState() {
            creatable.cleanBind(contentUiComponent<UIComponent>()?.creatable
                    ?: SimpleBooleanProperty(Workspace.defaultCreatable))
        }

        val contentChangeListener = ChangeListener<Node?> { _, _, _ -> updateState() }

        updateState()

        contentProperty.addListener(contentChangeListener)

        return savable
    }

val StackPane.deletable: BooleanExpression
    get() {
        val deletable = SimpleBooleanProperty(true)

        fun updateState() {
            deletable.cleanBind(contentUiComponent<UIComponent>()?.deletable
                    ?: SimpleBooleanProperty(Workspace.defaultDeletable))
        }

        val contentChangeListener = ChangeListener<Node?> { _, _, _ -> updateState() }

        updateState()

        contentProperty.addListener(contentChangeListener)

        return deletable
    }


val StackPane.refreshable: BooleanExpression
    get() {
        val refreshable = SimpleBooleanProperty(true)

        fun updateState() {
            refreshable.cleanBind(contentUiComponent<UIComponent>()?.refreshable
                    ?: SimpleBooleanProperty(Workspace.defaultRefreshable))
        }

        val contentChangeListener = ChangeListener<Node?> { _, _, _ -> updateState() }

        updateState()

        contentProperty.addListener(contentChangeListener)

        return refreshable
    }

inline fun <reified T : UIComponent> StackPane.contentUiComponent(): T? = content?.uiComponent()
fun StackPane.onDelete() = contentUiComponent<UIComponent>()?.onDelete()
fun StackPane.onSave() = contentUiComponent<UIComponent>()?.onSave()
fun StackPane.onCreate() = contentUiComponent<UIComponent>()?.onCreate()
fun StackPane.onRefresh() = contentUiComponent<UIComponent>()?.onRefresh()
fun StackPane.onNavigateBack() = contentUiComponent<UIComponent>()?.onNavigateBack() ?: true
fun StackPane.onNavigateForward() = contentUiComponent<UIComponent>()?.onNavigateForward() ?: true
