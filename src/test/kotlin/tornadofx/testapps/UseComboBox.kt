package tornadofx.testapps

import javafx.beans.property.SimpleIntegerProperty
import javafx.util.StringConverter
import tornadofx.*

class UseComboBox: App(UseComboBoxView::class)

class UseComboBoxView: View() {
    override val root = anchorpane{
        val data = listOf(DataItem(1), DataItem(2), DataItem(3)).asObservable()
        tableview(data) {
            isEditable = true
            column("1", DataItem::a).useComboBox(listOf(1, 2, 3).asObservable(), IntegerConverter())
            column("2", DataItem::a).useChoiceBox(listOf(1, 2, 3).asObservable(), IntegerConverter())
            column("3", DataItem::a).useComboBox(listOf(1, 2, 3).asObservable())
        }
    }
}

class DataItem(a: Int){
    private val aProperty = SimpleIntegerProperty(a)
    var a: Int? by aProperty
}

class IntegerConverter: StringConverter<Int>() {
    override fun toString(`object`: Int): String {
        return when(`object`){
            1 -> "first"
            2 -> "second"
            3 -> "third"
            else -> `object`.toString()
        }

    }

    override fun fromString(string: String): Int {
        return when(string){
            "first" -> 1
            "second" -> 2
            "third" -> 3
            else -> throw IllegalArgumentException("unsupported value")
        }
    }
}