package tornadofx.testapps

import javafx.collections.FXCollections
import tornadofx.*
import java.time.LocalDateTime

class ShortLongPressTestApp : App(ShortLongPressTest::class)

class ShortLongPressTest : View("Short or long press") {
    val actuations = FXCollections.observableArrayList<String>()

    override val root = borderpane {
        top {
            stackpane {
                paddingAll = 20
                button("Short or longpress me") {
                    longpress { actuations.add("Long at ${LocalDateTime.now()}") }
                    shortpress { actuations.add("Short at ${LocalDateTime.now()}") }
                }
            }
        }
        center {
            listview(actuations)
        }
        bottom {
            label("Hold for 700ms to actuate long action, or less to actuate short action") {
                paddingAll = 20
            }
        }
    }
}
