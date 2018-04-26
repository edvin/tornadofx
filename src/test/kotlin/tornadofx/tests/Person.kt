package tornadofx.tests

import javafx.collections.FXCollections
import javafx.scene.input.DataFormat
import tornadofx.*
import java.io.Serializable
import java.util.*

class PersonRef(val id: UUID) : Serializable

class Person(name: String, age: Int) {
    constructor() : this("", 18)

    val id = UUID.randomUUID()

    var name: String by property(name)
    fun nameProperty() = getProperty(Person::name)

    var phone: String by property()
    fun phoneProperty() = getProperty(Person::phone)

    var email: String by property()
    fun emailProperty() = getProperty(Person::email)

    var age by property(age)
    fun ageProperty() = getProperty(Person::age)

    var parent by property<Person>()
    fun parentProperty() = getProperty(Person::parent)

    val children = FXCollections.observableArrayList<Person>()

    override fun toString() = name

    class PersonDataFormat : DataFormat("tornadofx.Person")

    companion object {
        val DATA_FORMAT = PersonDataFormat()
    }
}