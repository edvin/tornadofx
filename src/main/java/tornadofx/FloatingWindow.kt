package tornadofx

import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.canvas.Canvas
import javafx.scene.layout.BorderPane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color

class FloatingWindow(val view: UIComponent) : StackPane() {
    lateinit var window: BorderPane
    lateinit var coverNode: Node
    var offsetX = 0.0
    var offsetY = 0.0

    init {
        addClass("floating-window-wrapper")

        canvas {
            graphicsContext2D.fill = c("#000", 0.4)
            widthProperty().bind(this@FloatingWindow.widthProperty())
            heightProperty().bind(this@FloatingWindow.heightProperty())
            widthProperty().onChange { fillOverlay() }
            heightProperty().onChange { fillOverlay() }
        }

        borderpane {
            addClass("floating-window")
            window = this
            top {
                addClass("floating-window-top")
                hbox(5.0) {
                    style {
                        backgroundColor += Color.WHITE
                        padding = box(2.px)
                        alignment = Pos.CENTER
                    }
                    label(view.titleProperty) {
                        isMouseTransparent = true
                    }
                    spacer() {
                        isMouseTransparent = true
                    }
                    button("X") {
                        style {
                            backgroundInsets += box(0.px)
                            backgroundColor += Color.TRANSPARENT
                        }
                        setOnAction {
                            close()
                        }
                    }
                }
            }
        }
        moveWindowOnDrag()
    }

    private fun Canvas.fillOverlay() {
        graphicsContext2D.apply {
            clearRect(0.0, 0.0, width, height)
            fillRect(0.0, 0.0, width, height)
        }
    }

    fun openOver(coverNode: Node) {
        this.coverNode = coverNode
        val coverParent = coverNode.parent
        if (coverParent != null) {
            val indexInCoverParent = coverParent.getChildList()!!.indexOf(coverNode)
            coverNode.removeFromParent()
            coverParent.getChildList()!!.add(indexInCoverParent, this)
        } else {
            val scene = coverNode.scene
            scene.root = this
        }
        window.center = stackpane {
            this += view
            addClass("floating-window-content")
            style {
                backgroundColor += Color.WHITE
                padding = box(5.px)
            }
        }
        children.add(0, coverNode)
    }

    fun close() {
        coverNode.removeFromParent()
        removeFromParent()
        if (scene != null) {
            scene.root = coverNode as Parent
        }
    }

    override fun layoutChildren() {
        val prefHeight = window.prefHeight(width)
        val prefWidth = window.prefWidth(height)
        val x = (width - prefWidth) / 2
        val y = (height - prefHeight) / 2

        coverNode.resizeRelocate(0.0, 0.0, width, height)
        window.resizeRelocate(Math.max(0.0, x + offsetX), Math.max(0.0, y + offsetY), prefWidth, prefHeight)
    }

    private fun moveWindowOnDrag() {
        var x = 0.0
        var y = 0.0

        window.top.setOnMousePressed { mouseEvent ->
            x = mouseEvent.x
            y = mouseEvent.y
        }

        window.top.setOnMouseDragged { mouseEvent ->
            offsetX += mouseEvent.x - x
            offsetY += mouseEvent.y - y
            requestLayout()
        }
    }

}
