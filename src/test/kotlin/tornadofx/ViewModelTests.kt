package tornadofx

import org.junit.Assert
import org.junit.Test

open class ViewModelTests {

    @Test fun simple_commit() {
        val person = Person("John")
        val viewModel = PersonViewModel(person)

        viewModel.name = "Jay"
        Assert.assertEquals(person.name, "John")
        viewModel.commit()
        Assert.assertEquals(person.name, "Jay")
    }

    @Test fun swap_source_object() {
        val person1 = Person("Person 1")
        val person2 = Person("Person 2")

        val viewModel = PersonViewModel(person1)
        Assert.assertEquals(viewModel.name, "Person 1")

        viewModel.rebind {
            person = person2
        }

        Assert.assertEquals(viewModel.name, "Person 2")
    }

    @Test fun pojo_commit() {
        val person = JavaPerson()
        person.name = "John"
        val viewModel = JavaPersonViewModel(person)

        viewModel.name.value = "Jay"
        Assert.assertEquals(person.name, "John")
        viewModel.commit()
        Assert.assertEquals(person.name, "Jay")
    }

    @Test fun var_commit_check_dirty_state() {
        val person = Person("John")
        val viewModel = PersonViewModel(person)

        Assert.assertFalse(viewModel.isDirty())

        viewModel.name = "Jay"
        Assert.assertEquals(person.name, "John")
        Assert.assertTrue(viewModel.isDirty())

        viewModel.commit()
        Assert.assertEquals(person.name, "Jay")
        Assert.assertFalse(viewModel.isDirty())
    }

    @Test fun inline_viewmodel() {
        val person = Person("John")

        val viewModel = object : ViewModel() {
            val name = bind { person.nameProperty() }
        }

        viewModel.name.value = "Jay"
        Assert.assertEquals(person.name, "John")
        viewModel.commit()
        Assert.assertEquals(person.name, "Jay")
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