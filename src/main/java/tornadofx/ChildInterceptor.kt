package tornadofx

import javafx.event.EventTarget
import javafx.scene.Node

interface ChildInterceptor {
    operator fun invoke(parent: EventTarget, node: Node, index: Int?):Boolean
}