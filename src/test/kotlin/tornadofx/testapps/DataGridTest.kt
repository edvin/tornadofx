package tornadofx.testapps

import javafx.collections.FXCollections
import javafx.scene.control.SelectionMode
import tornadofx.*
import tornadofx.Stylesheet.Companion.selected
import tornadofx.testapps.DataGridTestApp.Companion.images

class DataGridTestApp : App(DataGridTest::class, DataGridStyles::class) {
    companion object {
        val images = mapOf(
                "kittens" to listOf(
                        "http://i.imgur.com/DuFZ6PQb.jpg",
                        "http://i.imgur.com/o2QoeNnb.jpg",
                        "http://i.imgur.com/P8wpBvub.jpg",
                        "http://i.imgur.com/a2NDDglb.jpg",
                        "http://i.imgur.com/79C37Scb.jpg",
                        "http://i.imgur.com/N0fPCU2b.jpg",
                        "http://i.imgur.com/qRfWC9wb.jpg",
                        "http://i.imgur.com/i3crLYcb.jpg"),

                "puppies" to listOf(
                        "http://i.imgur.com/37hCL2Pb.jpg",
                        "http://i.imgur.com/v9vBE67b.jpg",
                        "http://i.imgur.com/koQoEExb.jpg",
                        "http://i.imgur.com/qRfWC9wb.jpg",
                        "http://i.imgur.com/ftDV8oJb.jpg")

        )
    }
}

class DataGridTest : View("DataGrid") {
    var datagrid: DataGrid<String> by singleAssign()
    val list = FXCollections.observableArrayList<String>()

    override val root = borderpane {
        left {
            vbox {
                combobox(values = images.keys.toList()) {
                    promptText = "Select images"
                    valueProperty().onChange {
                        list.setAll(images[it])
                    }
                    shortcut("k") { value = "kittens" }
                    shortcut("p") { value = "puppies" }
                }
                button("Add").action {
                    list.add("http://i.imgur.com/bvqTBT0b.jpg")
                }
            }
        }
        center {
            datagrid = datagrid(list) {
                setPrefSize(550.0, 550.0)

                selectionModel.selectionMode = SelectionMode.SINGLE
                //maxCellsInRow = 3
                //maxRows = 3
                cellWidth = 164.0
                cellHeight = 164.0

                cellCache {
                    imageview(it, true)
                }

                onUserSelect(1) {
                    println("Selected $it")
                }
            }
        }
        bottom {
            label(stringBinding(datagrid.selectionModel.selectedItems) { joinToString(", ") })
            button("Remove from index 2").action {
                list.removeAt(2)
            }
        }
    }

}

class DataGridStyles : SimpleStylesheet({
    datagridCell and selected {
        opacity = 0.7
    }
})