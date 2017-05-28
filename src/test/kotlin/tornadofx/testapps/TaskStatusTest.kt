package tornadofx.testapps

import javafx.beans.property.SimpleStringProperty
import tornadofx.*

class TaskStatusTest : View("Task Status Test") {
    val status = TaskStatus()
    val output = SimpleStringProperty("")

    override val root = borderpane {
        top {
            toolbar {
                button("Run Async").action {
                    output.value += "\n"
                    runAsync(status) {
                        Thread.sleep(1000)
                        "hello"
                    } ui {
                        output.value += "$it\n"
                    }
                }
                button("Run Await").action {
                    output.value += "\n"
                    runAsync(status) {
                        Thread.sleep(1000)
                        "hello 2"
                    } ui {
                        output.value += "$it\n"
                    }
                    status.completed.awaitUntil()
                    output.value += "Await exited\n"
                }
                enableWhen { status.running.not() }
            }
        }
        center {
            textarea(output)
        }
        bottom {
            label("Running...") {
                visibleWhen { status.running }
            }
        }
    }

    override fun onDock() {
        status.completed.onChange {
            output.value += "completed $it\n"
        }
        status.running.onChange {
            output.value += "running $it\n"
        }
    }
}

class TaskStatusApp : App(TaskStatusTest::class)
