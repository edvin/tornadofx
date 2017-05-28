package tornadofx.testapps

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.util.converter.IntegerStringConverter
import tornadofx.*
import java.time.LocalDateTime

class AwaitUntilTest : View("Await Until Test") {
    val number = SimpleObjectProperty<Int>()
    val flag = SimpleBooleanProperty(false)
    val output = SimpleStringProperty("")

    override val root = borderpane {
        top {
            toolbar {
                label("Number:")
                textfield(number, IntegerStringConverter())
                button("Wait until 42").action {
                    number.awaitUntil { it == 42 }
                    output.value += "Number is now 42\n"
                }
                button("Wait until true").action {
                    runLater(2.seconds) {
                        flag.set(true)
                    }
                    flag.awaitUntil()
                    output.value += "Flag is true\n"
                }
                button("Look alive").action {
                    output.value += "Alive at ${LocalDateTime.now()}\n"
                }
            }
        }
        center {
            textarea(output)
        }
    }
}

class AwaitUntilApp : App(AwaitUntilTest::class)
