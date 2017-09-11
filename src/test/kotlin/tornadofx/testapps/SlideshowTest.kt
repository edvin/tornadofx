package tornadofx.testapps

import tornadofx.*

class SlideshowTestApp : App(SlideshowTest::class, NewViewTransitionStyles::class)

class SlideshowTest : View("Slideshow") {
    override val root = slideshow {
        slide<Slide1>()
        slide<Slide2>(ViewTransition.Fade(0.3.seconds))
        slide<Slide3>()
    }
}

class Slide1 : View("Slide 1") {
    override val root = stackpane {
        label(titleProperty)
        addClass(NewViewTransitionStyles.box, NewViewTransitionStyles.blue)
    }
}

class Slide2 : View("Slide 2") {
    override val root = stackpane {
        label(titleProperty)
        addClass(NewViewTransitionStyles.box, NewViewTransitionStyles.red)
    }
}

class Slide3 : View("Slide 3") {
    override val root = stackpane {
        label(titleProperty)
        addClass(NewViewTransitionStyles.box, NewViewTransitionStyles.blue)
    }
}
