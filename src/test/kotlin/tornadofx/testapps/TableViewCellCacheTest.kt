package tornadofx.testapps

import tornadofx.*

class TableViewCellCacheTestApp : App(TableViewCellCacheTest::class)

class TableViewCellCacheTest : View("TableView CellCache Test") {
    val kittens = DataGridTestApp.images["kittens"] as List<String>

    override val root = tableview(kittens.asObservable()) {
        prefWidth = 300.0

        column("Filename", String::class) {
            value { it.value.substringAfterLast("/") }
        }
        column("Image", String::class) {
            value { it.value }
            cellCache { imageview(it, true) }
            cellFormat {
                prefHeight = 160.0
                prefWidth = 160.0
            }
        }
    }
}

