package tornadofx

import javafx.beans.property.*
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Labeled
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Region
import javafx.scene.text.Text
import javafx.stage.Modality
import tornadofx.LayoutDebugger.DebugStyles.Companion.debugNode

@Suppress("UNCHECKED_CAST")
class LayoutDebugger : View() {
    override val root = BorderPane()

    val hoveredNode = SimpleObjectProperty<Node>()
    val selectedNode = SimpleObjectProperty<Node>()
    val selectedScene = SimpleObjectProperty<Scene>()
    var nodeTree: TreeView<Node> by singleAssign()
    val pickerActive = SimpleBooleanProperty(true)

    init {
        title = "Layout Debugger"

        with(root) {
            setPrefSize(1024.0, 768.0)

            left {
                treeview<Node> {
                    nodeTree = this
                    cellFormat {
                        graphic = null
                        text = it.javaClass.simpleName
                    }
                }
            }
        }

        hookListeners()
    }

    private fun hookListeners() {
        nodeTree.selectionModel.selectedItemProperty().addListener { observableValue, oldItem, newItem ->
            if (newItem != null) {
                setSelectedNode(newItem.value)
                newItem.value.addClass(debugNode)
            }

            if (oldItem != null)
                oldItem.value.removeClass(debugNode)

            hoveredNode.value = null
            pickerActive.value = false
        }

        selectedScene.addListener { observableValue, oldScene, newScene ->
            nodeTree.root = NodeTreeItem(newScene.root)
            nodeTree.populate(
                    itemFactory = {
                        NodeTreeItem(it)
                    },
                    childFactory = {
                        val value = it.value
                        if (value is Parent) value.childrenUnmodifiable else null
                    }
            )
        }

        hoveredNode.addListener { observableValue, oldNode, newNode ->
            if (pickerActive.value && oldNode != newNode) {
                oldNode?.removeClass(debugNode)
                newNode?.addClass(debugNode)
            }
        }
    }

    val hoverHandler = EventHandler<MouseEvent> { event ->
        val currentHover = hoveredNode.value
        var newHover = event.target
        if (newHover is Node) {
            // Select labeled parent instead of text node
            if (newHover is Text && newHover.parent is Labeled) newHover = newHover.parent

            if (currentHover != newHover)
                hoveredNode.value = newHover as Node
        }

    }

    val clickHandler = EventHandler<MouseEvent> { event ->
        val clickedTarget = event.target
        if (clickedTarget is Node) setSelectedNode(clickedTarget)
        event.consume()
    }

    private fun setSelectedNode(node: Node) {
        selectedNode.value = node
//        node.removeClass(debugNode)
//        node.addClass(debugNode)
        val treeItem = selectedNode.value.properties["tornadofx.layoutdebugger.treeitem"] as TreeItem<Node>?
        if (treeItem != null) nodeTree.selectionModel.select(treeItem)
        root.center = NodePropertyView(node)
    }

    companion object {
        fun startDebugging(scene: Scene) {
            val debugger = find(LayoutDebugger::class)
            scene.addEventFilter(MouseEvent.MOUSE_MOVED, debugger.hoverHandler)
            scene.addEventFilter(MouseEvent.MOUSE_CLICKED, debugger.clickHandler)
            scene.stylesheets.add(DebugStyles().base64URL.toExternalForm())
            debugger.selectedScene.value = scene
            debugger.openModal(modality = Modality.NONE)
        }

        fun stopDebugging(scene: Scene) {
            val debugger = find(LayoutDebugger::class)
            scene.removeEventFilter(MouseEvent.MOUSE_MOVED, debugger.hoverHandler)
            scene.removeEventFilter(MouseEvent.MOUSE_CLICKED, debugger.clickHandler)
            scene.stylesheets.remove(DebugStyles().base64URL.toExternalForm())
            debugger.selectedScene.value = null
        }
    }

    class DebugStyles : Stylesheet() {
        companion object {
            val debugNode by cssclass()
        }

        init {
            s(debugNode) {
                backgroundColor = multi(c("#cecece", 0.6))
            }
        }
    }

    class NodeTreeItem(node: Node) : TreeItem<Node>(node) {
        init {
            value.properties["tornadofx.layoutdebugger.treeitem"] = this
        }
    }

    inner class NodePropertyView(val node: Node) : Form() {
        init {
            fieldset {
                field("className") {
                    text(node.javaClass.name)
                }
                field("styleClass") {
                    text(node.styleClass.joinToString(", "))
                }
            }
            fieldset("Dimensions") {

            }
            fieldset("Properties") {
                if (node is Region) {
                    field("Color") {

                    }
                    field("Background color") {
                        for (fill in node.background.fills) {
//                            colorpicker().valueProperty().bind(Simple)
                        }
                    }
                }
                for (method in node.javaClass.methods.filter { it.name.endsWith("Property") && it.parameterCount == 0 && Property::class.java.isAssignableFrom(it.returnType) }) {
                    val prop = method.invoke(node) as Property<Any>?
                    val propertyType = getPropertyType(prop, node)

                    if (propertyType != null) {
                            when (propertyType) {
                                StringProperty::class.java -> {
                                    field(prop!!.name) {
                                        textfield().bind(prop as StringProperty)
                                    }
                                }
                                DoubleProperty::class.java -> {
                                    field(prop!!.name) {
                                        textfield().bind(prop as DoubleProperty)
                                    }
                                }
                                IntegerProperty::class.java -> {
                                    field(prop!!.name) {
                                        textfield().bind(prop as IntegerProperty)
                                    }
                                }
                            }
                    }
                }

            }
        }
    }

    private fun getPropertyType(prop: Property<Any>?, node: Node) : Class<*>? {
        if (prop == null) return null
        if (prop.name == null) return null
        val cl = node.javaClass
        cl.methods.find { it.name == "${prop.name}Property" && it.parameterCount == 0 }?.apply { return returnType }
        cl.declaredMethods.find { it.name == "${prop.name}Property" && it.parameterCount == 0 }?.apply { return returnType }
        return null
    }
}
