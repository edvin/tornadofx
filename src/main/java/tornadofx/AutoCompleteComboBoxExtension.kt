import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.scene.control.ComboBox
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.util.Callback


fun <T> ComboBox<T>.makeAutocompletable() : AutoCompleteComboBoxExtension<T> = AutoCompleteComboBoxExtension<T>(this)



/**
 * Created by anouira on 15/02/2017.
 */
class AutoCompleteComboBoxExtension<T>(private val comboBox : ComboBox<T>,
                                       var autoCompleteFilter : Callback<String, ObservableList<T>> = Callback {
                                           (comboBox.items ?: FXCollections.emptyObservableList()).filtered { current -> comboBox.converter.toString(current).contains(it, true) }
                                       }) : EventHandler<KeyEvent> {
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
                KeyCode.V -> ""
                else -> return
            }
        }
        if(isShiftDown) {
            when (code) {
                KeyCode.LEFT,KeyCode.RIGHT,KeyCode.HOME,KeyCode.END -> return
                else -> ""
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
            KeyCode.RIGHT, KeyCode.LEFT, KeyCode.HOME, KeyCode.END, KeyCode.TAB, KeyCode.SHIFT, KeyCode.CONTROL ->  return
            else -> ""
        }

        val list = autoCompleteFilter.call(text)

        comboBox.items = list
        comboBox.editor.text = text
        if (!moveCaretToPos) {
            caretPos = -1
        }
        moveCaret(caretPosition)
        if (!(list?.isEmpty() ?: true)) {
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