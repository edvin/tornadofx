package tornadofx

import javafx.beans.property.*
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.event.EventTarget
import javafx.geometry.HorizontalDirection
import javafx.geometry.HorizontalDirection.LEFT
import javafx.geometry.HorizontalDirection.RIGHT
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.ToggleButton
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color

fun EventTarget.drawer(side: HorizontalDirection = LEFT, multiselect: Boolean = false, floatingContent: Boolean = false, op: Drawer.() -> Unit) = opcr(this, Drawer(side, multiselect, floatingContent), op)

class Drawer(side: HorizontalDirection, multiselect: Boolean, floatingContent: Boolean) : BorderPane() {
    val dockingSideProperty: ObjectProperty<HorizontalDirection> = SimpleObjectProperty(side)
    var dockingSide by dockingSideProperty

    val floatingDrawersProperty: BooleanProperty = SimpleBooleanProperty(floatingContent)
    var floatingDrawers by floatingDrawersProperty

    val buttonArea = VBox().addClass(DrawerStyles.buttonArea)
    val contentArea = ExpandedDrawerContentArea()

    val items = FXCollections.observableArrayList<DrawerItem>()

    val multiselectProperty: BooleanProperty = SimpleBooleanProperty(multiselect)
    var multiselect by multiselectProperty

    val contextMenu = ContextMenu()

    override fun getUserAgentStylesheet() = DrawerStyles().base64URL.toExternalForm()

    fun item(title: String? = null, icon: Node? = null, expanded: Boolean = false, op: DrawerItem.() -> Unit) =
            item(SimpleStringProperty(title), SimpleObjectProperty(icon), expanded, op)

    fun item(uiComponent: UIComponent, expanded: Boolean = false, op: (DrawerItem.() -> Unit)? = null): DrawerItem {
        val item = DrawerItem(this, uiComponent.titleProperty, uiComponent.iconProperty)
        item.button.textProperty().bind(uiComponent.headingProperty)
        item.children.add(uiComponent.root)
        op?.invoke(item)
        items.add(item)
        if (expanded) item.button.isSelected = true
        (parent?.uiComponent<UIComponent>() as? Workspace)?.apply {
            if (root.dynamicComponentMode) {
                root.dynamicComponents.add(item)
            }
        }
        return item
    }

    fun item(title: ObservableValue<String?>, icon: ObservableValue<Node?>? = null, expanded: Boolean = false, op: DrawerItem.() -> Unit): DrawerItem {
        val item = DrawerItem(this, title, icon)
        item.button.textProperty().bind(title)
        op(item)
        items.add(item)
        if (expanded) item.button.isSelected = true
        (parent?.uiComponent<UIComponent>() as? Workspace)?.apply {
            if (root.dynamicComponentMode) {
                root.dynamicComponents.add(item)
            }
        }
        return item
    }

    init {
        addClass(DrawerStyles.drawer)

        configureDockingSide()
        configureContextMenu()
        enforceMultiSelect()

        // Redraw if floating mode is toggled
        floatingDrawersProperty.onChange {
            configureContentArea()
            parent?.requestLayout()
            scene?.root?.requestLayout()
        }

        // Adapt docking behavior to parent
        parentProperty().onChange {
            if (it is BorderPane) {
                if (it.left == this) dockingSide = LEFT
                else if (it.right == this) dockingSide = RIGHT
            }
        }

        // Track side property change
        dockingSideProperty.onChange { configureDockingSide() }

        // Track button additions/removal
        items.onChange { change ->
            while (change.next()) {
                if (change.wasAdded()) {
                    change.addedSubList.forEach {
                        configureRotation(it.button)
                        buttonArea.add(Group(it.button))
                    }
                }
                if (change.wasRemoved()) {
                    change.removed.forEach {
                        val group = it.button.parent
                        it.button.removeFromParent()
                        group.removeFromParent()
                    }
                }
            }
        }
    }

    private fun enforceMultiSelect() {
        multiselectProperty.onChange {
            if (!multiselect) {
                contentArea.children.toTypedArray().drop(1).forEach {
                    (it as DrawerItem).button.isSelected = false
                }
            }
        }
    }

