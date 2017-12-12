package tornadofx

import com.sun.glass.ui.Application
import com.sun.javafx.tk.Toolkit
import javafx.application.Platform
import javafx.beans.property.*
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.concurrent.Task
import javafx.concurrent.Worker
import javafx.scene.Node
import javafx.scene.control.Labeled
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Region
import javafx.util.Duration
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Level
import java.util.logging.Logger

internal val log = Logger.getLogger("tornadofx.async")
internal val dummyUncaughtExceptionHandler = Thread.UncaughtExceptionHandler { t, e -> log.log(Level.WARNING, e) { "Exception in ${t?.name ?: "?"}: ${e?.message ?: "?"}" } }

internal val tfxThreadPool = Executors.newCachedThreadPool(TFXThreadFactory(daemon = false))
internal val tfxDaemonThreadPool = Executors.newCachedThreadPool(TFXThreadFactory(daemon = true))

private class TFXThreadFactory(val daemon: Boolean) : ThreadFactory {
    private val threadCounter = AtomicLong(0L)
    override fun newThread(runnable: Runnable?) = Thread(runnable, threadName()).apply {
        isDaemon = daemon
    }

    private fun threadName() = "tornadofx-thread-${threadCounter.incrementAndGet()}" + if (daemon) "-daemon" else ""
}

private fun awaitTermination(pool: ExecutorService, timeout: Long) {
    synchronized(pool) {
        // Disable new tasks from being submitted
        pool.shutdown()
    }
    try {
        // Wait a while for existing tasks to terminate
        if (!pool.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
            synchronized(pool) {
                pool.shutdownNow() // Cancel currently executing tasks
            }
            // Wait a while for tasks to respond to being cancelled
            if (!pool.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                log.log(Level.SEVERE, "Executor did not terminate")
            }
        }
    } catch (ie: InterruptedException) {
        // (Re-)Cancel if current thread also interrupted
        synchronized(pool) {
            pool.shutdownNow()
        }
        // Preserve interrupt status
        Thread.currentThread().interrupt()
    }
}

fun terminateAsyncExecutors(timeoutMillis: Long) {
    awaitTermination(tfxThreadPool, timeoutMillis)
    awaitTermination(tfxDaemonThreadPool, timeoutMillis)
}

fun <T> task(taskStatus: TaskStatus? = null, func: FXTask<*>.() -> T): Task<T> = task(daemon = false, taskStatus = taskStatus, func = func)

fun <T> task(daemon: Boolean = false, taskStatus: TaskStatus? = null, func: FXTask<*>.() -> T): Task<T> = FXTask(taskStatus, func = func).apply {
    setOnFailed({ (Thread.getDefaultUncaughtExceptionHandler() ?: dummyUncaughtExceptionHandler).uncaughtException(Thread.currentThread(), exception) })
    if (daemon) {
        tfxDaemonThreadPool.execute(this)
    } else {
        tfxThreadPool.execute(this)
    }
}

fun <T> runAsync(status: TaskStatus? = null, func: FXTask<*>.() -> T) = task(status, func)

fun <T> runAsync(daemon: Boolean = false, status: TaskStatus? = null, func: FXTask<*>.() -> T) = task(daemon, status, func)

infix fun <T> Task<T>.ui(func: (T) -> Unit) = success(func)

infix fun <T> Task<T>.success(func: (T) -> Unit) = apply {
    fun attachSuccessHandler() {
        if (state == Worker.State.SUCCEEDED) {
            func(value)
        } else {
            setOnSucceeded {
                func(value)
            }
        }
    }

    if (Application.isEventThread())
        attachSuccessHandler()
    else
        runLater { attachSuccessHandler() }
}

infix fun <T> Task<T>.fail(func: (Throwable) -> Unit) = apply {
    fun attachFailHandler() {
        if (state == Worker.State.FAILED) {
            func(exception)
        } else {
            setOnFailed {
                func(exception)
            }
        }
    }

    if (Application.isEventThread())
        attachFailHandler()
    else
        runLater { attachFailHandler() }
}

/**
 * Run the specified Runnable on the JavaFX Application Thread at some
 * unspecified time in the future.
 */
fun runLater(op: () -> Unit) = Platform.runLater(op)

