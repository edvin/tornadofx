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
        val viewModel = PersonViewModel(person)

        viewModel.name = "Jay"
        assertEquals(person.name, "John")
        viewModel.commit()
        assertEquals(person.name, "Jay")
    }

    @Test fun swap_source_object() {
        val person1 = Person("Person 1", 37)
        val person2 = Person("Person 2", 33)

        val viewModel = PersonViewModel(person1)
        assertEquals(viewModel.name, "Person 1")

        viewModel.rebind {
            person = person2
        }

        assertEquals(viewModel.name, "Person 2")
    }

    @Test fun pojo_commit() {
        val person = JavaPerson()
        person.name = "John"
        val viewModel = JavaPersonViewModel(person)

        viewModel.name.value = "Jay"
        assertEquals(person.name, "John")
        viewModel.commit()
        assertEquals(person.name, "Jay")
    }

    @Test fun var_commit_check_dirty_state() {
        val person = Person("John", 37)
        val viewModel = PersonViewModel(person)

        assertFalse(viewModel.isDirty())

        viewModel.name = "Jay"
        assertEquals(person.name, "John")
        assertTrue(viewModel.isDirty())

        viewModel.commit()
        assertEquals(person.name, "Jay")
        assertFalse(viewModel.isDirty())
    }

    @Test fun inline_viewmodel() {
        val person = Person("John", 37)

        val viewModel = object : ViewModel() {
            val name = bind { person.nameProperty() } as SimpleStringProperty
            val age = bind { person.ageProperty() } as SimpleIntegerProperty
        }

        viewModel.name.value = "Jay"
        assertEquals(person.name, "John")
        viewModel.commit()
        assertEquals(person.name, "Jay")
    }

    @Test fun tableview_master_detail() {
        val tableview = TableView<Person>()
        tableview.items.addAll(Person("John", 37), Person("Jay", 33))
        val viewModel = JavaFXPersonViewModel(tableview.items.first())
        assertEquals(viewModel.name.value, "John")
        viewModel.rebindOnChange(tableview) {
            person = it ?: Person()
        }
        tableview.selectionModel.select(1)
        assertEquals(viewModel.name.value, "Jay")
    }
}

// JavaFX Property
class JavaFXPersonViewModel(var person: Person) : ViewModel() {
    val name = bind { person.nameProperty() }
}

// JavaFX Property exposing both getter/setter as well (optional)
class PersonViewModel(var person: Person) : ViewModel() {
    var name : String by property { person.nameProperty() }
    fun nameProperty() = getProperty(PersonViewModel::name)
}

// Java POJO getter/setter property
class JavaPersonViewModel(person: JavaPerson) : ViewModel() {
    val name = bind { person.observable(JavaPerson::getName, JavaPerson::setName) }
}

// Kotlin var property
class PersonVarViewModel(person: Person) : ViewModel() {
    val name = bind { person.observable(Person::name) }
}