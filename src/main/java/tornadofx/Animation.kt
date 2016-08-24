package tornadofx

import javafx.animation.*
import javafx.beans.value.WritableValue
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.event.EventTarget
import javafx.geometry.Point2D
import javafx.scene.Node
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Paint
import javafx.util.Duration
import java.util.*

operator fun Timeline.plusAssign(keyFrame: KeyFrame) {
    keyFrames.add(keyFrame)
}

operator fun KeyFrame.plusAssign(keyValue: KeyValue) {
    values.add(keyValue)
}

fun EventTarget.timeline(play: Boolean = true, op: (Timeline).() -> Unit): Timeline {
    val timeline = Timeline()
    timeline.op()
    if (play) timeline.play()
    return timeline
}

fun UIComponent.move(time: Duration, destination: Point2D,
                     easing: Interpolator = Interpolator.EASE_BOTH, reversed: Boolean = false, play: Boolean = true,
                     op: (TranslateTransition.() -> Unit)? = null)
        = root.move(time, destination, easing, reversed, play, op)

fun Node.move(time: Duration, destination: Point2D,
              easing: Interpolator = Interpolator.EASE_BOTH, reversed: Boolean = false, play: Boolean = true,
              op: (TranslateTransition.() -> Unit)? = null): TranslateTransition {
    val target: Point2D
    if (reversed) {
        target = Point2D(translateX, translateY)
        translateX = destination.x
        translateY = destination.y
    } else {
        target = destination
    }
    return TranslateTransition(time, this).apply {
        interpolator = easing
        op?.invoke(this)
        toX = target.x
        toY = target.y
        if (play) play()
    }
}

fun UIComponent.rotate(time: Duration, angle: Double,
                       easing: Interpolator = Interpolator.EASE_BOTH, reversed: Boolean = false, play: Boolean = true,
                       op: (RotateTransition.() -> Unit)? = null)
        = root.rotate(time, angle, easing, reversed, play, op)

fun Node.rotate(time: Duration, angle: Double,
                easing: Interpolator = Interpolator.EASE_BOTH, reversed: Boolean = false, play: Boolean = true,
                op: (RotateTransition.() -> Unit)? = null): RotateTransition {
    val target: Double
    if (reversed) {
        target = rotate
        rotate = angle
    } else {
        target = angle
    }
    return RotateTransition(time, this).apply {
        interpolator = easing
        op?.invoke(this)
        toAngle = target
        if (play) play()
    }
}

fun UIComponent.scale(time: Duration, scale: Point2D,
                      easing: Interpolator = Interpolator.EASE_BOTH, reversed: Boolean = false, play: Boolean = true,
                      op: (ScaleTransition.() -> Unit)? = null)
        = root.scale(time, scale, easing, reversed, play, op)

fun Node.scale(time: Duration, scale: Point2D,
               easing: Interpolator = Interpolator.EASE_BOTH, reversed: Boolean = false, play: Boolean = true,
               op: (ScaleTransition.() -> Unit)? = null): ScaleTransition {
    val target: Point2D
    if (reversed) {
        target = Point2D(scaleX, scaleY)
        scaleX = scale.x
        scaleY = scale.y
    } else {
        target = scale
    }
    return ScaleTransition(time, this).apply {
        interpolator = easing
        op?.invoke(this)
        toX = target.x
        toY = target.y
        if (play) play()
    }
}

fun UIComponent.fade(time: Duration, opacity: Double,
                     easing: Interpolator = Interpolator.EASE_BOTH, reversed: Boolean = false, play: Boolean = true,
                     op: (FadeTransition.() -> Unit)? = null)
        = root.fade(time, opacity, easing, reversed, play, op)

fun Node.fade(time: Duration, alpha: Double,
              easing: Interpolator = Interpolator.EASE_BOTH, reversed: Boolean = false, play: Boolean = true,
              op: (FadeTransition.() -> Unit)? = null): FadeTransition {
    val target: Double
    if (reversed) {
        target = opacity
        opacity = alpha
    } else {
        target = alpha
    }
    return FadeTransition(time, this).apply {
        interpolator = easing
        op?.invoke(this)
        toValue = target
        if (play) play()
    }
}

fun UIComponent.transform(time: Duration, destination: Point2D, angle: Double, scale: Point2D, opacity: Double,
                          easing: Interpolator = Interpolator.EASE_BOTH, reversed: Boolean = false, play: Boolean = true,
                          op: (ParallelTransition.() -> Unit)? = null)
        = root.transform(time, destination, angle, scale, opacity, easing, reversed, play, op)

fun Node.transform(time: Duration, destination: Point2D, angle: Double, scale: Point2D, opacity: Double,
                   easing: Interpolator = Interpolator.EASE_BOTH, reversed: Boolean = false, play: Boolean = true,
                   op: (ParallelTransition.() -> Unit)? = null)
        = move(time, destination, easing, reversed, play)
        .and(rotate(time, angle, easing, reversed, play))
        .and(scale(time, scale, easing, reversed, play))
        .and(fade(time, opacity, easing, reversed, play))
        .apply {
            interpolator = easing
            op?.invoke(this)
            if (play) play()
        }

