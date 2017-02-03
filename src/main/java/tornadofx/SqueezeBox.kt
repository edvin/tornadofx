package tornadofx

import com.sun.javafx.scene.control.behavior.BehaviorBase
import com.sun.javafx.scene.control.skin.BehaviorSkinBase
import javafx.collections.FXCollections
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Control
import javafx.scene.control.TitledPane

class SqueezeBox : Control() {
    internal val panes = FXCollections.observableArrayList<TitledPane>()

    init {
        addClass(SqueezeBoxStyles.squeezeBox)
        children.onChange { change ->
            while (change.next()) {
                if (change.wasAdded()) {
                    change.addedSubList.forEach {
                        if (it is TitledPane) panes.add(it)
                    }
                }
                if (change.wasRemoved()) {
                    change.removed.forEach {
                        if (it is TitledPane) panes.remove(it)
                    }
                }
            }
        }
    }

    override fun getUserAgentStylesheet() = SqueezeBoxStyles().base64URL.toExternalForm()

    override fun createDefaultSkin() = SqueezeBoxSkin(this)

    internal fun addChild(child: Node) {
        children.add(child)
    }

}

class SqueezeBoxSkin(val control: SqueezeBox) : BehaviorSkinBase<SqueezeBox, SqueezeBoxBehavior>(control, SqueezeBoxBehavior(control)) {
    init {
        registerChangeListener(skinnable.widthProperty(), "WIDTH")
        registerChangeListener(skinnable.heightProperty(), "HEIGHT")
    }

    override fun computeMinHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return children.map { it.minHeight(width) }.sum() + topInset + bottomInset
    }

    override fun computePrefHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return children.map { it.prefHeight(width) }.sum() + topInset + bottomInset
    }

    override fun computeMinWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return children.map { it.minWidth(height) }.max() ?: 0.0 + leftInset + rightInset
    }

    override fun computePrefWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return children.map { it.prefWidth(height) }.max() ?: 0.0 + leftInset + rightInset
    }

    override fun layoutChildren(contentX: Double, contentY: Double, contentWidth: Double, contentHeight: Double) {
        var currentY = contentY
        control.panes.forEach { pane ->
            val prefHeight = pane.prefHeight(contentWidth)
            pane.resizeRelocate(contentX, currentY, contentWidth, prefHeight)
            pane.renderCloseButton(contentWidth, currentY)
            currentY += prefHeight
        }
    }

    private fun Node.renderCloseButton(contentWidth: Double, contentY: Double) {
        if (properties["tornadofx.closeable"] == true) {
            val closeButton = properties.getOrPut("tornadofx.closeButton") {
                Button().apply {
                    addClass(SqueezeBoxStyles.closeButton)
                    isFocusTraversable = false
                    control.addChild(this)
                    setOnAction {
                        this@renderCloseButton.removeFromParent()
                        removeFromParent()
                    }
                    graphic = svgpath(InternalWindow.Styles.crossPath)
                }
            } as Button
            closeButton.resizeRelocate(contentWidth - 20, contentY + 4, 16.0, 16.0)
        }
    }

    override fun handleControlPropertyChanged(propertyReference: String?) {
        super.handleControlPropertyChanged(propertyReference)
    }
}

fun EventTarget.squeezebox(op: SqueezeBox.() -> Unit) = opcr(this, SqueezeBox(), op)

fun SqueezeBox.fold(title: String? = null, expanded: Boolean = false, icon: Node? = null, closeable: Boolean = false, op: TitledPane.() -> Unit): TitledPane {
    val fold = TitledPane(title, null)
    fold.graphic = icon
    fold.isExpanded = expanded
    fold.properties["tornadofx.closeable"] = closeable
    addChild(fold)
    op.invoke(fold)
    return fold
}

class SqueezeBoxBehavior(control: SqueezeBox) : BehaviorBase<SqueezeBox>(control, mutableListOf())

class SqueezeBoxStyles : Stylesheet() {
    companion object {
        val squeezeBox by cssclass()
        val closeButton by cssclass()
    }

    init {
        squeezeBox child titledPane {
            title {
                backgroundRadius += box(0.px)
            }
        }
        closeButton {
            backgroundInsets += box(0.px)
        }
    }
}