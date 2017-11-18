@file:Suppress("unused", "UNCHECKED_CAST")

package tornadofx

import javafx.beans.DefaultProperty
import javafx.beans.property.*
import javafx.collections.ObservableList
import javafx.css.*
import javafx.event.EventTarget
import javafx.geometry.Orientation
import javafx.geometry.Orientation.VERTICAL
import javafx.geometry.Side
import javafx.scene.Node
import javafx.scene.control.Control
import javafx.scene.control.Skin
import javafx.scene.control.SkinBase
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Region
import javafx.scene.text.Text
import tornadofx.WizardStyles.Companion.graphic

@DefaultProperty("children")
class ListMenu : Control() {
    private val graphicFixedSizeInternal = FACTORY.createStyleableNumberProperty(this, "graphicFixedSize", "-fx-graphic-fixed-size", { it.graphicFixedSizeProperty })
    val graphicFixedSizeProperty: StyleableProperty<Number> get() = graphicFixedSizeInternal
    var graphicFixedSized: Number get() = graphicFixedSizeInternal.value; set(value) {
        graphicFixedSizeInternal.value = value
    }

    val themeProperty = SimpleStringProperty()
    var theme by themeProperty

    val activeItemProperty: ObjectProperty<ListMenuItem?> = object : SimpleObjectProperty<ListMenuItem?>(this, "active") {
        override fun set(item: ListMenuItem?) {
            val previouslyActive = get()
            if (item === previouslyActive)
                return

            previouslyActive?.pseudoClassStateChanged(ACTIVE_PSEUDOCLASS_STATE, false)
            previouslyActive?.internalActiveProperty?.value = false
            item?.pseudoClassStateChanged(ACTIVE_PSEUDOCLASS_STATE, true)
            item?.internalActiveProperty?.value = true
            super.set(item)
        }
    }
    var activeItem: ListMenuItem? by activeItemProperty

    val orientationProperty: ObjectProperty<Orientation> = object : SimpleObjectProperty<Orientation>(VERTICAL) {
        override fun invalidated() {
            isNeedsLayout = true
            requestLayout()
        }
    }
    var orientation: Orientation by orientationProperty

    val iconPositionProperty: ObjectProperty<Side> = object : SimpleObjectProperty<Side>(Side.LEFT) {
        override fun invalidated() {
            children.forEach { child ->
                (child as? ListMenuItem)?.needsLayout()
            }
        }
    }
    var iconPosition: Side by iconPositionProperty

    init {
        styleClass.add("list-menu")
        isFocusTraversable = true
        themeProperty.addListener { _, oldTheme, newTheme ->
            if (oldTheme != null) removeClass(oldTheme)
            if (newTheme != null) addClass(newTheme)
        }
    }

    override fun getControlCssMetaData(): List<CssMetaData<out Styleable, *>> = FACTORY.cssMetaData

    override fun createDefaultSkin() = ListMenuSkin(this)

    val items: ObservableList<ListMenuItem> get() = children as ObservableList<ListMenuItem>

    override fun getUserAgentStylesheet(): String = ListMenu::class.java.getResource("listmenu.css").toExternalForm()

    fun item(text: String? = null, graphic: Node? = null, tag: Any? = null, op :ListMenuItem.() -> Unit = {}): ListMenuItem {
        val item = ListMenuItem(text ?: tag?.toString(), graphic)
        item.tag = tag
        return opcr(this, item, op)
    }

    companion object {
        private val ACTIVE_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("active")
        private val FACTORY = StyleablePropertyFactory<ListMenu>(Region.getClassCssMetaData())
    }
}

class ListMenuItem(text: String? = null, graphic: Node? = null) : Control() {
    val graphicProperty: ObjectProperty<Node> = SimpleObjectProperty(this, "graphic", graphic)
    var graphic: Node? by graphicProperty

    val textProperty: StringProperty = SimpleStringProperty(text)
    var text: String? by textProperty

    internal val internalActiveProperty = ReadOnlyBooleanWrapper()
    val activeProperty: ReadOnlyBooleanProperty = internalActiveProperty.readOnlyProperty
    val active by activeProperty

