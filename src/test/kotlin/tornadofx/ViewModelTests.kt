package tornadofx

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.TableView
import javafx.stage.Stage
import org.junit.Assert.*
import org.junit.Test
import org.testfx.api.FxToolkit

open class ViewModelTests {
    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    @Test fun simple_commit() {
        val person = Person("John", 37)
        val model = PersonModel(person)

        model.name = "Jay"
        assertEquals(person.name, "John")
        model.commit()
        assertEquals(person.name, "Jay")
    }

@Test fun swap_source_object() {
    val person1 = Person("Person 1", 37)
    val person2 = Person("Person 2", 33)

    val model = PersonModel(person1)
    assertEquals(model.name, "Person 1")

    model.rebind {
        person = person2
    }

    assertEquals(model.name, "Person 2")
}

    @Test fun pojo_commit() {
        val person = JavaPerson()
        person.name = "John"
        val model = JavaPersonModel(person)

        model.name.value = "Jay"
        assertEquals(person.name, "John")
        model.commit()
        assertEquals(person.name, "Jay")
    }

    @Test fun var_commit_check_dirty_state() {
        val person = Person("John", 37)
        val model = PersonModel(person)

        assertFalse(model.isDirty())

        model.name = "Jay"
        assertEquals(person.name, "John")
        assertTrue(model.isDirty())

        model.commit()
        assertEquals(person.name, "Jay")
        assertFalse(model.isDirty())
    }

    @Test fun inline_viewmodel() {
        val person = Person("John", 37)

        val model = object : ViewModel() {
            val name = bind { person.nameProperty() } as SimpleStringProperty
            val age = bind { person.ageProperty() } as SimpleIntegerProperty
        }

        model.name.value = "Jay"
        assertEquals(person.name, "John")
        model.commit()
        assertEquals(person.name, "Jay")
    }

    @Test fun tableview_master_detail() {
        val tableview = TableView<Person>()
        tableview.items.addAll(Person("John", 37), Person("Jay", 33))
        val model = JavaFXPersonViewModel(tableview.items.first())
        assertEquals(model.name.value, "John")
        model.rebindOnChange(tableview) {
            person = it ?: Person()
        }
        tableview.selectionModel.select(1)
        assertEquals(model.name.value, "Jay")
    }
}

// JavaFX Property
class JavaFXPersonViewModel(var person: Person) : ViewModel() {
    val name = bind { person.nameProperty() }
}

// JavaFX Property exposing both getter/setter as well (optional)
class PersonModel(var person: Person) : ViewModel() {
    var name : String by property { person.nameProperty() }
    fun nameProperty() = getProperty(PersonModel::name)
}

// Java POJO getter/setter property
class JavaPersonModel(person: JavaPerson) : ViewModel() {
    val name = bind { person.observable(JavaPerson::getName, JavaPerson::setName) }
}

// Kotlin var property
class PersonVarModel(person: Person) : ViewModel() {
    val name = bind { person.observable(Person::name) }
}