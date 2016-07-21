package tornadofx

import javafx.beans.property.SimpleStringProperty
import javafx.scene.layout.BorderPane

class Person(name: String, age: Int) {
    constructor() : this("", 18)

    val nameProperty = SimpleStringProperty(name)
    var name by nameProperty

    var age by property(age)
    fun ageProperty() = getProperty(Person::age)
}

class Omnipotent : View() {
    override val root = BorderPane()
    init {
        title = "People"
        with(root) {
            left = listview<Person> {
                items = listOf(Person("One", 1), Person("Two", 2)).observable()
                cellFormat {
                    textProperty().bind(it.nameProperty)
                }
            }
        }
    }
}