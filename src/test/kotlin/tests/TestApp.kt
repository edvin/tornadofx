package tests

import tests.views.TestView
import tornadofx.App
import tornadofx.JsonModel
import tornadofx.importStylesheet

class Customer : JsonModel {

}
class TestApp : App() {
    override val primaryView = TestView::class

    init {
        importStylesheet("/tests/styles.css")
    }

}