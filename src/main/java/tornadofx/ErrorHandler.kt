package tornadofx

import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType.ERROR
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.util.logging.Level
import java.util.logging.Logger

class DefaultErrorHandler : Thread.UncaughtExceptionHandler {
    val log: Logger = Logger.getLogger("ErrorHandler")

    class ErrorEvent(val thread: Thread, val error: Throwable) {
        internal var consumed = false
        fun consume() {
            consumed = true
        }
    }

    companion object {
        // By default, all error messages are shown. Override to decide if certain errors should be handled another way.
        // Call consume to avoid error dialog.
        var filter: (ErrorEvent) -> Unit = {}
    }

    override fun uncaughtException(t: Thread, error: Throwable) {
        log.log(Level.SEVERE, "Uncaught error", error)

        if (isCycle(error)) {
            log.log(Level.INFO, "Detected cycle handling error, aborting.", error)
        } else {
            val event = ErrorEvent(t, error)
            filter(event)

            if (!event.consumed) {
                event.consume()
                runLater { showErrorDialog(error) }
            }
        }
    }

    private fun isCycle(error: Throwable): Boolean = error.stackTrace?.any { it.className.startsWith("${javaClass.name}\$uncaughtException$") } == true

    private fun showErrorDialog(error: Throwable) {
        val cause = Label(error.cause?.message.orEmpty()).apply { style { fontWeight = FontWeight.BOLD } }

        val textarea = TextArea().apply {
            prefRowCount = 20
            prefColumnCount = 50
            text = stringFromError(error)
        }

        Alert(ERROR).apply {
            title = error.message ?: "An error occurred"
            isResizable = true
            headerText = if (error.stackTrace?.isNotEmpty() == true) "Error in ${error.stackTrace[0]}" else "Error"
            dialogPane.content =
                    vbox {
                        add(cause)
                        if (error is RestException) {
                            try {
                                title = "HTTP Request Error: $title"
                                form {
                                    fieldset(error.message) {
                                        val response = error.response
                                        if (response != null) {
                                            field("Status") {
                                                label("${response.statusCode} ${response.reason}")
                                            }

                                            val c = response.text()

                                            if (c != null) {
                                                tabpane {
                                                    background = Color.TRANSPARENT.asBackground()

                                                    tab("Plain text") { textarea(c) }
                                                    tab("HTML") {
                                                        if (response.header("Content-Type")?.contains("html", true) == true) select()
                                                        webview { engine.loadContent(c) }
                                                    }
                                                    tab("Stacktrace") { add(textarea) }

                                                    tabs.withEach { isClosable = false }
                                                }
                                            } else {
                                                add(textarea)
                                            }
                                        } else {
                                            add(textarea)
                                        }
                                    }
                                }
                            } catch (_: Exception) {
                                add(textarea)
                            }
                        } else {
                            add(textarea)
                        }
                    }
            showAndWait()
        }
    }
}

private fun stringFromError(e: Throwable): String {
    val out = ByteArrayOutputStream()
    PrintWriter(out).use { e.printStackTrace(it) }
    return out.toString()
}
