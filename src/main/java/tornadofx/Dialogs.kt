package tornadofx

import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.stage.Window
import tornadofx.FileChooserMode.*
import tornadofx.Stylesheet.Companion.title
import tornadofx.WizardStyles.Companion.buttons
import java.io.File

/**
 * Show a confirmation dialog and execute the given action if confirmButton is clicked. The button types
 * of the confirmButton and cancelButton are configurable.
 */
fun confirm(header: String, content: String = "", confirmButton: ButtonType = ButtonType.OK, cancelButton: ButtonType = ButtonType.CANCEL, owner: Window? = null, title: String? = null, actionFn: () -> Unit) {
    alert(Alert.AlertType.CONFIRMATION, header, content, confirmButton, cancelButton, owner = owner, title = title) {
        if (it == confirmButton) actionFn()
    }
}

/**
 * Show an alert dialog of the given type with the given header and content.
 * You can override the default buttons for the alert type and supply an optional
 * function that will run when a button is clicked. The function runs in the scope
 * of the alert dialog and is passed the button that was clicked.
 *
 * You can optionally pass an owner window parameter.
 */
fun alert(type: Alert.AlertType,
          header: String,
          content: String? = null,
          vararg buttons: ButtonType,
          owner: Window? = null,
          title: String? = null,
          actionFn: Alert.(ButtonType) -> Unit = {}): Alert {

    val alert = Alert(type, content ?: "", *buttons)
    title?.let { alert.title = it }
    alert.headerText = header
    owner?.also { alert.initOwner(it) }
    val buttonClicked = alert.showAndWait()
    buttonClicked.ifPresent { actionFn(alert, buttonClicked.get()) }
    return alert
}

fun warning(header: String, content: String? = null, vararg buttons: ButtonType, owner: Window? = null, title: String? = null, actionFn: Alert.(ButtonType) -> Unit = {}) =
        alert(Alert.AlertType.WARNING, header, content, *buttons, owner = owner, title = title, actionFn = actionFn)

fun error(header: String, content: String? = null, vararg buttons: ButtonType, owner: Window? = null, title: String? = null, actionFn: Alert.(ButtonType) -> Unit = {}) =
        alert(Alert.AlertType.ERROR, header, content, *buttons, owner = owner, title = title, actionFn = actionFn)

fun information(header: String, content: String? = null, vararg buttons: ButtonType, owner: Window? = null, title: String? = null, actionFn: Alert.(ButtonType) -> Unit = {}) =
        alert(Alert.AlertType.INFORMATION, header, content, *buttons, owner = owner, title = title, actionFn = actionFn)

fun confirmation(header: String, content: String? = null, vararg buttons: ButtonType, owner: Window? = null, title: String? = null, actionFn: Alert.(ButtonType) -> Unit = {}) =
        alert(Alert.AlertType.CONFIRMATION, header, content, *buttons, owner = owner, title = title, actionFn = actionFn)

enum class FileChooserMode { None, Single, Multi, Save }

/**
 * Ask the user to select one or more files from a file chooser dialog. The mode will dictate how the dialog works,
 * by allowing single selection, multi selection or save functionality. The file dialog will only allow files
 * that match one of the given ExtensionFilters.
 *
 * This function blocks until the user has made a selection, and can optionally block a specified owner Window.
 *
 * If the user cancels, the returnedfile list will be empty.
 */
fun chooseFile(title: String? = null, filters: Array<FileChooser.ExtensionFilter>, mode: FileChooserMode = Single, owner: Window? = null, op: FileChooser.() -> Unit = {}): List<File> {
    val chooser = FileChooser()
    if (title != null) chooser.title = title
    chooser.extensionFilters.addAll(filters)
    op(chooser)
    return when (mode) {
        Single -> {
            val result = chooser.showOpenDialog(owner)
            if (result == null) emptyList() else listOf(result)
        }
        Multi -> chooser.showOpenMultipleDialog(owner) ?: emptyList()
        Save -> {
            val result = chooser.showSaveDialog(owner)
            if (result == null) emptyList() else listOf(result)
        }
        else -> emptyList()
    }
}

fun chooseDirectory(title: String? = null, initialDirectory: File? = null, owner: Window? = null, op: DirectoryChooser.() -> Unit = {}): File? {
    val chooser = DirectoryChooser()
    if (title != null) chooser.title = title
    if (initialDirectory != null) chooser.initialDirectory = initialDirectory
    op(chooser)
    return chooser.showDialog(owner)
}

fun Dialog<*>.toFront() = (dialogPane.scene.window as? Stage)?.toFront()