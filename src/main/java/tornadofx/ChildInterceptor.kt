package tornadofx

import javafx.event.EventTarget
import javafx.scene.Node
/**
 * An interceptor that can veto or provide another mechanism for adding children to their parent.
 *
 * All interceptors called for all builders right before the default mechanism adds the newly created
 * node to the parent container. Returning false means that the default will be used. Returning true means
 * that you added the child yourself and that the default mechanism should do nothing.
 *
 * This is useful for layout containers that need special handling when adding child nodes.
 *
 * Example: MigPane needs to add a default ComponentConstraint object to be able to manipulate the
 * constraint after the child is added.
 * ```
 * class MigPaneChildInterceptor: ChildInterceptor{
 *      override fun invoke(parent: EventTarget, node: Node, index: Int?): Boolean{
 *          return when(parent){
 *              is MigPane ->  {parent.add(node, CC()); true}
 *              else -> false
 *          }
 *      }
 * }
 * ```
 * @sample tornadofx.tests.FirstInterceptor
 * @sample tornadofx.tests.SecondInterceptor
 *
 */
interface ChildInterceptor {
    operator fun invoke(parent: EventTarget, node: Node, index: Int?):Boolean
}