package tornadofx

import javafx.animation.*
import javafx.beans.value.WritableValue
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Point2D
import javafx.geometry.Point3D
import javafx.scene.Node
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.Shape
import javafx.scene.transform.Rotate
import javafx.util.Duration

operator fun Timeline.plusAssign(keyFrame: KeyFrame) {
    keyFrames.add(keyFrame)
}

operator fun KeyFrame.plusAssign(keyValue: KeyValue) {
    values.add(keyValue)
}

fun SequentialTransition.timeline(op: (Timeline).() -> Unit) = timeline(false, op).also { children += it }

fun ParallelTransition.timeline(op: (Timeline).() -> Unit) = timeline(false, op).also { children += it }

fun sequentialTransition(play: Boolean = true, op: (SequentialTransition.() -> Unit)) = SequentialTransition().apply {
    op(this)
    if (play) play()
}

fun parallelTransition(play: Boolean = true, op: (ParallelTransition.() -> Unit)) = ParallelTransition().apply {
    op(this)
    if (play) play()
}

fun timeline(play: Boolean = true, op: (Timeline).() -> Unit) = Timeline().apply {
    op()
    if (play) play()
}

/**
 * A convenience function for creating a [TranslateTransition] on a [UIComponent].
 *
 * @param time How long the animation will take
 * @param destination Where to move the component (relative to its translation origin)
 * @param easing How to interpolate the motion
 * @param reversed Whether the animation should be played in reverse
 * @param play Whether the animation should start playing automatically
 * @param op Modify the animation after it is created
 * @return A TranslateTransition on this component
 */
fun UIComponent.move(
        time: Duration, destination: Point2D,
        easing: Interpolator = Interpolator.EASE_BOTH,
        reversed: Boolean = false,
        play: Boolean = true,
        op: TranslateTransition.() -> Unit = {}
) = root.move(time, destination, easing, reversed, play, op)

/**
 * A convenience function for creating a [TranslateTransition] on a [Node].
 *
 * @param time How long the animation will take
 * @param destination Where to move the node (relative to its translation origin)
 * @param easing How to interpolate the animation
 * @param reversed Whether the animation should be played in reverse
 * @param play Whether the animation should start playing automatically
 * @param op Modify the animation after it is created
 * @return A TranslateTransition on this node
 */
fun Node.move(time: Duration, destination: Point2D,
              easing: Interpolator = Interpolator.EASE_BOTH, reversed: Boolean = false, play: Boolean = true,
              op: TranslateTransition.() -> Unit = {}): TranslateTransition {
    val target: Point2D
    if (reversed) {
        target = point(translateX, translateY)
        translateX = destination.x
        translateY = destination.y
    } else {
        target = destination
    }
    return TranslateTransition(time, this).apply {
        interpolator = easing
        op(this)
        toX = target.x
        toY = target.y
        if (play) play()
    }
}

/**
 * A convenience function for creating a [RotateTransition] on a [UIComponent].
 *
 * @param time How long the animation will take
 * @param angle How far to rotate the component (in degrees; relative to its 0 rotation)
 * @param easing How to interpolate the animation
 * @param reversed Whether the animation should be played in reverse
 * @param play Whether the animation should start playing automatically
 * @param op Modify the animation after it is created
 * @return A RotateTransition on this component
 */
fun UIComponent.rotate(
        time: Duration,
        angle: Number,
        easing: Interpolator = Interpolator.EASE_BOTH,
        reversed: Boolean = false,
        play: Boolean = true,
        op: RotateTransition.() -> Unit = {}
) = root.rotate(time, angle, easing, reversed, play, op)

/**
 * A convenience function for creating a [RotateTransition] on a [Node].
 *
 * @param time How long the animation will take
 * @param angle How far to rotate the node (in degrees; relative to its 0 rotation)
 * @param easing How to interpolate the animation
 * @param reversed Whether the animation should be played in reverse
 * @param play Whether the animation should start playing automatically
 * @param op Modify the animation after it is created
 * @return A RotateTransition on this node
 */
fun Node.rotate(time: Duration, angle: Number,
                easing: Interpolator = Interpolator.EASE_BOTH, reversed: Boolean = false, play: Boolean = true,
                op: RotateTransition.() -> Unit = {}): RotateTransition {
    val target: Double
    if (reversed) {
        target = rotate
        rotate = angle.toDouble()
    } else {
        target = angle.toDouble()
    }
    return RotateTransition(time, this).apply {
        interpolator = easing
        op(this)
        toAngle = target
        if (play) play()
    }
}

