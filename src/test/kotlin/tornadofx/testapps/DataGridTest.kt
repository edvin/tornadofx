package tornadofx.testapps

import tornadofx.*
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
                        "http://i.imgur.com/ftDV8oJb.jpg")

        )

    }

}

class DataGridTest : View("DataGrid") {
    var datagrid: DataGrid<String> by singleAssign()

    override val root = borderpane {
        left {
            combobox(values = images.keys.toList()) {
                promptText = "Select images"
                valueProperty().onChange {
                    datagrid.items.setAll(images[it])
                }
            }
        }
        center {
            datagrid = datagrid<String> {
                setPrefSize(530.0, 530.0)

                cellWidth = 160.0
                cellHeight = 160.0

                cachedGraphic {
                    imageview(it, true)
                }
            }
        }
    }

}

class DataGridStyles : Stylesheet() {
    init {
        datagridCell and selected {
            opacity = 0.8
        }
    }
}