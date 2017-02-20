package tornadofx

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.scene.control.ComboBox
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.util.Callback


/**
 * Extension function to combobox to add autocomplete capabilities
 * Accept in parameter a callback to create the autocomplete list based on input text
 * Default filter use the string produced by the converter of combobox and search with contains ignore case the occurrence of typed text
 */
fun <T> ComboBox<T>.makeAutocompletable(autoCompleteFilter : ((String) -> List<T>)? = null) = AutoCompleteComboBoxExtension(this, autoCompleteFilter)

/**
 * Auto Complete support for combobox
 * Accept a call back to make custom filter
 * Default filter use the string produced by the converter of combobox and search with contains ignore case the occurrence of typed text
 * Created by anouira on 15/02/2017.
 */
class AutoCompleteComboBoxExtension<T>(val comboBox : ComboBox<T>, autoCompleteFilter: ((String) -> List<T>)?) : EventHandler<KeyEvent> {
    val data: ObservableList<T> = comboBox.items ?: FXCollections.emptyObservableList()
    var autoCompleteFilter_ : (String) -> List<T> = autoCompleteFilter ?: {
        data.filtered { current -> comboBox.converter.toString(current).contains(it, true) }
    }
    private var moveCaretToPos = false
    private var caretPos: Int = 0

    init {
        with(this.comboBox) {
            val valueTmp = value
            isEditable = true
            onKeyPressed = EventHandler<KeyEvent> { hide() }
            onKeyReleased = this@AutoCompleteComboBoxExtension
            value = valueTmp
        }
    }

    override fun handle(event: KeyEvent?) {
        val code = event?.code
        val isControlDown = event?.isControlDown ?: false
        val isShiftDown = event?.isShiftDown ?: false
        val text = comboBox.editor.text
        val caretPosition = comboBox.editor.caretPosition

        if(isControlDown) {
            when (code) {
                KeyCode.V -> Unit
                else -> return
            }
        }
        if(isShiftDown) {
            when (code) {
                KeyCode.LEFT,KeyCode.RIGHT,KeyCode.HOME,KeyCode.END -> return
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
            KeyCode.RIGHT, KeyCode.LEFT, KeyCode.HOME, KeyCode.END, KeyCode.TAB, KeyCode.SHIFT, KeyCode.CONTROL, KeyCode.ENTER, KeyCode.ESCAPE ->  return
            else -> Unit
        }

        val list = autoCompleteFilter_.invoke(text)

        comboBox.items = (list as? ObservableList<T>) ?: list.observable()
        comboBox.editor.text = text
        if (!moveCaretToPos) {
            caretPos = -1
        }
        moveCaret(caretPosition)
        if (list.isNotEmpty()) {
            comboBox.show()
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

}
