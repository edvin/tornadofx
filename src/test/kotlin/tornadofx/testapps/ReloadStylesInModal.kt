package tornadofx.testapps

import tornadofx.*

class ReloadStylesInModal : App(MainView::class, Styles::class) {
    class MainView : View() {
        override val root = hbox {
            button("Open").action {
                find<MyModal>().openModal()
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

