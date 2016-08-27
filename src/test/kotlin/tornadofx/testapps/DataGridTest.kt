package tornadofx.testapps

import tornadofx.*

class DataGridTestApp : App(DataGridTest::class, DataGridStyles::class)

class DataGridTest : View("DataGrid") {
    val kittens = listOf(
            "http://i.imgur.com/DuFZ6PQb.jpg",
            "http://i.imgur.com/o2QoeNnb.jpg",
            "http://i.imgur.com/P8wpBvub.jpg",
            "http://i.imgur.com/a2NDDglb.jpg",
            "http://i.imgur.com/79C37Scb.jpg",
            "http://i.imgur.com/N0fPCU2b.jpg",
            "http://i.imgur.com/qRfWC9wb.jpg",
            "http://i.imgur.com/i3crLYcb.jpg")

    override val root = datagrid(kittens) {
        setPrefSize(530.0, 530.0)

        cellWidth = 160.0
        cellHeight = 160.0

        cachedGraphic {
            imageview(it, true)
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
