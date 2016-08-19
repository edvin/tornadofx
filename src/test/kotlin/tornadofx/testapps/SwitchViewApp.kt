package tornadofx.testapps

import tornadofx.*

class SwitchViewApp : App(ContainerView::class)

class ContainerView : View("ContainerView") {
    val subView1: SubView1 by inject()

    override val root = borderpane {
        top {
            button("Switch center").setOnAction {
                subView1.replaceWith(SubView2::class, ViewTransition.SlideIn)
            }
        }
        center {
            this += subView1
        }
    }
}

class SubView1 : View("SubView2") {
    override val root = hbox { label("I'm subview 1") }
}

class SubView2 : View("SubView2") {
    override val root = hbox { label("I'm subview 2") }
}