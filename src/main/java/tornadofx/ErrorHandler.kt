package tornadofx

import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType.ERROR
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.VBox

import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.util.logging.Level
import java.util.logging.Logger

class DefaultErrorHandler : Thread.UncaughtExceptionHandler {
    val log = Logger.getLogger("ErrorHandler")

    class ErrorEvent(val thread: Thread, val error: Throwable) {
        internal var consumed = false
        fun consume() {
            consumed = true
        }
    }

    companion object {
        // By default, all error messages are shown. Override to decide if certain errors should be handled another way.
        // Call consume to avoid error dialog.
        var filter: (ErrorEvent) -> Unit = { }
    }

    override fun uncaughtException(t: Thread, error: Throwable) {
        log.log(Level.SEVERE, "Uncaught error", error)

        val event = ErrorEvent(t, error)
        filter(event)
        if (!event.consumed) {
            Platform.runLater {
                val cause = Label(if (error.cause != null) error.cause?.message else "").apply {
                    style = "-fx-font-weight: bold"
                }

                val textarea = TextArea().apply {
                    prefRowCount = 20
                    prefColumnCount = 50
                    text = stringFromError(error)
                }

                Alert(ERROR).apply {
                    title = error.message ?: "An error occured"
                    isResizable = true
                    headerText = if (error.stackTrace?.isEmpty() ?: true) "Error" else "Error in " + error.stackTrace[0].toString()
                    dialogPane.content = VBox(cause, textarea)
                    showAndWait()
                }
            }
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