package tornadofx

import com.sun.org.apache.bcel.internal.Repository.addClass
import javafx.beans.property.*
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.event.EventTarget
import javafx.geometry.Orientation
import javafx.geometry.Side
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToolBar
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import tornadofx.DrawerStyles.Companion.buttonArea
import tornadofx.DrawerStyles.Companion.contentArea
import tornadofx.Stylesheet.Companion.bottom
import tornadofx.Stylesheet.Companion.contextMenu
import tornadofx.Stylesheet.Companion.left
import tornadofx.Stylesheet.Companion.right

fun EventTarget.drawer(side: Side = Side.LEFT, multiselect: Boolean = false, floatingContent: Boolean = false, op: Drawer.() -> Unit) = opcr(this, Drawer(side, multiselect, floatingContent), op)

class Drawer(side: Side, multiselect: Boolean, floatingContent: Boolean) : BorderPane() {
    val dockingSideProperty: ObjectProperty<Side> = SimpleObjectProperty(side)
    var dockingSide by dockingSideProperty

    val floatingDrawersProperty: BooleanProperty = SimpleBooleanProperty(floatingContent)
    var floatingDrawers by floatingDrawersProperty

    val maxContentSizeProperty: ObjectProperty<Number> = SimpleObjectProperty<Number>()
    var maxContentSize by maxContentSizeProperty

    val fixedContentSizeProperty: ObjectProperty<Number> = SimpleObjectProperty<Number>()
    var fixedContentSize by fixedContentSizeProperty

    val buttonArea = ToolBar().addClass(DrawerStyles.buttonArea)
    val contentArea = ExpandedDrawerContentArea()

    val items = FXCollections.observableArrayList<DrawerItem>()

    val multiselectProperty: BooleanProperty = SimpleBooleanProperty(multiselect)
    var multiselect by multiselectProperty

    val contextMenu = ContextMenu()

    override fun getUserAgentStylesheet() = DrawerStyles().base64URL.toExternalForm()

    fun item(title: String? = null, icon: Node? = null, expanded: Boolean = false, showHeader: Boolean = multiselect, op: DrawerItem.() -> Unit) =
            item(SimpleStringProperty(title), SimpleObjectProperty(icon), expanded, showHeader, op)

    fun item(title: ObservableValue<String?>, icon: ObservableValue<Node?>? = null, expanded: Boolean = multiselect, showHeader: Boolean = true, op: DrawerItem.() -> Unit): DrawerItem {
        val item = DrawerItem(this, title, icon, showHeader)
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

    fun item(uiComponent: UIComponent, expanded: Boolean = false, showHeader: Boolean = multiselect, op: DrawerItem.() -> Unit = {}): DrawerItem {
        val item = DrawerItem(this, uiComponent.titleProperty, uiComponent.iconProperty, showHeader)
        item.button.textProperty().bind(uiComponent.headingProperty)
        item.children.add(uiComponent.root)
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
            updateContentArea()
            parent?.requestLayout()
            scene?.root?.requestLayout()
        }

        // Adapt docking behavior to parent
        parentProperty().onChange {
            if (it is BorderPane) {
                if (it.left == this) dockingSide = Side.LEFT
                else if (it.right == this) dockingSide = Side.RIGHT
                else if (it.bottom == this) dockingSide = Side.BOTTOM
                else if (it.top == this) dockingSide = Side.TOP
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
                        contentArea.children.remove(it)
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
        button.rotate = when (dockingSide) {
            Side.LEFT -> -90.0
            Side.RIGHT -> 90.0
            else -> 0.0
        }
    }

    private fun configureDockingSide() {
        when (dockingSide) {
            Side.LEFT -> {
                left = buttonArea
                right = null
                bottom = null
                top = null
                buttonArea.orientation = Orientation.VERTICAL
            }
            Side.RIGHT -> {
                left = null
                right = buttonArea
                bottom = null
                top = null
                buttonArea.orientation = Orientation.VERTICAL
            }
            Side.BOTTOM -> {
                left = null
                right = null
                bottom = buttonArea
                top = null
                buttonArea.orientation = Orientation.HORIZONTAL
            }
            Side.TOP -> {
                left = null
                right = null
                bottom = null
                top = buttonArea
                buttonArea.orientation = Orientation.HORIZONTAL
            }
        }

        buttonArea.items.forEach {
            val button = (it as Group).children.first() as ToggleButton
            configureRotation(button)
        }
    }

    internal fun updateExpanded(item: DrawerItem) {
        if (item.expanded) {
            if (item !in contentArea.children) {
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
        } else if (item in contentArea.children) {
            contentArea.children.remove(item)
        }

        updateContentArea()
    }

    // Dock is a child when there are expanded children
    private fun updateContentArea() {
        if (contentArea.children.isEmpty()) {
            center = null
            children.remove(contentArea)
        } else {
            if (fixedContentSize != null) {
                when (dockingSide) {
                    Side.LEFT, Side.RIGHT -> {
                        contentArea.maxWidth = fixedContentSize.toDouble()
                        contentArea.minWidth = fixedContentSize.toDouble()
                    }
                    Side.TOP, Side.BOTTOM -> {
                        contentArea.maxHeight = fixedContentSize.toDouble()
                        contentArea.minHeight = fixedContentSize.toDouble()
                    }
                }
            } else {
                contentArea.maxWidth = USE_COMPUTED_SIZE
                contentArea.minWidth = USE_COMPUTED_SIZE
                contentArea.maxHeight = USE_COMPUTED_SIZE
                contentArea.minHeight = USE_COMPUTED_SIZE
                if (maxContentSize != null) {
                    when (dockingSide) {
                        Side.LEFT, Side.RIGHT -> contentArea.maxWidth = maxContentSize.toDouble()
                        Side.TOP, Side.BOTTOM -> contentArea.maxHeight = maxContentSize.toDouble()
                    }
                }
            }

            if (floatingDrawers) {
                contentArea.isManaged = false
                if (contentArea !in children) children.add(contentArea)
            } else {
                contentArea.isManaged = true
                if (contentArea in children) children.remove(contentArea)
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

class DrawerItem(val drawer: Drawer, title: ObservableValue<String?>? = null, icon: ObservableValue<Node?>? = null, showHeader: Boolean) : VBox() {
    internal val button = ToggleButton().apply {
        if (title != null) textProperty().bind(title)
        if (icon != null) graphicProperty().bind(icon)
    }

    val expandedProperty = button.selectedProperty()
    var expanded by expandedProperty

    init {
        addClass(DrawerStyles.drawerItem)
        if (showHeader) {
            titledpane {
                textProperty().bind(title)
                isCollapsible = false
            }
        }
        button.selectedProperty().onChange { drawer.updateExpanded(this) }
        drawer.updateExpanded(this)

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

class DrawerStyles : Stylesheet() {
    companion object {
        val drawer by cssclass()
        val drawerItem by cssclass()
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
                spacing = 0.px
                padding = box(0.px)
                toggleButton {
                    backgroundInsets += box(0.px)
                    backgroundRadius += box(0.px)
                    and(selected) {
                        backgroundColor += c("#818181")
                        textFill = Color.WHITE
                    }
                }
            }
        }
        drawerItem child titledPane {
            title {
                backgroundRadius += box(0.px)
                padding = box(2.px, 5.px)
            }
            content {
                borderColor += box(Color.TRANSPARENT)
            }
        }
    }
}