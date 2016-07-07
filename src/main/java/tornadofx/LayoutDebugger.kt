package tornadofx

import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.SnapshotParameters
import javafx.scene.control.Labeled
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.stage.Modality
import tornadofx.LayoutDebugger.DebugStyles.Companion.debugHover

class LayoutDebugger : View() {
    override val root = BorderPane()

    val hoveredNode = SimpleObjectProperty<Node>()
    val selectedNode = SimpleObjectProperty<Node>()

    init {
        title = "Layout Debugger"

        with (root) {
            center {
                imageview {
                    selectedNode.addListener { observableValue, oldNode, newNode ->
                        image = newNode.snapshot(SnapshotParameters(), null)
                    }
                }
            }
        }
    }

    val hoverHandler = EventHandler<MouseEvent> { event ->
        val currentHover = hoveredNode.value
        var newHover = event.target
        if (newHover is Node) {
            // Select labeled parent instead of text node
            if (newHover is Text && newHover.parent is Labeled) newHover = newHover.parent

            if (currentHover != newHover) {
                if (currentHover is Node)
                    currentHover.removeClass(debugHover)

                hoveredNode.value = newHover as Node

                newHover.addClass(debugHover)
            }
        }

    }

    val clickHandler = EventHandler<MouseEvent> { event ->
        val clickedTarget = event.target
        if (clickedTarget is Node) {
            selectedNode.value = clickedTarget
            println(event.target)
        }
        event.consume()
    }

    companion object {
        fun startDebugging(scene: Scene) {
            val debugger = find(LayoutDebugger::class)
            scene.addEventFilter(MouseEvent.MOUSE_MOVED, debugger.hoverHandler)
            scene.addEventFilter(MouseEvent.MOUSE_CLICKED, debugger.clickHandler)
            scene.stylesheets.add(DebugStyles().base64URL.toExternalForm())
            debugger.openModal(modality = Modality.NONE)
        }

        fun stopDebugging(scene: Scene) {
            val debugger = find(LayoutDebugger::class)
            scene.removeEventFilter(MouseEvent.MOUSE_MOVED, debugger.hoverHandler)
            scene.removeEventFilter(MouseEvent.MOUSE_CLICKED, debugger.clickHandler)
            scene.stylesheets.remove(DebugStyles().base64URL.toExternalForm())
        }
    }

    class DebugStyles : Stylesheet() {
        companion object {
            val debugHover by cssclass()
        }

        init {
            s(debugHover) {
                textFill = Color.GREEN
                backgroundColor = multi(c("#ceceff", 0.7))
            }
        }
    }
}
