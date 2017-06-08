/*******************************************************************************
 * Copyright 2015 Alexander Casall, Manuel Mauky
 * Copyright 2016 Johannes Pfrang
 *  - Forked at mvvmFX 1.5.0 and converted to Kotlin/adapted to tornadoFX

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tornadofx

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.junit.Test
import java.util.ArrayList
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelWrapperTest {

    private inner class Person {
        var name: String? = null
        var age: Int? = null
        var nicknames: MutableList<String> = ArrayList()
    }

    // TODO: we need the JavaFX property as KProperty instead of KFunction, so we have to do things very explicit here.
    //       think about how we can change the ModelWrapper so we can improve this
    private inner class PersonFX {
        val _nameProperty = SimpleStringProperty()
        fun nameProperty() = _nameProperty
        var name: String
            get() = _nameProperty.get()
            set(value) = _nameProperty.set(value)

        val _ageProperty = SimpleIntegerProperty()
        fun ageProperty() = _ageProperty
        var age: Int
            get() = _ageProperty.get()
            set(value) = _ageProperty.set(value)

        val _nicknamesProperty = SimpleListProperty<String>(FXCollections.observableArrayList())
        fun nicknamesProperty() = _nicknamesProperty
        var nicknames: ObservableList<String>
            get() = _nicknamesProperty.get()
            set(value) = _nicknamesProperty.set(value)
    }

    @Test
    fun testWithKotlinProperties() {
        val person = Person()
        person.name = "horst"
        person.age = 32
        person.nicknames = arrayListOf("captain")

        val personWrapper = ModelWrapper(person)

        val name = personWrapper.field(Person::name)
        val age = personWrapper.field(Person::age)
        val nicknames = personWrapper.field(Person::nicknames)

        assertEquals(name.value, "horst")
        assertEquals(age.value, 32)
        assertEquals(nicknames.value, listOf("captain"))


        name.value = "hugo"
        age.value = 33
        nicknames.add("player")

        // still the old values
        assertEquals(person.name, "horst")
        assertEquals(person.age, 32)
        assertEquals(person.nicknames, listOf("captain"))


        personWrapper.commit()

        // now the new values are reflected in the wrapped person
        assertEquals(person.name, "hugo")
        assertEquals(person.age, 33)
        assertEquals(person.nicknames, listOf("captain", "player"))



        name.value = "luise"
        age.value = 15
        nicknames.value = FXCollections.observableArrayList("student")

        personWrapper.reset()

        assertEquals(name.value, null)
        assertEquals(age.value, 0)
        assertEquals(nicknames.value.size, 0)

        // the wrapped object has still the values from the last commit.
        assertEquals(person.name, "hugo")
        assertEquals(person.age, 33)
        assertEquals(person.nicknames, listOf("captain", "player"))


        personWrapper.reload()
        // now the properties have the values from the wrapped object
        assertEquals(name.value, "hugo")
        assertEquals(age.value, 33)
        assertEquals(nicknames.get(), listOf("captain", "player"))


        val otherPerson = Person()
        otherPerson.name = "gisela"
        otherPerson.age = 23
        otherPerson.nicknames = arrayListOf("referee")

        personWrapper.set(otherPerson)
        personWrapper.reload()

        assertEquals(name.value, "gisela")
        assertEquals(age.value, 23)
        assertEquals(nicknames.value, listOf("referee"))

        name.value = "georg"
        age.value = 24
        nicknames.value = FXCollections.observableArrayList("spectator")

        personWrapper.commit()

        // old person has still the old values
        assertEquals(person.name, "hugo")
        assertEquals(person.age, 33)
        assertEquals(person.nicknames, listOf("captain", "player"))

        // new person has the new values
        assertEquals(otherPerson.name, "georg")
        assertEquals(otherPerson.age, 24)
        assertEquals(otherPerson.nicknames, listOf("spectator"))

    }


    @Test
    fun testWithJavaFXProperties() {
        val person = PersonFX()
        person.name = "horst"
        person.age = 32
        person.nicknames = FXCollections.observableArrayList("captain")

        val personWrapper = ModelWrapper(person)

        val name = personWrapper.field(PersonFX::_nameProperty)
        val age = personWrapper.field(PersonFX::_ageProperty)
        val nicknames = personWrapper.field(PersonFX::_nicknamesProperty)

        assertEquals(name.value, "horst")
        assertEquals(age.value, 32)
        assertEquals(nicknames.value, listOf("captain"))


        name.value = "hugo"
        age.value = 33
        nicknames.add("player")

        // still the old values
        assertEquals(person.name, "horst")
        assertEquals(person.age, 32)
        assertEquals(person.nicknames, listOf("captain"))


        personWrapper.commit()

        // now the new values are reflected in the wrapped person
        assertEquals(person.name, "hugo")
        assertEquals(person.age, 33)
        assertEquals(person.nicknames, listOf("captain", "player"))



        name.value = "luise"
        age.value = 15
        nicknames.value = FXCollections.observableArrayList("student")

        personWrapper.reset()

        assertEquals(name.value, null)
        assertEquals(age.value, 0)
        assert(nicknames.value.isEmpty())

        // the wrapped object has still the values from the last commit.
        assertEquals(person.name, "hugo")
        assertEquals(person.age, 33)
        assertEquals(person.nicknames, listOf("captain", "player"))


        personWrapper.reload()
        // now the properties have the values from the wrapped object
        assertEquals(name.value, "hugo")
        assertEquals(age.value, 33)
        assertEquals(nicknames.get(), listOf("captain", "player"))


        val otherPerson = PersonFX()
        otherPerson.name = "gisela"
        otherPerson.age = 23
        otherPerson.nicknames = FXCollections.observableArrayList("referee")

        personWrapper.set(otherPerson)
        personWrapper.reload()

        assertEquals(name.value, "gisela")
        assertEquals(age.value, 23)
        assertEquals(nicknames.get(), listOf("referee"))

        name.value = "georg"
        age.value = 24
        nicknames.value = FXCollections.observableArrayList("spectator")

        personWrapper.commit()

        // old person has still the old values
        assertEquals(person.name, "hugo")
        assertEquals(person.age, 33)
        assertEquals(person.nicknames, listOf("captain", "player"))

        // new person has the new values
        assertEquals(otherPerson.name, "georg")
        assertEquals(otherPerson.age, 24)
    }


    @Test
    fun testDirtyFlag() {
        val person = Person()
        person.name = "horst"
        person.age = 32
        person.nicknames = mutableListOf("captain")

        val personWrapper = ModelWrapper(person)

        assertFalse(personWrapper.isDirty)

        val name = personWrapper.field(Person::name)
        val age = personWrapper.field(Person::age)
        val nicknames = personWrapper.field(Person::nicknames)

        name.set("hugo")

        assertTrue(personWrapper.isDirty)

        personWrapper.commit()
        assertFalse(personWrapper.isDirty)

        age.set(33)
        assertTrue(personWrapper.isDirty)

        age.set(32)
        assertTrue(personWrapper.isDirty) // dirty is still true

        personWrapper.reload()
        assertFalse(personWrapper.isDirty)


        nicknames.add("player")
        assertTrue(personWrapper.isDirty)

        nicknames.remove("player")
        assertTrue(personWrapper.isDirty) // dirty is still true

        personWrapper.commit()
        assertFalse(personWrapper.isDirty)

        name.set("hans")
        assertTrue(personWrapper.isDirty)

        personWrapper.reset()
        assertTrue(personWrapper.isDirty)


        personWrapper.reload()
        assertFalse(personWrapper.isDirty)

        nicknames.set(FXCollections.observableArrayList("player"))
        assertTrue(personWrapper.isDirty)

        personWrapper.reset()
        assertTrue(personWrapper.isDirty)

        personWrapper.reload()
        assertFalse(personWrapper.isDirty)
    }

    @Test
    fun testDirtyFlagWithFxProperties() {
        val person = PersonFX()
        person.name = "horst"
        person.age = 32

        val personWrapper = ModelWrapper(person)

        assertFalse(personWrapper.isDirty)

        val name = personWrapper.field(PersonFX::_nameProperty)
        val age = personWrapper.field(PersonFX::_ageProperty)
        val nicknames = personWrapper.field(PersonFX::_nicknamesProperty)

        name.set("hugo")

        assertTrue(personWrapper.isDirty)

        personWrapper.commit()
        assertFalse(personWrapper.isDirty)

        age.set(33)
        assertTrue(personWrapper.isDirty)

        age.set(32)
        assertTrue(personWrapper.isDirty) // dirty is still true

        personWrapper.reload()
        assertFalse(personWrapper.isDirty)


        nicknames.add("player")
        assertTrue(personWrapper.isDirty)

        nicknames.remove("player")
        assertTrue(personWrapper.isDirty) // dirty is still true

        personWrapper.commit()
        assertFalse(personWrapper.isDirty)

        name.set("hans")
        assertTrue(personWrapper.isDirty)

        personWrapper.reset()
        assertTrue(personWrapper.isDirty)


        personWrapper.reload()
        assertFalse(personWrapper.isDirty)

        nicknames.set(FXCollections.observableArrayList("player"))
        assertTrue(personWrapper.isDirty)

        personWrapper.reset()
        assertTrue(personWrapper.isDirty)

        personWrapper.reload()
        assertFalse(personWrapper.isDirty)
    }

    @Test
    fun testDifferentFlag() {
        val person = Person()
        person.name = "horst"
        person.age = 32
        person.nicknames = mutableListOf("captain")

        val personWrapper = ModelWrapper(person)

        assertFalse(personWrapper.isDifferent)

        val name = personWrapper.field(Person::name)
        val age = personWrapper.field(Person::age)
        val nicknames = personWrapper.field(Person::nicknames)


        name.set("hugo")
        assertTrue(personWrapper.isDifferent)

        personWrapper.commit()
        assertFalse(personWrapper.isDifferent)


        age.set(33)
        assertTrue(personWrapper.isDifferent)

        age.set(32)
        assertFalse(personWrapper.isDifferent)


        nicknames.remove("captain")
        assertTrue(personWrapper.isDifferent)

        nicknames.add("captain")
        assertFalse(personWrapper.isDifferent)

        nicknames.add("player")
        assertTrue(personWrapper.isDifferent)

        nicknames.remove("player")
        assertFalse(personWrapper.isDifferent)

        nicknames.value = FXCollections.observableArrayList("spectator")
        assertTrue(personWrapper.isDifferent)

        personWrapper.reload()
        assertFalse(personWrapper.isDifferent)

        nicknames.add("captain") // duplicate captain
        assertTrue(personWrapper.isDifferent)

        person.nicknames.add(
                "captain") // now both have 2x "captain" but the modelWrapper has no chance to realize this change in the model element...
        // ... for this reason the different flag will still be true
        assertTrue(personWrapper.isDifferent)

        // ... but if we add another value to the nickname-Property, the modelWrapper can react to this change
        person.nicknames.add("other")
        nicknames.add("other")
        assertFalse(personWrapper.isDifferent)



        nicknames.add("player")
        assertTrue(personWrapper.isDifferent)

        nicknames.remove("player")
        assertFalse(personWrapper.isDifferent)

        nicknames.value = FXCollections.observableArrayList("spectator")
        assertTrue(personWrapper.isDifferent)

        personWrapper.reload()
        assertFalse(personWrapper.isDifferent)


        name.value = "hans"
        assertTrue(personWrapper.isDifferent)

        personWrapper.reload()
        assertFalse(personWrapper.isDifferent)


        personWrapper.reset()
        assertTrue(personWrapper.isDifferent)
    }

    @Test
    fun testDifferentFlagWithFxProperties() {
        val person = PersonFX()
        person.name = "horst"
        person.age = 32
        person.nicknames = FXCollections.observableArrayList("captain")

        val personWrapper = ModelWrapper(person)

        assertFalse(personWrapper.isDifferent)

        val name = personWrapper.field(PersonFX::_nameProperty)
        val age = personWrapper.field(PersonFX::_ageProperty)
        val nicknames = personWrapper.field(PersonFX::_nicknamesProperty)


        name.set("hugo")
        assertTrue(personWrapper.isDifferent)

        personWrapper.commit()
        assertFalse(personWrapper.isDifferent)


        age.set(33)
        assertTrue(personWrapper.isDifferent)

        age.set(32)
        assertFalse(personWrapper.isDifferent)


        nicknames.remove("captain")
        assertTrue(personWrapper.isDifferent)

        nicknames.add("captain")
        assertFalse(personWrapper.isDifferent)

        person.nicknames.add("captain") // duplicate value
        nicknames.add("captain")
        assertFalse(personWrapper.isDifferent)

        nicknames.add("player")
        assertTrue(personWrapper.isDifferent)

        person.nicknames.add("player")
        assertTrue(
                personWrapper.isDifferent) // still true because the modelWrapper can't detect the change in the model

        person.name = "luise"
        name.set(
                "luise") // this triggers the recalculation of the different-flag which will now detect the previous change to the nicknames list
        assertFalse(personWrapper.isDifferent)



        nicknames.value = FXCollections.observableArrayList("spectator")
        assertTrue(personWrapper.isDifferent)

        personWrapper.reload()
        assertFalse(personWrapper.isDifferent)


        name.value = "hans"
        assertTrue(personWrapper.isDifferent)

        personWrapper.reload()
        assertFalse(personWrapper.isDifferent)


        personWrapper.reset()
        assertTrue(personWrapper.isDifferent)
    }

    @Test
    fun defaultValuesCanBeUpdatedToCurrentValues() {
        val person = Person()
        person.name = "horst"
        person.age = 32
        person.nicknames = mutableListOf("captain")

        val cut = ModelWrapper(person)

        val nameField = cut.field(Person::name, person.name)
        nameField.set("test")
        cut.commit()
        cut.useCurrentValuesAsDefaults()
        cut.reset()
        assertEquals(person.name, "test")
        assertEquals(nameField.get(), "test")

        val ageField = cut.field(Person::age, person.age)
        ageField.set(42)
        cut.commit()
        cut.useCurrentValuesAsDefaults()
        cut.reset()
        assertEquals(person.age, 42)
        assertEquals(ageField.get(), 42)

        val nicknames = cut.field(Person::nicknames, person.nicknames)
        nicknames.add("myname")
        nicknames.remove("captain")
        cut.commit()
        cut.useCurrentValuesAsDefaults()
        cut.reset()
        assertEquals(person.nicknames, listOf("myname"))
        assertEquals(nicknames.get(), listOf("myname"))
    }

    @Test
    fun valuesShouldBeUpdatedWhenModelInstanceChanges() {
        val person1 = Person()
        person1.name = "horst"
        person1.age = 32
        person1.nicknames = mutableListOf("captain")
        val person2 = Person()
        person2.name = "dieter"
        person2.age = 42
        person2.nicknames = mutableListOf("robin")

        val modelProp = SimpleObjectProperty(person1)

        val cut = ModelWrapper(modelProp)

        val nameField = cut.field(Person::name, person1.name)
        val ageField = cut.field(Person::age, person1.age)
        val nicknames = cut.field(Person::nicknames, person1.nicknames)

        assertEquals(nameField.get(), person1.name)
        assertEquals(ageField.get(), person1.age)
        assertEquals(nicknames.get(), person1.nicknames)

        modelProp.set(person2)
        assertEquals(nameField.get(), person2.name)
        assertEquals(ageField.get(), person2.age)
        assertEquals(nicknames.get(), person2.nicknames)

        cut.reset()
        assertEquals(nameField.get(), person2.name)
        assertEquals(ageField.get(), person2.age)
        assertEquals(nicknames.get(), person2.nicknames)
    }

    @Test
    fun testFieldEquality() {
        val person = Person()
        person.name = "horst"
        person.age = 32
        person.nicknames = mutableListOf("captain")

        val personWrapper = ModelWrapper(person)

        val name = personWrapper.field(Person::name)
        val name2 = personWrapper.field(Person::name)
        assert(personWrapper.size == 1)

        val age = personWrapper.field(Person::age)
        val age2 = personWrapper.field(Person::age)
        assert(personWrapper.size == 2)

        val nicknames = personWrapper.field(Person::nicknames)
        val nicknames2 = personWrapper.field(Person::nicknames)
        assert(personWrapper.size == 3)
    }

    @Test
    fun testFieldEqualityWithFxProperties() {
        val person = PersonFX()
        person.name = "horst"
        person.age = 32
        person.nicknames = FXCollections.observableArrayList("captain")

        val personWrapper = ModelWrapper(person)

        val name = personWrapper.field(PersonFX::_nameProperty)
        val name2 = personWrapper.field(PersonFX::_nameProperty)
        assert(personWrapper.size == 1)

        val age = personWrapper.field(PersonFX::_ageProperty)
        val age2 = personWrapper.field(PersonFX::_ageProperty)
        assert(personWrapper.size == 2)

        val nicknames = personWrapper.field(PersonFX::_nicknamesProperty)
        val nicknames2 = personWrapper.field(PersonFX::_nicknamesProperty)
        assert(personWrapper.size == 3)
    }
}
