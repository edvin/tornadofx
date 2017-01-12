package tornadofx.testapps

import tornadofx.*

class InlineFragmentTestApp : App(InlineFragmentTestView::class)

class InlineFragmentTestView : View("Inline Fragment") {
    override val root = vbox {
        label("Added the old fashioned way")
        button("Create fragment") {
            setOnAction {
                adhocWindow("Ooh, so inline!") {
                    button("Click to close") {
                        setPrefSize(100.0, 100.0)
                        setOnAction { closeModal() }
                    }
                }
            }
        }
    }
}