package tornadofx.testapps

import tornadofx.*

class ReloadStylesInModal : App(MainView::class, Styles::class) {
    class MainView : SimpleView({
        hbox {
            button("Open").action {
                find<MyModal>().openModal()
            }
        }
    })

    class MyModal : SimpleFragment({
        vbox {
            label("My label")
            button("Close").action {
                close()
            }
        }
    })

    class Styles : Stylesheet(Base::class)

    class Base : SimpleStylesheet({
        button {
            fontSize = 30.px
        }
        label {
            fontSize = 30.px
        }
    })
}

