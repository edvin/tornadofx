package tornadofx

import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyIntegerProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.BorderPane
import kotlin.reflect.KClass

class Slideshow(val scope: Scope = DefaultScope) : BorderPane() {
    val slides: ObservableList<Slide> = FXCollections.observableArrayList<Slide>()

    val defaultTransitionProperty: ObjectProperty<ViewTransition> = SimpleObjectProperty(ViewTransition.Slide(0.5.seconds, ViewTransition.Direction.LEFT))
    var defaultTransition: ViewTransition? by defaultTransitionProperty

    val currentSlideProperty: ObjectProperty<Slide?> = SimpleObjectProperty()
    var currentSlide by currentSlideProperty

    private val realIndexProperty = SimpleIntegerProperty(-1)
    val indexProperty: ReadOnlyIntegerProperty = ReadOnlyIntegerProperty.readOnlyIntegerProperty(realIndexProperty)
    val index by indexProperty

    fun slide(view: KClass<out UIComponent>, transition: ViewTransition? = null) = slides.addAll(Slide(view, transition))

    fun next(): Boolean {
        if (index + 1 >= slides.size) return false
        goto(slides[index + 1], true)
        return true
    }

    fun previous(): Boolean {
        if (index <= 0) return false
        goto(slides[index - 1], false)
        return true
    }

    init {
        slides.addListener { change: ListChangeListener.Change<out Slide> ->
            while (change.next()) {
                if (change.wasAdded() && currentSlide == null)
                    next()
            }
        }

        isFocusTraversable = true

        sceneProperty().onChange {
            scene?.addEventFilter(KeyEvent.KEY_PRESSED) {
                if (!it.isConsumed && it.isAltDown) {
                    @Suppress("NON_EXHAUSTIVE_WHEN")
                    when (it.code) {
                        KeyCode.LEFT -> previous()
                        KeyCode.RIGHT -> next()
                    }
                }
            }
        }
    }

    private fun goto(slide: Slide, forward: Boolean) {
        val nextUI = slide.getUI(scope)

        if (center == null) {
            center = nextUI.root
        } else {
            val transition = if (forward) slide.transition ?: defaultTransition else null
            center.replaceWith(nextUI.root, transition)
        }

        val delta = if (forward) 1 else -1
        val newIndex = index + delta
        currentSlide = slides[newIndex]
        realIndexProperty.value = newIndex
    }

    class Slide(val view: KClass<out UIComponent>, val transition: ViewTransition? = null) {
        private var ui: UIComponent? = null
        fun getUI(scope: Scope): UIComponent {
            if (ui == null) ui = find(view, scope)
            return ui!!
        }
    }
}