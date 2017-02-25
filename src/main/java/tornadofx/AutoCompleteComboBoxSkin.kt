package tornadofx

import com.sun.javafx.scene.control.behavior.ComboBoxListViewBehavior
import com.sun.javafx.scene.control.skin.ComboBoxPopupControl
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.ComboBox
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.util.Callback


/**
 * Extension function to combobox to add autocomplete capabilities
 * Accept in parameter a callback to create the autocomplete list based on input text
 * Default filter use the string produced by the converter of combobox and search with contains ignore case the occurrence of typed text
 */
fun <T> ComboBox<T>.makeAutocompletable(autoCompleteFilter: ((String) -> List<T>)? = null) {
    skin = AutoCompleteComboBoxSkin(this, autoCompleteFilter)
}

/**
 * Auto Complete support for combobox
 * Accept a call back to make custom filter
 * Default filter use the string produced by the converter of combobox and search with contains ignore case the occurrence of typed text
 * Created by anouira on 15/02/2017.
 */
class AutoCompleteComboBoxSkin<T>(val comboBox: ComboBox<T>, autoCompleteFilter: ((String) -> List<T>)?) : EventHandler<KeyEvent>, ComboBoxPopupControl<T>(comboBox, ComboBoxListViewBehavior(comboBox)) {
    var autoCompleteFilter_: (String) -> List<T> = autoCompleteFilter ?: {
        comboBox.items.filter { current -> comboBox.converter.toString(current).contains(it, true) }
    }
    private var moveCaretToPos = false
    private var caretPos: Int = 0
    private val listView = ListView<T>(comboBox.items)
    private var lastText: String = ""
    private var skipValueUpdate = false

    init {
        with(comboBox) {
            val valueTmp = value
            isEditable = true
            onKeyReleased = this@AutoCompleteComboBoxSkin
            value = valueTmp
            arrowButton.setOnMouseClicked {
                if (isShowing) {
                    resetFilter()
                }
            }
        }
        listView.selectionModel.selectedItemProperty().onChange {
            if (!skipValueUpdate) comboBox.value = it
        }
        listView.onUserSelect(clickCount = 1) {
            comboBox.value = it
            comboBox.hide()
            updateDisplayArea()
        }
        val cellFormat: (ListCell<T>.(T) -> Unit) = {
            if (it is Node) {
                graphic = it
            } else if (it != null) {
                if (converter != null)
                    text = converter.toString(it)
                else if (it is String)
                    text = it
                else
                    text = it.toString()
            }
        }
        listView.properties["tornadofx.cellFormat"] = cellFormat
        listView.cellFactory = Callback { SmartListCell(DefaultScope, listView) }
    }

    private fun resetFilter() {
        listView.items = comboBox.items
        lastText = ""
    }

    override fun handle(event: KeyEvent) {
        val text = comboBox.editor.text
        val inputChanged = lastText != text
        lastText = text
        val code = event.code
        val isControlDown = event.isControlDown
        val isShiftDown = event.isShiftDown
        val caretPosition = comboBox.editor.caretPosition

        if (isControlDown) {
            when (code) {
                KeyCode.V -> Unit
                else -> return
            }
        }
        if (isShiftDown) {
            when (code) {
                KeyCode.LEFT, KeyCode.RIGHT, KeyCode.HOME, KeyCode.END -> return
                else -> Unit
            }
        }

        when (code) {
            KeyCode.DOWN, KeyCode.UP -> {
                if (!comboBox.isShowing) {
                    comboBox.show()
                } else {
                    caretPos = -1
                    moveCaret(text.length)
                }
                return
            }
            KeyCode.BACK_SPACE, KeyCode.DELETE -> {
                moveCaretToPos = true
                caretPos = comboBox.editor.caretPosition
            }
            KeyCode.ESCAPE -> {
                if (comboBox.isShowing) {
                    comboBox.hide()
                }
                return
            }
            KeyCode.ENTER -> {
                if (comboBox.isShowing) {
                    comboBox.hide()
                }
                if (!listView.selectionModel.isEmpty) {
                    comboBox.value = listView.selectedItem
                }
                return
            }
            KeyCode.RIGHT, KeyCode.LEFT, KeyCode.HOME, KeyCode.END, KeyCode.TAB, KeyCode.SHIFT, KeyCode.CONTROL -> return
            else -> Unit
        }

        if (inputChanged) {
            val list = autoCompleteFilter_.invoke(text)
            listView.items = (list as? ObservableList<T>) ?: list.observable()
            listView.requestLayout()
            comboBox.editor.text = text
            if (!moveCaretToPos) {
                caretPos = -1
            }
            moveCaret(caretPosition)
            if (list.isEmpty()) {
                comboBox.hide()
            } else {
                comboBox.show()
                if (listView.selectedItem == null) {
                    skipValueUpdate = true
                    listView.selectionModel.selectFirst()
                    skipValueUpdate = false
                }
                listView.requestFocus()
            }
        }
    }

    private fun moveCaret(textLength: Int) {
        if (caretPos == -1) {
            comboBox.editor.positionCaret(textLength)
        } else {
            comboBox.editor.positionCaret(caretPos)
        }
        moveCaretToPos = false
    }

    override fun getDisplayNode(): Node? {
        updateDisplayNode()
        return editableInputNode
    }

    override fun getPopupContent() = listView

    override fun getEditor() = if (skinnable.isEditable) (skinnable as ComboBox<T>).editor else null

    override fun getConverter() = (skinnable as ComboBox<T>).converter
}