/**
 * Run the specified Runnable on the JavaFX Application Thread after a
 * specified delay.
 *
 * runLater(10.seconds) {
 *     // Do something on the application thread
 * }
 *
 * This function returns a TimerTask which includes a runningProperty as well as the owning timer.
 * You can cancel the task before the time is up to abort the execution.
 */
fun runLater(delay: Duration, op: () -> Unit): FXTimerTask {
    val timer = Timer(true)
    val task = FXTimerTask(op, timer)
    timer.schedule(task, delay.toMillis().toLong())
    return task
}

/**
 * Wait on the UI thread until a certain value is available on this observable.
 *
 * This method does not block the UI thread even though it halts further execution until the condition is met.
 */
fun <T> ObservableValue<T>.awaitUntil(condition: (T) -> Boolean) {
    if (!Toolkit.getToolkit().canStartNestedEventLoop()) {
        throw IllegalStateException("awaitUntil is not allowed during animation or layout processing")
    }

    val changeListener = object : ChangeListener<T> {
        override fun changed(observable: ObservableValue<out T>?, oldValue: T, newValue: T) {
            if (condition(value)) {
                runLater {
                    Toolkit.getToolkit().exitNestedEventLoop(this@awaitUntil, null)
                    removeListener(this)
                }
            }
        }
    }

    changeListener.changed(this, value, value)
    addListener(changeListener)
    Toolkit.getToolkit().enterNestedEventLoop(this)
}

/**
 * Wait on the UI thread until this observable value is true.
 *
 * This method does not block the UI thread even though it halts further execution until the condition is met.
 */
fun ObservableValue<Boolean>.awaitUntil() {
    this.awaitUntil { it }
}

/**
 * Replace this node with a progress node while a long running task
 * is running and swap it back when complete.
 *
 * If this node is Labeled, the graphic property will contain the progress bar instead while the task is running.
 *
 * The default progress node is a ProgressIndicator that fills the same
 * client area as the parent. You can swap the progress node for any Node you like.
 *
 * For latch usage see [runAsyncWithOverlay]
 */
fun Node.runAsyncWithProgress(latch: CountDownLatch, timeout: Duration? = null, progress: Node = ProgressIndicator()): Task<Boolean> {
    return if (timeout == null) {
        runAsyncWithProgress(progress) { latch.await(); true }
    } else {
        runAsyncWithOverlay(progress) { latch.await(timeout.toMillis().toLong(), TimeUnit.MILLISECONDS) }
    }
}

/**
 * Replace this node with a progress node while a long running task
 * is running and swap it back when complete.
 *
 * If this node is Labeled, the graphic property will contain the progress bar instead while the task is running.
 *
 * The default progress node is a ProgressIndicator that fills the same
 * client area as the parent. You can swap the progress node for any Node you like.
 */
fun <T : Any> Node.runAsyncWithProgress(progress: Node = ProgressIndicator(), op: () -> T): Task<T> {
    if (this is Labeled) {
        val oldGraphic = graphic
        (progress as? Region)?.setPrefSize(16.0, 16.0)
        graphic = progress
        return task {
            try {
                op()
            } finally {
                runLater {
                    this@runAsyncWithProgress.graphic = oldGraphic
                }
            }
        }
    } else {
        val paddingHorizontal = (this as? Region)?.paddingHorizontal?.toDouble() ?: 0.0
        val paddingVertical = (this as? Region)?.paddingVertical?.toDouble() ?: 0.0
        (progress as? Region)?.setPrefSize(boundsInParent.width - paddingHorizontal, boundsInParent.height - paddingVertical)
        val children = requireNotNull(parent.getChildList()) { "This node has no child list, and cannot contain the progress node" }
        val index = children.indexOf(this)
        children.add(index, progress)
        removeFromParent()
        return task {
            val result = op()
            runLater {
                children.add(index, this@runAsyncWithProgress)
                progress.removeFromParent()
            }
            result
        }
    }
}

