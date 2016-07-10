package tornadofx

class Person(name: String, age: Int) {
    var name: String by property(name)
    fun nameProperty() = getProperty(Person::name)

    var age by property(age)
    fun ageProperty() = getProperty(Person::age)
}