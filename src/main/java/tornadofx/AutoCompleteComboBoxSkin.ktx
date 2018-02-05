package tornadofx

import com.sun.javafx.scene.control.behavior.ComboBoxListViewBehavior
import com.sun.javafx.scene.control.skin.ComboBoxPopupControl
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.css.PseudoClass
import javafx.event.EventHandler
import javafx.scene.AccessibleRole
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.util.Callback


/**
 * Extension function to combobox to add autocomplete capabilities
 * Accept in parameter a callback to create the autocomplete list based on input text
 * Default filter use the string produced by the converter of combobox and search with contains ignore case the occurrence of typed text
 * @param automaticPopupWidth Use the width required to display all content. Default: true
 */
fun <T> ComboBox<T>.makeAutocompletable(automaticPopupWidth: Boolean = true, autoCompleteFilter: ((String) -> List<T>)? = null) {
    skin = AutoCompleteComboBoxSkin(this, autoCompleteFilter, automaticPopupWidth)
}

interface Resettable {
    fun reset()
}

interface FilterHandler {
    fun handleFilterChange(text : String)
    fun validateSelection()
    fun showSuggestion() : Boolean
    fun hideSuggestion() : Boolean

}

class FilterTooltipHandler(val control : Control, val filterHandler : FilterHandler) : EventHandler<KeyEvent> , Resettable {

    private val filter = SimpleStringProperty("")
    private val tooltip = Tooltip()

    init {
        tooltip.textProperty().bind(filter)
        filter.addListener { _, _, newValue -> handleFilterChanged_(newValue) }
    }

    override fun handle(event: KeyEvent) {
        val code = event.code
        val isControlDown = event.isControlDown
        val isShiftDown = event.isShiftDown
        var filterValue: String = filter.get()

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
                filterHandler.showSuggestion()
            }
            KeyCode.BACK_SPACE -> {
                if(filterValue.isNotBlank()) {
                    filterValue = filterValue.substring(0, filterValue.length - 1)
                }
            }
            KeyCode.ESCAPE -> {
                reset()
                filterHandler.hideSuggestion()
                return
            }
            KeyCode.ENTER -> {
                filterHandler.validateSelection()
                return
            }
            KeyCode.RIGHT, KeyCode.LEFT, KeyCode.HOME, KeyCode.END, KeyCode.TAB, KeyCode.SHIFT, KeyCode.CONTROL -> return
            else -> filterValue += event.text
        }

        filter.value = filterValue
    }

    private fun handleFilterChanged_(newValue: String) {
        filterHandler.handleFilterChange(newValue)
        if(newValue.isNotBlank()) {
            showTooltip()
        } else {
            tooltip.hide()
        }
    }

    private fun showTooltip() {
        if (!tooltip.isShowing) {
            val stage = control.scene.window
            val posX = stage.x + control.localToScene(control.boundsInLocal).minX
            val posY = stage.y + control.localToScene(control.boundsInLocal).minY
            tooltip.show(stage, posX, posY)
        }
    }

    override fun reset() {
        filter.value = ""
        tooltip.hide()
    }

}


class FilterInputTextHandler(val editor : TextField, val filterHandler : FilterHandler) : EventHandler<KeyEvent>, Resettable {
    private var lastText: String = ""

    override fun handle(event: KeyEvent) {
        val text = editor.text
        val inputChanged = lastText != text
        lastText = text
        val code = event.code
        val isControlDown = event.isControlDown
        val isShiftDown = event.isShiftDown
        val caretPosition = editor.caretPosition

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
                if(!filterHandler.showSuggestion()) {
                    editor.positionCaret(text.length)
                }
                return
            }
            KeyCode.ESCAPE -> {
                filterHandler.hideSuggestion()
                return
            }
            KeyCode.ENTER -> {
                filterHandler.validateSelection()
                return
            }
            KeyCode.RIGHT, KeyCode.LEFT, KeyCode.HOME, KeyCode.END, KeyCode.TAB, KeyCode.SHIFT, KeyCode.CONTROL -> return
            else -> Unit
        }

        if (inputChanged) {
            filterHandler.handleFilterChange(text)
            editor.text = text
            editor.positionCaret(caretPosition)
        }
    }

    override fun reset() {
        lastText = ""
    }
}