/**
 * A convenience function for creating a [ScaleTransition] on a [UIComponent].
 *
 * @param time How long the animation will take
 * @param scale How to scale the component (relative to its default scale)
 * @param easing How to interpolate the animation
 * @param reversed Whether the animation should be played in reverse
 * @param play Whether the animation should start playing automatically
 * @param op Modify the animation after it is created
 * @return A ScaleTransition on this component
 */
fun UIComponent.scale(
        time: Duration,
        scale: Point2D,
        easing: Interpolator = Interpolator.EASE_BOTH,
        reversed: Boolean = false,
        play: Boolean = true,
        op: ScaleTransition.() -> Unit = {}
) = root.scale(time, scale, easing, reversed, play, op)

/**
 * A convenience function for creating a [ScaleTransition] on a [Node].
 *
 * @param time How long the animation will take
 * @param scale How to scale the node (relative to its default scale)
 * @param easing How to interpolate the animation
 * @param reversed Whether the animation should be played in reverse
 * @param play Whether the animation should start playing automatically
 * @param op Modify the animation after it is created
 * @return A ScaleTransition on this node
 */
fun Node.scale(time: Duration, scale: Point2D,
               easing: Interpolator = Interpolator.EASE_BOTH, reversed: Boolean = false, play: Boolean = true,
               op: ScaleTransition.() -> Unit = {}): ScaleTransition {
    val target: Point2D
    if (reversed) {
        target = point(scaleX, scaleY)
        scaleX = scale.x
        scaleY = scale.y
    } else {
        target = scale
    }
    return ScaleTransition(time, this).apply {
        interpolator = easing
        op(this)
        toX = target.x
        toY = target.y
        if (play) play()
    }
}

/**
 * A convenience function for creating a [FadeTransition] on a [UIComponent].
 *
 * @param time How long the animation will take
 * @param opacity The final opacity of the component
 * @param easing How to interpolate the animation
 * @param reversed Whether the animation should be played in reverse
 * @param play Whether the animation should start playing automatically
 * @param op Modify the animation after it is created
 * @return A FadeTransition on this component
 */
fun UIComponent.fade(
        time: Duration,
        opacity: Number,
        easing: Interpolator = Interpolator.EASE_BOTH,
        reversed: Boolean = false,
        play: Boolean = true,
        op: FadeTransition.() -> Unit = {}
) = root.fade(time, opacity, easing, reversed, play, op)

/**
 * A convenience function for creating a [FadeTransition] on a [Node].
 *
 * @param time How long the animation will take
 * @param opacity The final opacity of the node
 * @param easing How to interpolate the animation
 * @param reversed Whether the animation should be played in reverse
 * @param play Whether the animation should start playing automatically
 * @param op Modify the animation after it is created
 * @return A FadeTransition on this node
 */
fun Node.fade(time: Duration, opacity: Number,
              easing: Interpolator = Interpolator.EASE_BOTH, reversed: Boolean = false, play: Boolean = true,
              op: FadeTransition.() -> Unit = {}): FadeTransition {
    val target: Double
    if (reversed) {
        target = this.opacity
        this.opacity = opacity.toDouble()
    } else {
        target = opacity.toDouble()
    }
    return FadeTransition(time, this).apply {
        interpolator = easing
        op(this)
        toValue = target
        if (play) play()
    }
}

/**
 * A convenience function for creating a [TranslateTransition], [RotateTransition], [ScaleTransition], [FadeTransition]
 * on a [UIComponent] that all run simultaneously.
 *
 * @param time How long the animation will take
 * @param destination Where to move the component (relative to its translation origin)
 * @param angle How far to rotate the component (in degrees; relative to its 0 rotation)
 * @param scale How to scale the component (relative to its default scale)
 * @param opacity The final opacity of the component
 * @param easing How to interpolate the animation
 * @param reversed Whether the animation should be played in reverse
 * @param play Whether the animation should start playing automatically
 * @param op Modify the animation after it is created
 * @return A ParallelTransition on this component
 */
fun UIComponent.transform(
        time: Duration,
        destination: Point2D,
        angle: Number,
        scale: Point2D,
        opacity: Number,
        easing: Interpolator = Interpolator.EASE_BOTH,
        reversed: Boolean = false,
        play: Boolean = true,
        op: ParallelTransition.() -> Unit = {}
) = root.transform(time, destination, angle, scale, opacity, easing, reversed, play, op)

