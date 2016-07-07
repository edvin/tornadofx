package tornadofx

import org.junit.Assert
import org.junit.Test

open class ViewModelTests {

    @Test fun simple_commit() {
        val person = Person("John")
        val viewModel = PersonViewModel(person)

        viewModel.name.value = "Jay"
        Assert.assertEquals(person.name, "John")
        viewModel.commit()
        Assert.assertEquals(person.name, "Jay")
    }

    @Test fun swap_source_object() {
        val person1 = Person("Person 1")
        val person2 = Person("Person 2")

        val viewModel = PersonViewModel(person1)
        Assert.assertEquals(viewModel.name.value, "Person 1")

        viewModel.person = person2
        viewModel.rebind()
        Assert.assertEquals(viewModel.name.value, "Person 2")
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

        viewModel.name.value = "Jay"
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

class PersonViewModel(var person: Person) : ViewModel() {
    val name = bind { person.nameProperty() }
}

class JavaPersonViewModel(person: JavaPerson) : ViewModel() {
    val name = bind { person.observable(JavaPerson::getName, JavaPerson::setName) }
}

class PersonVarViewModel(person: Person) : ViewModel() {
    val name = bind { person.observable(Person::name) }
}