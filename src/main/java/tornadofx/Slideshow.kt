package tornadofx

import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyIntegerProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
import javafx.scene.layout.BorderPane
import javafx.util.Duration
import tornadofx.ViewTransition.Direction.RIGHT
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.reflect.KClass

class Slideshow(val scope: Scope = FX.defaultScope, defaultTimeout: Duration? = null) : BorderPane() {
    val slides: ObservableList<Slide> = FXCollections.observableArrayList<Slide>()

    var defaultTimeoutProperty = SimpleObjectProperty<Duration>(defaultTimeout)
    var defaultTimeout by defaultTimeoutProperty

    val defaultTransitionProperty: ObjectProperty<ViewTransition> = SimpleObjectProperty(ViewTransition.Swap(.3.seconds))
    var defaultTransition: ViewTransition? by defaultTransitionProperty

    val defaultBackTransitionProperty: ObjectProperty<ViewTransition> = SimpleObjectProperty(ViewTransition.Swap(.3.seconds, RIGHT))
    var defaultBackTransition: ViewTransition? by defaultBackTransitionProperty

    val currentSlideProperty: ObjectProperty<Slide?> = SimpleObjectProperty()
    var currentSlide: Slide? by currentSlideProperty

    val nextKeyProperty: ObjectProperty<KeyCombination> = SimpleObjectProperty(KeyCombination.valueOf("Alt+Right"))
    var nextKey: KeyCombination by nextKeyProperty

    val previousKeyProperty: ObjectProperty<KeyCombination> = SimpleObjectProperty(KeyCombination.valueOf("Alt+Left"))
    var previousKey: KeyCombination by previousKeyProperty

    private val realIndexProperty = SimpleIntegerProperty(-1)
    val indexProperty: ReadOnlyIntegerProperty = ReadOnlyIntegerProperty.readOnlyIntegerProperty(realIndexProperty)
    val index by indexProperty

    inline fun <reified T : UIComponent> slide(transition: ViewTransition? = null, timeout: Duration? = defaultTimeout) = slide(T::class, transition, timeout)
    fun slide(view: KClass<out UIComponent>, transition: ViewTransition? = null, timeout: Duration? = defaultTimeout) = slides.addAll(Slide(view, transition, timeout))

    fun hasNext() = index < (slides.size - 1)

    private var timer: Timer? = null
    private var task: TimerTask? = null

    fun next(): Boolean {
        if (!hasNext()) return false
        goto(slides[index + 1], true)
        return true
    }

    fun previous(): Boolean {
        if (index <= 0) return false
        goto(slides[index - 1], false)
        return true
    }

    init {
        showFirstSlideWhenAvailable()
        hookNavigationShortcuts()
        listenForNextSlide()
    }

    private fun listenForNextSlide() {
        currentSlideProperty.onChange { slide ->
            slide?.timeout?.let { timeout ->
                if (timer == null) {
                    timer = Timer("SlideshowTimer${this@Slideshow}")
                }
                task?.cancel()
                task = timerTask {
                    runLater {
                        next()
                    }
                }
                timer!!.schedule(task, timeout.toMillis().toLong())
            }
        }
    }

    private fun hookNavigationShortcuts() {
        sceneProperty().onChange {
            scene?.addEventHandler(KeyEvent.KEY_PRESSED) {
                if (!it.isConsumed) {
                    if (nextKey.match(it)) next()
                    else if (previousKey.match(it)) previous()
                }
            }
        }
    }

    private fun showFirstSlideWhenAvailable() {
        slides.addListener { change: ListChangeListener.Change<out Slide> ->
            while (change.next()) {
                if (change.wasAdded() && currentSlide == null)
                    next()
            }
        }
    }

    private fun goto(slide: Slide, forward: Boolean) {
        val nextUI = slide.getUI(scope)

        // Avoid race conditions if last transition is still in progress
        val centerRightNow = center
        if (centerRightNow == null) {
            center = nextUI.root
        } else {
            val transition = if (forward) slide.transition ?: defaultTransition else defaultBackTransition
            nextUI.root.removeFromParent()
            centerRightNow.replaceWith(nextUI.root, transition)
        }

        val delta = if (forward) 1 else -1
        val newIndex = index + delta
        currentSlide = slides[newIndex]
        realIndexProperty.value = newIndex
    }

    class Slide(val view: KClass<out UIComponent>, val transition: ViewTransition? = null, val timeout: Duration? = null) {
        private lateinit var ui: UIComponent
        fun getUI(scope: Scope): UIComponent {
            if (!::ui.isInitialized) ui = find(view, scope)
            return ui
        }
    }
}