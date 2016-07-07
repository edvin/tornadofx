package tornadofx

class Person(name: String) {
    var name: String by property(name)
    fun nameProperty() = getProperty(Person::name)
}