fun Animation.and(vararg animation: Animation, op: (ParallelTransition.() -> Unit)? = null): ParallelTransition {
    return ParallelTransition(this, *animation).apply { op?.invoke(this) }
}

fun Animation.then(vararg animation: Animation, op: (SequentialTransition.() -> Unit)? = null): SequentialTransition {
    return SequentialTransition(this, *animation).apply { op?.invoke(this) }
}

fun Timeline.keyframe(duration: Duration, op: (KeyFrameBuilder).() -> Unit): KeyFrame {
    val keyFrameBuilder = KeyFrameBuilder(duration)
    keyFrameBuilder.op()

    return keyFrameBuilder.build().apply {
        this@keyframe += this
    }
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

fun <T> WritableValue<T>.animate(endValue: T, duration: Duration, interpolator: Interpolator? = null, op: (Timeline.() -> Unit)? = null) {
    val writableValue = this
    val timeline = Timeline()

    timeline.apply {
        keyframe(duration) {
            keyvalue(writableValue, endValue, interpolator)
        }
    }

    op?.apply { this.invoke(timeline) }

    timeline.play()

}

val Number.millis: Duration get() = Duration.millis(this.toDouble())
val Number.seconds: Duration get() = Duration.seconds(this.toDouble())
val Number.minutes: Duration get() = Duration.minutes(this.toDouble())
val Number.hours: Duration get() = Duration.hours(this.toDouble())

operator fun Duration.plus(duration: Duration): Duration = this.add(duration)
operator fun Duration.minus(duration: Duration): Duration = this.minus(duration)

class ViewTransition {
    companion object {
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
}

abstract class ViewTransition2(val newOnTop: Boolean = true) {
    abstract fun transit(current: UIComponent, replacement: UIComponent, stack: StackPane): Animation

    open fun onComplete(removed: UIComponent, replacement: UIComponent) = Unit

    internal fun call(current: UIComponent, replacement: UIComponent, attachTemp: (StackPane) -> Unit, attachReplacement: (UIComponent) -> Unit) {
        current.muteDocking = true
        replacement.muteDocking = true

        val stack = stack(current, replacement)
        current.isTransitioning = true
        replacement.isTransitioning = true
        attachTemp(stack)

        val animation = transit(current, replacement, stack)
        val oldFinish: EventHandler<ActionEvent>? = animation.onFinished
        animation.setOnFinished {
            stack.children.clear()
            current.removeFromParent()
            replacement.removeFromParent()
            current.muteDocking = false
            replacement.muteDocking = false
            attachReplacement(replacement)
            oldFinish?.handle(it)
            onComplete(current, replacement)
            current.isTransitioning = false
            replacement.isTransitioning = false
        }
        animation.play()
    }

    protected fun StackPane.moveToTop(component: UIComponent) = moveToTop(component.root)
    protected fun StackPane.moveToTop(node: Node) {
        if (children.remove(node)) children.add(node)
    }

    open fun stack(current: UIComponent, replacement: UIComponent): StackPane {
        return if (newOnTop) StackPane(current.root, replacement.root) else StackPane(replacement.root, current.root)
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

class Fade(val duration: Duration) : ViewTransition2(false) {
    override fun transit(current: UIComponent, replacement: UIComponent, stack: StackPane) = current.fade(duration, 0.0, play = false)

    override fun onComplete(removed: UIComponent, replacement: UIComponent) {
        removed.root.opacity = 1.0
    }
}

class FadeThrough(val duration: Duration, val color: Paint) : ViewTransition2() {
    private val bg = Pane().apply { background = Background(BackgroundFill(color, null, null)) }
    val halfTime = duration.divide(2.0)
    override fun transit(current: UIComponent, replacement: UIComponent, stack: StackPane): Animation {
        return current.fade(halfTime, 0.0, easing = Interpolator.EASE_IN, play = false)
                .then(replacement.fade(halfTime, 0.0, easing = Interpolator.EASE_OUT, reversed = true, play = false))
    }

    override fun stack(current: UIComponent, replacement: UIComponent): StackPane {
        return StackPane(bg, replacement.root, current.root)
    }

    override fun onComplete(removed: UIComponent, replacement: UIComponent) {
        removed.root.opacity = 1.0
    }
}

abstract class ReversibleViewTransition(newOnTop: Boolean = true) : ViewTransition2(newOnTop) {
    abstract fun reversed(): ReversibleViewTransition
}

class Slide(val duration: Duration, val direction: Direction = Direction.LEFT) : ReversibleViewTransition() {
    override fun transit(current: UIComponent, replacement: UIComponent, stack: StackPane): Animation {
        val bounds = current.root.boundsInLocal
        val destination = when (direction) {
            Direction.UP -> Point2D(0.0, -bounds.height)
            Direction.RIGHT -> Point2D(bounds.width, 0.0)
            Direction.DOWN -> Point2D(0.0, bounds.height)
            Direction.LEFT -> Point2D(-bounds.width, 0.0)
        }
        return current.move(duration, destination, play = false)
                .and(replacement.move(duration, destination.multiply(-1.0), reversed = true, play = false))
    }

    override fun onComplete(removed: UIComponent, replacement: UIComponent) {
        removed.root.translateX = 0.0
        removed.root.translateY = 0.0
    }

    override fun reversed() = Slide(duration, direction.reversed())
}

class Cover(val duration: Duration, val direction: Direction = Direction.RIGHT) : ReversibleViewTransition() {
    override fun transit(current: UIComponent, replacement: UIComponent, stack: StackPane): Animation {
        val bounds = current.root.boundsInLocal
        val destination = when (direction) {
            Direction.UP -> Point2D(0.0, -bounds.height)
            Direction.RIGHT -> Point2D(bounds.width, 0.0)
            Direction.DOWN -> Point2D(0.0, bounds.height)
            Direction.LEFT -> Point2D(-bounds.width, 0.0)
        }
        return replacement.move(duration, destination, reversed = true, play = false)
    }

    override fun reversed() = Reveal(duration, direction)
}

class Reveal(val duration: Duration, val direction: Direction = Direction.LEFT) : ReversibleViewTransition(false) {
    override fun transit(current: UIComponent, replacement: UIComponent, stack: StackPane): Animation {
        val bounds = current.root.boundsInLocal
        val destination = when (direction) {
            Direction.UP -> Point2D(0.0, -bounds.height)
            Direction.RIGHT -> Point2D(bounds.width, 0.0)
            Direction.DOWN -> Point2D(0.0, bounds.height)
            Direction.LEFT -> Point2D(-bounds.width, 0.0)
        }
        return current.move(duration, destination, play = false)
    }

    override fun onComplete(removed: UIComponent, replacement: UIComponent) {
        removed.root.translateX = 0.0
        removed.root.translateY = 0.0
    }

    override fun reversed() = Cover(duration, direction)
}

class Metro(val duration: Duration, val direction: Direction = Direction.LEFT, val distancePercentage: Double = 0.1) : ReversibleViewTransition(false) {
    override fun transit(current: UIComponent, replacement: UIComponent, stack: StackPane): Animation {
        val bounds = current.root.boundsInLocal
        val destination = when (direction) {
            Direction.UP -> Point2D(0.0, -bounds.height * distancePercentage)
            Direction.RIGHT -> Point2D(bounds.width * distancePercentage, 0.0)
            Direction.DOWN -> Point2D(0.0, bounds.height * distancePercentage)
            Direction.LEFT -> Point2D(-bounds.width * distancePercentage, 0.0)
        }
        val opacity = 0.0
        val scale = Point2D(1.0, 1.0)
        return current.transform(duration.divide(2.0), destination, 0.0, scale, opacity, play = false)
                .then(replacement.transform(duration.divide(2.0), destination.multiply(-1.0), 0.0, scale, opacity,
                        reversed = true, play = false))
    }

    override fun onComplete(removed: UIComponent, replacement: UIComponent) {
        removed.root.translateX = 0.0
        removed.root.translateY = 0.0
        removed.root.opacity = 1.0
    }

    override fun reversed() = Metro(duration, direction.reversed())
}

class Swap(val duration: Duration, val direction: Direction = Direction.LEFT, val scaling: Point2D = Point2D(.75, .75)) : ReversibleViewTransition(false) {
    override fun transit(current: UIComponent, replacement: UIComponent, stack: StackPane): Animation {
        val bounds = current.root.boundsInLocal
        val destination = when (direction) {
            Direction.UP -> Point2D(0.0, -bounds.height * 0.5)
            Direction.RIGHT -> Point2D(bounds.width * 0.5, 0.0)
            Direction.DOWN -> Point2D(0.0, bounds.height * 0.5)
            Direction.LEFT -> Point2D(-bounds.width * 0.5, 0.0)
        }
        val halfTime = duration.divide(2.0)
        return current.scale(duration, scaling, play = false).and(replacement.scale(duration, scaling, reversed = true, play = false))
                .and(current.move(halfTime, destination).and(replacement.move(halfTime, destination.multiply(-1.0))) {
                    setOnFinished { stack.moveToTop(replacement) }
                }.then(current.move(halfTime, Point2D(0.0, 0.0)).and(replacement.move(halfTime, Point2D(0.0, 0.0)))))
    }

    override fun onComplete(removed: UIComponent, replacement: UIComponent) {
        removed.root.translateX = 0.0
        removed.root.translateY = 0.0
        removed.root.scaleX = 1.0
        removed.root.scaleY = 1.0
        replacement.root.translateX = 0.0
        replacement.root.translateY = 0.0
        replacement.root.scaleX = 1.0
        replacement.root.scaleY = 1.0
    }

    override fun reversed(): ReversibleViewTransition {
        return Swap(duration, direction.reversed())
    }
}

class NewsFlash(val duration: Duration, val rotations: Double) : ViewTransition2(true) {
    override fun transit(current: UIComponent, replacement: UIComponent, stack: StackPane): Animation {
        return replacement.transform(duration, Point2D.ZERO, rotations * 360, Point2D.ZERO, 1.0,
                easing = Interpolator.EASE_IN, reversed = true, play = false)
    }
}
