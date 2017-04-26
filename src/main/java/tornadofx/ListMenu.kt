package tornadofx

import javafx.beans.DefaultProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
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
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Region
import javafx.scene.text.Text

@DefaultProperty("children")
class ListMenu : Control() {
    private val graphicFixedSize = FACTORY.createStyleableNumberProperty(this, "graphicFixedSize", "-fx-graphic-fixed-size", { it.graphicFixedSizeProperty })
    val graphicFixedSizeProperty: StyleableProperty<Number> get() = graphicFixedSize

    val themeProperty = SimpleStringProperty()
    var theme by themeProperty

    val activeItemProperty: ObjectProperty<ListMenuItem> = object : SimpleObjectProperty<ListMenuItem>(this, "active") {
        override fun set(item: ListMenuItem) {
            val previouslyActive = get()
            if (item === previouslyActive)
                return

            previouslyActive?.pseudoClassStateChanged(ACTIVE_PSEUDOCLASS_STATE, false)
            item.pseudoClassStateChanged(ACTIVE_PSEUDOCLASS_STATE, true)
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

    val items: List<ListMenuItem> get() = children.map { it as? ListMenuItem }.filterNotNull()

    val iconPositionProperty: ObjectProperty<Side> = object : SimpleObjectProperty<Side>(Side.LEFT) {
        override fun invalidated() {
            children.forEach { child ->
                if (child is ListMenuItem) {
                    child.needsLayout()
                }
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
    public override fun getChildren(): ObservableList<Node> = super.getChildren()

    override fun getUserAgentStylesheet(): String = ListMenu::class.java.getResource("listmenu.css").toExternalForm()

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

    internal val menu: ListMenu
        get() = parent as ListMenu
}

class ListMenuSkin(control: ListMenu) : SkinBase<ListMenu>(control) {

    private fun acc(fn: (Node) -> Double) = children.sumByDouble { fn(it) }

    private fun biggest(fn: (Node) -> Double) = children.map { fn(it) }.max() ?: 0.0

    override fun computeMinWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double) =
            if (skinnable.orientation == VERTICAL) biggest { n -> n.minWidth(height) } else acc { n -> n.minWidth(height) }

    override fun computeMinHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double) =
            if (skinnable.orientation == VERTICAL) acc { n -> n.minHeight(width) } else biggest { n -> n.minHeight(width) }

    override fun computePrefWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        val prefWidth = if (skinnable.orientation == VERTICAL)
            biggest { n -> n.prefWidth(height) } + leftInset + rightInset
        else
            acc { n -> n.prefWidth(height) } + leftInset + rightInset

        return Math.max(prefWidth, skinnable.prefWidth)
    }

    override fun computePrefHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        val prefHeight = if (skinnable.orientation == VERTICAL)
            acc { n -> n.prefHeight(width) } + topInset + bottomInset
        else
            biggest { n -> n.prefHeight(width) } + topInset + bottomInset

        return Math.max(prefHeight, skinnable.prefHeight)
    }

    override fun computeMaxWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double) =
            computePrefWidth(height, topInset, rightInset, bottomInset, leftInset)

    override fun computeMaxHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double) =
            computePrefHeight(width, topInset, rightInset, bottomInset, leftInset)

    override fun layoutChildren(x: Double, y: Double, w: Double, h: Double) {
        var xx = x
        var yy = y
        for (node in children) {
            if (skinnable.orientation == VERTICAL) {
                val prefHeight = node.prefHeight(-1.0)
                node.resizeRelocate(xx, yy, w, prefHeight)
                yy += prefHeight
            } else {
                val prefWidth = node.prefWidth(-1.0)
                node.resizeRelocate(xx, yy, prefWidth, h)
                xx += prefWidth
            }
        }
    }

}

class ListMenuItemSkin(control: ListMenuItem) : SkinBase<ListMenuItem>(control) {
    val text = Text()

    init {
        text.textProperty().bind(control.textProperty)
        children.add(text)

        if (skinnable.graphic != null)
            children.add(skinnable.graphic)

        skinnable.graphicProperty.addListener { _, old, new ->
            if (old != null) children.remove(old)
            if (new != null) children.add(new)
        }

        control.addEventHandler(MouseEvent.MOUSE_PRESSED) {
            control.requestFocus()
            control.menu.activeItem = control
        }
    }

    private fun acc(fn: (Node) -> Double): Double {
        var v = fn(text)
        if (skinnable.graphic != null)
            v += fn(skinnable.graphic!!)
        return v
    }

