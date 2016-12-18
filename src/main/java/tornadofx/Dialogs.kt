package tornadofx

import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Window
import tornadofx.FileChooserMode.*
import java.io.File

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
          actionFn: (Alert.(ButtonType) -> Unit)? = null): Alert {

    val alert = Alert(type, content, *buttons)
    alert.headerText = header
    val buttonClicked = alert.showAndWait()
    buttonClicked.ifPresent { actionFn?.invoke(alert, buttonClicked.get()) }
    return alert
}

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
fun chooseFile(title: String? = null, filters: Array<FileChooser.ExtensionFilter>, mode: FileChooserMode = Single, owner: Window? = null, op: (FileChooser.() -> Unit)? = null): List<File> {
    val chooser = FileChooser()
    if (title != null) chooser.title = title
    chooser.extensionFilters.addAll(filters)
    op?.invoke(chooser)
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

fun chooseDirectory(title: String? = null, initialDirectory: File? = null, owner: Window? = null, op: (DirectoryChooser.() -> Unit)? = null): File? {
    val chooser = DirectoryChooser()
    if (title != null) chooser.title = title
    if (initialDirectory != null) chooser.initialDirectory = initialDirectory
    op?.invoke(chooser)
    return chooser.showDialog(owner)
}