package tornadofx

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.stage.Modality
import javafx.util.StringConverter
import javafx.util.converter.DoubleStringConverter
import tornadofx.LayoutDebugger.DebugStyles.Companion.debugNode

@Suppress("UNCHECKED_CAST")
class LayoutDebugger : View() {
    override val root = BorderPane()

    val hoveredNode = SimpleObjectProperty<Node>()
    val selectedNode = SimpleObjectProperty<Node>()
    val selectedScene = SimpleObjectProperty<Scene>()
    var nodeTree: TreeView<Node> by singleAssign()
    val pickerActive = SimpleBooleanProperty(true)
    val propertyContainer = ScrollPane()

    init {
        title = "Layout Debugger"

        with(root) {
            setPrefSize(800.0, 600.0)

            center {
                splitpane {
                    setDividerPosition(0, 0.3)
                    items {
                        treeview<Node> {
                            nodeTree = this
                            cellFormat {
                                graphic = null
                                text = it.javaClass.simpleName
                            }
                        }
                        this += propertyContainer.apply {
                            padding = Insets(10.0)
                        }
                    }

                }
            }

            bottom {
                hbox(10.0) {
                    togglebutton("Pick") {
                        selectedProperty().bindBidirectional(pickerActive)
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
        if (!pickerActive.value) return@EventHandler
        val clickedTarget = event.target
        if (clickedTarget is Node) setSelectedNode(clickedTarget)
        event.consume()
    }

    private fun setSelectedNode(node: Node) {
        selectedNode.value = node
        val treeItem = selectedNode.value.properties["tornadofx.layoutdebugger.treeitem"] as TreeItem<Node>?
        if (treeItem != null) nodeTree.selectionModel.select(treeItem)
        propertyContainer.content = NodePropertyView(node)
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
            debugNode {
                backgroundColor += c("ffff00", 0.2)
            }
        }
    }

    class NodeTreeItem(node: Node) : TreeItem<Node>(node) {
        init {
            value.properties["tornadofx.layoutdebugger.treeitem"] = this
        }
    }

    /**
     * Property editor for node. We can't bind directly to these properties as
     * they might have app bindings already, so we have to jump through some hoops
     * to synchronize values between this editor and the Node properties.
     */
    inner class NodePropertyView(node: Node) : Form() {
        init {
            fieldset("Node info") {
                field("ClassName") {
                    text(node.javaClass.name)
                }
                field("StyleClass") {
                    text(node.styleClass.map { ".$it" }.joinToString(", "))
                }
            }
            fieldset("Dimensions") {
                field("Bounds in parent") {
                    label("${node.boundsInParent.width} x ${node.boundsInParent.height}")
                }
                field("Bounds in local") {
                    label("${node.boundsInLocal.width} x ${node.boundsInLocal.height}")
                }
            }
            fieldset("Properties") {
                if (node is Labeled) {
                    field("Text") {
                        textfield() {
                            if (node.textProperty().isBound) {
                                textProperty().bind(node.textProperty())
                                isEditable = false
                                tooltip = Tooltip("The value is bound in the app, mounted read only")
                            } else {
                                textProperty().bindBidirectional(object : SimpleObjectProperty<String>(node.text) {
                                    override fun set(newValue: String?) {
                                        super.set(newValue)
                                        node.text = newValue
                                    }
                                })
                            }

                            prefColumnCount = 30
                        }
                    }
                }
                if (node is TextInputControl) {
                    field("Text") {
                        textfield(node.text) {
                            prefColumnCount = 30
                            textProperty().addListener { observableValue, oldValue, newValue ->
                                node.text = newValue
                            }
                        }
                    }
                }
                if (node is Region) {
                    // Background/fills is immutable, so a new background object must be created with the augmented fill
                    if (node.background?.fills?.isNotEmpty() ?: false) {
                        field("Background fill") {
                            node.background.fills.forEachIndexed { i, backgroundFill ->
                                val initialColor: Color? = if (backgroundFill.fill is Color) backgroundFill.fill as Color else null
                                colorpicker(initialColor) {
                                    isEditable = true
                                    valueProperty().addListener { observableValue, oldColor, newColor ->
                                        val newFills = node.background.fills.mapIndexed { ix, fill ->
                                            if (ix == i) BackgroundFill(newColor, fill.radii, fill.insets)
                                            else fill
                                        }
                                        node.background = Background(newFills, node.background.images)
                                    }
                                }
                            }
                        }
                    } else {
                        // Add one background color if none present
                        field("Background fill") {
                            colorpicker() {
                                isEditable = true
                                valueProperty().addListener { observableValue, oldColor, newColor ->
                                    node.background = Background(BackgroundFill(newColor, null, null))
                                }
                            }
                        }
                    }

                    // Padding
                    field("Padding") {
                        textfield() {
                            textProperty().bindBidirectional(object : SimpleObjectProperty<Insets>(node.padding) {
                                override fun set(newValue: Insets?) {
                                    super.set(newValue)
                                    node.padding = newValue
                                }
                            }, InsetsConverter())
                            prefColumnCount = 20
                        }
                    }

                    field("Pref width") {
                        textfield() {
                            textProperty().bindBidirectional(object : SimpleObjectProperty<Double>(node.prefWidth) {
                                override fun set(newValue: Double) {
                                    super.set(newValue)
                                    node.prefWidth = newValue
                                }
                            }, DoubleStringConverter())
                            prefColumnCount = 20
                        }
                    }

                    field("Pref height") {
                        textfield() {
                            textProperty().bindBidirectional(object : SimpleObjectProperty<Double>(node.prefHeight) {
                                override fun set(newValue: Double) {
                                    super.set(newValue)
                                    node.prefHeight = newValue
                                }
                            }, DoubleStringConverter())
                            prefColumnCount = 20
                        }
                    }



                }
            }

        }
    }

    class InsetsConverter : StringConverter<Insets>() {
        override fun toString(v: Insets?) = if (v == null) "" else "${v.top.s()} ${v.right.s()} ${v.bottom.s()} ${v.left.s()}"
        override fun fromString(s: String?) : Insets {
            try {
                if (s == null || s.isEmpty()) return Insets.EMPTY
                val parts = s.split(" ")
                if (parts.size == 1) return Insets(s.toDouble())
                if (parts.size == 2) return Insets(parts[0].toDouble(), parts[1].toDouble(), parts[0].toDouble(), parts[1].toDouble())
                if (parts.size == 4) return Insets(parts[0].toDouble(), parts[1].toDouble(), parts[2].toDouble(), parts[3].toDouble())
            } catch (ignored: Exception) {
            }
            return Insets.EMPTY
        }

        private fun Double.s(): String {
            if (this == 0.0) return "0"
            val s = toString()
            if (s.endsWith(".0")) return s.substringBefore(".")
            return s
        }
    }


}