/**
 * A convenience function for creating a [TranslateTransition], [RotateTransition], [ScaleTransition], [FadeTransition]
 * on a [Node] that all run simultaneously.
 *
 * @param time How long the animation will take
 * @param destination Where to move the node (relative to its translation origin)
 * @param angle How far to rotate the node (in degrees; relative to its 0 rotation)
 * @param scale How to scale the node (relative to its default scale)
 * @param opacity The final opacity of the node
 * @param easing How to interpolate the animation
 * @param reversed Whether the animation should be played in reverse
 * @param play Whether the animation should start playing automatically
 * @param op Modify the animation after it is created
 * @return A ParallelTransition on this node
 */
fun Node.transform(
        time: Duration,
        destination: Point2D,
        angle: Number,
        scale: Point2D,
        opacity: Number,
        easing: Interpolator = Interpolator.EASE_BOTH,
        reversed: Boolean = false,
        play: Boolean = true,
        op: ParallelTransition.() -> Unit = {}
) = move(time, destination, easing, reversed, play)
        .and(rotate(time, angle, easing, reversed, play))
        .and(scale(time, scale, easing, reversed, play))
        .and(fade(time, opacity, easing, reversed, play))
        .apply {
            interpolator = easing
            op(this)
            if (play) play()
        }

/**
 * A convenience function for creating a parallel animation from multiple animations.
 *
 * @receiver The base animation
 * @param animation The animations to play with this one
 * @param op Modify the animation after it is created
 * @return A ParallelTransition
 */
fun Animation.and(vararg animation: Animation, op: ParallelTransition.() -> Unit = {}) = when {
    this is ParallelTransition -> this.apply { children += animation }
    else -> ParallelTransition(this, *animation)
}.also(op)

infix fun Animation.and(animation: Animation) = when {
    this is ParallelTransition -> this.apply { children += animation }
    else -> ParallelTransition(this, animation)
}

/**
 * A convenience function for playing multiple animations in parallel.
 *
 * @receiver The animations to play in parallel
 * @param play Whether to start playing immediately
 * @param op Modify the animation before playing
 * @return A ParallelTransition
 */
fun Iterable<Animation>.playParallel(
        play: Boolean = true,
        op: ParallelTransition.() -> Unit = {}
) = ParallelTransition().apply {
    children.setAll(toList())
    op(this)
    if (play) play()
}

/**
 * A convenience function for creating a sequential animation from multiple animations.
 *
 * @receiver The base animation
 * @param animation The animations to play with this one
 * @param op Modify the animation after it is created
 * @return A SequentialTransition
 */
fun Animation.then(vararg animation: Animation, op: SequentialTransition.() -> Unit = {}) = when {
    this is SequentialTransition -> this.apply { children += animation }
    else -> SequentialTransition(this, *animation)
}.also(op)

infix fun Animation.then(animation: Animation) = when {
    this is SequentialTransition -> this.apply { children += animation }
    else -> SequentialTransition(this, animation)
}

/**
 * A convenience function for playing multiple animations in sequence.
 *
 * @receiver The animations to play in sequence
 * @param play Whether to start playing immediately
 * @param op Modify the animation before playing
 * @return A SequentialTransition
 */
fun Iterable<Animation>.playSequential(
        play: Boolean = true,
        op: SequentialTransition.() -> Unit = {}
) = SequentialTransition().apply {
    children.setAll(toList())
    op(this)
    if (play) play()
}

fun Shape.animateFill(time: Duration, from: Color, to: Color,
                      easing: Interpolator = Interpolator.EASE_BOTH, reversed: Boolean = false, play: Boolean = true,
                      op: FillTransition.() -> Unit = {}): FillTransition {
    return FillTransition(time, this, from, to).apply {
        interpolator = easing
        if (reversed) {
            fromValue = to
            toValue = from
        }
        op(this)
        if (play) play()
    }
}

fun Shape.animateStroke(time: Duration, from: Color, to: Color,
                        easing: Interpolator = Interpolator.EASE_BOTH, reversed: Boolean = false, play: Boolean = true,
                        op: StrokeTransition.() -> Unit = {}): StrokeTransition {
    return StrokeTransition(time, this, from, to).apply {
        interpolator = easing
        if (reversed) {
            fromValue = to
            toValue = from
        }
        op(this)
        if (play) play()
    }
}

fun Node.follow(time: Duration, path: Shape,
                easing: Interpolator = Interpolator.EASE_BOTH, reversed: Boolean = false, play: Boolean = true,
                op: PathTransition.() -> Unit = {}): PathTransition {
    return PathTransition(time, path, this).apply {
        interpolator = easing
        op(this)
        if (reversed && rate > 0.0) rate = -rate
        if (play) play()
    }
}

