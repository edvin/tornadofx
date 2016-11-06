package tornadofx

import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.canvas.Canvas
import javafx.scene.layout.BorderPane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import java.net.URL

class FloatingWindow(val view: UIComponent) : StackPane() {
    private lateinit var window: BorderPane
    private lateinit var coverNode: Node
    private var indexInCoverParent: Int? = null
    private var coverParent: Parent? = null
    private var offsetX = 0.0
    private var offsetY = 0.0

    class Styles : Stylesheet() {
        companion object {
            val floatingWindowWrapper by cssclass()
            val window by cssclass()
            val top by cssclass()
            val closebutton by cssclass()
        }

        init {
            floatingWindowWrapper {
                top {
                    backgroundColor += Color.WHITE
                    padding = box(0.px, 0.px, 0.px, 5.px)
                    alignment = Pos.CENTER

                    button {
                        padding = box(4.px, 12.px)
                        backgroundRadius += box(0.px)
                        backgroundColor += Color.WHITE
                        and(hover) {
                            backgroundColor += Color.RED
                            star {
                                fill = Color.WHITE
                            }
                        }
                        star {
                            shape = "M7.48 8l3.75 3.75-1.48 1.48L6 9.48l-3.75 3.75-1.48-1.48L4.52 8 .77 4.25l1.48-1.48L6 6.52l3.75-3.75 1.48 1.48z"
                        }
                    }
                }
            }
        }
    }

    override fun getUserAgentStylesheet() = URL("css://${Styles::class.java.name}").toExternalForm()!!

    private fun doInit() {
        addClass(Styles.floatingWindowWrapper)

        canvas {
            graphicsContext2D.fill = c("#000", 0.4)
            widthProperty().bind(this@FloatingWindow.widthProperty())
            heightProperty().bind(this@FloatingWindow.heightProperty())
            widthProperty().onChange { fillOverlay() }
            heightProperty().onChange { fillOverlay() }
        }

        borderpane {
            addClass(Styles.window)
            window = this
            top {
                hbox(5.0) {
                    addClass(Styles.top)
                    label(view.titleProperty) {
                        isMouseTransparent = true
                    }
                    spacer() {
                        isMouseTransparent = true
                    }
                    button {
                        setOnMouseClicked {
                            close()
                        }
                        graphic = svgpath("M7.48 8l3.75 3.75-1.48 1.48L6 9.48l-3.75 3.75-1.48-1.48L4.52 8 .77 4.25l1.48-1.48L6 6.52l3.75-3.75 1.48 1.48z") {
                            addClass(Styles.closebutton)
                        }
                    }
                }
            }
        }
        moveWindowOnDrag()
    }

    private fun Canvas.fillOverlay() {
        graphicsContext2D.apply {
            val lb = coverNode.layoutBounds
            clearRect(0.0, 0.0, lb.width, lb.height)
            fillRect(0.0, 0.0, lb.width, lb.height)
        }
    }

    fun openOver(coverNode: Node) {
        this.coverNode = coverNode
        coverParent = coverNode.parent
        if (coverParent != null) {
            indexInCoverParent = coverParent!!.getChildList()!!.indexOf(coverNode)
            coverNode.removeFromParent()
            coverParent!!.getChildList()!!.add(indexInCoverParent!!, this)
        } else {
            val scene = coverNode.scene
            scene.root = this
        }
        doInit()
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
        if (indexInCoverParent != null) {
            coverParent!!.getChildList()!!.add(indexInCoverParent!!, coverNode)
        } else {
            scene.root = coverNode as Parent
        }
    }

    override fun layoutChildren() {
        val lb = coverNode.layoutBounds
        val prefHeight = window.prefHeight(lb.width)
        val prefWidth = window.prefWidth(lb.height)
        val x = (lb.width - prefWidth) / 2
        val y = (lb.height - prefHeight) / 2

        coverNode.resizeRelocate(0.0, 0.0, lb.width, lb.height)
        window.resizeRelocate(Math.max(0.0, x + offsetX), Math.max(0.0, y + offsetY), Math.min(prefWidth, lb.width), Math.min(prefHeight, lb.height))
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
            window.top.layoutX += mouseEvent.x - x
            window.top.layoutY += mouseEvent.y - y
        }
    }

}
