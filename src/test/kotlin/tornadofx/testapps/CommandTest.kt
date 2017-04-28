package tornadofx.testapps

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType.INFORMATION
import tornadofx.*

class CommandTestApp : App(CommandTest::class)

class CommandTest : View("Command test") {
    val ctrl: CommandController by inject()
    val nameProperty = SimpleStringProperty()
    val numberProperty = SimpleDoubleProperty()

    override val root = borderpane {
        top {
            menubar {
                menu("Talk") {
                    item("Say hello").command = ctrl.helloCommand
                }
            }
        }
        center {
            vbox(20) {
                minWidth = 600.0
                paddingAll = 50

                hbox(10) {
                    alignment = Pos.CENTER

                    label("Name:")
                    textfield(nameProperty)

                    button {
                        command = ctrl.helloCommand
                        commandParameterProperty.bind(nameProperty)
                    }

                    button("Call action off of UI thread").action {
                        runAsync {
                            ctrl.helloCommand.execute("Some Name")
                        }
                    }
                }
                hbox(10) {
                    alignment = Pos.CENTER
                    label("Enter number:")
                    textfield(numberProperty)
                    hyperlink("Calculate square root") {
                        command = ctrl.squareRootCommand
                        commandParameterProperty.bind(numberProperty)
                    }
                    label("Square root:")
                    label(ctrl.squareRootResult)
                }
            }
        }
    }
}

class CommandController : Controller() {
    val helloCommand = Command(this::hello, ui = true, title = "Say hello")
    val squareRootCommand = Command(this::squareRoot)
    val squareRootResult = SimpleIntegerProperty()

    private fun hello(name: String?) {
        Alert(INFORMATION, "Hello, ${name ?: "there"}").showAndWait()
    }

    private fun squareRoot(value: Double) {
        squareRootResult.value = Math.sqrt(value).toInt()
    }
}