fun pause(time: Duration, play: Boolean = true, op: PauseTransition.() -> Unit = {}) = PauseTransition(time).apply {
    op(this)
    if (play) play()
}

fun Timeline.keyframe(duration: Duration, op: (KeyFrameBuilder).() -> Unit): KeyFrame {
    val keyFrame = KeyFrameBuilder(duration).also(op).build()
    return keyFrame.also { this += it }
}

class KeyFrameBuilder(val duration: Duration) {

    var keyValues: MutableList<KeyValue> = ArrayList()
    var name: String? = null
    private var _onFinished: (ActionEvent) -> Unit = {}

    fun setOnFinished(onFinished: (ActionEvent) -> Unit) {
        this._onFinished = onFinished
    }

    operator fun plusAssign(keyValue: KeyValue) {
        keyValues.add(keyValue)
    }

    fun <T> keyvalue(writableValue: WritableValue<T>, endValue: T, interpolator: Interpolator? = null): KeyValue {
        val keyValue = interpolator?.let { KeyValue(writableValue, endValue, it) } ?: KeyValue(writableValue, endValue)
        this += keyValue
        return keyValue
    }

    internal fun build() = KeyFrame(duration, name, _onFinished, keyValues)

}

fun <T> WritableValue<T>.animate(endValue: T, duration: Duration, interpolator: Interpolator? = null, op: Timeline.() -> Unit = {}) {
    val writableValue = this
    val timeline = Timeline()

    timeline.apply {
        keyframe(duration) {
            keyvalue(writableValue, endValue, interpolator)
        }
        op()
        play()
    }
}

val Number.millis: Duration get() = Duration.millis(this.toDouble())
val Number.seconds: Duration get() = Duration.seconds(this.toDouble())
val Number.minutes: Duration get() = Duration.minutes(this.toDouble())
val Number.hours: Duration get() = Duration.hours(this.toDouble())

operator fun Duration.plus(duration: Duration): Duration = this.add(duration)
operator fun Duration.minus(duration: Duration): Duration = this.minus(duration)

/**
 * A class that, when used with [replaceWith] or [UIComponent.replaceWith], allows you to replace [View]s, [Fragment]s,
 * or [Node]s using a transition effect.
 *
 * To create a new ViewTransition, you need to implement the [ViewTransition.create] function. You should also override
 * [ViewTransition.onComplete] to cleanup any changes to the nodes (such as resetting transformations).
 *
 * During the transition, the view/fragment/node being transitioned is replaced with a temporary [StackPane] containing
 * both the current node and its replacement. By default, this StackPane contains only these two nodes with the current
 * node on top. You can override [ViewTransition.stack] to change how this works.
 *
 * If you need to change the order of the stack during the transition, you have access to the stack as a parameter of
 * the create method, and there is also a StackPane extension function ([ViewTransition.moveToTop]) for convenience.
 */
abstract class ViewTransition {
    /**
     * Create an animation to play for the transition between two nodes. The [StackPane] used as a placeholder during
     * the transition is also provided.
     *
     * There are a number of useful extensions functions for nodes defined above to make animations easier. See any of
     * the ViewTransitions defined below for examples.
     *
     * @param current The node currently in the scenegraph that is to be replaced
     * @param replacement The node that will be in the scenegraph after the transition
     * @param stack The StackPane containing the nodes during the transition
     * @return The animation that will play during the transition
     */
    abstract fun create(current: Node, replacement: Node, stack: StackPane): Animation

    /**
     * Will be called after the transition is finished and the replacement node has been docked. This function is useful
     * for resetting changes made to the node during the transition, such as position, scale, and opacity.
     *
     * See [Metro] for an example.
     *
     * @param removed The node that was removed from the scenegraph
     * @param replacement The node now in the scenegraph
     */
    open fun onComplete(removed: Node, replacement: Node) = Unit

    var setup: StackPane.() -> Unit = {}
    /**
     * This allows users to modify the generated stack after the the ViewTransition is generated (so they can add things
     * like AnchorPane and VGrow/HGrow constraints).
     */
    fun setup(setup: StackPane.() -> Unit) {
        this.setup = setup
    }

