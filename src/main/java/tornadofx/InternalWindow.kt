package tornadofx

import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.canvas.Canvas
import javafx.scene.effect.DropShadow
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import tornadofx.InternalWindow.Styles.Companion.crossPath
import java.net.URL

class InternalWindow(icon: Node?, modal: Boolean, escapeClosesWindow: Boolean, closeButton: Boolean, overlayPaint : Paint = c("#000", 0.4)) : StackPane() {
    private lateinit var window: BorderPane
    private lateinit var coverNode: Node
    private lateinit var view: UIComponent
    private var titleProperty = SimpleStringProperty()
    var overlay: Canvas? = null
    private var indexInCoverParent: Int? = null
    private var coverParent: Parent? = null
    private var offsetX = 0.0
    private var offsetY = 0.0

    init {
        if (escapeClosesWindow) {
            addEventFilter(KeyEvent.KEY_PRESSED) {
                if (it.code == KeyCode.ESCAPE)
                    close()
            }
        }

        addClass(Styles.floatingWindowWrapper)

        if (modal) {
            canvas {
                overlay = this
                graphicsContext2D.fill = overlayPaint
                widthProperty().bind(this@InternalWindow.widthProperty())
                heightProperty().bind(this@InternalWindow.heightProperty())
                widthProperty().onChange { fillOverlay() }
                heightProperty().onChange { fillOverlay() }
            }
        }

        borderpane {
            addClass(Styles.window)
            window = this
            top {
                hbox(5.0) {
                    addClass(Styles.top)
                    label(titleProperty) {
                        graphic = icon
                        isMouseTransparent = true
                    }
                    spacer {
                        isMouseTransparent = true
                    }
                    if (closeButton) {
                        button {
                            setOnMouseClicked {
                                close()
                            }
                            graphic = svgpath(crossPath) {
                                addClass(Styles.closebutton)
                            }
                        }
                    }
                }
            }
        }
        moveWindowOnDrag()

        window.center = stackpane {
            addClass(Styles.floatingWindowContent)
        }
    }

    class Styles : Stylesheet() {
        companion object {
            val floatingWindowWrapper by cssclass()
            val floatingWindowContent by cssclass()
            val window by cssclass()
            val top by cssclass()
            val closebutton by cssclass()
            val crossPath = "M7.48 8l3.75 3.75-1.48 1.48L6 9.48l-3.75 3.75-1.48-1.48L4.52 8 .77 4.25l1.48-1.48L6 6.52l3.75-3.75 1.48 1.48z"
        }

        init {
            floatingWindowWrapper {
                window {
                    effect = DropShadow()
                }

                top {
                    backgroundColor += Color.WHITE
                    padding = box(1.px, 1.px, 2.px, 5.px)
                    borderColor += box(Color.TRANSPARENT, Color.TRANSPARENT, Color.GRAY, Color.TRANSPARENT)
                    borderWidth += box(0.2.px)

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
                    }
                }

                floatingWindowContent {
                    backgroundColor += c(244, 244, 244)
                    padding = box(5.px)
                }
            }
        }
    }

    override fun getUserAgentStylesheet() = URL("css://${Styles::class.java.name}").toExternalForm()!!

    fun fillOverlay() {
        overlay?.graphicsContext2D.apply {
            val lb = coverNode.layoutBounds
            this?.clearRect(0.0, 0.0, lb.width, lb.height)
            this?.fillRect(0.0, 0.0, lb.width, lb.height)
        }
    }

    fun open(view: UIComponent, owner: Node) {
        if (owner.parent is InternalWindow) return
        this.view = view
        this.coverNode = owner
        this.coverParent = owner.parent
        this.titleProperty.bind(view.titleProperty)

        coverNode.uiComponent<UIComponent>()?.muteDocking = true

        if (coverParent != null) {
            indexInCoverParent = coverParent!!.getChildList()!!.indexOf(owner)
            owner.removeFromParent()
            coverParent!!.getChildList()!!.add(indexInCoverParent!!, this)
        } else {
            val scene = owner.scene
            scene.root = this
        }

        coverNode.uiComponent<UIComponent>()?.muteDocking = false

        (window.center as Parent) += view

        children.add(0, owner)
        fillOverlay()
        view.callOnDock()
    }

    fun close() {
        coverNode.uiComponent<UIComponent>()?.muteDocking = true

        coverNode.removeFromParent()
        removeFromParent()
        if (indexInCoverParent != null) {
            coverParent!!.getChildList()!!.add(indexInCoverParent!!, coverNode)
        } else {
            scene?.root = coverNode as Parent?
        }
        coverNode.uiComponent<UIComponent>()?.muteDocking = false

        view.callOnUndock()
    }

    override fun layoutChildren() {
        val lb = coverNode.layoutBounds
        val prefHeight = window.prefHeight(lb.width)
        val prefWidth = window.prefWidth(lb.height)
        val x = (lb.width - prefWidth) / 2
        val y = (lb.height - prefHeight) / 2

        coverNode.resizeRelocate(0.0, 0.0, lb.width, lb.height)

        if (offsetX != 0.0 || offsetY != 0.0) {
            val windowX = x + offsetX
            val windowY = y + offsetY

            window.resizeRelocate(windowX, windowY, window.width, window.height)
        } else {
            val windowWidth = Math.min(prefWidth, lb.width)
            val windowHeight = Math.min(prefHeight, lb.height)

            window.resizeRelocate(Math.max(0.0, x), Math.max(0.0, y), windowWidth, windowHeight)
        }
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
