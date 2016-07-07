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
            val name = person.nameProperty().wrap()
        }

        viewModel.name.value = "Jay"
        Assert.assertEquals(person.name, "John")
        viewModel.commit()
        Assert.assertEquals(person.name, "Jay")
    }

}

class PersonViewModel(person: Person) : ViewModel() {
    val name = person.nameProperty().wrap()
}

class JavaPersonViewModel(person: JavaPerson) : ViewModel() {
    val name = person.wrap(JavaPerson::getName, JavaPerson::setName)
}

class PersonVarViewModel(person: Person) : ViewModel() {
    val name = person.wrap(Person::name)
}