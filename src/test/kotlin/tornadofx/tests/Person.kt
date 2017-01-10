package tornadofx.tests

import tornadofx.getProperty
import tornadofx.property

class Person(name: String, age: Int) {
    constructor() : this("", 18)

    var name: String by property(name)
    fun nameProperty() = getProperty(Person::name)

    var age by property(age)
    fun ageProperty() = getProperty(Person::age)

    var parent by property<Person>()
    fun parentProperty() = getProperty(Person::parent)

    override fun toString() = name
}