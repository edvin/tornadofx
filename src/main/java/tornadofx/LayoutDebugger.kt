package tornadofx

import javafx.application.Platform
import javafx.beans.property.DoubleProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.StringProperty
import javafx.event.EventHandler
import javafx.geometry.Bounds
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.control.*
import javafx.scene.input.MouseEvent
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Shape
import javafx.stage.Modality
import javafx.util.StringConverter
import javafx.util.converter.DoubleStringConverter

@Suppress("UNCHECKED_CAST")
class LayoutDebugger : Fragment() {
    override val root = BorderPane()

    lateinit var debuggingScene: Scene
    private lateinit var debuggingRoot: Parent

    val hoveredNode = SimpleObjectProperty<Node>()
    val selectedNode = SimpleObjectProperty<Node>()
    var nodeTree: TreeView<NodeContainer> by singleAssign()
    val propertyContainer = ScrollPane()
    val stackpane = StackPane()
    val overlay = Canvas()
    val gc = overlay.graphicsContext2D

    init {
        overlay.isMouseTransparent = true
        stackpane += overlay
        gc.fill = c("#99bbbb", 0.4)

        with(root) {
            setPrefSize(800.0, 600.0)

            center {
                splitpane {
                    setDividerPosition(0, 0.3)
                    treeview<NodeContainer> {
                        nodeTree = this
                    }
                    this += propertyContainer.apply {
                        padding = Insets(10.0)
                    }
                }
            }
        }

        hookListeners()
    }

    private fun hookListeners() {
        nodeTree.selectionModel.selectedItemProperty().onChange {
            if (it?.value != null) setSelectedNode(it!!.value.node)
        }

        // Position overlay over hovered node
        hoveredNode.addListener { observableValue, oldNode, newNode ->
            if (oldNode != newNode && newNode != null)
                positionOverlayOver(newNode)
            else if (newNode == null && oldNode != null)
                clearOverlay()
        }
    }

    private fun positionOverlayOver(node: Node) {
        val p = node.localToScene(node.boundsInLocal)
        clearOverlay()
        gc.fillRect(p.minX, p.minY, p.width, p.height)
    }

    private fun clearOverlay() {
        gc.clearRect(0.0, 0.0, overlay.width, overlay.height)
    }

    val hoverHandler = EventHandler<MouseEvent> { event ->
        val newHover = event.target
        if (newHover is Node && hoveredNode.value != newHover)
            hoveredNode.value = newHover
    }

    val sceneExitedHandler = EventHandler<MouseEvent> { event ->
        hoveredNode.value = null
    }

    val clickHandler = EventHandler<MouseEvent> { event ->
        val clickedTarget = event.target
        if (clickedTarget is Node) setSelectedNode(clickedTarget)
        event.consume()
    }

    private fun setSelectedNode(node: Node) {
        selectedNode.value = node
        val treeItem = selectedNode.value.properties["tornadofx.layoutdebugger.treeitem"]
        if (treeItem is NodeTreeItem) {
            nodeTree.selectionModel.select(treeItem)
            propertyContainer.nodePropertyView(treeItem.value.node)
        }
    }

    override fun onDock() {
        // Prevent the debugger from being reloaded
        Platform.runLater {
            modalStage!!.scene.properties["tornadofx.layoutdebugger"] = this
        }

        title = "Layout Debugger [%s]".format(debuggingScene)

        with(debuggingScene) {
            // Prevent the scene from being reloaded while the debugger is running
            properties["tornadofx.layoutdebugger"] = this@LayoutDebugger
            addEventFilter(MouseEvent.MOUSE_EXITED, sceneExitedHandler)
            addEventFilter(MouseEvent.MOUSE_MOVED, hoverHandler)
            addEventFilter(MouseEvent.MOUSE_CLICKED, clickHandler)
        }

        // Overlay has same size as scene
        overlay.widthProperty().cleanBind(debuggingScene.widthProperty())
        overlay.heightProperty().cleanBind(debuggingScene.heightProperty())

        // Stackpane becomes the new scene root and contains the currentScene.root and our overlay
        debuggingRoot = debuggingScene.root
        stackpane += debuggingRoot
        stackpane += debuggingRoot
        debuggingScene.root = stackpane
        overlay.toFront()

        // Populate the node tree
        with(nodeTree) {
            root = NodeTreeItem(NodeContainer(debuggingRoot))
            populate(
                    itemFactory = { NodeTreeItem(it) },
                    childFactory = { it.value.node.getChildList()?.map(::NodeContainer) }
            )

            // Hover on a node in the tree should visualize in the scene graph
            setCellFactory {
                object : TreeCell<NodeContainer>() {
                    init {
                        addEventFilter(MouseEvent.MOUSE_ENTERED) {
                            item?.apply {
                                hoveredNode.value = node
                                style {
                                    backgroundColor += gc.fill
                                }
                            }
                        }
                        addEventFilter(MouseEvent.MOUSE_EXITED) {
                            hoveredNode.value = null
                            style {
                                backgroundColor = multi()
                            }
                        }
                    }

                    override fun updateItem(item: NodeContainer?, empty: Boolean) {
                        super.updateItem(item, empty)

                        graphic = null
                        text = null

                        if (!empty && item != null)
                            text = item.node.javaClass.name.substringAfterLast("\\$").substringAfterLast(".")
                    }
                }
            }
        }
    }

