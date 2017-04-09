package tornadofx.testapps

import tornadofx.*
import tornadofx.tests.CustomerModel

class WizardTestApp : App(WizardTestView::class)

class WizardTestView : View("Wizard Test") {
    override val root = button("Create customer") {
        isDefaultButton = true
        action {
            // TODO: Find a nicer way to find the Wizard in a new scope
            find<CustomerWizard>(Scope()).openModal()
        }
    }
}

class CustomerWizard : Wizard("Create customer", "Provide customer information") {
    val customer: CustomerModel by inject()

    init {
        add(WizardStep1::class)
        add(WizardStep2::class)
    }

    override fun onSave() {
        println(customer.name)
    }
}

class WizardStep1 : View("Customer Data") {
    val customer: CustomerModel by inject()
    override val complete = customer.valid

    override val root = form {
        fieldset(title) {
            field("Name") {
                textfield(customer.name).required()
            }
            field("Zip/City") {
                textfield(customer.zip) {
                    prefColumnCount = 7
                    required()
                }
                textfield(customer.city) {
                    required()
                }
            }
        }
    }

    override fun onSave() {
        customer.commit()
    }
}

class WizardStep2 : View("Contact person") {
    override val root = form {
        fieldset(title) {
            field("Name") {
                textfield()
            }
            field("Phone") {
                textfield()
            }
            field("Email") {
                textfield()
            }
        }
    }
}
