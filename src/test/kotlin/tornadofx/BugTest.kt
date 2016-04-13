package tornadofx

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.Separator
import javafx.scene.layout.StackPane

class MyApp : SingleViewApp("Hello world") {
    override val root = StackPane()

    init {
        with(root) {
            val choices: ObservableList<Any> = FXCollections.observableArrayList("Hello", Separator(), "Bello")
            choicebox(choices, { x, y, z ->
                println(z)
            })

        }
    }

}