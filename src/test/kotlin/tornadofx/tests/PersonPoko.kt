package tornadofx.tests

data class PersonPoko(var name: String, var phone: String?, var email: String?, var age: Int, var parent: PersonPoko?) {
    constructor(name: String, age: Int) : this(name, null, null, age, null)
    constructor() : this("", 18)
}