    internal fun call(current: Node, replacement: Node, attach: (Node) -> Unit) {
        current.isTransitioning = true
        replacement.isTransitioning = true
        val currentUIComponent = current.properties[UI_COMPONENT_PROPERTY] as? UIComponent
        val replacementUIComponent = replacement.properties[UI_COMPONENT_PROPERTY] as? UIComponent
        currentUIComponent?.muteDocking = true
        replacementUIComponent?.muteDocking = true

        val stack = stack(current, replacement)
        stack.setup()
        attach(stack)

        create(current, replacement, stack).apply {
            val oldFinish: EventHandler<ActionEvent>? = onFinished
            setOnFinished {
                stack.children.clear()
                current.removeFromParent()
                replacement.removeFromParent()
                stack.removeFromParent()
                currentUIComponent?.let {
                    it.muteDocking = false
                    it.callOnUndock()
                }
                replacementUIComponent?.muteDocking = false
                attach(replacement)
                oldFinish?.handle(it)
                onComplete(current, replacement)
                current.isTransitioning = false
                replacement.isTransitioning = false
            }
        }.play()
    }

    /**
     * If the given node exists in this [StackPane], move it to be on top (visually).
     *
     * @param node The node to move to the top of the stack
     */
    protected fun StackPane.moveToTop(node: Node) {
        if (children.remove(node)) children.add(node)
    }

    protected fun Image.toCanvas() = Canvas(width, height).also {
        it.graphicsContext2D.drawImage(this, 0.0, 0.0)
    }

    /**
     * Create a [StackPane] in which the transition will take place. You should generally put both the current and the
     * replacement nodes in the stack, but it isn't technically required.
     *
     * See [FadeThrough] for an example.
     *
     * By default this returns a StackPane with only the current and replacement nodes in it (the current node will be
     * on top).
     *
     * @param current The node that is currently in the scenegraph
     * @param replacement The node that will be in the scenegraph after the transition
     * @return The [StackPane] that will be in the scenegraph during the transition
     */
    open fun stack(current: Node, replacement: Node) = StackPane(replacement, current)

    companion object {
        @Deprecated("Use `Slide(0.2.seconds)`", ReplaceWith("Slide(0.2.seconds)"))
        val SlideIn = fun(existing: UIComponent, replacement: UIComponent, transitionCompleteCallback: () -> Unit) {
            replacement.root.translateX = existing.root.boundsInLocal.width

            val existingSlide = TranslateTransition(0.2.seconds, existing.root).apply {
                toX = -existing.root.boundsInLocal.width
                interpolator = Interpolator.EASE_OUT
            }

            val replacementSlide = TranslateTransition(0.2.seconds, replacement.root).apply {
                toX = 0.0
                onFinished = EventHandler { transitionCompleteCallback() }
                interpolator = Interpolator.EASE_OUT
            }

            existingSlide.play()
            replacementSlide.play()
        }

        @Deprecated("Use `Slide(0.2.seconds, Direction.LEFT)`", ReplaceWith("Slide(0.2.seconds, Direction.RIGHT)"))
        val SlideOut = fun(existing: UIComponent, replacement: UIComponent, transitionCompleteCallback: () -> Unit) {
            replacement.root.translateX = -existing.root.boundsInLocal.width

            val existingSlide = TranslateTransition(0.2.seconds, existing.root).apply {
                toX = existing.root.boundsInLocal.width
                interpolator = Interpolator.EASE_OUT
            }

            val replacementSlide = TranslateTransition(0.2.seconds, replacement.root).apply {
                toX = 0.0
                onFinished = EventHandler { transitionCompleteCallback() }
                interpolator = Interpolator.EASE_OUT
            }

            existingSlide.play()
            replacementSlide.play()
        }
    }

    enum class Direction { UP, RIGHT, DOWN, LEFT;

        fun reversed() = when (this) {
            UP -> DOWN
            RIGHT -> LEFT
            DOWN -> UP
            LEFT -> RIGHT
        }
    }

    /**
     * A [ViewTransition] that smoothly fades from one node to another.
     *
     * @param duration How long the transition will take
     */
    class Fade(val duration: Duration) : ViewTransition() {
        override fun create(current: Node, replacement: Node, stack: StackPane) = current.fade(duration, 0, play = false)

        override fun onComplete(removed: Node, replacement: Node) {
            removed.opacity = 1.0
        }
    }

    /**
     * A [ViewTransition] that fades from one node to another through a Paint (useful for fading through black).
     *
     * By default this fades through transparent, meaning it will show whatever is underneath the current node.
     *
     * @param duration How long the transition will take
     * @param color The color to fade through (can be any [Paint])
     */
    class FadeThrough(duration: Duration, val color: Paint = Color.TRANSPARENT) : ViewTransition() {
        private val bg = Pane().apply { background = Background(BackgroundFill(color, null, null)) }
        val halfTime: Duration = duration.divide(2.0)
        override fun create(
                current: Node,
                replacement: Node,
                stack: StackPane
        ) = current.fade(halfTime, 0, easing = Interpolator.EASE_IN, play = false)
                .then(replacement.fade(halfTime, 0, easing = Interpolator.EASE_OUT, reversed = true, play = false))

