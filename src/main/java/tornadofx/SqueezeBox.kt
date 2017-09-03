package tornadofx

import com.sun.javafx.scene.control.behavior.BehaviorBase
import com.sun.javafx.scene.control.skin.BehaviorSkinBase
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Control
import javafx.scene.control.TitledPane

class SqueezeBox(multiselect: Boolean = true, fillHeight: Boolean = false) : Control() {
    internal val panes = FXCollections.observableArrayList<TitledPane>()

    val multiselectProperty: BooleanProperty = SimpleBooleanProperty(multiselect)
    var multiselect by multiselectProperty

    val fillHeightProperty: BooleanProperty = SimpleBooleanProperty(fillHeight)
    var fillHeight by fillHeightProperty

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

        multiselectProperty.onChange {
            if (!multiselect) {
                panes.filter { it.isExpanded }.drop(1).forEach { it.isExpanded = false }
            }
        }
    }

    override fun getUserAgentStylesheet() = SqueezeBoxStyles().base64URL.toExternalForm()

    override fun createDefaultSkin() = SqueezeBoxSkin(this)

    internal fun addChild(child: Node) {
        children.add(child)
    }

    internal fun updateExpanded(pane: TitledPane) {
        if (!multiselect && pane.isExpanded) {
            panes.filter { it != pane && it.isExpanded }.forEach { it.isExpanded = false }
        }
    }
}

class SqueezeBoxSkin(val control: SqueezeBox) : BehaviorSkinBase<SqueezeBox, SqueezeBoxBehavior>(control, SqueezeBoxBehavior(control)) {
    init {
        registerChangeListener(skinnable.widthProperty(), "WIDTH")
        registerChangeListener(skinnable.heightProperty(), "HEIGHT")
    }

    override fun computeMinHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return children.sumByDouble { it.minHeight(width) } + topInset + bottomInset
    }

    override fun computePrefHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return children.sumByDouble { it.prefHeight(width) } + topInset + bottomInset
    }

    override fun computeMinWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return children.map { it.minWidth(height) }.max() ?: 0.0 + leftInset + rightInset
    }

    override fun computePrefWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return children.map { it.prefWidth(height) }.max() ?: 0.0 + leftInset + rightInset
    }

    override fun layoutChildren(contentX: Double, contentY: Double, contentWidth: Double, contentHeight: Double) {
        var currentY = contentY
        var extraHeightPerExpandedPane = 0.0
        if (skinnable.fillHeight) {
            var totalPrefHeight: Double = 0.0
            var expandedCount = 0
            control.panes.forEach {
                if (it.isExpanded) expandedCount += 1
                totalPrefHeight += it.prefHeight(contentWidth)
            }
            extraHeightPerExpandedPane = (contentHeight - totalPrefHeight) / expandedCount
        }
        control.panes.forEach { pane ->
            val prefHeight = pane.prefHeight(contentWidth) + if (pane.isExpanded) extraHeightPerExpandedPane else 0.0
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

fun EventTarget.squeezebox(multiselect: Boolean = true, fillHeight: Boolean = true, op: SqueezeBox.() -> Unit) = opcr(this, SqueezeBox(multiselect, fillHeight), op)

fun SqueezeBox.fold(title: String? = null, expanded: Boolean = false, icon: Node? = null, closeable: Boolean = false, op: TitledPane.() -> Unit): TitledPane {
    val fold = TitledPane(title, null)
    fold.expandedProperty().onChange { updateExpanded(fold) }
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