/**
 * Auto Complete support for combobox
 * Accept a call back to make custom filter
 * Default filter use the string produced by the converter of combobox and search with contains ignore case the occurrence of typed text
 * Created by anouira on 15/02/2017.
 */
open class AutoCompleteComboBoxSkin<T>(val comboBox: ComboBox<T>, autoCompleteFilter: ((String) -> List<T>)?, automaticPopupWidth: Boolean) : ComboBoxPopupControl<T>(comboBox, ComboBoxListViewBehavior(comboBox)), FilterHandler {
    var autoCompleteFilter_: (String) -> List<T> = autoCompleteFilter ?: {
        comboBox.items.filter { current -> comboBox.converter.toString(current).contains(it, true) }
    }

    protected val listView = ListView<T>(comboBox.items)
    private var skipValueUpdate = false
    private var comboBoxItems: ObservableList<T> = if (comboBox.items == null) FXCollections.emptyObservableList<T>() else comboBox.items

    private val filterHandler by lazy<EventHandler<KeyEvent>> {
        if(comboBox.isEditable) {
            FilterInputTextHandler(comboBox.editor, this)

        } else {
            FilterTooltipHandler(comboBox, this)
        }

    }

    init {
        with(comboBox) {
            onKeyReleased = filterHandler
            focusedProperty().onChange { focus -> if(!focus) {
                resetFilter()
                comboBox.hide()
            }}
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
            if(filterHandler is FilterTooltipHandler) {
                resetFilter()
            }
            comboBox.hide()
            updateDisplayArea()
        }
        if (!automaticPopupWidth) {
            // Note that we cannot bind prefWidthProperty because JavaFX sets
            // the preferred width upon reconfiguration. (ComboBoxPopupControl.java:322)
            comboBox.widthProperty().onChange {
                if (!listView.prefWidthProperty().isBound) listView.prefWidth = it
            }
        }
        updateCellFactory()
        updateButtonCell()
        updateValue()
        comboBox.cellFactoryProperty().onChange { updateCellFactory() }
    }

