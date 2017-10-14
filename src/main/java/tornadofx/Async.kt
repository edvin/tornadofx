package tornadofx

import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.concurrent.Task
import javafx.scene.Node
import javafx.scene.layout.BorderPane
import javafx.util.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Covers node with overlay (by default - an instance of [MaskPane]) until [latch] is released by another thread.
 * It's useful when more control over async execution is needed, like:
 * * A task already have its thread and overlay should be visible until some callback is invoked (it should invoke
 * [CountDownLatch.countDown] or [Latch.release]) in order to unlock UI. Keep in mind that if [latch] is not released
 * and [timeout] is not set, overlay may never get removed.
 * * An overlay should be removed after some time, even if task is getting unresponsive (use [timeout] for this).
 * * In addition to masking UI, you need an access to property indicating if background process is running;
 * [Latch.lockedProperty] serves exactly that purpose.
 * * More threads are involved in task execution. You can create a [CountDownLatch] for number of workers, call
 * [CountDownLatch.countDown] from each of them when it's done and overlay will stay visible until all workers finish
 * their jobs.
 *
 * @param latch an instance of [CountDownLatch], usage of [Latch] is recommended.
 * @param timeout timeout after which overlay will be removed anyway. Can be `null` (which means no timeout).
 * @param overlayNode optional custom overlay node. For best effect set transparency.
 *
 * # Example
 *
 * ```kotlin
 * val latch = Latch()
 * root.runAsyncWithOverlay(latch) ui {
 *   //UI action
 * }
 *
 * Thread({
 *   Thread.sleep(2000)
 *   latch.release()
 * }).start()
 * ```
 */
@JvmOverloads
fun Node.runAsyncWithOverlay(latch: CountDownLatch, timeout: Duration? = null, overlayNode: Node = MaskPane()): Task<Boolean> {
    return if(timeout == null) {
        runAsyncWithOverlay(overlayNode) { latch.await(); true }
    } else {
        runAsyncWithOverlay(overlayNode) { latch.await(timeout.toMillis().toLong(), TimeUnit.MILLISECONDS) }
    }
}

/**
 * Runs given task in background thread, covering node with overlay (default one is [MaskPane]) until task is done.
 *
 * # Example
 *
 * ```kotlin
 * root.runAsyncWithOverlay {
 *   Thread.sleep(2000)
 * } ui {
 *   //UI action
 * }
 * ```
 *
 * @param overlayNode optional custom overlay node. For best effect set transparency.
 */
@JvmOverloads
fun <T : Any> Node.runAsyncWithOverlay(overlayNode: Node = MaskPane(), op: () -> T): Task<T> {
    val overlayContainer = stackpane { add(overlayNode) }

    replaceWith(overlayContainer)
    overlayContainer.children.add(0,this)

    return task {
        try { op() }
        finally { runLater { overlayContainer.replaceWith(this@runAsyncWithOverlay) } }
    }
}

/**
 * A basic mask pane, intended for blocking gui underneath. Styling example:
 *
 * ```css
 * .mask-pane {
 *     -fx-background-color: rgba(0,0,0,0.5);
 *     -fx-accent: aliceblue;
 * }
 *
 * .mask-pane > .progress-indicator {
 *     -fx-max-width: 300;
 *     -fx-max-height: 300;
 * }
 * ```
 */
class MaskPane : BorderPane() {
    init {
        addClass("mask-pane")
        center = progressindicator()
    }

    override fun getUserAgentStylesheet() = MaskPane::class.java.getResource("maskpane.css").toExternalForm()!!
}

/**
 * Adds some superpowers to good old [CountDownLatch], like exposed [lockedProperty] or ability to release latch
 * immediately.
 *
 * All documentation of superclass applies here. Default behavior has not been altered.
 */
class Latch(count: Int) : CountDownLatch(count) {
    /**
     * Initializes latch with count of `1`, which means that the first invocation of [countDown] will allow all
     * waiting threads to proceed.
     */
    constructor() : this(1)

    private val lockedProperty by lazy { ReadOnlyBooleanWrapper(locked) }

    /**
     * Locked state of this latch exposed as a property.
     */
    fun lockedProperty() : ReadOnlyBooleanProperty = lockedProperty.readOnlyProperty

    /**
     * Locked state of this latch.
     */
    val locked get() = count > 0L

    /**
     * Releases latch immediately and allows waiting thread(s) to proceed. Can be safely used if this latch has been
     * initialized with `count` of `1`, should be used with care otherwise - [countDown] invocations ar preferred in
     * such cases.
     */
    fun release() = (1..count).forEach { countDown() } //maybe not the prettiest way, but works fine

    override fun countDown() {
        super.countDown()
        lockedProperty.set(locked)
    }
}
