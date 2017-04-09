package tornadofx.tests

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*

class Customer {
    val nameProperty = SimpleStringProperty()
    var name by nameProperty

    val zipProperty = SimpleStringProperty()
    var zip by zipProperty

    val cityProperty = SimpleStringProperty()
    var city by cityProperty


    val primaryContactProperty = SimpleObjectProperty<Person>()
    var primaryContact by primaryContactProperty
}

class CustomerModel : ItemViewModel<Customer>() {
    val name = bind { item?.nameProperty }
    val zip = bind { item?.zipProperty }
    val city = bind { item?.cityProperty }
    val primaryContact = bind { item?.primaryContactProperty }
}
