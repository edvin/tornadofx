package tornadofx.testapps

import javafx.geometry.Insets
import tornadofx.*

class SwitchViewApp : App(ContainerView::class)

class ContainerView : View("ContainerView") {
    val subView1: SubView1 by inject()
    val subView2: SubView2 by inject()

    override val root = borderpane {
        top {
            button("Switch view").setOnAction {
                if (center.lookup("#view1") != null)
                    subView1.replaceWith(subView2, ViewTransition.Slide(0.2.seconds))
                else
                    subView2.replaceWith(subView1, ViewTransition.Slide(0.2.seconds, ViewTransition.Direction.RIGHT))
            }
        }
        center {
            this += subView1
        }
    }
}

class SubView1 : View("SubView2") {
    override val root = hbox {
        id = "view1"
        label("I'm subview 1")
    }
}

class SubView2 : View("SubView2") {
    override val root = hbox {
        label("I'm subview 2")
    }
}
