package tornadofx.testapps

import javafx.scene.control.Label
import javafx.scene.paint.Color
import tornadofx.*
import tornadofx.ViewTransition.Fade
import tornadofx.ViewTransition.NewsFlash

class SlideshowTestApp : App(SlideshowTest::class)

class SlideshowTest : View("Slideshow") {
    override val root = slideshow {
        setPrefSize(400.0, 400.0)
        slide(Slide1::class)
        slide(Slide2::class, NewsFlash(.5.seconds))
        slide(Slide3::class, Fade(.5.seconds))
    }
}

class Slide1 : View("Slide 1") {
    override val root = stackpane {
        textfield(titleProperty)
    }
}

class Slide2 : View("Slide 2") {
    override val root = stackpane { label(titleProperty) }

    init {
        root.style {
            backgroundColor += Color.LIGHTCORAL
        }
    }
}

class Slide3 : View("Slide 3") {
    override val root = stackpane { label(titleProperty) }
}