    init {
        styleClass.add("list-item")
        isFocusTraversable = true
    }

    override fun createDefaultSkin(): Skin<*> {
        return ListMenuItemSkin(this)
    }

    fun needsLayout() {
        isNeedsLayout = true
        requestLayout()
    }

    fun whenSelected(op: () -> Unit) {
        activeProperty.onChange { if (it) op() }
    }

    internal val menu: ListMenu
        get() = parent as ListMenu
}

class ListMenuSkin(control: ListMenu) : SkinBase<ListMenu>(control) {

    private fun acc(fn: (Node) -> Double) = children.sumByDouble { fn(it) }

    private fun biggest(fn: (Node) -> Double) = children.map { fn(it) }.max() ?: 0.0

    override fun computeMinWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double) =
            if (skinnable.orientation == VERTICAL) biggest { it.minWidth(height) } else acc { it.minWidth(height) }

    override fun computeMinHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double) =
            if (skinnable.orientation == VERTICAL) acc { it.minHeight(width) } else biggest { it.minHeight(width) }

    override fun computePrefWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        val prefWidth = if (skinnable.orientation == VERTICAL)
            biggest { it.prefWidth(height) } + leftInset + rightInset
        else
            acc { it.prefWidth(height) } + leftInset + rightInset

        return Math.max(prefWidth, skinnable.prefWidth)
    }

    override fun computePrefHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        val prefHeight = if (skinnable.orientation == VERTICAL)
            acc { it.prefHeight(width) } + topInset + bottomInset
        else
            biggest { it.prefHeight(width) } + topInset + bottomInset

        return Math.max(prefHeight, skinnable.prefHeight)
    }

    override fun computeMaxWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double) =
            if (skinnable.maxWidth == Region.USE_COMPUTED_SIZE) computePrefWidth(height, topInset, rightInset, bottomInset, leftInset) else skinnable.maxWidth

    override fun computeMaxHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double) =
            if (skinnable.maxHeight == Region.USE_COMPUTED_SIZE) computePrefHeight(width, topInset, rightInset, bottomInset, leftInset) else skinnable.maxHeight

    override fun layoutChildren(x: Double, y: Double, w: Double, h: Double) {
        var currentX = x
        var currentY = y
        for (node in children) {
            if (skinnable.orientation == VERTICAL) {
                val prefHeight = node.prefHeight(-1.0)
                node.resizeRelocate(currentX, currentY, w, prefHeight)
                currentY += prefHeight
            } else {
                val prefWidth = node.prefWidth(-1.0)
                node.resizeRelocate(currentX, currentY, prefWidth, h)
                currentX += prefWidth
            }
        }
    }
}

class ListMenuItemSkin(control: ListMenuItem) : SkinBase<ListMenuItem>(control) {
    val text = Text().addClass("text")

    private fun registerGraphic(node: Node) {
        children.add(node)
        node.addClass("graphic")
        (node as? ImageView)?.isMouseTransparent = true
    }

    init {
        text.textProperty().bind(control.textProperty)
        children.add(text)

        if (skinnable.graphic != null) registerGraphic(skinnable.graphic!!)

        skinnable.graphicProperty.addListener { _, old, new ->
            if (old != null) children.remove(old)
            if (new != null) registerGraphic(new)
        }

        control.addEventHandler(MouseEvent.MOUSE_PRESSED) {
            control.requestFocus()
            control.menu.activeItem = control
        }
    }

    override fun computePrefWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        var w = if (text.text.isNullOrBlank()) 0.0 else text.prefWidth(height)

        if (skinnable.graphic != null) {
            val graphicSize = Math.max(skinnable.graphic!!.prefWidth(-1.0), graphicFixedSize)
            if (iconPosition.isVertical)
                w += graphicSize
            else
                w = Math.max(w, graphicSize)
        }

