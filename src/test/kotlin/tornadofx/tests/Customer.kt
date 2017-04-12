package tornadofx.tests

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tornadofx.getValue
import tornadofx.setValue

class Customer {
    enum class Type { Private, Company }

    val nameProperty = SimpleStringProperty()
    var name by nameProperty

    val zipProperty = SimpleStringProperty()
    var zip by zipProperty

    val cityProperty = SimpleStringProperty()
    var city by cityProperty

    val typeProperty = SimpleObjectProperty<Type>()
    var type by typeProperty

    override fun toString(): String {
        return "Customer(name=$name, zip=$zip, city=$city)"
    }
}

class CustomerModel : ItemViewModel<Customer>() {
    val name = bind(autocommit = true) { item?.nameProperty }
    val zip = bind(autocommit = true) { item?.zipProperty }
    val city = bind(autocommit = true) { item?.cityProperty }
    val type = bind(autocommit = true) { item?.typeProperty }
}
