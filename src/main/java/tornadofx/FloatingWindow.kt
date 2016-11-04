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
    lateinit var window: BorderPane
    lateinit var coverNode: Node
    var offsetX = 0.0
    var offsetY = 0.0

    class Styles : Stylesheet() {
        companion object {
            val floatingWindowWrapper by cssclass()
            val window by cssclass()
            val top by cssclass()
            val closebutton by cssclass()
        }

        init {
            floatingWindowWrapper {
                minWidth = 200.px
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

    init {
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
            window.top.layoutX += mouseEvent.x - x
            window.top.layoutY += mouseEvent.y - y
        }
    }

}