    override fun handleFilterChange(text : String) {
        val list = autoCompleteFilter_.invoke(text)
        listView.items = (list as? ObservableList<T>) ?: list.observable()
        listView.requestLayout()

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

    /** Specific auto complete implementation */
    override fun validateSelection() {
        if (!listView.selectionModel.isEmpty) {
            comboBox.value = listView.selectedItem
        }
        if (comboBox.isShowing) {
            comboBox.hide()
        }
    }

    override fun showSuggestion() : Boolean {
        if(!comboBox.isShowing) {
            comboBox.show()
            return true
        }
        return false
    }

    override fun hideSuggestion() : Boolean {
        if(comboBox.isShowing) {
            comboBox.hide()
            return true
        }
        return false
    }

    private fun updateCellFactory() {
        if (comboBox.cellFactory != null) {
            listView.cellFactory = comboBox.cellFactory
        } else {
            createDefaultCellFactory()
        }
    }

    private fun createDefaultCellFactory() {
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
        (filterHandler as? Resettable)?.reset()
    }

    /** End of Specific auto complete implementation */

    /** Implementation copied from ComboBoxListViewSkin */
    private var buttonCell: ListCell<T>? = null

    // These three pseudo class states are duplicated from Cell
    private val PSEUDO_CLASS_SELECTED = PseudoClass.getPseudoClass("selected")
    private val PSEUDO_CLASS_EMPTY = PseudoClass.getPseudoClass("empty")
    private val PSEUDO_CLASS_FILLED = PseudoClass.getPseudoClass("filled")

    override fun getDisplayNode(): Node? {
        val displayNode: Node
        if (comboBox.isEditable) {
            displayNode = editableInputNode
        } else {
            displayNode = buttonCell as Node
        }

        updateDisplayNode()

        return displayNode
    }

    private fun updateButtonCell() {
        buttonCell = if (comboBox.buttonCell != null)
            comboBox.buttonCell
        else
            getDefaultCellFactory().call(listView)
        buttonCell?.setMouseTransparent(true)
        buttonCell?.updateListView(listView)
        updateDisplayArea()
        // As long as the screen-reader is concerned this node is not a list item.
        // This matters because the screen-reader counts the number of list item
        // within combo and speaks it to the user.
        buttonCell?.setAccessibleRole(AccessibleRole.NODE)
    }

    private fun getDefaultCellFactory(): Callback<ListView<T>, ListCell<T>> {
        return Callback {
            object : ListCell<T>() {
                public override fun updateItem(item: T, empty: Boolean) {
                    super.updateItem(item, empty)
                    updateDisplayText(this, item, empty)
                }
            }
        }
    }

    private fun updateDisplayText(cell: ListCell<T>?, item: T?, empty: Boolean): Boolean {
        if (empty) {
            if (cell == null) return true
            cell.graphic = null
            cell.text = null
            return true
        } else if (item is Node) {
            val currentNode = cell!!.graphic
            val newNode = item
            if (currentNode == null || currentNode != newNode) {
                cell.text = null
                cell.graphic = newNode
            }
            return false
        } else {
            // run item through StringConverter if it isn't null
            val c = comboBox.converter
            val s = if (item == null) comboBox.promptText else if (c == null) item.toString() else c.toString(item)
            cell!!.text = s
            cell.graphic = null
            return s == null || s.isEmpty()
        }
    }

    private fun updateValue() {
        val newValue = comboBox.value

        val listViewSM = listView.selectionModel

        if (newValue == null) {
            listViewSM.clearSelection()
        } else {
            // RT-22386: We need to test to see if the value is in the comboBox
            // items list. If it isn't, then we should clear the listview
            // selection
            val indexOfNewValue = getIndexOfComboBoxValueInItemsList()
            if (indexOfNewValue == -1) {
                //listSelectionLock = true
                listViewSM.clearSelection()
                //listSelectionLock = false
            } else {
                val index = comboBox.selectionModel.selectedIndex
                if (index in comboBoxItems.indices) {
                    val itemsObj = comboBoxItems.get(index)
                    if (itemsObj != null && itemsObj == newValue) {
                        listViewSM.select(index)
                    } else {
                        listViewSM.select(newValue)
                    }
                } else {
                    // just select the first instance of newValue in the list
                    val listViewIndex = comboBoxItems.indexOf(newValue)
                    if (listViewIndex == -1) {
                        // RT-21336 Show the ComboBox value even though it doesn't
                        // exist in the ComboBox items list (part one of fix)
                        updateDisplayNode()
                    } else {
                        listViewSM.select(listViewIndex)
                    }
                }
            }
        }
    }

    private fun getIndexOfComboBoxValueInItemsList(): Int {
        val value = comboBox.value
        val index = comboBoxItems.indexOf(value)
        return index
    }

    override fun updateDisplayNode() {
        if (editor != null) {
            super.updateDisplayNode()
        } else {
            val value = comboBox.value
            val index = getIndexOfComboBoxValueInItemsList()
            if (index > -1) {
                buttonCell?.setItem(null)
                buttonCell?.updateIndex(index)
            } else {
                // RT-21336 Show the ComboBox value even though it doesn't
                // exist in the ComboBox items list (part two of fix)
                buttonCell?.updateIndex(-1)
                val empty = updateDisplayText(buttonCell, value, false)

                // Note that empty boolean collected above. This is used to resolve
                // RT-27834, where we were getting different styling based on whether
                // the cell was updated via the updateIndex method above, or just
                // by directly updating the text. We fake the pseudoclass state
                // for empty, filled, and selected here.
                buttonCell?.pseudoClassStateChanged(PSEUDO_CLASS_EMPTY, empty)
                buttonCell?.pseudoClassStateChanged(PSEUDO_CLASS_FILLED, !empty)
                buttonCell?.pseudoClassStateChanged(PSEUDO_CLASS_SELECTED, true)
            }
        }
    }

    override fun getPopupContent() = listView

    override fun getEditor() = if (skinnable.isEditable) (skinnable as ComboBox<T>).editor else null

    override fun getConverter() = (skinnable as ComboBox<T>).converter

    /* End of Implementation copied from ComboBoxListViewSkin */


}
