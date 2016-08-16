package tornadofx.testapps

import tornadofx.*

class ReloadStylesInModal : App(MainView::class, Styles::class) {
    override fun init() {
        reloadStylesheetsOnFocus()
    }
}

class MainView : View() {
    override val root = hbox {
        button("Open") {
            setOnAction {
                find(MyModal::class).openModal()
            }
        }
    }
}

class MyModal : Fragment() {
    override val root = vbox {
        label("My label")
        button("Close") {
            setOnAction {
                closeModal()
            }
        }
    }
}

class Styles : Stylesheet() {
    init {
        button {
            fontSize = 20.px
        }
        label {
            fontSize = 20.px
        }
    }
}