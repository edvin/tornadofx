package tornadofx.tests

import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.text.FontWeight
import tornadofx.*

class ListViewTestApp : App(ListViewFragmentTestView::class)

class ListViewItem(value: String) {
    val valueProperty = SimpleStringProperty(value)
}

class MyItemViewModel : ItemViewModel<ListViewItem>() {
    val value = bind { item?.valueProperty }
}

class PersonListFragment : ListCellFragment<Person>() {
    val person = PersonModel().bindTo(this)

    override val root = form {
        fieldset {
            field("Name") {
                label(person.name)
            }
            field("Age") {
                label(person.age)
            }
            label(stringBinding(person.age) { "$value years old" }) {
                alignment = Pos.CENTER_RIGHT
                style {
                    fontSize = 22.px
                    fontWeight = FontWeight.BOLD
                }
            }
        }
    }
}

class ListViewFragmentTestView : View("ListCellFragment Test") {
    val items = observableListOf(Person("John", 42), Person("Jane", 24), Person("Tommy", 11))

    override val root = listview(items) {
        cellFragment(PersonListFragment::class)
    }
}