        return w + leftInset + rightInset
    }

    override fun computePrefHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        var h = if (text.text.isNullOrBlank()) 0.0 else text.prefHeight(width)

        if (skinnable.graphic != null) {
            val graphicSize = Math.max(skinnable.graphic!!.prefHeight(-1.0), graphicFixedSize)
            if (iconPosition.isHorizontal)
                h += graphicSize
            else
                h = Math.max(h, graphicSize)
        }
        return h + topInset + bottomInset
    }

    override fun computeMaxWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return computePrefWidth(height, topInset, rightInset, bottomInset, leftInset)
    }

    override fun computeMaxHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return computePrefHeight(width, topInset, rightInset, bottomInset, leftInset)
    }

    override fun computeMinWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double) =
            computePrefWidth(height, topInset, rightInset, bottomInset, leftInset)

    override fun computeMinHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double) =
            computePrefHeight(width, topInset, rightInset, bottomInset, leftInset)

    private val iconPosition: Side get() = skinnable.menu.iconPosition

    private val textPrefWidth: Double = if (text.text.isNullOrBlank()) 0.0 else text.prefWidth(-1.0)
    private val textPrefHeight: Double = if (text.text.isNullOrBlank()) 0.0 else text.prefHeight(-1.0)

    override fun layoutChildren(x: Double, y: Double, w: Double, h: Double) {
        var currentX = x
        var currentY = y
        val graphic = skinnable.graphic
        val hasText = textPrefWidth > 0.0

        if (!hasText) {
            if (graphic != null) {
                val centeredX = x + w / 2 - graphic.layoutBounds.width / 2
                val centeredY = y + h / 2 - graphic.layoutBounds.height / 2
                graphic.relocate(centeredX, centeredY)
            }
            return
        }

        when (iconPosition) {
            Side.TOP -> {
                if (graphic != null) {
                    val centeredX = currentX + w / 2 - graphic.layoutBounds.width / 2
                    graphic.relocate(centeredX, currentY)
                    currentY += Math.max(graphic.layoutBounds.height, graphicFixedSize)
                }
                val centeredX = currentX + w / 2 - textPrefWidth / 2
                text.resizeRelocate(centeredX, currentY, textPrefWidth, textPrefHeight)
            }
            Side.BOTTOM -> {
                if (hasText) {
                    val centeredX = currentX + w / 2 - textPrefWidth / 2
                    text.resizeRelocate(centeredX, currentY, textPrefWidth, textPrefHeight)
                    currentY += textPrefHeight
                }

                if (graphic != null) {
                    val fixedSize = graphicFixedSize
                    if (fixedSize > graphic.layoutBounds.height) {
                        currentY += (fixedSize - graphic.layoutBounds.height) / 2
                    }
                    val centeredX = currentX + w / 2 - graphic.layoutBounds.width / 2
                    graphic.relocate(centeredX, currentY)
                }
            }

            Side.LEFT -> {
                if (graphic != null) {
                    val centeredY = currentY + h / 2 - graphic.layoutBounds.height / 2
                    graphic.relocate(currentX, centeredY)
                    currentX += Math.max(graphic.layoutBounds.width, graphicFixedSize)
                }
                val centeredY = currentY + h / 2 - textPrefHeight / 2
                text.resizeRelocate(currentX, centeredY, textPrefWidth, textPrefHeight)
            }
            Side.RIGHT -> {
                if (graphic != null) {
                    val centeredY = currentY + h / 2 - graphic.layoutBounds.height / 2
                    val graphicWidth = Math.max(graphic.layoutBounds.width, graphicFixedSize)
                    graphic.resizeRelocate(w - skinnable.padding.right, centeredY, graphicWidth, graphic.prefHeight(-1.0))
                }

                val centeredY = currentY + h / 2 - textPrefHeight / 2
                text.resizeRelocate(currentX, centeredY, textPrefWidth, textPrefHeight)
            }
        }
    }

    private val graphicFixedSize: Double get() = skinnable.menu.graphicFixedSizeProperty.value.toDouble()
}

fun EventTarget.listmenu(orientation: Orientation = VERTICAL, iconPosition: Side = Side.LEFT, theme: String? = null, tag: Any? = null, op: ListMenu.() -> Unit = {}): ListMenu {
    val listmenu = ListMenu().apply {
        this.orientation = orientation
        this.iconPosition = iconPosition
        this.theme = theme
        this.tag = tag
    }
    return opcr(this, listmenu, op)
}
