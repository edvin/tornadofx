package tornadofx.testapps

import tornadofx.*

class SlideshowTestApp : App(SlideshowTest::class, NewViewTransitionStyles::class)

class SlideshowTest : SimpleView("Slideshow", {
    slideshow {
        slide<Slide1>()
        slide<Slide2>(ViewTransition.Fade(0.3.seconds))
        slide<Slide3>()
    }
})

class Slide1 : SimpleView("Slide 1", {
    stackpane {
        label(titleProperty)
        addClass(NewViewTransitionStyles.box, NewViewTransitionStyles.blue)
    }
})

class Slide2 : SimpleView("Slide 2", {
    stackpane {
        label(titleProperty)
        addClass(NewViewTransitionStyles.box, NewViewTransitionStyles.red)
    }
})

class Slide3 : SimpleView("Slide 3", {
    stackpane {
        label(titleProperty)
        addClass(NewViewTransitionStyles.box, NewViewTransitionStyles.blue)
    }
})