    private fun biggest(fn: (Node) -> Double): Double {
        val v = fn(text)

        if (skinnable.graphic != null) {
            val gval = fn(skinnable.graphic!!)
            if (gval > v) return gval
        }

        return v
    }

    override fun computeMinWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        if (iconPosition.isHorizontal)
            return biggest({ n -> n.minWidth(height) })
        else
            return acc({ n -> n.minWidth(height) })
    }

    override fun computeMinHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        if (iconPosition.isHorizontal)
            return acc({ n -> n.minHeight(width) })
        else
            return biggest({ n -> n.minHeight(width) })
    }

    override fun computePrefWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        var w = text.prefWidth(height)

        if (skinnable.graphic != null && iconPosition.isVertical)
            w += Math.max(skinnable.graphic!!.prefWidth(-1.0), graphicFixedSize)

        return w + leftInset + rightInset
    }

    override fun computePrefHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        var h = text.prefHeight(width)

        if (skinnable.graphic != null && iconPosition.isHorizontal)
            h += Math.max(skinnable.graphic!!.prefHeight(-1.0), graphicFixedSize)

        return h + topInset + bottomInset
    }

    override fun computeMaxWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return computePrefWidth(height, topInset, rightInset, bottomInset, leftInset)
    }

    override fun computeMaxHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return computePrefHeight(width, topInset, rightInset, bottomInset, leftInset)
    }

    private val iconPosition: Side get() = skinnable.menu.iconPosition

    override fun layoutChildren(x: Double, y: Double, w: Double, h: Double) {
        var xx = x
        var yy = y
        val graphic = skinnable.graphic

        when (iconPosition) {
            Side.TOP -> {
                if (graphic != null) {
                    val centeredX = xx + w / 2 - graphic.layoutBounds.width / 2
                    graphic.relocate(centeredX, yy)
                    yy += Math.max(graphic.layoutBounds.height, graphicFixedSize)
                }

                val centeredX = xx + w / 2 - text.prefWidth(-1.0) / 2
                text.resizeRelocate(centeredX, yy, text.prefWidth(-1.0), text.prefHeight(-1.0))
            }
            Side.BOTTOM -> {
                var centeredX = xx + w / 2 - text.prefWidth(-1.0) / 2
                text.resizeRelocate(centeredX, yy, text.prefWidth(-1.0), text.prefHeight(-1.0))

                if (graphic != null) {
                    yy += text.prefHeight(-1.0)
                    val fixedSize = graphicFixedSize
                    if (fixedSize > graphic.layoutBounds.height)
                        yy += fixedSize - graphic.layoutBounds.height

                    centeredX = xx + w / 2 - graphic.layoutBounds.width / 2
                    graphic.relocate(centeredX, yy)
                }
            }

            Side.LEFT -> {
                if (graphic != null) {
                    val centeredY = yy + h / 2 - graphic.layoutBounds.height / 2
                    graphic.relocate(xx, centeredY)

                    xx += Math.max(graphic.layoutBounds.width, graphicFixedSize)
                }

                val centeredY = yy + h / 2 - text.prefHeight(-1.0) / 2
                text.resizeRelocate(xx, centeredY, text.prefWidth(-1.0), text.prefHeight(-1.0))
            }
            Side.RIGHT -> {
                if (graphic != null) {
                    val centeredY = yy + h / 2 - graphic.layoutBounds.height / 2
                    val graphicWidth = Math.max(graphic.layoutBounds.width, graphicFixedSize)

                    graphic.resizeRelocate(w, centeredY, graphicWidth, graphic.prefHeight(-1.0))
                }

                val centeredY = yy + h / 2 - text.prefHeight(-1.0) / 2
                text.resizeRelocate(xx, centeredY, text.prefWidth(-1.0), text.prefHeight(-1.0))
            }
        }
    }

    private val graphicFixedSize: Double get() = skinnable.menu.graphicFixedSizeProperty.value.toDouble()

}

fun EventTarget.listmenu(orientation: Orientation = VERTICAL, iconPosition: Side = Side.LEFT, theme: String? = null, op: (ListMenu.() -> Unit)? = null): ListMenu {
    val listmenu = ListMenu()
    listmenu.orientation = orientation
    listmenu.iconPosition = iconPosition
    listmenu.theme = theme
    return opcr(this, listmenu, op)
}

fun ListMenu.item(text: String? = null, graphic: Node? = null, op: (ListMenuItem.() -> Unit)? = null) =
        opcr(this, ListMenuItem(text, graphic), op)