    override fun onUndock() {
        debuggingRoot.removeFromParent()
        debuggingScene.root = debuggingRoot
        debuggingScene.properties["tornadofx.layoutdebugger"] = null
        debuggingScene.removeEventFilter(MouseEvent.MOUSE_MOVED, hoverHandler)
        debuggingScene.removeEventFilter(MouseEvent.MOUSE_CLICKED, clickHandler)
    }

    companion object {
        fun debug(scene: Scene) = with(find<LayoutDebugger>()) {
            debuggingScene = scene
            openModal(modality = Modality.NONE)
        }
    }

    class NodeContainer(val node: Node)
    inner class NodeTreeItem(container: NodeContainer) : TreeItem<NodeContainer>(container) {
        init {
            container.node.properties["tornadofx.layoutdebugger.treeitem"] = this
        }
    }

    /**
     * Property editor for node. We can't bind directly to these properties as
     * they might have app bindings already, so we have to jump through some hoops
     * to synchronize values between this editor and the Node properties.
     */
    fun ScrollPane.nodePropertyView(node: Node) = form {
        fieldset("Node info") {
            field("ClassName") {
                text(node.javaClass.name)
            }
            field("StyleClass") {
                text(node.styleClass.joinToString(", ") { ".$it" })
            }
        }
        fieldset("Dimensions") {
            fun Bounds.describe() = "(${minX.toInt()}, ${width.toInt()}), (${minY.toInt()}, ${height.toInt()})"
            field("Layout bounds") {
                label(node.layoutBounds.describe())
            }
            field("Bounds in parent") {
                label(node.boundsInParent.describe())
            }
            field("Bounds in local") {
                label(node.boundsInLocal.describe())
            }
        }
        fieldset("Properties") {
            if (node is Labeled) {
                field("Text") {
                    textfield() {
                        textProperty().shadowBindTo(node.textProperty())
                        prefColumnCount = 30
                    }
                }
            }
            if (node is TextInputControl) {
                field("Text") {
                    textfield(node.text) {
                        prefColumnCount = 30
                        textProperty().shadowBindTo(node.textProperty())
                    }
                }
            }
            if (node is Shape) {
                field("Fill") {
                    colorpicker(node.fill as? Color) {
                        valueProperty().onChange { newValue ->
                            node.fillProperty().unbind()
                            node.fill = newValue
                        }
                    }
                }
            }
            if (node.parent is HBox) {
                field("HBox Grow") {
                    combobox<Priority> {
                        items = listOf(Priority.SOMETIMES, Priority.ALWAYS, Priority.NEVER).observable()
                        value = HBox.getHgrow(node) ?: Priority.NEVER
                        valueProperty().onChange {
                            HBox.setHgrow(node, it)
                        }
                    }
                }
                field("HBox Margin") {
                    textfield(InsetsConverter().toString(HBox.getMargin(node))) {
                        textProperty().onChange {
                            HBox.setMargin(node, InsetsConverter().fromString(it))
                        }
                        prefColumnCount = 10
                    }
                }
            }
            if (node.parent is VBox) {
                field("VBox Grow") {
                    combobox<Priority> {
                        items = listOf(Priority.SOMETIMES, Priority.ALWAYS, Priority.NEVER).observable()
                        value = VBox.getVgrow(node) ?: Priority.NEVER
                        valueProperty().onChange {
                            VBox.setVgrow(node, it)
                        }
                    }
                }
                field("VBox Margin") {
                    textfield(InsetsConverter().toString(VBox.getMargin(node))) {
                        textProperty().onChange {
                            VBox.setMargin(node, InsetsConverter().fromString(it))
                        }
                        prefColumnCount = 10
                    }
                }
            }
            if (node is HBox) {
                alignmentCombo(node.alignmentProperty())
                spacingEditor(node.spacingProperty())
            }
            if (node is VBox) {
                alignmentCombo(node.alignmentProperty())
                spacingEditor(node.spacingProperty())
            }
            if (node is StackPane) alignmentCombo(node.alignmentProperty())
            if (node is FlowPane) alignmentCombo(node.alignmentProperty())
            if (node is TitledPane) alignmentCombo(node.alignmentProperty())
            if (node is GridPane) alignmentCombo(node.alignmentProperty())

            if (node is Region) {
                // Background/fills is immutable, so a new background object must be created with the augmented fill
                if (node.background?.fills?.isNotEmpty() ?: false) {
                    field("Background fill") {
                        vbox(3.0) {
                            node.background.fills.forEachIndexed { i, backgroundFill ->
                                val initialColor: Color? = backgroundFill.fill as? Color
                                colorpicker(initialColor) {
                                    isEditable = true
                                    valueProperty().onChange { newColor ->
                                        val newFills = node.background.fills.mapIndexed { ix, fill ->
                                            if (ix == i) BackgroundFill(newColor, fill.radii, fill.insets)
                                            else fill
                                        }
                                        node.backgroundProperty().unbind()
                                        node.background = Background(newFills, node.background.images)
                                    }
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
                        textProperty().shadowBindTo(node.paddingProperty(), InsetsConverter())
                        prefColumnCount = 10
                    }
                }

                field("Pref/min/max width") {
                    textfield() {
                        textProperty().shadowBindTo(node.prefWidthProperty(), DoubleStringConverter())
                        prefColumnCount = 10
                    }
                    textfield() {
                        textProperty().shadowBindTo(node.minWidthProperty(), DoubleStringConverter())
                        prefColumnCount = 10
                    }
                    textfield() {
                        textProperty().shadowBindTo(node.maxWidthProperty(), DoubleStringConverter())
                        prefColumnCount = 10
                    }
                }

                field("Pref/min/max height") {
                    textfield {
                        textProperty().shadowBindTo(node.prefHeightProperty(), DoubleStringConverter())
                        prefColumnCount = 10
                    }
                    textfield {
                        textProperty().shadowBindTo(node.minHeightProperty(), DoubleStringConverter())
                        prefColumnCount = 10
                    }
                    textfield {
                        textProperty().shadowBindTo(node.maxHeightProperty(), DoubleStringConverter())
                        prefColumnCount = 10
                    }
                }

            }
        }
    }

    private fun Fieldset.spacingEditor(spacingProperty: DoubleProperty) {
        field("Spacing") {
            textfield {
                textProperty().shadowBindTo(spacingProperty, DoubleStringConverter())
                prefColumnCount = 10
            }
        }
    }

    fun Fieldset.alignmentCombo(property: Property<Pos>) {
        field("Alignment") {
            combobox<Pos> {
                items = listOf(Pos.TOP_LEFT, Pos.TOP_CENTER, Pos.TOP_RIGHT, Pos.CENTER_LEFT, Pos.CENTER, Pos.CENTER_RIGHT, Pos.BOTTOM_LEFT,
                        Pos.BOTTOM_CENTER, Pos.BOTTOM_RIGHT, Pos.BASELINE_LEFT, Pos.BASELINE_CENTER, Pos.BASELINE_RIGHT).observable()
                valueProperty().shadowBindTo(property)
            }
        }
    }

    class InsetsConverter : StringConverter<Insets>() {
        override fun toString(v: Insets?) = if (v == null) "" else "${v.top.s()} ${v.right.s()} ${v.bottom.s()} ${v.left.s()}"
        override fun fromString(s: String?): Insets {
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


    fun <T> Property<T>.shadowBindTo(nodeProperty: Property<T>) {
        bindBidirectional(object : SimpleObjectProperty<T>(nodeProperty.value) {
            override fun set(newValue: T?) {
                super.set(newValue)
                nodeProperty.unbind()
                nodeProperty.value = newValue
            }
        })
    }

    inline fun <reified T, reified C : StringConverter<out T>> StringProperty.shadowBindTo(nodeProperty: Property<T>, converter: C) {
        bindBidirectional(object : SimpleObjectProperty<T>(nodeProperty.value) {
            override fun set(newValue: T?) {
                super.set(newValue)
                nodeProperty.unbind()
                nodeProperty.value = newValue
            }
        }, converter as StringConverter<T>)
    }

}