        override fun stack(current: Node, replacement: Node) = StackPane(bg, replacement, current)

        override fun onComplete(removed: Node, replacement: Node) {
            removed.opacity = 1.0
        }
    }

    /**
     * A [ViewTransition] that can be reversed
     */
    abstract class ReversibleViewTransition<out T : ViewTransition> : ViewTransition() {
        /**
         * Create a transition to be the reverse of the current transition. It's generally recommended (though in no way
         * required) for the reverse transition to be a logical reversal of this transition.
         *
         * @return The reversed [ViewTransition]
         */
        protected abstract fun reversed(): T

        fun reversed(op: T.() -> Unit = {}): T = reversed().also(op)
    }

    /**
     * A [ReversibleViewTransition] that slides one node out (in a given direction) while sliding another in. The effect
     * is similar to panning the camera in the opposite direction.
     *
     * The reverse is a Slide in the opposite direction.
     *
     * @param duration How long the transition will take
     * @param direction The direction to slide the nodes
     */
    class Slide(val duration: Duration, val direction: Direction = Direction.LEFT) : ReversibleViewTransition<Slide>() {
        override fun create(current: Node, replacement: Node, stack: StackPane): Animation {
            val bounds = current.boundsInLocal
            val destination = when (direction) {
                Direction.UP -> point(0, -bounds.height)
                Direction.RIGHT -> point(bounds.width, 0)
                Direction.DOWN -> point(0, bounds.height)
                Direction.LEFT -> point(-bounds.width, 0)
            }
            return current.move(duration, destination, play = false)
                    .and(replacement.move(duration, destination.multiply(-1.0), reversed = true, play = false))
        }

        override fun stack(current: Node, replacement: Node) = super.stack(replacement, current)

        override fun onComplete(removed: Node, replacement: Node) {
            removed.translateX = 0.0
            removed.translateY = 0.0
        }

        override fun reversed() = Slide(duration, direction.reversed()).also { it.setup = setup }
    }

    /**
     * A [ReversibleViewTransition] where a node slides in in a given direction covering another node.
     *
     * The reverse is a [Reveal] in the opposite direction
     *
     * @param duration How long the transition will take
     * @param direction The direction to slide the node
     */
    class Cover(val duration: Duration, val direction: Direction = Direction.LEFT) : ReversibleViewTransition<Reveal>() {
        override fun create(current: Node, replacement: Node, stack: StackPane): Animation {
            val bounds = current.boundsInLocal
            val destination = when (direction) {
                Direction.UP -> point(0, bounds.height)
                Direction.RIGHT -> point(-bounds.width, 0)
                Direction.DOWN -> point(0, -bounds.height)
                Direction.LEFT -> point(bounds.width, 0)
            }
            return replacement.move(duration, destination, reversed = true, play = false)
        }

        override fun stack(current: Node, replacement: Node) = super.stack(replacement, current)

        override fun reversed() = Reveal(duration, direction.reversed()).also { it.setup = setup }
    }


    /**
     * A [ReversibleViewTransition] where a node slides out in a given direction revealing another node.
     *
     * The reverse is a [Cover] in the opposite direction
     *
     * @param duration How long the transition will take
     * @param direction The direction to slide the node
     */
    class Reveal(val duration: Duration, val direction: Direction = Direction.LEFT) : ReversibleViewTransition<Cover>() {
        override fun create(current: Node, replacement: Node, stack: StackPane): Animation {
            val bounds = current.boundsInLocal
            val destination = when (direction) {
                Direction.UP -> point(0, -bounds.height)
                Direction.RIGHT -> point(bounds.width, 0)
                Direction.DOWN -> point(0, bounds.height)
                Direction.LEFT -> point(-bounds.width, 0)
            }
            return current.move(duration, destination, play = false)
        }

        override fun onComplete(removed: Node, replacement: Node) {
            removed.translateX = 0.0
            removed.translateY = 0.0
        }

        override fun reversed() = Cover(duration, direction.reversed()).also { it.setup = setup }
    }

