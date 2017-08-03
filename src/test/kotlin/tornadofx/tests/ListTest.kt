package tornadofx.tests

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import org.junit.Assert
import org.junit.Test
import tornadofx.*

class ListTest {
    class Person(name: String, age: Int, checked: Boolean = true) {
        val name = SimpleStringProperty(name)
        val age = SimpleIntegerProperty(age)
        val checked = SimpleBooleanProperty(checked)
        override fun toString() = name.value
    }

    val persons = FXCollections.observableArrayList(
            Person("Samantha Stuart", 34, true),
            Person("Tom Marks", 42, false),
            Person("Stuart Gills", 17),
            Person("Nicole Williams", 27))

    @Test
    fun filterTest() {
        val data = SortedFilteredList(persons)
        data.predicate = { it.name.get().contains("Stu") }
        Assert.assertEquals(2, data.size)
    }

}