    private fun configureContextMenu() {
        contextMenu.checkmenuitem("Floating drawers") {
            selectedProperty().bindBidirectional(floatingDrawersProperty)
        }
        contextMenu.checkmenuitem("Multiselect") {
            selectedProperty().bindBidirectional(multiselectProperty)
        }
        buttonArea.setOnContextMenuRequested {
            contextMenu.show(buttonArea, it.screenX, it.screenY)
        }
    }

    private fun configureRotation(button: ToggleButton) {
        button.rotate = if (dockingSide == LEFT) -90.0 else 90.0
    }

    private fun configureDockingSide() {
        if (dockingSide == LEFT) {
            left = buttonArea
            right = null
        } else {
            left = null
            right = buttonArea
        }

        buttonArea.children.forEach {
            val button = (it as Group).children.first() as ToggleButton
            configureRotation(button)
        }
    }

    internal fun updateExpanded(item: DrawerItem) {
        if (item.expanded) {
            if (!contentArea.children.contains(item)) {
                if (!multiselect) {
                    contentArea.children.toTypedArray().forEach {
                        (it as DrawerItem).button.isSelected = false
                    }
                }
                // Insert into content area in position according to item order
                val itemIndex = items.indexOf(item)
                var inserted = false
                for (child in contentArea.children) {
                    val childIndex = items.indexOf(child)
                    if (childIndex > itemIndex) {
                        val childIndexInContentArea = contentArea.children.indexOf(child)
                        contentArea.children.add(childIndexInContentArea, item)
                        inserted = true
                        break
                    }
                }
                if (!inserted) {
                    contentArea.children.add(item)
                }
            }
        } else if (contentArea.children.contains(item)) {
            contentArea.children.remove(item)
        }

        configureContentArea()
    }

    // Dock is a child when there are expanded children
    private fun configureContentArea() {
        if (contentArea.children.isEmpty()) {
            center = null
            children.remove(contentArea)
        } else {
            if (floatingDrawers) {
                contentArea.isManaged = false
                if (!children.contains(contentArea)) children.add(contentArea)
            } else {
                contentArea.isManaged = true
                if (children.contains(contentArea)) children.remove(contentArea)
                center = contentArea
            }
        }
    }

    override fun layoutChildren() {
        super.layoutChildren()
        if (floatingDrawers && contentArea.children.isNotEmpty()) {
            val buttonBounds = buttonArea.layoutBounds
            contentArea.resizeRelocate(buttonBounds.maxX, buttonBounds.minY, contentArea.prefWidth(-1.0), buttonBounds.height)
        }
    }
}

class ExpandedDrawerContentArea : VBox() {
    init {
        addClass(DrawerStyles.contentArea)
        children.onChange { change ->
            while (change.next()) {
                if (change.wasAdded()) {
                    change.addedSubList.forEach {
                        if (VBox.getVgrow(it) == null) {
                            VBox.setVgrow(it, Priority.ALWAYS)
                        }
                    }
                }
            }
        }
    }
}

class DrawerItem(val drawer: Drawer, title: ObservableValue<String?>? = null, icon: ObservableValue<Node?>? = null) : StackPane() {
    internal val button = ToggleButton().apply {
        if (title != null) textProperty().bind(title)
        if (icon != null) graphicProperty().bind(icon)
    }

    val expanded: Boolean get() = button.isSelected

    init {
        button.selectedProperty().onChange { drawer.updateExpanded(this) }
        drawer.updateExpanded(this)
    }
}

class DrawerStyles : Stylesheet() {
    companion object {
        val drawer by cssclass()
        val buttonArea by cssclass()
        val contentArea by cssclass()
    }

    init {
        drawer {
            contentArea {
                borderColor += box(Color.DARKGRAY)
                borderWidth += box(0.5.px)
            }
            buttonArea {
                // -fx-shadow-highlight-color, -fx-outer-border, -fx-inner-border, -fx-body-color
                unsafe("-fx-background-color", raw("-fx-body-color"))

                toggleButton {
                    backgroundInsets += box(-1.px)
                    backgroundRadius += box(0.px)
                    and(selected) {
                        backgroundColor += c("#818181")
                        textFill = Color.WHITE
                    }
                }
            }
        }
    }
}
