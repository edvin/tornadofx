package tornadofx.tests

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*

class Customer {
    enum class Type { Private, Company }

    val nameProperty = SimpleStringProperty()
    var name by nameProperty

    val zipProperty = SimpleStringProperty()
    var zip by zipProperty

    val cityProperty = SimpleStringProperty()
    var city by cityProperty

    val typeProperty = SimpleObjectProperty<Type>(Type.Private)
    var type by typeProperty

    override fun toString(): String {
        return "Customer(name=$name, zip=$zip, city=$city)"
    }
}

class CustomerModel : ItemViewModel<Customer>() {
    val name = bind(Customer::nameProperty, autocommit = true)
    val zip  = bind(Customer::zipProperty, autocommit = true)
    val city = bind(Customer::cityProperty, autocommit = true)
    val type = bind(Customer::typeProperty, autocommit = true)
}
