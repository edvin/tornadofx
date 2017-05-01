package tornadofx.testapps

import tornadofx.*

class SwitchViewApp : App(ContainerView::class)

class ContainerView : View("ContainerView") {
    val subView1: SubView1 by inject()
    val subView2: SubView2 by inject()

    override val root = borderpane {
        top {
            hbox(10) {
                button("Switch view").action {
                    if (center.lookup("#view1") != null)
                        subView1.replaceWith(subView2, ViewTransition.Slide(0.2.seconds))
                    else
                        subView2.replaceWith(subView1, ViewTransition.Slide(0.2.seconds, ViewTransition.Direction.RIGHT))
                }
                button("Fire event").action { fire(MySwitchViewEvent) }
            }
        }
        center {
            add(subView1)
        }
    }
}

class SubView1 : View("SubView1") {
    override val root = hbox {
        id = "view1"
        label("I'm subview 1")
    }
    init {
        subscribe<MySwitchViewEvent> {
            println("SubView1 received event")
        }
    }
}

class SubView2 : View("SubView2") {
    override val root = hbox {
        label("I'm subview 2")
    }
    init {
        subscribe<MySwitchViewEvent> {
            println("SubView2 received event")
        }
    }
}

object MySwitchViewEvent : FXEvent()