    /**
     * A [ReversibleViewTransition] where a node fades and slides out in a given direction while another node fades and
     * slides in. The effect is similar to effects commonly used in Windows applications (thus the name).
     *
     * The reverse is a Metro in the opposite direction.
     *
     * @param duration How long the transition will take
     * @param direction The direction to slide the nodes
     * @param distancePercentage The distance to move the nodes as a percentage of the size of the current node
     */
    class Metro(val duration: Duration, val direction: Direction = Direction.LEFT, val distancePercentage: Double = 0.1) : ReversibleViewTransition<Metro>() {
        override fun create(current: Node, replacement: Node, stack: StackPane): Animation {
            val bounds = current.boundsInLocal
            val destination = when (direction) {
                Direction.UP -> point(0, -bounds.height * distancePercentage)
                Direction.RIGHT -> point(bounds.width * distancePercentage, 0)
                Direction.DOWN -> point(0, bounds.height * distancePercentage)
                Direction.LEFT -> point(-bounds.width * distancePercentage, 0)
            }
            return current.transform(duration.divide(2.0), destination, 0, point(1, 1), 0, play = false)
                    .then(replacement.transform(duration.divide(2.0), destination.multiply(-1.0), 0, point(1, 1), 0,
                            reversed = true, play = false))
        }

        override fun onComplete(removed: Node, replacement: Node) {
            removed.translateX = 0.0
            removed.translateY = 0.0
            removed.opacity = 1.0
        }

        override fun reversed() = Metro(duration, direction.reversed(), distancePercentage).also { it.setup = setup }
    }

    /**
     * A [ReversibleViewTransition] that swaps the two nodes in a way that looks like two cards in a deck being swapped.
     *
     * The reverse is a Swap in the opposite direction
     *
     * @param duration How long the transition will take
     * @param direction The direction the current node will initially move
     * @param scale The starting scale of the replacement node and ending scale of the current node
     */
    class Swap(val duration: Duration, val direction: Direction = Direction.LEFT, val scale: Point2D = point(.75, .75)) : ReversibleViewTransition<Swap>() {
        override fun create(current: Node, replacement: Node, stack: StackPane): Animation {
            val bounds = current.boundsInLocal
            val destination = when (direction) {
                Direction.UP -> point(0, -bounds.height * 0.5)
                Direction.RIGHT -> point(bounds.width * 0.5, 0)
                Direction.DOWN -> point(0, bounds.height * 0.5)
                Direction.LEFT -> point(-bounds.width * 0.5, 0)
            }
            val halfTime = duration.divide(2.0)
            return current.scale(duration, scale, play = false).and(replacement.scale(duration, scale, reversed = true, play = false))
                    .and(current.move(halfTime, destination).and(replacement.move(halfTime, destination.multiply(-1.0))) {
                        setOnFinished { stack.moveToTop(replacement) }
                    }.then(current.move(halfTime, Point2D.ZERO).and(replacement.move(halfTime, Point2D.ZERO))))
        }

        override fun onComplete(removed: Node, replacement: Node) {
            removed.translateX = 0.0
            removed.translateY = 0.0
            removed.scaleX = 1.0
            removed.scaleY = 1.0
        }

        override fun reversed() = Swap(duration, direction.reversed(), scale).also { it.setup = setup }
    }

    /**
     * A [ViewTransition] similar to flipping a node over to reveal another on its back. The effect has no perspective
     * (it isn't 3D) due to the limits of JavaFX's affine transformation system.
     *
     * @param duration How long the transition will take
     * @param vertical Whether to flip the card vertically or horizontally
     */
    class Flip(duration: Duration, vertical: Boolean = false) : ViewTransition() {
        val halfTime: Duration = duration.divide(2.0)
        val targetAxis: Point3D = (if (vertical) Rotate.X_AXIS else Rotate.Y_AXIS)

        override fun create(current: Node, replacement: Node, stack: StackPane): Animation {
            return current.rotate(halfTime, 90, easing = Interpolator.EASE_IN, play = false) { axis = targetAxis}.then(
                    replacement.rotate(halfTime, 90, easing = Interpolator.EASE_OUT, reversed = true, play = false) {
                        axis = targetAxis
                    }
            )
        }

        override fun onComplete(removed: Node, replacement: Node) {
            removed.rotate = 0.0
            removed.rotationAxis = Rotate.Z_AXIS
            replacement.rotationAxis = Rotate.Z_AXIS
        }
    }

    /**
     * A [ViewTransition] where a node spins and grows like a newspaper.
     *
     * The effect is similar to this: http://www.canstockphoto.com/spinning-news-paper-4032376.html
     *
     * @param duration How long the transition will take
     * @param rotations How many time the paper will rotate (use a negative value for clockwise rotation)
     */
    class NewsFlash(val duration: Duration, val rotations: Number = 2) : ViewTransition() {
        override fun create(current: Node, replacement: Node, stack: StackPane) = replacement.transform(
                time = duration,
                destination = Point2D.ZERO,
                angle = rotations.toDouble() * 360,
                scale = Point2D.ZERO,
                opacity = 1,
                easing = Interpolator.EASE_IN,
                reversed = true,
                play = false
        )

