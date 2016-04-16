package tornadofx

import javafx.scene.control.Alert
import javafx.scene.control.ButtonType

/**
 * Show an alert dialog of the given type with the given header and content.
 * You can override the default buttons for the alert type and supply an optional
 * function that will run when a button is clicked. The function runs in the scope
 * of the alert dialog and is passed the button that was clicked.
 */
fun alert(type: Alert.AlertType,
          header: String,
          content: String,
          vararg buttons: ButtonType,
          actionFn: (Alert.(ButtonType) -> Unit)? = null) {

    val alert = Alert(type, content, *buttons)
    alert.headerText = header
    val buttonClicked = alert.showAndWait()
    buttonClicked.ifPresent { actionFn?.invoke(alert, buttonClicked.get()) }
}
