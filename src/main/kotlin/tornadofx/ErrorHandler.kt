package tornadofx

import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.VBox

import java.io.ByteArrayOutputStream
import java.io.PrintWriter

class DefaultErrorHandler : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, error: Throwable) {
        Platform.runLater {
            val alert = Alert(Alert.AlertType.ERROR)
            val errorMessage = error.message
            alert.title = errorMessage ?: "An error occured"
            alert.isResizable = true

            val pos = error.stackTrace[0].toString()
            alert.headerText = "Error in " + pos

            val textarea = TextArea().apply {
                prefRowCount = 20
                prefColumnCount = 50
                text = stringFromError(error)
            }

            val cause = Label(if (error.cause != null) error.cause?.message else "").apply {
                style = "-fx-font-weight: bold"
            }

            alert.dialogPane.content = VBox(cause, textarea)
            alert.showAndWait()
        }
    }

}

private fun stringFromError(e: Throwable): String {
    val out = ByteArrayOutputStream()
    val writer = PrintWriter(out)
    e.printStackTrace(writer)
    writer.close()
    return out.toString()
}