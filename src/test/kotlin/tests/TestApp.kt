package tests

import tests.views.TestView
import tornadofx.App
import tornadofx.importStylesheet

class TestApp : App() {
    override val primaryView = TestView::class

    init {
        importStylesheet("/tests/styles.css")
    }

}