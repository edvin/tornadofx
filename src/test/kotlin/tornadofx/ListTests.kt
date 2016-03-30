package tornadofx

import javafx.collections.FXCollections
import org.junit.Assert
import org.junit.Test

class ListTests {
    data class Person(val name: String, val age: Int)

    val persons = FXCollections.observableArrayList(
            Person("Samantha Stuart", 34),
            Person("Tom Marks", 42),
            Person("Stuart Gills", 17),
            Person("Nicole Williams", 27))

    @Test
    fun filterTest() {
        val data = SortedFilteredList(persons)
        data.predicate = { it.name.contains("Stu")}
        Assert.assertEquals(2, data.filteredItems.size)
    }

}