/**
 * Covers node with overlay (by default - an instance of [MaskPane]) until [latch] is released by another thread.
 * It's useful when more control over async execution is needed, like:
 * * A task already have its thread and overlay should be visible until some callback is invoked (it should invoke
 * [CountDownLatch.countDown] or [Latch.release]) in order to unlock UI. Keep in mind that if [latch] is not released
 * and [timeout] is not set, overlay may never get removed.
 * * An overlay should be removed after some time, even if task is getting unresponsive (use [timeout] for this).
 * Keep in mind that this timeout applies to overlay only, not the latch itself.
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
 * # Example 1
 * The simplest case: overlay is visible for two seconds - until latch release. Replace [Thread.sleep] with any
 * blocking action. Manual thread creation is for the sake of the example only.
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
 *
 * # Example 2
 * The latch won't be released until both workers are done. In addition, until workers are done, button will stay
 * disabled. New latch has to be created and rebound every time.
 *
 * ```kotlin
 * val latch = Latch(2)
 * root.runAsyncWithOverlay(latch)
 * button.disableWhen(latch.lockedProperty())
 * runAsync(worker1.work(); latch.countDown())
 * runAsync(worker2.work(); latch.countDown())
 */
@JvmOverloads
fun Node.runAsyncWithOverlay(latch: CountDownLatch, timeout: Duration? = null, overlayNode: Node = MaskPane()): Task<Boolean> {
    return if (timeout == null) {
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
    overlayContainer.children.add(0, this)

    return task {
        try {
            op()
        } finally {
            runLater { overlayContainer.replaceWith(this@runAsyncWithOverlay) }
        }
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
     * Locked state of this latch exposed as a property. Keep in mind that latch instance can be used only once, so
     * this property has to rebound every time.
     */
    fun lockedProperty(): ReadOnlyBooleanProperty = lockedProperty.readOnlyProperty

    /**
     * Locked state of this latch. `true` if and only if [CountDownLatch.getCount] is greater than `0`.
     * Once latch is released it changes to `false` permanently.
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

class FXTimerTask(val op: () -> Unit, val timer: Timer) : TimerTask() {
    private val internalRunning = ReadOnlyBooleanWrapper(false)
    val runningProperty: ReadOnlyBooleanProperty get() = internalRunning.readOnlyProperty
    val running: Boolean get() = runningProperty.value

    private val internalCompleted = ReadOnlyBooleanWrapper(false)
    val completedProperty: ReadOnlyBooleanProperty get() = internalCompleted.readOnlyProperty
    val completed: Boolean get() = completedProperty.value

    override fun run() {
        internalRunning.value = true
        Platform.runLater {
            try {
                op()
            } finally {
                internalRunning.value = false
                internalCompleted.value = true
            }
        }
    }
}

class FXTask<T>(val status: TaskStatus? = null, val func: FXTask<*>.() -> T) : Task<T>() {
    private var internalCompleted = ReadOnlyBooleanWrapper(false)
    val completedProperty: ReadOnlyBooleanProperty get() = internalCompleted.readOnlyProperty
    val completed: Boolean get() = completedProperty.value

    override fun call() = func(this)

    init {
        status?.item = this
    }

    override fun succeeded() {
        internalCompleted.value = true
    }

    override fun failed() {
        internalCompleted.value = true
    }

    override fun cancelled() {
        internalCompleted.value = true
    }

    override public fun updateProgress(workDone: Long, max: Long) {
        super.updateProgress(workDone, max)
    }

    override public fun updateProgress(workDone: Double, max: Double) {
        super.updateProgress(workDone, max)
    }

    @Suppress("UNCHECKED_CAST")
    fun value(v: Any) {
        super.updateValue(v as T)
    }

    override public fun updateTitle(t: String?) {
        super.updateTitle(t)
    }

    override public fun updateMessage(m: String?) {
        super.updateMessage(m)
    }

}

open class TaskStatus : ItemViewModel<FXTask<*>>() {
    val running: ReadOnlyBooleanProperty = bind { SimpleBooleanProperty().apply { if (item != null) bind(item.runningProperty()) } }
    val completed: ReadOnlyBooleanProperty = bind { SimpleBooleanProperty().apply { if (item != null) bind(item.completedProperty) } }
    val message: ReadOnlyStringProperty = bind { SimpleStringProperty().apply { if (item != null) bind(item.messageProperty()) } }
    val title: ReadOnlyStringProperty = bind { SimpleStringProperty().apply { if (item != null) bind(item.titleProperty()) } }
    val progress: ReadOnlyDoubleProperty = bind { SimpleDoubleProperty().apply { if (item != null) bind(item.progressProperty()) } }
}