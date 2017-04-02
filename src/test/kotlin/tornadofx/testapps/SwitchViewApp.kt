package tornadofx.testapps

import javafx.scene.control.Alert.AlertType.ERROR
import tornadofx.*

class GotoViewApp : App(GotoContainerView::class)

class GotoContainerView : View("ContainerView") {
    val gotoSubView1: GotoSubView1 by inject()
    val gotoSubView2: GotoSubView2 by inject()

    override val root = borderpane {
        top {
            button("Goto view").action {
                if (center.lookup("#view1") != null)
                    gotoSubView1.goto(gotoSubView2)
                else
                    gotoSubView2.goto(gotoSubView1)
            }
        }
        center {
            add(gotoSubView1)
        }
    }
}

class GotoSubView1 : View("GotoSubView2") {
    override val root = hbox {
        id = "view1"
        label("I'm subview 1")
    }

    override fun onGoto(source: UIComponent) {
        when (source) {
            is GotoSubView2 -> if (source.isDocked) source.replaceWith(this) else openModal()
            is GotoSubView1 -> source.replaceWith(this)
            else -> alert(ERROR, "I refuse!", "Not going to happen")
        }
    }
}

class GotoSubView2 : View("GotoSubView2") {
    override val root = hbox {
        label("I'm subview 2")
    }
}
