package tornadofx

import com.sun.javafx.scene.control.behavior.BehaviorBase
import com.sun.javafx.scene.control.skin.BehaviorSkinBase
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.control.Accordion
import javafx.scene.control.Control
import javafx.scene.control.TitledPane

class SqueezeBox : Control() {
    val panes: ObservableList<TitledPane> = FXCollections.observableArrayList()

    init {
        addClass(SqueezeBoxStyles.squeezeBox)
    }

    override fun createDefaultSkin() = SqueezeBoxSkin(this)
}

class SqueezeBoxSkin(control: SqueezeBox) : BehaviorSkinBase<SqueezeBox, SqueezeBoxBehavior>(control, SqueezeBoxBehavior(control)) {
    init {
        registerChangeListener(skinnable.widthProperty(), "WIDTH")
        registerChangeListener(skinnable.heightProperty(), "HEIGHT")
        children.addAll(control.panes)
    }

    override fun computeMinHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return children.map { it.minHeight(width) }.sum() + topInset + bottomInset
    }

    override fun computePrefHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return children.map { it.prefHeight(width) }.sum() + topInset + bottomInset
    }

    override fun computeMinWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return children.map { it.minWidth(height) }.sum() + leftInset + rightInset
    }

    override fun computePrefWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return children.map { it.prefWidth(height) }.sum() + leftInset + rightInset
    }

    override fun layoutChildren(contentX: Double, contentY: Double, contentWidth: Double, contentHeight: Double) {
        var currentY = contentY
        children.forEach { pane ->
            val prefHeight = pane.prefHeight(contentWidth)
            pane.resizeRelocate(contentX, currentY, contentWidth, prefHeight)
            currentY += prefHeight
        }
    }

    override fun handleControlPropertyChanged(propertyReference: String?) {
        super.handleControlPropertyChanged(propertyReference)
    }
}

fun EventTarget.squeezebox(op: SqueezeBox.() -> Unit) = opcr(this, SqueezeBox(), op)

fun SqueezeBox.fold(title: String? = null, expanded: Boolean = false, op: TitledPane.() -> Unit): TitledPane {
    val fold = TitledPane(title, null)
    fold.isExpanded = expanded
    panes += fold
    op.invoke(fold)
    return fold
}

class SqueezeBoxBehavior(control: SqueezeBox) : BehaviorBase<SqueezeBox>(control, mutableListOf())

class SqueezeBoxStyles : Stylesheet() {
    companion object {
        val squeezeBox by cssclass()
    }
}