        override fun stack(current: Node, replacement: Node) = super.stack(replacement, current)
    }

    /**
     * A [ReversibleViewTransition] where a node grows and fades out revealing another node.
     *
     * The reverse is an [Implode]
     *
     * @param duration How long the transition will take
     * @param scale How big to scale the node as it fades out
     */
    class Explode(val duration: Duration, val scale: Point2D = point(2, 2)) : ReversibleViewTransition<Implode>() {
        override fun create(
                current: Node,
                replacement: Node,
                stack: StackPane
        ) = current.transform(duration, Point2D.ZERO, 0, scale, 0, play = false)

        override fun onComplete(removed: Node, replacement: Node) {
            removed.scaleX = 1.0
            removed.scaleY = 1.0
            removed.opacity = 1.0
        }

        override fun reversed() = Implode(duration, scale).also { it.setup = setup }
    }

    /**
     * A [ReversibleViewTransition] where a node shrinks and fades in revealing another node.
     *
     * The reverse is an [Explode]
     *
     * @param duration How long the transition will take
     * @param scale The initial size the node shrinks from
     */
    class Implode(val duration: Duration, val scale: Point2D = point(2, 2)) : ReversibleViewTransition<Explode>() {
        override fun create(
                current: Node,
                replacement: Node,
                stack: StackPane
        ) = replacement.transform(duration, Point2D.ZERO, 0, scale, 0, reversed = true, play = false)

        override fun stack(current: Node, replacement: Node) = super.stack(replacement, current)

        override fun reversed() = Explode(duration, scale).also { it.setup = setup }
    }

    /**
     * A [ReversibleViewTransition] where a node is wiped away in a given direction revealing another node.
     *
     * The reverse is an Wipe in the opposite direction
     *
     * @param duration How long the transition will take
     * @param direction The direction the node is wiped away
     */
    class Wipe(val duration: Duration, val direction: Direction = Direction.LEFT) : ReversibleViewTransition<Wipe>() {
        override fun stack(current: Node, replacement: Node) =
            StackPane(replacement, current.snapshot(null, null).toCanvas())

        override fun create(current: Node, replacement: Node, stack: StackPane): Animation {
            val canvas = stack.children[1] as Canvas
            val gc = canvas.graphicsContext2D
            return object : Transition() {
                var x = 0.0
                var y = 0.0
                var width = canvas.width
                var height = canvas.height

                init {
                    cycleDuration = duration
                }

                override fun interpolate(frac: Double) {
                    when (direction) {
                        ViewTransition.Direction.UP, ViewTransition.Direction.DOWN -> {
                            height = frac * canvas.height
                            if (direction == ViewTransition.Direction.UP) y = canvas.height - height
                        }
                        ViewTransition.Direction.LEFT, ViewTransition.Direction.RIGHT -> {
                            width = frac * canvas.width
                            if (direction == ViewTransition.Direction.LEFT) x = canvas.width - width
                        }
                    }
                    gc.clearRect(x, y, width, height)
                }
            }
        }

        override fun reversed() = Wipe(duration, direction.reversed()).also { it.setup = setup }
    }

    /**
     * A [ViewTransition] where a node is dissolved revealing another node.
     *
     * @param duration How long the transition will take
     * @param hChunks The number of horizontal chunks the current node will be broken into
     * @param vChunks The number of vertical chunks the current node will be broken into
     */
    class Dissolve(val duration: Duration, val hChunks: Int = 50, val vChunks: Int = 50) : ViewTransition() {
        override fun stack(current: Node, replacement: Node) =
            StackPane(replacement, current.snapshot(null, null).toCanvas())

        override fun create(current: Node, replacement: Node, stack: StackPane): Animation {
            val canvas = stack.children[1] as Canvas
            val width = canvas.width / hChunks
            val height = canvas.height / vChunks
            val gc = canvas.graphicsContext2D
            val originalSize = hChunks * vChunks
            val indices = MutableList(originalSize) { it }
            indices.shuffle()
            return object : Transition() {
                init {
                    cycleDuration = duration
                }

                override fun interpolate(frac: Double) {
                    if (indices.isEmpty()) return
                    repeat((frac * originalSize).toInt() - originalSize + indices.lastIndex) {
                        val index = indices.removeAt(indices.lastIndex)
                        gc.clearRect(index % hChunks * width, index / hChunks * height, width, height)
                    }
                }
            }
        }
    }
}
