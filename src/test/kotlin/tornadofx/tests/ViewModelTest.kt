package tornadofx.tests

import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.TableView
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.stage.Stage
import org.junit.Assert.*
import org.junit.Test
import org.testfx.api.FxToolkit
import tornadofx.*

open class ViewModelTest {
    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    @Test fun rebind_to_null_unbinds() {
        val personProperty = SimpleObjectProperty<Person>()

        val person1 = Person("John", 37)
        val person2 = Person("Jay", 32)

        val model = PersonAutoModel(null)
        model.rebindOnChange(personProperty) {
            model.person = it
        }

        model.name.onChange {
            println("Person name changed to $it")
        }

        personProperty.value = person1
        assertEquals("John", model.name.value)
        personProperty.value = person2
        assertEquals("Jay", model.name.value)
        personProperty.value = null
        assertNull(model.person)
    }

    @Test fun auto_commit() {
        val person = Person("John", 37)
        val model = PersonAutoModel(person)

        assertEquals(person.name, "John")
        model.name.value = "Jay"
        assertEquals(person.name, "Jay")
    }

    @Test fun external_change() {
        val person = Person("John", 37)
        val model = PersonModel(person)

        assertEquals(model.name.value, "John")
        person.name = "Jay"
        assertEquals(model.name.value, "Jay")
    }

    @Test fun simple_commit() {
        val person = Person("John", 37)
        val model = PersonModel(person)
        val isNameDirty = model.dirtyStateFor(PersonModel::name)
        isNameDirty.onChange {
            println("Name is dirty: $it")
        }
        model.name.value = "Jay"
        assertEquals(person.name, "John")
        model.commit()
        assertEquals(person.name, "Jay")
    }

    @Test fun swap_source_object() {
        val person1 = Person("Person 1", 37)
        val person2 = Person("Person 2", 33)

        val model = PersonModel(person1)
        assertEquals(model.name.value, "Person 1")

        model.item = person2

        assertEquals(model.name.value, "Person 2")
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

        assertFalse(model.isDirty)

        model.name.value = "Jay"
        assertEquals(person.name, "John")
        assertTrue(model.name.isDirty)
        assertTrue(model.isDirty)

        model.commit()
        assertEquals(person.name, "Jay")
        assertFalse(model.name.isDirty)
        assertFalse(model.isDirty)
    }

    @Test fun inline_viewmodel() {
        val person = Person("John", 37)

        val model = object : ViewModel() {
            val name = bind { person.nameProperty() } as SimpleStringProperty
        }

        model.name.value = "Jay"
        assertEquals(person.name, "John")
        model.commit()
        assertEquals(person.name, "Jay")
    }

    @Test fun tableview_master_detail() {
        val tableview = TableView<Person>()
        tableview.items.addAll(Person("John", 37), Person("Jay", 33))
        val model = PersonModel(tableview.items.first())
        assertEquals(model.name.value, "John")
        tableview.bindSelected(model)
        tableview.selectionModel.select(1)
        assertEquals(model.name.value, "Jay")
    }

    @Test fun treeview_master_detail() {
        val rootPerson = Person("John", 37)
        val treeview = TreeView<Person>(TreeItem(rootPerson))
        treeview.populate { it.value.children }
        rootPerson.children.add(Person("Jay", 33))
        val model = PersonModel(rootPerson)
        assertEquals(model.name.value, "John")
        treeview.bindSelected(model)
        treeview.selectionModel.select(treeview.root.children.first())
        assertEquals(model.name.value, "Jay")
    }

    @Test fun committed_properties() {
        val person = Person("John", 37)
        val model = object : PersonModel(person) {
            override fun onCommit(commits: List<Commit>) {
                assertEquals(4, commits.size)
                assertEquals(1, commits.filter { it.changed }.size)
                val theOnlyChange = commits.find { it.changed }!!
                assertEquals(name, theOnlyChange.property)
                assertEquals("John", theOnlyChange.oldValue)
                assertEquals("Johnnie", theOnlyChange.newValue)
            }
        }
        model.name.value = "Johnnie"
        model.commit()
    }
}

class PersonAutoModel(var person: Person? = null) : ViewModel() {
    val name = bind(true) { person?.nameProperty() ?: SimpleStringProperty() as Property<String> }
}

// JavaFX Property
open class PersonModel(person: Person? = null) : ItemViewModel<Person>(person) {
    val name = bind { item?.nameProperty() }
    val age = bind { item?.ageProperty() }
    val phone = bind { item?.phoneProperty() }
    val email = bind { item?.emailProperty() }
}

// Java POJO getter/setter property
class JavaPersonModel(person: JavaPerson) : ViewModel() {
    val name = bind { person.observable(JavaPerson::getName, JavaPerson::setName) }
}

// Kotlin var property
class PersonVarModel(person: Person) : ViewModel() {
    val name = bind { person.observable(Person::name) }
}