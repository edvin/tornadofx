package tornadofx.testapps

import tornadofx.*

class ReloadStylesInModal : App(MainView::class, Styles::class) {
    override fun init() {
        reloadStylesheetsOnFocus()
    }

    class MainView : View() {
        override val root = hbox {
            button("Open").action {
                find(MyModal::class).openModal()
            }
        }
    }

    class MyModal : Fragment() {
        override val root = vbox {
            label("My label")
            button("Close").action {
                close()
            }
        }
    }

    class Styles : Stylesheet(Base::class)

    class Base : Stylesheet() {
        init {
            button {
                fontSize = 30.px
            }
            label {
                fontSize = 30.px
            }
        }
    }
}

